# Use-Case Status: Reserved school periods

## Current

- Use case: UC-1
- Status: BLOCKED
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | BLOCKED | none | Implemented and focused paths green; full gate red | Not submitted |

## UC-1 Evidence

- Started from: `15cedab28511d65e8d93da0e2fabb78d56a2783f`
- Pre-existing dirty files: `README.md`, `examples/mvk.json`, `kernel-cli/src/main/java/org/schoolkernel/domain/{DefinitionValidator,KernelCatalog}.java`, `kernel-cli/src/main/java/org/schoolkernel/solver/{ScheduleEvaluator,SchoolConstraintProvider}.java`, `kernel-cli/src/test/java/org/schoolkernel/{cli/{PlanCliIT,SchoolQualityCliIT,VerifyCliIT},contract/ContractTest,domain/KernelCatalogTest,solver/SchoolQualityConstraintTest}.java`, `kernel-contract/src/main/resources/schema/{result-v1,school-definition-v1,verification-result-v1}.schema.json`, `spec/README.md`, `spec/kernel-cohort-start-quality/`, `timetable-workspace/src/main/java/org/schoolkernel/workspace/RepairDraftService.java`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/{WorkspaceImportIT,WorkspaceRepairDraftIT}.java`. These belong to the earlier uncommitted cohort-start-quality work and are not part of this feature submission.
- Implementation submission: none; no checkpoint or commit was made because the required full gate failed and the pre-existing dirty cohort-start-quality changes overlap this feature.
- Changed files for this feature: `README.md`, `examples/mvk.json`, `kernel-cli/src/main/java/org/schoolkernel/contract/{RevisionService,SchoolDefinitionDto}.java`, `kernel-cli/src/main/java/org/schoolkernel/domain/{BaselineVerifier,DefinitionValidator,SchoolDefinition}.java`, `kernel-cli/src/main/java/org/schoolkernel/solver/{PlanningMapper,PreflightFeasibilityCheck}.java`, `kernel-cli/src/test/java/org/schoolkernel/benchmark/TargetScaleBenchmark.java`, `kernel-cli/src/test/java/org/schoolkernel/cli/ReservedPeriodCliIT.java`, `kernel-cli/src/test/java/org/schoolkernel/domain/BaselineVerifierTest.java`, `kernel-cli/src/test/java/org/schoolkernel/solver/ReplanningSolverTest.java`, `kernel-contract/src/main/resources/schema/school-definition-v1.schema.json`, `spec/README.md`, this feature directory, and `timetable-workspace/src/test/java/org/schoolkernel/workspace/{WorkspaceImportIT,WorkspaceRepairDraftIT}.java`. Files already dirty before this feature retain their earlier changes.
- `./mvnw -q -pl kernel-cli -am verify`: PASS after the final source adjustment; `ReservedPeriodCliIT` ran 6 tests with zero failures/errors/skips.
- `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceImportIT,WorkspaceRepairDraftIT -Dfailsafe.failIfNoSpecifiedTests=false verify`: PASS after the final source adjustment; PostgreSQL-backed `WorkspaceImportIT` ran 23 tests and `WorkspaceRepairDraftIT` ran 10, all green. The new reservation cases passed through the real HTTP import and repair-draft boundaries.
- Final packaged `./school-kernel plan --definition examples/mvk.json --output /tmp/mvk-reserved.iPbg9S/result-final.json --time-limit PT30S --seed 0`: `FEASIBLE`, 520 assignments, zero reserved-period assignments, zero cohort gaps, zero cohort late starts. Independent packaged `verify` returned `VERIFIED` (`/tmp/mvk-reserved.iPbg9S/verification-final.json`). This is one time-limited observed result, not an optimality claim.
- `./mvnw -q clean verify`: FAILED before the final narrow validator adjustment, with 5 failures, 0 errors, and 0 skips among 92 workspace integration tests. Failures were in `AcceptedInspectionBrowserIT` (3), `ProposalReviewBrowserIT` (1), and `RepairCompletionBrowserIT` (1). The added workspace import and repair tests passed; kernel tests passed. The final validator adjustment was followed by the green kernel and focused workspace reruns above.
- `git diff --check`: PASS. Test runs left no new tracked runtime-data changes.

| Contract element | Evidence |
|---|---|
| UC-1 main steps 1-4, G1-G2, success postcondition | `ReservedPeriodCliIT.uc1MainAndG1ReserveExplicitNonZeroPeriodWithoutChangingResourceAvailability`; `WorkspaceImportIT.importsReservedPeriodDefinition`; final MVK packaged plan and independent verification |
| UC-1 extensions 1a-1b, G5 | `ReservedPeriodCliIT.uc1Extensions1aAnd1bRepairMovesOldAssignmentAndPreservesPredecessor`; `WorkspaceRepairDraftIT.compilesReservationAwareRepairWithoutChangingAcceptedPair` |
| UC-1 extensions 2a-2d, minimal guarantee | `ReservedPeriodCliIT.uc1Extensions2bAnd2cRefuseInvalidPolicyAndImpossibleRegularRange`, `uc1G1DoesNotInferReservationFromZeroSuffixAndEmptyListChangesNoPlacement`, and forged-result verification within the repair test |
| UC-1 G3-G4 | `ReservedPeriodCliIT.uc1G3QualityCountsRegularPeriodsOnlyAndG4PreservesLegacyMeaning`, `uc1G3DayWithOnlyReservedPeriodIsNotAvailableForWeekBalance`; full kernel regression |
| RULE-1 | `school-definition-v1.schema.json:22`, `DefinitionValidator.java:58`, `RevisionService.java:27` |
| RULE-2 | `PlanningMapper.java:25`, `PreflightFeasibilityCheck.java:25`, `BaselineVerifier.java:61`, `DefinitionValidator.java:377` |
| RULE-3 | `PlanningMapper.java:25` supplies only regular periods to the existing gap, week-balance, and late-start calculators; packaged score assertions in `ReservedPeriodCliIT` |
| RULE-4 | MVK result and verification under `/tmp/mvk-reserved.iPbg9S/`; full gate remains red |

## Blockers

- `VALIDATION_FAILURE`: RULE-4 requires the applicable full regression gate to be green. The full reactor reproduced 5 pre-existing browser inspection/proposal failures that do not exercise reserved-period definitions; no frontend code was changed here. The reservation UC cannot enter convergence until those owning-feature regressions are fixed and the full gate rerun, or the user explicitly changes its verification scope.
- `TECHNICAL`: the earlier uncommitted catalog-3 cohort-start-quality work overlaps `README.md`, `examples/mvk.json`, `DefinitionValidator.java`, the definition schema, `spec/README.md`, and workspace tests. A coherent immutable reservation-only commit cannot be made from this worktree without also absorbing that blocked work. Preserve both change sets and resolve the earlier checkpoint before submission.

## Deviations

None.
