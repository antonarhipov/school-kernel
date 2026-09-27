package org.schoolkernel.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.SchoolDefinitionDto;

import tools.jackson.databind.node.ObjectNode;

class DefinitionValidatorTest {
    @Test
    @DisplayName("UC-1 ext 2a and 2b: semantic errors are collected deterministically before solving")
    void collectsSemanticErrors() throws Exception {
        ObjectNode input = validInput();
        input.put("basedOnRevision", "sha256:" + "0".repeat(64));
        input.withArray("subjects").add(input.withArray("subjects").get(0).deepCopy());
        ((ObjectNode) input.withArray("teachers").get(0)).withArray("qualifiedSubjectIds").removeAll();

        var dto = JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class);
        ValidationReport report = new DefinitionValidator().validateForPlan(dto).report();

        assertFalse(report.isValid());
        assertEquals(3, report.totalErrors());
        assertTrue(report.errors().get(0).location().compareTo(report.errors().get(1).location()) <= 0);
        assertTrue(report.errors().stream().anyMatch(error -> error.message().contains("forbidden")));
        assertTrue(report.errors().stream().anyMatch(error -> error.message().contains("duplicate")));
        assertTrue(report.errors().stream().anyMatch(error -> error.message().contains("not qualified")));
    }

    @Test
    @DisplayName("UC-1 G1: omitted availability expands to every declared period")
    void omittedAvailabilityMeansAllPeriods() throws Exception {
        var dto = JsonSupport.mapper().treeToValue(validInput(), SchoolDefinitionDto.class);

        SchoolDefinition definition = new DefinitionValidator().validateForPlan(dto).definition();

        assertEquals(java.util.Set.of("mon-1"), definition.teachers().getFirst().availablePeriodIds());
        assertEquals(java.util.Set.of("mon-1"), definition.cohorts().getFirst().availablePeriodIds());
        assertEquals(java.util.Set.of("mon-1"), definition.rooms().getFirst().availablePeriodIds());
    }

    @Test
    void catalogFourCohortDailySpreadDefaultsToOneAndPreservesAnExplicitValue() throws Exception {
        ObjectNode input = validInput();
        input.put("catalogVersion", 4);
        var validator = new DefinitionValidator();
        assertEquals(1, validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .definition().cohorts().getFirst().maxDailyLessonSpread());

        ((ObjectNode) input.withArray("cohorts").get(0)).put("maxDailyLessonSpread", 2);
        assertEquals(2, validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .definition().cohorts().getFirst().maxDailyLessonSpread());
    }

    @Test
    void cohortDailyGapsAreForbiddenByDefaultOnlyFromCatalogFive() throws Exception {
        ObjectNode input = validInput();
        var validator = new DefinitionValidator();
        input.put("catalogVersion", 4);
        assertEquals(SchoolDefinition.Cohort.UNLIMITED_GAPS,
                validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                        .definition().cohorts().getFirst().maxDailyGaps());

        input.put("catalogVersion", 5);
        assertEquals(0, validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .definition().cohorts().getFirst().maxDailyGaps());

        ((ObjectNode) input.withArray("cohorts").get(0)).put("maxDailyGaps", 2);
        assertEquals(2, validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .definition().cohorts().getFirst().maxDailyGaps());
    }

    @Test
    void dayEdgeFieldDefaults() throws Exception {
        ObjectNode input = validInput();
        input.put("catalogVersion", 6);
        var validator = new DefinitionValidator();
        var defaults = validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .definition();
        assertEquals(SchoolDefinition.Cohort.NO_START_BOUND, defaults.cohorts().getFirst().latestStartSlot());
        assertEquals(3, defaults.cohorts().getFirst().preferredLatestStartSlot());
        assertEquals(new SchoolDefinition.Subject("math", "Mathematics"), defaults.subjects().getFirst());

        ((ObjectNode) input.withArray("cohorts").get(0)).put("latestStartSlot", 2).put("preferredLatestStartSlot", 1);
        ((ObjectNode) input.withArray("subjects").get(0)).put("reservedPeriodsAllowed", true)
                .put("maxWeeklyReservedLessonsPerCohort", 1).put("dayEdgeOnly", true)
                .put("maxDailyLessonsPerCohort", 1);
        var declared = validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .definition();
        assertEquals(2, declared.cohorts().getFirst().latestStartSlot());
        assertEquals(1, declared.cohorts().getFirst().preferredLatestStartSlot());
        assertEquals(new SchoolDefinition.Subject("math", "Mathematics", true, 1, true, 1),
                declared.subjects().getFirst());
    }

    @Test
    void dailySpreadLimitIsUnboundedUnlessDeclared() throws Exception {
        ObjectNode input = validInput();
        input.put("catalogVersion", 7);
        var validator = new DefinitionValidator();
        var defaults = validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .definition();
        assertEquals(SchoolDefinition.Cohort.NO_SPREAD_LIMIT, defaults.cohorts().getFirst().dailyLessonSpreadLimit());

        ((ObjectNode) input.withArray("cohorts").get(0)).put("dailyLessonSpreadLimit", 2);
        var declared = validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .definition();
        assertEquals(2, declared.cohorts().getFirst().dailyLessonSpreadLimit());
    }

    @Test
    void reservedPeriodLockIsValidOnlyForPermittedSubjects() throws Exception {
        ObjectNode input = validInput();
        input.put("catalogVersion", 6);
        input.putArray("reservedPeriodIds").add("mon-1");
        ((ObjectNode) input.withArray("lessons").get(0)).put("periodLock", "mon-1");
        var validator = new DefinitionValidator();
        var refused = validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class));
        assertTrue(refused.report().errors().stream()
                .anyMatch(error -> error.message().equals("period lock contradicts school reservation")));

        ((ObjectNode) input.withArray("subjects").get(0)).put("reservedPeriodsAllowed", true);
        assertTrue(validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .report().isValid());
    }

    @Test
    @DisplayName("UC-1 validation reporting: details are deterministic and capped while total is preserved")
    void validationReportsAreDeterministicAndCapped() {
        var detected = java.util.stream.IntStream.range(0, 1_005)
                .mapToObj(index -> new ValidationError(
                        "/z/" + String.format("%04d", 1_004 - index),
                        List.of("entity-" + index),
                        "safe error"))
                .toList();

        ValidationReport report = ValidationReport.from(detected);

        assertEquals(1_005, report.totalErrors());
        assertTrue(report.truncated());
        assertEquals(1_000, report.errors().size());
        assertEquals("/z/0000", report.errors().getFirst().location());
        assertEquals("/z/0999", report.errors().getLast().location());
    }

    @Test
    @DisplayName("Curator RULE-2: a curator lesson needs the cohort's curator, who is qualified for it by role")
    void curatorLessonsBelongToTheCohortCurator() throws Exception {
        ObjectNode input = curatorInput();
        var validator = new DefinitionValidator();
        var accepted = validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class));
        assertTrue(accepted.report().isValid(), accepted.report().toString());
        var cohort = accepted.definition().cohorts().getFirst();
        assertEquals("teacher-1", cohort.curatorTeacherId());
        assertEquals("room-1", cohort.homeRoomId());
        assertTrue(accepted.definition().subjects().getLast().curatorLesson());

        ObjectNode other = input.deepCopy();
        other.withArray("teachers").addObject().put("id", "teacher-2").put("displayName", "Teacher Two")
                .putArray("qualifiedSubjectIds").add("class-hour");
        ((ObjectNode) other.withArray("lessons").get(1)).put("teacherId", "teacher-2");
        assertEquals(List.of("curator lesson must be taught by the cohort's curator"), messages(validator, other));

        ObjectNode uncurated = input.deepCopy();
        ((ObjectNode) uncurated.withArray("cohorts").get(0)).remove("curatorTeacherId");
        assertEquals(java.util.Set.of("teacher is not qualified for the lesson subject",
                        "curator lesson requires the cohort to declare a curator"),
                java.util.Set.copyOf(messages(validator, uncurated)));

        ObjectNode unknown = input.deepCopy();
        ((ObjectNode) unknown.withArray("cohorts").get(0)).put("curatorTeacherId", "nobody").put("homeRoomId", "nowhere");
        assertTrue(messages(validator, unknown).containsAll(List.of("unknown teacher reference", "unknown room reference")));
    }

    @Test
    @DisplayName("Curator RULE-2: a home room that cannot host the curator lesson is invalid input")
    void homeRoomContradictionsAreInvalidInput() throws Exception {
        var validator = new DefinitionValidator();
        ObjectNode locked = curatorInput();
        locked.withArray("rooms").addObject().put("id", "room-2").put("displayName", "Room Two").put("capacity", 25)
                .putArray("capabilityIds");
        ((ObjectNode) locked.withArray("lessons").get(1)).put("roomLock", "room-2");
        assertEquals(List.of("room lock contradicts cohort home room"), messages(validator, locked));

        ObjectNode small = curatorInput();
        ((ObjectNode) small.withArray("rooms").get(0)).put("capacity", 10);
        assertTrue(messages(validator, small).contains("cohort home room contradicts room capacity"));

        ObjectNode lab = curatorInput();
        ((ObjectNode) lab.withArray("lessons").get(1)).putArray("requiredRoomCapabilityIds").add("lab");
        assertEquals(List.of("cohort home room contradicts required room capabilities"), messages(validator, lab));
    }

    /** Catalog 8: cohort-1 has curator teacher-1 and home room room-1; lesson-2 is its class hour. */
    private static ObjectNode curatorInput() throws Exception {
        ObjectNode input = validInput();
        input.put("catalogVersion", 8);
        input.withArray("subjects").addObject().put("id", "class-hour").put("displayName", "Class hour")
                .put("curatorLesson", true);
        ((ObjectNode) input.withArray("cohorts").get(0)).put("curatorTeacherId", "teacher-1")
                .put("homeRoomId", "room-1");
        input.withArray("lessons").addObject().put("id", "lesson-2").put("displayName", "Class hour")
                .put("subjectId", "class-hour").put("cohortId", "cohort-1").put("teacherId", "teacher-1");
        return input;
    }

    private static List<String> messages(DefinitionValidator validator, ObjectNode input) throws Exception {
        return validator.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class))
                .report().errors().stream().map(ValidationError::message).toList();
    }

    private static ObjectNode validInput() throws Exception {
        try (InputStream input = DefinitionValidatorTest.class.getResourceAsStream("/fixtures/valid-plan.json")) {
            return (ObjectNode) JsonSupport.mapper().readTree(input);
        }
    }
}
