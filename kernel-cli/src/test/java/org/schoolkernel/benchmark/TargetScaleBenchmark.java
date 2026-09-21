package org.schoolkernel.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.solver.SolverAdapter;

/** Non-gating target-scale measurement. Run with {@code ./mvnw -Pbenchmark test}. */
class TargetScaleBenchmark {
    @Test
    @DisplayName("UC-1 G11: report target-scale planning measurement without a CI threshold")
    void targetScalePlanningMeasurement() {
        SchoolDefinition definition = targetScaleDefinition();
        long started = System.nanoTime();

        var result = new SolverAdapter().solve(
                definition,
                new SolverAdapter.ExecutionControls(null, 1, 0));

        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        assertEquals(1_000, result.schedule().getLessons().size());
        assertTrue(result.evaluation().complete());
        assertTrue(result.evaluation().feasible());
        System.out.printf(
                "TARGET_SCALE lessons=1000 teachers=100 cohorts=60 rooms=100 periods=60 elapsedMs=%d termination=%s%n",
                elapsedMillis,
                result.terminationReason());
    }

    private static SchoolDefinition targetScaleDefinition() {
        var periods = IntStream.range(0, 60)
                .mapToObj(index -> new SchoolDefinition.Period(
                        "period-" + index,
                        "Period " + index,
                        DayOfWeek.of(index / 10 + 1),
                        index % 10 + 1,
                        null,
                        null))
                .toList();
        Set<String> periodIds = periods.stream()
                .map(SchoolDefinition.Period::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        var subjects = List.of(new SchoolDefinition.Subject("subject", "Subject"));
        var teachers = IntStream.range(0, 100)
                .mapToObj(index -> new SchoolDefinition.Teacher(
                        "teacher-" + index,
                        "Teacher " + index,
                        Set.of("subject"),
                        periodIds,
                        Set.of()))
                .toList();
        var cohorts = IntStream.range(0, 60)
                .mapToObj(index -> new SchoolDefinition.Cohort(
                        "cohort-" + index,
                        "Cohort " + index,
                        20,
                        periodIds,
                        Set.of()))
                .toList();
        var rooms = IntStream.range(0, 100)
                .mapToObj(index -> new SchoolDefinition.Room(
                        "room-" + index,
                        "Room " + index,
                        30,
                        Set.of(),
                        periodIds))
                .toList();
        var lessons = IntStream.range(0, 1_000)
                .mapToObj(index -> new SchoolDefinition.Lesson(
                        "lesson-" + index,
                        "Lesson " + index,
                        "subject",
                        "cohort-" + (index % 60),
                        "teacher-" + (index % 100),
                        null,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        null,
                        null))
                .toList();
        return new SchoolDefinition(
                1,
                1,
                "benchmark-school",
                "Benchmark School",
                null,
                subjects,
                teachers,
                cohorts,
                rooms,
                periods,
                lessons,
                Map.of(
                        "soft.teacher-gap", 0L,
                        "soft.series-same-day", 0L,
                        "soft.undesirable-period", 0L,
                        "soft.non-preferred-room", 0L));
    }
}
