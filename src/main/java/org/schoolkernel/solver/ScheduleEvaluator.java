package org.schoolkernel.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.schoolkernel.domain.SchoolDefinition;

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
        var hard = zeroCounts(SchoolDefinition.HARD_CONSTRAINT_IDS);
        var soft = zeroCounts(SchoolDefinition.SOFT_CONSTRAINT_IDS);
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
            collision(hard, "hard.teacher-period", teacherPeriods,
                    lesson.getTeacherId() + "\u0000" + periodId, lesson.getId());
            collision(hard, "hard.cohort-period", cohortPeriods,
                    lesson.getCohortId() + "\u0000" + periodId, lesson.getId());
            collision(hard, "hard.room-period", roomPeriods,
                    lesson.getRoom().id() + "\u0000" + periodId, lesson.getId());

            incrementIf(hard, "hard.teacher-availability",
                    !lesson.getTeacherAvailablePeriodIds().contains(periodId));
            incrementIf(hard, "hard.cohort-availability",
                    !lesson.getCohortAvailablePeriodIds().contains(periodId));
            incrementIf(hard, "hard.room-availability",
                    !lesson.getRoom().availablePeriodIds().contains(periodId));
            incrementIf(hard, "hard.room-capacity", lesson.getRoom().capacity() < lesson.getCohortSize());
            incrementIf(hard, "hard.room-capability",
                    !lesson.getRoom().capabilityIds().containsAll(lesson.getRequiredRoomCapabilityIds()));
            incrementIf(hard, "hard.period-lock",
                    lesson.getPeriodLock() != null && !lesson.getPeriodLock().equals(periodId));
            incrementIf(hard, "hard.room-lock",
                    lesson.getRoomLock() != null && !lesson.getRoomLock().equals(lesson.getRoom().id()));

            if (lesson.getBaselinePeriodId() != null) {
                boolean forcedPeriod = lesson.getPeriodLock() != null
                        && !lesson.getPeriodLock().equals(lesson.getBaselinePeriodId());
                boolean forcedRoom = lesson.getRoomLock() != null
                        && !lesson.getRoomLock().equals(lesson.getBaselineRoomId());
                if (!lesson.getBaselinePeriodId().equals(periodId)) {
                    if (!forcedPeriod) {
                        periodMoves++;
                    }
                } else if (!lesson.getBaselineRoomId().equals(lesson.getRoom().id()) && !forcedRoom) {
                    roomOnlyMoves++;
                }
            }

            soft.compute("soft.undesirable-period",
                    (key, count) -> count + SchoolConstraintProvider.undesirableMatchCount(lesson));
            incrementIf(soft, "soft.non-preferred-room",
                    !lesson.getPreferredRoomIds().isEmpty()
                            && !lesson.getPreferredRoomIds().contains(lesson.getRoom().id()));
        }

        var teacherDays = new HashMap<String, List<PlanningLesson>>();
        var seriesDays = new HashMap<String, Long>();
        for (var lesson : schedule.getLessons()) {
            String day = lesson.getPeriod().weekday().name();
            teacherDays.computeIfAbsent(lesson.getTeacherId() + "\u0000" + day, ignored -> new ArrayList<>())
                    .add(lesson);
            if (lesson.getSeriesId() != null) {
                seriesDays.merge(lesson.getSeriesId() + "\u0000" + day, 1L, Long::sum);
            }
        }
        teacherDays.values().forEach(lessons -> soft.compute(
                "soft.teacher-gap", (key, count) -> count + SchoolConstraintProvider.countTeacherGaps(lessons)));
        seriesDays.values().forEach(count -> soft.compute(
                "soft.series-same-day", (key, total) -> total + Math.max(0, count - 1)));

        long ordinaryPenalty = 0;
        for (String constraintId : SchoolDefinition.SOFT_CONSTRAINT_IDS) {
            ordinaryPenalty = Math.addExact(
                    ordinaryPenalty,
                    Math.multiplyExact(soft.get(constraintId), weights.get(constraintId)));
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

    private static void incrementIf(Map<String, Long> counts, String id, boolean matches) {
        if (matches) {
            counts.compute(id, (ignored, count) -> count + 1);
        }
    }
}
