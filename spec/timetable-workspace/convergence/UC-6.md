# Convergence: UC-6 - Decide whether a proposal becomes current

## Summary

- Submission: revised `spec/timetable-workspace/checkpoints/UC-6.md` at `844b3d13a3fae36d96a30a90427a95fa36825a15`
- Verdict: PENDING WALKTHROUGH
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic; prior C-1 resolved
- Suite: focused legacy PostgreSQL/HTTP/Chrome 2/0/0/0; clean reactor 165/0/0/0 (run/failures/errors/skipped)
- Working tree impact from verification: none before convergence artifacts

## Protocol Gate

1. Exactly one target is ready: status named only UC-6 as `READY_FOR_CONVERGENCE`; UC-8 is `NOT_STARTED` and no other UC was active.
2. The revised checkpoint and C-1 implementation are committed together at immutable submission `844b3d1`, based on rejection checkpoint `129727a`; original submission was `f02223d`.
3. Required UC-5 is `APPROVED`; its production packaged repair proposal is consumed directly by the UC-6 browser and HTTP journeys.
4. No other use case is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`.
5. The checkpoint has rows for the main scenario, every extension and guarantee, both postconditions, Requires UC-5, RULE-1/2 and all 14 UC-6-specific rules, commands, changed files, and UC-1 through UC-5 regression.
6. Every file in the base-to-submission diff was inspected. The diff is attributable to proposal review/decision, accepted-manifest pin provenance, subsequent-repair cleanup, tests, and checkpoint evidence; it contains no UC-7/UC-8 behavior or unrelated user work.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator in real Chrome | Open an already persisted UC-5 repair proposal after installing revised UC-6 | Proposal impact review opens | Independent Playwright against the packaged `844b3d1` build and existing Merivälja Kool volume rendered the feasible proposal, unique total 1, all six categories, direct/ripple totals, old/proposed values, group/context controls, and confirmation/decision controls with 0 console errors or warnings. |
| Administrator over protected HTTP | Exact acceptance and later repair | Bundle advances atomically and prior attempt pin does not carry | `WorkspaceRepairPlanningIT` independently passed: exact proposal definition/result became accepted, proposal/draft disappeared, manifest recorded attempt provenance, and next compiled definition used the new parent without the prior room pin. |
| Administrator over protected HTTP | Extension 5a/5b discard or revise | Only proposal is removed | Independent integration run returned `REPAIR_DRAFT`, compared the complete accepted baseline/draft, and proved proposal absence. |
| Administrator over protected HTTP | Extension 6a stale identity | Refuse, invalidate, retain prior baseline | Twelve independent corruptions each returned `409 STALE_PROPOSAL`, retained exact accepted data, removed proposal eligibility, and returned to the draft. |
| Administrator over protected HTTP | Extension 6b storage failure | Roll back and keep proposal retryable | Injected PostgreSQL trigger produced safe `503 STORAGE_UNAVAILABLE`; document/version/lifecycle remained exact and retry succeeded after trigger removal. |
| Administrator in real Chrome | G5 target-scale opening | Below one second, solver excluded | Focused run measured 318.600 ms; clean convergence reactor measured 343.200 ms on the approximately 1,000-lesson fixture, both excluding solver time. |

The first focused Docker attempt was sandbox-denied before container initialization; the authorized reproduction completed successfully. The first unit command used the wrong reactor-safe Surefire property and failed before executing tests; the corrected command ran the target test successfully. Neither setup failure is product counterevidence.

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Separates direct from ripple and preserves six categories | `ProposalReviewService.java:28-51,66-96`; legacy snapshot derivation at `WorkspaceController.java:224-243`; overlap unit assertions; live Merivälja Chrome visible labels | STRONG | yes |
| Main step 2 | Shows unique/category totals and class/teacher/room/day old/proposed groupings | Server review arrays at `ProposalReviewService.java:61-96,122-166`; unit group-context assertions; Chrome category/total text | STRONG | yes |
| Main step 3 | Shows exact old/proposed dimensions, emphasizes changes, links contexts | `ProposalReviewService.java:67-88,100-120`; `app.js` before/after cards and native context buttons; Chrome old/proposed/context reproduction | STRONG | yes |
| Main step 4 | Shows status, unique, move/forced, termination, and advancement warning | Real Chrome assertions at `WorkspaceBrowserIT.java:406-417`; centralized proposal facts/warning in `app.js`/`messages.js` | STRONG | yes |
| Main step 5 | Explicitly confirms acceptance | Native checkbox disables accept until selected; real Chrome focuses both controls and presses Space at `WorkspaceBrowserIT.java:425-428` | STRONG | yes |
| Main step 6 | Revalidates identity, stores complete bundle, then advances current | Full identity/verification guard including missing-derived-review compatibility at `RepairProposalService.java:48-127`; exact legacy-shape acceptance assertions at `WorkspaceRepairPlanningIT.java:140-178` | STRONG | yes |
| Main step 7 | Clears transient state/pins and shows reusable new baseline | `WorkspaceRepairPlanningIT.java:173-192` asserts proposal/draft cleanup, manifest provenance, next-parent revision, and prior pin removal; Chrome observes current baseline | STRONG | yes |
| Extension 1a | Overlap appears in both explanations but once in unique total | Unit fixture places l1 in teacher and period categories plus direct effects while asserting two unique lessons across three entries | STRONG | yes |
| Extension 2a | Empty categories/groupings show zero | Server always emits six category/group arrays; unit asserts empty entries and Chrome observes `Additions 0` and `Cancellations 0` | STRONG | yes |
| Extension 3a | Unchanged lesson is accepted, quiet, and excluded | Scale Chrome first observes total 100, opens an unchanged lesson, and observes explicit exclusion plus accepted-assignment detail at `WorkspaceBrowserIT.java:564-570` | STRONG | yes |
| Extension 5a | Discard removes only proposal | Protected DELETE path and exact complete-document assertions at `WorkspaceRepairPlanningIT.java:187-201` | STRONG | yes |
| Extension 5b | Revise invalidates immediately and returns to retained draft | Separate native `Revise intent` button maps to the same server-authoritative proposal invalidation; production route outcome is exercised at `WorkspaceRepairPlanningIT.java:187-201` | STRONG | yes |
| Extension 6a | Any identity mismatch refuses and invalidates without accepted mutation | Twelve-field corruption matrix and exact accepted comparison at `WorkspaceRepairPlanningIT.java:203-239` | STRONG | yes |
| Extension 6b | Storage failure retains old baseline and retryable proposal | Real PostgreSQL trigger rollback and successful retry at `WorkspaceRepairPlanningIT.java:241-271` | STRONG | yes |
| G1 | Unique headline; authoritative categories; no cross-group sum | ID union and independent category/group arrays in `ProposalReviewService`; overlap/group unit assertions | STRONG | yes |
| G2 | Exact identities and explicit missing-name fallback | Review sides copy source IDs at `ProposalReviewService.java:100-113`; browser `entityName` renders stable ID plus `Name unavailable`; accepted-view regressions remain green | STRONG | yes |
| G3 | Six category semantics remain unchanged | Fixed six arrays are copied by lesson membership without reclassification at `ProposalReviewService.java:19-47`; overlap test pins retained membership | STRONG | yes |
| G4 | Keyboard-operable review/selection/discard/confirmation/context | Native buttons/select/checkbox with explicit text; keyboard acceptance and context-return Chrome journeys pass with no color-only state | STRONG | yes |
| G5 | Review opens below one second at validation scale | Independent Chrome measurements 318.600 ms focused and 343.200 ms clean; 1,000 lessons and solver excluded | STRONG | yes |
| G6 | Only confirmed atomic acceptance changes reference | Disabled-before-confirmation UI, server identity/verification guard, single conditional SQL update, and injected rollback evidence | STRONG | yes |
| Success postcondition | Exact bundle current; no proposal/draft; subsequent direct repair possible | Exact definition/result equality, transient absence, new parent revision, and removed attempt lock at `WorkspaceRepairPlanningIT.java:170-192` | STRONG | yes |
| Minimal guarantee | Non-success preserves prior baseline and never current-labels proposal | Discard, twelve stale cases, and storage rollback compare complete accepted/document state and proper proposal eligibility | STRONG | yes |
| Requires UC-5 | Consumes approved UC-5 verified proposal | Browser and integration tests create the proposal through production packaged replan, remove the later derived review to reproduce the pre-UC-6 persistence shape, then reload and accept it; the live persisted UC-5 proposal also renders | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Preserve external kernel process boundary | Diff adds workspace-only review/decision; packaged UC-5 path and architecture suite pass | PASS |
| RULE-2 | Keep pinned Java/Spring/PostgreSQL baseline | Clean Java 25/Spring Boot 4.1.1/PostgreSQL 18.6 reactor: 165 green | PASS |
| RULE-3 | One relationally guarded JSONB aggregate via JdbcClient | `RepairProposalService` loads one aggregate; `WorkspaceMutation`/`WorkspaceRepository` conditionally replace one JSONB document; no new store/model | PASS |
| RULE-5 | Enforce exact lifecycle state machine below UI | Service requires `REPAIR_PROPOSAL`; SQL guards both allowed target transitions; lifecycle regression and negative paths pass | PASS |
| RULE-6 | Strong ETag/If-Match and same conditional SQL version | Accept/discard call `requireMatchingVersion`; expected version appears in guarded repository update; real HTTP commands use CSRF/ETag | PASS |
| RULE-7 | Accepted-state change is one transaction | Complete accepted document is prepared then conditionally updated once; trigger failure proves document/version rollback and retry | PASS |
| RULE-8 | Preserve lossless canonical JSON fidelity | Kernel documents are deep-copied; review identity uses canonical bytes to survive JSONB property ordering; exact tree comparisons pass | PASS |
| RULE-17 | Full proposal identity and acceptance guard | `RepairProposalService.java:94-127` checks every authoritative identity/count and independent verifier revision; present review tampering remains rejected, while only an absent derived legacy review is regenerated; corruption matrix and legacy acceptance pass | PASS |
| RULE-18 | Exact categories and unique/direct/ripple/group review | Server-authored fixed category list, unit overlap/zero/group assertions, and Chrome old/proposed presentation pass | PASS |
| RULE-19 | Complete native browser snapshot | Browser renders accepted/proposed/unchanged/focused context from complete snapshot; real Chrome journeys pass | PASS |
| RULE-20 | Accessible English-localized conventions | Central catalog, semantic controls, keyboard acceptance, explicit state text, and architecture/browser tests pass | PASS |
| RULE-21 | Same-origin local security without identity | Explicit route matrix remains under Host/Origin filter and CSRF; hostile/missing origin, CSRF, Host, CORS, and denied-route regression pass | PASS |
| RULE-22 | Stable safe API failures | Stale/storage responses assert stable codes; no candidate or technical detail is returned; error regressions pass | PASS |
| RULE-23 | Safe observability/evidence | Acceptance retains safe `lastRun`; logs contain only bounded metadata and full prohibited-value regressions pass | PASS |
| RULE-24 | PostgreSQL-only isolated actor verification | Independent focused and clean runs use disposable PostgreSQL 18.6, HTTP, packaged CLI, and real Chrome with state/absence assertions | PASS |
| RULE-25 | Separate validation-scale interaction measurements | Repeatable 1,000-lesson Chrome timing is below one second and explicitly excludes solver time; prior interaction regressions remain below limits | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Accepted aggregate, import, security, persistence, verifier | Clean reactor import/Flyway/security/archive/kernel verification suites | PASS |
| UC-2 | Shared proposal routes, acceptance transaction, verifier | Initial planning/proposal/acceptance and browser journey in clean reactor | PASS |
| UC-3 | Accepted snapshot, context navigation, browser model, scale | Whole-school/empty/complete snapshot and target-scale Chrome tests | PASS |
| UC-4 | Repair draft, pins, manifest locks, narrow UI | Repair draft HTTP/Chrome tests and pin-feedback p95 remain green | PASS |
| UC-5 | Required packaged proposal, run lifecycle, review input | Nine repair-planning integration tests and packaged/Chrome repair run pass | PASS |
| Kernel UC-1/UC-2 | Definition/result revisions, verify/plan/replan contracts | Full kernel unit and packaged CLI suites pass in the 165-test reactor | PASS |

## Findings

No active critical, gap, protocol, drift, or cosmetic findings.

Prior C-1 is resolved. The complete snapshot now derives only a missing legacy review from authoritative accepted,
draft, definition, and result inputs without rewriting persistence. Acceptance permits that missing derived member but
still canonical-compares every present review, so the existing tamper case remains ineligible. PostgreSQL/HTTP and real
Chrome tests remove the review from a packaged UC-5 proposal before reload and acceptance. The packaged live build
also renders the existing Merivälja Kool proposal with zero console errors or warnings.

## Walkthrough

The prior walkthrough failure is repaired and independently reproduced through page opening. User confirmation of the
remaining interaction script is pending. On the now-open desktop-width workspace with the repair proposal:

1. Confirm the page says the prior accepted baseline is still current and shows the unique changed-lesson total, all six category totals including zeros, and direct versus solver-ripple counts.
2. Expand class, teacher, room, and day groupings; confirm every grouping says old, proposed, or both and no cross-group grand total is implied.
3. Open one changed lesson; compare old/proposed subject, class, teacher, period, and room, and confirm only changed dimensions are emphasized.
4. Open whole-school and one focused context, then return to the impact review using the keyboard.
5. Inspect an unchanged accepted lesson and confirm it is visually quiet and explicitly excluded from change totals.
6. Confirm `Accept repair as current` is disabled until the accounting checkbox is selected; select it and activate acceptance using the keyboard.
7. Confirm the newly accepted timetable is shown as current and a new protected repair can be started.
8. In a separate proposal, choose `Discard proposal` or `Revise intent`; confirm the prior accepted timetable stays current and the retained repair draft reopens.

## Status Update

`READY_FOR_CONVERGENCE` -> `PENDING_WALKTHROUGH`; UC-8 remains independently eligible, but execution stays on UC-6. UC-7 remains ineligible because it includes UC-6.

## Response to execute

PENDING WALKTHROUGH: confirm the repaired UC-6 administrator review, context, keyboard acceptance, and discard/revise script.
