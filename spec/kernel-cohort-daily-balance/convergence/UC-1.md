# Convergence: UC-1 - Obtain a cohort-balanced timetable proposal

## Summary

- Submission: spec/kernel-cohort-daily-balance/checkpoints/UC-1.md in 13b05dbf03c4e26a805d5a9e1796099be6dc548b, based on 11a9f1d54e9c3246b9363bcd79809e38f5d10814.
- Verdict: APPROVE against the revised data-only UC-1 contract.
- Findings: 0 CRITICAL, 0 GAP, 0 PROTOCOL, 0 DRIFT, 0 COSMETIC.
- Suite: focused ContractTest and SchoolQualityConstraintTest passed; full kernel 69 unit and 42 packaged CLI tests passed; affected Docker-backed workspace 45 integration tests passed. All completed runs had 0 failures, 0 errors, and 0 skips.
- Working tree impact from verification: none. Git status was clean before and after tests and the packaged run.

## Protocol Gate

1. UC-1 is the sole use case in this feature, and its committed status is READY_FOR_CONVERGENCE. No other use case is active.
2. The checkpoint and all six submitted files are committed together in 13b05db, with declared base 11a9f1d. Git status was clean at the start of convergence.
3. UC-1 has no Requires, Includes, or Extends dependency.
4. The checkpoint has rows for four main steps, eight extensions, five guarantees, both postconditions, all five rules, test commands, changed files, and legacy/workspace regression.
5. I inspected the full six-file submission diff, the detailed specification and rules, cited tests and production paths, and fresh runtime output. The diff changes MVK weights, exact-value and gap tests, README, status, and checkpoint only; no unrelated or later-UC behavior is present.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator, packaged CLI | Plan MVK for 30 seconds with seed 0 | FEASIBLE / TIME_LIMIT, 520 assignments, zero cohort gaps, three balance matches | Fresh /tmp/mvk-converge.DZeYTf/result.json is FEASIBLE / TIME_LIMIT, catalog 4, with 520 distinct lesson IDs for 520 definition lessons and seven ordinary-preference rows. |
| Administrator, inspecting 6B | Daily counts, first periods, internal holes | 3,4,4,4,4; starts 1,1,1,3,2; zero gaps | Independent assignment tally reproduced exactly those values. |
| Administrator, inspecting all cohorts | Load, gap, start, room, and teacher quality | Zero gaps and late starts; three cohorts exceed spread one | Independent tally gives zero internal cohort gaps, zero late starts, two balance matches from 5A and 5B at spread two, 62 teacher gaps, and 419 non-preferred rooms. The fresh reported score has exactly those counts and 4,567 aggregate ordinary penalty. The small balance difference is a permitted time-limited outcome. |
| Administrator, packaged CLI | Verify same definition/result pair | VERIFIED | Fresh /tmp/mvk-converge.DZeYTf/verification.json reports VERIFIED, catalog 4, and matching definition and timetable revisions. |
| Administrator, packaged CLI | Legacy repair, invalid, zero-weight, and unsuccessful branches | Packaged CLI suite passed | SchoolQualityCliIT.java:198-294 drives plan, verify, replan, invalid targets, zero weight, and no feasible result through the packaged process; all 42 packaged CLI tests passed. |
| Administrator, workspace HTTP | Import and repair preserve accepted authority | Docker-backed integration passed | WorkspaceImportIT, WorkspaceRepairDraftIT, and WorkspaceRepairPlanningIT passed 24, 11, and 10 HTTP/PostgreSQL cases respectively. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1: submit catalog 4 target | MVK has 23 target-one cohorts and five exact overrides | examples/mvk.json:1-13,495-635; ContractTest.java:79-105 compares IDs, targets, overrides, and reservations; fresh packaged plan accepts the definition | STRONG | yes |
| Main 2: validate and search with weighted preferences | Gap preference dominates MVK tuning and candidate is feasible | SchoolConstraintProvider.java:158-189 keeps gap and balance at ordinary-preference level; examples/mvk.json:7-12 sets gap weight 1,000,000; fresh packaged result is FEASIBLE | STRONG | yes |
| Main 3: publish candidate, effective weights and penalties | Seven rows include million-weight gap penalty | Fresh result has seven rows, gap weight 1,000,000 and zero matches/penalty, balance weight 1,000 and two matches/2,000 penalty, plus 4,567 total ordinary penalty | STRONG | yes |
| Main 4: inspect daily counts, first lessons and holes | 6B has balanced gap-free days | Independent tally of all 520 assignments covers all 23 cohorts and five weekdays; 6B is 3,4,4,4,4 / 1,1,1,3,2 with zero holes; 5A and 5B retain spread-two excess | STRONG | yes |
| Extension 1a: verified-predecessor repair | Complete successor applies target and retains predecessor | SchoolQualityCliIT.java:216-243 tests packaged replan and unchanged predecessor bytes; WorkspaceRepairDraftIT.java:126-143 checks accepted JSON equality | STRONG | yes |
| Extension 1b: omitted target means one | Validator supplies default | DefinitionValidator.java:432-436 and DefinitionValidatorTest.java:49-59 assert default and explicit values | STRONG | yes |
| Extension 1c: legacy predecessor remains verifiable; successor is 4 | Packaged and workspace lineage | SchoolQualityCliIT.java:216-243 and WorkspaceRepairPlanningIT.java:92-135 assert catalog-4 successor/result and unchanged accepted pair; legacy CLI regression passed | STRONG | yes |
| Extension 1d: direct legacy plan/verify retains meaning | Old catalog field and rows remain versioned | ContractTest.java:51-76, KernelCatalogTest.java:14-50, and full packaged PlanCliIT, ReplanCliIT, VerifyCliIT regression | STRONG | yes |
| Extension 2a: invalid target, no search or timetable | Negative, fractional, old-catalog cases rejected | SchoolQualityCliIT.java:260-280 asserts INVALID_INPUT, no timetable, and no solver-start log; DefinitionLoader rejects before search | STRONG | yes |
| Extension 2b: zero balance weight | Matches reported, aggregate penalty zero | SchoolQualityCliIT.java:245-257 asserts four matches and zero aggregate balance penalty through packaged CLI | STRONG | yes |
| Extension 2c: no feasible candidate | No timetable; accepted predecessor preserved | SchoolQualityCliIT.java:282-294 asserts NO_FEASIBLE_SOLUTION_FOUND without timetable; WorkspaceRepairPlanningIT.java:384-419 checks accepted JSON after failure | STRONG | yes |
| Extension 3a: bounded feasible excess is not optimality | Feasible output may retain matches | Packaged CLI fixture at SchoolQualityCliIT.java:245-257 retains four matches; fresh MVK result is FEASIBLE / TIME_LIMIT with two matches and no optimality claim | STRONG | yes |
| G1: pairwise excess over available days, including zeros | Cohort-specific exact formula | SchoolConstraintProvider.java:291-320 and SchoolQualityConstraintTest.java:143-162 cover targets zero/two, unavailable and zero-lesson days; independent MVK tally gives two matches | STRONG | yes |
| G2: solver and evaluator agree | Incremental and published scores agree | SchoolQualityConstraintTest.java:166-181, ScheduleEvaluator.java:103-105, packaged VERIFIED result, and independent fresh score tally | STRONG | yes |
| G3: catalog 4 has seven rows; legacy unchanged | Versioned input and result contract | KernelCatalog.java:92-121, ContractTest.java:51-76, KernelCatalogTest.java:14-50, fresh seven-row result, and legacy packaged regression | STRONG | yes |
| G4: hard/stability priorities outrank preferences | Target change cannot justify extra moves | SchoolConstraintProvider.java:19-22,180-189; PlanningMapper.java:62-69; SchoolQualityCliIT.java:229-243 checks zero period/room moves for target change | STRONG | yes |
| G5: exact MVK gap priority and normal-limit inspection | Million-weight gap, higher late-start and balance weights | ContractTest.java:79-105 pins exact configuration; SchoolQualityConstraintTest.java:36-60 proves one internal available hole costs 1,000,000 and leading/trailing or reserved holes cost zero; fresh 30-second plan and independent all-cohort tally | STRONG | yes |
| Success postcondition | Complete feasible, revision-verifiable candidate | 520 distinct assignments for 520 lessons; FEASIBLE, balance penalty 2,000; packaged verify is VERIFIED; no acceptance action | STRONG | yes |
| Minimal guarantee | Failed/interrupted planning has no candidate and retains accepted pair | SchoolQualityCliIT.java:260-294, PlanCliIT.java:289-305, and WorkspaceRepairPlanningIT.java:384-419 | STRONG | yes |
| Relationships | None | UC-1 declares no dependency or extension relation | STRONG | yes |
| RULE-1 | Versioned cohort setting | Schema, validator, revision, catalog, and packaged legacy assertions cited below | STRONG | yes |
| RULE-2 | Exact scoring and signed 64-bit bounds | Constraint/evaluator code, exact and incremental tests, conservative bound, fresh runtime tally | STRONG | yes |
| RULE-3 | Repair lineage and priority | Score levels, workspace compiler, packaged replan, accepted-pair assertions | STRONG | yes |
| RULE-4 | Actor-boundary and regression evidence | Fresh plan/verify, negative packaged cases, 111 kernel and 45 Docker-backed workspace cases, git diff --check | STRONG | yes |
| RULE-5 | MVK gap priority | Exact weights test, internal/outside gap scoring tests, fresh zero-gap plan and independent score tally | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Catalog 4 MUST accept only non-negative integer cohort targets, default omission to one, reject the field in catalogs 1-3, include it in revisions, and retain old meanings and seven catalog-4 rows. | school-definition-v1.schema.json:74-79,143-150; DefinitionValidatorTest.java:49-59; ContractTest.java:51-76; KernelCatalogTest.java:14-50; packaged legacy tests | PASS |
| RULE-2 | Timefold and evaluator MUST use each cohort target and exact G1 formula, including available zero-lesson days, with signed 64-bit weighted bounds. | PlanningMapper.java:43-50; SchoolConstraintProvider.java:180-189,291-320; ScheduleEvaluator.java:103-114; DefinitionValidator.java:308-334; SchoolQualityConstraintTest.java:143-181; fresh tally | PASS |
| RULE-3 | Repair MUST preserve hard, period-move, room-move and preference priority, carry catalog-4 targets into successors, and leave accepted pairs unchanged. | SchoolConstraintProvider.java:19-22; RepairDraftService.java:182-212; SchoolQualityCliIT.java:216-243; WorkspaceRepairDraftIT.java:126-143; WorkspaceRepairPlanningIT.java:92-135 | PASS |
| RULE-4 | Packaged boundaries MUST demonstrate feasible and failed outcomes; MVK MUST match exact configuration and be inspected at its normal limit; complete affected suites MUST pass. | ContractTest.java:79-105; fresh packaged plan/verify and independent count/start/gap/room tally; 111 kernel and 45 workspace tests green; git diff --check | PASS |
| RULE-5 | MVK MUST weight cohort gaps at 1,000,000, late starts at 10,000, and balance at 1,000; each internal available hole MUST contribute one match and 1,000,000 penalty, outside holes MUST NOT count, and the score row MUST remain ordinary preference. | examples/mvk.json:7-12; ContractTest.java:79-105; SchoolQualityConstraintTest.java:36-60; SchoolConstraintProvider.java:158-166; fresh score has ordinary cohort-gap row at weight 1,000,000 and independently tallied zero matches | PASS |

