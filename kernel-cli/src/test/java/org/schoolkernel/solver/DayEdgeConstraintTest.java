package org.schoolkernel.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;

import org.junit.jupiter.api.Test;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.SchoolDefinitionDto;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.domain.KernelCatalog;
import org.schoolkernel.domain.SchoolDefinition;

import tools.jackson.databind.node.ObjectNode;

class DayEdgeConstraintTest {
    private static final PeriodValue M0 = new PeriodValue("m0", DayOfWeek.MONDAY, 1, true);
    private static final PeriodValue M1 = new PeriodValue("m1", DayOfWeek.MONDAY, 2);
    private static final PeriodValue M2 = new PeriodValue("m2", DayOfWeek.MONDAY, 3);
    private static final PeriodValue M3 = new PeriodValue("m3", DayOfWeek.MONDAY, 4);
    private static final PeriodValue M4 = new PeriodValue("m4", DayOfWeek.MONDAY, 5);
    private static final PeriodValue T0 = new PeriodValue("t0", DayOfWeek.TUESDAY, 1, true);
    private static final PeriodValue T1 = new PeriodValue("t1", DayOfWeek.TUESDAY, 2);
    private static final List<PeriodValue> PERIODS = List.of(M0, M1, M2, M3, M4, T0, T1);
    private static final Set<String> ALL = PERIODS.stream().map(PeriodValue::id).collect(Collectors.toSet());
    private static final PlacementRules EDGE = new PlacementRules(
            PlacementRules.UNLIMITED, 3, true, 1, true, 1);

    private final ConstraintVerifier<SchoolConstraintProvider, SchoolSchedule> verifier =
            ConstraintVerifier.build(new SchoolConstraintProvider(), SchoolSchedule.class, PlanningLesson.class);

    @Test
    void latestStartCountsRegularSlotsAndIgnoresUnboundedCohorts() {
        var bounded = startBound(2);
        var onTime = lesson("a", "subject", M2, bounded);
        verifier.verifyThat(SchoolConstraintProvider::cohortLatestStart).given(onTime).hasNoImpact();

        var reservedFirst = lesson("b", "subject", M0, bounded);
        var late = lesson("c", "subject", M3, bounded);
        verifier.verifyThat(SchoolConstraintProvider::cohortLatestStart).given(reservedFirst, late).hasNoImpact();

        verifier.verifyThat(SchoolConstraintProvider::cohortLatestStart).given(late).penalizesBy(1);
        var evaluation = evaluate(late);
        assertEquals(1, evaluation.hardMatchCounts().get(KernelCatalog.COHORT_LATEST_START.id()));
        assertFalse(evaluation.feasible());
        assertEquals(List.of(List.of("class", "MONDAY", "m3")),
                diagnostic(late, KernelCatalog.COHORT_LATEST_START.id()));

        var unbounded = lesson("d", "subject", M4, PlacementRules.NONE);
        verifier.verifyThat(SchoolConstraintProvider::cohortLatestStart).given(unbounded).hasNoImpact();
        assertTrue(evaluate(unbounded).feasible());
    }

    @Test
    void preferredStartReplacesTheCatalogThreeThresholdPerCohort() {
        var byDefault = lesson("a", "subject", M3, PlacementRules.NONE);
        verifier.verifyThat(SchoolConstraintProvider::cohortLateStart).given(byDefault).hasNoImpact();
        byDefault.setPeriod(M4);
        verifier.verifyThat(SchoolConstraintProvider::cohortLateStart).given(byDefault).penalizesBy(1);

        var preferFirst = lesson("b", "subject", M2, new PlacementRules(
                PlacementRules.UNLIMITED, 1, false, PlacementRules.UNLIMITED, false, PlacementRules.UNLIMITED));
        verifier.verifyThat(SchoolConstraintProvider::cohortLateStart).given(preferFirst).penalizesBy(1);
        assertEquals(1, evaluate(preferFirst).softMatchCounts().get(KernelCatalog.COHORT_LATE_START.id()));
        assertTrue(evaluate(preferFirst).feasible());
    }

    @Test
    void reservedPeriodMisuseIsPhysicalAndShapeRulesAreLevelOne() {
        var ordinary = lesson("a", "subject", M0, PlacementRules.NONE);
        verifier.verifyThat(SchoolConstraintProvider::reservedPeriod).given(ordinary).penalizesBy(1);
        assertEquals(List.of(List.of("a", "m0")), diagnostic(ordinary, KernelCatalog.RESERVED_PERIOD.id()));
        var permitted = lesson("b", "support", M0, EDGE);
        verifier.verifyThat(SchoolConstraintProvider::reservedPeriod).given(permitted).hasNoImpact();

        var first = lesson("c", "subject", M1, PlacementRules.NONE);
        var edge = lesson("d", "support", M2, EDGE);
        var last = lesson("e", "subject", M3, PlacementRules.NONE);
        BendableScore score = score(ordinary, first, edge, last);
        assertEquals(-1, score.hardScore(0));
        assertEquals(-1, score.hardScore(1));
    }

