package org.schoolkernel.solver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
        // Reserved periods widen the search only for schools with a subject that may use them.
        boolean reservedInRange = definition.subjects().stream()
                .anyMatch(SchoolDefinition.Subject::reservedPeriodsAllowed);
        var periods = definition.periods().stream()
                .filter(period -> reservedInRange || !definition.reservedPeriodIds().contains(period.id()))
                .map(period -> new PeriodValue(period.id(), period.weekday(), period.order(),
                        definition.reservedPeriodIds().contains(period.id())))
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
        Map<String, SchoolDefinition.Subject> subjects = definition.subjects().stream()
                .collect(Collectors.toMap(SchoolDefinition.Subject::id, Function.identity()));
        Map<String, SchoolDefinition.Room> definedRooms = definition.rooms().stream()
                .collect(Collectors.toMap(SchoolDefinition.Room::id, Function.identity()));
        var lessons = definition.lessons().stream().map(lesson -> {
            var teacher = teachers.get(lesson.teacherId());
            var cohort = cohorts.get(lesson.cohortId());
            var baseline = baselineAssignments.get(lesson.id());
            var planningLesson = new PlanningLesson(
                    lesson.id(), lesson.subjectId(), lesson.cohortId(), cohort.size(), lesson.teacherId(),
                    lesson.seriesId(), teacher.availablePeriodIds(), teacher.undesirablePeriodIds(),
                    cohort.availablePeriodIds(), cohort.undesirablePeriodIds(), lesson.undesirablePeriodIds(),
                    lesson.requiredRoomCapabilityIds(), preferredRoomIds(lesson, cohort, definedRooms),
                    lesson.periodLock(), lesson.roomLock(),
                    baseline == null ? null : baseline.periodId(),
                    baseline == null ? null : baseline.roomId(),
                    periods, cohort.maxDailyLessonSpread(), cohort.maxDailyGaps(),
                    PlacementRules.of(cohort, subjects.get(lesson.subjectId())));
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
                        entry -> BendableScore.ofSoft(2, 3, 2, entry.getValue())));
        return new SchoolSchedule(periods, rooms, lessons, ConstraintWeightOverrides.of(overrides));
    }

    /**
     * A lesson without its own room preference prefers its cohort's home room when that room could host it, so
     * music and sports lessons are not penalized for leaving the classroom.
     */
    private static Set<String> preferredRoomIds(
            SchoolDefinition.Lesson lesson, SchoolDefinition.Cohort cohort, Map<String, SchoolDefinition.Room> rooms) {
        if (!lesson.preferredRoomIds().isEmpty() || cohort.homeRoomId() == null) {
            return lesson.preferredRoomIds();
        }
        var home = rooms.get(cohort.homeRoomId());
        boolean fits = home.capacity() >= cohort.size()
                && home.capabilityIds().containsAll(lesson.requiredRoomCapabilityIds());
        return fits ? Set.of(home.id()) : lesson.preferredRoomIds();
    }

    private static boolean canKeepBaseline(PlanningLesson lesson, PeriodValue period, RoomValue room) {
        return period != null && room != null
                && (!period.reserved() || lesson.getPlacementRules().reservedPeriodsAllowed())
                && lesson.getTeacherAvailablePeriodIds().contains(period.id())
                && lesson.getCohortAvailablePeriodIds().contains(period.id())
                && room.availablePeriodIds().contains(period.id())
                && room.capacity() >= lesson.getCohortSize()
                && room.capabilityIds().containsAll(lesson.getRequiredRoomCapabilityIds())
                && (lesson.getPeriodLock() == null || lesson.getPeriodLock().equals(period.id()))
                && (lesson.getRoomLock() == null || lesson.getRoomLock().equals(room.id()))
                && (lesson.getPlacementRules().homeRoomId() == null
                        || lesson.getPlacementRules().homeRoomId().equals(room.id()));
    }
}