## Related-UC Regression

| Use case | Relationship or shared surface | Evidence | Result |
|---|---|---|---|
| No related UC in this feature | UC-1 has no declared relation | Sole UC in spec.md | N/A |
| Existing catalogs 1-3 | Shared schema, plan/replan/verify, score rows | Full 42 packaged CLI tests, including legacy catalog cases, and catalog unit tests | PASS |
| Existing workspace import and repair | Accepted lineage and packaged kernel invocation | WorkspaceImportIT 24, WorkspaceRepairDraftIT 11, WorkspaceRepairPlanningIT 10 | PASS |

## Verification Categories

- Lifecycle/data: existing accepted -> repair draft -> solving repair -> proposal path is exercised through HTTP/PostgreSQL in WorkspaceRepairPlanningIT.java:92-135; unsuccessful repair retains accepted JSON at :384-419. No new acceptance transition or persisted shape was submitted.
- Security: no route or access-policy change was submitted. SecurityConfiguration.java:25-35 retains the existing access matrix and denies other routes/methods; the CLI uses local paths. No login, role, or owner axis applies to this data-only revision.
- Presentation: no UI is part of this UC, so no human walkthrough applies. Counts, starts, and holes were derived from published assignments; no first-start consistency guarantee was inferred.
- Test/repository hygiene: no new skip, architecture exemption, tracked generated change, or production double in the submission. Git status was empty before and after verification; git diff --check passed.

## Findings

No critical, gap, protocol, drift, or cosmetic findings. Gap avoidance, daily spread, and late starts remain soft preferences. The fresh bounded run leaves 5A and 5B at spread two and 419 non-preferred rooms; it does not claim a global optimum. First starts within regular slots 1-3 still vary because the existing preference does not compare them.

## Status Update

UC-1: READY_FOR_CONVERGENCE -> APPROVED. Current remains UC-1; no further UC exists in this feature. The status row links to this report.

## Response to execute

APPROVED
