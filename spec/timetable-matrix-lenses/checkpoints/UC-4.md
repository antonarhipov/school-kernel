# Use-Case Checkpoint: UC-4 - Read a lens on a narrow screen

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: 218adfe2a8bd47b8806160ea6caeba0484eead94
- Submission commit: HEAD at convergence
- Relations verified: UC-4 extends approved UC-1 at its 1a narrow-viewport branch. The narrow picker applies UC-1's teacher and room filter transitions and feeds their row arrangement to the existing Day matrix renderer. The UC-1 and UC-2 matrix-lens browser cases are in the 20-case `MatrixLensBrowserIT` regression run.

Paths below use `timetable-workspace/src` as the root. `app.js`, `inspection-state.js`, and `styles.css` are in `main/resources/static/workspace/`; browser ITs are in `test/java/org/schoolkernel/workspace/`.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1: Day at the last valid weekday; first class unless an explicit filter or lens is active | `MatrixLensBrowserIT.readsOneEntityDayOnANarrowScreen:30-40,78-94` proves the initial Monday class, saved Friday after reload, preservation of an already-active teacher lens, and desktop Week restoration; `preservesAnExplicitClassWhenNarrowing:104-112` proves an existing class filter wins. `app.js:461-463,2024-2025` derives the narrow Day and implicit first class without writing the desktop range. | PASS |
| Main 2: narrow notice, lifecycle, no edit/cancel/decision control | `MatrixLensBrowserIT.java:43-47` checks the 390 px read-only notice, disabled tiles, absent desktop and mutation controls; `app.js:507-510` builds the lifecycle-labelled narrow surface for timetable-bearing lifecycle modes. `ProposalReviewBrowserIT.java:56-65` checks Proposal and absent decisions. | PASS |
| Main 3: choose class, teacher, room, and weekday | `MatrixLensBrowserIT.java:49-73,104-124` drives the native picker and weekday controls in Chrome. The narrow control transitions are `app.js:920-960`, reusing `applyLens` for teacher and room. | PASS |
| Main 4: one readable Day row group, normative tiles, horizontal scroll | `MatrixLensBrowserIT.java:39-55,67-73,95-97` checks the exact class, teacher, and room tile fields and row headers. `:43-45` checks a scrollable matrix, no page overflow, and a period-cell width of at least 150 px at 390 px. The class and populated teacher screenshots under `target/workbench-layout/matrix-lenses-uc4-*-390.png` were visually inspected. | PASS |
| Extension 3a: empty chosen weekday | `MatrixLensBrowserIT.java:58-62` proves teacher-16 has zero Friday tiles and every declared-unavailable period is marked by text; `:113-117` proves an empty class day has only ordinary empty cells. `:73` checks a room without declared availability. | PASS |
| G1: Day matrix renderer only | `app.js:934` calls `renderDayMatrix`; `day-renderer.js:2-10` is the same renderer used on desktop; the focused renderer and all focused entry/state code are removed. The browser checks `.matrix-wrap` and absence of `.focused-schedule` at `MatrixLensBrowserIT.java:43-47`. | PASS |
| G2: no desktop claim or mutation | The notice explicitly says read-only and does not claim the desktop workflow (`messages.js`); `MatrixLensBrowserIT.java:43-47,75-76,98-99` checks disabled tiles, absent mutation controls, no non-GET request, and an unchanged stored document. `ProposalReviewBrowserIT.java:56-65` covers proposal decisions. | PASS |
| G3: desktop range remains stored | `MatrixLensBrowserIT.java:63-65,78-86` compares the exact three-key preference after choosing a narrow weekday, reloads to Friday, and returns to the complete desktop Week. `inspection-state.js:111-116,132-136` changes the weekday without changing the stored range. | PASS |
| Success postcondition | `MatrixLensBrowserIT.java:39-45,49-73,95-97` and the inspected 390 px images establish a readable, single-row class/teacher/room Day matrix. | PASS |
| Minimal guarantee | `MatrixLensBrowserIT.java:63-65,75-86,98-99,125-126` checks the stored Week preference and byte-identical workspace document across picker, weekday, reload, and viewport transitions. | PASS |
| Extends UC-1 at 1a | `app.js:853-856,920-960` branches at 700 px and passes UC-1's lens row arrangement through `renderDayMatrix`; the 20-case matrix-lens class checks the approved wide Week/Day behavior and the new narrow branch. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `app.js:950-960` uses `teacherFilterId` / `roomId` through `inspectionState.selectFilter`; `inspection-state.js:86-96` keeps them exclusive and rejects undeclared IDs. `MatrixLensBrowserIT.java:49-73,118-124` uses the picker. | PASS |
| RULE-2 | `inspection-state.js:125-136` accepts and writes exactly `version`, `range`, `weekdayId`; `MatrixLensBrowserIT.java:63-65,78-86` checks the exact value and lens reset on reload. | PASS |
| RULE-3 | Narrow calls the existing `renderDayMatrix` at `app.js:934`; tiles use `lessonButton` / `comparisonLessonButton` at `app.js:1090-1120`. No separate matrix or lens renderer remains. | PASS |
| RULE-5 | `app.js:972-1005` supplies availability-aware empty markup only for an empty declared lens cell; `MatrixLensBrowserIT.java:58-62,73,113-117` tests declared, undeclared, and class cells. | PASS |
| RULE-6 | `MatrixLensBrowserIT.java:75-76,98-99,125-126` checks empty mutation logs and the exact stored document. The picker code at `app.js:935-960` only changes presentation state. | PASS |
| RULE-7 | `messages.js` supplies the narrow picker label, Day matrix label, and notice. `app.js:920-934` uses those entries and escapes entity option names through `selectControl`; `MatrixLensBrowserIT.java:43-56` checks the visible status and matrix accessible label by value. The approved UC-1 accessible tile-name checks remain green in the 20-case matrix-lens class. | PASS |
| RULE-8 | `focused-renderer.js` is deleted; `renderFocused`, focused state functions, and unused message keys are removed. Production search for their names returns no matches. `MatrixLensBrowserIT.java:43-47` checks the focused surface is absent. | PASS |
| RULE-9 | Focused-agenda assertions were replaced with Day-matrix assertions in `AcceptedInspectionBrowserIT`, `ProposalReviewBrowserIT`, `RepairCompletionBrowserIT`, `RepairDraftBrowserIT`, and `RepairRunBrowserIT`; `Workbench.focusEntity` became the narrow picker helpers. The unchanged parts of those regression journeys remain in their original methods. | PASS |

