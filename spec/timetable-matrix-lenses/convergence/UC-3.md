# Convergence: UC-3 - Inspect and edit lessons within a lens

## Summary

- Submission: `checkpoints/UC-3.md` and implementation at `eb7680c` (base `fee1bb5`). The current checkout is a descendant; intervening commits did not change UC-3 production or browser-test files.
- Verdict: PENDING WALKTHROUGH.
- Findings: 0 critical, 0 gap, 0 protocol.
- Suite: focused `MatrixLensBrowserIT` 18 run / 0 failures / 0 errors / 0 skipped; full `./mvnw -q -pl timetable-workspace -am verify` workspace unit 26 / 0 / 0 / 0 and workspace integration 124 / 2 failures / 9 errors / 0 skipped. The 11 failing integration test IDs exactly match the pre-UC-3 baseline in `convergence/UC-1.md` and the UC-2 verifier run. `MatrixLensBrowserIT` is 18/18 in both runs; `AcceptedInspectionBrowserIT` and `WorkspaceManualDraftIT` are each 11/11 in the full run.
- Working tree impact from verification: none. `git status --short` was empty before and after. `node --check` for both changed JavaScript modules and `git diff --check fee1bb5 eb7680c` passed.

The first focused attempt could not reach the tests because the sandbox denied the Testcontainers Docker socket (1 initialization error). The focused and full runs above used Docker access and started PostgreSQL 18.6 and real headless Chrome.

Paths below are relative to `timetable-workspace/src`. `app.js` and `messages.js` are in `main/resources/static/workspace/`; `MatrixLensBrowserIT.java`, `Workbench.java`, `WorkbenchBrowserSupport.java`, and `WorkspaceManualDraftIT.java` are in `test/java/org/schoolkernel/workspace/`.

## Protocol Gate

