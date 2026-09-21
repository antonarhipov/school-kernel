package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
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
@SpringBootTest(classes = WorkspaceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkspaceRepairPlanningIT {
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("repair_planning").withUsername("workspace").withPassword("workspace");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("workspace.kernel-executable", () -> ROOT.resolve("school-kernel").toString());
    }

    @LocalServerPort int port;
    @Autowired JdbcClient jdbc;
    @Autowired WorkspaceRecovery recovery;

    private HttpClient client;

    @BeforeEach
    void reset() {
        jdbc.sql("""
                UPDATE workspace_aggregate
                SET lifecycle_state='EMPTY', version=0, active_run_id=NULL, document='{}'::jsonb
                WHERE workspace_id=1
                """).update();
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test
    @DisplayName("UC-5 main, G1-G6, RULE-10/11/15/17: packaged replan creates an exact verified repair proposal while accepted data stays current")
    void createsVerifiedRepairProposalWithoutReplacingAcceptedBaseline() throws Exception {
        establishAcceptedBaseline();
        command("POST", "/api/repair-draft", session(),
                "{\"resourceType\":\"TEACHER\",\"resourceId\":\"teacher-alex\",\"periodIds\":[\"mon-1\"]}");
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();
        JsonNode draftBefore = storedDocument().path("repairDraft").deepCopy();
        Session draftSession = session();

        HttpResponse<String> started = command("POST", "/api/runs", draftSession, "{\"limit\":\"PT30S\"}");
        assertEquals(202, started.statusCode(), started.body());
        JsonNode solving = body(started);
        assertEquals("SOLVING_REPAIR", solving.path("state").stringValue());
        assertTrue(solving.path("acceptedTimetable").booleanValue());
        assertEquals("PT30S", solving.path("workspace").path("run").path("limit").stringValue());
        assertEquals(acceptedBefore, solving.path("workspace").path("acceptedBaseline"));
        UUID runId = UUID.fromString(solving.path("workspace").path("run").path("id").stringValue());

        HttpResponse<String> frozen = command("PATCH", "/api/repair-draft", session(),
                "{\"action\":\"PIN\",\"lessonId\":\"lesson-science-1\",\"dimensions\":[\"ROOM\"]}");
        assertEquals(409, frozen.statusCode());
        assertEquals("INVALID_WORKSPACE_TRANSITION", body(frozen).path("code").stringValue());

        JsonNode proposalSnapshot = awaitState("REPAIR_PROPOSAL", Duration.ofSeconds(45));
        JsonNode proposal = proposalSnapshot.path("workspace").path("proposal");
        assertEquals("REPAIR", proposal.path("kind").stringValue());
        assertEquals(acceptedBefore.path("result").path("timetableRevision"), proposal.path("acceptedTimetableRevision"));
        assertEquals(draftBefore.path("intentRevision"), proposal.path("intentRevision"));
        assertEquals(acceptedBefore.path("result").path("inputRevision"), proposal.path("definition").path("basedOnRevision"));
        assertEquals(proposal.path("successorDefinitionRevision"), proposal.path("result").path("inputRevision"));
        assertEquals(proposal.path("proposedTimetableRevision"), proposal.path("result").path("timetableRevision"));
        assertEquals("FEASIBLE", proposal.path("result").path("status").stringValue());
        assertEquals("PT30S", proposal.path("result").path("limit").path("duration").stringValue());
        JsonNode changeReport = proposal.path("result").path("changeReport");
        assertEquals(6, changeReport.size());
        for (String category : new String[] {"additions", "cancellations", "teacherChanges", "forcedMoves", "periodMoves", "roomOnlyMoves"}) {
            assertTrue(changeReport.path(category).isArray(), category);
        }
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
        assertEquals("FEASIBLE", body(get("/api/runs/" + runId)).path("status").stringValue());
        assertEquals(proposal, body(get("/api/proposal")));
        assertFalse(proposal.has("resultRevision"));
    }

    @Test
    @DisplayName("UC-5 extensions 4b/4c and G7: unsuccessful evidence offers only an unchanged two-minute retry and never exposes a proposal")
    void gatesTwoMinuteRetryByUnchangedUnsuccessfulIntent() throws Exception {
        establishAcceptedBaseline();
        command("POST", "/api/repair-draft", session(),
                "{\"resourceType\":\"TEACHER\",\"resourceId\":\"teacher-alex\",\"periodIds\":[\"mon-1\",\"mon-2\",\"mon-3\"]}");
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();
        JsonNode intentBefore = storedDocument().path("repairDraft").path("intent").deepCopy();

        ObjectNode arranged = (ObjectNode) storedDocument();
        String intentRevision = arranged.path("repairDraft").path("intentRevision").stringValue();
        ObjectNode arrangedLastRun = arranged.putObject("lastRun");
        arrangedLastRun.put("id", UUID.randomUUID().toString()).put("kind", "REPAIR")
                .put("status", "FAILED").put("code", "NO_FEASIBLE_SOLUTION_FOUND")
                .put("message", "No feasible repair was found within this run.")
                .put("limit", "PT30S").put("intentRevision", intentRevision).put("feasible", false);
        arrangedLastRun.putObject("searchDiagnostics").put("totalMatches", 2).put("truncated", false)
                .putArray("constraints").addObject().put("constraintId", "hard.teacher-period")
                .put("matchCount", 2).putArray("examples").addArray().add("lesson-math-1");
        jdbc.sql("UPDATE workspace_aggregate SET document=CAST(:document AS jsonb) WHERE workspace_id=1")
                .param("document", JSON.writeValueAsString(arranged)).update();
        JsonNode failed = body(get("/api/workspace"));
        JsonNode lastRun = failed.path("workspace").path("lastRun");
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", lastRun.path("code").stringValue());
        assertTrue(lastRun.path("searchDiagnostics").path("constraints").isArray());
        assertFalse(failed.path("workspace").has("proposal"));
        assertEquals(intentBefore, failed.path("workspace").path("repairDraft").path("intent"));
        assertEquals(acceptedBefore, failed.path("workspace").path("acceptedBaseline"));

        command("PATCH", "/api/repair-draft", session(),
                "{\"action\":\"STAGE_UNAVAILABILITY\",\"resourceType\":\"TEACHER\",\"resourceId\":\"teacher-alex\",\"periodIds\":[\"mon-1\"]}");
        JsonNode beforeRefusal = storedDocument();
        HttpResponse<String> refused = command("POST", "/api/runs", session(), "{\"limit\":\"PT2M\"}");
        assertEquals(409, refused.statusCode());
        assertEquals("RETRY_NOT_AVAILABLE", body(refused).path("code").stringValue());
        assertEquals(beforeRefusal, storedDocument());

        command("PATCH", "/api/repair-draft", session(),
                "{\"action\":\"STAGE_UNAVAILABILITY\",\"resourceType\":\"TEACHER\",\"resourceId\":\"teacher-alex\",\"periodIds\":[\"mon-1\",\"mon-2\",\"mon-3\"]}");
        HttpResponse<String> retry = command("POST", "/api/runs", session(), "{\"limit\":\"PT2M\"}");
        assertEquals(202, retry.statusCode(), retry.body());
        assertEquals("PT2M", body(retry).path("workspace").path("run").path("limit").stringValue());
        String retryRunId = body(retry).path("workspace").path("run").path("id").stringValue();
        command("DELETE", "/api/runs/" + retryRunId, session(), "");
        assertEquals("PT2M", storedDocument().path("lastRun").path("limit").stringValue());
        assertFalse(storedDocument().has("proposal"));
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
    }

    @Test
    @DisplayName("UC-5 extensions 2a/2b/3a and minimal guarantee: cancellation, stale start, and restart preserve the exact draft and accepted bundle")
    void cancellationStalenessAndRestartAreSafe() throws Exception {
        establishAcceptedBaseline();
        command("POST", "/api/repair-draft", session(),
                "{\"resourceType\":\"ROOM\",\"resourceId\":\"room-101\",\"periodIds\":[\"mon-2\"]}");
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();
        JsonNode draftBefore = storedDocument().path("repairDraft").deepCopy();
        Session beforeStart = session();
        HttpResponse<String> started = command("POST", "/api/runs", beforeStart, "{\"limit\":\"PT30S\"}");
        UUID runId = UUID.fromString(body(started).path("workspace").path("run").path("id").stringValue());
        HttpResponse<String> cancelled = command("DELETE", "/api/runs/" + runId, session(), "");
        assertEquals(200, cancelled.statusCode(), cancelled.body());
        assertEquals("REPAIR_DRAFT", body(cancelled).path("state").stringValue());
        assertEquals(draftBefore, storedDocument().path("repairDraft"));
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
        assertFalse(storedDocument().has("proposal"));

        HttpResponse<String> stale = command("POST", "/api/runs", beforeStart, "{\"limit\":\"PT30S\"}");
        assertEquals(412, stale.statusCode());
        JsonNode beforeRecovery = storedDocument();
        UUID interrupted = UUID.randomUUID();
        jdbc.sql("""
                UPDATE workspace_aggregate SET lifecycle_state='SOLVING_REPAIR', active_run_id=:run_id,
                document=jsonb_set(document, '{run}', CAST(:run AS jsonb)) WHERE workspace_id=1
                """).param("run_id", interrupted)
                .param("run", "{\"id\":\"" + interrupted + "\",\"kind\":\"REPAIR\"}").update();
        recovery.recoverInterruptedRun();
        assertEquals("REPAIR_DRAFT", lifecycle());
        assertEquals(beforeRecovery.path("repairDraft"), storedDocument().path("repairDraft"));
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
        assertFalse(storedDocument().has("run"));
        assertFalse(storedDocument().has("proposal"));
    }

    private void establishAcceptedBaseline() throws Exception {
        JsonNode definition = JSON.readTree(ROOT.resolve("examples/initial-school.json").toFile());
        JsonNode result = JSON.readTree(Path.of("src/test/resources/uc5-accepted-result.json").toFile());
        var document = JSON.createObjectNode();
        document.putObject("school").put("id", "demo-school").put("displayName", "Demo School");
        document.put("importMode", "ACCEPTED_PAIR");
        document.put("definitionRevision", result.path("inputRevision").stringValue());
        document.put("timetableRevision", result.path("timetableRevision").stringValue());
        var baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", definition);
        baseline.set("result", result);
        var manifest = baseline.putObject("manifest");
        manifest.put("manifestVersion", 1);
        manifest.put("definitionSchemaVersion", 1);
        manifest.put("resultSchemaVersion", 1);
        manifest.put("catalogVersion", 1);
        manifest.put("schoolId", "demo-school");
        manifest.put("inputRevision", result.path("inputRevision").stringValue());
        manifest.put("timetableRevision", result.path("timetableRevision").stringValue());
        manifest.putArray("locks");
        jdbc.sql("""
                UPDATE workspace_aggregate SET lifecycle_state='ACCEPTED_BASELINE', version=7,
                active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                """).param("document", JSON.writeValueAsString(document)).update();
    }

    private JsonNode awaitState(String state, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        JsonNode last = null;
        while (System.nanoTime() < deadline) {
            last = body(get("/api/workspace"));
            if (state.equals(last.path("state").stringValue())) return last;
            Thread.sleep(50);
        }
        throw new AssertionError("Did not reach " + state + "; last=" + last);
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
        HttpRequest.BodyPublisher publisher = body == null || body.isEmpty()
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header(session.csrfHeader(), session.csrfToken()).header("If-Match", session.etag())
                .header("Origin", "http://localhost:" + port).method(method, publisher);
        if (body != null && !body.isEmpty()) request.header("Content-Type", "application/json");
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private JsonNode body(HttpResponse<String> response) throws Exception { return JSON.readTree(response.body()); }
    private JsonNode storedDocument() throws Exception { return JSON.readTree(jdbc.sql("SELECT document::text FROM workspace_aggregate WHERE workspace_id=1").query(String.class).single()); }
    private String lifecycle() { return jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1").query(String.class).single(); }
    private record Session(String csrfHeader, String csrfToken, String etag) {}
}
