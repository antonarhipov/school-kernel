# Use-Case Checkpoint: UC-1 - Obtain a cohort-balanced timetable proposal

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `11a9f1d54e9c3246b9363bcd79809e38f5d10814`
- Submission commit: HEAD at convergence
- Relations verified: none; UC-1 is the sole use case in this feature.
- Revision: prior approval was superseded by the revised MVK cohort-gap configuration and RULE-5 in the base contract. Catalog, solver, and workspace production code remain unchanged.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main step 1 | `examples/mvk.json:7-13,495-635` gives every cohort target 1 and five exact overrides; `ContractTest.mvkHasExactBalancedPreferenceConfiguration:81` checks every value; packaged MVK plan accepts the definition | PASS |
| UC-1 main step 2 | `SchoolQualityCliIT.java:204` receives `FEASIBLE`; `SchoolConstraintProvider.java:180` uses the configured target in scoring | PASS |
| UC-1 main step 3 | `SchoolQualityCliIT.java:208` asserts catalog 4 and seven rows on its fixture; packaged MVK result reports cohort-gap weight 1,000,000 and zero matches, late-start weight 10,000 and zero matches, weekly-balance weight 1,000 and three matches | PASS |
| UC-1 main step 4 | Independent assignment tally of all 23 cohorts gives zero internal gaps and zero late starts; 20 meet spread one and 5A, 5B, and 6C have spread two. 6B has `3,4,4,4,4`, first regular slots `1,1,1,3,2`, and zero gaps | PASS |
| UC-1 extension 1a | `SchoolQualityCliIT.java:218` replans from a catalog 3 predecessor to a catalog 4 successor with target 2; `WorkspaceRepairDraftIT.java:128` retains an accepted target and pair | PASS |
| UC-1 extension 1b | `DefinitionValidatorTest.java:50` proves omitted target becomes one | PASS |
| UC-1 extension 1c | `WorkspaceRepairPlanningIT.java:93` takes a catalog 1 accepted pair to a packaged catalog 4 proposal and leaves the accepted pair unchanged | PASS |
| UC-1 extension 1d | `ContractTest.java:52` rejects the new field under catalogs 1-3; existing `PlanCliIT`, `VerifyCliIT`, and `SchoolQualityCliIT` packaged regression cases verify their prior rows and results | PASS |
| UC-1 extension 2a | `SchoolQualityCliIT.java:261` tests negative, fractional, and old-catalog settings: `INVALID_INPUT`, no solver start, no timetable | PASS |
| UC-1 extension 2b | `SchoolQualityCliIT.java:244` sets weight zero: four reported matches, zero aggregate penalty | PASS |
| UC-1 extension 2c | `SchoolQualityCliIT.java:282` gets `NO_FEASIBLE_SOLUTION_FOUND` with no timetable; prior CLI interruption regression is `PlanCliIT.java:291` | PASS |
| UC-1 extension 3a | `SchoolQualityCliIT.java:245-257` produces a bounded `FEASIBLE` result with four remaining matches; the revised MVK result is `FEASIBLE`/`TIME_LIMIT` with three remaining balance matches and no optimality claim | PASS |
| UC-1 G1 | `SchoolQualityConstraintTest.java:143` asserts exact pairwise counts for targets 0 and 2 and an unavailable day; independent MVK pairwise tally gives three matches, agreeing with the reported score | PASS |
| UC-1 G2 | `SchoolQualityConstraintTest.java:166` checks an incremental score change; `ScheduleEvaluator.java:103` and packaged MVK `verify` agree with the solver result | PASS |
| UC-1 G3 | `KernelCatalogTest.java:14`, `ContractTest.java:52`, `SchoolQualityCliIT.java:198`, and old-catalog packaged regression checks | PASS |
| UC-1 G4 | `SchoolConstraintProvider.java:187` stays at ordinary-preference score level; `SchoolQualityCliIT.java:237` asserts no period or room move for the target change | PASS |
| UC-1 G5 | `ContractTest.java:79-105` pins all 23 cohort IDs, target 1, five overrides, and reservations. Normal 30-second seed-0 plan gives zero cohort gaps, zero late starts, three balance matches, 64 teacher gaps, and 421 non-preferred rooms; independent assignment tally agrees. `SchoolQualityConstraintTest.java:36-60` confirms one internal hole costs 1,000,000, while leading/trailing empty periods cost zero | PASS |
| UC-1 success postcondition | Catalog 4 MVK result is `FEASIBLE` with 520 complete assignments; packaged `verify` returned `VERIFIED` | PASS |
| UC-1 minimal guarantee | `SchoolQualityCliIT.java:261` asserts rejected/unsuccessful results contain no timetable, and `PlanCliIT.java:291` covers interruption without publication | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `school-definition-v1.schema.json:78`, `KernelCatalog.java:12`, `DefinitionValidator.java:436`, `ContractTest.java:52`; old catalogs retain their prior rows and reject the field | PASS |
| RULE-2 | `PlanningMapper.java:50`, `SchoolConstraintProvider.java:180`, `ScheduleEvaluator.java:103`, `SchoolQualityConstraintTest.java:143`, independent MVK tally, and the conservative existing `DefinitionValidator.java:313` overflow bound | PASS |
| RULE-3 | `RepairDraftService.java:188`, `WorkspaceRepairDraftIT.java:128`, `WorkspaceRepairPlanningIT.java:93`, and packaged catalog 3 to 4 replan; accepted documents and zero extra moves asserted | PASS |
| RULE-4 | `ContractTest.java:79-105` checks normative MVK values; packaged plan and verify, independent count/start/gap/room inspection, complete kernel suite, affected Docker-backed workspace suite, and `git diff --check` | PASS |
| RULE-5 | `examples/mvk.json:7-13`, `ContractTest.java:79-105`, `SchoolQualityConstraintTest.java:36-60`, packaged plan/verify, and independent zero-gap/zero-penalty tally | PASS |

