package org.schoolkernel.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.schoolkernel.contract.DefinitionSchemaValidator;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.SchoolDefinitionDto;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.solver.PreflightFeasibilityCheck;

class MerivaljaExampleFixtureTests {
    @Test
    void fixtureIsAValidPreflightFeasiblePlanDefinition() throws IOException {
        assertValidFixture("/fixtures/merivalja-5a-5b.json", 2, 42);
    }

    @Test
    void selectedClassesFixtureIsAValidPreflightFeasiblePlanDefinition() throws IOException {
        assertValidFixture("/fixtures/merivalja-1a-1b-4a-4avr-4b-9a-9b.json", 7, 176);
    }

    private void assertValidFixture(String resource, int expectedCohorts, int expectedLessons) throws IOException {
        try (var input = getClass().getResourceAsStream(resource)) {
            var json = JsonSupport.mapper().readTree(input);
            assertTrue(new DefinitionSchemaValidator().validate(json).isEmpty());

            var dto = JsonSupport.mapper().treeToValue(json, SchoolDefinitionDto.class);
            var outcome = new DefinitionValidator().validateForPlan(dto);

            assertTrue(outcome.report().isValid());
            assertEquals(expectedCohorts, outcome.definition().cohorts().size());
            assertEquals(expectedLessons, outcome.definition().lessons().size());
            assertTrue(new PreflightFeasibilityCheck().findObviousFailures(outcome.definition()).isEmpty());
        }
    }
}
