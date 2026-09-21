package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
@SpringBootTest(
        classes = {WorkspaceApplication.class, WorkspaceRepairPlanningIT.ProcessConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkspaceRepairPlanningIT {
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
    private static volatile ObjectNode cachedProposalWithoutPin;
    private static volatile ObjectNode cachedProposalWithPin;

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
    @Autowired RepairDraftService drafts;
    @Autowired ProposalReviewService reviews;
    @Autowired SwitchingRepairProcessLauncher processes;

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
        assertEquals(6, proposal.path("review").path("categories").size());
        assertEquals(proposal.path("review").path("uniqueChangedLessonCount").intValue(),
                proposal.path("review").path("changedLessons").size());
    }

    @Test
    @DisplayName("UC-6 main/G1-G6/success and RULE-7/17/18: explicit acceptance atomically advances the exact verified repair and clears draft state")
    void acceptsExactRepairProposalAndStartsNextRepairWithoutPriorAttemptPins() throws Exception {
        JsonNode proposalSnapshot = createRepairProposal(true);
        JsonNode oldAccepted = proposalSnapshot.path("workspace").path("acceptedBaseline").deepCopy();
        JsonNode proposal = proposalSnapshot.path("workspace").path("proposal").deepCopy();
        assertTrue(proposal.path("review").path("categories").valueStream()
                .allMatch(category -> category.has("count") && category.path("lessonIds").isArray()));
        assertEquals(proposalSnapshot.path("version").longValue() - 2,
                proposal.path("sourceWorkspaceVersion").longValue(), "source version");
        assertEquals(proposalSnapshot.path("workspace").path("acceptedBaseline").path("result").path("timetableRevision"),
                proposal.path("acceptedTimetableRevision"), "accepted timetable identity");
        assertEquals(proposalSnapshot.path("workspace").path("repairDraft").path("intentRevision"),
                proposal.path("intentRevision"), "intent identity");
        assertEquals(proposal.path("result").path("inputRevision"), proposal.path("successorDefinitionRevision"),
                "definition identity");
        assertEquals(proposal.path("result").path("timetableRevision"), proposal.path("proposedTimetableRevision"),
                "proposed timetable identity");
        assertTrue(java.util.Arrays.equals(
                CanonicalJson.bytes(reviews.create(
                        proposalSnapshot.path("workspace").path("acceptedBaseline"),
                        proposalSnapshot.path("workspace").path("repairDraft"),
                        proposal.path("definition"), proposal.path("result"))),
                CanonicalJson.bytes(proposal.path("review"))), "review identity");

        HttpResponse<String> acceptedResponse = command("POST", "/api/proposal/accept", session(), null);
        assertEquals(200, acceptedResponse.statusCode(), acceptedResponse.body());
        JsonNode accepted = body(acceptedResponse);
        assertEquals("ACCEPTED_BASELINE", accepted.path("state").stringValue());
        assertEquals(proposal.path("definition"), accepted.path("workspace").path("acceptedBaseline").path("definition"));
        assertEquals(proposal.path("result"), accepted.path("workspace").path("acceptedBaseline").path("result"));
        assertFalse(accepted.path("workspace").has("proposal"));
        assertFalse(accepted.path("workspace").has("repairDraft"));
        assertEquals(proposal.path("proposedTimetableRevision"), accepted.path("workspace").path("timetableRevision"));
        JsonNode roomPin = accepted.path("workspace").path("acceptedBaseline").path("manifest").path("locks")
                .valueStream().filter(lock -> "lesson-science-1".equals(lock.path("lessonId").stringValue()))
                .findFirst().orElseThrow();
        assertEquals("ATTEMPT_SCOPED", roomPin.path("roomLockOrigin").stringValue());
        assertFalse(oldAccepted.equals(accepted.path("workspace").path("acceptedBaseline")));

        command("POST", "/api/repair-draft", session(),
                "{\"resourceType\":\"TEACHER\",\"resourceId\":\"teacher-alex\",\"periodIds\":[\"mon-2\"]}");
        JsonNode nextDefinition = drafts.compiledDefinition(storedDocument());
        JsonNode nextLesson = nextDefinition.path("lessons").valueStream()
                .filter(lesson -> "lesson-science-1".equals(lesson.path("id").stringValue()))
                .findFirst().orElseThrow();
        assertFalse(nextLesson.has("roomLock"), "a prior attempt-scoped pin must not carry into the next repair");
        assertEquals(proposal.path("result").path("inputRevision"), nextDefinition.path("basedOnRevision"));
    }

    @Test
    @DisplayName("UC-6 extensions 5a/5b and minimal guarantee: discard or revise removes only the proposal and retains exact draft and baseline")
    void discardsOnlyProposalAndRetainsRepairDraft() throws Exception {
        JsonNode proposalSnapshot = createRepairProposal(false);
        JsonNode acceptedBefore = proposalSnapshot.path("workspace").path("acceptedBaseline").deepCopy();
        JsonNode draftBefore = proposalSnapshot.path("workspace").path("repairDraft").deepCopy();

        HttpResponse<String> discardedResponse = command("DELETE", "/api/proposal", session(), null);
        assertEquals(200, discardedResponse.statusCode(), discardedResponse.body());
        JsonNode discarded = body(discardedResponse);
        assertEquals("REPAIR_DRAFT", discarded.path("state").stringValue());
        assertEquals(acceptedBefore, discarded.path("workspace").path("acceptedBaseline"));
        assertEquals(draftBefore, discarded.path("workspace").path("repairDraft"));
        assertFalse(discarded.path("workspace").has("proposal"));
    }

    @Test
    @DisplayName("UC-6 extension 6a/RULE-17: every proposal identity, result, and review mismatch invalidates eligibility without accepted mutation")
    void invalidatesEveryStaleRepairProposalIdentity() throws Exception {
        JsonNode arranged = createRepairProposal(false);
        ObjectNode pristine = (ObjectNode) arranged.path("workspace").deepCopy();
        JsonNode acceptedBefore = pristine.path("acceptedBaseline").deepCopy();
        long proposalVersion = arranged.path("version").longValue();
        List<java.util.function.Consumer<ObjectNode>> corruptions = List.of(
                document -> ((ObjectNode) document.path("proposal")).put("sourceWorkspaceVersion", -1),
                document -> ((ObjectNode) document.path("proposal")).put("acceptedTimetableRevision", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("successorDefinitionRevision", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("intentRevision", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("proposedTimetableRevision", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("runId", UUID.randomUUID().toString()),
                document -> ((ObjectNode) document.path("proposal")).put("limit", "PT2M"),
                document -> ((ObjectNode) document.path("proposal")).put("terminationReason", "wrong"),
                document -> ((ObjectNode) document.path("proposal")).put("elapsedTimeMs", -1),
                document -> ((ObjectNode) document.path("proposal").path("changeCounts")).put("periodMoves", -1),
                document -> ((ObjectNode) document.path("proposal").path("result")).put("schoolId", "wrong"),
                document -> ((ObjectNode) document.path("proposal").path("review")).put("uniqueChangedLessonCount", -1));

        for (java.util.function.Consumer<ObjectNode> corruption : corruptions) {
            ObjectNode corrupted = pristine.deepCopy();
            corruption.accept(corrupted);
            jdbc.sql("""
                    UPDATE workspace_aggregate SET lifecycle_state='REPAIR_PROPOSAL', version=:version,
                    active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                    """).param("version", proposalVersion).param("document", JSON.writeValueAsString(corrupted)).update();

            HttpResponse<String> response = command("POST", "/api/proposal/accept", session(), null);
            assertEquals(409, response.statusCode(), response.body());
            assertEquals("STALE_PROPOSAL", body(response).path("code").stringValue());
            assertEquals("REPAIR_DRAFT", lifecycle());
            assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
            assertFalse(storedDocument().has("proposal"));
        }
    }

    @Test
    @DisplayName("UC-6 extension 6b/RULE-7: failed durable acceptance keeps byte-exact baseline and unchanged proposal for retry")
    void rollsBackFailedAcceptanceAndKeepsProposalReviewable() throws Exception {
        JsonNode proposalSnapshot = createRepairProposal(false);
        JsonNode before = proposalSnapshot.path("workspace").deepCopy();
        long versionBefore = proposalSnapshot.path("version").longValue();
        jdbc.sql("""
                CREATE OR REPLACE FUNCTION fail_repair_accept() RETURNS trigger AS $$
                BEGIN
                  IF NEW.lifecycle_state = 'ACCEPTED_BASELINE' THEN RAISE EXCEPTION 'injected acceptance failure'; END IF;
                  RETURN NEW;
                END; $$ LANGUAGE plpgsql
                """).update();
        jdbc.sql("""
                CREATE TRIGGER fail_repair_accept_trigger BEFORE UPDATE ON workspace_aggregate
                FOR EACH ROW EXECUTE FUNCTION fail_repair_accept()
                """).update();
        try {
            HttpResponse<String> failed = command("POST", "/api/proposal/accept", session(), null);
            assertEquals(503, failed.statusCode(), failed.body());
            assertEquals("STORAGE_UNAVAILABLE", body(failed).path("code").stringValue());
            assertEquals("REPAIR_PROPOSAL", lifecycle());
            assertEquals(versionBefore, jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1").query(Long.class).single());
            assertEquals(before, storedDocument());
        } finally {
            jdbc.sql("DROP TRIGGER IF EXISTS fail_repair_accept_trigger ON workspace_aggregate").update();
            jdbc.sql("DROP FUNCTION IF EXISTS fail_repair_accept()").update();
        }
        assertEquals(200, command("POST", "/api/proposal/accept", session(), null).statusCode());
        assertEquals("ACCEPTED_BASELINE", lifecycle());
    }

    @Test
    @DisplayName("UC-5 extensions 4b/4c and G7: unsuccessful evidence offers only an unchanged two-minute retry and never exposes a proposal")
    void gatesTwoMinuteRetryByUnchangedUnsuccessfulIntent() throws Exception {
        establishAcceptedBaseline();
        command("POST", "/api/repair-draft", session(),
                "{\"resourceType\":\"TEACHER\",\"resourceId\":\"teacher-alex\",\"periodIds\":[\"mon-1\",\"mon-2\",\"mon-3\"]}");
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();
        JsonNode intentBefore = storedDocument().path("repairDraft").path("intent").deepCopy();

        processes.failure = RepairFailure.NO_FEASIBLE;
        HttpResponse<String> unsuccessful = command("POST", "/api/runs", session(), "{\"limit\":\"PT30S\"}");
        assertEquals(202, unsuccessful.statusCode(), unsuccessful.body());
        awaitState("REPAIR_DRAFT", Duration.ofSeconds(5));
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
        processes.failure = null;
        processes.blockReplan = true;
        HttpResponse<String> retry = command("POST", "/api/runs", session(), "{\"limit\":\"PT2M\"}");
        assertEquals(202, retry.statusCode(), retry.body());
        assertEquals("PT2M", body(retry).path("workspace").path("run").path("limit").stringValue());
        processes.awaitBlocked();
        String retryRunId = body(retry).path("workspace").path("run").path("id").stringValue();
        command("DELETE", "/api/runs/" + retryRunId, session(), "");
        assertEquals("PT2M", storedDocument().path("lastRun").path("limit").stringValue());
        assertFalse(storedDocument().has("proposal"));
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
    }

    @Test
    @DisplayName("UC-5 extensions 4a/4d/5a and G2/G3: every failed or rejected repair output returns through HTTP to the exact draft with no candidate")
    void failedAndRejectedRepairOutputsNeverBecomeProposals() throws Exception {
        establishAcceptedBaseline();
        command("POST", "/api/repair-draft", session(),
                "{\"resourceType\":\"TEACHER\",\"resourceId\":\"teacher-alex\",\"periodIds\":[\"mon-1\"]}");
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();
        JsonNode draftBefore = storedDocument().path("repairDraft").deepCopy();

        for (FailureExpectation expectation : List.of(
                new FailureExpectation(RepairFailure.INVALID_INPUT, "INVALID_INPUT"),
                new FailureExpectation(RepairFailure.INTERNAL_ERROR, "INTERNAL_ERROR"),
                new FailureExpectation(RepairFailure.TRANSPORT, "TRANSPORT_FAILURE"),
                new FailureExpectation(RepairFailure.INTERRUPTED, "INTERRUPTED"),
                new FailureExpectation(RepairFailure.MISMATCHED, "REJECTED_OUTPUT"))) {
            processes.failure = expectation.failure();
            HttpResponse<String> started = command("POST", "/api/runs", session(), "{\"limit\":\"PT30S\"}");
            assertEquals(202, started.statusCode(), started.body());
            JsonNode snapshot = awaitState("REPAIR_DRAFT", Duration.ofSeconds(5));
            assertEquals(expectation.code(), snapshot.path("workspace").path("lastRun").path("code").stringValue());
            assertEquals(draftBefore, snapshot.path("workspace").path("repairDraft"));
            assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
            assertFalse(snapshot.path("workspace").has("proposal"));
            assertFalse(snapshot.path("workspace").path("lastRun").has("result"));
            assertFalse(snapshot.path("workspace").path("lastRun").has("timetable"));
            assertFalse(snapshot.toString().contains("secret raw"));
            if (expectation.failure() == RepairFailure.INVALID_INPUT) {
                assertEquals("INVALID_REFERENCE", snapshot.path("workspace").path("lastRun")
                        .path("validationReport").path("errors").get(0).path("code").stringValue());
            }
        }
        assertTrue(storedDocument().path("lastRun").path("message").isTextual());
    }

    @Test
    @DisplayName("UC-5 RULE-11/12/23: active conflict, forced cancellation, watchdog, and stale completion are bounded and cannot publish")
    void boundsAndSuppressesConcurrentLateRepairRuns() throws Exception {
        establishAcceptedBaseline();
        command("POST", "/api/repair-draft", session(),
                "{\"resourceType\":\"ROOM\",\"resourceId\":\"room-101\",\"periodIds\":[\"mon-2\"]}");
        JsonNode acceptedBefore = storedDocument().path("acceptedBaseline").deepCopy();
        JsonNode draftBefore = storedDocument().path("repairDraft").deepCopy();

        processes.blockReplan = true;
        HttpResponse<String> started = command("POST", "/api/runs", session(), "{\"limit\":\"PT30S\"}");
        processes.awaitBlocked();
        int processCount = processes.commands().size();
        HttpResponse<String> conflict = command("POST", "/api/runs", session(), "{\"limit\":\"PT30S\"}");
        assertEquals(409, conflict.statusCode());
        assertEquals(processCount, processes.commands().size());
        String runId = body(started).path("workspace").path("run").path("id").stringValue();
        command("DELETE", "/api/runs/" + runId, session(), "");
        assertTrue(processes.lastBlocking.forceCalled);
        assertEquals(draftBefore, storedDocument().path("repairDraft"));
        assertEquals(acceptedBefore, storedDocument().path("acceptedBaseline"));
        assertFalse(storedDocument().has("proposal"));

        processes.reset();
        processes.failure = RepairFailure.WATCHDOG;
        command("POST", "/api/runs", session(), "{\"limit\":\"PT30S\"}");
        JsonNode timedOut = awaitState("REPAIR_DRAFT", Duration.ofSeconds(5));
        assertEquals("KERNEL_TIMEOUT", timedOut.path("workspace").path("lastRun").path("code").stringValue());
        assertTrue(processes.lastBlocking.forceCalled);
        assertFalse(timedOut.path("workspace").has("proposal"));

        processes.reset();
        processes.blockReplan = true;
        command("POST", "/api/runs", session(), "{\"limit\":\"PT30S\"}");
        processes.awaitBlocked();
        ObjectNode changed = (ObjectNode) storedDocument();
        changed.remove("run");
        jdbc.sql("""
                UPDATE workspace_aggregate SET lifecycle_state='REPAIR_DRAFT', version=version+1,
                active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                """).param("document", JSON.writeValueAsString(changed)).update();
        JsonNode beforeLate = storedDocument();
        processes.lastBlocking.release(4);
        Thread.sleep(150);
        assertEquals(beforeLate, storedDocument());
        assertFalse(storedDocument().has("proposal"));
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

    private JsonNode createRepairProposal(boolean addRoomPin) throws Exception {
        ObjectNode cached = addRoomPin ? cachedProposalWithPin : cachedProposalWithoutPin;
        if (cached != null) {
            ObjectNode snapshot = cached.deepCopy();
            jdbc.sql("""
                    UPDATE workspace_aggregate SET lifecycle_state='REPAIR_PROPOSAL', version=:version,
                    active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                    """).param("version", snapshot.path("version").longValue())
                    .param("document", JSON.writeValueAsString(snapshot.path("workspace"))).update();
            return snapshot;
        }
        establishAcceptedBaseline();
        command("POST", "/api/repair-draft", session(),
                "{\"resourceType\":\"TEACHER\",\"resourceId\":\"teacher-alex\",\"periodIds\":[\"mon-1\"]}");
        if (addRoomPin) {
            HttpResponse<String> pinned = command("PATCH", "/api/repair-draft", session(),
                    "{\"action\":\"PIN\",\"lessonId\":\"lesson-science-1\",\"dimensions\":[\"ROOM\"]}");
            assertEquals(200, pinned.statusCode(), pinned.body());
        }
        HttpResponse<String> started = command("POST", "/api/runs", session(), "{\"limit\":\"PT30S\"}");
        assertEquals(202, started.statusCode(), started.body());
        ObjectNode snapshot = (ObjectNode) awaitState("REPAIR_PROPOSAL", Duration.ofSeconds(45));
        if (addRoomPin) cachedProposalWithPin = snapshot.deepCopy();
        else cachedProposalWithoutPin = snapshot.deepCopy();
        return snapshot;
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
    private record FailureExpectation(RepairFailure failure, String code) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class ProcessConfiguration {
        @Bean
        @Primary
        SwitchingRepairProcessLauncher switchingRepairProcessLauncher() {
            return new SwitchingRepairProcessLauncher();
        }
    }

    private enum RepairFailure {
        NO_FEASIBLE, INVALID_INPUT, INTERNAL_ERROR, TRANSPORT, INTERRUPTED, MISMATCHED, WATCHDOG
    }

    static final class SwitchingRepairProcessLauncher extends KernelProcessLauncher {
        private final CopyOnWriteArrayList<List<String>> commands = new CopyOnWriteArrayList<>();
        volatile RepairFailure failure;
        volatile boolean blockReplan;
        volatile BlockingRepairProcess lastBlocking;

        @Override
        public Process start(List<String> arguments) throws java.io.IOException {
            commands.add(List.copyOf(arguments));
            if (arguments.size() < 2 || !"replan".equals(arguments.get(1))) return super.start(arguments);
            if (failure == RepairFailure.TRANSPORT) throw new java.io.IOException("secret raw repair transport detail");
            if (failure == RepairFailure.INTERRUPTED) return new InterruptedRepairProcess();
            if (failure == RepairFailure.WATCHDOG) {
                lastBlocking = new BlockingRepairProcess(true);
                return lastBlocking;
            }
            if (blockReplan) {
                lastBlocking = new BlockingRepairProcess(false);
                return lastBlocking;
            }
            if (failure != null) {
                Path output = Path.of(arguments.get(arguments.indexOf("--output") + 1));
                ObjectNode result = JSON.createObjectNode();
                switch (failure) {
                    case NO_FEASIBLE -> {
                        result.put("status", "NO_FEASIBLE_SOLUTION_FOUND");
                        result.put("elapsedTimeMs", 17);
                        result.put("terminationReason", "TIME_LIMIT");
                        result.putObject("searchDiagnostics").put("totalMatches", 2).put("truncated", false)
                                .putArray("constraints").addObject().put("constraintId", "hard.teacher-period")
                                .put("matchCount", 2).putArray("examples").addArray().add("lesson-math-1");
                    }
                    case INVALID_INPUT -> {
                        result.put("status", "INVALID_INPUT");
                        result.put("elapsedTimeMs", 5);
                        result.putObject("validationReport").putArray("errors").addObject()
                                .put("code", "INVALID_REFERENCE").put("message", "A repair reference is invalid.")
                                .putArray("entityIds").add("lesson-math-1");
                    }
                    case INTERNAL_ERROR -> result.put("status", "INTERNAL_ERROR").put("elapsedTimeMs", 3);
                    case MISMATCHED -> {
                        result.put("schemaVersion", 1).put("status", "FEASIBLE")
                                .put("correlationId", "wrong").put("schoolId", "wrong").put("seed", 0);
                        result.putObject("limit").put("type", "TIME").put("duration", "PT30S");
                        result.put("terminationReason", "TIME_LIMIT").putObject("timetable").putArray("assignments");
                    }
                    default -> throw new IllegalStateException("Unsupported configured failure " + failure);
                }
                Files.write(output, JSON.writeValueAsBytes(result));
                int exit = failure == RepairFailure.NO_FEASIBLE ? 3
                        : failure == RepairFailure.INVALID_INPUT ? 2
                        : failure == RepairFailure.MISMATCHED ? 0 : 4;
                return new CompletedRepairProcess(exit);
            }
            return super.start(arguments);
        }

        void awaitBlocked() throws Exception {
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (lastBlocking == null && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(lastBlocking != null, "repair process did not block");
        }

        List<List<String>> commands() { return List.copyOf(commands); }

        void reset() {
            if (lastBlocking != null) lastBlocking.release(137);
            failure = null;
            blockReplan = false;
            lastBlocking = null;
            commands.clear();
        }
    }

    static class CompletedRepairProcess extends Process {
        private final int exit;

        CompletedRepairProcess(int exit) { this.exit = exit; }
        @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        @Override public InputStream getInputStream() { return InputStream.nullInputStream(); }
        @Override public InputStream getErrorStream() {
            return new java.io.ByteArrayInputStream("secret raw stderr path assignment".getBytes(StandardCharsets.UTF_8));
        }
        @Override public int waitFor() { return exit; }
        @Override public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException { return true; }
        @Override public int exitValue() { return exit; }
        @Override public void destroy() {}
        @Override public Process destroyForcibly() { return this; }
        @Override public boolean isAlive() { return false; }
    }

    static final class InterruptedRepairProcess extends CompletedRepairProcess {
        InterruptedRepairProcess() { super(137); }
        @Override public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException("injected repair interruption");
        }
    }

    static final class BlockingRepairProcess extends Process {
        private final CountDownLatch completed = new CountDownLatch(1);
        private final boolean immediateTimeout;
        private volatile int exit = 137;
        volatile boolean forceCalled;

        BlockingRepairProcess(boolean immediateTimeout) { this.immediateTimeout = immediateTimeout; }
        void release(int value) { exit = value; completed.countDown(); }
        @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        @Override public InputStream getInputStream() { return InputStream.nullInputStream(); }
        @Override public InputStream getErrorStream() { return InputStream.nullInputStream(); }
        @Override public int waitFor() throws InterruptedException { completed.await(); return exit; }
        @Override public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            return !immediateTimeout && completed.await(timeout, unit);
        }
        @Override public int exitValue() {
            if (completed.getCount() > 0) throw new IllegalThreadStateException();
            return exit;
        }
        @Override public void destroy() {}
        @Override public Process destroyForcibly() { forceCalled = true; release(137); return this; }
        @Override public boolean isAlive() { return completed.getCount() > 0; }
    }
}
