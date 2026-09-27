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
