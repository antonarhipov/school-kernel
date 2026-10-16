package org.schoolkernel.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.schoolkernel.domain.KernelCatalog;

public final class ScheduleEvaluator {
    public record Evaluation(
            boolean complete,
            boolean feasible,
            Map<String, Long> hardMatchCounts,
            Map<String, Long> softMatchCounts,
            long periodMoves,
            long roomOnlyMoves,
            long ordinaryPreferencePenalty) {}

    public Evaluation evaluate(SchoolSchedule schedule, Map<String, Long> weights) {
        var hard = zeroCounts(KernelCatalog.hardConstraintIds());
        var soft = zeroCounts(KernelCatalog.softConstraintIds());
        boolean complete = schedule.getLessons().stream()
                .allMatch(lesson -> lesson.getPeriod() != null && lesson.getRoom() != null);
        if (!complete) {
            return new Evaluation(false, false, Map.copyOf(hard), Map.copyOf(soft), 0, 0, 0);
        }

        long periodMoves = 0;
        long roomOnlyMoves = 0;
        var teacherPeriods = new HashMap<String, List<String>>();
        var cohortPeriods = new HashMap<String, List<String>>();
        var roomPeriods = new HashMap<String, List<String>>();
        for (var lesson : schedule.getLessons()) {
            String periodId = lesson.getPeriod().id();
            collision(hard, KernelCatalog.TEACHER_PERIOD.id(), teacherPeriods,
                    lesson.getTeacherId() + "\u0000" + periodId, lesson.getId());
            collision(hard, KernelCatalog.COHORT_PERIOD.id(), cohortPeriods,
                    lesson.getCohortId() + "\u0000" + periodId, lesson.getId());
            collision(hard, KernelCatalog.ROOM_PERIOD.id(), roomPeriods,
                    lesson.getRoom().id() + "\u0000" + periodId, lesson.getId());

            incrementIf(hard, KernelCatalog.TEACHER_AVAILABILITY.id(),
                    !lesson.getTeacherAvailablePeriodIds().contains(periodId));
            incrementIf(hard, KernelCatalog.COHORT_AVAILABILITY.id(),
                    !lesson.getCohortAvailablePeriodIds().contains(periodId));
            incrementIf(hard, KernelCatalog.ROOM_AVAILABILITY.id(),
                    !lesson.getRoom().availablePeriodIds().contains(periodId));
            incrementIf(hard, KernelCatalog.ROOM_CAPACITY.id(), lesson.getRoom().capacity() < lesson.getCohortSize());
            incrementIf(hard, KernelCatalog.ROOM_CAPABILITY.id(),
                    !lesson.getRoom().capabilityIds().containsAll(lesson.getRequiredRoomCapabilityIds()));
            incrementIf(hard, KernelCatalog.PERIOD_LOCK.id(),
                    lesson.getPeriodLock() != null && !lesson.getPeriodLock().equals(periodId));
            incrementIf(hard, KernelCatalog.ROOM_LOCK.id(),
                    lesson.getRoomLock() != null && !lesson.getRoomLock().equals(lesson.getRoom().id()));
            incrementIf(hard, KernelCatalog.ROOM_ASSIGNMENT.id(),
                    !lesson.roomAssignmentAllows(lesson.getRoom().id()));
            incrementIf(hard, KernelCatalog.COHORT_HOME_ROOM.id(), SchoolConstraintProvider.outsideHomeRoom(lesson));
            incrementIf(hard, KernelCatalog.RESERVED_PERIOD.id(),
                    SchoolConstraintProvider.usesForbiddenReservedPeriod(lesson));

            if (lesson.getBaselinePeriodId() != null) {
                boolean forcedPeriod = lesson.getPeriodLock() != null
                        && !lesson.getPeriodLock().equals(lesson.getBaselinePeriodId());
                boolean forcedRoom = (lesson.getRoomLock() != null
                        && !lesson.getRoomLock().equals(lesson.getBaselineRoomId()))
                        || !lesson.roomAssignmentAllows(lesson.getBaselineRoomId());
                if (!lesson.getBaselinePeriodId().equals(periodId)) {
                    if (!forcedPeriod) {
                        periodMoves++;
                    }
                } else if (!lesson.getBaselineRoomId().equals(lesson.getRoom().id()) && !forcedRoom) {
                    roomOnlyMoves++;
                }
            }

            soft.compute(KernelCatalog.UNDESIRABLE_PERIOD.id(),
                    (key, count) -> count + SchoolConstraintProvider.undesirableMatchCount(lesson));
            incrementIf(soft, KernelCatalog.NON_PREFERRED_ROOM.id(),
                    !lesson.getPreferredRoomIds().isEmpty()
                            && !lesson.getPreferredRoomIds().contains(lesson.getRoom().id()));
        }

        var teacherDays = new HashMap<String, List<PlanningLesson>>();
        var cohortDays = new HashMap<String, List<PlanningLesson>>();
        var cohorts = new HashMap<String, List<PlanningLesson>>();
        var seriesDays = new HashMap<String, Long>();
        for (var lesson : schedule.getLessons()) {
            String day = lesson.getPeriod().weekday().name();
            teacherDays.computeIfAbsent(lesson.getTeacherId() + "\u0000" + day, ignored -> new ArrayList<>())
                    .add(lesson);
            cohortDays.computeIfAbsent(lesson.getCohortId() + "\u0000" + day, ignored -> new ArrayList<>())
                    .add(lesson);
            cohorts.computeIfAbsent(lesson.getCohortId(), ignored -> new ArrayList<>()).add(lesson);
            if (lesson.getSeriesId() != null) {
                seriesDays.merge(lesson.getSeriesId() + "\u0000" + day, 1L, Long::sum);
            }
        }
        teacherDays.values().forEach(lessons -> soft.compute(
                KernelCatalog.TEACHER_GAP.id(),
                (key, count) -> count + SchoolConstraintProvider.countTeacherGaps(lessons)));
        cohortDays.values().forEach(lessons -> hard.compute(
                KernelCatalog.COHORT_DAILY_GAPS.id(),
                (key, count) -> count + SchoolConstraintProvider.countCohortExcessGaps(lessons)));
        cohortDays.values().forEach(lessons -> hard.compute(
                KernelCatalog.COHORT_LATEST_START.id(),
                (key, count) -> count + SchoolConstraintProvider.countCohortLatestStartViolation(lessons)));
        cohortDays.values().forEach(lessons -> hard.compute(
                KernelCatalog.SUBJECT_DAY_EDGE.id(),
                (key, count) -> count + SchoolConstraintProvider.dayEdgeViolations(lessons).size()));
        cohortDays.values().forEach(lessons -> hard.compute(
                KernelCatalog.SUBJECT_DAILY_LIMIT.id(),
                (key, count) -> count + sum(SchoolConstraintProvider.subjectDailyExcess(lessons))));
        cohorts.values().forEach(lessons -> hard.compute(
                KernelCatalog.SUBJECT_RESERVED_LIMIT.id(),
                (key, count) -> count + sum(SchoolConstraintProvider.subjectReservedExcess(lessons))));
        cohorts.values().forEach(lessons -> hard.compute(
                KernelCatalog.COHORT_DAILY_SPREAD.id(),
                (key, count) -> count + SchoolConstraintProvider.countCohortSpreadExcess(lessons)));
        cohortDays.values().forEach(lessons -> soft.compute(
                KernelCatalog.COHORT_GAP.id(),
                (key, count) -> count + SchoolConstraintProvider.countCohortGaps(lessons)));
        cohortDays.values().forEach(lessons -> soft.compute(
                KernelCatalog.COHORT_LATE_START.id(),
                (key, count) -> count + SchoolConstraintProvider.countCohortLateStart(lessons)));
        cohorts.values().forEach(lessons -> soft.compute(
                KernelCatalog.COHORT_WEEK_BALANCE.id(),
                (key, count) -> count + SchoolConstraintProvider.countCohortWeekImbalance(lessons)));
        seriesDays.values().forEach(count -> soft.compute(
                KernelCatalog.SERIES_SAME_DAY.id(), (key, total) -> total + Math.max(0, count - 1)));

        long ordinaryPenalty = 0;
        for (String constraintId : KernelCatalog.softConstraintIds()) {
            ordinaryPenalty = Math.addExact(
                    ordinaryPenalty,
                    Math.multiplyExact(soft.get(constraintId), weights.getOrDefault(constraintId, 0L)));
        }
        boolean feasible = hard.values().stream().allMatch(count -> count == 0);
        return new Evaluation(
                true, feasible, Map.copyOf(hard), Map.copyOf(soft), periodMoves, roomOnlyMoves, ordinaryPenalty);
    }

    private static LinkedHashMap<String, Long> zeroCounts(List<String> ids) {
        var result = new LinkedHashMap<String, Long>();
        ids.forEach(id -> result.put(id, 0L));
        return result;
    }

    private static void collision(
            Map<String, Long> counts,
            String constraintId,
            Map<String, List<String>> owners,
            String key,
            String lessonId) {
        var lessons = owners.computeIfAbsent(key, ignored -> new ArrayList<>());
        counts.compute(constraintId, (ignored, count) -> count + lessons.size());
        lessons.add(lessonId);
    }

    private static long sum(Map<String, Long> excess) {
        return excess.values().stream().mapToLong(Long::longValue).sum();
    }

    private static void incrementIf(Map<String, Long> counts, String id, boolean matches) {
        if (matches) {
            counts.compute(id, (ignored, count) -> count + 1);
        }
    }
}
