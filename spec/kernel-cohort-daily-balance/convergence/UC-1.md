# Convergence: UC-1 - Obtain a cohort-balanced timetable proposal

## Summary

- Submission: `spec/kernel-cohort-daily-balance/checkpoints/UC-1.md`, committed in `1fea4f327108801f93f0103523366f357c21300d` against `b207e88b06ff1895e6496cd42af49191a375aa29`.
- Verdict: **APPROVE WITH NOTES**.
- Findings: 0 CRITICAL, 0 GAP, 0 PROTOCOL, 0 DRIFT, 1 COSMETIC.
- Suite: focused 23 unit + 9 packaged CLI tests; full relevant kernel 68 unit + 42 packaged CLI tests; affected workspace 45 Docker-backed integration tests. Each run had 0 failures, 0 errors, and 0 skips.
- Working tree impact from verification: none; `git status --short` was empty before and after the runs, and `git diff --check` passed. This report and its status update are the verifier's only changes.

## Protocol Gate

1. UC-1 alone was `READY_FOR_CONVERGENCE` in `status.md`; the feature has no other UCs.
2. The checkpoint and implementation are together in immutable commit `1fea4f3`; the base is `b207e88`. The worktree was clean at handoff.
3. UC-1 has no Requires, Includes, or Extends dependencies, and no other UC is active.
4. The checkpoint has separate rows for four main steps, eight extensions, five guarantees, both postconditions, all four rules, validation commands, changed files, and legacy/workspace regression.
5. I inspected the 29-file submitted diff, the full detailed specification and rules, relevant production paths and assertions, and the packaged runtime result. The diff is attributable to this feature; no unrelated pre-existing changes or later-UC implementation were found.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator, packaged CLI | Plan MVK for 30 seconds, seed 0 | `FEASIBLE`, catalog 4, 520 assignments | Fresh `converge.json`: `FEASIBLE` / `TIME_LIMIT`, 520 of 520 lessons assigned; seven score rows, weight 4, four balance matches, penalty 16, zero reserved-period assignments. |
| Administrator, reading candidate | Inspect cohort loads | 6B spread 2; three cohorts spread 3 | Independent tally of every assignment gives 6B `5,5,3,3,3` versus baseline `2,5,5,3,4`; 20 of 23 cohorts meet spread 2. 5B, 6A, and 9B have spread 3; pairwise excess totals four, exactly the reported match count. |
| Administrator, packaged CLI | Verify the definition/result pair | `VERIFIED` | `converge-verification.json` reports `VERIFIED`, catalog 4. |
| Administrator, packaged CLI | Catalog 3 predecessor to catalog 4 repair; invalid and infeasible branches | Feasible successor; failures have no timetable | `SchoolQualityCliIT.java:198-294` drove the packaged JAR through plan, verify, replan, negative/fractional/legacy-field rejection, zero weight, and infeasible search; 9/9 focused tests passed. |
| Administrator, workspace HTTP | Import and repair | Catalog 4 setting survives; old accepted pair remains | `WorkspaceImportIT.java:143-156`, `WorkspaceRepairDraftIT.java:126-143`, and `WorkspaceRepairPlanningIT.java:92-135` exercised HTTP/PostgreSQL and packaged repair. The three suites passed 45/45 with Docker. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1: submit catalog 4 target | MVK and packaged fixture use target 2 | `examples/mvk.json:1-10,494-633` declares 23 targets; packaged plan in `SchoolQualityCliIT.java:198-204` | STRONG | yes |
| Main 2: validate, search, prefer lower weighted excess | Feasible result; target reaches solver | `DefinitionLoader.java:72-101`; `PlanningMapper.java:43-50,62-69`; `SchoolConstraintProvider.java:180-189`; packaged result `FEASIBLE` | STRONG | yes |
| Main 3: publish candidate, weights and penalty | Seven rows, score and effective weights | `converge.json` has catalog 4, 520 assignments, seven rows, balance matches 4, weight 4, aggregate penalty 16; `SchoolQualityCliIT.java:205-214` | STRONG | yes |
| Main 4: inspect cohort counts and remaining excess | 6B improves; excess remains visible | Independent weekday tally of all 23 cohorts from result assignments and definition; 6B spread 2, three spread 3, total four matches | STRONG | yes |
| Extension 1a: verified-predecessor repair applies successor target, retains predecessor | Packaged replan and workspace copy | `SchoolQualityCliIT.java:216-243` asserts catalog 3 predecessor bytes unchanged and catalog 4 proposal; workspace HTTP and accepted JSON equality at `WorkspaceRepairDraftIT.java:126-143` | STRONG | yes |
| Extension 1b: omission means one | Validator default | `DefinitionValidator.java:432-436`; `DefinitionValidatorTest.java:49-59` asserts omitted 1 and explicit 2 | STRONG | yes |
| Extension 1c: older predecessor stays verifiable; workspace successor is 4 | Legacy repair retains accepted pair | `SchoolQualityCliIT.java:216-243`; `WorkspaceRepairPlanningIT.java:92-135` asserts successor/result catalog 4, revisions and accepted-pair equality; catalog 1-3 regressions passed | STRONG | yes |
| Extension 1d: direct legacy plan/verify retains meaning | Old field rejected, rows retained | `ContractTest.java:27-74`; `KernelCatalogTest.java:18-28`; packaged `PlanCliIT`, `VerifyCliIT`, and `SchoolQualityCliIT` regression (42/42) | STRONG | yes |
| Extension 2a: invalid target rejected before search, no candidate | Three invalid variants | `SchoolQualityCliIT.java:260-280` asserts exit 2, `INVALID_INPUT`, no timetable; `DefinitionLoader.java:72-78` rejects schema before `PlanService.java:94` solves | STRONG | yes |
| Extension 2b: zero weight reports matches but no balance penalty | Four matches, zero penalty | Packaged case `SchoolQualityCliIT.java:245-257` asserts exact matches and aggregate penalty | STRONG | yes |
| Extension 2c: no feasible candidate, accepted predecessor preserved | No timetable, no accepted mutation | Packaged `SchoolQualityCliIT.java:282-294` asserts exit 3 and no timetable; workspace HTTP failure regression `WorkspaceRepairPlanningIT.java:384-419` retains accepted JSON | STRONG | yes |
| Extension 3a: bounded feasible excess is not optimality | Three cohorts remain above target | Fresh MVK result is `FEASIBLE` / `TIME_LIMIT`, with four matches and no optimality claim; independent tally identifies three above-target cohorts | STRONG | yes |
| G1: exact pairwise formula and available zero days | Cohort-specific formula | `SchoolConstraintProvider.java:302-320`; `SchoolQualityConstraintTest.java:131-151` covers targets 0/2, zero-lesson and unavailable Wednesday; MVK tally reproduces four | STRONG | yes |
| G2: solver and published match count agree | Incremental and evaluator tests | `SchoolQualityConstraintTest.java:154-170`, `ScheduleEvaluator.java:103-105`, packaged verify and independent MVK tally all agree | STRONG | yes |
| G3: catalog 4 has seven rows; 1-3 unchanged | Versioned rows/revisions | `KernelCatalog.java:92-121`; `KernelCatalogTest.java:14-50`; `ContractTest.java:51-74`; packaged legacy regression and catalog 4 verification | STRONG | yes |
| G4: hard and stability scores outrank ordinary balance | Score levels and zero-move repair | `SchoolConstraintProvider.java:19-22,180-189`; `PlanningMapper.java:62-69`; `SchoolQualityCliIT.java:229-243` asserts no period/room move for target change | STRONG | yes |
| G5: MVK all target 2, weight 4, assessed at normal limit | 6B spread improves | `examples/mvk.json:1-10,494-633`; fresh 30-second run and independent 23-cohort tally; remaining excess is reported, not hidden | STRONG | yes |
| Success postcondition: complete feasible verifiable candidate with penalty | MVK plan/verify | 520 assignments for 520 lessons, `FEASIBLE`, balance penalty 16; packaged verification `VERIFIED`; no acceptance requested | STRONG | yes |
| Minimal guarantee: failed/interrupted plan has no candidate; accepted pair preserved | CLI and workspace negative cases | `SchoolQualityCliIT.java:260-294`, `PlanCliIT.java:289-305`, `WorkspaceRepairPlanningIT.java:384-419` and its accepted-pair equality assertions | STRONG | yes |
| Relationships | None | `spec.md` UC-1 Relations lists none; no dependency gate or later-UC behavior | STRONG | yes |
| RULE-1 | Version, placement, default, legacy, revisions and rows | Schema `school-definition-v1.schema.json:74-79,143-150`; `ContractTest.java:51-74`; `KernelCatalogTest.java:14-28` | STRONG | yes |
| RULE-2 | Exact solver/evaluator score and 64-bit bound | `SchoolConstraintProvider.java:180-189,291-320`; `ScheduleEvaluator.java:103-114`; `DefinitionValidator.java:308-334`; exact unit and MVK independent tally | STRONG | yes |
| RULE-3 | Repair priority and immutable lineage | `SchoolConstraintProvider.java:19-22`; `RepairDraftService.java:182-212`; packaged replan and workspace accepted-pair assertions | STRONG | yes |
| RULE-4 | Packaged actor and full regression evidence | Packaged plan/verify, invalid/infeasible tests, 30-second MVK run, 68+42 kernel and 45 workspace tests, `git diff --check` | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | “Catalog version 4 MUST accept a non-negative integer `maxDailyLessonSpread` only on cohort entries, default its omission to one, and reject the field under catalogs 1 through 3.” Also revisions and prior catalog rows/semantics MUST be retained. | Schema and revision assertions at `ContractTest.java:51-74`; validator default at `DefinitionValidatorTest.java:49-59`; exact catalog order at `KernelCatalogTest.java:14-50`; packaged legacy cases | PASS |
| RULE-2 | “The Timefold constraint and independent evaluator MUST use each cohort's validated target and the exact UC-1 G1 pairwise formula” and stay within signed 64-bit weighted bounds. | `PlanningMapper.java:43-50`; `SchoolConstraintProvider.java:180-189,291-320`; `ScheduleEvaluator.java:103-114`; conservative 21-times-lessons bound at `DefinitionValidator.java:308-334`; exact/incremental tests and independent runtime tally | PASS |
| RULE-3 | Repair MUST preserve score priority, catalog 4 targets, successor upgrade, and accepted definition/result immutability. | Score levels at `SchoolConstraintProvider.java:19-22`; `RepairDraftService.java:182-212`; packaged `SchoolQualityCliIT.java:216-243`; HTTP workspace `WorkspaceRepairDraftIT.java:126-143` and `WorkspaceRepairPlanningIT.java:92-135` | PASS |
| RULE-4 | Packaged candidate/failure boundary, normal-limit MVK and independent counts, complete kernel and affected workspace suites MUST pass. | Fresh packaged MVK result and `VERIFIED` file; independent counts; focused 23+9, full 68+42, workspace 45, no failures/errors/skips; `git diff --check` | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| No related UC in this feature | No declared Requires/Includes/Extends | Sole UC in `spec.md`; protocol gate satisfied | N/A |
| Existing catalogs 1-3 | Shared schema, catalog, plan/replan/verify and score rows | Full 42 packaged CLI tests, including legacy and catalog 3 cases; exact catalog unit test | PASS |
| Existing workspace import/repair | Shared accepted lineage, transitions and packaged kernel | `WorkspaceImportIT` 24, `WorkspaceRepairDraftIT` 11, `WorkspaceRepairPlanningIT` 10; no failures/errors/skips | PASS |

