package org.schoolkernel.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.schoolkernel.domain.KernelCatalog;

class SchoolQualityConstraintTest {
    private static final PeriodValue M1 = new PeriodValue("m1", DayOfWeek.MONDAY, 10);
    private static final PeriodValue M2 = new PeriodValue("m2", DayOfWeek.MONDAY, 20);
    private static final PeriodValue M3 = new PeriodValue("m3", DayOfWeek.MONDAY, 30);
    private static final PeriodValue M4 = new PeriodValue("m4", DayOfWeek.MONDAY, 40);
    private static final PeriodValue M5 = new PeriodValue("m5", DayOfWeek.MONDAY, 50);
    private static final PeriodValue T1 = new PeriodValue("t1", DayOfWeek.TUESDAY, 1);
    private static final PeriodValue T2 = new PeriodValue("t2", DayOfWeek.TUESDAY, 2);
    private static final PeriodValue W1 = new PeriodValue("w1", DayOfWeek.WEDNESDAY, 1);
    private static final List<PeriodValue> PERIODS = List.of(M1, M2, M3, M4, M5, T1, T2, W1);
    private static final Set<String> ALL = PERIODS.stream().map(PeriodValue::id).collect(Collectors.toSet());
    private static final RoomValue ROOM = new RoomValue("r", 30, Set.of(), ALL);

    private final ConstraintVerifier<SchoolConstraintProvider, SchoolSchedule> verifier =
            ConstraintVerifier.build(new SchoolConstraintProvider(), SchoolSchedule.class, PlanningLesson.class);

    @Test
    void cohortGapCountsOnlyAvailablePeriodsInsideTheSchoolDay() {
        var first = lesson("a", ALL, M1);
        var last = lesson("b", ALL, M3);
        verifier.verifyThat(SchoolConstraintProvider::cohortGap).given(first, last).penalizesBy(1);
        assertEquals(1, new ScheduleEvaluator().evaluate(schedule(first, last),
                KernelCatalog.defaultSoftWeights()).softMatchCounts().get(KernelCatalog.COHORT_GAP.id()));

        last.setPeriod(M2);
        verifier.verifyThat(SchoolConstraintProvider::cohortGap).given(first, last).hasNoImpact();

        var blockedFirst = lesson("c", Set.of("m1", "m3", "t1", "t2", "w1"), M1);
        var blockedLast = lesson("d", Set.of("m1", "m3", "t1", "t2", "w1"), M3);
        verifier.verifyThat(SchoolConstraintProvider::cohortGap)
                .given(blockedFirst, blockedLast).hasNoImpact();
    }

    @Test
    void incrementalScoreRecognizesAWithinDayGapClosure() {
        var first = lesson("a", ALL, M1);
        var last = lesson("b", ALL, M3);
        var factory = new DefaultSolverFactory<SchoolSchedule>(
                SolverAdapter.baseConfig(new SolverAdapter.ExecutionControls(null, 10, 0)));
        try (var director = factory.<BendableScore>getScoreDirectorFactory().buildScoreDirector()) {
            director.setWorkingSolution(schedule(first, last));
            BendableScore before = director.calculateScore().raw();
            director.beforeVariableChanged(last, "period");
            last.setPeriod(M2);
            director.afterVariableChanged(last, "period");
            BendableScore after = director.calculateScore().raw();
            assertEquals(1, after.softScore(2) - before.softScore(2));
        }
    }

