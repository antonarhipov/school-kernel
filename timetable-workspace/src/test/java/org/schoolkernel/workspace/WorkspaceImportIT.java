package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.CRC32;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = {WorkspaceApplication.class, WorkspaceImportIT.RecordingConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkspaceImportIT {
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("workspace")
            .withUsername("workspace")
            .withPassword("workspace");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("workspace.kernel-executable", () -> ROOT.resolve("school-kernel").toString());
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    RecordingProcessLauncher processes;

    @TempDir
    Path temporaryDirectory;

    private HttpClient client;

    @BeforeEach
    void resetWorkspace() {
        jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state = 'EMPTY', version = 0, active_run_id = NULL, document = '{}'::jsonb
                        WHERE workspace_id = 1
                        """)
                .update();
        processes.reset();
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test
    @DisplayName("UC-1 main definition mode: real local HTTP import durably opens an initial draft")
    void importsInitialDefinition() throws Exception {
        Session session = session();
        byte[] definition = Files.readAllBytes(ROOT.resolve("examples/initial-school.json"));

        HttpResponse<String> response = post(session, Map.of("definition", new FilePart("school.json", definition)), true, true);

        assertEquals(200, response.statusCode());
        JsonNode snapshot = JSON.readTree(response.body());
        assertEquals("INITIAL_DRAFT", snapshot.path("state").stringValue());
        assertFalse(snapshot.path("acceptedTimetable").booleanValue());
        assertEquals("demo-school", snapshot.path("workspace").path("school").path("id").stringValue());
        assertEquals("Demo School", snapshot.path("workspace").path("school").path("displayName").stringValue());
        assertFalse(snapshot.path("workspace").path("school").has("displayNameAvailable"));
        assertEquals("INITIAL_DEFINITION", snapshot.path("workspace").path("importMode").stringValue());
        assertFalse(snapshot.path("workspace").has("acceptedBaseline"));
        assertEquals("INITIAL_DRAFT", lifecycle());
        assertEquals(1L, version());
        assertEquals(JSON.readTree(definition), storedDocument().path("initialDefinition"));
        assertNotNull(response.headers().firstValue("ETag").orElse(null));
        assertEquals(1, processes.count());
    }

    @Test
    @DisplayName("UC-1 main accepted mode: exact definition/result pair becomes one atomic accepted baseline")
    void importsAcceptedPair() throws Exception {
        Path result = plannedResult(ROOT.resolve("examples/initial-school.json"), "accepted-result.json");
        Session session = session();
        byte[] definition = Files.readAllBytes(ROOT.resolve("examples/initial-school.json"));
        byte[] resultBytes = Files.readAllBytes(result);

        HttpResponse<String> response = post(session, Map.of(
                "definition", new FilePart("school.json", definition),
                "result", new FilePart("result.json", resultBytes)), true, true);

        assertEquals(200, response.statusCode());
        JsonNode snapshot = JSON.readTree(response.body());
        assertEquals("ACCEPTED_BASELINE", snapshot.path("state").stringValue());
        assertTrue(snapshot.path("acceptedTimetable").booleanValue());
        JsonNode stored = storedDocument().path("acceptedBaseline");
        assertEquals(JSON.readTree(definition), stored.path("definition"));
        assertEquals(JSON.readTree(resultBytes), stored.path("result"));
        assertEquals("PERSISTENT_POLICY",
                stored.path("manifest").path("locks").isEmpty()
                        ? "PERSISTENT_POLICY"
                        : stored.path("manifest").path("locks").get(0).path("periodLockOrigin").stringValue());
        assertEquals(1L, version());
    }

    @Test
    @DisplayName("UC-1 extension 1b: exact accepted archive imports and unsafe archive is rejected without mutation")
    void archiveImportAndUnsafeRejection() throws Exception {
        Path definitionPath = ROOT.resolve("examples/initial-school.json");
        Path resultPath = plannedResult(definitionPath, "archive-result.json");
        JsonNode definition = JSON.readTree(definitionPath);
        JsonNode result = JSON.readTree(resultPath);
        ObjectNode manifest = JSON.createObjectNode();
        manifest.put("manifestVersion", 1);
        manifest.put("definitionSchemaVersion", 1);
        manifest.put("resultSchemaVersion", 1);
        manifest.put("catalogVersion", 1);
        manifest.put("schoolId", result.path("schoolId").stringValue());
        manifest.put("inputRevision", result.path("inputRevision").stringValue());
        manifest.put("timetableRevision", result.path("timetableRevision").stringValue());
        manifest.putArray("locks");
        byte[] acceptedArchive = zip(Map.of(
                "school-definition.json", JSON.writeValueAsBytes(definition),
                "timetable-result.json", JSON.writeValueAsBytes(result),
                "workspace-manifest.json", JSON.writeValueAsBytes(manifest)));

        Session acceptedSession = session();
        HttpResponse<String> accepted = post(acceptedSession,
                Map.of("archive", new FilePart("accepted.zip", acceptedArchive)), true, true);
        assertEquals(200, accepted.statusCode());
        assertEquals("ACCEPTED_BASELINE", lifecycle());

        resetWorkspace();
        byte[] unsafe = zip(Map.of(
                "school-definition.json", JSON.writeValueAsBytes(definition),
                "timetable-result.json", JSON.writeValueAsBytes(result),
                "workspace-manifest.json", JSON.writeValueAsBytes(manifest),
                "../extra.json", "{}".getBytes(StandardCharsets.UTF_8)));
        Session unsafeSession = session();
        HttpResponse<String> rejected = post(unsafeSession,
                Map.of("archive", new FilePart("unsafe.zip", unsafe)), true, true);
        assertEquals(422, rejected.statusCode());
        assertEquals("UNSAFE_ARCHIVE", JSON.readTree(rejected.body()).path("code").stringValue());
        assertEmpty();
    }

    @Test
    @DisplayName("UC-1 extension 1b: complete unsafe archive corpus is rejected through HTTP without kernel invocation")
    void malformedArchiveMatrixAtHttpBoundary() throws Exception {
        byte[] definition = Files.readAllBytes(ROOT.resolve("examples/initial-school.json"));
        List<ArchiveCase> cases = List.of(
                new ArchiveCase(zip(Map.of(
                        "school-definition.json", definition,
                        "timetable-result.json", "{}".getBytes(StandardCharsets.UTF_8))),
                        422, "INCOMPLETE_ARCHIVE"),
                new ArchiveCase(rawZip(List.of(
                        new ArchiveEntry("school-definition.json", definition, false),
                        new ArchiveEntry("school-definition.json", definition, false),
                        new ArchiveEntry("timetable-result.json", "{}".getBytes(StandardCharsets.UTF_8), false),
                        new ArchiveEntry("workspace-manifest.json", "{}".getBytes(StandardCharsets.UTF_8), false))),
                        422, "UNSAFE_ARCHIVE"),
                new ArchiveCase(rawZip(List.of(
                        new ArchiveEntry("school-definition.json", definition, true))),
                        422, "UNREADABLE_ARCHIVE"),
                new ArchiveCase("not a zip".getBytes(StandardCharsets.UTF_8),
                        422, "UNREADABLE_ARCHIVE"),
                new ArchiveCase(zip(Map.of(
                        "school-definition.json", new byte[(int) SafeImportReader.JSON_LIMIT + 1],
                        "timetable-result.json", "{}".getBytes(StandardCharsets.UTF_8),
                        "workspace-manifest.json", "{}".getBytes(StandardCharsets.UTF_8))),
                        413, "IMPORT_TOO_LARGE"));

        for (ArchiveCase invalid : cases) {
            resetWorkspace();
            Session session = session();
            HttpResponse<String> response = post(session,
                    Map.of("archive", new FilePart("invalid.zip", invalid.bytes())), true, true);
            assertEquals(invalid.status(), response.statusCode(), invalid.code());
            assertCurrentProblem(response, invalid.code(), "EMPTY", "\"ws-0\"");
            assertEmpty();
            assertEquals(0, processes.count());
        }
    }

    @Test
    @DisplayName("UC-1 extensions 1a, 2a, 2c and RULE-6: refused imports preserve the empty aggregate")
    void refusedImportsDoNotMutateOrDiscloseCandidate() throws Exception {
        Path result = plannedResult(ROOT.resolve("examples/initial-school.json"), "orphan-result.json");
        Session resultOnlySession = session();
        HttpResponse<String> resultOnly = post(resultOnlySession,
                Map.of("result", new FilePart("result.json", Files.readAllBytes(result))), true, true);
        assertEquals(422, resultOnly.statusCode());
        assertEquals("MATCHING_DEFINITION_REQUIRED", JSON.readTree(resultOnly.body()).path("code").stringValue());
        assertFalse(resultOnly.body().contains("assignments"));
        assertEmpty();

        Session malformedSession = session();
        HttpResponse<String> malformed = post(malformedSession,
                Map.of("definition", new FilePart("definition.json", "{".getBytes(StandardCharsets.UTF_8))), true, true);
        assertEquals(422, malformed.statusCode());
        assertEmpty();

        Session missingVersionSession = session();
        HttpResponse<String> missingVersion = post(missingVersionSession,
                Map.of("definition", new FilePart("definition.json",
                        Files.readAllBytes(ROOT.resolve("examples/initial-school.json")))), true, false);
        assertEquals(428, missingVersion.statusCode());
        assertEmpty();

        Session staleSession = session();
        HttpResponse<String> stale = post(new Session(staleSession.csrfHeader(), staleSession.csrfToken(), "\"ws-99\""),
                Map.of("definition", new FilePart("definition.json",
                        Files.readAllBytes(ROOT.resolve("examples/initial-school.json")))), true, true);
        assertEquals(412, stale.statusCode());
        assertEmpty();
        assertEquals(0, processes.count(), "precondition refusals must not invoke the kernel");
    }

    @Test
    @DisplayName("UC-1 extensions 2a and 2b: every definition and baseline invalidity is refused through real HTTP")
    void completeVerificationInvalidityMatrixAtHttpBoundary() throws Exception {
        ObjectNode definition = (ObjectNode) JSON.readTree(ROOT.resolve("examples/initial-school.json"));
        ObjectNode validResult = (ObjectNode) JSON.readTree(plannedResult(
                ROOT.resolve("examples/initial-school.json"), "matrix-result.json"));
        processes.reset();

        List<ObjectNode> invalidDefinitions = new ArrayList<>();
        ObjectNode unsupportedSchema = definition.deepCopy();
        unsupportedSchema.put("schemaVersion", 2);
        invalidDefinitions.add(unsupportedSchema);
        ObjectNode unsupportedCatalog = definition.deepCopy();
        unsupportedCatalog.put("catalogVersion", 2);
        invalidDefinitions.add(unsupportedCatalog);
        ObjectNode missingName = definition.deepCopy();
        missingName.remove("displayName");
        invalidDefinitions.add(missingName);
        ObjectNode blankName = definition.deepCopy();
        blankName.put("displayName", " ");
        invalidDefinitions.add(blankName);
        ObjectNode successor = definition.deepCopy();
        successor.put("basedOnRevision", "sha256:" + "0".repeat(64));
        invalidDefinitions.add(successor);

        for (ObjectNode invalidDefinition : invalidDefinitions) {
            assertHttpVerificationFailure(invalidDefinition, null);
            assertEquals(1, processes.count());
        }

        List<ObjectNode> invalidResults = new ArrayList<>();
        ObjectNode wrongSchool = validResult.deepCopy();
        wrongSchool.put("schoolId", "another-school");
        refreshTimetableRevision(wrongSchool);
        invalidResults.add(wrongSchool);
        ObjectNode wrongRevision = validResult.deepCopy();
        wrongRevision.put("inputRevision", "sha256:" + "0".repeat(64));
        refreshTimetableRevision(wrongRevision);
        invalidResults.add(wrongRevision);
        ObjectNode incomplete = validResult.deepCopy();
        ((tools.jackson.databind.node.ArrayNode) incomplete.path("timetable").path("assignments")).remove(1);
        refreshTimetableRevision(incomplete);
        invalidResults.add(incomplete);
        ObjectNode collision = validResult.deepCopy();
        var assignments = (tools.jackson.databind.node.ArrayNode) collision.path("timetable").path("assignments");
        ((ObjectNode) assignments.get(1)).put("periodId", assignments.get(0).path("periodId").stringValue());
        refreshTimetableRevision(collision);
        invalidResults.add(collision);
        ObjectNode unknownRoom = validResult.deepCopy();
        ((ObjectNode) unknownRoom.path("timetable").path("assignments").get(0)).put("roomId", "unknown-room");
        refreshTimetableRevision(unknownRoom);
        invalidResults.add(unknownRoom);

        for (ObjectNode invalidResult : invalidResults) {
            assertHttpVerificationFailure(definition, invalidResult);
            assertEquals(1, processes.count());
        }
    }

    @Test
    @DisplayName("RULE-6: racing imports with one ETag have exactly one durable winner")
    void racingImportsHaveOneWinner() throws Exception {
        Session session = session();
        Map<String, FilePart> parts = Map.of("definition", new FilePart(
                "definition.json", Files.readAllBytes(ROOT.resolve("examples/initial-school.json"))));

        List<Integer> statuses;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> post(session, parts, true, true).statusCode());
            var second = executor.submit(() -> post(session, parts, true, true).statusCode());
            statuses = java.util.stream.Stream.of(first.get(), second.get()).sorted().toList();
        }

        assertEquals(List.of(200, 412), statuses);
        assertEquals("INITIAL_DRAFT", lifecycle());
        assertEquals(1L, version());
        assertEquals(JSON.readTree(parts.get("definition").bytes()), storedDocument().path("initialDefinition"));
    }

    @Test
    @DisplayName("UC-1 G5 and RULE-21: missing CSRF and hostile origin are denied with no state change")
    void localSecurityDenialsPreserveState() throws Exception {
        Session session = session();
        Map<String, FilePart> parts = Map.of("definition", new FilePart(
                "definition.json", Files.readAllBytes(ROOT.resolve("examples/initial-school.json"))));

        HttpResponse<String> noCsrf = post(session, parts, false, true);
        assertEquals(403, noCsrf.statusCode());
        assertCurrentProblem(noCsrf, "REQUEST_FORBIDDEN", "EMPTY", "\"ws-0\"");
        assertEmpty();

        HttpResponse<String> hostile = postWithOrigin(session, parts, "https://attacker.invalid");
        assertEquals(403, hostile.statusCode());
        assertCurrentProblem(hostile, "LOCAL_REQUEST_DENIED", "EMPTY", "\"ws-0\"");
        assertEmpty();

        HttpResponse<String> missingOrigin = post(session, parts, true, true, null);
        assertEquals(403, missingOrigin.statusCode());
        assertCurrentProblem(missingOrigin, "LOCAL_REQUEST_DENIED", "EMPTY", "\"ws-0\"");
        assertEmpty();
        assertEquals(0, processes.count());
    }

    @Test
    @DisplayName("RULE-21: the local route surface serves only the workspace and protected API")
    void localRouteSurfaceIsNarrow() throws Exception {
        HttpResponse<String> root = client.send(HttpRequest.newBuilder(uri("/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(302, root.statusCode());
        assertEquals("/workspace/", root.headers().firstValue("Location").orElseThrow());

        HttpResponse<String> page = client.send(HttpRequest.newBuilder(uri("/workspace/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("id=\"page-title\""));
        assertTrue(page.body().contains("type=\"module\" src=\"/workspace/app.js\""));
        assertTrue(page.headers().firstValue("Content-Security-Policy").orElseThrow().contains("default-src 'self'"));

        HttpResponse<String> messages = client.send(HttpRequest.newBuilder(uri("/workspace/messages.js")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, messages.statusCode());
        assertTrue(messages.body().contains("pageTitle: 'Operations workspace'"));

        HttpResponse<String> login = client.send(HttpRequest.newBuilder(uri("/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, login.statusCode());
        assertFalse(login.body().contains("password"));
        HttpResponse<String> actuator = client.send(HttpRequest.newBuilder(uri("/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, actuator.statusCode());

        HttpResponse<String> preflight = client.send(HttpRequest.newBuilder(uri("/api/import"))
                        .header("Origin", "https://attacker.invalid")
                        .header("Access-Control-Request-Method", "POST")
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, preflight.statusCode());
        assertTrue(preflight.headers().firstValue("Access-Control-Allow-Origin").isEmpty());

        HttpResponse<String> forwarded = client.send(HttpRequest.newBuilder(uri("/api/workspace"))
                        .header("X-Forwarded-Host", "attacker.invalid")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, forwarded.statusCode());

        String hostileHost = rawRequest("GET /api/workspace HTTP/1.1\r\n"
                + "Host: attacker.invalid\r\nConnection: close\r\n\r\n");
        assertTrue(hostileHost.startsWith("HTTP/1.1 403"));
        assertTrue(hostileHost.contains("LOCAL_REQUEST_DENIED"));

        String wrongLoopbackPort = rawRequest("GET /api/workspace HTTP/1.1\r\n"
                + "Host: localhost:" + (port + 1) + "\r\nConnection: close\r\n\r\n");
        assertTrue(wrongLoopbackPort.startsWith("HTTP/1.1 403"));
        assertTrue(wrongLoopbackPort.contains("LOCAL_REQUEST_DENIED"));

        HttpResponse<String> traversal = client.send(HttpRequest.newBuilder(uri("/workspace/%2e%2e/application.yml"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertTrue(traversal.statusCode() == 400 || traversal.statusCode() == 403 || traversal.statusCode() == 404);
        assertFalse(traversal.body().contains("WORKSPACE_DATABASE_PASSWORD"));

        HttpResponse<String> missing = client.send(HttpRequest.newBuilder(uri("/workspace/missing.js")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, missing.statusCode());
        assertCurrentProblem(missing, "RESOURCE_NOT_FOUND", "EMPTY", "\"ws-0\"");
    }

    @Test
    @DisplayName("RULE-22: malformed multipart uses the complete safe 400 problem shape")
    void malformedRequestProblemShape() throws Exception {
        Session session = session();
        HttpRequest request = HttpRequest.newBuilder(uri("/api/import"))
                .header("Origin", "http://localhost:" + port)
                .header(session.csrfHeader(), session.csrfToken())
                .header("If-Match", session.etag())
                .header("Content-Type", "multipart/form-data")
                .POST(HttpRequest.BodyPublishers.ofString("not multipart"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertCurrentProblem(response, "MALFORMED_REQUEST", "EMPTY", "\"ws-0\"");
        assertEmpty();
        assertEquals(0, processes.count());
    }

    @Test
    @DisplayName("UC-1 precondition and RULE-5: a second import is refused without changing the first draft")
    void occupiedWorkspaceRefusesImport() throws Exception {
        Session first = session();
        byte[] definition = Files.readAllBytes(ROOT.resolve("examples/initial-school.json"));
        HttpResponse<String> imported = post(first,
                Map.of("definition", new FilePart("definition.json", definition)), true, true);
        assertEquals(200, imported.statusCode());
        JsonNode before = storedDocument();
        long beforeVersion = version();

        Session second = session();
        HttpResponse<String> refused = post(second,
                Map.of("definition", new FilePart("definition.json", definition)), true, true);

        assertEquals(409, refused.statusCode());
        JsonNode problem = JSON.readTree(refused.body());
        assertEquals("INVALID_WORKSPACE_TRANSITION", problem.path("code").stringValue());
        assertEquals("INITIAL_DRAFT", problem.path("state").stringValue());
        assertTrue(problem.path("correlationId").isTextual());
        assertEquals(beforeVersion, version());
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("RULE-5: UC-1 import is refused from every non-empty lifecycle without process, data, or version effects")
    void exhaustiveLifecycleRefusalMatrix() throws Exception {
        byte[] definition = Files.readAllBytes(ROOT.resolve("examples/initial-school.json"));
        for (WorkspaceState state : WorkspaceState.values()) {
            if (state == WorkspaceState.EMPTY) {
                continue;
            }
            ObjectNode sentinel = JSON.createObjectNode();
            sentinel.put("sentinel", state.name());
            jdbc.sql("""
                            UPDATE workspace_aggregate
                            SET lifecycle_state=:state, version=41, active_run_id=NULL,
                                document=CAST(:document AS jsonb)
                            WHERE workspace_id=1
                            """)
                    .param("state", state.name())
                    .param("document", JSON.writeValueAsString(sentinel))
                    .update();
            processes.reset();
            Session session = session();

            HttpResponse<String> response = post(session,
                    Map.of("definition", new FilePart("definition.json", definition)), true, true);

            assertEquals(409, response.statusCode(), state.name());
            assertCurrentProblem(response, "INVALID_WORKSPACE_TRANSITION", state.name(), "\"ws-41\"");
            assertEquals(41L, version(), state.name());
            assertEquals(sentinel, storedDocument(), state.name());
            assertEquals(0, processes.count(), state.name());
        }
    }

    @Test
    @DisplayName("UC-1 extension 4a and RULE-7: storage failure rolls back import and exposes no accepted baseline")
    void storageFailureRollsBack() throws Exception {
        byte[] definition = Files.readAllBytes(ROOT.resolve("examples/initial-school.json"));
        byte[] result = Files.readAllBytes(plannedResult(
                ROOT.resolve("examples/initial-school.json"), "rollback-result.json"));
        jdbc.sql("""
                CREATE OR REPLACE FUNCTION reject_workspace_update() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'injected workspace write failure'; END;
                $$ LANGUAGE plpgsql
                """).update();
        jdbc.sql("""
                CREATE TRIGGER reject_workspace_update
                BEFORE UPDATE ON workspace_aggregate
                FOR EACH ROW EXECUTE FUNCTION reject_workspace_update()
                """).update();
        try {
            Session session = session();
            HttpResponse<String> response = post(session, Map.of(
                    "definition", new FilePart("definition.json", definition),
                    "result", new FilePart("result.json", result)), true, true);

            assertEquals(503, response.statusCode());
        } finally {
            jdbc.sql("DROP TRIGGER reject_workspace_update ON workspace_aggregate").update();
            jdbc.sql("DROP FUNCTION reject_workspace_update()").update();
        }
        assertEmpty();
    }

    @Test
    @DisplayName("RULE-7: deferred commit failure rolls back the complete accepted bundle and canonical state")
    void commitFailureRollsBackAcceptedImport() throws Exception {
        byte[] definition = Files.readAllBytes(ROOT.resolve("examples/initial-school.json"));
        byte[] result = Files.readAllBytes(plannedResult(
                ROOT.resolve("examples/initial-school.json"), "commit-failure-result.json"));
        byte[] before = CanonicalJson.bytes(storedDocument());
        jdbc.sql("""
                CREATE OR REPLACE FUNCTION reject_workspace_commit() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'injected deferred commit failure'; END;
                $$ LANGUAGE plpgsql
                """).update();
        jdbc.sql("""
                CREATE CONSTRAINT TRIGGER reject_workspace_commit
                AFTER UPDATE ON workspace_aggregate
                DEFERRABLE INITIALLY DEFERRED
                FOR EACH ROW EXECUTE FUNCTION reject_workspace_commit()
                """).update();
        try {
            Session session = session();
            HttpResponse<String> response = post(session, Map.of(
                    "definition", new FilePart("definition.json", definition),
                    "result", new FilePart("result.json", result)), true, true);
            assertEquals(503, response.statusCode());
        } finally {
            jdbc.sql("DROP TRIGGER reject_workspace_commit ON workspace_aggregate").update();
            jdbc.sql("DROP FUNCTION reject_workspace_commit()").update();
        }
        assertEmpty();
        assertTrue(java.util.Arrays.equals(before, CanonicalJson.bytes(storedDocument())));
    }

    @Test
    @DisplayName("RULE-7: database connection termination during acceptance leaves the complete prior state unchanged")
    void connectionTerminationRollsBackAcceptedImport() throws Exception {
        byte[] definition = Files.readAllBytes(ROOT.resolve("examples/initial-school.json"));
        byte[] result = Files.readAllBytes(plannedResult(
                ROOT.resolve("examples/initial-school.json"), "connection-failure-result.json"));
        byte[] before = CanonicalJson.bytes(storedDocument());
        jdbc.sql("""
                CREATE OR REPLACE FUNCTION terminate_workspace_connection() RETURNS trigger AS $$
                BEGIN
                  PERFORM pg_terminate_backend(pg_backend_pid());
                  RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """).update();
        jdbc.sql("""
                CREATE TRIGGER terminate_workspace_connection
                BEFORE UPDATE ON workspace_aggregate
                FOR EACH ROW EXECUTE FUNCTION terminate_workspace_connection()
                """).update();
        try {
            Session session = session();
            HttpResponse<String> response = post(session, Map.of(
                    "definition", new FilePart("definition.json", definition),
                    "result", new FilePart("result.json", result)), true, true);
            assertEquals(503, response.statusCode());
        } finally {
            jdbc.sql("DROP TRIGGER IF EXISTS terminate_workspace_connection ON workspace_aggregate").update();
            jdbc.sql("DROP FUNCTION IF EXISTS terminate_workspace_connection()").update();
        }
        assertEmpty();
        assertTrue(java.util.Arrays.equals(before, CanonicalJson.bytes(storedDocument())));
    }

    @Test
    @DisplayName("RULE-7: a durable accepted bundle survives a fresh application context unchanged")
    void acceptedImportSurvivesApplicationRestart() throws Exception {
        byte[] definition = Files.readAllBytes(ROOT.resolve("examples/initial-school.json"));
        byte[] result = Files.readAllBytes(plannedResult(
                ROOT.resolve("examples/initial-school.json"), "restart-result.json"));
        Session session = session();
        assertEquals(200, post(session, Map.of(
                "definition", new FilePart("definition.json", definition),
                "result", new FilePart("result.json", result)), true, true).statusCode());
        JsonNode before = storedDocument();
        byte[] canonicalBefore = CanonicalJson.bytes(before);

        SpringApplication application = new SpringApplication(WorkspaceApplication.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        try (ConfigurableApplicationContext restarted = application.run(
                "--server.port=0",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--workspace.kernel-executable=" + ROOT.resolve("school-kernel"))) {
            WorkspaceAggregate restored = restarted.getBean(WorkspaceRepository.class).load();
            assertEquals(WorkspaceState.ACCEPTED_BASELINE, restored.state());
            assertEquals(1L, restored.version());
            assertEquals(before, restored.document());
            assertTrue(java.util.Arrays.equals(canonicalBefore, CanonicalJson.bytes(restored.document())));
        }
    }

    @Test
    @DisplayName("RULE-8: Unicode, key order, whitespace, arrays and omitted fields round-trip losslessly through HTTP and JSONB")
    void canonicalWorkspaceBoundaryFidelity() throws Exception {
        ObjectNode definition = (ObjectNode) JSON.readTree(ROOT.resolve("examples/initial-school.json"));
        definition.put("displayName", "Demo Õppekool \uD83C\uDF93");
        ((ObjectNode) definition.path("subjects").get(0)).put("displayName", "Matemaatika e\u0301");
        ((ObjectNode) definition.path("teachers").get(0)).remove("undesirablePeriodIds");
        byte[] submitted = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(definition);
        Session session = session();

        HttpResponse<String> response = post(session,
                Map.of("definition", new FilePart("unicode-definition.json", submitted)), true, true);

        assertEquals(200, response.statusCode());
        JsonNode storedDefinition = storedDocument().path("initialDefinition");
        assertEquals(definition, storedDefinition);
        assertEquals("Demo Õppekool \uD83C\uDF93", storedDocument().path("school").path("displayName").stringValue());
        assertFalse(storedDefinition.path("teachers").get(0).has("undesirablePeriodIds"));
        assertTrue(java.util.Arrays.equals(CanonicalJson.bytes(definition), CanonicalJson.bytes(storedDefinition)));
    }

    private Session session() throws Exception {
        HttpResponse<String> csrf = client.send(HttpRequest.newBuilder(uri("/api/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, csrf.statusCode());
        JsonNode token = JSON.readTree(csrf.body());
        HttpResponse<String> workspace = client.send(HttpRequest.newBuilder(uri("/api/workspace")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, workspace.statusCode());
        return new Session(
                token.path("headerName").stringValue(),
                token.path("token").stringValue(),
                workspace.headers().firstValue("ETag").orElseThrow());
    }

    private HttpResponse<String> post(
            Session session, Map<String, FilePart> parts, boolean includeCsrf, boolean includeVersion) throws Exception {
        return post(session, parts, includeCsrf, includeVersion, "http://localhost:" + port);
    }

    private HttpResponse<String> postWithOrigin(Session session, Map<String, FilePart> parts, String origin) throws Exception {
        return post(session, parts, true, true, origin);
    }

    private HttpResponse<String> post(
            Session session,
            Map<String, FilePart> parts,
            boolean includeCsrf,
            boolean includeVersion,
            String origin) throws Exception {
        String boundary = "----workspace-" + UUID.randomUUID();
        byte[] body = multipart(boundary, parts);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri("/api/import"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (origin != null) {
            request.header("Origin", origin);
        }
        if (includeCsrf) {
            request.header(session.csrfHeader(), session.csrfToken());
        }
        if (includeVersion) {
            request.header("If-Match", session.etag());
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private Path plannedResult(Path definition, String name) throws Exception {
        Path result = temporaryDirectory.resolve(name);
        Process process = new ProcessBuilder(
                ROOT.resolve("school-kernel").toString(),
                "plan", "--definition", definition.toString(), "--output", result.toString(),
                "--step-limit", "100").start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue(), new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        return result;
    }

    private void assertHttpVerificationFailure(ObjectNode definition, ObjectNode result) throws Exception {
        resetWorkspace();
        Session session = session();
        Map<String, FilePart> parts = result == null
                ? Map.of("definition", new FilePart("definition.json", JSON.writeValueAsBytes(definition)))
                : Map.of(
                        "definition", new FilePart("definition.json", JSON.writeValueAsBytes(definition)),
                        "result", new FilePart("result.json", JSON.writeValueAsBytes(result)));
        HttpResponse<String> response = post(session, parts, true, true);
        assertEquals(422, response.statusCode());
        assertCurrentProblem(response, "KERNEL_VERIFICATION_FAILED", "EMPTY", "\"ws-0\"");
        assertFalse(response.body().contains("assignments"));
        assertEmpty();
    }

    private static void assertCurrentProblem(
            HttpResponse<String> response, String code, String state, String etag) throws Exception {
        JsonNode problem = JSON.readTree(response.body());
        assertEquals(code, problem.path("code").stringValue());
        assertTrue(problem.path("message").isTextual());
        assertTrue(problem.path("correlationId").isTextual());
        assertEquals(state, problem.path("state").stringValue());
        assertEquals(etag, problem.path("etag").stringValue());
        assertEquals(etag, response.headers().firstValue("ETag").orElseThrow());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
    }

    private static void refreshTimetableRevision(ObjectNode result) throws Exception {
        ObjectNode scope = JSON.createObjectNode();
        scope.put("schemaVersion", result.path("schemaVersion").intValue());
        scope.put("schoolId", result.path("schoolId").stringValue());
        scope.put("inputRevision", result.path("inputRevision").stringValue());
        scope.set("assignments", result.path("timetable").path("assignments"));
        result.put("timetableRevision", "sha256:" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(CanonicalJson.bytes(scope))));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String rawRequest(String request) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String lifecycle() {
        return jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single();
    }

    private long version() {
        return jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1")
                .query(Long.class).single();
    }

    private JsonNode storedDocument() throws Exception {
        return JSON.readTree(jdbc.sql("SELECT document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single());
    }

    private void assertEmpty() throws Exception {
        assertEquals("EMPTY", lifecycle());
        assertEquals(0L, version());
        assertEquals(JSON.readTree("{}"), storedDocument());
    }

    private static byte[] multipart(String boundary, Map<String, FilePart> parts) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (var entry : parts.entrySet()) {
            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"; filename=\""
                    + entry.getValue().filename() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            body.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(entry.getValue().bytes());
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return body.toByteArray();
    }

    private static byte[] zip(Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] rawZip(List<ArchiveEntry> entries) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (ArchiveEntry entry : entries) {
            byte[] name = entry.name().getBytes(StandardCharsets.UTF_8);
            CRC32 crc = new CRC32();
            crc.update(entry.bytes());
            little(bytes, 0x04034b50, 4);
            little(bytes, 20, 2);
            little(bytes, entry.encrypted() ? 1 : 0, 2);
            little(bytes, 0, 2);
            little(bytes, 0, 2);
            little(bytes, 0, 2);
            little(bytes, crc.getValue(), 4);
            little(bytes, entry.bytes().length, 4);
            little(bytes, entry.bytes().length, 4);
            little(bytes, name.length, 2);
            little(bytes, 0, 2);
            bytes.write(name);
            bytes.write(entry.bytes());
        }
        return bytes.toByteArray();
    }

    private static void little(ByteArrayOutputStream output, long value, int bytes) {
        for (int index = 0; index < bytes; index++) {
            output.write((int) (value >>> (8 * index)) & 0xff);
        }
    }

    private record Session(String csrfHeader, String csrfToken, String etag) {}
    private record FilePart(String filename, byte[] bytes) {}
    private record ArchiveEntry(String name, byte[] bytes, boolean encrypted) {}
    private record ArchiveCase(byte[] bytes, int status, String code) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingConfiguration {
        @Bean
        @Primary
        RecordingProcessLauncher recordingProcessLauncher() {
            return new RecordingProcessLauncher();
        }
    }

    static final class RecordingProcessLauncher extends KernelProcessLauncher {
        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public Process start(List<String> arguments) throws java.io.IOException {
            invocations.incrementAndGet();
            return super.start(arguments);
        }

        int count() {
            return invocations.get();
        }

        void reset() {
            invocations.set(0);
        }
    }
}