## Verification Categories

- Lifecycle/data: unchanged initial import -> `INITIAL_DRAFT`, accepted -> `REPAIR_DRAFT` -> `SOLVING_REPAIR` -> `REPAIR_PROPOSAL` path was exercised at `WorkspaceImportIT.java:143-156` and `WorkspaceRepairPlanningIT.java:92-135`; unsuccessful repair retained the accepted JSON at `WorkspaceRepairPlanningIT.java:384-419`. No new acceptance transition or persistence migration was introduced. The catalog 4 target is retained by value and revisions reflect it.
- Security: no route or access-policy files changed. `SecurityConfiguration.java:25-35` still enumerates the GET/POST/PATCH/DELETE matrix and denies every other request; CSRF and the existing Host/Origin filter remain in place. CLI paths require local files. There are no login, role, or owner concepts to retest for this feature.
- Presentation: this is a CLI/data-contract UC; no UI change or human walkthrough is required. The administrator can derive per-cohort daily counts from the published assignments and input definition, as independently reproduced above.
- Test hygiene: no new disabled tests, architecture exemptions, migrations, or tracked generated data in the diff. Tests use isolated temp paths and Docker-backed PostgreSQL where applicable. The deliberate connection-termination log in `WorkspaceImportIT` did not fail the suite.

## Findings

### COSMETIC

- **K-1 — Stale repair catalog sentence.** RULE-3 says workspace compilation “MUST ... upgrade older accepted catalogs to a version 4 successor.” `RepairDraftService.java:182-189` and `WorkspaceRepairPlanningIT.java:121-124` do so, but `README.md:65` still says “catalog `3` successor.” This is documentation-only and cannot mis-score or change accepted state. Revision outcome: change that sentence to catalog `4` in a separate follow-up after this immutable verdict.

No critical, gap, protocol, or drift findings.

## Status Update

UC-1: `READY_FOR_CONVERGENCE` -> `APPROVED`. Current remains UC-1; no further UC is eligible in this one-UC feature. The status row links to this report.

## Response to execute

APPROVED WITH NOTES: K-1 is a documentation-only repair catalog version correction.
