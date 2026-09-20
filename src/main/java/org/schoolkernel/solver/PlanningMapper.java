package org.schoolkernel.solver;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        Map<String, SchoolDefinition.Teacher> teachers = definition.teachers().stream()
                .collect(Collectors.toMap(SchoolDefinition.Teacher::id, Function.identity()));
        Map<String, SchoolDefinition.Cohort> cohorts = definition.cohorts().stream()
                .collect(Collectors.toMap(SchoolDefinition.Cohort::id, Function.identity()));
        var lessons = definition.lessons().stream().map(lesson -> {
            var teacher = teachers.get(lesson.teacherId());
            var cohort = cohorts.get(lesson.cohortId());
            var baseline = baselineAssignments.get(lesson.id());
            return new PlanningLesson(
                    lesson.id(), lesson.subjectId(), lesson.cohortId(), cohort.size(), lesson.teacherId(),
                    lesson.seriesId(), teacher.availablePeriodIds(), teacher.undesirablePeriodIds(),
                    cohort.availablePeriodIds(), cohort.undesirablePeriodIds(), lesson.undesirablePeriodIds(),
                    lesson.requiredRoomCapabilityIds(), lesson.preferredRoomIds(), lesson.periodLock(), lesson.roomLock(),
                    baseline == null ? null : baseline.periodId(),
                    baseline == null ? null : baseline.roomId(),
                    periods);
        }).toList();

        var overrides = definition.softWeights().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> BendableScore.ofSoft(1, 3, 2, entry.getValue())));
        return new SchoolSchedule(periods, rooms, lessons, ConstraintWeightOverrides.of(overrides));
    }
}
