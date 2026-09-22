package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
@SpringBootTest(
        classes = {WorkspaceApplication.class, WorkspaceInitialPlanningIT.ProcessConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkspaceInitialPlanningIT {
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("initial_planning")
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
    SwitchingProcessLauncher processes;

    @Autowired
    WorkspaceRecovery recovery;

    private HttpClient client;

    @BeforeEach
    void reset() {
        jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state='EMPTY', version=0, active_run_id=NULL, document='{}'::jsonb
                        WHERE workspace_id=1
                        """).update();
        processes.reset();
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test
    @DisplayName("UC-2 main, G1-G6, RULE-10/11/17: real HTTP and packaged plan create only a proposal until explicit atomic acceptance")
    void plansAndExplicitlyAcceptsInitialTimetable() throws Exception {
        JsonNode definition = importInitial(ROOT.resolve("examples/initial-school.json"));
        Session beforeRun = session();

        HttpResponse<String> started = command("POST", "/api/runs", beforeRun, null);
        assertEquals(202, started.statusCode());
        JsonNode solving = JSON.readTree(started.body());
        assertEquals("SOLVING_INITIAL", solving.path("state").stringValue());
        assertFalse(solving.path("acceptedTimetable").booleanValue());
        assertEquals("PT30S", solving.path("workspace").path("run").path("limit").stringValue());
        UUID runId = UUID.fromString(solving.path("workspace").path("run").path("id").stringValue());

        JsonNode proposalSnapshot = awaitState("INITIAL_PROPOSAL", Duration.ofSeconds(20));
        JsonNode proposal = proposalSnapshot.path("workspace").path("proposal");
        assertEquals("INITIAL", proposal.path("kind").stringValue());
        assertEquals("NO_BASELINE", proposal.path("acceptedTimetableRevision").stringValue());
        assertEquals("NO_INTENT", proposal.path("intentRevision").stringValue());
        assertEquals("FEASIBLE", proposal.path("result").path("status").stringValue());
        assertEquals(2, proposal.path("result").path("timetable").path("assignments").size());
        assertEquals("PT30S", proposal.path("result").path("limit").path("duration").stringValue());
        assertFalse(proposalSnapshot.path("acceptedTimetable").booleanValue());
        assertEquals("FEASIBLE", JSON.readTree(get("/api/runs/" + runId).body()).path("status").stringValue());

        Session proposalSession = session();
        HttpResponse<String> acceptedResponse = command("POST", "/api/proposal/accept", proposalSession, null);
        assertEquals(200, acceptedResponse.statusCode());
        JsonNode acceptedSnapshot = JSON.readTree(acceptedResponse.body());
        assertEquals("ACCEPTED_BASELINE", acceptedSnapshot.path("state").stringValue());
        assertTrue(acceptedSnapshot.path("acceptedTimetable").booleanValue());
        JsonNode accepted = acceptedSnapshot.path("workspace").path("acceptedBaseline");
        assertEquals(definition, accepted.path("definition"));
        assertEquals(proposal.path("result"), accepted.path("result"));
        assertEquals(proposal.path("proposedTimetableRevision").stringValue(),
                acceptedSnapshot.path("workspace").path("timetableRevision").stringValue());
        assertFalse(acceptedSnapshot.path("workspace").has("proposal"));
        assertFalse(acceptedSnapshot.path("workspace").has("initialDefinition"));

        List<String> plan = processes.commands().stream().filter(command -> command.contains("plan")).findFirst().orElseThrow();
        assertEquals("plan", plan.get(1));
        assertTrue(plan.containsAll(List.of("--definition", "--output", "--time-limit", "30s", "--correlation-id")));
        assertFalse(plan.contains("--seed"));
        assertFalse(plan.contains("--step-limit"));
        assertNotEquals(plan.get(plan.indexOf("--definition") + 1), plan.get(plan.indexOf("--output") + 1));
        assertTrue(processes.commands().stream().anyMatch(command -> command.contains("verify")));
    }

    @Test
    @DisplayName("UC-2 extensions 2a and 4a: cancel and discard return to the unchanged initial draft with no proposal")
    void cancelAndDiscardPreserveInitialDraft() throws Exception {
        JsonNode definition = importInitial(ROOT.resolve("examples/initial-school.json"));
        processes.blockPlan = true;
        HttpResponse<String> started = command("POST", "/api/runs", session(), null);
        JsonNode solving = JSON.readTree(started.body());
        UUID runId = UUID.fromString(solving.path("workspace").path("run").path("id").stringValue());
        processes.awaitPlan();

        HttpResponse<String> cancelled = command("DELETE", "/api/runs/" + runId, session(), null);
        assertEquals(200, cancelled.statusCode());
        JsonNode cancelledState = JSON.readTree(cancelled.body());
        assertEquals("INITIAL_DRAFT", cancelledState.path("state").stringValue());
        assertEquals(definition, cancelledState.path("workspace").path("initialDefinition"));
        assertEquals("CANCELLED", cancelledState.path("workspace").path("lastRun").path("status").stringValue());
        assertFalse(cancelledState.path("workspace").has("proposal"));
        Thread.sleep(300);
        assertEquals("INITIAL_DRAFT", lifecycle());

        processes.blockPlan = false;
        command("POST", "/api/runs", session(), null);
        awaitState("INITIAL_PROPOSAL", Duration.ofSeconds(20));
        HttpResponse<String> discarded = command("DELETE", "/api/proposal", session(), null);
        assertEquals(200, discarded.statusCode());
        JsonNode draft = JSON.readTree(discarded.body());
        assertEquals("INITIAL_DRAFT", draft.path("state").stringValue());
        assertEquals(definition, draft.path("workspace").path("initialDefinition"));
        assertFalse(draft.path("workspace").has("proposal"));
    }

    @Test
    @DisplayName("UC-2 extensions 1a and 3a: replacement is atomic and unsuccessful search retains the exact replacement")
    void replacesDraftAndPreservesItAfterUnsuccessfulSearch() throws Exception {
        importInitial(ROOT.resolve("examples/initial-school.json"));
        JsonNode replacement = JSON.readTree(ROOT.resolve("kernel-cli/src/test/resources/fixtures/search-conflict-plan.json"));
        ((ObjectNode) replacement).put("displayName", "Conflict School");
        HttpResponse<String> replaced = multipart(
                "/api/initial-draft/replace", session(), "definition", "replacement.json",
                JSON.writeValueAsBytes(replacement));
        assertEquals(200, replaced.statusCode());
        JsonNode storedReplacement = JSON.readTree(replaced.body()).path("workspace").path("initialDefinition");
        assertEquals(replacement, storedReplacement);

        command("POST", "/api/runs", session(), null);
        JsonNode failed = awaitState("INITIAL_DRAFT", Duration.ofSeconds(20));
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND",
                failed.path("workspace").path("lastRun").path("code").stringValue());
        assertEquals("No feasible timetable was found within this run.",
                failed.path("workspace").path("lastRun").path("message").stringValue());
        assertEquals(replacement, failed.path("workspace").path("initialDefinition"));
        assertFalse(failed.path("workspace").has("proposal"));
        assertFalse(failed.path("acceptedTimetable").booleanValue());

        JsonNode beforeInvalid = storedDocument();
        long beforeVersion = version();
        ObjectNode invalid = ((ObjectNode) replacement).deepCopy();
        invalid.remove("schoolId");
        HttpResponse<String> rejected = multipart(
                "/api/initial-draft/replace", session(), "definition", "invalid.json",
                JSON.writeValueAsBytes(invalid));
        assertEquals(422, rejected.statusCode());
        assertEquals(beforeVersion, version());
        assertEquals(beforeInvalid, storedDocument());
    }

    @Test
    @DisplayName("UC-2 extension 3b and RULE-10: mismatched packaged output returns to the exact draft with no candidate")
    void mismatchedKernelOutputNeverBecomesProposal() throws Exception {
        JsonNode definition = importInitial(ROOT.resolve("examples/initial-school.json"));
        processes.mismatchPlanEvidence = true;

        command("POST", "/api/runs", session(), null);
        JsonNode failed = awaitState("INITIAL_DRAFT", Duration.ofSeconds(20));

        assertEquals("REJECTED_OUTPUT", failed.path("workspace").path("lastRun").path("code").stringValue());
        assertEquals(definition, failed.path("workspace").path("initialDefinition"));
        assertFalse(failed.path("workspace").has("proposal"));
        assertFalse(failed.path("acceptedTimetable").booleanValue());
    }

    @Test
    @DisplayName("UC-2 extension 3a and RULE-22: every planning failure class returns safe HTTP diagnostics and the exact draft")
    void planningFailureClassesReturnSafeDiagnosticsAndExactDraft() throws Exception {
        List<FailureExpectation> failures = List.of(
                new FailureExpectation(PlanFailure.INVALID_INPUT, "INVALID_INPUT",
                        "School Kernel rejected the initial definition."),
                new FailureExpectation(PlanFailure.INTERNAL_ERROR, "INTERNAL_ERROR",
                        "School Kernel could not complete planning."),
                new FailureExpectation(PlanFailure.TRANSPORT_FAILURE, "TRANSPORT_FAILURE",
                        "School Kernel could not be started."),
                new FailureExpectation(PlanFailure.INTERRUPTED, "INTERRUPTED",
                        "Planning was interrupted."));

        for (FailureExpectation failure : failures) {
            reset();
            JsonNode definition = importInitial(ROOT.resolve("examples/initial-school.json"));
            processes.planFailure = failure.failure();

            command("POST", "/api/runs", session(), null);
            JsonNode failed = awaitState("INITIAL_DRAFT", Duration.ofSeconds(20));

            JsonNode lastRun = failed.path("workspace").path("lastRun");
            assertEquals(failure.code(), lastRun.path("code").stringValue(), failure.failure().name());
            assertEquals(failure.message(), lastRun.path("message").stringValue(), failure.failure().name());
            assertEquals(definition, failed.path("workspace").path("initialDefinition"));
            assertFalse(failed.path("workspace").has("proposal"));
            assertFalse(failed.path("acceptedTimetable").booleanValue());
            assertFalse(failed.toString().contains(ConfiguredFailureProcess.RAW_DIAGNOSTIC));
        }
    }

    @Test
    @DisplayName("UC-2 extensions 2b, 5a and 6a: restart, stale identity, and acceptance storage failure never create accepted state")
    void restartStaleIdentityAndStorageFailureAreSafe() throws Exception {
        JsonNode definition = importInitial(ROOT.resolve("examples/initial-school.json"));
        ObjectNode interrupted = (ObjectNode) storedDocument().deepCopy();
        UUID interruptedRun = UUID.randomUUID();
        interrupted.putObject("run").put("id", interruptedRun.toString()).put("status", "RUNNING");
        jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state='SOLVING_INITIAL', version=version+1,
                            active_run_id=:run, document=CAST(:document AS jsonb)
                        WHERE workspace_id=1
                        """)
                .param("run", interruptedRun)
                .param("document", JSON.writeValueAsString(interrupted))
                .update();
        recovery.recoverInterruptedRun();
        assertEquals("INITIAL_DRAFT", lifecycle());
        assertEquals(definition, storedDocument().path("initialDefinition"));
        assertFalse(storedDocument().has("run"));
        assertFalse(storedDocument().has("proposal"));

        command("POST", "/api/runs", session(), null);
        awaitState("INITIAL_PROPOSAL", Duration.ofSeconds(20));
        ObjectNode corrupted = (ObjectNode) storedDocument().deepCopy();
        ((ObjectNode) corrupted.path("proposal")).put("proposedTimetableRevision", "sha256:" + "0".repeat(64));
        jdbc.sql("UPDATE workspace_aggregate SET document=CAST(:document AS jsonb) WHERE workspace_id=1")
                .param("document", JSON.writeValueAsString(corrupted)).update();
        HttpResponse<String> stale = command("POST", "/api/proposal/accept", session(), null);
        assertEquals(409, stale.statusCode());
        assertEquals("STALE_PROPOSAL", JSON.readTree(stale.body()).path("code").stringValue());
        assertEquals("INITIAL_DRAFT", lifecycle());
        assertFalse(storedDocument().has("proposal"));
        assertEquals(definition, storedDocument().path("initialDefinition"));

        command("POST", "/api/runs", session(), null);
        awaitState("INITIAL_PROPOSAL", Duration.ofSeconds(20));
        ObjectNode invalidResult = (ObjectNode) storedDocument().deepCopy();
        ((ObjectNode) invalidResult.path("proposal").path("result")).put("schoolId", "wrong-school");
        jdbc.sql("UPDATE workspace_aggregate SET document=CAST(:document AS jsonb) WHERE workspace_id=1")
                .param("document", JSON.writeValueAsString(invalidResult)).update();
        HttpResponse<String> revalidationFailure = command("POST", "/api/proposal/accept", session(), null);
        assertEquals(409, revalidationFailure.statusCode());
        assertEquals("STALE_PROPOSAL", JSON.readTree(revalidationFailure.body()).path("code").stringValue());
        assertEquals("INITIAL_DRAFT", lifecycle());
        assertFalse(storedDocument().has("proposal"));
        assertEquals(definition, storedDocument().path("initialDefinition"));

        command("POST", "/api/runs", session(), null);
        awaitState("INITIAL_PROPOSAL", Duration.ofSeconds(20));
        JsonNode beforeVerifierFailure = storedDocument();
        long beforeVerifierFailureVersion = version();
        processes.failVerify = true;
        HttpResponse<String> verifierFailure = command("POST", "/api/proposal/accept", session(), null);
        processes.failVerify = false;
        assertEquals(503, verifierFailure.statusCode());
        assertEquals("KERNEL_UNAVAILABLE", JSON.readTree(verifierFailure.body()).path("code").stringValue());
        assertEquals("INITIAL_PROPOSAL", lifecycle());
        assertEquals(beforeVerifierFailureVersion, version());
        assertEquals(beforeVerifierFailure, storedDocument());

        JsonNode beforeAcceptance = storedDocument();
        long beforeAcceptanceVersion = version();
        jdbc.sql("""
                        CREATE OR REPLACE FUNCTION fail_initial_accept() RETURNS trigger AS $$
                        BEGIN
                          IF NEW.lifecycle_state = 'ACCEPTED_BASELINE' THEN
                            RAISE EXCEPTION 'injected acceptance failure';
                          END IF;
                          RETURN NEW;
                        END;
                        $$ LANGUAGE plpgsql
                        """).update();
        jdbc.sql("""
                        CREATE TRIGGER fail_initial_accept_trigger
                        BEFORE UPDATE ON workspace_aggregate
                        FOR EACH ROW EXECUTE FUNCTION fail_initial_accept()
                        """).update();
        try {
            HttpResponse<String> failedAcceptance = command("POST", "/api/proposal/accept", session(), null);
            assertEquals(503, failedAcceptance.statusCode());
            assertEquals("STORAGE_UNAVAILABLE", JSON.readTree(failedAcceptance.body()).path("code").stringValue());
            assertEquals("INITIAL_PROPOSAL", lifecycle());
            assertEquals(beforeAcceptanceVersion, version());
            assertEquals(beforeAcceptance, storedDocument());
        } finally {
            jdbc.sql("DROP TRIGGER IF EXISTS fail_initial_accept_trigger ON workspace_aggregate").update();
            jdbc.sql("DROP FUNCTION IF EXISTS fail_initial_accept()").update();
        }
        recovery.recoverInterruptedRun();
        assertEquals("INITIAL_DRAFT", lifecycle());
        assertEquals(definition, storedDocument().path("initialDefinition"));
        assertFalse(storedDocument().has("proposal"));
    }

    @Test
    @DisplayName("UC-2 RULE-6/11/12/21/22: preconditions, single-run guard, and route surface reject without process or state changes")
    void concurrencyAndRouteGuardsAreMutationFree() throws Exception {
        importInitial(ROOT.resolve("examples/initial-school.json"));
        Session current = session();
        int before = processes.commands().size();
        HttpResponse<String> missing = commandWithoutVersion("POST", "/api/runs", current);
        assertEquals(428, missing.statusCode());
        assertEquals("PRECONDITION_REQUIRED", JSON.readTree(missing.body()).path("code").stringValue());
        assertEquals(before, processes.commands().size());

        processes.blockPlan = true;
        HttpResponse<String> started = command("POST", "/api/runs", session(), null);
        assertEquals(202, started.statusCode());
        processes.awaitPlan();
        JsonNode beforeConflict = storedDocument();
        long beforeConflictVersion = version();
        int afterFirstProcess = processes.commands().size();
        HttpResponse<String> second = command("POST", "/api/runs", session(), null);
        assertEquals(409, second.statusCode());
        assertEquals("INVALID_WORKSPACE_TRANSITION", JSON.readTree(second.body()).path("code").stringValue());
        assertEquals(beforeConflictVersion, version());
        assertEquals(beforeConflict, storedDocument());
        assertEquals(afterFirstProcess, processes.commands().size());

        HttpResponse<String> forbidden = client.send(HttpRequest.newBuilder(uri("/api/runs"))
                        .header(current.csrfHeader(), current.csrfToken())
                        .header("If-Match", session().etag())
                        .header("Origin", "https://attacker.invalid")
                        .POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, forbidden.statusCode());
        assertFalse(forbidden.body().contains("assignments"));

        JsonNode solving = JSON.readTree(started.body());
        command("DELETE", "/api/runs/" + solving.path("workspace").path("run").path("id").stringValue(), session(), null);
    }

    @Test
    @DisplayName("UC-2 extension 5a and RULE-17: every proposal identity and the stored result are revalidated before acceptance")
    void rejectsEveryStaleProposalIdentity() throws Exception {
        JsonNode definition = importInitial(ROOT.resolve("examples/initial-school.json"));
        command("POST", "/api/runs", session(), null);
        awaitState("INITIAL_PROPOSAL", Duration.ofSeconds(20));
        ObjectNode pristine = (ObjectNode) storedDocument().deepCopy();
        List<Consumer<ObjectNode>> corruptions = List.of(
                document -> ((ObjectNode) document.path("proposal")).put("sourceWorkspaceVersion", -1),
                document -> ((ObjectNode) document.path("proposal")).put("acceptedTimetableRevision", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("successorDefinitionRevision", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("intentRevision", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("proposedTimetableRevision", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("runId", UUID.randomUUID().toString()),
                document -> ((ObjectNode) document.path("proposal")).put("limit", "PT2M"),
                document -> ((ObjectNode) document.path("proposal")).put("terminationReason", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("elapsedTimeMs", -1),
                document -> ((ObjectNode) document.path("proposal").path("changeCounts")).put("lessonCount", -1),
                document -> ((ObjectNode) document.path("proposal").path("result")).put("schoolId", "wrong-school"));

        long arrangedVersion = 100;
        for (Consumer<ObjectNode> corruption : corruptions) {
            ObjectNode corrupted = pristine.deepCopy();
            ((ObjectNode) corrupted.path("proposal")).put("sourceWorkspaceVersion", arrangedVersion - 2);
            corruption.accept(corrupted);
            jdbc.sql("""
                            UPDATE workspace_aggregate
                            SET lifecycle_state='INITIAL_PROPOSAL', version=:version,
                                active_run_id=NULL, document=CAST(:document AS jsonb)
                            WHERE workspace_id=1
                            """)
                    .param("version", arrangedVersion)
                    .param("document", JSON.writeValueAsString(corrupted))
                    .update();

            HttpResponse<String> response = command("POST", "/api/proposal/accept", session(), null);

            assertEquals(409, response.statusCode());
            assertEquals("STALE_PROPOSAL", JSON.readTree(response.body()).path("code").stringValue());
            assertEquals("INITIAL_DRAFT", lifecycle());
            assertEquals(definition, storedDocument().path("initialDefinition"));
            assertFalse(storedDocument().has("proposal"));
            assertFalse(storedDocument().has("acceptedBaseline"));
            arrangedVersion += 2;
        }
    }

    private JsonNode importInitial(Path path) throws Exception {
        HttpResponse<String> imported = multipart(
                "/api/import", session(), "definition", path.getFileName().toString(), Files.readAllBytes(path));
        assertEquals(200, imported.statusCode(), imported.body());
        JsonNode snapshot = JSON.readTree(imported.body());
        assertEquals("INITIAL_DRAFT", snapshot.path("state").stringValue());
        return snapshot.path("workspace").path("initialDefinition");
    }

    private JsonNode awaitState(String state, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        JsonNode last = null;
        while (System.nanoTime() < deadline) {
            HttpResponse<String> response = get("/api/workspace");
            last = JSON.readTree(response.body());
            if (state.equals(last.path("state").stringValue())) return last;
            Thread.sleep(50);
        }
        throw new AssertionError("Did not reach " + state + "; last snapshot=" + last);
    }

    private Session session() throws Exception {
        JsonNode token = JSON.readTree(get("/api/csrf").body());
        HttpResponse<String> workspace = get("/api/workspace");
        return new Session(
                token.path("headerName").stringValue(), token.path("token").stringValue(),
                workspace.headers().firstValue("ETag").orElseThrow());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> command(String method, String path, Session session, byte[] body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header(session.csrfHeader(), session.csrfToken())
                .header("If-Match", session.etag())
                .header("Origin", "http://localhost:" + port)
                .method(method, publisher);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> commandWithoutVersion(String method, String path, Session session) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path))
                        .header(session.csrfHeader(), session.csrfToken())
                        .header("Origin", "http://localhost:" + port)
                        .method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> multipart(
            String path, Session session, String name, String filename, byte[] bytes) throws Exception {
        String boundary = "----workspace-" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write("Content-Type: application/json\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        body.write(bytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header(session.csrfHeader(), session.csrfToken())
                        .header("If-Match", session.etag())
                        .header("Origin", "http://localhost:" + port)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
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

    private record Session(String csrfHeader, String csrfToken, String etag) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class ProcessConfiguration {
        @Bean
        @Primary
        SwitchingProcessLauncher switchingProcessLauncher() {
            return new SwitchingProcessLauncher();
        }
    }

    static final class SwitchingProcessLauncher extends KernelProcessLauncher {
        private final CopyOnWriteArrayList<List<String>> commands = new CopyOnWriteArrayList<>();
        volatile boolean blockPlan;
        volatile boolean failVerify;
        volatile boolean mismatchPlanEvidence;
        volatile PlanFailure planFailure;
        volatile Process blockingProcess;

        @Override
        public Process start(List<String> arguments) throws java.io.IOException {
            commands.add(List.copyOf(arguments));
            if (failVerify && arguments.size() > 1 && "verify".equals(arguments.get(1))) {
                return new ProcessBuilder("/usr/bin/false").start();
            }
            if (blockPlan && arguments.size() > 1 && "plan".equals(arguments.get(1))) {
                blockingProcess = new ProcessBuilder("/bin/sleep", "60").start();
                return blockingProcess;
            }
            if (arguments.size() > 1 && "plan".equals(arguments.get(1))) {
                if (planFailure == PlanFailure.TRANSPORT_FAILURE) {
                    throw new java.io.IOException(ConfiguredFailureProcess.RAW_DIAGNOSTIC);
                }
                if (planFailure == PlanFailure.INTERRUPTED) {
                    return new InjectedInterruptedProcess();
                }
                if (planFailure != null) {
                    Path output = Path.of(arguments.get(arguments.indexOf("--output") + 1));
                    ObjectNode result = JSON.createObjectNode();
                    result.put("status", planFailure.name());
                    result.put("elapsedTimeMs", 1);
                    result.put("terminationReason", "TIME_LIMIT");
                    if (planFailure == PlanFailure.INVALID_INPUT) {
                        result.putObject("validationReport").putArray("errors");
                    }
                    Files.write(output, JSON.writeValueAsBytes(result));
                    return new ConfiguredFailureProcess(planFailure == PlanFailure.INVALID_INPUT ? 2 : 4);
                }
                ArrayList<String> accelerated = new ArrayList<>(arguments);
                int limit = accelerated.indexOf("--time-limit");
                accelerated.set(limit, "--step-limit");
                accelerated.set(limit + 1, "100");
                Path output = Path.of(arguments.get(arguments.indexOf("--output") + 1));
                return new LimitRewritingProcess(
                        super.start(accelerated), output, mismatchPlanEvidence ? "PT2M" : "PT30S");
            }
            return super.start(arguments);
        }

        void awaitPlan() throws Exception {
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (blockingProcess == null && System.nanoTime() < deadline) Thread.sleep(20);
            assertTrue(blockingProcess != null && blockingProcess.isAlive());
        }

        List<List<String>> commands() {
            return List.copyOf(commands);
        }

        void reset() {
            blockPlan = false;
            failVerify = false;
            mismatchPlanEvidence = false;
            planFailure = null;
            if (blockingProcess != null && blockingProcess.isAlive()) blockingProcess.destroyForcibly();
            blockingProcess = null;
            commands.clear();
        }
    }

    private enum PlanFailure { INVALID_INPUT, INTERNAL_ERROR, TRANSPORT_FAILURE, INTERRUPTED }

    private record FailureExpectation(PlanFailure failure, String code, String message) {}

    static final class ConfiguredFailureProcess extends Process {
        static final String RAW_DIAGNOSTIC =
                "jdbc:postgresql://secret/path SQL stack trace Teacher One assignment";
        private final int exit;

        ConfiguredFailureProcess(int exit) {
            this.exit = exit;
        }

        @Override
        public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }

        @Override
        public InputStream getInputStream() { return InputStream.nullInputStream(); }

        @Override
        public InputStream getErrorStream() {
            return new java.io.ByteArrayInputStream(RAW_DIAGNOSTIC.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public int waitFor() { return exit; }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) { return true; }

        @Override
        public int exitValue() { return exit; }

        @Override
        public void destroy() {}

        @Override
        public Process destroyForcibly() { return this; }

        @Override
        public boolean isAlive() { return false; }
    }

    static final class InjectedInterruptedProcess extends Process {
        private volatile boolean alive = true;

        @Override
        public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }

        @Override
        public InputStream getInputStream() { return InputStream.nullInputStream(); }

        @Override
        public InputStream getErrorStream() { return InputStream.nullInputStream(); }

        @Override
        public int waitFor() throws InterruptedException { throw new InterruptedException("injected"); }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException("injected");
        }

        @Override
        public int exitValue() {
            if (alive) throw new IllegalThreadStateException();
            return 137;
        }

        @Override
        public void destroy() {}

        @Override
        public Process destroyForcibly() { alive = false; return this; }

        @Override
        public boolean isAlive() { return alive; }
    }

    static final class LimitRewritingProcess extends Process {
        private final Process delegate;
        private final Path output;
        private final String duration;
        private final AtomicBoolean rewritten = new AtomicBoolean();

        LimitRewritingProcess(Process delegate, Path output, String duration) {
            this.delegate = delegate;
            this.output = output;
            this.duration = duration;
        }

        @Override
        public OutputStream getOutputStream() { return delegate.getOutputStream(); }

        @Override
        public InputStream getInputStream() { return delegate.getInputStream(); }

        @Override
        public InputStream getErrorStream() { return delegate.getErrorStream(); }

        @Override
        public int waitFor() throws InterruptedException {
            int exit = delegate.waitFor();
            rewrite(exit);
            return exit;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            boolean finished = delegate.waitFor(timeout, unit);
            if (finished) rewrite(delegate.exitValue());
            return finished;
        }

        private void rewrite(int exit) {
            if (exit != 0 || !rewritten.compareAndSet(false, true)) return;
            try {
                ObjectNode result = (ObjectNode) JSON.readTree(output);
                ObjectNode limit = result.putObject("limit");
                limit.put("type", "TIME");
                limit.put("duration", duration);
                if ("STEP_LIMIT".equals(result.path("terminationReason").stringValue())) {
                    result.put("terminationReason", "TIME_LIMIT");
                }
                Files.write(output, JSON.writeValueAsBytes(result));
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
        }

        @Override
        public int exitValue() { return delegate.exitValue(); }

        @Override
        public void destroy() { delegate.destroy(); }

        @Override
        public Process destroyForcibly() { delegate.destroyForcibly(); return this; }

        @Override
        public boolean isAlive() { return delegate.isAlive(); }
    }
}
