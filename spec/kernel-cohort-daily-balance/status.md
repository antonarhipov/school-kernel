# Use-Case Status: Cohort daily lesson balance

## Current

- Use case: UC-1
- Status: APPROVED
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | [Committed submission](checkpoints/UC-1.md) at 208ec69 | [Convergence approved](convergence/UC-1.md) |

## UC-1 Evidence

- Revised contract: 2026-09-27 follow-up tightens MVK's normative target and weights and adds first-start inspection to main step 4 and G5. The prior approval covers the earlier target-two fixture only; UC-1 requires a new submission and convergence.

- Started: 2026-09-26 20:22 UTC
- Started from: `b207e88b06ff1895e6496cd42af49191a375aa29`
- Pre-existing dirty files: `spec/kernel-cohort-daily-balance/proposal.md`, `spec.md`, and `rules.md` were created for this feature before execution opened; no unrelated dirty files.
- Implementation submission: HEAD at convergence, with `checkpoints/UC-1.md`.
- Changed files: `README.md`, `examples/mvk.json`, `kernel-contract/src/main/resources/schema/{school-definition-v1,result-v1,verification-result-v1}.schema.json`, `kernel-cli/src/main/java/org/schoolkernel/{contract/SchoolDefinitionDto,domain/{DefinitionValidator,KernelCatalog,SchoolDefinition},solver/{PlanningLesson,PlanningMapper,SchoolConstraintProvider}}.java`, `kernel-cli/src/test/java/org/schoolkernel/{cli/{PlanCliIT,SchoolQualityCliIT,VerifyCliIT},contract/ContractTest,domain/{DefinitionValidatorTest,KernelCatalogTest},solver/SchoolQualityConstraintTest}.java`, `timetable-workspace/src/main/java/org/schoolkernel/workspace/RepairDraftService.java`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/{WorkspaceImportIT,WorkspaceRepairDraftIT,WorkspaceRepairPlanningIT}.java`, `spec/README.md`, and this feature's proposal, specification, rules, status, and checkpoint.
- `./mvnw -q -pl kernel-cli -am verify`: PASS after correcting a fixture lookup in the new CLI test; 68 kernel unit tests and 42 packaged CLI integration tests, zero failures/errors/skips.
- `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceImportIT,WorkspaceRepairDraftIT,WorkspaceRepairPlanningIT -Dfailsafe.failIfNoSpecifiedTests=false verify`: PASS with Docker Desktop access; 24 import, 11 repair-draft, and 10 repair-planning integration tests, zero failures/errors/skips. The sandboxed attempt could not reach the Docker socket before running test bodies; an elevated rerun passed.
- Packaged `./school-kernel plan --definition examples/mvk.json --output /tmp/mvk-cohort-balance.FYx63L/target4.json --time-limit PT30S --seed 0`: `FEASIBLE`, catalog 4, 520 assignments, 6B daily counts `5,5,3,3,3` in Monday-to-Friday order (spread 2), 9C spread 1, zero cohort gaps, two late starts, and four weekly-balance matches. An independent tally of the same assignments also gives four matches. The baseline had 6B counts `2,5,5,3,4`, 9C spread 5, and fourteen matches at target 2. Three cohorts in the updated run still have spread 3.
- Packaged `verify` of that MVK definition/result pair: `VERIFIED`; `git diff --check`: PASS. Tests left no additional tracked changes.

| Contract element | Evidence |
|---|---|
| UC-1 main steps 1-4 and success postcondition | `SchoolQualityCliIT.catalogFourPackagedPlanningAndRepairUseTheCohortDailySpread:198`; MVK packaged plan/result and independent cohort-count tally |
| UC-1 extensions 1a and 1c | `SchoolQualityCliIT.catalogFourPackagedPlanningAndRepairUseTheCohortDailySpread:198`; `WorkspaceRepairDraftIT.compilesCohortDailySpreadWithoutChangingAcceptedPair:128`; `WorkspaceRepairPlanningIT.createsVerifiedRepairProposalWithoutReplacingAcceptedBaseline:93` |
| UC-1 extension 1b | `DefinitionValidatorTest.catalogFourCohortDailySpreadDefaultsToOneAndPreservesAnExplicitValue:50` |
| UC-1 extension 1d and G3 | `ContractTest.cohortDailySpreadSchemaAndRevision:51`; existing packaged catalog 1-3 plan/verify cases in `PlanCliIT`, `VerifyCliIT`, and `SchoolQualityCliIT` |
| UC-1 extension 2a and minimal guarantee | `SchoolQualityCliIT.catalogFourRejectsInvalidDailySpreadWithoutPublishingATimetable:261`; `PlanCliIT.interruptionDoesNotPublish:291` |
| UC-1 extension 2b | Zero-weight branch in `SchoolQualityCliIT.catalogFourPackagedPlanningAndRepairUseTheCohortDailySpread:250` |
| UC-1 extension 2c | Unsuccessful-search branch in `SchoolQualityCliIT.catalogFourRejectsInvalidDailySpreadWithoutPublishingATimetable:282` |
| UC-1 extension 3a and G5 | MVK 30-second `FEASIBLE` result: score has four remaining matches and three cohorts with spread 3; no optimality claim |
| UC-1 G1 and G2 | `SchoolQualityConstraintTest.cohortSpecificDailySpreadChangesTheExactWeeklyBalanceMatches:132` and `incrementalScoreRecognizesMeetingTheConfiguredDailySpread:155`; packaged MVK `verify` and independent tally |
| UC-1 G4 | `PlanningMapper.java:50`, `SchoolConstraintProvider.java:187`, packaged replan's zero period/room moves, and existing solver priority tests |
| RULE-1 | `school-definition-v1.schema.json:78`, `KernelCatalog.java:12`, `ContractTest.java:51`, `RevisionService.definitionRevision` |
| RULE-2 | `SchoolConstraintProvider.java:180`, `ScheduleEvaluator.java:103`, `SchoolQualityConstraintTest.java:132`, unchanged conservative bound in `DefinitionValidator.java:313` |
| RULE-3 | `RepairDraftService.java:188`, `WorkspaceRepairDraftIT.java:128`, `WorkspaceRepairPlanningIT.java:121`, packaged catalog 3 to 4 repair |
| RULE-4 | Kernel and Docker-backed workspace suites above, MVK plan and verify, `git diff --check` |

## UC-1 Revision Evidence

- Started: 2026-09-26 21:17 UTC.
- Started from: `c4e9c8ddf31e081fcc3c555534715022019b09d0`.
- Pre-existing dirty files: none.
- Prior convergence: the approved target-two configuration in `444d69c` was superseded by the user's tighter balance request; its README-only K-1 note was corrected in `7dcb956`.
- Implementation submission: HEAD at convergence, with revised `checkpoints/UC-1.md`.
- Changed files: `README.md`, `examples/mvk.json`, `kernel-cli/src/test/java/org/schoolkernel/contract/ContractTest.java`, this status file, and the revised checkpoint. The proposal, specification, and rules were updated in base commit `c4e9c8d`.
- Focused `./mvnw -q -pl kernel-cli -am -Dtest=ContractTest -Dsurefire.failIfNoSpecifiedTests=false test`: PASS, including exact MVK values.
- Full `./mvnw -q -pl kernel-cli -am verify`: 69 unit and 42 packaged CLI tests, zero failures/errors/skips.
- Affected `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceImportIT,WorkspaceRepairDraftIT,WorkspaceRepairPlanningIT -Dfailsafe.failIfNoSpecifiedTests=false verify`: 24 import, 11 repair-draft, and 10 repair-planning Docker-backed tests, zero failures/errors/skips.
- Packaged `./school-kernel plan --definition examples/mvk.json --output /tmp/mvk-balance-JMwsNc/final-result.json --time-limit PT30S --seed 0`: `FEASIBLE` / `TIME_LIMIT`, catalog 4, 520 assignments, seven score rows; 23/23 cohorts have daily spread 0-1, with zero weekly-balance matches and zero late-start matches. 6B counts are `4,4,4,3,4` with first regular slots `3,1,3,1,3`.
- Independent inspection of assignments reproduced four cohort gaps, seven teacher gaps, zero late starts, and 76 non-preferred rooms. The maximum first-start spread is two slots; no exact first-start-spread preference exists in this data-only revision.
- Packaged `verify` of the same definition/result pair: `VERIFIED`; `git diff --check`: PASS; tests left no additional tracked changes.

| Revised contract element | Evidence |
|---|---|
| UC-1 main steps 1-4 and G5 | `ContractTest.mvkHasExactBalancedPreferenceConfiguration:81`; packaged MVK plan/result; independent 23-cohort daily-count, first-start, gap, and preferred-room tally |
| UC-1 extension 3a | `SchoolQualityCliIT.catalogFourPackagedPlanningAndRepairUseTheCohortDailySpread:245` retains bounded `FEASIBLE` output with remaining matches; revised MVK run is time-limited without an optimality claim |
| UC-1 G1/G2 and success postcondition | Zero independently tallied balance matches, 520 complete assignments, packaged `VERIFIED` result |
| RULE-4 | Exact fixture test, normal 30-second packaged plan and verify, full kernel and affected workspace regression suites, `git diff --check` |

## Blockers

None.

## Deviations

None.
