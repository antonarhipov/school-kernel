CREATE TABLE workspace_aggregate (
    workspace_id SMALLINT PRIMARY KEY CHECK (workspace_id = 1),
    lifecycle_state VARCHAR(32) NOT NULL CHECK (lifecycle_state IN (
        'EMPTY', 'INITIAL_DRAFT', 'SOLVING_INITIAL', 'INITIAL_PROPOSAL',
        'ACCEPTED_BASELINE', 'REPAIR_DRAFT', 'SOLVING_REPAIR', 'REPAIR_PROPOSAL'
    )),
    version BIGINT NOT NULL CHECK (version >= 0),
    active_run_id UUID,
    document JSONB NOT NULL
);

INSERT INTO workspace_aggregate (workspace_id, lifecycle_state, version, active_run_id, document)
VALUES (1, 'EMPTY', 0, NULL, '{}'::jsonb);
