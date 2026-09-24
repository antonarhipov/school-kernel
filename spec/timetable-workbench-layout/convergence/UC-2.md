# Convergence: UC-2 - Prepare a protected repair in the wide task area

## Summary

- Submission: `spec/timetable-workbench-layout/checkpoints/UC-2.md` at `255ab36` (base `5966fab`)
- Verdict: APPROVE
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: independent focused run, 26 unit + 4 browser + 9 Draft integration; independent full reactor, 26 unit + 84 integration including 36 browser; 0 failures, errors, or skips in both
- Working tree impact from verification: none tracked; `git status --short` was clean before and after both runs. Screenshots are in ignored `timetable-workspace/target/workbench-layout/`.

## Protocol Gate

1. UC-2 alone was `READY_FOR_CONVERGENCE`; UC-3–UC-5 were `NOT_STARTED`, and no other UC was active.
2. The checkpoint, implementation, tests, and ledger are together in immutable commit `255ab36`; its declared base resolves to `5966fab`.
3. Required UC-1 and UC-1's extension point 5a are approved in `convergence/UC-1.md` and commit `5966fab`.
4. The checkpoint has rows for seven main steps, all thirteen extensions, seven guarantees, both postconditions, both relationships, all nine applicable rules, changed files, commands, and shared regression.
5. All seven changed files were inspected. The production diff changes only `app.js`, `messages.js`, and `styles.css`; it adds no server route, schema, dependency, durable field, or UC-3/UC-4 workflow. Two browser/HTTP test files and this feature's status/checkpoint complete the submission.
6. The worktree was clean at the initial audit and after the independent focused and full runs. `node --check` and `git diff --check 5966fab..255ab36` passed; the architecture tests remain green without a new exemption.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator, packaged Chrome and isolated PostgreSQL | UC-1 accepted Week into setup and staged teacher Draft | Exact 1,000 accepted lessons, unstaged setup no write, teacher-16/period-0 saved | Independently reran `preparesWideProtectedDraftAtNormativeScaleInRealBrowser`. `WorkspaceBrowserIT.java:660–693` asserts the 1,000-ID canvas, selected-resource prefill, no-write setup close, invalid-period refusal, exact saved intent, direct-effect ID, and unchanged accepted bundle. |
| Administrator | Conflict and individual/bulk protection | Conflict blocks solve; selective navigation, preview/cancel/confirm/undo preserve exact intent | The same browser run passed `:695–786`. Exact conflict code, pin sources, 60 preview IDs, PostgreSQL failures, named bulk action, undo, and ready Draft are compared with versioned stored JSON. The independent nine-case HTTP Draft suite passed. |
| Administrator | Mode, task, focused return, reload, discard, room/no-effect and narrow | Context retained; only Draft actions write | Browser `:788–858` passed at 1600×900, 1280×800, 1279, 701, 700 and 390 CSS px. It intercepted mutation traffic for presentation actions and compared the full versioned JSON before/after; 700/390 had no mutation controls. Fresh `uc2-draft-{1600,1280}.png` screenshots were visually inspected. |
| Administrator through existing HTTP routes | Invalid and conflicting run, stale and failed save | No ineligible process, accepted/Draft exact | `WorkspaceRepairDraftIT.java:129–160,276–354` passed against freshly migrated storage. `RepairPlanningService.java:47–65` guards state, unsaved revision and conflicts before scheduling. The HTTP tests assert exact document/version and absent run/proposal where applicable. |
| Administrator | Proposal revision into Draft | Retained Draft reopens without a second Draft | The full browser regression passed `WorkspaceBrowserIT.java:2617–2647`, which follows the production verified-Proposal revision and checks unchanged Current and Draft/pins in the wide area. |

