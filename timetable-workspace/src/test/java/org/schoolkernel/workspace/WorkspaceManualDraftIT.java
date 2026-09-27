package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
class WorkspaceManualDraftIT {
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("manual_draft").withUsername("workspace").withPassword("workspace");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("workspace.kernel-executable", () -> ROOT.resolve("school-kernel").toString());
    }

    @LocalServerPort int port;
    @Autowired JdbcClient jdbc;
    @Autowired WorkspaceRepository repository;

    private HttpClient client;

    @BeforeEach
    void reset() throws Exception {
        storeAccepted();
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test
    @DisplayName("UC-1: Open manual editing draft copies accepted baseline, increments version, and preserves baseline untouched")
    void opensManualDraftFromAcceptedBaseline() throws Exception {
        JsonNode baselineBefore = storedDocument().path("acceptedBaseline").deepCopy();
        long versionBefore = jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1")
                .query(Long.class).single();

        Session session = session();
        HttpResponse<String> response = command("POST", "/api/manual-draft", session, "");
        assertEquals(200, response.statusCode());
        assertEquals("\"ws-" + (versionBefore + 1) + "\"", response.headers().firstValue("ETag").orElseThrow());

        JsonNode body = body(response);
        assertEquals("MANUAL_DRAFT", body.path("state").stringValue());
        assertTrue(body.path("acceptedTimetable").booleanValue());

        JsonNode manualDraft = body.path("workspace").path("manualDraft");
        assertNotNull(manualDraft);
        assertTrue(manualDraft.path("conflicts").isEmpty(), "New manual draft starts with zero conflicts");
        assertTrue(manualDraft.path("modifications").isEmpty(), "New manual draft starts with zero modifications");
        assertEquals(baselineBefore.path("result").path("timetable").path("assignments"),
                manualDraft.path("assignments"), "Draft assignments copy accepted baseline assignments exactly");
        assertTrue(manualDraft.path("draftRevision").stringValue().startsWith("sha256:"));

        // Guarantees G1 & G2: acceptedBaseline in document remains identical to baselineBefore
        assertEquals(baselineBefore, storedDocument().path("acceptedBaseline"));
        assertEquals("MANUAL_DRAFT", lifecycle());
    }

    @Test
    @DisplayName("UC-1 Extension 1a: cannot open manual draft when workspace is not in ACCEPTED_BASELINE")
    void refusesOpenDraftFromNonAcceptedState() throws Exception {
        jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state='EMPTY', version=10, document='{}'::jsonb WHERE workspace_id=1").update();
        Session session = session();
        HttpResponse<String> response = command("POST", "/api/manual-draft", session, "");
        assertEquals(409, response.statusCode());
        assertEquals("INVALID_WORKSPACE_TRANSITION", body(response).path("code").stringValue());
        assertEquals("EMPTY", lifecycle());
    }

    @Test
    @DisplayName("UC-1 Extension 3a, RULE-2: enforces optimistic concurrency on manual draft creation")
    void enforcesOptimisticConcurrency() throws Exception {
        Session session = session();
        HttpResponse<String> missing = commandWithoutVersion("POST", "/api/manual-draft", session, "");
        assertEquals(428, missing.statusCode());

        HttpResponse<String> stale = client.send(HttpRequest.newBuilder(uri("/api/manual-draft"))
                .header(session.csrfHeader(), session.csrfToken())
                .header("If-Match", "\"ws-999\"")
                .header("Origin", "http://localhost:" + port)
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(412, stale.statusCode());
        assertEquals("STALE_WORKSPACE_VERSION", body(stale).path("code").stringValue());
        assertEquals("ACCEPTED_BASELINE", lifecycle());
    }

    @Test
    @DisplayName("UC-2: reassign lesson slot cleanly, track modification, zero conflicts")
    void reassignsLessonCleanlyAndTracksModifications() throws Exception {
        Session session = session();
        HttpResponse<String> openResponse = command("POST", "/api/manual-draft", session, "");
        assertEquals(200, openResponse.statusCode());
        String draftEtag = openResponse.headers().firstValue("ETag").orElseThrow();
        Session draftSession = new Session(session.csrfHeader(), session.csrfToken(), draftEtag);

        ObjectNode payload = JSON.createObjectNode();
        payload.put("action", "REASSIGN_LESSON");
        payload.put("lessonId", "lesson-science-1");
        payload.put("periodId", "mon-3");
        payload.put("roomId", "room-101");
        payload.put("teacherId", "teacher-alex");

        HttpResponse<String> mutateResponse = command("PATCH", "/api/manual-draft", draftSession, JSON.writeValueAsString(payload));
        assertEquals(200, mutateResponse.statusCode());
        JsonNode body = body(mutateResponse);
        assertEquals("MANUAL_DRAFT", body.path("state").stringValue());

        JsonNode manualDraft = body.path("workspace").path("manualDraft");
        assertTrue(manualDraft.path("conflicts").isEmpty(), "Clean reassignment should have zero conflicts");
        assertFalse(manualDraft.path("modifications").isEmpty(), "Modifications map must track the changed lesson");

        JsonNode mod = manualDraft.path("modifications").path("lesson-science-1");
        assertTrue(mod.path("periodChanged").booleanValue());
        assertFalse(mod.path("roomChanged").booleanValue());
        assertFalse(mod.path("teacherChanged").booleanValue());
        assertEquals("mon-2", mod.path("originalPeriodId").stringValue());

        // Baseline remains untouched (Guarantee G1)
        assertEquals(storedDocument().path("acceptedBaseline").path("result").path("timetable").path("assignments").get(1).path("periodId").stringValue(), "mon-2");
    }

    @Test
    @DisplayName("UC-2 & UC-3: detect teacher, room, and cohort clashes, and persist conflicting draft state")
    void detectsClashesAndPersistsConflicts() throws Exception {
        Session session = session();
        HttpResponse<String> openResponse = command("POST", "/api/manual-draft", session, "");
        String draftEtag = openResponse.headers().firstValue("ETag").orElseThrow();
        Session draftSession = new Session(session.csrfHeader(), session.csrfToken(), draftEtag);

        // Reassign science-1 into Monday 1, Room 102, Teacher Alex (where math-1 is already scheduled)
        ObjectNode payload = JSON.createObjectNode();
        payload.put("action", "REASSIGN_LESSON");
        payload.put("lessonId", "lesson-science-1");
        payload.put("periodId", "mon-1");
        payload.put("roomId", "room-102");
        payload.put("teacherId", "teacher-alex");

        HttpResponse<String> mutateResponse = command("PATCH", "/api/manual-draft", draftSession, JSON.writeValueAsString(payload));
        assertEquals(200, mutateResponse.statusCode());
        JsonNode body = body(mutateResponse);

        JsonNode manualDraft = body.path("workspace").path("manualDraft");
        JsonNode conflicts = manualDraft.path("conflicts");
        assertFalse(conflicts.isEmpty(), "Conflicts must be detected and populated");

        Set<String> conflictCodes = new HashSet<>();
        for (JsonNode conflict : conflicts) {
            conflictCodes.add(conflict.path("code").stringValue());
            assertTrue(conflict.path("description").stringValue().length() > 5);
        }

        assertTrue(conflictCodes.contains("TEACHER_CLASH"), "Teacher double-booking should be detected");
        assertTrue(conflictCodes.contains("ROOM_CLASH"), "Room double-booking should be detected");
        assertTrue(conflictCodes.contains("COHORT_CLASH"), "Cohort double-booking should be detected");
        assertTrue(conflictCodes.contains("ROOM_INCOMPATIBLE"), "Room 102 lacks 'lab' capability required by Science 1");

        // Verify state is persisted in DB even with conflicts (Guarantee G3)
        JsonNode storedDraft = storedDocument().path("manualDraft");
        assertEquals(conflicts.size(), storedDraft.path("conflicts").size());
        assertEquals("MANUAL_DRAFT", lifecycle());
    }

    @Test
    @DisplayName("UC-2 / UC-4: revert individual lesson restores original assignment and clears modifications")
    void revertsLessonAssignment() throws Exception {
        Session session = session();
        HttpResponse<String> openResponse = command("POST", "/api/manual-draft", session, "");
        String draftEtag = openResponse.headers().firstValue("ETag").orElseThrow();
        Session draftSession = new Session(session.csrfHeader(), session.csrfToken(), draftEtag);

        ObjectNode payload = JSON.createObjectNode();
        payload.put("action", "REASSIGN_LESSON");
        payload.put("lessonId", "lesson-science-1");
        payload.put("periodId", "mon-3");
        payload.put("roomId", "room-101");
        payload.put("teacherId", "teacher-alex");

        HttpResponse<String> mutateResponse = command("PATCH", "/api/manual-draft", draftSession, JSON.writeValueAsString(payload));
        String mutatedEtag = mutateResponse.headers().firstValue("ETag").orElseThrow();
        Session mutatedSession = new Session(session.csrfHeader(), session.csrfToken(), mutatedEtag);

        ObjectNode revertPayload = JSON.createObjectNode();
        revertPayload.put("action", "REVERT_LESSON");
        revertPayload.put("lessonId", "lesson-science-1");

        HttpResponse<String> revertResponse = command("PATCH", "/api/manual-draft", mutatedSession, JSON.writeValueAsString(revertPayload));
        assertEquals(200, revertResponse.statusCode());
        JsonNode manualDraft = body(revertResponse).path("workspace").path("manualDraft");

        assertTrue(manualDraft.path("modifications").isEmpty(), "Modifications map should be empty after revert");
        assertTrue(manualDraft.path("conflicts").isEmpty());

        JsonNode revertedAssignment = null;
        for (JsonNode a : manualDraft.path("assignments")) {
            if ("lesson-science-1".equals(a.path("lessonId").stringValue())) {
                revertedAssignment = a;
                break;
            }
        }
        assertNotNull(revertedAssignment);
        assertEquals("mon-2", revertedAssignment.path("periodId").stringValue(), "Restores baseline period");
        assertEquals("room-101", revertedAssignment.path("roomId").stringValue(), "Restores baseline room");
        assertEquals("teacher-alex", revertedAssignment.path("teacherId").stringValue(), "Restores baseline teacher");
    }

    @Test
    @DisplayName("UC-2 Extension 3a, 4a, RULE-2: enforces optimistic locking and validates entities on mutate")
    void enforcesOptimisticLockingAndInputValidation() throws Exception {
        Session session = session();
        HttpResponse<String> openResponse = command("POST", "/api/manual-draft", session, "");
        String draftEtag = openResponse.headers().firstValue("ETag").orElseThrow();
        Session draftSession = new Session(session.csrfHeader(), session.csrfToken(), draftEtag);

        // Missing If-Match
        HttpResponse<String> missing = commandWithoutVersion("PATCH", "/api/manual-draft", draftSession, "{}");
        assertEquals(428, missing.statusCode());

        // Stale If-Match
        HttpResponse<String> stale = client.send(HttpRequest.newBuilder(uri("/api/manual-draft"))
                .header(draftSession.csrfHeader(), draftSession.csrfToken())
                .header("If-Match", "\"ws-999\"")
                .header("Origin", "http://localhost:" + port)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(412, stale.statusCode());

        // Nonexistent lesson
        ObjectNode badLesson = JSON.createObjectNode().put("action", "REASSIGN_LESSON").put("lessonId", "unknown-lesson")
                .put("periodId", "mon-1").put("roomId", "room-101").put("teacherId", "teacher-alex");
        HttpResponse<String> badLessonResp = command("PATCH", "/api/manual-draft", draftSession, JSON.writeValueAsString(badLesson));
        assertEquals(422, badLessonResp.statusCode());

        // Nonexistent period
        ObjectNode badPeriod = JSON.createObjectNode().put("action", "REASSIGN_LESSON").put("lessonId", "lesson-science-1")
                .put("periodId", "unknown-period").put("roomId", "room-101").put("teacherId", "teacher-alex");
        HttpResponse<String> badPeriodResp = command("PATCH", "/api/manual-draft", draftSession, JSON.writeValueAsString(badPeriod));
        assertEquals(422, badPeriodResp.statusCode());
    }

    @Test
    @DisplayName("UC-3: inspects conflict details, causal explanations, competing lessons, and multi-conflict enumeration (G1, G2, Ext 2a)")
    void inspectsConflictDetailsAndCausalExplanations() throws Exception {
        Session session = session();
        HttpResponse<String> openResponse = command("POST", "/api/manual-draft", session, "");
        String draftEtag = openResponse.headers().firstValue("ETag").orElseThrow();
        Session draftSession = new Session(session.csrfHeader(), session.csrfToken(), draftEtag);

        // Reassign lesson-science-1 into mon-1, room-102, teacher-alex (causes multiple simultaneous conflicts with lesson-math-1)
        ObjectNode mutatePayload = JSON.createObjectNode();
        mutatePayload.put("action", "REASSIGN_LESSON");
        mutatePayload.put("lessonId", "lesson-science-1");
        mutatePayload.put("periodId", "mon-1");
        mutatePayload.put("roomId", "room-102");
        mutatePayload.put("teacherId", "teacher-alex");

        HttpResponse<String> mutateResp = command("PATCH", "/api/manual-draft", draftSession, JSON.writeValueAsString(mutatePayload));
        assertEquals(200, mutateResp.statusCode());
        String mutatedEtag = mutateResp.headers().firstValue("ETag").orElseThrow();

        JsonNode responseBody = body(mutateResp);
        JsonNode manualDraft = responseBody.path("workspace").path("manualDraft");
        JsonNode conflicts = manualDraft.path("conflicts");
        assertFalse(conflicts.isEmpty(), "Conflicts must be present");

        // Filter conflicts pertaining to lesson-science-1
        List<JsonNode> scienceConflicts = new ArrayList<>();
        conflicts.forEach(c -> {
            if ("lesson-science-1".equals(c.path("lessonId").stringValue())) {
                scienceConflicts.add(c);
            }
        });

        // UC-3 Extension 2a: Multi-conflict enumeration (simultaneous teacher, room, cohort clashes)
        assertTrue(scienceConflicts.size() >= 3, "Should itemize multiple distinct conflicts");

        Set<String> codes = new HashSet<>();
        for (JsonNode c : scienceConflicts) {
            String code = c.path("code").stringValue();
            codes.add(code);
            // UC-3 G1: Complete causal explanation: contested resource, period, and competing assignments
            assertNotNull(c.path("resourceType").stringValue(), "Resource type must be present");
            assertNotNull(c.path("resourceId").stringValue(), "Resource ID must be present");
            assertEquals("mon-1", c.path("periodId").stringValue(), "Period must match contested slot");
            assertTrue(c.path("description").stringValue().contains("double-booked")
                    || c.path("description").stringValue().contains("unavailable")
                    || c.path("description").stringValue().contains("capability"), "Must provide causal reason");

            // For clashes, competingLessonIds must include lesson-math-1
            if (code.endsWith("_CLASH")) {
                List<String> competing = new ArrayList<>();
                c.path("competingLessonIds").forEach(comp -> competing.add(comp.stringValue()));
                assertTrue(competing.contains("lesson-math-1"), "Competing lessons must identify lesson-math-1");
            }
        }
        assertTrue(codes.contains("TEACHER_CLASH"));
        assertTrue(codes.contains("ROOM_CLASH"));
        assertTrue(codes.contains("COHORT_CLASH"));

        // UC-3 G2: Non-mutating inspection: fetching workspace via GET preserves ETag and state
        HttpResponse<String> inspectResp = client.send(HttpRequest.newBuilder(uri("/api/workspace"))
                .header("Accept", "application/json")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, inspectResp.statusCode());
        assertEquals(mutatedEtag, inspectResp.headers().firstValue("ETag").orElseThrow(), "ETag unchanged after inspection");

        JsonNode inspectedBody = body(inspectResp);
        assertEquals(responseBody.path("version").intValue(), inspectedBody.path("version").intValue(), "Version unchanged");
        assertEquals(conflicts.size(), inspectedBody.path("workspace").path("manualDraft").path("conflicts").size(), "Conflicts unchanged");
    }

    @Test
    @DisplayName("UC-5: Discard manual editing draft with confirmation, restoring accepted baseline")
    void discardsManualDraftWithConfirmationAndRestoresAcceptedBaseline() throws Exception {
        Session session = session();
        HttpResponse<String> openResponse = command("POST", "/api/manual-draft", session, "");
        assertEquals(200, openResponse.statusCode());
        String draftEtag = openResponse.headers().firstValue("ETag").orElseThrow();
        Session draftSession = new Session(session.csrfHeader(), session.csrfToken(), draftEtag);

        // Reassign a lesson to have a pending modification
        ObjectNode mutatePayload = JSON.createObjectNode();
        mutatePayload.put("action", "REASSIGN_LESSON");
        mutatePayload.put("lessonId", "lesson-science-1");
        mutatePayload.put("periodId", "mon-3");
        mutatePayload.put("roomId", "room-101");
        mutatePayload.put("teacherId", "teacher-alex");

        HttpResponse<String> mutateResponse = command("PATCH", "/api/manual-draft", draftSession, JSON.writeValueAsString(mutatePayload));
        assertEquals(200, mutateResponse.statusCode());
        String mutatedEtag = mutateResponse.headers().firstValue("ETag").orElseThrow();
        Session mutatedSession = new Session(session.csrfHeader(), session.csrfToken(), mutatedEtag);

        // Extension 3a / Guarantee G2: Attempt discard without confirmation payload
        HttpResponse<String> unconfirmedResponse = command("DELETE", "/api/manual-draft", mutatedSession, "{\"confirmed\":false}");
        assertEquals(422, unconfirmedResponse.statusCode());
        assertEquals("CONFIRMATION_REQUIRED", body(unconfirmedResponse).path("code").stringValue());

        // Verify draft is still present with modification
        HttpResponse<String> verifyDraftResp = client.send(HttpRequest.newBuilder(uri("/api/workspace"))
                .header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("MANUAL_DRAFT", body(verifyDraftResp).path("state").stringValue());
        assertFalse(body(verifyDraftResp).path("workspace").path("manualDraft").isMissingNode());

        // Main success step 3-6 / Guarantee G1: Discard with explicit confirmation
        HttpResponse<String> discardResponse = command("DELETE", "/api/manual-draft", mutatedSession, "{\"confirmed\":true}");
        assertEquals(200, discardResponse.statusCode());

        JsonNode discardedBody = body(discardResponse);
        assertEquals("ACCEPTED_BASELINE", discardedBody.path("state").stringValue());
        assertTrue(discardedBody.path("workspace").path("manualDraft").isMissingNode(), "manualDraft node must be removed");

        // Verify via fresh GET /api/workspace
        HttpResponse<String> baselineResp = client.send(HttpRequest.newBuilder(uri("/api/workspace"))
                .header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, baselineResp.statusCode());
        JsonNode baselineBody = body(baselineResp);
        assertEquals("ACCEPTED_BASELINE", baselineBody.path("state").stringValue());
        assertTrue(baselineBody.path("workspace").path("manualDraft").isMissingNode());

        // RULE-1: Attempting to discard when already in ACCEPTED_BASELINE is rejected with 409
        String baselineEtag = baselineResp.headers().firstValue("ETag").orElseThrow();
        Session baselineSession = new Session(session.csrfHeader(), session.csrfToken(), baselineEtag);
        HttpResponse<String> invalidTransition = command("DELETE", "/api/manual-draft", baselineSession, "{\"confirmed\":true}");
        assertEquals(409, invalidTransition.statusCode());
        assertEquals("INVALID_WORKSPACE_TRANSITION", body(invalidTransition).path("code").stringValue());
    }

    private void storeAccepted() throws Exception {
        ObjectNode definition = (ObjectNode) JSON.readTree(ROOT.resolve("examples/initial-school.json").toFile());
        ObjectNode result = JSON.createObjectNode().put("status", "FEASIBLE").put("inputRevision", "sha256:accepted-input")
                .put("timetableRevision", "sha256:accepted-timetable");
        var assignments = result.putObject("timetable").putArray("assignments");
        assignments.add(assignment("lesson-math-1", "math", "cohort-7a", "teacher-alex", "mon-1", "room-102"));
        assignments.add(assignment("lesson-science-1", "science", "cohort-7a", "teacher-alex", "mon-2", "room-101"));
        ObjectNode document = JSON.createObjectNode();
        document.putObject("school").put("id", "demo-school").put("displayName", "Demo School");
        document.put("definitionRevision", "sha256:accepted-input").put("timetableRevision", "sha256:accepted-timetable");
        ObjectNode baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", definition);
        baseline.set("result", result);
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

    private HttpResponse<String> commandWithoutVersion(String method, String path, Session session, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header(session.csrfHeader(), session.csrfToken())
                .header("Origin", "http://localhost:" + port)
                .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private JsonNode storedDocument() {
        return repository.load().document();
    }

    private String lifecycle() {
        return jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single();
    }

    private static JsonNode body(HttpResponse<String> response) throws Exception {
        return JSON.readTree(response.body());
    }

    private record Session(String csrfHeader, String csrfToken, String etag) {}
}
