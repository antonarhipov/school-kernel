package org.schoolkernel.workspace;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Repository
@Profile("demo")
public class DemoWorkspaceStore implements WorkspaceStore {
    private final DemoWorkspaces workspaces;
    public DemoWorkspaceStore(DemoWorkspaces workspaces) { this.workspaces = workspaces; }
    @Override public boolean ephemeral() { return true; }
    @Override public void recordRepairSaveFailure(long version) {
        locked(entry -> { entry.failedRepairSaveVersion = version; return null; });
    }
    @Override public boolean repairSaveFailed(long version) {
        return locked(entry -> entry.failedRepairSaveVersion == version);
    }

    private <T> T locked(Function<DemoWorkspaces.Entry, T> action) {
        var entry = workspaces.current();
        entry.lock.lock();
        try {
            workspaces.current();
            return action.apply(entry);
        } finally { entry.lock.unlock(); }
    }

    @Override public WorkspaceAggregate load() {
        return locked(entry -> copy(entry.aggregate));
    }

    @Override public Runnable contextual(Runnable action) {
        var owner = workspaces.current();
        return () -> workspaces.bound(owner, action);
    }

    @Override public RepairPinContext loadRepairPinContext(String lessonId) {
        var current = load();
        var baseline = current.document().path("acceptedBaseline");
        return new RepairPinContext(current.state(), current.version(),
                (ObjectNode) current.document().path("repairDraft"),
                find(baseline.path("result").path("timetable").path("assignments"), "lessonId", lessonId),
                find(baseline.path("definition").path("lessons"), "id", lessonId),
                find(baseline.path("manifest").path("locks"), "lessonId", lessonId));
    }

    private static JsonNode find(JsonNode array, String field, String id) {
        for (JsonNode item : array) if (id.equals(item.path(field).stringValue())) return item.deepCopy();
        return null;
    }

    @Override public Optional<Long> clear(long version) {
        return locked(entry -> {
            var current = entry.aggregate;
            if (solving(current.state())) throw new WorkspaceProblem(org.springframework.http.HttpStatus.CONFLICT,
                    "INVALID_WORKSPACE_TRANSITION", "Cancel or finish the active run before clearing this workspace.");
            if (current.state() == WorkspaceState.DEFINITION_DRAFT) return Optional.empty();
            return write(entry, version, current.state(), WorkspaceState.EMPTY, null,
                    tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode(), null);
        });
    }

    @Override public Optional<Long> resetAndReplace(long version, WorkspaceState state, JsonNode document) {
        return locked(entry -> {
            if (solving(entry.aggregate.state())) throw new WorkspaceProblem(org.springframework.http.HttpStatus.CONFLICT,
                    "INVALID_WORKSPACE_TRANSITION", "Cancel or finish the active run before replacing this workspace.");
            return write(entry, version, entry.aggregate.state(), state, null, document, null);
        });
    }

    @Override public Optional<Long> replace(long version, WorkspaceState expected, WorkspaceState next, JsonNode document) {
        return locked(entry -> write(entry, version, expected, next, null, document, null));
    }
    @Override public Optional<Long> startRun(long version, UUID run, JsonNode document) {
        return locked(entry -> write(entry, version, WorkspaceState.INITIAL_DRAFT, WorkspaceState.SOLVING_INITIAL, run, document, null));
    }
    @Override public Optional<Long> startRepairRun(long version, UUID run, JsonNode document) {
        return locked(entry -> write(entry, version, WorkspaceState.REPAIR_DRAFT, WorkspaceState.SOLVING_REPAIR, run, document, null));
    }
    @Override public Optional<Long> finishRun(long version, UUID run, WorkspaceState next, JsonNode document) {
        return locked(entry -> write(entry, version, WorkspaceState.SOLVING_INITIAL, next, null, document, run));
    }
    @Override public Optional<Long> finishRepairRun(long version, UUID run, WorkspaceState next, JsonNode document) {
        return locked(entry -> write(entry, version, WorkspaceState.SOLVING_REPAIR, next, null, document, run));
    }
    @Override public Optional<Long> replaceInitialDraft(long version, WorkspaceState expected, JsonNode document) {
        return replace(version, expected, WorkspaceState.INITIAL_DRAFT, document);
    }
    @Override public Optional<Long> replaceRepairDraft(long version, JsonNode draft) {
        return locked(entry -> {
            ObjectNode document = (ObjectNode) entry.aggregate.document().deepCopy();
            document.set("repairDraft", draft.deepCopy());
            return write(entry, version, WorkspaceState.REPAIR_DRAFT, WorkspaceState.REPAIR_DRAFT, null, document, null);
        });
    }
    @Override public Optional<Long> acceptInitialProposal(long version, JsonNode document) {
        return replace(version, WorkspaceState.INITIAL_PROPOSAL, WorkspaceState.ACCEPTED_BASELINE, document);
    }
    @Override public Optional<Long> acceptRepairProposal(long version, JsonNode document) {
        return replace(version, WorkspaceState.REPAIR_PROPOSAL, WorkspaceState.ACCEPTED_BASELINE, document);
    }
    @Override public int recoverInterruptedInitialRun() { return 0; }
    @Override public int recoverInterruptedRepairRun() { return 0; }

    private Optional<Long> write(DemoWorkspaces.Entry entry, long version, WorkspaceState expected,
            WorkspaceState next, UUID nextRun, JsonNode document, UUID expectedRun) {
        var current = entry.aggregate;
        if (next == WorkspaceState.DEFINITION_DRAFT) throw new WorkspaceProblem(
                org.springframework.http.HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", "This action is unavailable in the demo.");
        if (current.version() != version || current.state() != expected
                || expectedRun != null && !expectedRun.equals(current.activeRunId())) return Optional.empty();
        entry.aggregate = new WorkspaceAggregate(next, version + 1, nextRun, document.deepCopy());
        return Optional.of(version + 1);
    }

    private static boolean solving(WorkspaceState state) {
        return state == WorkspaceState.SOLVING_INITIAL || state == WorkspaceState.SOLVING_REPAIR;
    }
    private static WorkspaceAggregate copy(WorkspaceAggregate source) {
        return new WorkspaceAggregate(source.state(), source.version(), source.activeRunId(), source.document().deepCopy());
    }
}
