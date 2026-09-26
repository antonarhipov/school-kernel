# Use-Case Status: Cohort day-start quality

## Current

- Use case: UC-1
- Status: BLOCKED
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | BLOCKED | none | Implemented but full regression gate is red; priority confirmation pending | - |

## UC-1 Evidence

- Started: 2026-09-26 14:21 UTC
- Started from: `15cedab28511d65e8d93da0e2fabb78d56a2783f`
- Pre-existing dirty files: none; this feature's proposal, specification, and rules were created before execution opened.
- Implementation submission: pending; no checkpoint or commit was made because the required gate did not pass.
- Changed files: `README.md`, `examples/mvk.json`, `kernel-cli/src/main/java/org/schoolkernel/domain/{DefinitionValidator,KernelCatalog}.java`, `kernel-cli/src/main/java/org/schoolkernel/solver/{ScheduleEvaluator,SchoolConstraintProvider}.java`, `kernel-cli/src/test/java/org/schoolkernel/{cli/{PlanCliIT,SchoolQualityCliIT,VerifyCliIT},contract/ContractTest,domain/KernelCatalogTest,solver/SchoolQualityConstraintTest}.java`, `kernel-contract/src/main/resources/schema/{result-v1,school-definition-v1,verification-result-v1}.schema.json`, `spec/README.md`, this feature directory, `timetable-workspace/src/main/java/org/schoolkernel/workspace/RepairDraftService.java`, and `timetable-workspace/src/test/java/org/schoolkernel/workspace/{WorkspaceImportIT,WorkspaceRepairDraftIT}.java`.
- Focused kernel and workspace tests: passed, including packaged catalog-3 planning, verification, legacy predecessor repair, zero-weight reporting, and invalid/unsuccessful outputs.
- Normal-limit packaged MVK plan: `FEASIBLE`, 520 assignments, 115 taught cohort-days, zero internal cohort gaps, zero late starts, and 91 nonpreferred-room assignments; independent `verify` returned `VERIFIED` (`/tmp/mvk-start.0eLJNe/result.json` and `verification.json`). One observed 30-second run does not establish an optimality guarantee.
- `./mvnw -q clean verify`: kernel passed (98 tests, no failures/errors/skips); workspace integration tests ran 90 with 11 failures and 1 error, all in browser journeys. `AcceptedInspectionBrowserIT` had five failures, `ProposalReviewBrowserIT` five, `RepairDraftBrowserIT` one, and `RepairCompletionBrowserIT` one click timeout. Workspace unit tests passed.
- Isolated `./mvnw -q -pl timetable-workspace -am -Dit.test=RepairCompletionBrowserIT -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false verify`: the earlier Day click succeeded, but the same journey failed its UC-5 G3 1280px proposal-layout assertion at `RepairCompletionBrowserIT.java:134`; it did not pass.
- `git diff --check`: passed. The test runs left no tracked runtime-data changes.

## Blockers

- `VALIDATION_FAILURE`: RULE-4 requires green applicable kernel and workspace regression gates before convergence. The full workspace gate is red in browser journeys unrelated to the new constraint calculation; the isolated rerun also failed. The feature changed no frontend files, and several failing inspection/proposal tests load stored fixture documents without compiling a repair successor. Do not submit UC-1 for convergence until the workbench browser regressions are resolved in their owning feature and the full gate is rerun, or the user explicitly changes RULE-4's verification scope.
- `SPEC_AMBIGUITY`: The user's gap-over-room priority is explicit, but the relative priority of one gap and one late cohort-day is not. The provisional MVK weights are gap `10`, late start `5`, nonpreferred room `1` per match. A confirmation question is pending; adjust the definition and acceptance evidence if the desired order differs.

## Deviations

None.