The test fixture in `WorkspaceBrowserIT.java:3054–3093` rebuilds the normative 1,000-assignment school and independently verifies its accepted definition/result before storage. The browser compares rendered lesson IDs and complete versioned PostgreSQL JSON by value, including absence of run/proposal after refused Draft actions. No credential or schema is introduced by UC-2. The four-row presentation owner remains `inspection-state.js:1–104`; the existing service lifecycle remains the mutation boundary.

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1 | Setup opens without write | Browser `WorkspaceBrowserIT.java:664–675`, exact versioned document, task outside inspector; focused PASS | STRONG | yes |
| Main 2 | Teacher/period staged | Browser `:676–684`, saved teacher-16 and period-0 values; focused PASS | STRONG | yes |
| Main 3 | Durable Draft decorates accepted canvas | Browser `:680–693`, exact direct lesson-960, 1,000 accepted IDs and task controls; focused PASS | STRONG | yes |
| Main 4 | Conflict inspected/resolved and lesson protected | Browser `:695–716`, exact code/ID and room pin; HTTP `WorkspaceRepairDraftIT.java:129–160`; focused PASS | STRONG | yes |
| Main 5 | Bulk exact preview/confirm/undo | Browser `:717–786` compares 60 IDs, PERIOD dimension, action and retained individual pin; HTTP `:165–265`; focused PASS | STRONG | yes |
| Main 6 | Wide sections and retained presentation context | Browser `:788–818` checks mode/task/focus/selection, no mutation traffic, exact document/version and geometry; focused PASS | STRONG | yes |
| Main 7 | One saved ready Draft | Browser `:780–786` checks `readyToSolve`, no conflict, exact pin and unchanged Current; focused PASS | STRONG | yes |
| Extension 1a | Unstaged setup closes | Browser `:670–675` checks closed task and unchanged versioned aggregate; focused PASS | STRONG | yes |
| Extension 1b | Narrow read-only Current/Draft | Browser `:825–839` checks 700/390 controls absent and exact stored state; focused/full PASS | STRONG | yes |
| Extension 1c | Proposal revision reopens retained Draft | Browser `:2617–2647` checks production revise path, same Draft/pins, Current and wide task; full PASS | STRONG | yes |
| Extension 2a | Invalid resource or period refused | Browser `:676–678` refuses zero periods; HTTP `WorkspaceRepairDraftIT.java:276–289` refuses missing IDs and checks document/version; focused PASS | STRONG | yes |
| Extension 2b | Failed or stale write cannot enable solve | Browser `:819–824` checks stale version, visible failure and disabled solve; HTTP `:329–354` drives failed storage and refused run; focused PASS | STRONG | yes |
| Extension 3a | Saved zero-direct-effect rule | Browser `:852–858` asserts room-99/period-0, empty direct IDs and ready Draft; focused PASS | STRONG | yes |
| Extension 4a | Pin/policy conflict remains blocking | Browser `:695–710` checks exact pin conflict/canvas cue and disabled solve; HTTP `:129–160` checks refusal/no run and policy conflict; focused PASS | STRONG | yes |
| Extension 4b | Excluded lesson brought into view selectively | Browser `:702–707` checks Tuesday→Monday, only excluding room filter cleared, search/teacher retained, same selected ID and no write; focused PASS | STRONG | yes |
| Extension 5a | Cancelled preview writes nothing | Browser `:717–729` compares exact versioned document before/after preview and cancel; focused PASS | STRONG | yes |
| Extension 5b | Failed bulk save/undo leaves last durable Draft | Browser `:730–783` uses real PostgreSQL rejection and compares exact state/version, failure text, disabled solve, then explicit successful retry; focused PASS | STRONG | yes |
| Extension 6a | Confirmed discard returns to Current | Browser `:846–850` checks Draft/pins removed and exact accepted bundle; focused PASS | STRONG | yes |
| Extension 6b | Reload restores durable Draft/default open task | Browser `:840–845` checks Draft mode/open area, saved intent, reset ephemeral fields and exact stored state; focused PASS | STRONG | yes |
| Extension 7a | Conflict/unsaved run refused | HTTP `WorkspaceRepairDraftIT.java:140–150,329–354` checks 409 and exact no-run state; `RepairPlanningService.java:47–65` enforces `DRAFT_NOT_DURABLE`/`DRAFT_CONFLICT`; browser disabled solve; focused PASS | STRONG | yes |
| G1 | Accepted assignments remain Current; pins attempt-scoped | `app.js:377–466,709–741`, browser `:679–716`, exact accepted JSON and pin sources; focused PASS | STRONG | yes |
| G2 | Task beneath canvas, inspector contextual, geometry and access | `styles.css:331–386`; browser `:689–693,807–816,863–899` and fresh screenshots show ≤35% task, independent scroll, heading/full row, reachable decisions at 1600/1280; focused PASS | STRONG | yes |
| G3 | Presentation saves nothing; guarded explicit writes only | Browser `:788–806,819–824` records no mutating fetch and exact versioned JSON; HTTP `:306–354` checks conditional/failure boundary; focused PASS | STRONG | yes |
| G4 | Exact IDs, names, periods, conflicts, lock provenance and text cues | `app.js:423–466,671–684,709–741`; browser `:695–716,1871–1880`, HTTP `:129–160`; focused/full PASS | STRONG | yes |
| G5 | Preview no-write, exact confirmation, identified undo, safe failure | Browser `:717–786` and HTTP `:165–265,329–354`; focused PASS | STRONG | yes |
| G6 | Intermediate wide/stacked and narrow read-only | Browser `:813–839`, CSS `:370–386`; focused PASS | STRONG | yes |
| G7 | Normative real-browser complete branch set | Browser `:660–859,3054–3093`, exact fixture and storage; focused/full PASS | STRONG | yes |
| Success postcondition | Saved conflict-free Draft, same accepted whole school | Browser `:780–786`, 1,000-ID accepted canvas and exact PostgreSQL accepted pair; focused PASS | STRONG | yes |
| Minimal guarantee | Refused/cancelled/stale/failed actions preserve last durable Current and no ineligible run | Browser `:670–678,702–707,717–750,760–783,819–850`; HTTP `:140–150,276–289,329–354`; focused PASS | STRONG | yes |
| Requires UC-1 | Consumes approved accepted whole-school postcondition | Browser `:664–669` starts from verifier-checked 1,000-ID UC-1 canvas; full shared accepted/focus regression PASS | STRONG | yes |
| Extends UC-1 at 5a | Uses unstaged setup then production stage path | Browser `:670–684`; approved UC-1 setup and Current inspection remain green in full regression | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | “MUST extend the existing packaged workspace” and “MUST NOT fork accepted assignments” | Seven-file diff adds no route, model, dependency or persistence field; `app.js:40–153,377–399` uses one snapshot/accepted model; browser exact 1,000 IDs | PASS |
| RULE-2 | One state owner “MUST derive available/default modes”; presentation actions “MUST NOT issue durable mutation requests” | `inspection-state.js:1–104`, `app.js:118–153,393–411`; browser `:788–806,840–845` checks per-mode collapse, reload and exact no-write state | PASS |
| RULE-3 | Services “MUST enforce only” declared transitions and refusals at mutation boundary | `RepairPlanningService.java:47–65`, `RepairDraftService.java:42–114`; HTTP `WorkspaceRepairDraftIT.java:129–160,276–354` checks conflict, invalid, stale, unsaved and storage refusal with exact state | PASS |
| RULE-5 | Inspector “MUST” stay contextual; wide task “MUST” be beneath canvas and ≤35% height | `app.js:377–466,587–611,709–716`, `styles.css:331–386`; measured browser geometry, reachable decisions and fresh screenshots at both desktop sizes | PASS |
| RULE-7 | Views “MUST use authoritative school-controlled display names”; unmappable references “MUST be refused” | `app.js:423–466,679–684,1150,1243`; browser exact names/IDs and prior-attempt provenance; HTTP invalid-resource refusal; full invalid-import regression | PASS |
| RULE-8 | Setup “MUST open” before write; preview “MUST not persist”; failed save “MUST neither claim success nor enable solve” | Browser `:670–786,819–858`; `app.js:495–530,747–789`; exact PostgreSQL failure, preview, confirm, undo and narrow checks | PASS |
| RULE-12 | Route/access table “MUST remain unchanged”; narrow UI “MUST” be read-only | No server/security diff; full `WorkspaceImportIT` hostile Host/Origin/CSRF/`If-Match` and denied-route regression (19/19), browser `:825–839`; no browser document storage | PASS |
| RULE-13 | New text “MUST use” the English catalog; cues and controls “MUST” be non-color/native | New labels in `messages.js:76–82,191–209`; `app.js:423–466,709–741` uses escaped text, native controls and conflict/pin/status labels; full Utilities/initial-journey regression | PASS |
| RULE-14 | Normative exact isolated real-browser relationship evidence and full shared regression “MUST” run; automation is not administrator approval | `WorkspaceBrowserIT.java:660–899,2617–2647,3054–3093`; independent focused and 84-case full integration runs green; walkthrough remains pending | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Workbench-layout UC-1 | Required accepted canvas, setup extension 5a, inspection state, focus and responsive layout | Independent full `WorkspaceBrowserIT` 36/36 and exact UC-2 opening 1,000-ID canvas; prior UC-1 approval unchanged | PASS |
| Existing workspace UC-1–UC-8 and inspection-UX UC-1–UC-3 | Shared import, local access, accepted model, repair lifecycle and renderer | Independent full reactor 26 unit + 84 integration, 0 failures/errors/skips; `WorkspaceImportIT` 19/19, Draft 9/9, Repair Planning 10/10 | PASS |
| `timetable-ux-polish` UC-1–UC-4 and UC-5 automation | Shared operational modes and browser flow | Full shared browser suite green; separate UC-5 five-administrator gate is still `PENDING_WALKTHROUGH` in its own ledger | PASS automation only |

## Findings

None. The full run logged pin-feedback p95 471.9 ms against a 250 ms reference and proposal-review opening 1146 ms against a 1000 ms reference; both are explicitly diagnostic-only, not functional or rule gates. No test assertion failed.

## Walkthrough

Automated convergence passed. The administrator confirmed “UC-2 walkthrough passed” on 2026-09-24. The confirmed script covered the supplied accepted school: unstaged repair setup; teacher unavailability, direct effect and blocking conflict while Current stays accepted; conflict resolution and protection; bulk preview/cancel/confirm/undo; Draft collapse, mode switching, focused return and reload; the saved ready Draft; no-effect room rule, discard, and narrow read-only view. Automated fault cases covered invalid, stale and failed writes. This confirmation closes only UC-2, not the separate UC-5 five-participant feature gate.

## Status Update

UC-2 `PENDING_WALKTHROUGH` → `APPROVED` following explicit administrator confirmation; UC-3 is next eligible. UC-1 remains `APPROVED`; UC-4–UC-5 remain `NOT_STARTED`.

## Response to execute

APPROVED