1. UC-3 is the sole target and its status was `READY_FOR_CONVERGENCE` before this audit (`status.md:5-16`). Pass.
2. `checkpoints/UC-3.md` and the complete implementation are committed together at `eb7680c`. The checkpoint names base `fee1bb5`; `git diff fee1bb5 eb7680c` resolves exactly five changed files. Later commits changed documentation, test-profile configuration, and the UC-2 verdict, but not the UC-3 source or tests. Pass.
3. UC-1, the `Requires` dependency and base of `Extends UC-1 at 5a`, is `APPROVED` (`status.md:13`). UC-2 is also now `APPROVED` (`status.md:14`). Pass.
4. Within this feature, no other UC is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`: UC-1/UC-2 are approved and UC-4 is not started. UC-3's earlier overlap with UC-2 was explicitly authorized and recorded in the checkpoint; UC-2 received its verdict before this audit. Pass.
5. The checkpoint supplies evidence for main steps 1-5, extensions 1a/2a/5a/5b, G1-G3, both postconditions, the UC-1 relationship, RULE-1/3/4/6/7, changed files, test commands, and approved-UC regressions. Pass.
6. The implementation diff contains only `app.js`, `messages.js`, `MatrixLensBrowserIT.java`, the checkpoint, and the status ledger. The UI changes are attributable to lens inspection/editing and the shared Day selection fix needed for main step 5; there is no UC-4 narrow-surface implementation or unrelated user change in the submission. Pass.

## Runtime Reproduction

All journeys below ran as the administrator through the real browser and workspace HTTP server, backed by an isolated PostgreSQL Testcontainer. The browser class resets the database and browser context per test (`WorkbenchBrowserSupport.java:134-148`).

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Main 1-2: choose a lens tile and inspect | Complete details and three Show week actions | `MatrixLensBrowserIT.java:582-593` passed: selected lesson 960 opened the inspector with teacher, room, and class actions; no non-GET request. The shared detail builder is `app.js:1175-1261`. |
| Administrator | Main 3-5: edit and follow the lesson | One durable edit, new lens cell, selection retained | `:595-612` passed: one `PATCH /api/manual-draft` per edit, persisted `period-40`, selected tile in the new Week cell, then Day followed a cross-weekday edit to Thursday without losing the teacher lens. |
| Administrator | 1a and 2a: leave or change a lens from a link/action | Clear excluding lens with named notice; class Show week selects class rows | `:662-686` passed for class Show week and a draft direct-effect link. `:273-301` passed for a Proposal review target. Diagnostic buttons bind to the same tested `selectDraftLesson` path (`app.js:1010-1013,821-838`); the conflict overlay lists competing IDs but has no lesson link (`app.js:1297-1309`). |
| Administrator | 5a: edit the lens resource away | Tile leaves; lens, selection, details, announcement remain | `:617-639` passed for both teacher and room reassignment, with exact new resource values and only two edit requests. |
| Administrator | 5b: edit into a clash | Both tiles in one cell; conflict indicators and overlay | `:644-657` passed for the period-4 teacher clash, both highlighted tiles, keyboard-opened `TEACHER_CLASH` overlay, and retained selection. |
| Administrator | Non-mutating lens actions | No workspace mutation | `:662-676`, `:273-301`, and the UC-1/UC-2 journeys in the 18/18 class passed with empty mutation logs and byte-equal stored documents. `Workbench.java:315-340` records every non-GET/HEAD request. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Trigger/precondition | Enter from an approved lens | `MatrixLensBrowserIT.java:585-590,620-624,647-650,668-670` enters through the approved Teacher/Room filter path; UC-1 lens journeys in the same class pass | STRONG | yes |
| Main 1: select a tile | `:582` | `:590` uses the real lesson tile; `app.js:1706-1723` binds selection to the inspector | STRONG | yes |
| Main 2: complete inspector and Show week | `:590-592` | Browser checks the open lesson inspector and all three actions; `app.js:1175-1261,1265-1271` builds the same full detail panel used on class rows | STRONG | yes |
| Main 3: edit period, room, or teacher through existing editor | `:596-601` | Browser edits period (`:596,608`), teacher (`:625`), and room (`:635`) through `#manual-edit-form`; `app.js:1769-1824` binds the existing editor | STRONG | yes |
| Main 4: apply, validate, persist, re-render | `:599-601` | Exact `PATCH /api/manual-draft` count and stored assignment at `:599-601,629,639`; shared `mutate` renders the server result (`app.js:2125-2174`); `WorkspaceManualDraftIT` passed 11/11 | STRONG | yes |
| Main 5: still represented in its new lens cell, selected | `:597-612` | Week cell title, `aria-pressed`, open inspector, lens row, and Day Thursday follow all asserted by value at `:597-612` | STRONG | yes |
| 1a: outside review/diagnostic/draft target | `:662-686` plus UC-1 review test | Draft link and Proposal review target clear the excluding lens, name the criterion, and select the target (`:684-686,288-291`). Diagnostic links call the same `selectDraftLesson` handler (`app.js:1010-1013,821-838`); no conflict-overlay navigation link exists (`app.js:1297-1309`) | STRONG | yes |
| 2a: teacher/room Show week | UC-1/UC-2 lens journeys | Real inspector Teacher/Room actions switch the lens in `:138-157,419-453`; `app.js:1288-1294` uses the same transition as the filter select | STRONG | yes |
| 2a: class Show week | `:671-676` | Browser asserts class filter `cohort-16`, cleared teacher lens, one class row, selected lesson, open inspector, no mutation and byte-equal document | STRONG | yes |
| 5a.1: remove tile | `:625-637` | Browser asserts no lesson tile after teacher and room reassignment | STRONG | yes |
| 5a.2: keep selection/details open | `:627,637` | Browser asserts selected lesson title, open inspector, and edited teacher value; `app.js:174-180,1648-1697` anchors the detached inspector | STRONG | yes |
| 5a.3: announce departure and new resource | `:626,636` | Browser awaits exact teacher and room departure sentences; `messages.js:323` supplies the localized text | STRONG | yes |
| 5a.4: lens unchanged | `:627,637` | Browser checks retained teacher/room filter and teacher lens row; only edit PATCHes were sent | STRONG | yes |
| 5b: same-cell clash and overlay | `:644-657` | Browser asserts both highlighted tiles and indicators in period-4, keyboard opens the overlay containing `TEACHER_CLASH`; `app.js:1297-1352` uses the existing overlay | STRONG | yes |
| G1: class-row parity | Same tile/inspector/editor/overlay path | `app.js:896-906,1028-1077,1119-1123` uses the shared renderers and details; the 18/18 lens class checks selection, editing, conflicts, and Proposal sides (`:273-301`); passing `ProposalReviewBrowserIT.java:66-89` checks both sides' comparison cues in resource lenses | STRONG | yes |
| G2: no silent disappearance | `:617-639` | Both departure cases retain inspector details and announce the new teacher/room by value | STRONG | yes |
| G3: lens presentation does not mutate | `:593,675-676` | Empty non-GET logs and exact durable-document equality for presentation actions; edit paths issue exactly the expected PATCHes (`:599,612,639`) | STRONG | yes |
| Success postcondition | Inspect/edit without leaving matrix | Week and Day edits, class action, departure, and clash remain in the matrix and inspector (`:582-686`) | STRONG | yes |
| Minimal guarantee | Manual-draft persistence and failure semantics unchanged | UI calls the existing `PATCH /api/manual-draft` with the same action fields (`app.js:1793-1800,2125-2174`); `WorkspaceManualDraftIT` passed all 11 real HTTP/DB cases, including invalid and stale requests (`WorkspaceManualDraftIT.java:118-129,267-285`) | STRONG | yes |
| Requires UC-1 | Consume its approved lens | Tests enter Teacher/Room lenses through UC-1 paths; all seven UC-1 lens tests pass within the 18/18 class | STRONG | yes |
| Extends UC-1 at 5a | Extend tile selection inside an active lens | `:585-590,620-624,647-650` enters the approved lens before selection; UC-1's base journeys remain green | STRONG | yes |

