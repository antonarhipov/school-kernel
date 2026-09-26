# Use-Case Checkpoint: UC-1 - Obtain a cohort-balanced timetable proposal

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `c4e9c8ddf31e081fcc3c555534715022019b09d0`
- Submission commit: HEAD at convergence
- Relations verified: none; UC-1 is the sole use case in this feature.
- Revision: the prior target-two approval in `444d69c` was invalidated by the revised normative MVK configuration in the base commit; catalog and solver production code remain unchanged.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main step 1 | `examples/mvk.json:7-13,495-635` gives every cohort target 1 and five exact overrides; `ContractTest.mvkHasExactBalancedPreferenceConfiguration:81` checks every value; packaged MVK plan accepts the definition | PASS |
| UC-1 main step 2 | `SchoolQualityCliIT.java:204` receives `FEASIBLE`; `SchoolConstraintProvider.java:180` uses the configured target in scoring | PASS |
| UC-1 main step 3 | `SchoolQualityCliIT.java:208` asserts catalog 4 and seven rows on its fixture; packaged MVK result reports weekly-balance weight 30, zero matches, zero balance penalty, and seven rows | PASS |
| UC-1 main step 4 | Independent tally of packaged MVK assignments gives all 23 cohorts spread 0-1; 6B has `4,4,4,3,4`, first regular slots `3,1,3,1,3`; no start is after slot 3 | PASS |
| UC-1 extension 1a | `SchoolQualityCliIT.java:218` replans from a catalog 3 predecessor to a catalog 4 successor with target 2; `WorkspaceRepairDraftIT.java:128` retains an accepted target and pair | PASS |
| UC-1 extension 1b | `DefinitionValidatorTest.java:50` proves omitted target becomes one | PASS |
| UC-1 extension 1c | `WorkspaceRepairPlanningIT.java:93` takes a catalog 1 accepted pair to a packaged catalog 4 proposal and leaves the accepted pair unchanged | PASS |
| UC-1 extension 1d | `ContractTest.java:52` rejects the new field under catalogs 1-3; existing `PlanCliIT`, `VerifyCliIT`, and `SchoolQualityCliIT` packaged regression cases verify their prior rows and results | PASS |
| UC-1 extension 2a | `SchoolQualityCliIT.java:261` tests negative, fractional, and old-catalog settings: `INVALID_INPUT`, no solver start, no timetable | PASS |
| UC-1 extension 2b | `SchoolQualityCliIT.java:244` sets weight zero: four reported matches, zero aggregate penalty | PASS |
| UC-1 extension 2c | `SchoolQualityCliIT.java:282` gets `NO_FEASIBLE_SOLUTION_FOUND` with no timetable; prior CLI interruption regression is `PlanCliIT.java:291` | PASS |
| UC-1 extension 3a | `SchoolQualityCliIT.java:245-257` produces a bounded `FEASIBLE` result with four remaining matches; the revised MVK result is `FEASIBLE`/`TIME_LIMIT` with no optimality claim | PASS |
| UC-1 G1 | `SchoolQualityConstraintTest.java:132` asserts exact pairwise counts for targets 0 and 2 and an unavailable day; independent MVK counts all have spread at most 1 and agree with the reported zero matches | PASS |
| UC-1 G2 | `SchoolQualityConstraintTest.java:155` checks an incremental score change; `ScheduleEvaluator.java:103` and packaged MVK `verify` agree with the solver result | PASS |
| UC-1 G3 | `KernelCatalogTest.java:14`, `ContractTest.java:52`, `SchoolQualityCliIT.java:198`, and old-catalog packaged regression checks | PASS |
| UC-1 G4 | `SchoolConstraintProvider.java:187` stays at ordinary-preference score level; `SchoolQualityCliIT.java:237` asserts no period or room move for the target change | PASS |
| UC-1 G5 | `ContractTest.java:79-105` pins all 23 cohort IDs, target 1, five overrides, and reservations. Normal 30-second seed-0 plan gives 3 cohorts at spread 0 and 20 at spread 1, zero late starts, four cohort gaps, seven teacher gaps, and 76 non-preferred rooms; independent assignment tally agrees. | PASS |
| UC-1 success postcondition | Catalog 4 MVK result is `FEASIBLE` with 520 complete assignments; packaged `verify` returned `VERIFIED` | PASS |
| UC-1 minimal guarantee | `SchoolQualityCliIT.java:261` asserts rejected/unsuccessful results contain no timetable, and `PlanCliIT.java:291` covers interruption without publication | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `school-definition-v1.schema.json:78`, `KernelCatalog.java:12`, `DefinitionValidator.java:436`, `ContractTest.java:52`; old catalogs retain their prior rows and reject the field | PASS |
| RULE-2 | `PlanningMapper.java:50`, `SchoolConstraintProvider.java:180`, `ScheduleEvaluator.java:103`, `SchoolQualityConstraintTest.java:132`, independent MVK tally, and the conservative existing `DefinitionValidator.java:313` overflow bound | PASS |
| RULE-3 | `RepairDraftService.java:188`, `WorkspaceRepairDraftIT.java:128`, `WorkspaceRepairPlanningIT.java:93`, and packaged catalog 3 to 4 replan; accepted documents and zero extra moves asserted | PASS |
| RULE-4 | `ContractTest.java:79-105` checks normative MVK values; packaged plan and verify, independent count/start/gap/room inspection, complete kernel suite, affected Docker-backed workspace suite, and `git diff --check` | PASS |

