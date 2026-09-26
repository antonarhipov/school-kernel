package org.schoolkernel.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContractTest {
    @Test
    @DisplayName("UC-1 G1: Draft 2020-12 schema accepts the normative valid definition")
    void schemaAcceptsValidDefinition() throws Exception {
        var input = JsonSupport.mapper().readTree(resource("/fixtures/valid-plan.json"));

        assertTrue(new DefinitionSchemaValidator().validate(input).isEmpty());
    }

    @Test
    @DisplayName("Schedule quality: catalogs 1, 2, and 3 keep distinct override sets")
    void catalogVersionsKeepDistinctOverrides() throws Exception {
        var legacy = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        var versionTwo = legacy.deepCopy();
        versionTwo.put("catalogVersion", 2);
        versionTwo.putArray("softConstraintOverrides").addObject()
                .put("constraintId", "soft.cohort-week-balance").put("weight", 0);
        assertTrue(new DefinitionSchemaValidator().validate(versionTwo).isEmpty());
        legacy.set("softConstraintOverrides", versionTwo.path("softConstraintOverrides").deepCopy());
        assertFalse(new DefinitionSchemaValidator().validate(legacy).isEmpty());

        var versionThree = versionTwo.deepCopy();
        versionThree.put("catalogVersion", 3);
        versionThree.putArray("softConstraintOverrides").addObject()
                .put("constraintId", "soft.cohort-late-start").put("weight", 0);
        assertTrue(new DefinitionSchemaValidator().validate(versionThree).isEmpty());
        versionTwo.set("softConstraintOverrides", versionThree.path("softConstraintOverrides").deepCopy());
        assertFalse(new DefinitionSchemaValidator().validate(versionTwo).isEmpty());
    }

    @Test
    @DisplayName("Cohort balance UC-1: catalog 4 accepts a cohort spread target and older catalogs reject it")
    void cohortDailySpreadSchemaAndRevision() throws Exception {
        var definition = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        definition.put("catalogVersion", 4);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        String withoutTarget = new RevisionService().definitionRevision(definition);

        var cohort = (tools.jackson.databind.node.ObjectNode) definition.withArray("cohorts").get(0);
        cohort.put("maxDailyLessonSpread", 2);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        assertNotEquals(withoutTarget, new RevisionService().definitionRevision(definition));

        for (int version : new int[] {1, 2, 3}) {
            var oldCatalog = definition.deepCopy();
            oldCatalog.put("catalogVersion", version);
            assertFalse(new DefinitionSchemaValidator().validate(oldCatalog).isEmpty());
        }
        cohort.put("maxDailyLessonSpread", 0);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        cohort.put("maxDailyLessonSpread", -1);
        assertFalse(new DefinitionSchemaValidator().validate(definition).isEmpty());
        cohort.put("maxDailyLessonSpread", 1.5);
        assertFalse(new DefinitionSchemaValidator().validate(definition).isEmpty());
    }

    @Test
    @DisplayName("Cohort balance UC-1 G5: MVK has the exact daily-load and start-time preference configuration")
    void mvkHasExactBalancedPreferenceConfiguration() throws Exception {
        var definition = JsonSupport.mapper().readTree(Path.of("..", "examples", "mvk.json"));
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        assertEquals(4, definition.path("catalogVersion").intValue());
        var cohorts = definition.path("cohorts");
        assertEquals(List.of("1a", "1b", "1c", "2a", "2b", "2c", "3a", "3b", "4a", "4b",
                        "5a", "5b", "5d", "6a", "6b", "6c", "7a", "7b", "8a", "8b", "9a", "9b", "9c"),
                StreamSupport.stream(cohorts.spliterator(), false)
                        .map(cohort -> cohort.path("id").stringValue()).toList());
        cohorts.forEach(cohort -> {
            assertEquals(1, cohort.path("maxDailyLessonSpread").intValue());
            assertFalse(cohort.has("undesirablePeriodIds"));
        });
        assertEquals(JsonSupport.mapper().readTree("""
                [
                  {"constraintId":"soft.teacher-gap","weight":5},
                  {"constraintId":"soft.cohort-gap","weight":20},
                  {"constraintId":"soft.cohort-late-start","weight":30},
                  {"constraintId":"soft.cohort-week-balance","weight":30},
                  {"constraintId":"soft.non-preferred-room","weight":5}
                ]
                """), definition.path("softConstraintOverrides"));
        assertEquals(List.of("mon-0", "tue-0", "wed-0", "thu-0", "fri-0"),
                StreamSupport.stream(definition.path("reservedPeriodIds").spliterator(), false)
                        .map(period -> period.stringValue()).toList());
    }

    @Test
    @DisplayName("UC-1 ext 2a: schema rejects unknown properties")
    void schemaRejectsUnknownProperties() throws Exception {
        var input = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        input.put("unexpected", true);

        var errors = new DefinitionSchemaValidator().validate(input);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.message().contains("unexpected")));
    }

    @Test
    @DisplayName("UC-1 ext 2a and RULE-3: unsupported versions and coercive scalar types are rejected")
    void schemaAndBindingStayStrict() throws Exception {
        var unsupported = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        unsupported.put("schemaVersion", 2);
        assertFalse(new DefinitionSchemaValidator().validate(unsupported).isEmpty());

        assertThrows(tools.jackson.core.JacksonException.class,
                () -> JsonSupport.mapper().readValue(
                        "{\"schemaVersion\":\"1\"}", SchoolDefinitionDto.class));
    }

    @Test
    @DisplayName("UC-1 G1: school displayName is required, nonblank, Unicode metadata and revision-bearing")
    void schoolDisplayNameContract() throws Exception {
        var valid = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        var missing = valid.deepCopy();
        missing.remove("displayName");
        assertFalse(new DefinitionSchemaValidator().validate(missing).isEmpty());
        var blank = valid.deepCopy();
        blank.put("displayName", "");
        assertFalse(new DefinitionSchemaValidator().validate(blank).isEmpty());
        var unicode = valid.deepCopy();
        unicode.put("displayName", "Õppekool 🎓");
        assertTrue(new DefinitionSchemaValidator().validate(unicode).isEmpty());
        assertNotEquals(
                new RevisionService().definitionRevision(valid),
                new RevisionService().definitionRevision(unicode));
    }

    @Test
    @DisplayName("RULE-3: bundled result schema is valid Draft 2020-12 and resolves offline")
    void resultSchemaLoadsOffline() throws Exception {
        try (InputStream input = ContractTest.class.getResourceAsStream("/schema/result-v1.schema.json")) {
            var schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(input);
            schema.initializeValidators();
        }
    }

    @Test
    @DisplayName("UC-1 G6: RFC 8785 number vector is canonicalized exactly")
    void canonicalizesPublishedNumberVector() throws Exception {
        var input = JsonSupport.mapper().readTree(
                "{\"numbers\":[333333333.33333329,1E30,4.50,2e-3,0.000000000000000000000000001]}");

        assertEquals(
                "{\"numbers\":[333333333.3333333,1e+30,4.5,0.002,1e-27]}",
                JsonSupport.canonicalString(input));
    }

    @Test
    @DisplayName("UC-1 G6: definition revision ignores entity and set input order")
    void definitionRevisionNormalizesOrder() throws Exception {
        var first = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        ((tools.jackson.databind.node.ObjectNode) first.withArray("teachers").get(0))
                .withArray("qualifiedSubjectIds").add("science");
        var second = first.deepCopy();
        var teacher = (tools.jackson.databind.node.ObjectNode) second.withArray("teachers").get(0);
        teacher.withArray("qualifiedSubjectIds").removeAll();
        teacher.withArray("qualifiedSubjectIds").add("science");
        teacher.withArray("qualifiedSubjectIds").add("math");

        var service = new RevisionService();
        assertEquals(service.definitionRevision(first), service.definitionRevision(second));
    }

    @Test
    @DisplayName("RULE-5: SHA-256 is verified against an independently fixed canonical-text digest")
    void hashesCanonicalTextToIndependentDigest() {
        assertEquals(
                "sha256:015abd7f5cc57a2dd94b7590f04ad8084273905ee33ec5cebeae62276a97f862",
                RevisionService.sha256OfCanonicalText("{\"a\":1}"));
    }

    @Test
    @DisplayName("UC-1 G6: timetable revision covers assignments but excludes score and timing metadata")
    void timetableRevisionUsesExactAssignmentScope() throws Exception {
        var assignments = JsonSupport.mapper().createArrayNode();
        assignments.addObject()
                .put("lessonId", "lesson-1")
                .put("subjectId", "math")
                .put("cohortId", "cohort-1")
                .put("teacherId", "teacher-1")
                .put("periodId", "mon-1")
                .put("roomId", "room-1");
        var service = new RevisionService();

        String original = service.timetableRevision(1, "school", "sha256:" + "0".repeat(64), assignments);
        ((tools.jackson.databind.node.ObjectNode) assignments.get(0)).put("periodId", "mon-2");

        assertNotEquals(original,
                service.timetableRevision(1, "school", "sha256:" + "0".repeat(64), assignments));
    }

    private static byte[] resource(String name) throws Exception {
        try (InputStream input = ContractTest.class.getResourceAsStream(name)) {
            return input.readAllBytes();
        }
    }
}
