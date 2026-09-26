# Use-Case Status: Class timetable quality

## Current

- Use case: UC-1
- Status: BLOCKED
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | BLOCKED | none | Uncommitted implementation and focused evidence | Not submitted; full regression gate failed |

## UC-1 Evidence

- Started: 2026-09-26
- Started from: `d642bee1d8414e6c0eba8a6822f4360e40decbe4`
- Pre-existing dirty files: none
- Implementation submission: none; no checkpoint or convergence invoked
- Changed files: `README.md`, `examples/mv5.json`, `kernel-cli/src/main/java/org/schoolkernel/application/DefinitionLoader.java`, `VerifyService.java`, `contract/ResultFactory.java`, `contract/VerificationResultFactory.java`, `domain/DefinitionValidator.java`, `domain/KernelCatalog.java`, `solver/PlanningMapper.java`, `solver/ScheduleEvaluator.java`, `solver/SchoolConstraintProvider.java`, `kernel-cli/src/test/java/org/schoolkernel/cli/PlanCliIT.java`, `SchoolQualityCliIT.java`, `VerifyCliIT.java`, `contract/ContractTest.java`, `domain/KernelCatalogTest.java`, `solver/ComponentBoundaryTest.java`, `solver/SchoolQualityConstraintTest.java`, `kernel-cli/src/test/resources/fixtures/mv5-accepted-assignments.tsv`, `kernel-contract/src/main/resources/schema/result-v1.schema.json`, `school-definition-v1.schema.json`, `verification-result-v1.schema.json`, `spec/README.md`, `spec/kernel-schedule-quality/proposal.md`, `spec.md`, `rules.md`, `status.md`, `timetable-workspace/src/main/java/org/schoolkernel/workspace/KernelVerifier.java`, `RepairDraftService.java`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceImportIT.java`, `WorkspaceRepairDraftIT.java`.
- `./mvnw -q -pl kernel-cli -am verify`: PASS; 62 kernel unit and 31 packaged CLI integration tests, no failures or skips. MV5 initial planning produced 5A loads `5,5,4,4,4` with zero class-gap and week-balance matches; MV5 repair used one period move, zero room-only moves, and at most one class gap. Legacy catalog-1 predecessor verification, zero-weight reporting, and rejected/unsuccessful outcomes passed.
- `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceRepairPlanningIT,WorkspaceRepairDraftIT -Dfailsafe.failIfNoSpecifiedTests=false verify`: PASS; 19 PostgreSQL-backed repair integration tests, no failures or skips. A catalog-1 accepted baseline remains unchanged while its repair successor uses catalog 2.
- Packaged full-MV5 planning with `--time-limit PT30S` and independently with deterministic `--step-limit 300000`: both FEASIBLE; 5A loads `5,5,4,4,4`, school-wide class-gap and week-balance matches zero. Time-limited output reports `TIME_LIMIT`, not optimality.
- `git diff --check`: PASS; tests generated no additional tracked changes.
- `./mvnw -q clean verify`: FAILED in the unchanged workbench browser suite, 10 failures among 90 workspace integration tests. Kernel and non-browser workspace tests in that run passed. This full run preceded the baseline-seeding refinement; the focused kernel and workspace suites above passed afterward.

## Blockers

- `VALIDATION_FAILURE` (RULE-4): the required full reactor regression gate is not green. `RepairCompletionBrowserIT` and `ProposalReviewBrowserIT` report the Proposal action below an 800px viewport at 1280px width (`taskBottom=841.47`), while other unchanged workbench browser tests report missing expected proposal/lesson presentation. No presentation source or browser test was changed for this feature. The focused kernel and PostgreSQL repair suites were rerun successfully after the solver refinement; the unrelated browser failures were not modified or hidden. Options: authorize a separate workbench-layout/inspection fix and rerun the full gate, or explicitly amend the RULE-4 gate for this UC while retaining the browser failures as open debt. Recommendation: fix the workbench regression separately, then submit this UC for convergence. Until that decision, no checkpoint or approval is recorded.

## Deviations

None.
