package org.schoolkernel.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.domain.KernelCatalog;

class ReplanningSolverTest {
    private static final SolverAdapter.ExecutionControls CONTROLS =
            new SolverAdapter.ExecutionControls(null, 100, 0);

    @Test
    @DisplayName("UC-2 G5: preserving a period dominates room and preference improvements")
    void periodStabilityDominatesRoomAndPreferences() {
        SchoolDefinition definition = definition(
                List.of(
                        room("r1", Set.of("p2")),
                        room("r2", Set.of("p1"))),
                List.of(period("p1", 1), period("p2", 2)),
                Set.of("r1"));

        SolverAdapter.SolveResult result = new SolverAdapter().solve(
                definition, CONTROLS,
                Map.of("lesson", new PlanningMapper.BaselineAssignment("p1", "r1")));
        PlanningLesson lesson = result.schedule().getLessons().get(0);

        assertEquals("p1", lesson.getPeriod().id());
        assertEquals("r2", lesson.getRoom().id());
        assertEquals(0, result.evaluation().periodMoves());
        assertEquals(1, result.evaluation().roomOnlyMoves());
        assertEquals(1, result.evaluation().ordinaryPreferencePenalty());
    }

    @Test
    @DisplayName("UC-2 G5: preserving a room dominates ordinary preference improvements")
    void roomStabilityDominatesPreferences() {
        SchoolDefinition definition = definition(
                List.of(
                        room("r1", Set.of("p1")),
                        room("r2", Set.of("p1"))),
                List.of(period("p1", 1)),
                Set.of("r2"));

        SolverAdapter.SolveResult result = new SolverAdapter().solve(
                definition, CONTROLS,
                Map.of("lesson", new PlanningMapper.BaselineAssignment("p1", "r1")));
        PlanningLesson lesson = result.schedule().getLessons().get(0);

        assertEquals("p1", lesson.getPeriod().id());
        assertEquals("r1", lesson.getRoom().id());
        assertEquals(0, result.evaluation().periodMoves());
        assertEquals(0, result.evaluation().roomOnlyMoves());
        assertEquals(1, result.evaluation().ordinaryPreferencePenalty());
    }

    private static SchoolDefinition definition(
            List<SchoolDefinition.Room> rooms,
            List<SchoolDefinition.Period> periods,
            Set<String> preferredRooms) {
        Set<String> periodIds = periods.stream().map(SchoolDefinition.Period::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new SchoolDefinition(
                1,
                1,
                "school",
                "School",
                "sha256:" + "0".repeat(64),
                List.of(new SchoolDefinition.Subject("subject", "Subject")),
                List.of(new SchoolDefinition.Teacher(
                        "teacher", "Teacher", Set.of("subject"), periodIds, Set.of())),
                List.of(new SchoolDefinition.Cohort("cohort", "Cohort", 20, periodIds, Set.of())),
                rooms,
                periods,
                List.of(new SchoolDefinition.Lesson(
                        "lesson", "Lesson", "subject", "cohort", "teacher", null,
                        Set.of(), preferredRooms, Set.of(), null, null)),
                KernelCatalog.softConstraintIds().stream()
                        .collect(java.util.stream.Collectors.toUnmodifiableMap(id -> id, ignored -> 1L)));
    }

    private static SchoolDefinition.Room room(String id, Set<String> availablePeriods) {
        return new SchoolDefinition.Room(id, id, 30, Set.of(), availablePeriods);
    }

    private static SchoolDefinition.Period period(String id, int order) {
        return new SchoolDefinition.Period(id, id, DayOfWeek.MONDAY, order, null, null);
    }
}
