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

    private static ObjectNode validInput() throws Exception {
        try (InputStream input = DefinitionValidatorTest.class.getResourceAsStream("/fixtures/valid-plan.json")) {
            return (ObjectNode) JsonSupport.mapper().readTree(input);
        }
    }
}
