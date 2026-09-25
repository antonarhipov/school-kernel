# Convergence: UC-5 - Complete a repair in the wide workbench

## Summary

- Submission: `checkpoints/UC-5.md` at `75d0d891af9850403f91820120490f9bfb47920c` (base `5542555b9855df55851c9880b7e45ad29ceae79a`)
- Verdict: REJECT
- Findings: 1 critical, 2 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: independent focused UC-5 browser journey passed; independent full reactor ran 195 tests (58 kernel unit, 27 kernel integration, 26 workspace unit, 84 workspace integration including 36 browser), with 0 assertion failures, 1 browser error, 0 skips. The error was a CDP reply timeout in the approved UC-2 browser regression.
- Working tree impact from verification: none tracked. `spec.md` was already dirty before verification and was not changed or included. Screenshots under `timetable-workspace/target/workbench-layout/` are ignored artifacts.

## Protocol Gate

1. UC-5 alone was `READY_FOR_CONVERGENCE`; required and included UC-1 through UC-4 were `APPROVED`; no other UC was active.
2. The implementation, test, ledger and checkpoint were committed together at `75d0d89`. `git diff 5542555..75d0d89` contains only the UC-5 browser journey, checkpoint and ledger. The separately dirty `spec.md` edit was excluded.
3. The checkpoint has rows for all five main steps, five extensions, five guarantees, both postconditions, four included relationships, all fourteen rules, validation commands, changed files and related regression results. Its claimed visual inspection did not catch C-1; that is a substantive finding, not a missing protocol artifact.
4. `git diff --check 5542555..75d0d89` passed. The independent focused journey passed with isolated PostgreSQL, Chrome and the packaged kernel boundary. The independent full suite was attempted but did not pass. `git status --short` before and after contained only the pre-existing `spec.md` edit.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator in real Chrome on the 1,000-lesson school | Current through Draft, Solving, Proposal, acceptance, then a second Draft | One continuous browser journey with exact durable handoffs | Independent focused UC-5 journey passed. `WorkspaceBrowserIT.java:2991-3154` drives the packaged workspace, isolated storage, process boundary and independent verifier, then checks the accepted successor and next Draft. |
| Administrator reviewing Proposal Week at 1600 x 900 and 1280 x 800 | Subject and room remain legible with multiple investigation and comparison cues | Six screenshots and geometry assertions passed | Fresh `uc5-proposal-1600.png` and `uc5-proposal-1280.png` show badge text spilling across adjacent tiles; room labels are not visually present in affected occupied tiles. Geometry assertions at `WorkspaceBrowserIT.java:3158-3210` do not inspect a tile's subject/room visibility or badge bounds. |
| Administrator at 390 x 844 | Proposal becomes a read-only agenda | Same verified Proposal read without decisions | Focused browser journey passed the narrow-state and exact-document assertions at `WorkspaceBrowserIT.java:3094-3104`. |
| Administrator exercising shared approved flows | UC-1 through UC-4 browser regression remains green | Executor's full 195-test run passed | Independent full run exited 1: `WorkspaceBrowserIT.preparesProtectedRepairDraftInRealBrowser` timed out on `Cdp.command` at `WorkspaceBrowserIT.java:1500,3668-3704`; Failsafe reports 84 workspace integration tests, 1 error, 0 skips. This run cannot certify the complete shared regression. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1 | Exact Current and UC-1 inspection | Browser `WorkspaceBrowserIT.java:2991-3020` checks verifier-accepted school, lesson IDs, selection, focus and unchanged accepted JSON; focused run passed | STRONG | yes |
| Main 2 | UC-2 stages protected, conflict-free Draft | Browser `:3021-3046` checks exact intent/effects/pin and old Current; focused run passed | STRONG | yes |
| Main 3 | UC-3 frozen run, cancellation and verified handoff | Browser `:3048-3084` checks exact run/Draft/Current and independent Proposal verification; focused run passed | STRONG | yes |
| Main 4 | UC-4 review and explicit acceptance with understandable canvas | Browser `:3085-3123` checks values and decision path, but simultaneous Proposal cues visibly obscure tile rooms in both desktop screenshots | WEAK | no |
| Main 5 | Exact accepted successor and another Draft | Browser `:3124-3154` checks exact accepted definition/result/revision, removed Draft/Proposal and next Draft parent; focused run passed | STRONG | yes |
| Extension 2a | Conflict or unsaved Draft blocks solve | Browser `:3027-3036,1688-1729` checks no process and exact document/version | STRONG | yes |
| Extension 3a | Cancel/interruption/failure/rejection retain Draft and Current | Browser `:3068-3084,1900-2250` and HTTP/JDBC failure journeys check exact terminal state | STRONG | yes |
| Extension 4a | Revise/discard do not accept | Browser `:2882-2968` checks retained Draft and old Current | STRONG | yes |
| Extension 4b | Stale or failed acceptance keeps old Current | Browser `:1808-1845,2969-2985` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:306-373` check refusal/invalidation or retryable Proposal | STRONG | yes |
| Extension 5a | Next Draft parents successor without inherited pins | Browser `:3129-3154` compares revision, direct effect and empty attempt pin/bulk sets | STRONG | yes |
| G1 | One accepted identity and model through modes | Browser `:2991-3154` checks same 1,000 IDs and exact durable handoffs | STRONG | yes |
| G2 | Current, Draft, run and Proposal distinguishable at decisions | Browser `:3021-3123,3158-3210` checks text and active modes | STRONG | yes |
| G3 | Desktop canvas plus task and narrow read-only view | Browser `:3043,3060,3094-3104,3158-3210` measures region geometry but omits tile content bounds; screenshots show lost room visibility | WEAK | no |
| G4 | Failure keeps exact old accepted bundle | Browser `:3027-3036,3068-3084,3114-3123` and failure tests compare exact state | STRONG | yes |
| G5 automated portion | One real journey and complete shared regression | Focused journey passed; independent full 195-test run had one UC-2 browser timeout | WEAK | no |
| Success postcondition | Accepted successor Current and another repair possible | Browser `:3124-3154` checks exact successor and second Draft | STRONG | yes |
| Minimal guarantee | Incomplete steps do not advance Current | Browser and HTTP/JDBC refusal paths cited above compare old accepted data | STRONG | yes |
| Requires and Includes UC-1 | UC-1's real accepted inspection is consumed | Same Chrome session begins from verified 1,000-lesson Current at `:2991-3020` | STRONG | yes |
| Requires and Includes UC-2 | UC-2's saved protected Draft is consumed | Same session stages and reads the exact Draft at `:3021-3046` | STRONG | yes |
| Requires and Includes UC-3 | UC-3's independently verified Proposal is consumed | Same session runs/cancels/re-runs and verifies result at `:3048-3084` | STRONG | yes |
| Requires and Includes UC-4 | UC-4 comparison and acceptance are consumed | Same session compares and accepts at `:3085-3123`, but the wide comparison is visually unreadable in affected tiles | WEAK | no |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | One packaged workbench and snapshot authority MUST remain | UC-5 submission changes no production model, route, storage or dependency; browser checks exact stable IDs | PASS |
| RULE-2 | Presentation MUST NOT mutate workspace | Browser `:3043,3064-3067,3114-3123,3158-3210` compares document/version; approved request-trace regression exists | PASS |
| RULE-3 | Service lifecycle MUST refuse ineligible transitions | Existing `RepairDraftService`/`RepairPlanningService` and `WorkspaceRepairPlanningIT.java:90-535`; UC-5 conflict/cancellation/refusal paths | PASS |
| RULE-4 | “There MUST be exactly one Compact density, with subject leading and room visible in every occupied tile” | `app.js:814,1170-1171` adds long labels; `styles.css:325-328,358-360` gives non-shrinking badges precedence over room; Proposal screenshots at both desktop widths show overflow and invisible room labels; geometry test `WorkspaceBrowserIT.java:3158-3210` misses it | FAIL C-1, G-1 |
| RULE-5 | Side inspector and below-canvas task MUST remain usable | Browser `:3158-3210` measures task, canvas heading/row, inspector and decisions at both desktop widths | PASS for region geometry |
| RULE-6 | Complete/either-side population and honest narrowing MUST hold | Browser `:2991-3020,3085-3123` and UC-4 shared cases check IDs and either-side matching | PASS for data semantics |
| RULE-7 | Exact authoritative labels and safe text MUST hold | Accessible/inspector labels are complete; visible room-label loss in Proposal conflicts with RULE-4 | PASS for source/escaping; visual failure under RULE-4 |
| RULE-8 | Draft setup, protection and refusals MUST remain explicit | Browser `:3021-3046` plus existing Draft cases | PASS |
| RULE-9 | Solving MUST remain frozen and accepted | Browser `:3048-3084` plus process failure cases | PASS |
| RULE-10 | Proposal MUST preserve stable comparison identity and counts | Browser `:3085-3123` plus UC-4 comparison cases check values; tile readability is separately failed under RULE-4 | PASS for values |
| RULE-11 | Only explicit durable acceptance MAY advance Current | Browser `:3124-3154` and transactional refusal tests compare exact old/new bundle | PASS |
| RULE-12 | Local route and narrow read-only boundary MUST hold | No server diff; hostile-route tests and 390px browser check `:3094-3104` passed | PASS |
| RULE-13 | Textual/structural accessible cues MUST remain | Existing message catalog and native controls; Proposal cue text is present but visually collides with adjacent tiles | FAIL as presentation consequence of C-1 |
| RULE-14 | Exact isolated journey and complete shared regression MUST run | Focused normative journey passed; independent full suite has one UC-2 browser timeout | FAIL G-2; human gate pending |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Workbench-layout UC-1 through UC-4 | Shared browser canvas, Draft, run and review | Independent full suite ran 36 browser cases; UC-2 `preparesProtectedRepairDraftInRealBrowser` timed out in CDP at line 1500; the other 35 browser cases passed | INCOMPLETE G-2 |
| Existing workspace/kernel | Import, repair services, process/verification and local routes | Independent full run passed 85 kernel tests, 26 workspace unit tests and the other 83 workspace integration tests | PASS for completed cases |
| `timetable-ux-polish` UC-5 | Shared repair browser journey | Focused UC-5 journey passed; this audit does not change that feature's separately pending participant gate | PASS automation only |

## Findings

### C-1 CRITICAL - Proposal Week tiles hide room labels and spill cues into adjacent tiles

RULE-4 requires “subject leading and room visible in every occupied tile”; UC-5 main step 4 requires inspectable accepted/proposed impact, and G3 requires usable desktop canvas alongside the task. The independently inspected `uc5-proposal-1600.png` and `uc5-proposal-1280.png` show long side-match and investigation badges crossing tile boundaries and crowding out room labels. `app.js:814` renders full investigation labels even for Week, and `app.js:1170-1171` appends full matching-side labels; non-shrinking badge CSS at `styles.css:328,360` lets them consume the tile width before the room. Revision outcome: keep subject and a visibly legible room within every occupied Week tile, contain all cues inside their tile under simultaneous long-name investigation/comparison state, and retain complete authoritative details and matching-side meaning via visible compact cues, inspector and accessible name.

### G-1 GAP - The UC-5 geometry check can pass with a visually broken tile

RULE-4's visible subject/room and multiple-cue requirement is claimed by the checkpoint, but `WorkspaceBrowserIT.java:3158-3210` measures only outer task/canvas/inspector rectangles and horizontal scroll widths. It never measures a Week tile's room or cue bounds, so the focused test passed alongside C-1. Revision outcome: add real-browser assertions at 1600 and 1280 for subject and room visibility and for each visible badge remaining within its own tile, using the same simultaneous investigation and Proposal state; preserve the existing exact-ID, viewport and task-area checks.

### G-2 GAP - Independent full shared regression is not green

RULE-14 requires “the complete shared browser regression.” The independent `./mvnw -q -pl timetable-workspace -am verify` run exited 1: `WorkspaceBrowserIT.preparesProtectedRepairDraftInRealBrowser` timed out waiting for `Runtime.evaluate` at `WorkspaceBrowserIT.java:1500,3668-3704`; Failsafe recorded 36 browser cases, 1 error, 0 skips. This is not evidence that the UC-5 production change caused the timeout; the cause is unproven. Revision outcome: reproduce or diagnose the timeout as needed and obtain a clean full relevant suite on the revised submission, with no unjustified skip.

## Walkthrough

Not requested while C-1, G-1 and G-2 block automated convergence. Once repaired, the UC-5 walkthrough should follow its five main steps and five extensions: locate an exact accepted lesson; stage a protected, conflict-free Draft; inspect Current and frozen intent during Solving; explain the Proposal's origin/destination, same-slot change, direct/ripple/protection and Current status in the wide canvas/task area; explicitly accept and identify only the new Current; begin a second repair from that successor. It should also confirm that blocked Draft, failed/cancelled run, revise/discard and stale/failed acceptance preserve old Current. The separate feature gate then requires six recorded tasks for five timetable administrators from at least three schools, with the specified success and Current-identification thresholds. No participant results have been supplied.

## Status Update

UC-5 `READY_FOR_CONVERGENCE` -> `NEEDS_REVISION`. No next UC is eligible. UC-1 through UC-4 remain `APPROVED`; the separately dirty `spec.md` and other feature verdicts are untouched.

## Response to execute

REVISE UC-5: contain Proposal Week cues, keep room labels visible, assert tile geometry, and obtain a clean shared regression.