    @Test
    void lateStartCountsOneTaughtCohortDayBeyondThirdDeclaredSlot() {
        var third = lesson("third", ALL, M3);
        verifier.verifyThat(SchoolConstraintProvider::cohortLateStart).given(third).hasNoImpact();

        var fourth = lesson("fourth", ALL, M4);
        var fifth = lesson("fifth", ALL, M5);
        verifier.verifyThat(SchoolConstraintProvider::cohortLateStart).given(fourth).penalizesBy(1);
        verifier.verifyThat(SchoolConstraintProvider::cohortLateStart).given(fourth, fifth).penalizesBy(1);
        assertEquals(1, new ScheduleEvaluator().evaluate(schedule(fourth, fifth),
                KernelCatalog.defaultSoftWeights()).softMatchCounts().get(KernelCatalog.COHORT_LATE_START.id()));

        var tuesday = lesson("tuesday", ALL, T2);
        verifier.verifyThat(SchoolConstraintProvider::cohortLateStart).given(fourth, tuesday).penalizesBy(1);
        third.setPeriod(M3);
        verifier.verifyThat(SchoolConstraintProvider::cohortLateStart).given(third, fourth).hasNoImpact();
    }

    @Test
    void incrementalScoreRecognizesThirdSlotStart() {
        var first = lesson("first", ALL, M4);
        var factory = new DefaultSolverFactory<SchoolSchedule>(
                SolverAdapter.baseConfig(new SolverAdapter.ExecutionControls(null, 10, 0)));
        try (var director = factory.<BendableScore>getScoreDirectorFactory().buildScoreDirector()) {
            director.setWorkingSolution(schedule(first));
            BendableScore before = director.calculateScore().raw();
            director.beforeVariableChanged(first, "period");
            first.setPeriod(M3);
            director.afterVariableChanged(first, "period");
            BendableScore after = director.calculateScore().raw();
            assertEquals(1, after.softScore(2) - before.softScore(2));
        }
    }

    @Test
    void weekBalanceIncludesAvailableDaysWithNoLessonsAndCanBeDisabled() {
        var a = lesson("a", ALL, M1);
        var b = lesson("b", ALL, M2);
        var c = lesson("c", ALL, M3);
        var d = lesson("d", ALL, T1);
        verifier.verifyThat(SchoolConstraintProvider::cohortWeekBalance).given(a, b, c, d).penalizesBy(3);
        var evaluator = new ScheduleEvaluator();
        var skewed = evaluator.evaluate(schedule(a, b, c, d), KernelCatalog.defaultSoftWeights());
        assertEquals(3, skewed.softMatchCounts().get(KernelCatalog.COHORT_WEEK_BALANCE.id()));

        Map<String, Long> disabled = new java.util.HashMap<>(KernelCatalog.defaultSoftWeights());
        disabled.put(KernelCatalog.COHORT_WEEK_BALANCE.id(), 0L);
        assertEquals(skewed.ordinaryPreferencePenalty() - 3,
                evaluator.evaluate(schedule(a, b, c, d), disabled).ordinaryPreferencePenalty());

        c.setPeriod(T2);
        d.setPeriod(W1);
        verifier.verifyThat(SchoolConstraintProvider::cohortWeekBalance).given(a, b, c, d).hasNoImpact();
        assertEquals(0, evaluator.evaluate(schedule(a, b, c, d), KernelCatalog.defaultSoftWeights())
                .softMatchCounts().get(KernelCatalog.COHORT_WEEK_BALANCE.id()));

        var monday = lesson("e", Set.of("m1", "m2", "m3", "t1", "t2"), M1);
        var tuesday = lesson("f", Set.of("m1", "m2", "m3", "t1", "t2"), T1);
        verifier.verifyThat(SchoolConstraintProvider::cohortWeekBalance)
                .given(monday, tuesday).hasNoImpact();
    }

    private static PlanningLesson lesson(String id, Set<String> cohortAvailability, PeriodValue period) {
        var lesson = new PlanningLesson(
                id, "subject", "class", 20, "teacher-" + id, null,
                ALL, Set.of(), cohortAvailability, Set.of(), Set.of(),
                Set.of(), Set.of(), null, null, PERIODS);
        lesson.setPeriod(period);
        lesson.setRoom(ROOM);
        return lesson;
    }

    private static SchoolSchedule schedule(PlanningLesson... lessons) {
        return new SchoolSchedule(PERIODS, List.of(ROOM), List.of(lessons), ConstraintWeightOverrides.none());
    }
}
