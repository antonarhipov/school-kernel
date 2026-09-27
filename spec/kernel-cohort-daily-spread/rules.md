# Technical Rules: Hard cohort daily lesson spread

## Rules

### RULE-1 - Versioned field

- Applies to: UC-1
- Constraint: Catalog 7 MUST accept a non-negative integer `dailyLessonSpreadLimit` on cohorts and map its omission to
  no limit. Catalogs 1 through 6 MUST reject the field. It MUST participate in definition revisions.
- Verification: `ContractTest.dailySpreadLimitSchemaAndRevision`,
  `DefinitionValidatorTest.dailySpreadLimitIsUnboundedUnlessDeclared`.

### RULE-2 - One count at every boundary

- Constraint: `SchoolConstraintProvider.cohortDailySpread`, `ScheduleEvaluator`, and `HardConstraintDiagnostics` MUST
  share the catalog 4 weekly-balance count, evaluated against the limit, and report identical matches.
- Verification: `DailySpreadConstraintTest`.

### RULE-3 - Score level

- Constraint: `hard.cohort-daily-spread` MUST use hard level 1 with the other day-shape rules.
- Reason: Physical conflicts are resolved first, as for gaps and start bounds.
- Verification: `DailySpreadConstraintTest.spreadIsDayShapeHard`.

### RULE-4 - Actor-boundary evidence

- Constraint: Packaged planning MUST refuse a forced spread beyond the limit under catalog 7, accept it once the limit
  allows it, and accept it under catalog 6. MVK MUST plan feasibly at a 60-second limit, and its result MUST verify.
- Verification: `DailySpreadCliIT`; packaged MVK plan and verify;
  `ContractTest.mvkHasExactBalancedPreferenceConfiguration` for the MVK start bounds and music rooms.

### RULE-5 - Workspace run limit

- Constraint: The workspace's normal initial and repair runs MUST pass a one-minute kernel limit, report it as `PT1M`,
  and keep a watchdog no later than ten seconds after it. The two-minute retry is unchanged.
- Verification: `KernelPlannerTest`, `WorkspaceInitialPlanningIT`, `WorkspaceRepairPlanningIT`.
