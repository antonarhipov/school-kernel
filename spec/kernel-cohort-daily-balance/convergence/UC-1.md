# Convergence: UC-1 - Obtain a cohort-balanced timetable proposal

## Summary

- Submission: spec/kernel-cohort-daily-balance/checkpoints/UC-1.md in commit 208ec69, based on c4e9c8d. The earlier approval in commit 444d69c covered the superseded target-two configuration.
- Verdict: APPROVE against the revised data-only UC-1 contract.
- Findings: 0 CRITICAL, 0 GAP, 0 PROTOCOL, 0 DRIFT, 0 COSMETIC.
- Suite: focused ContractTest passed; full kernel 69 unit and 42 packaged CLI tests passed; affected Docker-backed workspace 45 integration tests passed. Each completed run had 0 failures, 0 errors, and 0 skips.
- Working tree impact from verification: none. Git status was clean before and after tests and the packaged run. The sandboxed workspace attempt stopped at Docker initialization; the identical command passed with Docker socket access.
- Scope limit: the result has no starts after regular slot 3, but first-lesson times are not equalized within slots 1-3. The specification expressly excludes a first-start-spread score, so this approval does not claim that part of the user's broader goal is achieved.

## Protocol Gate

1. UC-1 was the only use case and its status was READY_FOR_CONVERGENCE before verification.
2. The checkpoint and all five implementation files are committed together in 208ec69; the declared base is c4e9c8d. Git status was clean.
3. UC-1 declares no Requires, Includes, or Extends relation; no other use case is active in this feature.
4. The checkpoint has rows for four main steps, eight extensions, five guarantees, both postconditions, all four rules, commands, changed files, and legacy/workspace regression.
5. I inspected the full five-file submission diff, specification, rules, cited assertions, scoring code, and runtime results. The diff is confined to the MVK fixture, its exact-value test, README, status, and checkpoint; no unrelated or later-UC implementation was included.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator, packaged CLI | Plan MVK for 30 seconds with seed 0 | FEASIBLE / TIME_LIMIT; 520 assignments; all cohort load spreads 0-1 | Fresh /tmp/mvk-converge-BNtRht/result.json is FEASIBLE / TIME_LIMIT, catalog 4, with 520 distinct lesson IDs for 520 definition lessons and seven preference rows. |
| Administrator, inspecting candidate | Compare daily counts and first lessons | 6B counts 4,4,4,3,4; starts 3,1,3,1,3 | Independently tallied 3 cohorts at load spread 0 and 20 at spread 1. 6B is 4,4,4,3,4 with starts 1,1,3,1,3. Four cohorts have first-start spread 1 and 19 have spread 2; no start is after slot 3. The difference from the executor's starts is a time-limited search result, not a contract failure. |
| Administrator, inspecting score | Balance, gaps, starts, rooms | Zero weekly-balance and late-start matches; four cohort gaps, seven teacher gaps, 76 non-preferred rooms | Fresh score reports balance 0 at weight 30, late starts 0 at weight 30, cohort gaps 3, teacher gaps 4, non-preferred rooms 82. Independent assignment and preferred-room tally reproduces those counts. |
| Administrator, packaged CLI | Verify the same definition/result pair | VERIFIED | /tmp/mvk-converge-BNtRht/verification.json reports VERIFIED, catalog 4. |
| Administrator, packaged CLI | Legacy repair, invalid and unsuccessful branches | Packaged CLI tests passed | SchoolQualityCliIT.java:198-294 drives the packaged process through plan, verify, replan, invalid targets, zero weight, and no feasible result; the full 42 packaged CLI tests passed. |
| Administrator, workspace HTTP | Import and repair retain accepted authority | Docker-backed integration passed | WorkspaceImportIT, WorkspaceRepairDraftIT, and WorkspaceRepairPlanningIT passed 24, 11, and 10 HTTP/PostgreSQL cases respectively when Docker was accessible. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1: submit catalog 4 target | MVK gives all 23 cohorts target 1 and five overrides | examples/mvk.json:1-13,497-636; ContractTest.java:79-107 compares all IDs, targets, overrides, and reservations; fresh packaged plan accepted it | STRONG | yes |
| Main 2: validate and search using weighted preferences | Target reaches Timefold and candidate is feasible | DefinitionLoader.java:72-101, PlanningMapper.java:43-50, SchoolConstraintProvider.java:180-189; fresh packaged FEASIBLE result | STRONG | yes |
| Main 3: publish candidate, weights and penalty | Seven rows, zero balance matches at weight 30 | Fresh result has seven rows, effective balance weight 30, match count 0, and aggregate balance penalty 0; SchoolQualityCliIT.java:205-214 covers catalog and row shape | STRONG | yes |
| Main 4: inspect counts, first lessons and remaining excess | All 23 targets met and no late starts | Independent tally of all assignments gives all 23 daily counts, first slots 1-3, zero load excess, and 6B 4,4,4,3,4 / 1,1,3,1,3 | STRONG | yes |
| Extension 1a: verified-predecessor repair | Successor target applies; predecessor unchanged | SchoolQualityCliIT.java:216-243 checks packaged replan and unchanged predecessor bytes; WorkspaceRepairDraftIT.java:126-143 checks accepted JSON equality | STRONG | yes |
| Extension 1b: omitted target means one | Validator default | DefinitionValidator.java:432-436 and DefinitionValidatorTest.java:49-59 | STRONG | yes |
| Extension 1c: legacy predecessor remains verifiable; successor is 4 | Packaged and workspace lineage | SchoolQualityCliIT.java:216-243 and WorkspaceRepairPlanningIT.java:92-135 assert catalog 4 successor/result and accepted-pair equality; legacy CLI regression passed | STRONG | yes |
| Extension 1d: direct legacy plan/verify retains meaning | Prior rows and field rejection | ContractTest.java:51-76, KernelCatalogTest.java:14-50, and full packaged PlanCliIT, ReplanCliIT, VerifyCliIT regression | STRONG | yes |
| Extension 2a: invalid target, no search or timetable | Negative, fractional, legacy-field cases | SchoolQualityCliIT.java:260-280 asserts INVALID_INPUT, no timetable, and no solver-start log; DefinitionLoader.java:72-78 rejects before PlanService.java:94 | STRONG | yes |
| Extension 2b: zero balance weight | Matches reported, penalty zero | SchoolQualityCliIT.java:245-257 asserts four matches with zero aggregate balance penalty through packaged CLI | STRONG | yes |
| Extension 2c: no feasible candidate | No timetable; accepted predecessor preserved | SchoolQualityCliIT.java:282-294 asserts NO_FEASIBLE_SOLUTION_FOUND with no timetable; WorkspaceRepairPlanningIT.java:384-419 checks accepted JSON after failure | STRONG | yes |
| Extension 3a: bounded feasible excess is not optimality | Fixture with four remaining matches | SchoolQualityCliIT.java:245-257 covers a FEASIBLE result with excess; fresh MVK result says TIME_LIMIT, not optimality | STRONG | yes |
| G1: pairwise excess over available days, including zeros | Target-specific formula | SchoolConstraintProvider.java:302-320 and SchoolQualityConstraintTest.java:131-151 cover targets 0/2, unavailable day and zero-lesson available day; fresh MVK daily counts independently imply zero excess | STRONG | yes |
| G2: solver and evaluator agree | Incremental and independent evaluation | SchoolQualityConstraintTest.java:154-170, ScheduleEvaluator.java:103-105, packaged VERIFIED result, and independent MVK tally | STRONG | yes |
| G3: catalog 4 has seven rows; legacy unchanged | Versioned schema, rows and revisions | KernelCatalog.java:92-121; ContractTest.java:51-76; KernelCatalogTest.java:14-50; fresh seven-row result and legacy packaged regression | STRONG | yes |
| G4: hard/stability priorities outrank balance | Score level and no extra repair moves | SchoolConstraintProvider.java:19-22,180-189; PlanningMapper.java:62-69; SchoolQualityCliIT.java:229-243 checks zero period/room moves for target change | STRONG | yes |
| G5: exact MVK configuration and normal-limit inspection | 23 target-one cohorts and five weights | ContractTest.java:79-107; fresh packaged 30-second result and independent count, first-start, gap, and room tally; no global-optimum claim | STRONG | yes |
| Success postcondition | Complete feasible revision-verifiable candidate with balance penalty | 520 distinct assignments for 520 lessons; FEASIBLE; balance penalty 0; packaged verify reports VERIFIED; no acceptance action | STRONG | yes |
| Minimal guarantee | Failed/interrupted search publishes no candidate, preserves accepted pair | SchoolQualityCliIT.java:260-294, PlanCliIT.java:289-305, and WorkspaceRepairPlanningIT.java:384-419 | STRONG | yes |
| Relationships | None | UC-1 relations in spec.md declare none; no dependency or later-UC behavior | STRONG | yes |
| RULE-1 | Versioned cohort setting | Schema, validator, revision, catalog, and packaged legacy assertions cited below | STRONG | yes |
| RULE-2 | Exact scoring and signed 64-bit bounds | Constraint/evaluator code, exact and incremental tests, conservative bound, fresh runtime tally | STRONG | yes |
| RULE-3 | Repair lineage and priority | Score levels, workspace compiler, packaged replan, accepted-pair assertions | STRONG | yes |
| RULE-4 | Actor-boundary and regression evidence | Fresh plan/verify, negative packaged cases, 69+42 kernel and 45 Docker-backed workspace cases, git diff --check | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Catalog 4 MUST accept only a non-negative integer cohort target, default omission to one, reject the field in catalogs 1-3, include it in revisions, and retain old meanings and seven catalog-4 rows. | school-definition-v1.schema.json:74-79,143-150; DefinitionValidatorTest.java:49-59; ContractTest.java:51-76; KernelCatalogTest.java:14-50; packaged legacy tests | PASS |
| RULE-2 | Timefold and evaluator MUST use the cohort's target and exact G1 formula, with signed 64-bit weighted bounds. | PlanningMapper.java:43-50; SchoolConstraintProvider.java:180-189,291-320; ScheduleEvaluator.java:103-114; DefinitionValidator.java:308-334; SchoolQualityConstraintTest.java:131-170; independent MVK tally | PASS |
| RULE-3 | Repair MUST preserve hard, period-move, room-move and preference priority, carry targets into version-4 successors, and leave accepted pairs unchanged. | SchoolConstraintProvider.java:19-22; RepairDraftService.java:182-212; SchoolQualityCliIT.java:216-243; WorkspaceRepairDraftIT.java:126-143; WorkspaceRepairPlanningIT.java:92-135 | PASS |
| RULE-4 | Packaged boundaries MUST demonstrate feasible and failed outcomes; MVK MUST match exact configuration and be independently inspected at its normal limit; complete affected suites MUST pass. | ContractTest.java:79-107; fresh packaged plan/verify and independent tally; 69+42 kernel and 45 workspace tests all green; git diff --check | PASS |

