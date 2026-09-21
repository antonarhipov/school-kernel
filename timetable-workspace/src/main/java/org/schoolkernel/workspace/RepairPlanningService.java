package org.schoolkernel.workspace;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class RepairPlanningService {
    private static final String NORMAL_LIMIT = "PT30S";
    private static final String RETRY_LIMIT = "PT2M";
    private static final Set<String> CHANGE_CATEGORIES = Set.of(
            "additions", "cancellations", "teacherChanges", "forcedMoves", "periodMoves", "roomOnlyMoves");

    private final WorkspaceRepository repository;
    private final WorkspaceMutation mutation;
    private final RepairDraftService drafts;
    private final KernelPlanner planner;
    private final ProposalReviewService reviews;
    private final ObjectMapper json;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("repair-planning-", 0).factory());

    public RepairPlanningService(
            WorkspaceRepository repository,
            WorkspaceMutation mutation,
            RepairDraftService drafts,
            KernelPlanner planner,
            ProposalReviewService reviews,
            ObjectMapper json) {
        this.repository = repository;
        this.mutation = mutation;
        this.drafts = drafts;
        this.planner = planner;
        this.reviews = reviews;
        this.json = json;
    }

    public WorkspaceAggregate start(String ifMatch, JsonNode request) {
        WorkspaceAggregate current = repository.load();
        long sourceVersion = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.REPAIR_DRAFT) {
            throw transition("Repair generation can start only from a repair draft.");
        }
        if (drafts.hasUnsavedBrowserState(sourceVersion)) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "DRAFT_NOT_DURABLE",
                    "Repair generation is unavailable after an auto-save failure. Reapply or discard the unsaved change first.");
        }
        JsonNode draft = current.document().path("repairDraft");
        if (!draft.path("readyToSolve").booleanValue() || !draft.path("conflicts").isEmpty()) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "DRAFT_CONFLICT",
                    "Resolve every blocking repair-draft conflict before generating a proposal.");
        }
        String limit = requestedLimit(request);
        String intentRevision = draft.path("intentRevision").stringValue();
        if (RETRY_LIMIT.equals(limit) && !eligibleForRetry(current.document(), intentRevision)) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "RETRY_NOT_AVAILABLE",
                    "The two-minute retry is available only after an unchanged unsuccessful repair run.");
        }

        ObjectNode successorDefinition = drafts.compiledDefinition(current.document());
        JsonNode accepted = current.document().path("acceptedBaseline");
        JsonNode currentDefinition = accepted.path("definition").deepCopy();
        JsonNode currentResult = accepted.path("result").deepCopy();
        String acceptedTimetableRevision = currentResult.path("timetableRevision").stringValue();
        String acceptedInputRevision = currentResult.path("inputRevision").stringValue();
        if (!acceptedInputRevision.equals(successorDefinition.path("basedOnRevision").stringValue())) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "STALE_WORKSPACE_STATE",
                    "The repair draft no longer matches the accepted baseline.");
        }

        UUID runId = UUID.randomUUID();
        ObjectNode document = (ObjectNode) current.document().deepCopy();
        document.remove("proposal");
        ObjectNode run = document.putObject("run");
        run.put("id", runId.toString());
        run.put("kind", "REPAIR");
        run.put("status", "RUNNING");
        run.put("sourceWorkspaceVersion", sourceVersion);
        run.put("limit", limit);
        run.put("acceptedTimetableRevision", acceptedTimetableRevision);
        run.put("acceptedInputRevision", acceptedInputRevision);
        run.put("intentRevision", intentRevision);
        WorkspaceAggregate started = mutation.startRepairRun(sourceVersion, runId, document);
        executor.submit(() -> finish(
                runId,
                started.version(),
                sourceVersion,
                limit,
                acceptedTimetableRevision,
                intentRevision,
                currentDefinition,
                currentResult,
                successorDefinition));
        return started;
    }

    public JsonNode run(UUID runId) {
        WorkspaceAggregate current = repository.load();
        JsonNode active = current.document().path("run");
        if (runId.toString().equals(text(active.path("id"))) && "REPAIR".equals(text(active.path("kind")))) {
            return active;
        }
        JsonNode last = current.document().path("lastRun");
        if (runId.toString().equals(text(last.path("id"))) && "REPAIR".equals(text(last.path("kind")))) {
            return last;
        }
        JsonNode proposal = current.document().path("proposal");
        if (runId.toString().equals(text(proposal.path("runId"))) && "REPAIR".equals(text(proposal.path("kind")))) {
            ObjectNode response = json.createObjectNode();
            response.put("id", runId.toString());
            response.put("kind", "REPAIR");
            response.put("status", "FEASIBLE");
            response.put("limit", proposal.path("limit").stringValue());
            response.put("terminationReason", proposal.path("terminationReason").stringValue());
            return response;
        }
        throw new WorkspaceProblem(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "The repair run does not exist.");
    }

    public WorkspaceAggregate cancel(String ifMatch, UUID runId) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.SOLVING_REPAIR || !runId.equals(current.activeRunId())) {
            throw transition("Only the active repair run can be cancelled.");
        }
        planner.cancel(runId);
        ObjectNode document = (ObjectNode) current.document().deepCopy();
        String limit = document.path("run").path("limit").stringValue();
        String intentRevision = document.path("run").path("intentRevision").stringValue();
        document.remove("run");
        document.remove("proposal");
        ObjectNode lastRun = document.putObject("lastRun");
        lastRun.put("id", runId.toString());
        lastRun.put("kind", "REPAIR");
        lastRun.put("status", "CANCELLED");
        lastRun.put("limit", limit);
        lastRun.put("intentRevision", intentRevision);
        lastRun.put("message", "Repair generation was cancelled.");
        return mutation.finishRepairRun(expectedVersion, runId, WorkspaceState.REPAIR_DRAFT, document);
    }

    private void finish(
            UUID runId,
            long runningVersion,
            long sourceVersion,
            String limit,
            String acceptedTimetableRevision,
            String intentRevision,
            JsonNode currentDefinition,
            JsonNode currentResult,
            JsonNode successorDefinition) {
        KernelPlanner.Outcome outcome = planner.replan(
                runId, currentDefinition, currentResult, successorDefinition, limit);
        boolean interrupted = Thread.interrupted();
        try {
            persistOutcome(runId, runningVersion, sourceVersion, limit, acceptedTimetableRevision,
                    intentRevision, successorDefinition, outcome);
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private void persistOutcome(
            UUID runId,
            long runningVersion,
            long sourceVersion,
            String limit,
            String acceptedTimetableRevision,
            String intentRevision,
            JsonNode successorDefinition,
            KernelPlanner.Outcome outcome) {
        WorkspaceAggregate current;
        try {
            current = repository.load();
        } catch (RuntimeException unavailable) {
            return;
        }
        JsonNode activeRun = current.document().path("run");
        if (current.version() != runningVersion
                || current.state() != WorkspaceState.SOLVING_REPAIR
                || !runId.equals(current.activeRunId())
                || sourceVersion != activeRun.path("sourceWorkspaceVersion").longValue()
                || !acceptedTimetableRevision.equals(text(activeRun.path("acceptedTimetableRevision")))
                || !intentRevision.equals(text(activeRun.path("intentRevision")))) {
            return;
        }

        ObjectNode document = (ObjectNode) current.document().deepCopy();
        document.remove("run");
        ObjectNode lastRun = document.putObject("lastRun");
        lastRun.put("id", runId.toString());
        lastRun.put("kind", "REPAIR");
        lastRun.put("limit", limit);
        lastRun.put("intentRevision", intentRevision);
        lastRun.put("status", outcome.kind().name());
        if (outcome.kind() == KernelPlanner.Kind.FEASIBLE) {
            JsonNode result = outcome.result();
            JsonNode changeReport = result.path("changeReport");
            lastRun.put("terminationReason", result.path("terminationReason").stringValue());
            lastRun.put("elapsedTimeMs", result.path("elapsedTimeMs").longValue());
            lastRun.put("feasible", true);
            lastRun.set("changeCounts", changeCounts(changeReport));

            ObjectNode proposal = document.putObject("proposal");
            proposal.put("kind", "REPAIR");
            proposal.put("sourceWorkspaceVersion", sourceVersion);
            proposal.put("acceptedTimetableRevision", acceptedTimetableRevision);
            proposal.put("successorDefinitionRevision", result.path("inputRevision").stringValue());
            proposal.put("intentRevision", intentRevision);
            proposal.set("definition", successorDefinition.deepCopy());
            proposal.set("result", result.deepCopy());
            proposal.put("proposedTimetableRevision", result.path("timetableRevision").stringValue());
            proposal.put("runId", runId.toString());
            proposal.put("limit", limit);
            proposal.put("terminationReason", result.path("terminationReason").stringValue());
            proposal.put("elapsedTimeMs", result.path("elapsedTimeMs").longValue());
            proposal.set("changeCounts", changeCounts(changeReport));
            proposal.set("review", reviews.create(
                    current.document().path("acceptedBaseline"),
                    current.document().path("repairDraft"),
                    successorDefinition,
                    result));
            try {
                mutation.finishRepairRun(runningVersion, runId, WorkspaceState.REPAIR_PROPOSAL, document);
            } catch (WorkspaceProblem ignored) {
                // Cancellation or another conditional transition won; the late result is ineligible.
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
            mutation.finishRepairRun(runningVersion, runId, WorkspaceState.REPAIR_DRAFT, document);
        } catch (WorkspaceProblem ignored) {
            // Cancellation or another conditional transition won; the late result is ineligible.
        }
    }

    private ObjectNode changeCounts(JsonNode report) {
        ObjectNode counts = json.createObjectNode();
        CHANGE_CATEGORIES.stream().sorted().forEach(category -> counts.put(category, report.path(category).size()));
        return counts;
    }

    private static boolean eligibleForRetry(JsonNode document, String intentRevision) {
        JsonNode last = document.path("lastRun");
        return "REPAIR".equals(text(last.path("kind")))
                && "NO_FEASIBLE_SOLUTION_FOUND".equals(text(last.path("code")))
                && intentRevision.equals(text(last.path("intentRevision")));
    }

    private static String requestedLimit(JsonNode request) {
        if (request == null || request.isNull() || request.isMissingNode() || request.isEmpty()) return NORMAL_LIMIT;
        String limit = text(request.path("limit"));
        if (NORMAL_LIMIT.equals(limit) || RETRY_LIMIT.equals(limit)) return limit;
        throw new WorkspaceProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_RUN_LIMIT",
                "Choose the 30-second repair run or the available two-minute retry.");
    }

    private static WorkspaceProblem transition(String message) {
        return new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION", message);
    }

    private static String text(JsonNode node) {
        return node.isTextual() ? node.stringValue() : null;
    }

    @PreDestroy
    void close() {
        executor.shutdownNow();
    }
}
