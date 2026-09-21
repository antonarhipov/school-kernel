package org.schoolkernel.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;

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
}
