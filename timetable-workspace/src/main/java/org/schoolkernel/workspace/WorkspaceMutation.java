package org.schoolkernel.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;

import java.util.UUID;

@Component
public class WorkspaceMutation {
    private final WorkspaceRepository repository;

    public WorkspaceMutation(WorkspaceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WorkspaceAggregate importVerified(
            long expectedVersion,
            WorkspaceState nextState,
            JsonNode document) {
        if (repository.replace(expectedVersion, WorkspaceState.EMPTY, nextState, document).isEmpty()) {
            WorkspaceAggregate current = repository.load();
            if (current.version() != expectedVersion) {
                throw new WorkspaceProblem(
                        HttpStatus.PRECONDITION_FAILED,
                        "STALE_WORKSPACE_VERSION",
                        "The workspace changed. Reload it before importing.");
            }
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "INVALID_WORKSPACE_TRANSITION",
                    "School data can be imported only into an empty workspace.");
        }
        return repository.load();
    }

    @Transactional
    public WorkspaceAggregate startInitialRun(long expectedVersion, UUID runId, JsonNode document) {
        if (repository.startRun(expectedVersion, runId, document).isEmpty()) {
            throw transitionProblem(expectedVersion, "Initial planning can start only from an initial draft.");
        }
        return repository.load();
    }

    @Transactional
    public WorkspaceAggregate finishInitialRun(
            long expectedVersion,
            UUID runId,
            WorkspaceState nextState,
            JsonNode document) {
        if (repository.finishRun(expectedVersion, runId, nextState, document).isEmpty()) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "STALE_RUN",
                    "This planning run is no longer active.");
        }
        return repository.load();
    }

    @Transactional
    public WorkspaceAggregate startRepairRun(long expectedVersion, UUID runId, JsonNode document) {
        if (repository.startRepairRun(expectedVersion, runId, document).isEmpty()) {
            throw transitionProblem(expectedVersion, "Repair generation can start only from a ready repair draft.");
        }
        return repository.load();
    }

    @Transactional
    public WorkspaceAggregate finishRepairRun(
            long expectedVersion,
            UUID runId,
            WorkspaceState nextState,
            JsonNode document) {
        if (repository.finishRepairRun(expectedVersion, runId, nextState, document).isEmpty()) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "STALE_RUN",
                    "This repair run is no longer active.");
        }
        return repository.load();
    }

    @Transactional
    public WorkspaceAggregate replaceInitial(
            long expectedVersion,
            WorkspaceState expectedState,
            JsonNode document) {
        if (repository.replaceInitialDraft(expectedVersion, expectedState, document).isEmpty()) {
            throw transitionProblem(expectedVersion, "The initial definition cannot be replaced in the current state.");
        }
        return repository.load();
    }

    @Transactional
    public WorkspaceAggregate acceptInitial(long expectedVersion, JsonNode document) {
        if (repository.acceptInitialProposal(expectedVersion, document).isEmpty()) {
            throw transitionProblem(expectedVersion, "This proposal is no longer eligible for acceptance.");
        }
        return repository.load();
    }

    @Transactional
    public WorkspaceAggregate acceptRepair(long expectedVersion, JsonNode document) {
        if (repository.acceptRepairProposal(expectedVersion, document).isEmpty()) {
            throw transitionProblem(expectedVersion, "This repair proposal is no longer eligible for acceptance.");
        }
        return repository.load();
    }

    @Transactional
    public WorkspaceAggregate replaceRepair(
            long expectedVersion,
            WorkspaceState expectedState,
            WorkspaceState nextState,
            JsonNode document) {
        if (repository.replace(expectedVersion, expectedState, nextState, document).isEmpty()) {
            throw transitionProblem(expectedVersion, "The repair draft changed. Reload it before continuing.");
        }
        return repository.load();
    }

    @Transactional
    public long replaceRepairDraft(long expectedVersion, JsonNode repairDraft) {
        return repository.replaceRepairDraft(expectedVersion, repairDraft)
                .orElseThrow(() -> transitionProblem(
                        expectedVersion, "The repair draft changed. Reload it before continuing."));
    }

    private WorkspaceProblem transitionProblem(long expectedVersion, String message) {
        WorkspaceAggregate current = repository.load();
        if (current.version() != expectedVersion) {
            return new WorkspaceProblem(
                    HttpStatus.PRECONDITION_FAILED,
                    "STALE_WORKSPACE_VERSION",
                    "The workspace changed. Reload it before continuing.");
        }
        return new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION", message);
    }
}