    @Test
    void edgeOnlyLessonsMayStartOrEndTheDayButNotSitInside() {
        var first = lesson("a", "subject", M1, PlacementRules.NONE);
        var edge = lesson("b", "support", M2, EDGE);
        var last = lesson("c", "subject", M3, PlacementRules.NONE);
        verifier.verifyThat(SchoolConstraintProvider::subjectDayEdge).given(first, edge, last).penalizesBy(1);
        assertEquals(1, evaluate(first, edge, last).hardMatchCounts().get(KernelCatalog.SUBJECT_DAY_EDGE.id()));
        assertEquals(List.of(List.of("b", "class", "m2")),
                diagnostic(List.of(first, edge, last), KernelCatalog.SUBJECT_DAY_EDGE.id()));

        edge.setPeriod(M4);
        verifier.verifyThat(SchoolConstraintProvider::subjectDayEdge).given(first, edge, last).hasNoImpact();
        edge.setPeriod(M0);
        verifier.verifyThat(SchoolConstraintProvider::subjectDayEdge).given(first, edge, last).hasNoImpact();
        edge.setPeriod(T1);
        verifier.verifyThat(SchoolConstraintProvider::subjectDayEdge).given(first, edge, last).hasNoImpact();
        assertTrue(evaluate(first, edge, last).hardMatchCounts().values().stream().allMatch(count -> count == 0));
    }

    @Test
    void dailyAndWeeklyReservedLimitsCountExcessPerCohort() {
        var monday = lesson("a", "support", M0, EDGE);
        var mondayAgain = lesson("b", "support", M4, EDGE);
        verifier.verifyThat(SchoolConstraintProvider::subjectDailyLimit).given(monday, mondayAgain).penalizesBy(1);
        assertEquals(List.of(List.of("class", "support", "MONDAY")),
                diagnostic(List.of(monday, mondayAgain), KernelCatalog.SUBJECT_DAILY_LIMIT.id()));

        var tuesday = lesson("c", "support", T0, EDGE);
        verifier.verifyThat(SchoolConstraintProvider::subjectDailyLimit).given(monday, tuesday).hasNoImpact();
        verifier.verifyThat(SchoolConstraintProvider::subjectReservedLimit).given(monday, tuesday).penalizesBy(1);
        var evaluation = evaluate(monday, tuesday);
        assertEquals(1, evaluation.hardMatchCounts().get(KernelCatalog.SUBJECT_RESERVED_LIMIT.id()));
        assertEquals(List.of(List.of("class", "support", "m0", "t0")),
                diagnostic(List.of(monday, tuesday), KernelCatalog.SUBJECT_RESERVED_LIMIT.id()));

        tuesday.setPeriod(T1);
        verifier.verifyThat(SchoolConstraintProvider::subjectReservedLimit).given(monday, tuesday).hasNoImpact();
    }

    @Test
    void anOccupiedReservedPeriodJoinsTheDayForGaps() {
        var support = gapFree("a", "support", M0, EDGE);
        var ordinary = gapFree("b", "subject", M2, PlacementRules.NONE);
        verifier.verifyThat(SchoolConstraintProvider::cohortDailyGaps).given(support, ordinary).penalizesBy(1);
        assertEquals(List.of(List.of("class", "m1")),
                diagnostic(List.of(support, ordinary), KernelCatalog.COHORT_DAILY_GAPS.id()));

        support.setPeriod(M1);
        verifier.verifyThat(SchoolConstraintProvider::cohortDailyGaps).given(support, ordinary).hasNoImpact();
        var third = gapFree("c", "subject", M3, PlacementRules.NONE);
        support.setPeriod(M1);
        ordinary.setPeriod(M2);
        verifier.verifyThat(SchoolConstraintProvider::cohortDailyGaps).given(support, ordinary, third).hasNoImpact();
    }

    @Test
    void incrementalScoreTracksEdgeAndStartMoves() {
        // Start bounds belong to the cohort, so every lesson of the cohort carries the same bound.
        var first = lesson("a", "subject", M1, startBound(2));
        var edge = lesson("b", "support", M2, new PlacementRules(2, 3, true, 1, true, 1));
        var last = lesson("c", "subject", M3, startBound(2));
        var factory = new DefaultSolverFactory<SchoolSchedule>(
                SolverAdapter.baseConfig(new SolverAdapter.ExecutionControls(null, 10, 0)));
        try (var director = factory.<BendableScore>getScoreDirectorFactory().buildScoreDirector()) {
            director.setWorkingSolution(schedule(first, edge, last));
            assertEquals(-1, director.calculateScore().raw().hardScore(1));
            move(director, edge, M4);
            assertEquals(0, director.calculateScore().raw().hardScore(1));
            move(director, first, M4);
            move(director, edge, M0);
            assertEquals(0, director.calculateScore().raw().hardScore(1));
            move(director, edge, T1);
            // Monday now starts at m3, after two regular slots.
            assertEquals(-1, director.calculateScore().raw().hardScore(1));
        }
    }