## Validation

- Focused command: `./mvnw -q -pl kernel-cli -am -Dtest=ContractTest -Dsurefire.failIfNoSpecifiedTests=false test` — PASS, including the new exact-MVK assertion.
- Full relevant kernel command: `./mvnw -q -pl kernel-cli -am verify` — 69 unit and 42 packaged CLI integration tests, zero failures/errors/skips after the new assertion was added.
- Full relevant workspace command: `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceImportIT,WorkspaceRepairDraftIT,WorkspaceRepairPlanningIT -Dfailsafe.failIfNoSpecifiedTests=false verify` — 24 import, 11 repair-draft, and 10 repair-planning integration tests, zero failures/errors/skips, with Docker Desktop access.
- Working tree impact from tests: no additional tracked file changes; `git diff --check` passed.
- Runtime evidence: `./school-kernel plan --definition examples/mvk.json --output /tmp/mvk-balance-JMwsNc/final-result.json --time-limit PT30S --seed 0` returned `FEASIBLE`, catalog 4, `TIME_LIMIT`, 520 assignments, seven score rows, and zero balance and late-start matches. `./school-kernel verify --definition examples/mvk.json --result /tmp/mvk-balance-JMwsNc/final-result.json --output /tmp/mvk-balance-JMwsNc/final-verification.json` returned `VERIFIED`. Independent assignment inspection reproduced four cohort gaps, seven teacher gaps, zero late starts, and 76 non-preferred rooms. A second 30-second trial with the same values also reached 23 of 23 load targets and zero late starts.
- Changed files: `README.md`, `examples/mvk.json`, `kernel-cli/src/test/java/org/schoolkernel/contract/ContractTest.java`, `spec/kernel-cohort-daily-balance/status.md`, and this checkpoint. The revised proposal, specification, rules, and `NEEDS_REVISION` transition are in base commit `c4e9c8d`.
- Approved UCs regression-tested: none in this new feature; existing catalog 1-3 CLI journeys and affected workspace import/repair journeys passed.

## Notes

The spread and late-start scores are preferences. The observed normal-limit result reaches the tightest possible daily-load spread for all 23 cohorts and avoids late starts, but has four within-day cohort gaps and 76 non-preferred room assignments. First-lesson periods still vary by up to two regular slots within a cohort; the existing catalog has no score for differences among starts in slots 1-3. No global optimality claim is made for the time-limited run.

READY FOR CONVERGENCE: UC-1
