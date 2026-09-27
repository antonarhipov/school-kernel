package org.schoolkernel.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.schoolkernel.domain.SchoolDefinition;

class HomeRoomConstraintTest {
    private static final PeriodValue M1 = new PeriodValue("m1", DayOfWeek.MONDAY, 1);
    private static final List<PeriodValue> PERIODS = List.of(M1);
    private static final Set<String> ALL = Set.of("m1");
    private static final RoomValue HOME = new RoomValue("home", 30, Set.of("general"), ALL);
    private static final RoomValue HALL = new RoomValue("hall", 30, Set.of("sports"), ALL);

    private final ConstraintVerifier<SchoolConstraintProvider, SchoolSchedule> verifier =
            ConstraintVerifier.build(new SchoolConstraintProvider(), SchoolSchedule.class, PlanningLesson.class);

    @Test
    @DisplayName("Curator RULE-3: a curator lesson outside the home room is one physical hard match at every boundary")
    void curatorLessonOutsideHomeRoomIsPhysicalHard() {
        var away = lesson("home", HALL);
        verifier.verifyThat(SchoolConstraintProvider::cohortHomeRoom).given(away).penalizesBy(1);
        var evaluation = new ScheduleEvaluator().evaluate(schedule(away), Map.of());
        assertEquals(1, evaluation.hardMatchCounts().get(KernelCatalog.COHORT_HOME_ROOM.id()));
        assertFalse(evaluation.feasible());
        assertEquals(List.of(List.of("lesson", "hall", "home")),
                HardConstraintDiagnostics.from(schedule(away), evaluation).stream()
                        .filter(diagnostic -> diagnostic.constraintId().equals(KernelCatalog.COHORT_HOME_ROOM.id()))
                        .findFirst().orElseThrow().examples());
        BendableScore score = score(away);
        assertEquals(-1, score.hardScore(0));
        assertEquals(0, score.hardScore(1));

        verifier.verifyThat(SchoolConstraintProvider::cohortHomeRoom).given(lesson("home", HOME)).hasNoImpact();
        verifier.verifyThat(SchoolConstraintProvider::cohortHomeRoom).given(lesson(null, HALL)).hasNoImpact();
    }

    @Test
    @DisplayName("Curator RULE-4: only curator lessons are bound; other lessons prefer a home room that fits them")
    void homeRoomBindsCuratorLessonsAndIsTheDefaultPreference() {
        var definition = new SchoolDefinition(1, 8, "school", "School", null,
                List.of(new SchoolDefinition.Subject("class-hour", "Class hour", false,
                                SchoolDefinition.Subject.UNLIMITED, false, SchoolDefinition.Subject.UNLIMITED, true),
                        new SchoolDefinition.Subject("math", "Math"),
                        new SchoolDefinition.Subject("sport", "Sport")),
                List.of(new SchoolDefinition.Teacher("t", "T", Set.of("class-hour", "math", "sport"), ALL, Set.of())),
                List.of(new SchoolDefinition.Cohort("c", "C", 20, ALL, Set.of(), 1, 0,
                        SchoolDefinition.Cohort.NO_START_BOUND, 3, SchoolDefinition.Cohort.NO_SPREAD_LIMIT,
                        "t", "home")),
                List.of(new SchoolDefinition.Room("home", "Home", 30, Set.of("general"), ALL),
                        new SchoolDefinition.Room("other", "Other", 30, Set.of("general"), ALL),
                        new SchoolDefinition.Room("hall", "Hall", 30, Set.of("sports"), ALL)),
                List.of(new SchoolDefinition.Period("m1", "M1", DayOfWeek.MONDAY, 1, null, null)),
                Set.of(),
                List.of(lessonDefinition("hour", "class-hour", Set.of(), Set.of()),
                        lessonDefinition("math", "math", Set.of("general"), Set.of()),
                        lessonDefinition("chosen", "math", Set.of("general"), Set.of("other")),
                        lessonDefinition("sport", "sport", Set.of("sports"), Set.of())),
                KernelCatalog.defaultSoftWeights());
        var lessons = new PlanningMapper().toPlanningProblem(definition).getLessons();
        assertEquals("home", lessons.get(0).getPlacementRules().homeRoomId());
        assertNull(lessons.get(1).getPlacementRules().homeRoomId());
        assertEquals(Set.of("home"), lessons.get(1).getPreferredRoomIds());
        assertEquals(Set.of("other"), lessons.get(2).getPreferredRoomIds());
        assertEquals(Set.of(), lessons.get(3).getPreferredRoomIds());
    }

    @Test
    void mvkBindsKlassitundToTheHomeRoom() throws Exception {
        var mvk = JsonSupport.mapper().readTree(Path.of("..", "examples", "mvk.json"));
        var outcome = new DefinitionValidator().validateForPlan(
                JsonSupport.mapper().treeToValue(mvk, SchoolDefinitionDto.class));
        assertTrue(outcome.report().isValid(), outcome.report().toString());
        new PlanningMapper().toPlanningProblem(outcome.definition()).getLessons().forEach(lesson -> {
            boolean klassitund = lesson.getSubjectId().equals("klassitund");
            assertEquals(klassitund, lesson.getPlacementRules().homeRoomId() != null, lesson.getId());
            if (klassitund) {
                assertEquals(lesson.getPreferredRoomIds(), Set.of(lesson.getPlacementRules().homeRoomId()));
            }
        });
    }

    private static SchoolDefinition.Lesson lessonDefinition(
            String id, String subjectId, Set<String> capabilities, Set<String> preferred) {
        return new SchoolDefinition.Lesson(id, id, subjectId, "c", "t", null, capabilities, preferred, Set.of(),
                null, null);
    }

    private static PlanningLesson lesson(String homeRoomId, RoomValue room) {
        var rules = new PlacementRules(PlacementRules.UNLIMITED, 3, false, PlacementRules.UNLIMITED, false,
                PlacementRules.UNLIMITED, PlacementRules.UNLIMITED, homeRoomId);
        var lesson = new PlanningLesson("lesson", "class-hour", "class", 20, "teacher", null,
                ALL, Set.of(), ALL, Set.of(), Set.of(), Set.of(), Set.of(), null, null, null, null, PERIODS, 1,
                Integer.MAX_VALUE, rules);
        lesson.setPeriod(M1);
        lesson.setRoom(room);
        return lesson;
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
        return new SchoolSchedule(PERIODS, List.of(HOME, HALL), List.of(lessons), ConstraintWeightOverrides.none());
    }
}
