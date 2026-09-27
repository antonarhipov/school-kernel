package org.schoolkernel.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class ManualDraftService {
    private final WorkspaceRepository repository;
    private final WorkspaceMutation mutation;
    private final ObjectMapper json;

    public ManualDraftService(WorkspaceRepository repository, WorkspaceMutation mutation, ObjectMapper json) {
        this.repository = repository;
        this.mutation = mutation;
        this.json = json;
    }

    public WorkspaceAggregate start(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        requireState(current, WorkspaceState.ACCEPTED_BASELINE,
                "Manual editing can start only from an accepted baseline.");

        ObjectNode document = (ObjectNode) current.document().deepCopy();
        ObjectNode acceptedBaseline = (ObjectNode) document.path("acceptedBaseline");
        JsonNode baselineAssignments = acceptedBaseline.path("result").path("timetable").path("assignments");

        ObjectNode draft = document.putObject("manualDraft");
        draft.set("assignments", baselineAssignments.deepCopy());
        draft.putObject("modifications");
        draft.putArray("conflicts");
        draft.put("draftRevision", "sha256:" + Long.toHexString(System.currentTimeMillis()));

        return mutation.replaceManualDraft(
                expectedVersion,
                WorkspaceState.ACCEPTED_BASELINE,
                WorkspaceState.MANUAL_DRAFT,
                document);
    }

    private static void requireState(WorkspaceAggregate current, WorkspaceState expected, String message) {
        if (current.state() != expected) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION", message);
        }
    }
}
