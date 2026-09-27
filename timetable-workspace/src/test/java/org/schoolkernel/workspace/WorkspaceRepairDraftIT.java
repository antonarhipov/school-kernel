package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = WorkspaceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkspaceRepairDraftIT {
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("repair_draft").withUsername("workspace").withPassword("workspace");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("workspace.kernel-executable", () -> ROOT.resolve("school-kernel").toString());
    }

    @LocalServerPort int port;
    @Autowired JdbcClient jdbc;
    @Autowired RepairDraftService repairs;
    @Autowired WorkspaceRepository repository;

    private HttpClient client;

    @BeforeEach
    void reset() throws Exception {
        storeAccepted(false);
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test
    @DisplayName("UC-4 main, G1/G5, RULE-15: HTTP stages weekly teacher unavailability, exact direct effects, and durable independent pins")
    void preparesDurableRepairDraftWithoutChangingAcceptedBundle() throws Exception {
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();
        JsonNode started = body(command("POST", "/api/repair-draft", session(), """
                {"resourceType":"TEACHER","resourceId":"teacher-alex","periodIds":["mon-1"]}
                """));
        assertEquals("REPAIR_DRAFT", started.path("state").stringValue());
        assertTrue(started.path("acceptedTimetable").booleanValue());
        JsonNode draft = started.path("workspace").path("repairDraft");
        assertEquals(JSON.readTree("[\"lesson-math-1\"]"), draft.path("directEffectLessonIds"));
        assertTrue(draft.path("intentRevision").stringValue().matches("sha256:[0-9a-f]{64}"));
        assertTrue(draft.path("directEffectRevision").stringValue().matches("sha256:[0-9a-f]{64}"));
        assertTrue(draft.path("readyToSolve").booleanValue());

        HttpResponse<String> pinnedResponse = commandMinimal("PATCH", "/api/repair-draft", session(), """
                {"action":"PIN","lessonId":"lesson-science-1","dimensions":["PERIOD","ROOM"]}
                """);
        assertEquals("\"ws-9\"", pinnedResponse.headers().firstValue("ETag").orElseThrow());
        JsonNode pin = body(pinnedResponse).path("repairDraft").path("intent").path("pins").get(0);
        assertEquals("lesson-science-1", pin.path("lessonId").stringValue());
        assertEquals("INDIVIDUAL", pin.path("periodSources").get(0).stringValue());
        assertEquals("INDIVIDUAL", pin.path("roomSources").get(0).stringValue());
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));

        ObjectNode compiled = repairs.compiledDefinition(repository.load().document());
        assertEquals(1, acceptedBefore.path("definition").path("catalogVersion").intValue());
        assertEquals(6, compiled.path("catalogVersion").intValue());
        assertEquals("sha256:accepted-input", compiled.path("basedOnRevision").stringValue());
        assertEquals(JSON.readTree("[\"mon-2\",\"mon-3\"]"),
                compiled.path("teachers").get(0).path("availablePeriodIds"));
        assertEquals("mon-2", compiled.path("lessons").get(1).path("periodLock").stringValue());
        assertEquals("room-101", compiled.path("lessons").get(1).path("roomLock").stringValue());
        assertFalse(acceptedBefore.path("definition").path("teachers").get(0).has("availablePeriodIds"));
    }

    @Test
    @DisplayName("Reserved periods UC-1 extension 1a: repair successor retains reservation and accepted predecessor")
    void compilesReservationAwareRepairWithoutChangingAcceptedPair() throws Exception {
        ObjectNode document = (ObjectNode) storedDocument();
        ((ObjectNode) document.path("acceptedBaseline").path("definition"))
                .putArray("reservedPeriodIds").add("mon-3");
        jdbc.sql("UPDATE workspace_aggregate SET document=CAST(:document AS jsonb) WHERE workspace_id=1")
                .param("document", JSON.writeValueAsString(document)).update();
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();

        JsonNode started = body(command("POST", "/api/repair-draft", session(), teacherUnavailable("mon-1")));
        assertEquals("REPAIR_DRAFT", started.path("state").stringValue());
        ObjectNode successor = repairs.compiledDefinition(repository.load().document());

        assertEquals(JSON.readTree("[\"mon-3\"]"), successor.path("reservedPeriodIds"));
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
        assertEquals(JSON.readTree("[\"mon-2\",\"mon-3\"]"),
                successor.path("teachers").get(0).path("availablePeriodIds"),
                "reservation remains separate from resource availability");
    }

    @Test
    @DisplayName("Cohort balance UC-1 extension 1a: HTTP repair retains a configured cohort target and accepted pair")
    void compilesCohortDailySpreadWithoutChangingAcceptedPair() throws Exception {
        ObjectNode document = (ObjectNode) storedDocument();
        ObjectNode definition = (ObjectNode) document.path("acceptedBaseline").path("definition");
        definition.put("catalogVersion", 4);
        ((ObjectNode) definition.path("cohorts").get(0)).put("maxDailyLessonSpread", 2);
        jdbc.sql("UPDATE workspace_aggregate SET document=CAST(:document AS jsonb) WHERE workspace_id=1")
                .param("document", JSON.writeValueAsString(document)).update();
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();

        JsonNode started = body(command("POST", "/api/repair-draft", session(), teacherUnavailable("mon-1")));
        assertEquals("REPAIR_DRAFT", started.path("state").stringValue());
        ObjectNode compiled = repairs.compiledDefinition(repository.load().document());
        assertEquals(6, compiled.path("catalogVersion").intValue());
        assertEquals(2, compiled.path("cohorts").get(0).path("maxDailyLessonSpread").intValue());
        assertEquals("sha256:accepted-input", compiled.path("basedOnRevision").stringValue());
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
    }

    @Test
    @DisplayName("UC-4 room journey and RULE-15: omitted room availability is materialized and a room pin conflicts only at the unavailable period")
    void stagesRoomUnavailabilityAndMaterializesRemainingPeriods() throws Exception {
        JsonNode started = body(command("POST", "/api/repair-draft", session(), """
                {"resourceType":"ROOM","resourceId":"room-101","periodIds":["mon-2"]}
                """));
        JsonNode draft = started.path("workspace").path("repairDraft");
        assertEquals(JSON.readTree("[\"lesson-science-1\"]"), draft.path("directEffectLessonIds"));
        assertTrue(draft.path("readyToSolve").booleanValue());

        JsonNode periodOnly = body(command("PATCH", "/api/repair-draft", session(), """
                {"action":"PIN","lessonId":"lesson-science-1","dimensions":["PERIOD"]}
                """));
        assertTrue(periodOnly.path("workspace").path("repairDraft").path("readyToSolve").booleanValue(),
                "A period pin does not force the unavailable room");
        JsonNode roomPinned = body(command("PATCH", "/api/repair-draft", session(), """
                {"action":"PIN","lessonId":"lesson-science-1","dimensions":["ROOM"]}
                """));
        assertFalse(roomPinned.path("workspace").path("repairDraft").path("readyToSolve").booleanValue());
        ObjectNode compiled = repairs.compiledDefinition(repository.load().document());
        assertEquals(JSON.readTree("[\"mon-1\",\"mon-3\"]"),
                compiled.path("rooms").get(0).path("availablePeriodIds"));
        assertEquals("mon-2", compiled.path("lessons").get(1).path("periodLock").stringValue());
        assertEquals("room-101", compiled.path("lessons").get(1).path("roomLock").stringValue());
    }

    @Test
    @DisplayName("UC-4 extensions 4a/7a and G4: contradictory pin and policy lock are explicit blocking conflicts until resolved")
    void blocksConflictingPinsWithoutChoosingAWinner() throws Exception {
        command("POST", "/api/repair-draft", session(), teacherUnavailable("mon-1"));
        JsonNode conflicting = body(command("PATCH", "/api/repair-draft", session(), """
                {"action":"PIN","lessonId":"lesson-math-1","dimensions":["PERIOD"]}
                """));
        JsonNode draft = conflicting.path("workspace").path("repairDraft");
        assertFalse(draft.path("readyToSolve").booleanValue());
        assertEquals("lesson-math-1", draft.path("conflicts").get(0).path("lessonId").stringValue());
        assertEquals("PIN_CONTRADICTS_UNAVAILABILITY", draft.path("conflicts").get(0).path("code").stringValue());
        JsonNode beforeRefusal = storedDocument().deepCopy();
        long versionBeforeRefusal = jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1")
                .query(Long.class).single();
        HttpResponse<String> refusedRun = command("POST", "/api/runs", session(), "{\"limit\":\"PT30S\"}");
        assertEquals(409, refusedRun.statusCode(), "Workbench layout UC-2 ext 7a: conflict is refused below the UI");
        assertEquals("DRAFT_CONFLICT", body(refusedRun).path("code").stringValue());
        assertEquals(beforeRefusal, storedDocument(), "conflict refusal preserves Current and the exact Draft");
        assertEquals(versionBeforeRefusal, jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1")
                .query(Long.class).single());
        assertFalse(storedDocument().has("run"));
        assertFalse(storedDocument().has("proposal"));

        JsonNode resolved = body(command("PATCH", "/api/repair-draft", session(), """
                {"action":"UNPIN","lessonId":"lesson-math-1","dimensions":["PERIOD"]}
                """));
        assertTrue(resolved.path("workspace").path("repairDraft").path("readyToSolve").booleanValue());
        assertTrue(resolved.path("workspace").path("repairDraft").path("conflicts").isEmpty());

        storeAccepted(true);
        JsonNode policy = body(command("POST", "/api/repair-draft", session(), teacherUnavailable("mon-1")));
        assertFalse(policy.path("workspace").path("repairDraft").path("readyToSolve").booleanValue());
        assertEquals("lesson-math-1", policy.path("workspace").path("repairDraft").path("conflicts").get(0).path("lessonId").stringValue());
    }

    @Test
    @DisplayName("UC-4 main 5-6, extensions 5a/6a, G2, RULE-16: bulk preview is immutable, cancellable, confirmable, and exactly undoable")
    void previewsConfirmsAndUndoesExactBulkSnapshot() throws Exception {
        command("POST", "/api/repair-draft", session(), teacherUnavailable("mon-1"));
        Session previewSession = session();
        HttpResponse<String> previewResponse = command("POST", "/api/repair-draft/bulk-pin-preview", previewSession, """
                {"scope":"UNAFFECTED","dimensions":["PERIOD","ROOM"]}
                """);
        assertEquals(200, previewResponse.statusCode());
        JsonNode preview = body(previewResponse);
        assertEquals(1, preview.path("count").intValue());
        assertEquals("lesson-science-1", preview.path("lessonIds").get(0).stringValue());
        assertTrue(repository.load().document().path("repairDraft").path("intent").path("pins").isEmpty(),
                "Cancelling a preview means making no confirmation request and mutates nothing");

        ObjectNode confirmation = JSON.createObjectNode().put("action", "CONFIRM_BULK_PIN");
        confirmation.set("preview", preview);
        JsonNode confirmed = body(command("PATCH", "/api/repair-draft", session(), JSON.writeValueAsString(confirmation)));
        JsonNode action = confirmed.path("workspace").path("repairDraft").path("intent").path("bulkActions").get(0);
        assertEquals(preview.path("previewId"), action.path("id"));
        assertEquals(preview.path("lessonIds"), action.path("lessonIds"));

        command("PATCH", "/api/repair-draft", session(), """
                {"action":"PIN","lessonId":"lesson-science-1","dimensions":["ROOM"]}
                """);
        String bulkId = preview.path("previewId").stringValue();
        JsonNode undone = body(command("PATCH", "/api/repair-draft", session(),
                "{\"action\":\"UNDO_BULK_PIN\",\"bulkActionId\":\"" + bulkId + "\"}"));
        JsonNode retained = undone.path("workspace").path("repairDraft").path("intent").path("pins").get(0);
        assertTrue(retained.path("periodSources").isEmpty());
        assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), retained.path("roomSources"));
        assertTrue(undone.path("workspace").path("repairDraft").path("intent").path("bulkActions").isEmpty());
    }

    @Test
    @DisplayName("UC-4 RULE-16: day and class previews are deterministic and independent of focused-view filters")
    void selectsCompleteDayAndClassSnapshotsRegardlessOfViewFilters() throws Exception {
        command("POST", "/api/repair-draft", session(), teacherUnavailable("mon-3"));
        Session draft = session();
        HttpResponse<String> dayResponse = command("POST", "/api/repair-draft/bulk-pin-preview", draft, """
                {"scope":"DAY","scopeId":"MONDAY","dimensions":["ROOM"],
                 "viewFilter":{"lessonId":"lesson-math-1"}}
                """);
        assertEquals(draft.etag(), dayResponse.headers().firstValue("ETag").orElseThrow());
        JsonNode day = body(dayResponse);
        assertEquals(JSON.readTree("[\"lesson-math-1\",\"lesson-science-1\"]"), day.path("lessonIds"));
        assertEquals(2, day.path("count").intValue());

        JsonNode byClass = body(command("POST", "/api/repair-draft/bulk-pin-preview", draft, """
                {"scope":"CLASS","scopeId":"cohort-7a","dimensions":["PERIOD"],
                 "viewFilter":{"lessonId":"lesson-science-1"}}
                """));
        assertEquals(JSON.readTree("[\"lesson-math-1\",\"lesson-science-1\"]"), byClass.path("lessonIds"));
        assertEquals(2, byClass.path("count").intValue());

        JsonNode empty = body(command("POST", "/api/repair-draft/bulk-pin-preview", draft, """
                {"scope":"DAY","scopeId":"TUESDAY","dimensions":["PERIOD","ROOM"]}
                """));
        assertEquals(0, empty.path("count").intValue());
        ObjectNode confirmation = JSON.createObjectNode().put("action", "CONFIRM_BULK_PIN");
        confirmation.set("preview", empty);
        JsonNode confirmed = body(command("PATCH", "/api/repair-draft", draft,
                JSON.writeValueAsString(confirmation)));
        assertTrue(confirmed.path("workspace").path("repairDraft").path("intent").path("pins").isEmpty());
        assertTrue(confirmed.path("workspace").path("repairDraft").path("intent")
                .path("bulkActions").get(0).path("lessonIds").isEmpty());
    }

    @Test
    @DisplayName("UC-4 G2 and RULE-16: altered and stale previews cannot change the confirmed immutable snapshot")
    void refusesAlteredOrStaleBulkPreviewsWithoutMutation() throws Exception {
        command("POST", "/api/repair-draft", session(), teacherUnavailable("mon-1"));
        Session draft = session();
        ObjectNode altered = (ObjectNode) body(command("POST", "/api/repair-draft/bulk-pin-preview", draft, """
                {"scope":"UNAFFECTED","dimensions":["ROOM"]}
                """));
        ((tools.jackson.databind.node.ArrayNode) altered.path("lessonIds"))
                .set(0, JSON.getNodeFactory().textNode("lesson-math-1"));
        ObjectNode alteredConfirmation = JSON.createObjectNode().put("action", "CONFIRM_BULK_PIN");
        alteredConfirmation.set("preview", altered);
        JsonNode beforeAltered = storedDocument();
        HttpResponse<String> refusedAltered = command("PATCH", "/api/repair-draft", draft,
                JSON.writeValueAsString(alteredConfirmation));
        assertEquals(412, refusedAltered.statusCode());
        assertEquals(beforeAltered, storedDocument());

        ObjectNode stale = (ObjectNode) body(command("POST", "/api/repair-draft/bulk-pin-preview", draft, """
                {"scope":"UNAFFECTED","dimensions":["PERIOD"]}
                """));
        command("PATCH", "/api/repair-draft", draft, """
                {"action":"STAGE_UNAVAILABILITY","resourceType":"TEACHER",
                 "resourceId":"teacher-alex","periodIds":["mon-2"]}
                """);
        ObjectNode staleConfirmation = JSON.createObjectNode().put("action", "CONFIRM_BULK_PIN");
        staleConfirmation.set("preview", stale);
        JsonNode beforeStale = storedDocument();
        HttpResponse<String> refusedStale = command("PATCH", "/api/repair-draft", session(),
                JSON.writeValueAsString(staleConfirmation));
        assertEquals(412, refusedStale.statusCode());
        assertEquals(beforeStale, storedDocument());
    }

    @Test
    @DisplayName("UC-4 extensions 1a/2a/7b/7c and minimal guarantee: unsupported intent, no direct effect, restart, and confirmed discard are safe")
    void handlesExtensionsAndRestoresDurableDraft() throws Exception {
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();
        HttpResponse<String> unsupported = command("POST", "/api/repair-draft", session(), """
                {"action":"TEACHER_REASSIGNMENT","resourceType":"TEACHER","resourceId":"teacher-alex","periodIds":["mon-3"]}
                """);
        assertEquals(422, unsupported.statusCode());
        assertEquals("ACCEPTED_BASELINE", lifecycle());
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
        JsonNode beforeInvalid = storedDocument().deepCopy();
        long versionBeforeInvalid = jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1")
                .query(Long.class).single();
        for (String invalid : new String[] {
                "{\"resourceType\":\"TEACHER\",\"resourceId\":\"missing-teacher\",\"periodIds\":[\"mon-1\"]}",
                "{\"resourceType\":\"ROOM\",\"resourceId\":\"room-101\",\"periodIds\":[\"missing-period\"]}"
        }) {
            HttpResponse<String> refused = command("POST", "/api/repair-draft", session(), invalid);
            assertEquals(422, refused.statusCode(), "Workbench layout UC-2 ext 2a: invalid reference is refused");
            assertEquals("ACCEPTED_BASELINE", lifecycle());
            assertEquals(beforeInvalid, storedDocument());
            assertEquals(versionBeforeInvalid, jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1")
                    .query(Long.class).single());
        }

        JsonNode noEffect = body(command("POST", "/api/repair-draft", session(), teacherUnavailable("mon-3")));
        assertTrue(noEffect.path("workspace").path("repairDraft").path("directEffectLessonIds").isEmpty());
        JsonNode durableDraft = noEffect.path("workspace").path("repairDraft").deepCopy();
        assertEquals(durableDraft, repository.load().document().path("repairDraft"));

        HttpResponse<String> unconfirmed = command("DELETE", "/api/repair-draft", session(), "{\"confirmed\":false}");
        assertEquals(422, unconfirmed.statusCode());
        assertEquals("REPAIR_DRAFT", lifecycle());
        JsonNode discarded = body(command("DELETE", "/api/repair-draft", session(), "{\"confirmed\":true}"));
        assertEquals("ACCEPTED_BASELINE", discarded.path("state").stringValue());
        assertFalse(discarded.path("workspace").has("repairDraft"));
        assertEquals(acceptedBefore, discarded.path("workspace").path("acceptedBaseline"));
    }

    @Test
    @DisplayName("UC-4 RULE-6/22: missing, stale, and racing mutations preserve the exact accepted bundle and draft")
    void enforcesConditionalHttpMutation() throws Exception {
        Session accepted = session();
        HttpResponse<String> missing = commandWithoutVersion("POST", "/api/repair-draft", accepted, teacherUnavailable("mon-1"));
        assertEquals(428, missing.statusCode());
        HttpResponse<String> first = command("POST", "/api/repair-draft", accepted, teacherUnavailable("mon-1"));
        assertEquals(200, first.statusCode());
        JsonNode afterFirst = storedDocument();
        HttpResponse<String> stale = commandMinimal("PATCH", "/api/repair-draft", accepted, """
                {"action":"PIN","lessonId":"lesson-science-1","dimensions":["ROOM"]}
                """);
        assertEquals(412, stale.statusCode());
        assertEquals("STALE_WORKSPACE_VERSION", body(stale).path("code").stringValue());
        assertEquals(afterFirst, storedDocument());

        HttpResponse<String> malformed = command("PATCH", "/api/repair-draft", session(), "{");
        assertEquals(400, malformed.statusCode());
        assertEquals("MALFORMED_REQUEST", body(malformed).path("code").stringValue());
        assertEquals(afterFirst, storedDocument());
    }

    @Test
    @DisplayName("UC-4 extension 6b and minimal guarantee: auto-save failure retains the last durable draft and accepted baseline")
    void autoSaveFailureRetainsLastDurableDraft() throws Exception {
        command("POST", "/api/repair-draft", session(), teacherUnavailable("mon-1"));
        JsonNode before = storedDocument();
        jdbc.sql("""
                CREATE OR REPLACE FUNCTION reject_repair_autosave() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'injected repair autosave failure'; END;
                $$ LANGUAGE plpgsql
                """).update();
        jdbc.sql("""
                CREATE TRIGGER reject_repair_autosave BEFORE UPDATE ON workspace_aggregate
                FOR EACH ROW EXECUTE FUNCTION reject_repair_autosave()
                """).update();
        try {
            HttpResponse<String> failed = commandMinimal("PATCH", "/api/repair-draft", session(), """
                    {"action":"PIN","lessonId":"lesson-science-1","dimensions":["ROOM"]}
                    """);
            assertEquals(503, failed.statusCode());
            assertEquals("STORAGE_UNAVAILABLE", body(failed).path("code").stringValue());
        } finally {
            jdbc.sql("DROP TRIGGER reject_repair_autosave ON workspace_aggregate").update();
            jdbc.sql("DROP FUNCTION reject_repair_autosave()").update();
        }
        assertEquals(before, storedDocument());
        HttpResponse<String> refusedSolve = command("POST", "/api/runs", session(), "");
        assertEquals(409, refusedSolve.statusCode());
        assertEquals(before, storedDocument());
    }

    private void storeAccepted(boolean persistentPeriodLock) throws Exception {
        ObjectNode definition = (ObjectNode) JSON.readTree(ROOT.resolve("examples/initial-school.json").toFile());
        if (persistentPeriodLock) ((ObjectNode) definition.path("lessons").get(0)).put("periodLock", "mon-1");
        ObjectNode result = JSON.createObjectNode().put("status", "FEASIBLE").put("inputRevision", "sha256:accepted-input")
                .put("timetableRevision", "sha256:accepted-timetable");
        var assignments = result.putObject("timetable").putArray("assignments");
        assignments.add(assignment("lesson-math-1", "math", "cohort-7a", "teacher-alex", "mon-1", "room-102"));
        assignments.add(assignment("lesson-science-1", "science", "cohort-7a", "teacher-alex", "mon-2", "room-101"));
        ObjectNode document = JSON.createObjectNode();
        document.putObject("school").put("id", "demo-school").put("displayName", "Demo School");
        document.put("definitionRevision", "sha256:accepted-input").put("timetableRevision", "sha256:accepted-timetable");
        ObjectNode baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", definition); baseline.set("result", result);
        ObjectNode manifest = baseline.putObject("manifest").put("manifestVersion", 1);
        var locks = manifest.putArray("locks");
        if (persistentPeriodLock) locks.addObject().put("lessonId", "lesson-math-1").put("periodLockOrigin", "PERSISTENT_POLICY");
        jdbc.sql("""
                UPDATE workspace_aggregate SET lifecycle_state='ACCEPTED_BASELINE', version=7,
                active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                """).param("document", JSON.writeValueAsString(document)).update();
    }

    private static ObjectNode assignment(String lesson, String subject, String cohort, String teacher, String period, String room) {
        return JSON.createObjectNode().put("lessonId", lesson).put("subjectId", subject).put("cohortId", cohort)
                .put("teacherId", teacher).put("periodId", period).put("roomId", room);
    }

    private Session session() throws Exception {
        JsonNode token = body(get("/api/csrf"));
        HttpResponse<String> workspace = get("/api/workspace");
        return new Session(token.path("headerName").stringValue(), token.path("token").stringValue(),
                workspace.headers().firstValue("ETag").orElseThrow());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> command(String method, String path, Session session, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header(session.csrfHeader(), session.csrfToken())
                .header("If-Match", session.etag()).header("Origin", "http://localhost:" + port)
                .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> commandMinimal(String method, String path, Session session, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header(session.csrfHeader(), session.csrfToken())
                .header("If-Match", session.etag()).header("Origin", "http://localhost:" + port)
                .header("Content-Type", "application/json").header("Prefer", "return=minimal")
                .method(method, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> commandWithoutVersion(String method, String path, Session session, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header(session.csrfHeader(), session.csrfToken())
                .header("Origin", "http://localhost:" + port).header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private JsonNode body(HttpResponse<String> response) throws Exception { return JSON.readTree(response.body()); }
    private JsonNode storedDocument() throws Exception { return JSON.readTree(jdbc.sql("SELECT document::text FROM workspace_aggregate WHERE workspace_id=1").query(String.class).single()); }
    private String lifecycle() { return jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1").query(String.class).single(); }
    private static String teacherUnavailable(String period) { return "{\"resourceType\":\"TEACHER\",\"resourceId\":\"teacher-alex\",\"periodIds\":[\"" + period + "\"]}"; }
    private record Session(String csrfHeader, String csrfToken, String etag) {}
}