## Lifecycle, Security, and Presentation

| State or surface | Applicable behavior | Evidence |
|---|---|---|
| Accepted, Repair Draft, Solving | Lens selection and inspection stay on the shared matrix; no lens action mutates the workspace | UC-1 browser journeys in `MatrixLensBrowserIT.java:30-269` run these states; `app.js:854-906,984-993` shares the render and filter path |
| Repair Proposal | Comparison cues and review targets use the same lens and may clear an excluding filter | `MatrixLensBrowserIT.java:273-301` and passing `ProposalReviewBrowserIT.java:66-89`; `app.js:412-457,1065-1077` |
| Manual Draft | Period, teacher, and room edits use the existing manual-draft command; validation and version checks remain server-owned | `MatrixLensBrowserIT.java:582-657`; `ManualDraftService.java:89-102`; `WorkspaceManualDraftIT` 11/11 |
| HTTP and local access | The submission adds no route or access-rule change. The relevant surface is `GET /workspace/**`, `GET /api/workspace`, and `PATCH /api/manual-draft`; non-GET lens actions are absent. The inherited origin filter, CSRF, and `If-Match` remain in force | Diff contains only UI/test/spec files; `SecurityConfiguration.java:17-51`, `app.js:2103-2117`, and the empty mutation logs at `MatrixLensBrowserIT.java:593,675-676` |
| Presentation | Same matrix, detail panel, keyboard conflict overlay, and status region; the detached selection has a canvas anchor. This application has no role-specific login or logout flow. Final visual acceptance awaits the administrator walkthrough | `app.js:854-906,1648-1697`, `messages.js:323`; real-browser assertions at `MatrixLensBrowserIT.java:590-592,626-637,650-657,684-686` |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | “The lens MUST be represented by the existing `teacherFilterId` and `roomId` values”; they must be mutually exclusive and no parallel field may disagree | `inspection-state.js:86-97` retains one filter-state source; `app.js:472-484,917-945,1275-1285` reads/clears those values; UC-1/UC-2 browser transitions and UC-3 class Show week passed | Pass |
| RULE-3 | `renderWeekMatrix` and `renderDayMatrix` “MUST be the only functions that produce matrix markup” for class rows and lenses; no separate lens template | `app.js:854-906` calls the same Week/Day renderers for both arrangements; `:1028-1077` supplies the same tile builders; conflict/selection browser journeys passed | Pass |
| RULE-4 | Lens cells “MUST be filled from the displayed model's assignments” and visible tiles “MUST be exactly those satisfying” `isRepresented`; Manual Draft must use its draft model | `app.js:317,856-865,922-945,1878-1886,1972-1977`; persisted draft assignment and new cell verified at `MatrixLensBrowserIT.java:596-612`, departure at `:625-637` | Pass |
| RULE-6 | Lens and Show week presentation actions “MUST NOT issue any non-GET request” and must leave stored document byte-identical | `MatrixLensBrowserIT.java:662-676,273-301` and UC-1/UC-2 lens journeys check empty mutation logs plus exact stored document; `Workbench.java:315-340` records requests | Pass |
| RULE-7 | “Every new user-visible or accessible string MUST be a `messages.js` entry”; announcements use `#inspection-notice` | `messages.js:323` defines the new departure message; `app.js:174-195,821-834,1265-1271` writes it or named-clearing text to the status region; browser checks the exact departure and clearing text at `:626,636,684-686` | Pass |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Matrix lenses UC-1 | Approved lens entry, row rendering, Proposal review | Its seven real-browser journeys in `MatrixLensBrowserIT` passed; Proposal target and both comparison sides checked at `:273-301` | Pass |
| Matrix lenses UC-2 | Approved change/clear lens and selection behavior | Its seven real-browser journeys in `MatrixLensBrowserIT` passed, including reset, scroll restoration, and lens changes | Pass |
| Timetable manual editing UC-2/UC-3 | Shared manual editor, persistence, conflicts, and overlay | `WorkspaceManualDraftIT` 11/11; UC-3 browser period/teacher/room and conflict journeys passed | Pass |
| Accepted inspection and scale | Shared matrix and filters | `AcceptedInspectionBrowserIT` 11/11; `ScaleTimingBrowserIT` 3/3. Timing references are diagnostic only | Pass |
| Repair draft/run/completion and Proposal review | Shared inspector/task area | Full-run failures are exactly the 11 named baseline IDs: Proposal review 2, repair completion 1, repair draft 4, repair run 4. The set difference from the pre-UC-3 baseline is empty; the separate Proposal resource-lens comparison test passed | No new regression |

