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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.SchoolDefinitionDto;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.domain.KernelCatalog;

class DailySpreadConstraintTest {
    private static final PeriodValue M0 = new PeriodValue("m0", DayOfWeek.MONDAY, 1, true);
    private static final PeriodValue M1 = new PeriodValue("m1", DayOfWeek.MONDAY, 2);
    private static final PeriodValue M2 = new PeriodValue("m2", DayOfWeek.MONDAY, 3);
    private static final PeriodValue M3 = new PeriodValue("m3", DayOfWeek.MONDAY, 4);
    private static final PeriodValue T1 = new PeriodValue("t1", DayOfWeek.TUESDAY, 2);
    private static final PeriodValue W1 = new PeriodValue("w1", DayOfWeek.WEDNESDAY, 2);
    private static final List<PeriodValue> PERIODS = List.of(M0, M1, M2, M3, T1, W1);
    private static final Set<String> ALL = PERIODS.stream().map(PeriodValue::id).collect(Collectors.toSet());

    private final ConstraintVerifier<SchoolConstraintProvider, SchoolSchedule> verifier =
            ConstraintVerifier.build(new SchoolConstraintProvider(), SchoolSchedule.class, PlanningLesson.class);

    @Test
    @DisplayName("Daily spread RULE-2: the hard count is the weekly-balance count against the limit, including untaught days")
    void spreadExcessMatchesWeeklyBalanceAgainstTheLimit() {
        // Monday 3, Tuesday 0, Wednesday 0: pairs (3,0), (3,0), (0,0).
        var lessons = cohortWeek(1, M1, M2, M3);
        verifier.verifyThat(SchoolConstraintProvider::cohortDailySpread).given(lessons).penalizesBy(4);
        var evaluation = evaluate(lessons);
        assertEquals(4, evaluation.hardMatchCounts().get(KernelCatalog.COHORT_DAILY_SPREAD.id()));
        assertFalse(evaluation.feasible());
        assertEquals(List.of(List.of("class", "MONDAY", "TUESDAY")),
                diagnostic(lessons, KernelCatalog.COHORT_DAILY_SPREAD.id()));

        var relaxed = cohortWeek(3, M1, M2, M3);
        verifier.verifyThat(SchoolConstraintProvider::cohortDailySpread).given(relaxed).hasNoImpact();
        assertEquals(0, evaluate(relaxed).hardMatchCounts().get(KernelCatalog.COHORT_DAILY_SPREAD.id()));
    }

    @Test
    @DisplayName("Daily spread RULE-2: a cohort without a limit is never penalized and keeps its soft balance row")
    void unlimitedCohortsKeepOnlyTheSoftPreference() {
        var lessons = cohortWeek(PlacementRules.UNLIMITED, M1, M2, M3);
        verifier.verifyThat(SchoolConstraintProvider::cohortDailySpread).given(lessons).hasNoImpact();
        var evaluation = evaluate(lessons);
        assertTrue(evaluation.feasible());
        assertEquals(4, evaluation.softMatchCounts().get(KernelCatalog.COHORT_WEEK_BALANCE.id()));
    }

    @Test
    @DisplayName("Daily spread RULE-2: a lesson in a reserved period counts toward its weekday")
    void reservedPeriodLessonsCountTowardTheirDay() {
        // Monday 2 (one in m0), Tuesday 1, Wednesday 1.
        var lessons = cohortWeek(0, M0, M1, T1, W1);
        verifier.verifyThat(SchoolConstraintProvider::cohortDailySpread).given(lessons).penalizesBy(2);
    }

    @Test
    @DisplayName("Daily spread RULE-3: the limit is a day-shape rule on hard level 1")
    void spreadIsDayShapeHard() {
        // Monday 2, Tuesday 1, Wednesday 0: only (2,0) exceeds a limit of one.
        BendableScore score = score(cohortWeek(1, M1, M2, T1));
        assertEquals(0, score.hardScore(0));
        assertEquals(-1, score.hardScore(1));
    }

    @Test
    void incrementalScoreTracksDayMoves() {
        var lessons = cohortWeek(1, M1, M2, M3);
        var factory = new DefaultSolverFactory<SchoolSchedule>(
                SolverAdapter.baseConfig(new SolverAdapter.ExecutionControls(null, 10, 0)));
        try (var director = factory.<BendableScore>getScoreDirectorFactory().buildScoreDirector()) {
            director.setWorkingSolution(schedule(lessons));
            assertEquals(-4, director.calculateScore().raw().hardScore(1));
            move(director, lessons[2], T1);
            assertEquals(-1, director.calculateScore().raw().hardScore(1));
            move(director, lessons[1], W1);
            assertEquals(0, director.calculateScore().raw().hardScore(1));
        }
    }

    @Test
    void mvkCarriesTheCohortLimitOntoEveryLesson() throws Exception {
        var mvk = JsonSupport.mapper().readTree(Path.of("..", "examples", "mvk.json"));
        var outcome = new DefinitionValidator().validateForPlan(
                JsonSupport.mapper().treeToValue(mvk, SchoolDefinitionDto.class));
        assertTrue(outcome.report().isValid(), outcome.report().toString());
        SchoolSchedule problem = new PlanningMapper().toPlanningProblem(outcome.definition());
        problem.getLessons().forEach(lesson -> assertEquals(
                lesson.getCohortId().charAt(0) <= '4' ? 1 : 2,
                lesson.getPlacementRules().dailyLessonSpreadLimit(), lesson.getId()));
    }

    private static PlanningLesson[] cohortWeek(int limit, PeriodValue... periods) {
        var rules = new PlacementRules(PlacementRules.UNLIMITED, 3, true, PlacementRules.UNLIMITED,
                false, PlacementRules.UNLIMITED, limit);
        var lessons = new PlanningLesson[periods.length];
        for (int index = 0; index < periods.length; index++) {
            String id = "l" + index;
            lessons[index] = new PlanningLesson(
                    id, "subject", "class", 20, "teacher-" + id, null,
                    ALL, Set.of(), ALL, Set.of(), Set.of(),
                    Set.of(), Set.of(), null, null, null, null, PERIODS, 1, Integer.MAX_VALUE, rules);
            lessons[index].setPeriod(periods[index]);
            lessons[index].setRoom(new RoomValue("room-" + id, 30, Set.of(), ALL));
        }
        return lessons;
    }

    private static void move(
            ai.timefold.solver.core.impl.score.director.ScoreDirector<SchoolSchedule> director,
            PlanningLesson lesson, PeriodValue period) {
        director.beforeVariableChanged(lesson, "period");
        lesson.setPeriod(period);
        director.afterVariableChanged(lesson, "period");
    }

    private static ScheduleEvaluator.Evaluation evaluate(PlanningLesson... lessons) {
        return new ScheduleEvaluator().evaluate(schedule(lessons), Map.of());
    }

    private static List<List<String>> diagnostic(PlanningLesson[] lessons, String constraintId) {
        var schedule = schedule(lessons);
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
