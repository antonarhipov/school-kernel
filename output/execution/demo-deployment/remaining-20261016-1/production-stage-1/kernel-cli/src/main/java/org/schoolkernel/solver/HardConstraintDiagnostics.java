package org.schoolkernel.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.schoolkernel.domain.KernelCatalog;

public final class HardConstraintDiagnostics {
    private HardConstraintDiagnostics() {}

    public static List<ConstraintDiagnostic> from(
            SchoolSchedule schedule,
            ScheduleEvaluator.Evaluation evaluation) {
        var examples = new LinkedHashMap<String, List<List<String>>>();
        KernelCatalog.hardConstraintIds().forEach(id -> examples.put(id, new ArrayList<>()));
        var lessons = schedule.getLessons().stream()
                .filter(lesson -> lesson.getPeriod() != null && lesson.getRoom() != null)
                .sorted(Comparator.comparing(PlanningLesson::getId))
                .toList();

        for (int left = 0; left < lessons.size(); left++) {
            PlanningLesson first = lessons.get(left);
            for (int right = left + 1; right < lessons.size(); right++) {
                PlanningLesson second = lessons.get(right);
                if (!first.getPeriod().equals(second.getPeriod())) {
                    continue;
                }
                String periodId = first.getPeriod().id();
                if (first.getTeacherId().equals(second.getTeacherId())) {
                    examples.get(KernelCatalog.TEACHER_PERIOD.id()).add(List.of(
                            first.getId(), second.getId(), first.getTeacherId(), periodId));
                }
                if (first.getCohortId().equals(second.getCohortId())) {
                    examples.get(KernelCatalog.COHORT_PERIOD.id()).add(List.of(
                            first.getId(), second.getId(), first.getCohortId(), periodId));
                }
                if (first.getRoom().equals(second.getRoom())) {
                    examples.get(KernelCatalog.ROOM_PERIOD.id()).add(List.of(
                            first.getId(), second.getId(), first.getRoom().id(), periodId));
                }
            }
        }

        for (PlanningLesson lesson : lessons) {
            String periodId = lesson.getPeriod().id();
            String roomId = lesson.getRoom().id();
            addIf(examples, KernelCatalog.TEACHER_AVAILABILITY.id(),
                    !lesson.getTeacherAvailablePeriodIds().contains(periodId),
                    lesson.getId(), lesson.getTeacherId(), periodId);
            addIf(examples, KernelCatalog.COHORT_AVAILABILITY.id(),
                    !lesson.getCohortAvailablePeriodIds().contains(periodId),
                    lesson.getId(), lesson.getCohortId(), periodId);
            addIf(examples, KernelCatalog.ROOM_AVAILABILITY.id(),
                    !lesson.getRoom().availablePeriodIds().contains(periodId),
                    lesson.getId(), roomId, periodId);
            addIf(examples, KernelCatalog.ROOM_CAPACITY.id(),
                    lesson.getRoom().capacity() < lesson.getCohortSize(),
                    lesson.getId(), lesson.getCohortId(), roomId);
            addIf(examples, KernelCatalog.ROOM_CAPABILITY.id(),
                    !lesson.getRoom().capabilityIds().containsAll(lesson.getRequiredRoomCapabilityIds()),
                    lesson.getId(), roomId);
            addIf(examples, KernelCatalog.PERIOD_LOCK.id(),
                    lesson.getPeriodLock() != null && !lesson.getPeriodLock().equals(periodId),
                    lesson.getId(), periodId, lesson.getPeriodLock());
            addIf(examples, KernelCatalog.ROOM_LOCK.id(),
                    lesson.getRoomLock() != null && !lesson.getRoomLock().equals(roomId),
                    lesson.getId(), roomId, lesson.getRoomLock());
            if (!lesson.roomAssignmentAllows(roomId)) {
                var ids = new ArrayList<String>(List.of(lesson.getId(), roomId));
                ids.addAll(lesson.getRoomAssignmentPolicyIds());
                examples.get(KernelCatalog.ROOM_ASSIGNMENT.id()).add(List.copyOf(ids));
            }
            addIf(examples, KernelCatalog.COHORT_HOME_ROOM.id(),
                    SchoolConstraintProvider.outsideHomeRoom(lesson),
                    lesson.getId(), roomId, lesson.getPlacementRules().homeRoomId());
            addIf(examples, KernelCatalog.RESERVED_PERIOD.id(),
                    SchoolConstraintProvider.usesForbiddenReservedPeriod(lesson),
                    lesson.getId(), periodId);
        }

        var cohortDays = new LinkedHashMap<String, List<PlanningLesson>>();
        lessons.forEach(lesson -> cohortDays.computeIfAbsent(
                lesson.getCohortId() + "\u0000" + lesson.getPeriod().weekday(), ignored -> new ArrayList<>())
                .add(lesson));
        for (List<PlanningLesson> cohortDay : cohortDays.values()) {
            if (SchoolConstraintProvider.countCohortExcessGaps(cohortDay) > 0) {
                var entityIds = new ArrayList<String>();
                entityIds.add(cohortDay.getFirst().getCohortId());
                SchoolConstraintProvider.cohortGapPeriods(cohortDay).forEach(period -> entityIds.add(period.id()));
                examples.get(KernelCatalog.COHORT_DAILY_GAPS.id()).add(List.copyOf(entityIds));
            }
            String cohortId = cohortDay.getFirst().getCohortId();
            String weekday = cohortDay.getFirst().getPeriod().weekday().name();
            if (SchoolConstraintProvider.countCohortLatestStartViolation(cohortDay) > 0) {
                String firstPeriodId = cohortDay.stream()
                        .min(Comparator.comparingInt(lesson -> lesson.getPeriod().order()))
                        .orElseThrow().getPeriod().id();
                examples.get(KernelCatalog.COHORT_LATEST_START.id()).add(List.of(cohortId, weekday, firstPeriodId));
            }
            SchoolConstraintProvider.dayEdgeViolations(cohortDay).forEach(lesson -> examples
                    .get(KernelCatalog.SUBJECT_DAY_EDGE.id())
                    .add(List.of(lesson.getId(), cohortId, lesson.getPeriod().id())));
            SchoolConstraintProvider.subjectDailyExcess(cohortDay).keySet().forEach(subjectId -> examples
                    .get(KernelCatalog.SUBJECT_DAILY_LIMIT.id()).add(List.of(cohortId, subjectId, weekday)));
        }

        var cohortWeeks = new LinkedHashMap<String, List<PlanningLesson>>();
        lessons.forEach(lesson -> cohortWeeks.computeIfAbsent(lesson.getCohortId(), ignored -> new ArrayList<>())
                .add(lesson));
        for (List<PlanningLesson> cohortWeek : cohortWeeks.values()) {
            SchoolConstraintProvider.subjectReservedExcess(cohortWeek).keySet().forEach(subjectId -> {
                var entityIds = new ArrayList<String>(List.of(cohortWeek.getFirst().getCohortId(), subjectId));
                cohortWeek.stream()
                        .filter(lesson -> lesson.getSubjectId().equals(subjectId) && lesson.getPeriod().reserved())
                        .forEach(lesson -> entityIds.add(lesson.getPeriod().id()));
                examples.get(KernelCatalog.SUBJECT_RESERVED_LIMIT.id()).add(List.copyOf(entityIds));
            });
            if (SchoolConstraintProvider.countCohortSpreadExcess(cohortWeek) > 0) {
                var entityIds = new ArrayList<String>(List.of(cohortWeek.getFirst().getCohortId()));
                SchoolConstraintProvider.dailySpreadExtremes(cohortWeek).forEach(day -> entityIds.add(day.name()));
                examples.get(KernelCatalog.COHORT_DAILY_SPREAD.id()).add(List.copyOf(entityIds));
            }
        }

        return KernelCatalog.hardConstraintIds().stream()
                .filter(id -> evaluation.hardMatchCounts().getOrDefault(id, 0L) > 0)
                .map(id -> new ConstraintDiagnostic(
                        id,
                        evaluation.hardMatchCounts().get(id),
                        examples.get(id).stream().limit(1_000).toList()))
                .toList();
    }

    private static void addIf(
            Map<String, List<List<String>>> examples,
            String constraintId,
            boolean match,
            String... entityIds) {
        if (match) {
            examples.get(constraintId).add(List.of(entityIds));
        }
    }
}
