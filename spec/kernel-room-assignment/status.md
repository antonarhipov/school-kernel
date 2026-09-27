# Use-Case Status: Reusable hard room assignments

## Current

- Use case: UC-1
- Status: IN_PROGRESS
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | IN_PROGRESS | none | Implemented locally; full-suite gate green | Not submitted |

## UC-1 Evidence

- Started from: `3fb66a8`.
- Pre-existing dirty files: `spec/kernel-room-assignment/proposal.md` from the same request; no unrelated dirty files.
- Implementation submission: none; checkpoint and convergence are still pending.
- Changed files: `README.md`, `examples/mvk.json`, `spec/README.md`, `spec/kernel-room-assignment/{spec.md,rules.md,status.md}`;
  `kernel-contract/src/main/resources/schema/{school-definition-v1.schema.json,result-v1.schema.json,verification-result-v1.schema.json}`;
  `kernel-cli/src/main/java/org/schoolkernel/contract/{SchoolDefinitionDto.java,ResultFactory.java}`;
  `kernel-cli/src/main/java/org/schoolkernel/domain/{SchoolDefinition.java,RoomAssignmentResolver.java,DefinitionValidator.java,KernelCatalog.java,BaselineVerifier.java}`;
  `kernel-cli/src/main/java/org/schoolkernel/solver/{PlanningLesson.java,PlanningMapper.java,SchoolConstraintProvider.java,ScheduleEvaluator.java,PreflightFeasibilityCheck.java,HardConstraintDiagnostics.java}`;
  `kernel-cli/src/test/java/org/schoolkernel/{cli/RoomAssignmentCliIT.java,cli/PlanCliIT.java,cli/VerifyCliIT.java,contract/ContractTest.java,domain/RoomAssignmentContractTest.java,domain/KernelCatalogTest.java,solver/RoomAssignmentConstraintTest.java}`;
  `timetable-workspace/src/main/java/org/schoolkernel/workspace/RepairDraftService.java`,
  `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceImportIT.java`.
- `RoomAssignmentContractTest`: 4/4 green; `RoomAssignmentConstraintTest`: 2/2 green;
  packaged `RoomAssignmentCliIT`: 2/2 green.
- Complete `WorkspaceImportIT` rerun after changing its obsolete unsupported-catalog assertion to 10:
  25/25 green. After strengthening its catalog-9 case to run a real HTTP repair,
  `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceImportIT#importsAndRepairsRoomAssignmentDefinition -Dfailsafe.failIfNoSpecifiedTests=false verify`:
  1/1 green. The resulting proposal was `FEASIBLE`, independently `VERIFIED`, kept the exact catalog-9
  policies, assigned Math to `room-102`, and left the accepted baseline unchanged by JSON equality.
- `./school-kernel plan --definition examples/mvk.json --output <isolated-temp>/mvk-result.json --time-limit PT60S`:
  catalog 9, `FEASIBLE`, 520 assignments, `TIME_LIMIT`; packaged `verify` on that result: `VERIFIED`.
  Independent by-value assignment check: zero violations of the seven policies. Music remained soft:
  19 lessons in MU and 10 in K2.
- `git diff --check`: clean. The tests and packaged run made no tracked runtime-data changes.
- The prior full reactor reached 95 workspace integration cases and failed 6: the obsolete catalog-9
  rejection assertion (since corrected and covered by the 25/25 import rerun) and five browser/workbench
  assertions in `AcceptedInspectionBrowserIT` (3), `ProposalReviewBrowserIT` (1), and
  `RepairCompletionBrowserIT` (1). Separate UI fixes corrected the teacher-ribbon wording and the
  1280px Proposal layout. A fresh `./mvnw -q clean verify` now passes: 131 unit tests,
  50 packaged-kernel integration tests, and 95 workspace integration tests, with zero failures, errors,
  or skips. The five formerly failing browser tests are included, and no tracked runtime data changed.

## Blockers

None. The previous `VALIDATION_FAILURE` is resolved; UC-1 has not yet been submitted for convergence.

## Deviations

None.
