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

    public RepairPinContext loadRepairPinContext(String lessonId) {
        return jdbc.sql("""
                        SELECT lifecycle_state,
                               version,
                               (document -> 'repairDraft')::text,
                               (SELECT a.value
                                FROM jsonb_array_elements(document #> '{acceptedBaseline,result,timetable,assignments}') AS a(value)
                                WHERE a.value ->> 'lessonId' = :lesson_id
                                LIMIT 1)::text,
                               (SELECT l.value
                                FROM jsonb_array_elements(document #> '{acceptedBaseline,definition,lessons}') AS l(value)
                                WHERE l.value ->> 'id' = :lesson_id
                                LIMIT 1)::text,
                               (SELECT m.value
                                FROM jsonb_array_elements(COALESCE(document #> '{acceptedBaseline,manifest,locks}', '[]'::jsonb)) AS m(value)
                                WHERE m.value ->> 'lessonId' = :lesson_id
                                LIMIT 1)::text
                        FROM workspace_aggregate
                        WHERE workspace_id = 1
                        """)
                .param("lesson_id", lessonId)
                .query((row, ignored) -> new RepairPinContext(
                        WorkspaceState.valueOf(row.getString(1)),
                        row.getLong(2),
                        (tools.jackson.databind.node.ObjectNode) readNullable(row.getString(3)),
                        readNullable(row.getString(4)),
                        readNullable(row.getString(5)),
                        readNullable(row.getString(6))))
                .single();
    }

    public Optional<Long> clear(long expectedVersion) {
        return jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state = 'EMPTY',
                            version = version + 1,
                            active_run_id = NULL,
                            document = '{}'::jsonb
                        WHERE workspace_id = 1
                          AND version = :expected_version
                        RETURNING version
                        """)
                .param("expected_version", expectedVersion)
                .query(Long.class)
                .optional();
    }

    public Optional<Long> resetAndReplace(
            long expectedVersion,
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
                        RETURNING version
                        """)
                .param("next_state", nextState.name())
                .param("document", serialized)
                .param("expected_version", expectedVersion)
                .query(Long.class)
                .optional();
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

    public Optional<Long> startRepairRun(long expectedVersion, UUID runId, JsonNode document) {
        return update(expectedVersion, WorkspaceState.REPAIR_DRAFT, WorkspaceState.SOLVING_REPAIR,
                runId, document, false);
    }

    public Optional<Long> finishRun(
            long expectedVersion,
            UUID runId,
            WorkspaceState nextState,
            JsonNode document) {
        return update(expectedVersion, WorkspaceState.SOLVING_INITIAL, nextState, runId, document, true);
    }

    public Optional<Long> finishRepairRun(
            long expectedVersion,
            UUID runId,
            WorkspaceState nextState,
            JsonNode document) {
        return update(expectedVersion, WorkspaceState.SOLVING_REPAIR, nextState, runId, document, true);
    }

    public Optional<Long> replaceInitialDraft(
            long expectedVersion,
            WorkspaceState expectedState,
            JsonNode document) {
        return update(expectedVersion, expectedState, WorkspaceState.INITIAL_DRAFT, null, document, true);
    }

    public Optional<Long> replaceRepairDraft(long expectedVersion, JsonNode repairDraft) {
        return jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET version = version + 1,
                            document = jsonb_set(document, '{repairDraft}', CAST(:repair_draft AS jsonb), false)
                        WHERE workspace_id = 1
                          AND version = :expected_version
                          AND lifecycle_state = 'REPAIR_DRAFT'
                        RETURNING version
                        """)
                .param("repair_draft", serialize(repairDraft))
                .param("expected_version", expectedVersion)
                .query(Long.class)
                .optional();
    }

    public Optional<Long> acceptInitialProposal(long expectedVersion, JsonNode document) {
        return update(expectedVersion, WorkspaceState.INITIAL_PROPOSAL, WorkspaceState.ACCEPTED_BASELINE,
                null, document, true);
    }

    public Optional<Long> acceptRepairProposal(long expectedVersion, JsonNode document) {
        return update(expectedVersion, WorkspaceState.REPAIR_PROPOSAL, WorkspaceState.ACCEPTED_BASELINE,
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

    public int recoverInterruptedRepairRun() {
        return jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state = 'REPAIR_DRAFT',
                            version = version + 1,
                            active_run_id = NULL,
                            document = jsonb_set(document - 'run' - 'proposal', '{lastRun}',
                                jsonb_build_object('id', document->'run'->>'id',
                                    'kind', 'REPAIR', 'limit', document->'run'->>'limit',
                                    'intentRevision', document->'run'->>'intentRevision',
                                    'status', 'FAILED', 'feasible', false, 'code', 'INTERRUPTED',
                                    'message', 'Repair generation was interrupted.'))
                        WHERE workspace_id = 1
                          AND lifecycle_state = 'SOLVING_REPAIR'
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
                .param("next_run_id", nextState == WorkspaceState.SOLVING_INITIAL
                        || nextState == WorkspaceState.SOLVING_REPAIR ? runId : null)
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

    private JsonNode readNullable(String value) {
        return value == null ? null : readTree(value);
    }

    private String serialize(JsonNode value) {
        try {
            return json.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Workspace document could not be serialized", exception);
        }
    }

    public record RepairPinContext(
            WorkspaceState state,
            long version,
            tools.jackson.databind.node.ObjectNode repairDraft,
            JsonNode assignment,
            JsonNode lesson,
            JsonNode manifestLock) {}
}
