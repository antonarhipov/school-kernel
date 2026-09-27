package org.schoolkernel.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.domain.KernelCatalog;

class RoomAssignmentConstraintTest {
    private static final PeriodValue PERIOD = new PeriodValue("p1", DayOfWeek.MONDAY, 1);
    private static final RoomValue ORIGINAL = new RoomValue("original", 30, Set.of(), Set.of("p1"));
    private static final RoomValue REQUIRED = new RoomValue("required", 30, Set.of(), Set.of("p1"));
    private final ConstraintVerifier<SchoolConstraintProvider, SchoolSchedule> verifier =
            ConstraintVerifier.build(new SchoolConstraintProvider(), SchoolSchedule.class, PlanningLesson.class);

    @Test
    @DisplayName("Room assignment UC-1 G1/RULE-3: solver, evaluator and diagnostics count the same hard violation")
    void policyViolationIsOnePhysicalHardMatch() {
        PlanningLesson lesson = lesson();
        lesson.setRoomAssignment(Set.of("required"), List.of("policy-1"));
        lesson.setRoom(ORIGINAL);
        verifier.verifyThat(SchoolConstraintProvider::roomAssignment).given(lesson).penalizesBy(1);
        var wrong = new ScheduleEvaluator().evaluate(schedule(lesson), Map.of());
        assertFalse(wrong.feasible());
        assertEquals(1, wrong.hardMatchCounts().get(KernelCatalog.ROOM_ASSIGNMENT.id()));
        assertEquals(List.of(List.of("lesson", "original", "policy-1")),
                HardConstraintDiagnostics.from(schedule(lesson), wrong).stream()
                        .filter(value -> value.constraintId().equals(KernelCatalog.ROOM_ASSIGNMENT.id()))
                        .findFirst().orElseThrow().examples());

        lesson.setRoom(REQUIRED);
        verifier.verifyThat(SchoolConstraintProvider::roomAssignment).given(lesson).hasNoImpact();
        var allowed = new ScheduleEvaluator().evaluate(schedule(lesson), Map.of());
        assertTrue(allowed.feasible());
        assertEquals(0, allowed.hardMatchCounts().get(KernelCatalog.ROOM_ASSIGNMENT.id()));
    }

    @Test
    @DisplayName("Room assignment UC-1 ext 1a/RULE-4: only exclusion of the accepted room forces a room move")
    void policyDistinguishesForcedAndOrdinaryRoomMoves() {
        PlanningLesson lesson = lesson();
        lesson.setRoom(REQUIRED);
        lesson.setRoomAssignment(Set.of("required"), List.of("policy-1"));
        verifier.verifyThat(SchoolConstraintProvider::roomOnlyMove).given(lesson).hasNoImpact();
        assertEquals(0, new ScheduleEvaluator().evaluate(schedule(lesson), Map.of()).roomOnlyMoves());

        lesson.setRoomAssignment(Set.of("original", "required"), List.of("policy-1"));
        verifier.verifyThat(SchoolConstraintProvider::roomOnlyMove).given(lesson).penalizesBy(1);
        assertEquals(1, new ScheduleEvaluator().evaluate(schedule(lesson), Map.of()).roomOnlyMoves());
    }

    private static PlanningLesson lesson() {
        var lesson = new PlanningLesson("lesson", "subject", "cohort", 20, "teacher", null,
                Set.of("p1"), Set.of(), Set.of("p1"), Set.of(), Set.of(), Set.of(), Set.of(),
                null, null, "p1", "original", List.of(PERIOD));
        lesson.setPeriod(PERIOD);
        return lesson;
    }

    private static SchoolSchedule schedule(PlanningLesson lesson) {
        return new SchoolSchedule(List.of(PERIOD), List.of(ORIGINAL, REQUIRED), List.of(lesson),
                ConstraintWeightOverrides.none());
    }
}
