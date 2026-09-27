package org.schoolkernel.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.contract.DefinitionSchemaValidator;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.RevisionService;
import org.schoolkernel.contract.SchoolDefinitionDto;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class RoomAssignmentContractTest {
    private final DefinitionSchemaValidator schema = new DefinitionSchemaValidator();
    private final DefinitionValidator semantics = new DefinitionValidator();

    @Test
    @DisplayName("Room assignment UC-1 G4/RULE-1: catalog 9 policy variants are versioned, strict and revisioned")
    void versionedPolicyStructure() throws Exception {
        ObjectNode definition = basic();
        String before = new RevisionService().definitionRevision(definition);
        definition.putArray("roomAssignments").addObject()
                .put("id", "math-rooms").put("subjectId", "math")
                .putArray("allowedRoomIds").add("room-1");
        assertTrue(schema.validate(definition).isEmpty());
        assertNotEquals(before, new RevisionService().definitionRevision(definition));

        ObjectNode legacy = definition.deepCopy();
        legacy.put("catalogVersion", 8);
        assertFalse(schema.validate(legacy).isEmpty());

        ObjectNode home = definition.deepCopy();
        ((ObjectNode) home.withArray("roomAssignments").get(0)).remove("allowedRoomIds");
        ((ObjectNode) home.withArray("roomAssignments").get(0)).put("useHomeRoom", true);
        assertTrue(schema.validate(home).isEmpty());

        ObjectNode both = home.deepCopy();
        ((ObjectNode) both.withArray("roomAssignments").get(0)).putArray("allowedRoomIds").add("room-1");
        assertFalse(schema.validate(both).isEmpty());
        ((ObjectNode) both.withArray("roomAssignments").get(0)).remove("useHomeRoom");
        ((ObjectNode) both.withArray("roomAssignments").get(0)).putArray("allowedRoomIds");
        assertFalse(schema.validate(both).isEmpty());

        ObjectNode unknownField = definition.deepCopy();
        ((ObjectNode) unknownField.withArray("roomAssignments").get(0)).put("priority", 1);
        assertFalse(schema.validate(unknownField).isEmpty());
    }

    @Test
    @DisplayName("Room assignment UC-1 main/RULE-2: subject and teacher rules intersect; home room resolves per cohort")
    void selectorScopeIntersectionAndHomeRoom() throws Exception {
        ObjectNode definition = basic();
        definition.withArray("teachers").addObject().put("id", "teacher-2").put("displayName", "Teacher Two")
                .putArray("qualifiedSubjectIds").add("math");
        definition.withArray("rooms").addObject().put("id", "room-2").put("displayName", "Room Two")
                .put("capacity", 25).putArray("capabilityIds");
        definition.withArray("rooms").addObject().put("id", "room-3").put("displayName", "Room Three")
                .put("capacity", 25).putArray("capabilityIds");
        ((ObjectNode) definition.withArray("cohorts").get(0)).put("homeRoomId", "room-2");
        definition.withArray("lessons").addObject().put("id", "lesson-2").put("displayName", "Math 2")
                .put("subjectId", "math").put("cohortId", "cohort-1").put("teacherId", "teacher-2");
        definition.withArray("roomAssignments").addObject().put("id", "subject")
                .put("subjectId", "math").putArray("allowedRoomIds").add("room-1").add("room-2");
        definition.withArray("roomAssignments").addObject().put("id", "teacher")
                .put("subjectId", "math").put("teacherId", "teacher-1")
                .putArray("allowedRoomIds").add("room-2").add("room-3");
        assertTrue(schema.validate(definition).isEmpty());
        SchoolDefinition domain = validate(definition).definition();
        assertEquals(Set.of("room-2"), RoomAssignmentResolver.resolve(domain, domain.lessons().get(0)).allowedRoomIds());
        assertEquals(Set.of("room-1", "room-2"),
                RoomAssignmentResolver.resolve(domain, domain.lessons().get(1)).allowedRoomIds());

        ObjectNode home = definition.deepCopy();
        home.withArray("roomAssignments").addObject().put("id", "home")
                .put("subjectId", "math").put("useHomeRoom", true);
        domain = validate(home).definition();
        assertEquals(Set.of("room-2"), RoomAssignmentResolver.resolve(domain, domain.lessons().get(1)).allowedRoomIds());
    }

    @Test
    @DisplayName("Room assignment UC-1 ext 2a/RULE-2: direct policy contradictions are invalid before search")
    void rejectsDirectContradictions() throws Exception {
        ObjectNode input = basic();
        input.withArray("roomAssignments").addObject().put("id", "home")
                .put("subjectId", "math").put("useHomeRoom", true);
        assertError(input, "room assignment requires the matched cohort to declare a home room", "lesson-1", "home");

        ((ObjectNode) input.withArray("cohorts").get(0)).put("homeRoomId", "room-1");
        assertTrue(validate(input).report().isValid());
        ((ObjectNode) input.withArray("lessons").get(0)).put("roomLock", "room-2");
        input.withArray("rooms").addObject().put("id", "room-2").put("displayName", "Room Two")
                .put("capacity", 25).putArray("capabilityIds");
        assertError(input, "room assignment contradicts the lesson room lock", "lesson-1", "home");

        ((ObjectNode) input.withArray("lessons").get(0)).remove("roomLock");
        input.withArray("roomAssignments").addObject().put("id", "other")
                .put("subjectId", "math").putArray("allowedRoomIds").add("room-2");
        assertError(input, "matching room assignments have no room in common", "lesson-1", "other");

        input.withArray("roomAssignments").removeAll();
        input.withArray("roomAssignments").addObject().put("id", "small")
                .put("subjectId", "math").putArray("allowedRoomIds").add("room-2");
        ((ObjectNode) input.withArray("rooms").get(1)).put("capacity", 10);
        assertError(input, "room assignment has no room with sufficient capacity and required capabilities",
                "lesson-1", "small");
        ((ObjectNode) input.withArray("rooms").get(1)).put("capacity", 25);
        ((ObjectNode) input.withArray("lessons").get(0)).putArray("requiredRoomCapabilityIds").add("lab");
        assertError(input, "room assignment has no room with sufficient capacity and required capabilities",
                "lesson-1", "small");

        ObjectNode unknown = basic();
        unknown.withArray("roomAssignments").addObject().put("id", "future")
                .put("subjectId", "not-declared").putArray("allowedRoomIds").add("room-1");
        assertError(unknown, "unknown subject reference", "future", "not-declared");

        ObjectNode unknownTeacher = basic();
        unknownTeacher.withArray("roomAssignments").addObject().put("id", "teacher")
                .put("subjectId", "math").put("teacherId", "not-declared")
                .putArray("allowedRoomIds").add("room-1");
        assertError(unknownTeacher, "unknown teacher reference", "teacher", "not-declared");

        ObjectNode unknownRoom = basic();
        unknownRoom.withArray("roomAssignments").addObject().put("id", "room")
                .put("subjectId", "math").putArray("allowedRoomIds").add("not-declared");
        assertError(unknownRoom, "unknown room reference", "room", "not-declared");

        ObjectNode duplicate = basic();
        duplicate.withArray("roomAssignments").addObject().put("id", "same")
                .put("subjectId", "math").putArray("allowedRoomIds").add("room-1");
        duplicate.withArray("roomAssignments").addObject().put("id", "same")
                .put("subjectId", "math").putArray("allowedRoomIds").add("room-1");
        assertError(duplicate, "duplicate ID in roomAssignments", "same");

        ObjectNode curator = basic();
        ((ObjectNode) curator.withArray("subjects").get(0)).put("curatorLesson", true);
        ((ObjectNode) curator.withArray("cohorts").get(0)).put("curatorTeacherId", "teacher-1")
                .put("homeRoomId", "room-1");
        curator.withArray("rooms").addObject().put("id", "room-2").put("displayName", "Room Two")
                .put("capacity", 25).putArray("capabilityIds");
        curator.withArray("roomAssignments").addObject().put("id", "away")
                .put("subjectId", "math").putArray("allowedRoomIds").add("room-2");
        assertError(curator, "room assignment contradicts the curator home room", "lesson-1", "away");

        ObjectNode future = basic();
        future.withArray("subjects").addObject().put("id", "future").put("displayName", "Future");
        future.withArray("roomAssignments").addObject().put("id", "future")
                .put("subjectId", "future").putArray("allowedRoomIds").add("room-1");
        assertTrue(validate(future).report().isValid());
    }

    @Test
    @DisplayName("Room assignment UC-1 normative MVK/RULE-5: exact seven policies and synthetic A233 are present")
    void mvkPolicyValues() throws Exception {
        JsonNode mvk = JsonSupport.mapper().readTree(Path.of("..", "examples", "mvk.json"));
        assertTrue(schema.validate(mvk).isEmpty());
        assertEquals(9, mvk.path("catalogVersion").intValue());
        assertEquals(JsonSupport.mapper().readTree("""
                [
                  {"id":"mvk-history-a231","subjectId":"ajalugu","allowedRoomIds":["a231"]},
                  {"id":"mvk-piret-maths-b212","subjectId":"matemaatika","teacherId":"piret-noor","allowedRoomIds":["b212"]},
                  {"id":"mvk-heiki-literature-a209","subjectId":"kirjandus","teacherId":"heiki-raudla","allowedRoomIds":["a209"]},
                  {"id":"mvk-chemistry-a233","subjectId":"keemia","allowedRoomIds":["a233"]},
                  {"id":"mvk-physics-a223","subjectId":"fuusika","allowedRoomIds":["a223-lab"]},
                  {"id":"mvk-physical-education-halls","subjectId":"kehaline-kasvatus","allowedRoomIds":["kk1","kk2","kk3"]},
                  {"id":"mvk-study-support-home","subjectId":"opiabi","useHomeRoom":true}
                ]
                """), mvk.path("roomAssignments"));
        JsonNode a233 = java.util.stream.StreamSupport.stream(mvk.path("rooms").spliterator(), false)
                .filter(room -> room.path("id").stringValue().equals("a233")).findFirst().orElseThrow();
        assertEquals(JsonSupport.mapper().readTree("""
                {"id":"a233","displayName":"A233","capacity":30,"capabilityIds":["science-lab"]}
                """), a233);
        assertEquals(42, mvk.path("rooms").size());
        SchoolDefinition domain = validate((ObjectNode) mvk).definition();
        assertTrue(validate((ObjectNode) mvk).report().isValid());
        var counts = new java.util.HashMap<String, Integer>();
        int musicLessons = 0;
        for (var lesson : domain.lessons()) {
            var resolved = RoomAssignmentResolver.resolve(domain, lesson);
            resolved.policyIds().forEach(id -> counts.merge(id, 1, Integer::sum));
            if (lesson.subjectId().equals("muusikaopetus")) {
                musicLessons++;
                assertEquals(Set.of("mu"), lesson.preferredRoomIds());
                assertEquals(Set.of("music-room"), lesson.requiredRoomCapabilityIds());
                assertFalse(resolved.applies());
            }
            if (resolved.applies() && resolved.allowedRoomIds().size() == 1) {
                assertEquals(resolved.allowedRoomIds(), lesson.preferredRoomIds(), lesson.id());
            }
        }
        assertEquals(29, musicLessons);
        assertEquals(java.util.Map.of(
                "mvk-history-a231", 23, "mvk-piret-maths-b212", 23,
                "mvk-heiki-literature-a209", 10, "mvk-chemistry-a233", 10,
                "mvk-physics-a223", 10, "mvk-physical-education-halls", 18,
                "mvk-study-support-home", 18), counts);
    }

    private DefinitionValidator.Outcome validate(ObjectNode input) throws Exception {
        return semantics.validateForPlan(JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class));
    }

    private void assertError(ObjectNode input, String message, String... ids) throws Exception {
        assertTrue(schema.validate(input).isEmpty(), schema.validate(input).toString());
        var report = validate(input).report();
        assertFalse(report.isValid());
        assertTrue(report.errors().stream().anyMatch(error -> error.message().equals(message)
                && error.entityIds().containsAll(List.of(ids))), report.toString());
    }

    private static ObjectNode basic() throws Exception {
        ObjectNode input = (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "valid-plan.json"));
        input.put("catalogVersion", 9);
        return input;
    }
}