## Related-UC Regression

| Use case | Relationship or shared surface | Evidence | Result |
|---|---|---|---|
| No related UC in this feature | UC-1 has no declared relation | Sole UC in spec.md | N/A |
| Existing catalogs 1-3 | Shared schema, plan/replan/verify, score rows | Full 42 packaged CLI tests, including legacy catalog cases, and catalog unit tests | PASS |
| Existing workspace import and repair | Accepted lineage and packaged kernel invocation | WorkspaceImportIT 24, WorkspaceRepairDraftIT 11, WorkspaceRepairPlanningIT 10 | PASS |

## Verification Categories

- Lifecycle/data: existing accepted -> repair draft -> solving repair -> proposal path was exercised through HTTP/PostgreSQL in WorkspaceRepairPlanningIT.java:92-135; unsuccessful repair retained accepted JSON at :384-419. No new acceptance transition or persisted shape was submitted.
- Security: no route or access-policy change was submitted. SecurityConfiguration.java:25-35 still enumerates the existing access matrix and denies other methods/routes; CLI uses local paths. No login, role, or owner axis applies to this data-only revision.
- Presentation: no UI is part of this UC, so no human walkthrough applies. Counts and first periods were derived directly from the published assignments; no first-start consistency guarantee was inferred.
- Test/repository hygiene: no new skip, architecture exemption, generated tracked change, or production double in the submission. The initial workspace attempt failed before test bodies because the sandbox denied Docker's Unix socket. The same command with Docker access passed 45/45. Git status was empty after verification and git diff --check passed.

## Findings

No critical, gap, protocol, drift, or cosmetic findings against the revised data-only UC-1 contract. The start-time spread remains a disclosed product limitation: the current late-start preference treats starts in regular slots 1, 2, and 3 equally. Addressing consistency among those starts requires a separately chosen scoring contract; it cannot be asserted from this configuration.

## Status Update

UC-1: READY_FOR_CONVERGENCE -> APPROVED. Current remains UC-1; no further UC exists in this feature. The status row links to this report.

## Response to execute

APPROVED
