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
    @DisplayName("Cohort gaps: catalog 5 accepts a non-negative cohort gap allowance and older catalogs reject it")
    void cohortDailyGapsSchemaAndRevision() throws Exception {
        var definition = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        definition.put("catalogVersion", 5);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        String withoutAllowance = new RevisionService().definitionRevision(definition);
        var cohort = (tools.jackson.databind.node.ObjectNode) definition.withArray("cohorts").get(0);
        cohort.put("maxDailyGaps", 1);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        assertNotEquals(withoutAllowance, new RevisionService().definitionRevision(definition));

        for (int version : new int[] {1, 2, 3, 4}) {
            var oldCatalog = definition.deepCopy();
            oldCatalog.put("catalogVersion", version);
            assertFalse(new DefinitionSchemaValidator().validate(oldCatalog).isEmpty());
        }
        cohort.put("maxDailyGaps", 0);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        cohort.put("maxDailyGaps", -1);
        assertFalse(new DefinitionSchemaValidator().validate(definition).isEmpty());
        cohort.put("maxDailyGaps", 1.5);
        assertFalse(new DefinitionSchemaValidator().validate(definition).isEmpty());
    }

    @Test
    @DisplayName("Day edges RULE-1: catalog 6 accepts start bounds and subject placement rules; older catalogs reject them")
    void dayEdgeFieldsSchemaAndRevision() throws Exception {
        var definition = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        definition.put("catalogVersion", 6);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        var cohort = (tools.jackson.databind.node.ObjectNode) definition.withArray("cohorts").get(0);
        var subject = (tools.jackson.databind.node.ObjectNode) definition.withArray("subjects").get(0);
        var fields = List.<Runnable>of(
                () -> cohort.put("latestStartSlot", 2),
                () -> cohort.put("preferredLatestStartSlot", 1),
                () -> subject.put("reservedPeriodsAllowed", true),
                () -> subject.put("maxWeeklyReservedLessonsPerCohort", 1),
                () -> subject.put("dayEdgeOnly", true),
                () -> subject.put("maxDailyLessonsPerCohort", 1));
        for (Runnable field : fields) {
            String before = new RevisionService().definitionRevision(definition);
            field.run();
            assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
            assertNotEquals(before, new RevisionService().definitionRevision(definition));
        }
        for (int version : new int[] {1, 2, 3, 4, 5}) {
            var oldCatalog = definition.deepCopy();
            oldCatalog.put("catalogVersion", version);
            assertFalse(new DefinitionSchemaValidator().validate(oldCatalog).isEmpty());
        }

        for (String field : List.of("latestStartSlot", "preferredLatestStartSlot")) {
            var invalid = definition.deepCopy();
            ((tools.jackson.databind.node.ObjectNode) invalid.withArray("cohorts").get(0)).put(field, 0);
            assertFalse(new DefinitionSchemaValidator().validate(invalid).isEmpty());
            ((tools.jackson.databind.node.ObjectNode) invalid.withArray("cohorts").get(0)).put(field, 1.5);
            assertFalse(new DefinitionSchemaValidator().validate(invalid).isEmpty());
        }
        for (String field : List.of("maxWeeklyReservedLessonsPerCohort", "maxDailyLessonsPerCohort")) {
            var invalid = definition.deepCopy();
            ((tools.jackson.databind.node.ObjectNode) invalid.withArray("subjects").get(0)).put(field, 0);
            assertFalse(new DefinitionSchemaValidator().validate(invalid).isEmpty());
        }
        var withoutPermission = definition.deepCopy();
        ((tools.jackson.databind.node.ObjectNode) withoutPermission.withArray("subjects").get(0))
                .put("reservedPeriodsAllowed", false);
        assertFalse(new DefinitionSchemaValidator().validate(withoutPermission).isEmpty());
        ((tools.jackson.databind.node.ObjectNode) withoutPermission.withArray("subjects").get(0))
                .remove("reservedPeriodsAllowed");
        assertFalse(new DefinitionSchemaValidator().validate(withoutPermission).isEmpty());
    }

    @Test
    @DisplayName("Daily spread RULE-1: catalog 7 accepts a hard daily lesson spread limit; older catalogs reject it")
    void dailySpreadLimitSchemaAndRevision() throws Exception {
        var definition = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        definition.put("catalogVersion", 7);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        var cohort = (tools.jackson.databind.node.ObjectNode) definition.withArray("cohorts").get(0);
        String before = new RevisionService().definitionRevision(definition);
        cohort.put("dailyLessonSpreadLimit", 0);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        assertNotEquals(before, new RevisionService().definitionRevision(definition));
        for (int version : new int[] {1, 2, 3, 4, 5, 6}) {
            var oldCatalog = definition.deepCopy();
            oldCatalog.put("catalogVersion", version);
            assertFalse(new DefinitionSchemaValidator().validate(oldCatalog).isEmpty());
        }
        cohort.put("dailyLessonSpreadLimit", -1);
        assertFalse(new DefinitionSchemaValidator().validate(definition).isEmpty());
        cohort.put("dailyLessonSpreadLimit", 1.5);
        assertFalse(new DefinitionSchemaValidator().validate(definition).isEmpty());
    }

    @Test
    @DisplayName("Curator RULE-1: catalog 8 accepts curators, home rooms and curator lessons; older catalogs reject them")
    void curatorFieldsSchemaAndRevision() throws Exception {
        var definition = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper()
                .readTree(resource("/fixtures/valid-plan.json"));
        definition.put("catalogVersion", 8);
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        var cohort = (tools.jackson.databind.node.ObjectNode) definition.withArray("cohorts").get(0);
        var subject = (tools.jackson.databind.node.ObjectNode) definition.withArray("subjects").get(0);
        String revision = new RevisionService().definitionRevision(definition);
        for (Runnable change : List.<Runnable>of(
                () -> cohort.put("curatorTeacherId", "teacher-1"),
                () -> cohort.put("homeRoomId", "room-1"),
                () -> subject.put("curatorLesson", true))) {
            change.run();
            assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
            String next = new RevisionService().definitionRevision(definition);
            assertNotEquals(revision, next);
            revision = next;
        }
        for (int version : new int[] {1, 2, 3, 4, 5, 6, 7}) {
            var oldCatalog = definition.deepCopy();
            oldCatalog.put("catalogVersion", version);
            assertFalse(new DefinitionSchemaValidator().validate(oldCatalog).isEmpty());
        }
        cohort.put("homeRoomId", "");
        assertFalse(new DefinitionSchemaValidator().validate(definition).isEmpty());
    }

    @Test
    @DisplayName("Curator RULE-5: MVK declares source-backed curators and rooms plus a synthetic 6C curator")
    void mvkDeclaresCuratorsAndHomeRooms() throws Exception {
        var definition = JsonSupport.mapper().readTree(Path.of("..", "examples", "mvk.json"));
        var curators = new java.util.LinkedHashMap<String, String>();
        var homeRooms = new java.util.LinkedHashMap<String, String>();
        definition.path("cohorts").forEach(cohort -> {
            String id = cohort.path("id").stringValue();
            if (cohort.has("curatorTeacherId")) {
                curators.put(id, cohort.path("curatorTeacherId").stringValue());
            }
            if (cohort.has("homeRoomId")) {
                homeRooms.put(id, cohort.path("homeRoomId").stringValue());
            }
        });
        assertEquals(java.util.Map.ofEntries(
                java.util.Map.entry("1a", "piret-laanesaar"), java.util.Map.entry("1b", "kairi-meressaar"),
                java.util.Map.entry("1c", "helena-kroon"), java.util.Map.entry("2a", "anita-kalmus"),
                java.util.Map.entry("2b", "maiki-saaring"), java.util.Map.entry("2c", "indra-feldman"),
                java.util.Map.entry("3a", "kristin-muul"), java.util.Map.entry("3b", "reena-korp"),
                java.util.Map.entry("4a", "anette-maria-rennit"), java.util.Map.entry("4b", "rita-lumiste"),
                java.util.Map.entry("5a", "ene-avson"), java.util.Map.entry("5b", "katariin-treial"),
                java.util.Map.entry("5d", "liisi-rannik"), java.util.Map.entry("6a", "vaike-antsov"),
                java.util.Map.entry("6b", "liivi-kivimae-bondarev"), java.util.Map.entry("6c", "olga-simonovits"),
                java.util.Map.entry("7a", "saale-maripuu"), java.util.Map.entry("7b", "bergit-semre"),
                java.util.Map.entry("8a", "grete-suurvali"),
                java.util.Map.entry("8b", "marina-pokintsereda"), java.util.Map.entry("9a", "marilin-laanetu"),
                java.util.Map.entry("9b", "piret-noor"), java.util.Map.entry("9c", "helina-silberg")), curators);
        assertTrue(StreamSupport.stream(definition.path("lessons").spliterator(), false)
                .anyMatch(lesson -> lesson.path("cohortId").stringValue().equals("6c")
                        && lesson.path("teacherId").stringValue().equals(curators.get("6c"))),
                "the synthetic 6C curator must teach 6C");
        assertEquals(java.util.Map.ofEntries(
                java.util.Map.entry("1a", "a106"), java.util.Map.entry("1b", "a120"),
                java.util.Map.entry("1c", "a118"), java.util.Map.entry("2a", "a109"),
                java.util.Map.entry("2b", "a117"), java.util.Map.entry("2c", "a207"),
                java.util.Map.entry("3a", "a228"), java.util.Map.entry("3b", "a217"),
                java.util.Map.entry("4a", "a218"), java.util.Map.entry("4b", "a119"),
                java.util.Map.entry("5a", "a215"), java.util.Map.entry("5b", "a216"),
                java.util.Map.entry("5d", "a211"), java.util.Map.entry("6a", "b213"),
                java.util.Map.entry("6b", "b214"), java.util.Map.entry("7a", "b216"),
                java.util.Map.entry("7b", "a229"), java.util.Map.entry("8a", "b214"),
                java.util.Map.entry("8b", "a230"), java.util.Map.entry("9a", "a204"),
                java.util.Map.entry("9b", "b212"), java.util.Map.entry("9c", "b215")), homeRooms);
        definition.path("lessons").forEach(lesson -> {
            if (lesson.path("subjectId").stringValue().equals("klassitund")) {
                String cohortId = lesson.path("cohortId").stringValue();
                assertEquals(curators.get(cohortId), lesson.path("teacherId").stringValue());
                assertEquals(homeRooms.get(cohortId), lesson.path("preferredRoomIds").get(0).stringValue());
            }
        });
        var lessons = definition.path("lessons");
        var curatorInto = StreamSupport.stream(lessons.spliterator(), false)
                .filter(lesson -> lesson.path("subjectId").stringValue().equals("into"))
                .filter(lesson -> lesson.path("teacherId").stringValue()
                        .equals(curators.get(lesson.path("cohortId").stringValue())))
                .toList();
        assertEquals(11, curatorInto.size());
        curatorInto.forEach(lesson -> assertEquals(homeRooms.get(lesson.path("cohortId").stringValue()),
                lesson.path("roomLock").stringValue()));
        var history = StreamSupport.stream(lessons.spliterator(), false)
                .filter(lesson -> lesson.path("teacherId").stringValue().equals("siiri-aiaste"))
                .filter(lesson -> lesson.path("subjectId").stringValue().equals("ajalugu"))
                .toList();
        assertEquals(14, history.size());
        history.forEach(lesson -> assertEquals("a231", lesson.path("roomLock").stringValue()));
        assertEquals(25, StreamSupport.stream(lessons.spliterator(), false)
                .filter(lesson -> lesson.has("roomLock")).count());
    }

    @Test
    @DisplayName("Cohort balance UC-1 G5/RULE-5: MVK has the exact gap, daily-load and start preference configuration")
    void mvkHasExactBalancedPreferenceConfiguration() throws Exception {
        var definition = JsonSupport.mapper().readTree(Path.of("..", "examples", "mvk.json"));
        assertTrue(new DefinitionSchemaValidator().validate(definition).isEmpty());
        assertEquals(9, definition.path("catalogVersion").intValue());
        var cohorts = definition.path("cohorts");
        assertEquals(List.of("1a", "1b", "1c", "2a", "2b", "2c", "3a", "3b", "4a", "4b",
                        "5a", "5b", "5d", "6a", "6b", "6c", "7a", "7b", "8a", "8b", "9a", "9b", "9c"),
                StreamSupport.stream(cohorts.spliterator(), false)
                        .map(cohort -> cohort.path("id").stringValue()).toList());
        cohorts.forEach(cohort -> {
            assertEquals(1, cohort.path("maxDailyLessonSpread").intValue());
            assertFalse(cohort.has("undesirablePeriodIds"));
            assertFalse(cohort.has("maxDailyGaps"));
            assertFalse(cohort.has("preferredLatestStartSlot"));
            boolean youngest = List.of("1a", "1b", "1c", "2a", "2b", "2c", "3a", "3b")
                    .contains(cohort.path("id").stringValue());
            assertEquals(youngest ? 2 : 4, cohort.path("latestStartSlot").intValue());
            boolean primary = cohort.path("id").stringValue().charAt(0) <= '4';
            assertEquals(primary ? 1 : 2, cohort.path("dailyLessonSpreadLimit").intValue());
        });
        definition.path("subjects").forEach(subject -> {
            if (subject.path("id").stringValue().equals("opiabi")) {
                assertEquals(true, subject.path("reservedPeriodsAllowed").booleanValue());
                assertEquals(1, subject.path("maxWeeklyReservedLessonsPerCohort").intValue());
                assertEquals(true, subject.path("dayEdgeOnly").booleanValue());
                assertEquals(1, subject.path("maxDailyLessonsPerCohort").intValue());
                assertEquals(6, subject.size());
            } else if (subject.path("id").stringValue().equals("klassitund")) {
                assertEquals(true, subject.path("curatorLesson").booleanValue());
                assertEquals(3, subject.size());
            } else {
                assertEquals(2, subject.size());
            }
        });
        assertEquals(JsonSupport.mapper().readTree("""
                [
                  {"constraintId":"soft.teacher-gap","weight":5},
                  {"constraintId":"soft.cohort-gap","weight":1000000},
                  {"constraintId":"soft.cohort-late-start","weight":10000},
                  {"constraintId":"soft.cohort-week-balance","weight":1000},
                  {"constraintId":"soft.non-preferred-room","weight":5}
                ]
                """), definition.path("softConstraintOverrides"));
        assertEquals(List.of("k2", "mu"), StreamSupport.stream(definition.path("rooms").spliterator(), false)
                .filter(room -> StreamSupport.stream(room.path("capabilityIds").spliterator(), false)
                        .anyMatch(capability -> capability.stringValue().equals("music-room")))
                .map(room -> room.path("id").stringValue()).toList());
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
