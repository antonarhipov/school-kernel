# Technical Rules: Gap-free cohort days

## Rules

### RULE-1 - Versioned allowance

- Constraint: Catalog 5 MUST accept a non-negative integer `maxDailyGaps` on cohorts and default its omission to zero.
  Catalogs 1 through 4 MUST reject the field and map every cohort to an unlimited allowance. The field MUST
  participate in definition revisions.
- Verification: `ContractTest.cohortDailyGapsSchemaAndRevision`,
  `DefinitionValidatorTest.cohortDailyGapsAreForbiddenByDefaultOnlyFromCatalogFive`.

### RULE-2 - One gap count at every boundary

- Constraint: `SchoolConstraintProvider.cohortDailyGaps`, `ScheduleEvaluator`, and `HardConstraintDiagnostics` MUST
  share the catalog 2 gap count and report identical excess.
- Verification: `SchoolQualityConstraintTest.cohortGapIsHardFromCatalogFiveUnlessTheCohortAllowsIt`.

### RULE-3 - Score levels

- Constraint: The planning score MUST have two hard levels: level 0 for the physical hard catalog and level 1 for
  `hard.cohort-gap`. Stability and preference soft levels are unchanged.
- Reason: Resolving room and teacher conflicts before gaps preserves the search behavior that made the soft gap
  preference effective, while still refusing to publish gaps.
- Verification: `SolverConfigurationTest.bendableProductLevels`,
  `SchoolQualityConstraintTest.cohortGapHardLevelRanksBelowPhysicalConflicts`.

### RULE-4 - Actor-boundary evidence

- Constraint: Packaged planning MUST refuse a forced gap under catalog 5, accept it once the cohort allows it, and
  accept it under catalog 4. MVK MUST plan gap-free at the normal 30-second limit.
- Verification: `SchoolQualityCliIT.catalogFiveForbidsCohortGapsUnlessTheCohortAllowsThem`; packaged MVK plan.
