# Use-Case Checkpoint: UC-1 - Obtain a cohort-balanced timetable proposal

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `b207e88b06ff1895e6496cd42af49191a375aa29`
- Submission commit: HEAD at convergence
- Relations verified: none; UC-1 is the sole use case in this feature.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main step 1 | `examples/mvk.json:493` gives every cohort target 2; `SchoolQualityCliIT.catalogFourPackagedPlanningAndRepairUseTheCohortDailySpread:198` supplies a catalog 4 definition to the packaged CLI | PASS |
| UC-1 main step 2 | `SchoolQualityCliIT.java:204` receives `FEASIBLE`; `SchoolConstraintProvider.java:180` uses the configured target in scoring | PASS |
| UC-1 main step 3 | `SchoolQualityCliIT.java:208` asserts catalog 4, seven rows, and zero matches on its exact fixture; MVK result reports weight 4 and four matches | PASS |
| UC-1 main step 4 | MVK `timetable.assignments` provides cohort and period IDs; independent weekday tally observed 6B `5,5,3,3,3` and three other cohorts at spread 3 | PASS |
| UC-1 extension 1a | `SchoolQualityCliIT.java:218` replans from a catalog 3 predecessor to a catalog 4 successor with target 2; `WorkspaceRepairDraftIT.java:128` retains an accepted target and pair | PASS |
| UC-1 extension 1b | `DefinitionValidatorTest.java:50` proves omitted target becomes one | PASS |
| UC-1 extension 1c | `WorkspaceRepairPlanningIT.java:93` takes a catalog 1 accepted pair to a packaged catalog 4 proposal and leaves the accepted pair unchanged | PASS |
| UC-1 extension 1d | `ContractTest.java:51` rejects the new field under catalogs 1-3; existing `PlanCliIT`, `VerifyCliIT`, and `SchoolQualityCliIT` packaged regression cases verify their prior rows and results | PASS |
| UC-1 extension 2a | `SchoolQualityCliIT.java:261` tests negative, fractional, and old-catalog settings: `INVALID_INPUT`, no solver start, no timetable | PASS |
| UC-1 extension 2b | `SchoolQualityCliIT.java:244` sets weight zero: four reported matches, zero aggregate penalty | PASS |
| UC-1 extension 2c | `SchoolQualityCliIT.java:282` gets `NO_FEASIBLE_SOLUTION_FOUND` with no timetable; prior CLI interruption regression is `PlanCliIT.java:291` | PASS |
| UC-1 extension 3a | MVK time-limited result is `FEASIBLE`/`TIME_LIMIT` with four weekly-balance matches and no optimality claim | PASS |
| UC-1 G1 | `SchoolQualityConstraintTest.java:132` asserts exact pairwise counts for targets 0 and 2 and an unavailable day; independent MVK tally agrees with reported count 4 | PASS |
| UC-1 G2 | `SchoolQualityConstraintTest.java:155` checks an incremental score change; `ScheduleEvaluator.java:103` and packaged MVK `verify` agree with the solver result | PASS |
| UC-1 G3 | `KernelCatalogTest.java:14`, `ContractTest.java:51`, `SchoolQualityCliIT.java:198`, and old-catalog packaged regression checks | PASS |
| UC-1 G4 | `SchoolConstraintProvider.java:187` stays at ordinary-preference score level; `SchoolQualityCliIT.java:237` asserts no period or room move for the target change | PASS |
| UC-1 G5 | MVK has 23 of 23 targets set to 2 and weekly-balance weight 4. Normal 30-second plan improved 6B spread 3 to 2; three cohorts remain at spread 3 | PASS |
| UC-1 success postcondition | Catalog 4 MVK result is `FEASIBLE` with 520 complete assignments; packaged `verify` returned `VERIFIED` | PASS |
| UC-1 minimal guarantee | `SchoolQualityCliIT.java:261` asserts rejected/unsuccessful results contain no timetable, and `PlanCliIT.java:291` covers interruption without publication | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `school-definition-v1.schema.json:78`, `KernelCatalog.java:12`, `DefinitionValidator.java:436`, `ContractTest.java:51`; old catalogs retain their prior rows and reject the field | PASS |
| RULE-2 | `PlanningMapper.java:50`, `SchoolConstraintProvider.java:180`, `ScheduleEvaluator.java:103`, `SchoolQualityConstraintTest.java:132`, independent MVK tally, and the conservative existing `DefinitionValidator.java:313` overflow bound | PASS |
| RULE-3 | `RepairDraftService.java:188`, `WorkspaceRepairDraftIT.java:128`, `WorkspaceRepairPlanningIT.java:93`, and packaged catalog 3 to 4 replan; accepted documents and zero extra moves asserted | PASS |
| RULE-4 | Packaged CLI tests, normal-limit MVK plan and verify, complete kernel suite, affected Docker-backed workspace suite, and `git diff --check` | PASS |

