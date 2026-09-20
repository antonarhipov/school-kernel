package org.schoolkernel.solver;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SchoolConstraintProviderTest {
    private static final PeriodValue P1 = new PeriodValue("p1", DayOfWeek.MONDAY, 1);
    private static final PeriodValue P2 = new PeriodValue("p2", DayOfWeek.MONDAY, 2);
    private static final PeriodValue P3 = new PeriodValue("p3", DayOfWeek.MONDAY, 3);
    private static final List<PeriodValue> PERIODS = List.of(P1, P2, P3);
    private static final RoomValue R1 = new RoomValue("r1", 30, Set.of("lab"), Set.of("p1", "p2", "p3"));
    private static final RoomValue R2 = new RoomValue("r2", 10, Set.of(), Set.of("p1"));

    private final SchoolConstraintProvider provider = new SchoolConstraintProvider();
    private final ConstraintVerifier<SchoolConstraintProvider, SchoolSchedule> verifier =
            ConstraintVerifier.build(provider, SchoolSchedule.class, PlanningLesson.class);

    @Test
    @DisplayName("UC-1 G2 hard.teacher-period: matches collision and not distinct periods")
    void teacherPeriod() {
        var first = lesson("a", "t", "c1", null);
        var second = lesson("b", "t", "c2", null);
        assign(first, P1, R1);
        assign(second, P1, R2);
        verifier.verifyThat(SchoolConstraintProvider::teacherPeriod).given(first, second).penalizesBy(1);
        assign(second, P2, R2);
        verifier.verifyThat(SchoolConstraintProvider::teacherPeriod).given(first, second).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G2 hard.cohort-period: matches collision and not distinct periods")
    void cohortPeriod() {
        var first = lesson("a", "t1", "c", null);
        var second = lesson("b", "t2", "c", null);
        assign(first, P1, R1);
        assign(second, P1, R2);
        verifier.verifyThat(SchoolConstraintProvider::cohortPeriod).given(first, second).penalizesBy(1);
        assign(second, P2, R2);
        verifier.verifyThat(SchoolConstraintProvider::cohortPeriod).given(first, second).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G2 hard.room-period: matches collision and not distinct rooms")
    void roomPeriod() {
        var first = lesson("a", "t1", "c1", null);
        var second = lesson("b", "t2", "c2", null);
        assign(first, P1, R1);
        assign(second, P1, R1);
        verifier.verifyThat(SchoolConstraintProvider::roomPeriod).given(first, second).penalizesBy(1);
        assign(second, P1, R2);
        verifier.verifyThat(SchoolConstraintProvider::roomPeriod).given(first, second).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G2 availability catalog rows match only unavailable assignments")
    void availabilityRows() {
        var lesson = new PlanningLesson(
                "a", "subject", "c", 20, "t", null,
                Set.of("p1"), Set.of(),
                Set.of("p1"), Set.of(), Set.of(),
                Set.of(), Set.of(), null, null, PERIODS);
        assign(lesson, P2, R2);
        verifier.verifyThat(SchoolConstraintProvider::teacherAvailability).given(lesson).penalizesBy(1);
        verifier.verifyThat(SchoolConstraintProvider::cohortAvailability).given(lesson).penalizesBy(1);
        verifier.verifyThat(SchoolConstraintProvider::roomAvailability).given(lesson).penalizesBy(1);
        assign(lesson, P1, R2);
        verifier.verifyThat(SchoolConstraintProvider::teacherAvailability).given(lesson).hasNoImpact();
        verifier.verifyThat(SchoolConstraintProvider::cohortAvailability).given(lesson).hasNoImpact();
        verifier.verifyThat(SchoolConstraintProvider::roomAvailability).given(lesson).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G2 hard.room-capacity: matches undersized room only")
    void roomCapacity() {
        var lesson = lesson("a", "t", "c", null);
        assign(lesson, P1, R2);
        verifier.verifyThat(SchoolConstraintProvider::roomCapacity).given(lesson).penalizesBy(1);
        assign(lesson, P1, R1);
        verifier.verifyThat(SchoolConstraintProvider::roomCapacity).given(lesson).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G2 hard.room-capability: matches missing capability only")
    void roomCapability() {
        var lesson = lesson("a", "t", "c", null, Set.of("lab"), Set.of(), null, null);
        assign(lesson, P1, R2);
        verifier.verifyThat(SchoolConstraintProvider::roomCapability).given(lesson).penalizesBy(1);
        assign(lesson, P1, R1);
        verifier.verifyThat(SchoolConstraintProvider::roomCapability).given(lesson).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G2 lock catalog rows match only assignments that break the lock")
    void locks() {
        var lesson = lesson("a", "t", "c", null, Set.of(), Set.of(), "p1", "r1");
        assign(lesson, P2, R2);
        verifier.verifyThat(SchoolConstraintProvider::periodLock).given(lesson).penalizesBy(1);
        verifier.verifyThat(SchoolConstraintProvider::roomLock).given(lesson).penalizesBy(1);
        assign(lesson, P1, R1);
        verifier.verifyThat(SchoolConstraintProvider::periodLock).given(lesson).hasNoImpact();
        verifier.verifyThat(SchoolConstraintProvider::roomLock).given(lesson).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G3 soft.teacher-gap: counts available unassigned periods inside a block")
    void teacherGap() {
        var first = lesson("a", "t", "c1", null);
        var second = lesson("b", "t", "c2", null);
        assign(first, P1, R1);
        assign(second, P3, R2);
        verifier.verifyThat(SchoolConstraintProvider::teacherGap).given(first, second).penalizesBy(1);
        assign(second, P2, R2);
        verifier.verifyThat(SchoolConstraintProvider::teacherGap).given(first, second).hasNoImpact();

        var blockFirst = new PlanningLesson(
                "c", "subject", "c3", 20, "blocked", null,
                Set.of("p1", "p3"), Set.of(), Set.of("p1", "p2", "p3"), Set.of(), Set.of(),
                Set.of(), Set.of(), null, null, PERIODS);
        var blockSecond = new PlanningLesson(
                "d", "subject", "c4", 20, "blocked", null,
                Set.of("p1", "p3"), Set.of(), Set.of("p1", "p2", "p3"), Set.of(), Set.of(),
                Set.of(), Set.of(), null, null, PERIODS);
        assign(blockFirst, P1, R1);
        assign(blockSecond, P3, R2);
        verifier.verifyThat(SchoolConstraintProvider::teacherGap).given(blockFirst, blockSecond).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G3 soft.series-same-day: each occurrence after the first adds one match")
    void seriesSameDay() {
        var first = lesson("a", "t1", "c1", "series");
        var second = lesson("b", "t2", "c1", "series");
        assign(first, P1, R1);
        assign(second, P2, R2);
        verifier.verifyThat(SchoolConstraintProvider::seriesSameDay).given(first, second).penalizesBy(1);
        second.setPeriod(new PeriodValue("tu1", DayOfWeek.TUESDAY, 1));
        verifier.verifyThat(SchoolConstraintProvider::seriesSameDay).given(first, second).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G3 soft.undesirable-period: teacher, cohort, and lesson declarations are additive")
    void undesirablePeriod() {
        var lesson = new PlanningLesson(
                "a", "subject", "c", 20, "t", null,
                Set.of("p1", "p2", "p3"), Set.of("p1"),
                Set.of("p1", "p2", "p3"), Set.of("p1"), Set.of("p1"),
                Set.of(), Set.of(), null, null, PERIODS);
        assign(lesson, P1, R1);
        verifier.verifyThat(SchoolConstraintProvider::undesirablePeriod).given(lesson).penalizesBy(3);
        assign(lesson, P2, R1);
        verifier.verifyThat(SchoolConstraintProvider::undesirablePeriod).given(lesson).hasNoImpact();
    }

    @Test
    @DisplayName("UC-1 G3 soft.non-preferred-room: unordered preferred set has equal values")
    void nonPreferredRoom() {
        var lesson = lesson("a", "t", "c", null, Set.of(), Set.of("r1"), null, null);
        assign(lesson, P1, R2);
        verifier.verifyThat(SchoolConstraintProvider::nonPreferredRoom).given(lesson).penalizesBy(1);
        assign(lesson, P1, R1);
        verifier.verifyThat(SchoolConstraintProvider::nonPreferredRoom).given(lesson).hasNoImpact();
    }

    private static PlanningLesson lesson(String id, String teacher, String cohort, String series) {
        return lesson(id, teacher, cohort, series, Set.of(), Set.of(), null, null);
    }

    private static PlanningLesson lesson(
            String id,
            String teacher,
            String cohort,
            String series,
            Set<String> requiredCapabilities,
            Set<String> preferredRooms,
            String periodLock,
            String roomLock) {
        return new PlanningLesson(
                id, "subject", cohort, 20, teacher, series,
                Set.of("p1", "p2", "p3"), Set.of(),
                Set.of("p1", "p2", "p3"), Set.of(), Set.of(),
                requiredCapabilities, preferredRooms, periodLock, roomLock, PERIODS);
    }

    private static void assign(PlanningLesson lesson, PeriodValue period, RoomValue room) {
        lesson.setPeriod(period);
        lesson.setRoom(room);
    }
}
