package org.schoolkernel.workspace;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class InitialPlanningService {
    private static final String LIMIT = "PT1M";

    private final WorkspaceRepository repository;
    private final WorkspaceMutation mutation;
    private final KernelPlanner planner;
    private final KernelVerifier verifier;
    private final ManifestService manifests;
    private final ObjectMapper json;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("initial-planning-", 0).factory());

    public InitialPlanningService(
            WorkspaceRepository repository,
            WorkspaceMutation mutation,
            KernelPlanner planner,
            KernelVerifier verifier,
            ManifestService manifests,
            ObjectMapper json) {
        this.repository = repository;
        this.mutation = mutation;
        this.planner = planner;
        this.verifier = verifier;
        this.manifests = manifests;
        this.json = json;
    }

    public WorkspaceAggregate start(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.INITIAL_DRAFT) {
            throw transition("Initial planning can start only from an initial draft.");
        }
        UUID runId = UUID.randomUUID();
        ObjectNode document = (ObjectNode) current.document().deepCopy();
        ObjectNode run = document.putObject("run");
        run.put("id", runId.toString());
        run.put("kind", "INITIAL");
        run.put("status", "RUNNING");
        run.put("sourceWorkspaceVersion", expectedVersion);
        run.put("limit", LIMIT);
        document.remove("proposal");
        WorkspaceAggregate started = mutation.startInitialRun(expectedVersion, runId, document);
        JsonNode definition = document.path("initialDefinition").deepCopy();
        executor.submit(() -> finish(runId, started.version(), expectedVersion, definition));
        return started;
    }

    public JsonNode run(UUID runId) {
        WorkspaceAggregate current = repository.load();
        JsonNode active = current.document().path("run");
        if (runId.toString().equals(text(active.path("id")))) return active;
        JsonNode last = current.document().path("lastRun");
        if (runId.toString().equals(text(last.path("id")))) return last;
        JsonNode proposalRun = current.document().path("proposal");
        if (runId.toString().equals(text(proposalRun.path("runId")))) {
            ObjectNode response = json.createObjectNode();
            response.put("id", runId.toString());
            response.put("kind", "INITIAL");
            response.put("status", "FEASIBLE");
            response.put("limit", proposalRun.path("limit").stringValue());
            response.put("terminationReason", proposalRun.path("terminationReason").stringValue());
            return response;
        }
        throw new WorkspaceProblem(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "The planning run does not exist.");
    }

    public WorkspaceAggregate cancel(String ifMatch, UUID runId) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.SOLVING_INITIAL
                || !runId.equals(current.activeRunId())) {
            throw transition("Only the active initial planning run can be cancelled.");
        }
        planner.cancel(runId);
        ObjectNode document = (ObjectNode) current.document().deepCopy();
        document.remove("run");
        document.remove("proposal");
        ObjectNode lastRun = document.putObject("lastRun");
        lastRun.put("id", runId.toString());
        lastRun.put("kind", "INITIAL");
        lastRun.put("status", "CANCELLED");
        lastRun.put("limit", LIMIT);
        lastRun.put("message", "Planning was cancelled.");
        return mutation.finishInitialRun(
                expectedVersion, runId, WorkspaceState.INITIAL_DRAFT, document);
    }

    public WorkspaceAggregate discard(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.INITIAL_PROPOSAL) {
            throw transition("Only an initial proposal can be discarded.");
        }
        ObjectNode document = (ObjectNode) current.document().deepCopy();
        document.remove("proposal");
        return mutation.replaceInitial(expectedVersion, WorkspaceState.INITIAL_PROPOSAL, document);
    }

    @Transactional(noRollbackFor = WorkspaceProblem.class)
    public WorkspaceAggregate accept(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.INITIAL_PROPOSAL) {
            throw transition("Only an initial proposal can be accepted.");
        }
        JsonNode definition = current.document().path("initialDefinition");
        JsonNode proposal = current.document().path("proposal");
        JsonNode result = proposal.path("result");
        if (!proposalIdentityMatches(current, proposal, result)) {
            invalidate(current);
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "STALE_PROPOSAL",
                    "The initial proposal no longer matches the current draft.");
        }
        KernelVerifier.Verification verified;
        try {
            verified = verifier.verify(new ImportDocuments(
                    definition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        } catch (WorkspaceProblem problem) {
            if ("KERNEL_VERIFICATION_FAILED".equals(problem.code())) {
                invalidate(current);
                throw staleProposal();
            }
            throw problem;
        }
        if (!verified.definitionRevision().equals(proposal.path("successorDefinitionRevision").stringValue())
                || !verified.timetableRevision().equals(proposal.path("proposedTimetableRevision").stringValue())) {
            invalidate(current);
            throw staleProposal();
        }
        ObjectNode acceptedDocument = json.createObjectNode();
        acceptedDocument.set("school", current.document().path("school").deepCopy());
        acceptedDocument.put("importMode", "INITIAL_ACCEPTANCE");
        acceptedDocument.put("definitionRevision", verified.definitionRevision());
        acceptedDocument.put("timetableRevision", verified.timetableRevision());
        ObjectNode accepted = acceptedDocument.putObject("acceptedBaseline");
        accepted.set("definition", definition.deepCopy());
        accepted.set("result", result.deepCopy());
        accepted.set("manifest", manifests.validatedOrGenerated(new ImportDocuments(
                definition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE)));
        acceptedDocument.set("lastRun", current.document().path("lastRun").deepCopy());
        return mutation.acceptInitial(expectedVersion, acceptedDocument);
    }

    private void finish(UUID runId, long runningVersion, long sourceVersion, JsonNode definition) {
        KernelPlanner.Outcome outcome = planner.plan(runId, definition);
        boolean interrupted = Thread.interrupted();
        try {
            persistOutcome(runId, runningVersion, sourceVersion, definition, outcome);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void persistOutcome(
            UUID runId,
            long runningVersion,
            long sourceVersion,
            JsonNode definition,
            KernelPlanner.Outcome outcome) {
        WorkspaceAggregate current;
        try {
            current = repository.load();
        } catch (RuntimeException unavailable) {
            return;
        }
        if (current.version() != runningVersion
                || current.state() != WorkspaceState.SOLVING_INITIAL
                || !runId.equals(current.activeRunId())) {
            return;
        }
        ObjectNode document = (ObjectNode) current.document().deepCopy();
        document.remove("run");
        ObjectNode lastRun = document.putObject("lastRun");
        lastRun.put("id", runId.toString());
        lastRun.put("kind", "INITIAL");
        lastRun.put("limit", LIMIT);
        lastRun.put("status", outcome.kind().name());
        if (outcome.kind() == KernelPlanner.Kind.FEASIBLE) {
            JsonNode result = outcome.result();
            lastRun.put("terminationReason", result.path("terminationReason").stringValue());
            lastRun.put("elapsedTimeMs", result.path("elapsedTimeMs").longValue());
            lastRun.put("feasible", true);
            lastRun.put("assignmentCount", result.path("timetable").path("assignments").size());
            ObjectNode proposal = document.putObject("proposal");
            proposal.put("kind", "INITIAL");
            proposal.put("sourceWorkspaceVersion", sourceVersion);
            proposal.put("acceptedTimetableRevision", "NO_BASELINE");
            proposal.put("successorDefinitionRevision", result.path("inputRevision").stringValue());
            proposal.put("intentRevision", "NO_INTENT");
            proposal.set("result", result.deepCopy());
            proposal.put("proposedTimetableRevision", result.path("timetableRevision").stringValue());
            proposal.put("runId", runId.toString());
            proposal.put("limit", LIMIT);
            proposal.put("terminationReason", result.path("terminationReason").stringValue());
            proposal.put("elapsedTimeMs", result.path("elapsedTimeMs").longValue());
            ObjectNode counts = proposal.putObject("changeCounts");
            counts.put("lessonCount", result.path("timetable").path("assignments").size());
            try {
                mutation.finishInitialRun(
                        runningVersion, runId, WorkspaceState.INITIAL_PROPOSAL, document);
            } catch (WorkspaceProblem ignored) {
                // A cancel or state change won; late results are discarded.
            }
            return;
        }
        lastRun.put("feasible", false);
        lastRun.put("code", outcome.code());
        lastRun.put("message", outcome.message());
        if (outcome.result() != null) {
            lastRun.put("elapsedTimeMs", outcome.result().path("elapsedTimeMs").longValue());
            if (outcome.result().path("terminationReason").isTextual()) {
                lastRun.put("terminationReason", outcome.result().path("terminationReason").stringValue());
            }
            if (outcome.result().path("validationReport").isObject()) {
                lastRun.set("validationReport", outcome.result().path("validationReport").deepCopy());
            }
            if (outcome.result().path("searchDiagnostics").isObject()) {
                lastRun.set("searchDiagnostics", outcome.result().path("searchDiagnostics").deepCopy());
            }
        }
        document.remove("proposal");
        try {
            mutation.finishInitialRun(
                    runningVersion, runId, WorkspaceState.INITIAL_DRAFT, document);
        } catch (WorkspaceProblem ignored) {
            // A cancel or state change won; late results are discarded.
        }
    }

    private boolean proposalIdentityMatches(WorkspaceAggregate current, JsonNode proposal, JsonNode result) {
        return proposal.isObject()
                && result.isObject()
                && proposal.path("sourceWorkspaceVersion").longValue() == current.version() - 2
                && "NO_BASELINE".equals(text(proposal.path("acceptedTimetableRevision")))
                && "NO_INTENT".equals(text(proposal.path("intentRevision")))
                && text(current.document().path("definitionRevision")) != null
                && text(current.document().path("definitionRevision"))
                        .equals(text(proposal.path("successorDefinitionRevision")))
                && text(proposal.path("successorDefinitionRevision"))
                        .equals(text(result.path("inputRevision")))
                && text(proposal.path("proposedTimetableRevision")) != null
                && text(proposal.path("proposedTimetableRevision"))
                        .equals(text(result.path("timetableRevision")))
                && proposal.path("runId").isTextual()
                && text(proposal.path("runId"))
                        .equals(text(current.document().path("lastRun").path("id")))
                && LIMIT.equals(text(proposal.path("limit")))
                && "TIME".equals(text(result.path("limit").path("type")))
                && LIMIT.equals(text(result.path("limit").path("duration")))
                && text(proposal.path("terminationReason")) != null
                && text(proposal.path("terminationReason"))
                        .equals(text(result.path("terminationReason")))
                && proposal.path("elapsedTimeMs").longValue() == result.path("elapsedTimeMs").longValue()
                && proposal.path("changeCounts").path("lessonCount").intValue()
                        == result.path("timetable").path("assignments").size();
    }

    private void invalidate(WorkspaceAggregate current) {
        ObjectNode document = (ObjectNode) current.document().deepCopy();
        document.remove("proposal");
        mutation.replaceInitial(current.version(), WorkspaceState.INITIAL_PROPOSAL, document);
    }

    private static WorkspaceProblem transition(String message) {
        return new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION", message);
    }

    private static WorkspaceProblem staleProposal() {
        return new WorkspaceProblem(
                HttpStatus.CONFLICT,
                "STALE_PROPOSAL",
                "The initial proposal no longer matches the current draft.");
    }

    private static String text(JsonNode node) {
        return node.isTextual() ? node.stringValue() : null;
    }

    @PreDestroy
    void close() {
        executor.shutdownNow();
    }
}
