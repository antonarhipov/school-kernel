package org.schoolkernel.solver;

import java.util.*;
import org.schoolkernel.domain.SchoolDefinition;

/** Version-10 maximal runs over the full original teaching grid, independent of solver value filtering. */
public record ConsecutivePolicy(String cohortId, String subjectId, List<SchoolDefinition.Period> grid,
        List<SchoolDefinition.ConsecutiveBlock> rules) {
    public static ConsecutivePolicy of(SchoolDefinition definition, String cohort, String subject) {
        return new ConsecutivePolicy(cohort, subject, definition.periods().stream().sorted(
                Comparator.comparing(SchoolDefinition.Period::weekday).thenComparingInt(SchoolDefinition.Period::order)).toList(),
                definition.consecutiveBlocks().stream().filter(r -> r.cohortId().equals(cohort)
                        && r.subjectId().equals(subject)).toList());
    }
    public record Analysis(long violations, long exemption, List<List<String>> conflicts) {}

    public Analysis analyze(List<PlanningLesson> lessons) {
        var placed = new HashMap<String, List<PlanningLesson>>();
        lessons.stream().filter(l -> l.getPeriod() != null && l.getRoom() != null).forEach(l ->
                placed.computeIfAbsent(l.getPeriod().id(), ignored -> new ArrayList<>()).add(l));
        var runs = new ArrayList<List<PlanningLesson>>();
        var run = new ArrayList<PlanningLesson>();
        SchoolDefinition.Period previous = null;
        for (var period : grid) {
            var at = placed.getOrDefault(period.id(), List.of());
            boolean adjacent = previous != null && previous.weekday() == period.weekday() && previous.adjacentToNext();
            if (!adjacent || at.size() != 1) { if (!run.isEmpty()) runs.add(List.copyOf(run)); run.clear(); }
            if (at.size() == 1) run.add(at.getFirst());
            if (at.isEmpty()) { if (!run.isEmpty()) runs.add(List.copyOf(run)); run.clear(); }
            previous = period;
        }
        if (!run.isEmpty()) runs.add(List.copyOf(run));
        var validCounts = new HashMap<Integer, Long>();
        var conflicts = new ArrayList<List<String>>();
        for (var block : runs) {
            if (block.size() < 2) continue;
            var first = block.getFirst();
            boolean same = block.stream().allMatch(l -> l.getTeacherId().equals(first.getTeacherId())
                    && l.getRoom().id().equals(first.getRoom().id()));
            boolean declared = rules.stream().anyMatch(r -> r.length() == block.size());
            if (same && declared && block.stream().allMatch(ConsecutivePolicy::individuallyValid)) validCounts.merge(block.size(), 1L, Long::sum);
            else conflicts.add(evidence("observed run length=" + block.size() + "; shared teacher/room=" + same, block));
        }
        long violations = conflicts.size(), exemption = 0;
        for (var rule : rules) {
            long count = validCounts.getOrDefault(rule.length(), 0L);
            long difference = rule.operator().equals("EXACTLY") ? Math.abs(count - rule.weeklyCount())
                    : Math.max(0, rule.weeklyCount() - count);
            if (difference > 0) conflicts.add(evidence(rule.id() + ": expected " + rule.operator() + " "
                    + rule.weeklyCount() + " blocks of " + rule.length() + "; observed=" + count, lessons));
            violations += difference;
            exemption += (rule.length() - 1L) * Math.min(rule.weeklyCount(), count);
        }
        return new Analysis(violations, exemption, List.copyOf(conflicts));
    }
    private static boolean individuallyValid(PlanningLesson lesson) {
        var p=lesson.getPeriod(); var r=lesson.getRoom();
        return lesson.getTeacherAvailablePeriodIds().contains(p.id())
                && lesson.getCohortAvailablePeriodIds().contains(p.id()) && r.availablePeriodIds().contains(p.id())
                && r.capacity()>=lesson.getCohortSize() && r.capabilityIds().containsAll(lesson.getRequiredRoomCapabilityIds())
                && (lesson.getPeriodLock()==null||lesson.getPeriodLock().equals(p.id()))
                && (lesson.getRoomLock()==null||lesson.getRoomLock().equals(r.id()))
                && lesson.roomAssignmentAllows(r.id()) && !SchoolConstraintProvider.usesForbiddenReservedPeriod(lesson)
                && !SchoolConstraintProvider.outsideHomeRoom(lesson);
    }
    private List<String> evidence(String description, List<PlanningLesson> lessons) {
        var refs = new ArrayList<String>(List.of(cohortId, subjectId, description));
        for (var l : lessons) { refs.add(l.getId()); if (l.getPeriod() != null) refs.add(l.getPeriod().id()); }
        return List.copyOf(refs);
    }
    public static long seriesPenalty(List<PlanningLesson> lessons) {
        var days = new HashMap<java.time.DayOfWeek, Long>();
        lessons.forEach(l -> days.merge(l.getPeriod().weekday(), 1L, Long::sum));
        long ordinary = days.values().stream().mapToLong(n -> Math.max(0, n - 1)).sum();
        var policy = lessons.getFirst().getConsecutivePolicy();
        return Math.max(0, ordinary - (policy == null ? 0 : policy.analyze(lessons).exemption()));
    }
}