## Validation

- Focused command: `./mvnw -q -pl kernel-cli -am -Dtest=ContractTest,SchoolQualityConstraintTest -Dsurefire.failIfNoSpecifiedTests=false test` — PASS, including exact MVK configuration and gap scoring.
- Full relevant kernel command: `./mvnw -q -pl kernel-cli -am verify` — 69 unit and 42 packaged CLI integration tests, zero failures/errors/skips.
- Full relevant workspace command: `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceImportIT,WorkspaceRepairDraftIT,WorkspaceRepairPlanningIT -Dfailsafe.failIfNoSpecifiedTests=false verify` — 24 import, 11 repair-draft, and 10 repair-planning integration tests, zero failures/errors/skips, with Docker access. The initial sandboxed attempt could not connect to Docker and ran no test bodies; the authorized retry passed.
- Working tree impact from tests: no additional tracked file changes; `git diff --check` passed.
- Runtime evidence: `./school-kernel plan --definition examples/mvk.json --output /tmp/mvk-gap-priority-a4rAEA/final-result.json --time-limit PT30S --seed 0` returned `FEASIBLE`, catalog 4, `TIME_LIMIT`, 520 assignments, seven score rows, zero cohort gaps and late starts, and three balance matches. `./school-kernel verify --definition examples/mvk.json --result /tmp/mvk-gap-priority-a4rAEA/final-result.json --output /tmp/mvk-gap-priority-a4rAEA/post-test-verification.json` returned `VERIFIED`. Independent assignment inspection reproduced the reported cohort gap, weekly-balance, late-start, teacher-gap, and non-preferred-room counts; 6B has no internal empty periods.
- Changed files: `README.md`, `examples/mvk.json`, `kernel-cli/src/test/java/org/schoolkernel/contract/ContractTest.java`, `kernel-cli/src/test/java/org/schoolkernel/solver/SchoolQualityConstraintTest.java`, `spec/kernel-cohort-daily-balance/status.md`, and this checkpoint. The revised specification and rules are in the base commit.
- Approved UCs regression-tested: none in this new feature; existing catalog 1-3 CLI journeys and affected workspace import/repair journeys passed.

## Notes

The gap, spread, and late-start scores are preferences. The observed normal-limit result avoids all internal cohort gaps and late starts but leaves three cohorts at spread two and has 421 non-preferred room assignments. First-lesson periods still vary by up to two regular slots within a cohort; the existing catalog has no score for differences among starts in slots 1-3. No global optimality claim is made for the time-limited run. The older proposal visible in the browser workspace was not replaced or accepted.

READY FOR CONVERGENCE: UC-1