## Validation

- Focused command: `./mvnw -q -pl kernel-cli -am -Dtest=SchoolQualityConstraintTest,ContractTest,DefinitionValidatorTest,KernelCatalogTest -Dit.test=SchoolQualityCliIT -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false verify` — PASS after the test fixture lookup was corrected.
- Full relevant kernel command: `./mvnw -q -pl kernel-cli -am verify` — 68 unit and 42 packaged CLI integration tests, zero failures/errors/skips.
- Full relevant workspace command: `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceImportIT,WorkspaceRepairDraftIT,WorkspaceRepairPlanningIT -Dfailsafe.failIfNoSpecifiedTests=false verify` — 24 import, 11 repair-draft, and 10 repair-planning integration tests, zero failures/errors/skips, with Docker Desktop access. The sandboxed attempt stopped at Testcontainers initialization because Docker socket access was denied; the same command passed outside the sandbox.
- Working tree impact from tests: no additional tracked file changes; `git diff --check` passed.
- Runtime evidence: `./school-kernel plan --definition examples/mvk.json --output /tmp/mvk-cohort-balance.FYx63L/target4.json --time-limit PT30S --seed 0` returned `FEASIBLE`, catalog 4, `TIME_LIMIT`, 520 assignments, 6B spread 2, 9C spread 1, and four balance matches. `./school-kernel verify` of that pair returned `VERIFIED` in `/tmp/mvk-cohort-balance.FYx63L/target4-verification.json`. The original catalog 3 run is `/tmp/mvk-cohort-balance.FYx63L/baseline.json`.
- Changed files: `README.md`, `examples/mvk.json`, `kernel-cli/src/main/java/org/schoolkernel/contract/SchoolDefinitionDto.java`, `kernel-cli/src/main/java/org/schoolkernel/domain/DefinitionValidator.java`, `kernel-cli/src/main/java/org/schoolkernel/domain/KernelCatalog.java`, `kernel-cli/src/main/java/org/schoolkernel/domain/SchoolDefinition.java`, `kernel-cli/src/main/java/org/schoolkernel/solver/PlanningLesson.java`, `kernel-cli/src/main/java/org/schoolkernel/solver/PlanningMapper.java`, `kernel-cli/src/main/java/org/schoolkernel/solver/SchoolConstraintProvider.java`, `kernel-cli/src/test/java/org/schoolkernel/cli/PlanCliIT.java`, `kernel-cli/src/test/java/org/schoolkernel/cli/SchoolQualityCliIT.java`, `kernel-cli/src/test/java/org/schoolkernel/cli/VerifyCliIT.java`, `kernel-cli/src/test/java/org/schoolkernel/contract/ContractTest.java`, `kernel-cli/src/test/java/org/schoolkernel/domain/DefinitionValidatorTest.java`, `kernel-cli/src/test/java/org/schoolkernel/domain/KernelCatalogTest.java`, `kernel-cli/src/test/java/org/schoolkernel/solver/SchoolQualityConstraintTest.java`, `kernel-contract/src/main/resources/schema/result-v1.schema.json`, `kernel-contract/src/main/resources/schema/school-definition-v1.schema.json`, `kernel-contract/src/main/resources/schema/verification-result-v1.schema.json`, `spec/README.md`, `spec/kernel-cohort-daily-balance/{proposal,spec,rules,status}.md`, this checkpoint, `timetable-workspace/src/main/java/org/schoolkernel/workspace/RepairDraftService.java`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceImportIT.java`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceRepairDraftIT.java`, and `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceRepairPlanningIT.java`.
- Approved UCs regression-tested: none in this new feature; existing catalog 1-3 CLI journeys and affected workspace import/repair journeys passed.

## Notes

The spread is a preference. The observed MVK run still has three cohorts at spread 3 and two late starts; it has zero cohort gaps. The old run had no late starts, 6B at spread 3, and 9C at spread 5. No optimality claim is made for either time-limited run.

READY FOR CONVERGENCE: UC-1
