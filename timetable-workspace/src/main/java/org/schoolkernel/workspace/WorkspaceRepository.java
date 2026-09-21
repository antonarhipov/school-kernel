package org.schoolkernel.workspace;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Repository
public class WorkspaceRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public WorkspaceRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public WorkspaceAggregate load() {
        return jdbc.sql("""
                        SELECT lifecycle_state, version, active_run_id, document::text
                        FROM workspace_aggregate
                        WHERE workspace_id = 1
                        """)
                .query((row, ignored) -> new WorkspaceAggregate(
                        WorkspaceState.valueOf(row.getString(1)),
                        row.getLong(2),
                        row.getObject(3, UUID.class),
                        readTree(row.getString(4))))
                .single();
    }

    public Optional<Long> replace(
            long expectedVersion,
            WorkspaceState expectedState,
            WorkspaceState nextState,
            JsonNode document) {
        String serialized;
        try {
            serialized = json.writeValueAsString(document);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Workspace document could not be serialized", exception);
        }
        return jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state = :next_state,
                            version = version + 1,
                            active_run_id = NULL,
                            document = CAST(:document AS jsonb)
                        WHERE workspace_id = 1
                          AND version = :expected_version
                          AND lifecycle_state = :expected_state
                        RETURNING version
                        """)
                .param("next_state", nextState.name())
                .param("document", serialized)
                .param("expected_version", expectedVersion)
                .param("expected_state", expectedState.name())
                .query(Long.class)
                .optional();
    }

    public Optional<Long> startRun(long expectedVersion, UUID runId, JsonNode document) {
        return update(expectedVersion, WorkspaceState.INITIAL_DRAFT, WorkspaceState.SOLVING_INITIAL,
                runId, document, false);
    }

    public Optional<Long> finishRun(
            long expectedVersion,
            UUID runId,
            WorkspaceState nextState,
            JsonNode document) {
        return update(expectedVersion, WorkspaceState.SOLVING_INITIAL, nextState, runId, document, true);
    }

    public Optional<Long> replaceInitialDraft(
            long expectedVersion,
            WorkspaceState expectedState,
            JsonNode document) {
        return update(expectedVersion, expectedState, WorkspaceState.INITIAL_DRAFT, null, document, true);
    }

    public Optional<Long> acceptInitialProposal(long expectedVersion, JsonNode document) {
        return update(expectedVersion, WorkspaceState.INITIAL_PROPOSAL, WorkspaceState.ACCEPTED_BASELINE,
                null, document, true);
    }

    public int recoverInterruptedInitialRun() {
        return jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state = 'INITIAL_DRAFT',
                            version = version + 1,
                            active_run_id = NULL,
                            document = document - 'run' - 'proposal'
                        WHERE workspace_id = 1
                          AND lifecycle_state IN ('SOLVING_INITIAL', 'INITIAL_PROPOSAL')
                        """)
                .update();
    }

    private Optional<Long> update(
            long expectedVersion,
            WorkspaceState expectedState,
            WorkspaceState nextState,
            UUID runId,
            JsonNode document,
            boolean matchRun) {
        String serialized;
        try {
            serialized = json.writeValueAsString(document);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Workspace document could not be serialized", exception);
        }
        String runPredicate = matchRun && runId != null ? "AND active_run_id = :run_id" : "";
        var query = jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state = :next_state,
                            version = version + 1,
                            active_run_id = :next_run_id,
                            document = CAST(:document AS jsonb)
                        WHERE workspace_id = 1
                          AND version = :expected_version
                          AND lifecycle_state = :expected_state
                        """ + runPredicate + " RETURNING version")
                .param("next_state", nextState.name())
                .param("next_run_id", nextState == WorkspaceState.SOLVING_INITIAL ? runId : null)
                .param("document", serialized)
                .param("expected_version", expectedVersion)
                .param("expected_state", expectedState.name());
        if (matchRun && runId != null) {
            query = query.param("run_id", runId);
        }
        return query.query(Long.class).optional();
    }

    private JsonNode readTree(String value) {
        try {
            return json.readTree(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored workspace document is invalid JSON", exception);
        }
    }
}
