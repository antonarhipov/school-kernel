package org.schoolkernel.workspace;

import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Atomic, version-guarded storage for the workspace bound to the current operation. */
public interface WorkspaceStore {
    WorkspaceAggregate load();

    RepairPinContext loadRepairPinContext(String lessonId);

    Optional<Long> clear(long expectedVersion);

    Optional<Long> resetAndReplace(
            long expectedVersion,
            WorkspaceState nextState,
            JsonNode document);

    Optional<Long> replace(
            long expectedVersion,
            WorkspaceState expectedState,
            WorkspaceState nextState,
            JsonNode document);

    Optional<Long> startRun(long expectedVersion, UUID runId, JsonNode document);

    Optional<Long> startRepairRun(long expectedVersion, UUID runId, JsonNode document);

    Optional<Long> finishRun(
            long expectedVersion,
            UUID runId,
            WorkspaceState nextState,
            JsonNode document);

    Optional<Long> finishRepairRun(
            long expectedVersion,
            UUID runId,
            WorkspaceState nextState,
            JsonNode document);

    Optional<Long> replaceInitialDraft(
            long expectedVersion,
            WorkspaceState expectedState,
            JsonNode document);

    Optional<Long> replaceRepairDraft(long expectedVersion, JsonNode repairDraft);

    Optional<Long> acceptInitialProposal(long expectedVersion, JsonNode document);

    Optional<Long> acceptRepairProposal(long expectedVersion, JsonNode document);

    int recoverInterruptedInitialRun();

    int recoverInterruptedRepairRun();

    /** Carry explicit owner authority into background work; local storage needs no binding. */
    default Runnable contextual(Runnable action) { return action; }
    default boolean ephemeral() { return false; }
    void recordRepairSaveFailure(long version);
    boolean repairSaveFailed(long version);

    public record RepairPinContext(
            WorkspaceState state,
            long version,
            tools.jackson.databind.node.ObjectNode repairDraft,
            JsonNode assignment,
            JsonNode lesson,
            JsonNode manifestLock) {}
}