## Validation

- Focused commands: `./mvnw -q -pl timetable-workspace -am '-Dit.test=MatrixLensBrowserIT#readsOneEntityDayOnANarrowScreen+preservesAnExplicitClassWhenNarrowing' -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false verify` passed 2/2 on the final test source. A targeted Proposal comparison rerun passed 1/1 after correcting the fixture lesson ID.
- Full relevant suite: `./mvnw -q -pl timetable-workspace -am verify` ran 126 workspace integration tests: 2 failures, 9 errors, 0 skipped. All 11 failing method IDs exactly match the pre-UC-4 baseline in `convergence/UC-1.md`; they fail in the same repair/proposal pin and inspector paths. The full run preceded only the final saved-weekday/preexisting-lens assertions added to the already-passing UC-4 browser test; the production source was unchanged, and those final assertions passed in the focused rerun.
- Working tree impact from tests: none; `git status --short` contains only the intended UC-4 source, test, and evidence files, and `git diff --check` passes.
- Runtime evidence: real headless Chrome, Spring Boot server, and isolated PostgreSQL 18.6 Testcontainer; both 390 px screenshots have readable period tiles within the scrollable matrix.
- Changed files: `app.js`, deleted `focused-renderer.js`, `inspection-state.js`, `messages.js`, `styles.css`; `AcceptedInspectionBrowserIT.java`, `MatrixLensBrowserIT.java`, `ProposalReviewBrowserIT.java`, `RepairCompletionBrowserIT.java`, `RepairDraftBrowserIT.java`, `RepairRunBrowserIT.java`, `Workbench.java`; this checkpoint and `status.md`.
- Approved UC regression: `MatrixLensBrowserIT` passed 20/20, `AcceptedInspectionBrowserIT` 11/11, `WorkspaceManualDraftIT` 11/11, and `ScaleTimingBrowserIT` 3/3 in the full run. Proposal and repair browser failures are exactly the 11 baseline methods listed in `convergence/UC-1.md`.

## Notes

The full workspace suite has an 11-test pre-UC-4 repair/proposal browser failure baseline documented in `convergence/UC-1.md` and `convergence/UC-3.md`. This run's failing-ID set is identical, with no new failure. The UC-3 walkthrough remains pending and is not treated as an approval.

READY FOR CONVERGENCE: UC-4
