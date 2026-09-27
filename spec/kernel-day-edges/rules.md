# Technical Rules: Cohort start bounds and day-edge subjects

## Rules

### RULE-1 - Versioned fields

- Applies to: UC-1, UC-2
- Constraint: Catalog 6 MUST accept the cohort fields `latestStartSlot` and `preferredLatestStartSlot` and the
  subject fields `reservedPeriodsAllowed`, `maxWeeklyReservedLessonsPerCohort`, `dayEdgeOnly`, and
  `maxDailyLessonsPerCohort`. Catalogs 1 through 5 MUST reject them. Integer values MUST be at least 1. Every field
  MUST participate in definition revisions.
- Verification: `ContractTest.dayEdgeFieldsSchemaAndRevision`, `DefinitionValidatorTest.dayEdgeFieldDefaults`.

### RULE-2 - One count at every boundary

- Constraint: `SchoolConstraintProvider`, `ScheduleEvaluator`, and `HardConstraintDiagnostics` MUST share one static
  count per new hard constraint and report identical matches. Gap, start-slot, and weekly-balance helpers MUST treat an
  unoccupied reserved period exactly as catalog 5 did.
- Verification: `DayEdgeConstraintTest`.

### RULE-3 - Score levels and range

- Constraint: `hard.reserved-period` MUST use hard level 0; the other new hard constraints MUST use hard level 1.
  Reserved periods MUST enter the planning value range only when some subject permits them.
- Reason: Excluding reserved periods remains the cheapest way to honor the reservation. The hard constraint covers
  only the lessons that share the widened range.
- Verification: `DayEdgeConstraintTest.reservedPeriodMisuseIsPhysicalAndShapeRulesAreLevelOne`,
  `DayEdgeConstraintTest.reservedPeriodsJoinTheRangeOnlyWhenPermitted`.

### RULE-4 - Actor-boundary evidence

- Constraint: Packaged planning MUST refuse a forced late start and a forced middle-of-day edge lesson under catalog
  6 and accept them under catalog 5. MVK MUST plan feasibly at the normal 30-second limit, and its result MUST verify.
- Verification: `DayEdgeCliIT`; packaged MVK plan and verify.
