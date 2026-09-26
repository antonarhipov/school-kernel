package org.schoolkernel.solver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.schoolkernel.domain.KernelCatalog;
import org.schoolkernel.domain.SchoolDefinition;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.BendableScore;

public final class PlanningMapper {
    public record BaselineAssignment(String periodId, String roomId) {}

    public SchoolSchedule toPlanningProblem(SchoolDefinition definition) {
        return toPlanningProblem(definition, Map.of());
    }

    public SchoolSchedule toPlanningProblem(
            SchoolDefinition definition,
            Map<String, BaselineAssignment> baselineAssignments) {
        var periods = definition.periods().stream()
                .map(period -> new PeriodValue(period.id(), period.weekday(), period.order()))
                .toList();
        var rooms = definition.rooms().stream()
                .map(room -> new RoomValue(
                        room.id(), room.capacity(), room.capabilityIds(), room.availablePeriodIds()))
                .toList();
        var periodsById = periods.stream().collect(Collectors.toMap(PeriodValue::id, Function.identity()));
        var roomsById = rooms.stream().collect(Collectors.toMap(RoomValue::id, Function.identity()));

        Map<String, SchoolDefinition.Teacher> teachers = definition.teachers().stream()
                .collect(Collectors.toMap(SchoolDefinition.Teacher::id, Function.identity()));
        Map<String, SchoolDefinition.Cohort> cohorts = definition.cohorts().stream()
                .collect(Collectors.toMap(SchoolDefinition.Cohort::id, Function.identity()));
        var lessons = definition.lessons().stream().map(lesson -> {
            var teacher = teachers.get(lesson.teacherId());
            var cohort = cohorts.get(lesson.cohortId());
            var baseline = baselineAssignments.get(lesson.id());
            var planningLesson = new PlanningLesson(
                    lesson.id(), lesson.subjectId(), lesson.cohortId(), cohort.size(), lesson.teacherId(),
                    lesson.seriesId(), teacher.availablePeriodIds(), teacher.undesirablePeriodIds(),
                    cohort.availablePeriodIds(), cohort.undesirablePeriodIds(), lesson.undesirablePeriodIds(),
                    lesson.requiredRoomCapabilityIds(), lesson.preferredRoomIds(), lesson.periodLock(), lesson.roomLock(),
                    baseline == null ? null : baseline.periodId(),
                    baseline == null ? null : baseline.roomId(),
                    periods);
            if (baseline != null) {
                PeriodValue baselinePeriod = periodsById.get(baseline.periodId());
                RoomValue baselineRoom = roomsById.get(baseline.roomId());
                if (canKeepBaseline(planningLesson, baselinePeriod, baselineRoom)) {
                    planningLesson.setPeriod(baselinePeriod);
                    planningLesson.setRoom(baselineRoom);
                }
            }
            return planningLesson;
        }).toList();

        var effectiveWeights = new HashMap<String, Long>();
        KernelCatalog.softConstraintIds().forEach(id -> effectiveWeights.put(id, 0L));
        effectiveWeights.putAll(definition.softWeights());
        var overrides = effectiveWeights.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> BendableScore.ofSoft(1, 3, 2, entry.getValue())));
        return new SchoolSchedule(periods, rooms, lessons, ConstraintWeightOverrides.of(overrides));
    }

    private static boolean canKeepBaseline(PlanningLesson lesson, PeriodValue period, RoomValue room) {
        return period != null && room != null
                && lesson.getTeacherAvailablePeriodIds().contains(period.id())
                && lesson.getCohortAvailablePeriodIds().contains(period.id())
                && room.availablePeriodIds().contains(period.id())
                && room.capacity() >= lesson.getCohortSize()
                && room.capabilityIds().containsAll(lesson.getRequiredRoomCapabilityIds())
                && (lesson.getPeriodLock() == null || lesson.getPeriodLock().equals(period.id()))
                && (lesson.getRoomLock() == null || lesson.getRoomLock().equals(room.id()));
    }
}