    @Test
    void reservedPeriodsJoinTheRangeOnlyWhenPermitted() throws Exception {
        var mvk = (ObjectNode) JsonSupport.mapper().readTree(Path.of("..", "examples", "mvk.json"));
        SchoolSchedule permitted = new PlanningMapper().toPlanningProblem(domain(mvk));
        assertEquals(50, permitted.getPeriods().size());
        assertEquals(Set.of("mon-0", "tue-0", "wed-0", "thu-0", "fri-0"), permitted.getPeriods().stream()
                .filter(PeriodValue::reserved).map(PeriodValue::id).collect(Collectors.toSet()));
        var support = permitted.getLessons().stream()
                .filter(lesson -> lesson.getId().equals("1a.opiabi.01")).findFirst().orElseThrow();
        assertEquals(new PlacementRules(2, 3, true, 1, true, 1, 1), support.getPlacementRules());

        mvk.withArray("subjects").forEach(subject -> {
            ((ObjectNode) subject).remove("reservedPeriodsAllowed");
            ((ObjectNode) subject).remove("maxWeeklyReservedLessonsPerCohort");
        });
        SchoolSchedule reservedOnly = new PlanningMapper().toPlanningProblem(domain(mvk));
        assertEquals(45, reservedOnly.getPeriods().size());
        assertTrue(reservedOnly.getPeriods().stream().noneMatch(PeriodValue::reserved));
    }

    private static SchoolDefinition domain(ObjectNode definition) {
        var outcome = new DefinitionValidator().validateForPlan(
                JsonSupport.mapper().treeToValue(definition, SchoolDefinitionDto.class));
        assertTrue(outcome.report().isValid(), outcome.report().toString());
        return outcome.definition();
    }

    private static void move(
            ai.timefold.solver.core.impl.score.director.ScoreDirector<SchoolSchedule> director,
            PlanningLesson lesson, PeriodValue period) {
        director.beforeVariableChanged(lesson, "period");
        lesson.setPeriod(period);
        director.afterVariableChanged(lesson, "period");
    }

    private static PlacementRules startBound(int slot) {
        return new PlacementRules(slot, 3, false, PlacementRules.UNLIMITED, false, PlacementRules.UNLIMITED);
    }

    private static PlanningLesson lesson(String id, String subjectId, PeriodValue period, PlacementRules rules) {
        return lesson(id, subjectId, period, rules, Integer.MAX_VALUE);
    }

    private static PlanningLesson gapFree(String id, String subjectId, PeriodValue period, PlacementRules rules) {
        return lesson(id, subjectId, period, rules, 0);
    }

    private static PlanningLesson lesson(
            String id, String subjectId, PeriodValue period, PlacementRules rules, int maxDailyGaps) {
        var lesson = new PlanningLesson(
                id, subjectId, "class", 20, "teacher-" + id, null,
                ALL, Set.of(), ALL, Set.of(), Set.of(),
                Set.of(), Set.of(), null, null, null, null, PERIODS, 1, maxDailyGaps, rules);
        lesson.setPeriod(period);
        lesson.setRoom(new RoomValue("room-" + id, 30, Set.of(), ALL));
        return lesson;
    }

    private static ScheduleEvaluator.Evaluation evaluate(PlanningLesson... lessons) {
        return new ScheduleEvaluator().evaluate(schedule(lessons), Map.of());
    }

    private static List<List<String>> diagnostic(PlanningLesson lesson, String constraintId) {
        return diagnostic(List.of(lesson), constraintId);
    }

    private static List<List<String>> diagnostic(List<PlanningLesson> lessons, String constraintId) {
        var schedule = schedule(lessons.toArray(PlanningLesson[]::new));
        return HardConstraintDiagnostics.from(schedule, new ScheduleEvaluator().evaluate(schedule, Map.of()))
                .stream().filter(diagnostic -> diagnostic.constraintId().equals(constraintId))
                .findFirst().orElseThrow().examples();
    }

    private static BendableScore score(PlanningLesson... lessons) {
        var factory = new DefaultSolverFactory<SchoolSchedule>(
                SolverAdapter.baseConfig(new SolverAdapter.ExecutionControls(null, 10, 0)));
        try (var director = factory.<BendableScore>getScoreDirectorFactory().buildScoreDirector()) {
            director.setWorkingSolution(schedule(lessons));
            return director.calculateScore().raw();
        }
    }

    private static SchoolSchedule schedule(PlanningLesson... lessons) {
        var rooms = java.util.Arrays.stream(lessons).map(PlanningLesson::getRoom).distinct().toList();
        return new SchoolSchedule(PERIODS, rooms, List.of(lessons), ConstraintWeightOverrides.none());
    }
}
