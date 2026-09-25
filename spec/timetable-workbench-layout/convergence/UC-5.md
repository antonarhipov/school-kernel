# Convergence: UC-5 - Complete a repair in the wide workbench

## Summary

- Submission: revised `checkpoints/UC-5.md` at `14e88da` (revision base `d0aeb17`; original implementation `75d0d89`)
- Verdict: REJECT
- Findings: 0 critical, 1 gap, 0 protocol, 0 drift, 0 cosmetic. Prior C-1 and G-1 resolved; G-2 remains.
- Suite: independent focused UC-5 browser journey passed; independent full reactor ran 195 tests, with 0 failures, 2 browser errors, 0 skips. The 36-case browser class had both errors; the other 159 tests passed.
- Working tree impact from verification: none tracked. Only the pre-existing, unrelated `spec.md` edit was present before and after. Fresh screenshots are ignored under `timetable-workspace/target/workbench-layout/`.

## Protocol Gate

1. UC-5 alone was `READY_FOR_CONVERGENCE`; required and included UC-1 through UC-4 were `APPROVED`; no other UC was active.
2. The revision is an immutable commit at `14e88da`. `git diff d0aeb17..14e88da` contains only the UC-5 presentation fix, affected browser assertions, replacement checkpoint and ledger. The pre-existing `spec.md` edit remains outside the commit.
3. The replacement checkpoint covers all five main steps, five extensions, five guarantees, both postconditions, four included relationships, fourteen rules, changed files, focused tests and related regressions. It does not claim the missing five-administrator result.
4. `git diff --check d0aeb17..14e88da` passed. The independent focused journey used real Chrome, isolated PostgreSQL, packaged scheduling and independent verification. The independent full suite was attempted but did not pass, so the checkpoint's builder green result cannot close RULE-14.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator in one Chrome session | Current, Draft, Solving, Proposal, accepted successor, second Draft | Exact continuous 1,000-lesson journey and handoff comparisons | Independent focused `WorkspaceBrowserIT.completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessorInRealBrowser` passed after the committed revision. The browser checked exact stored document/version, frozen run, verified Proposal and new Draft parent. |
| Administrator reviewing simultaneous Proposal Week cues | Subject/room and badges remain within each occupied tile at 1600 and 1280 | `WorkspaceBrowserIT.java:3207-3238` checked all 1,001 representations at both widths. Fresh `uc5-proposal-1600.png` and `uc5-proposal-1280.png` from 09:21 show room labels and contained compact badges, with task decisions visible. Prior C-1 and G-1 are resolved. |
| Administrator reading verified Proposal at 390 | Read-only true lifecycle, no decisions | Independent focused journey passed `WorkspaceBrowserIT.java:3094-3104` and exact unchanged document check. |
| Administrator in shared approved flows | Complete 36-browser/195-reactor regression | Independent full reactor exited 1. `tracesScaleInvestigationAndOmittedAvailabilityInRealBrowser` read an empty `DevToolsActivePort` at `WorkspaceBrowserIT.java:3657`; `preparesProtectedRepairDraftInRealBrowser` timed out awaiting a `Runtime.evaluate` reply at `:1486,3701`. Failsafe reports 36 browser cases, 2 errors, 0 skips. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1 | Exact Current and UC-1 inspection | Independent focused browser `WorkspaceBrowserIT.java:2991-3020` checked verifier-accepted school, IDs, selection, focus and unchanged accepted JSON | STRONG | yes |
| Main 2 | UC-2 stages protected, conflict-free Draft | Browser `:3021-3046` checked exact intent/effects/pin and old Current | STRONG | yes |
| Main 3 | UC-3 frozen run, cancellation and verified handoff | Browser `:3048-3084` checked run/Draft/Current, cancellation and independently verified Proposal | STRONG | yes |
| Main 4 | UC-4 review and explicit acceptance with understandable canvas | Browser `:3085-3123,3207-3238` checked exact two-sided values, contained tiles at both desktops and explicit decision | STRONG | yes |
| Main 5 | Exact accepted successor and another Draft | Browser `:3124-3154` checked exact accepted definition/result/revision, removed Draft/Proposal and new Draft parent | STRONG | yes |
| Extension 2a | Conflict or unsaved Draft blocks solve | Browser `:3027-3036,1688-1729` checks no process and exact document/version | STRONG | yes |
| Extension 3a | Cancel/interruption/failure/rejection retain Draft and Current | Browser `:3068-3084,1900-2250` and HTTP/JDBC failure journeys check exact terminal state | STRONG | yes |
| Extension 4a | Revise/discard do not accept | Browser `:2882-2968` checks retained Draft and old Current | STRONG | yes |
| Extension 4b | Stale or failed acceptance keeps old Current | Browser `:1808-1845,2969-2985` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:306-373` check refusal/invalidation or retryable Proposal | STRONG | yes |
| Extension 5a | Next Draft parents successor without inherited pins | Browser `:3129-3154` compares revision, direct effect and empty attempt pin/bulk sets | STRONG | yes |
| G1 | One accepted identity and model through modes | Browser `:2991-3154` checks same 1,000 IDs and exact durable handoffs | STRONG | yes |
| G2 | Current, Draft, run and Proposal distinguishable at decisions | Browser `:3021-3123,3158-3206` checks text and active modes | STRONG | yes |
| G3 | Desktop canvas and task plus narrow read-only view | Browser `:3043,3060,3094-3104,3158-3245` measures region geometry and every Proposal Week tile; fresh visual review confirms subject/room/badge containment | STRONG | yes |
| G4 | Failure keeps exact old accepted bundle | Browser `:3027-3036,3068-3084,3114-3123` and failure tests compare exact state | STRONG | yes |
| G5 automated portion | One real journey and complete shared regression | Focused journey passed, but independent full run had two browser-harness errors; complete shared regression is not green | WEAK | no |
| Success postcondition | Accepted successor Current and another repair possible | Browser `:3124-3154` checks exact successor and second Draft | STRONG | yes |
| Minimal guarantee | Incomplete steps do not advance Current | Browser and HTTP/JDBC refusal paths above compare old accepted data | STRONG | yes |
| Requires and Includes UC-1 | UC-1's real accepted inspection is consumed | Same Chrome session begins from verified 1,000-lesson Current at `:2991-3020` | STRONG | yes |
| Requires and Includes UC-2 | UC-2's saved protected Draft is consumed | Same session stages and reads the exact Draft at `:3021-3046` | STRONG | yes |
| Requires and Includes UC-3 | UC-3's independently verified Proposal is consumed | Same session runs/cancels/re-runs and verifies result at `:3048-3084` | STRONG | yes |
| Requires and Includes UC-4 | UC-4 comparison and acceptance are consumed | Same session compares and accepts at `:3085-3123`, with tile geometry independently verified | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | One packaged workbench and snapshot authority MUST remain | Revision has no route/model/storage/dependency change; browser checks exact stable IDs | PASS |
| RULE-2 | Presentation MUST NOT mutate workspace | Browser `:3043,3064-3067,3114-3123,3158-3245` compares document/version; prior request-trace cases still exist | PASS for focused journey |
| RULE-3 | Service lifecycle MUST refuse ineligible transitions | Existing `RepairDraftService`/`RepairPlanningService`, `WorkspaceRepairPlanningIT.java:90-535`, and UC-5 refusal paths | PASS |
| RULE-4 | Subject leads and room MUST be visible in every occupied tile | `app.js:814,1170-1178` uses compact Week cues; `styles.css:325-360` reserves room width and wraps contained badges; `WorkspaceBrowserIT.java:3207-3238` checks all 1,001 tile representations at both viewports; fresh screenshots confirm | PASS; prior C-1/G-1 resolved |
| RULE-5 | Side inspector and below-canvas task MUST remain usable | Browser `:3158-3203` measures task, canvas heading/complete row, inspector and decisions at both desktops | PASS |
| RULE-6 | Complete/either-side population and honest narrowing MUST hold | Browser `:2509-2583,2991-3020,3085-3123` checks IDs and one-sided visible compact cues with full accessible names | PASS in focused and builder related-UC runs |
| RULE-7 | Exact authoritative labels and safe text MUST hold | Complete names/IDs in accessible/inspector labels; room text remains visible in Week | PASS |
| RULE-8 | Draft setup, protection and refusals MUST remain explicit | Browser `:3021-3046` plus Draft service cases | PASS for focused journey; full shared browser regression pending |
| RULE-9 | Solving MUST remain frozen and accepted | Browser `:3048-3084` plus process failure cases | PASS |
| RULE-10 | Proposal MUST preserve stable comparison identity and counts | Browser `:3085-3123` plus UC-4 comparison cases | PASS |
| RULE-11 | Only explicit durable acceptance MAY advance Current | Browser `:3124-3154` and transactional refusal tests compare old/new bundle | PASS |
| RULE-12 | Local route and narrow read-only boundary MUST hold | No server diff; hostile-route tests and 390px browser check `:3094-3104` | PASS |
| RULE-13 | Textual/structural accessible cues MUST remain | `messages.js:152-155` catalogs compact side-match labels; browser verifies visible badges, full accessible names, native decisions | PASS |
| RULE-14 | Exact isolated journey and complete shared regression MUST run | Focused normative journey passed; independent 195-test reactor had two Chrome/CDP errors | FAIL G-2; human gate pending |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Workbench-layout UC-1 through UC-4 | Shared browser canvas, Draft, run and review | Independent full suite ran 36 browser cases; two browser-harness errors at `:1053,1486`; the other 34 browser cases passed. Builder full suite had 36/36 pass, but cannot replace this independent run. | INCOMPLETE G-2 |
| Existing workspace/kernel | Import, repair services, process/verification and local routes | Independent full run passed all 85 kernel tests, 26 workspace unit tests and 48 non-browser workspace integration tests | PASS for completed cases |
| `timetable-ux-polish` UC-5 | Shared browser repair journey | Focused UC-5 journey passed; this audit does not change the separate participant-gate verdict | PASS automation only |

## Findings

### G-2 GAP - Complete independent browser regression remains non-green

RULE-14 requires “the complete shared browser regression.” The builder's revised full 195-test run passed, but the independent run did not. At `WorkspaceBrowserIT.java:3651-3657`, `startBrowser` waits only for the debug-port file to exist, then immediately reads line 0; the empty-file race is directly evidenced by `IndexOutOfBoundsException` in `tracesScaleInvestigationAndOmittedAvailabilityInRealBrowser`. Separately, a non-idempotent lesson click in the existing UC-2 browser test at `:1486` waited out `Cdp.command`'s 15-second reply timeout at `:3701`. The same approved UC-2 case timed out in the first convergence audit, though at a different command. The latter's cause is not proven; do not blindly replay the click. Revision outcome: make Chrome debug-port readiness atomic from the test's perspective, diagnose or bound CDP transport delays without hiding product failures or repeating non-idempotent actions, then obtain a clean independent full relevant suite with no skip. C-1 and G-1 are closed by the fresh visual and tile-bound evidence and must not be weakened.

## Walkthrough

Not requested while G-2 blocks automated convergence. Once the full gate passes, the UC-5 walkthrough should follow the five main steps and alternate refusal paths: locate an exact accepted lesson; stage a protected conflict-free Draft; inspect Current and frozen Draft during Solving; explain Proposal origin/destination, same-slot change, direct/ripple/protection and accepted Current in the wide canvas/task area; explicitly accept and identify only the new Current; begin another repair from that successor. It should confirm that conflicting/unsaved Draft, failed/cancelled run, revise/discard and stale/failed acceptance preserve old Current. The feature gate additionally requires six recorded tasks for five timetable administrators at three or more schools and its specified success/Current-identification thresholds. No participant record has been supplied.

## Status Update

UC-5 `READY_FOR_CONVERGENCE` -> `NEEDS_REVISION`. No next UC is eligible. UC-1 through UC-4 remain `APPROVED`; the pre-existing `spec.md` edit and other feature verdicts are untouched.

## Response to execute

REVISE UC-5: stabilize the Chrome/CDP browser harness and obtain a clean independent shared regression; retain the resolved tile-visibility assertions.