## Findings

No critical, gap, or protocol findings. The full suite remains red in 11 existing repair/proposal browser cases. Their IDs match the recorded baseline exactly; none is in UC-3 or its required UC-1/UC-2 and manual-draft journeys.

## Walkthrough

User confirmation is not yet recorded for UC-3. The administrator walkthrough derived from this UC is:

1. Open an accepted timetable, choose a Teacher or Room lens, select a tile, and inspect its complete details and Teacher, Room, and Class “Show week” actions.
2. In Manual Draft, edit a lesson's period while it remains in the lens; confirm its selected tile appears in the new cell. Use Class “Show week” and confirm class rows, the class filter, and the selected lesson.
3. Re-enter a Teacher or Room lens and reassign a selected lesson to another teacher or room. Confirm the tile leaves the lens, the lens stays active, the inspector stays open, and the announcement names the new resource.
4. Move a lesson into a clash with another lesson of the lens entity. Confirm both tiles and conflict indicators share the cell and the overlay explains the clash.
5. With a lens active, choose an outside repair review or draft target. Confirm the excluding criterion is named as cleared and the target opens. Confirm lens/Show week actions alone have not changed the saved workspace.

Human walkthrough result: pending.

## Status Update

UC-3: `READY_FOR_CONVERGENCE` -> `PENDING_WALKTHROUGH`. UC-4 remains `NOT_STARTED`; no next implementation is eligible until UC-3 is approved. The target row now points to this report and the immutable submission commit `eb7680c`.

## Response to execute

PENDING WALKTHROUGH: UC-3 automated convergence passes; the administrator walkthrough above still needs user confirmation.
