package org.schoolkernel.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.schoolkernel.domain.SchoolDefinition;

public final class HardConstraintDiagnostics {
    private HardConstraintDiagnostics() {}

    public static List<ConstraintDiagnostic> from(
            SchoolSchedule schedule,
            ScheduleEvaluator.Evaluation evaluation) {
        var examples = new LinkedHashMap<String, List<List<String>>>();
        SchoolDefinition.HARD_CONSTRAINT_IDS.forEach(id -> examples.put(id, new ArrayList<>()));
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
                    examples.get("hard.teacher-period").add(List.of(
                            first.getId(), second.getId(), first.getTeacherId(), periodId));
                }
                if (first.getCohortId().equals(second.getCohortId())) {
                    examples.get("hard.cohort-period").add(List.of(
                            first.getId(), second.getId(), first.getCohortId(), periodId));
                }
                if (first.getRoom().equals(second.getRoom())) {
                    examples.get("hard.room-period").add(List.of(
                            first.getId(), second.getId(), first.getRoom().id(), periodId));
                }
            }
        }

        for (PlanningLesson lesson : lessons) {
            String periodId = lesson.getPeriod().id();
            String roomId = lesson.getRoom().id();
            addIf(examples, "hard.teacher-availability",
                    !lesson.getTeacherAvailablePeriodIds().contains(periodId),
                    lesson.getId(), lesson.getTeacherId(), periodId);
            addIf(examples, "hard.cohort-availability",
                    !lesson.getCohortAvailablePeriodIds().contains(periodId),
                    lesson.getId(), lesson.getCohortId(), periodId);
            addIf(examples, "hard.room-availability",
                    !lesson.getRoom().availablePeriodIds().contains(periodId),
                    lesson.getId(), roomId, periodId);
            addIf(examples, "hard.room-capacity",
                    lesson.getRoom().capacity() < lesson.getCohortSize(),
                    lesson.getId(), lesson.getCohortId(), roomId);
            addIf(examples, "hard.room-capability",
                    !lesson.getRoom().capabilityIds().containsAll(lesson.getRequiredRoomCapabilityIds()),
                    lesson.getId(), roomId);
            addIf(examples, "hard.period-lock",
                    lesson.getPeriodLock() != null && !lesson.getPeriodLock().equals(periodId),
                    lesson.getId(), periodId, lesson.getPeriodLock());
            addIf(examples, "hard.room-lock",
                    lesson.getRoomLock() != null && !lesson.getRoomLock().equals(roomId),
                    lesson.getId(), roomId, lesson.getRoomLock());
        }

        return SchoolDefinition.HARD_CONSTRAINT_IDS.stream()
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
