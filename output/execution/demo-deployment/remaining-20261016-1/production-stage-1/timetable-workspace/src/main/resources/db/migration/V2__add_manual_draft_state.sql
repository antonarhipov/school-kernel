ALTER TABLE workspace_aggregate DROP CONSTRAINT workspace_aggregate_lifecycle_state_check;
ALTER TABLE workspace_aggregate ADD CONSTRAINT workspace_aggregate_lifecycle_state_check CHECK (lifecycle_state IN (
    'EMPTY', 'INITIAL_DRAFT', 'SOLVING_INITIAL', 'INITIAL_PROPOSAL',
    'ACCEPTED_BASELINE', 'REPAIR_DRAFT', 'SOLVING_REPAIR', 'REPAIR_PROPOSAL',
    'MANUAL_DRAFT'
));
