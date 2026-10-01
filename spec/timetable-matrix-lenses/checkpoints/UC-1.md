# Use-Case Checkpoint: UC-1 - See one teacher's or room's schedule in the matrix

## Summary

- Status: READY_FOR_CONVERGENCE (revision 1)
- Base commit: 064bf64. The first submission was e840460, rejected in 69ac6e4.
- Submission commit: HEAD at convergence
- Prior convergence findings addressed: C-1, G-1, D-1 (recorded for walkthrough confirmation), K-1
- Relations verified: UC-1 is the primary root, with no Requires, Includes, or Extends. Extension points 1a (UC-4) and
  5a (UC-3) are untouched: the narrow path still renders the focused agenda until UC-4, and tile selection opens the
  unchanged inspector.

Paths are relative to `timetable-workspace/src`. `MatrixLensBrowserIT` is
`test/java/org/schoolkernel/workspace/MatrixLensBrowserIT.java`, and `app.js` is `main/resources/static/workspace/app.js`.

## Revision 1

| Finding | Resolution | Evidence |
|---|---|---|
| C-1: no Show week in the Proposal comparison inspector | The comparison inspector now renders `showWeekActions` for each distinct declared teacher and room across the accepted and proposed sides. | `app.js:1102`, `app.js:1231`; `MatrixLensBrowserIT.appliesAndLeavesLensesDuringProposalReview:271` |
| G-1: Solving lens | Added a committed test that goes through `processes.blockReplan`, not the pin steps. It covers Show week, retained `#cancel-run` and inspector, the Day lens row, removal, no mutation, and an unchanged document. | `MatrixLensBrowserIT.appliesALensWhileARepairIsSolving:236` |
| G-1: Day room-lens tile and header | The main test now asserts the Day room-lens row header, the "Room" heading, the Monday lesson set, the tile `[subject, teacher, class]` by value, the accessible name, and that no availability claim is made. | `MatrixLensBrowserIT:105-119` |
| G-1: Proposal review-target navigation into and out of a lens | A target outside the lens clears it and announces "Cleared Teacher filter". A target inside the lens keeps it and selects the represented side. | `MatrixLensBrowserIT:286-296` |
| D-1: the lens follows a changed teacher investigation | Behavior is unchanged. It is recorded as an interpretation for the user to confirm in the walkthrough (Notes). | `app.js:1537` |
| K-1: understated executed evidence | The Notes now list exactly which rewritten blocks run. | Notes below |

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Trigger: Filters disclosure teacher/room | `MatrixLensBrowserIT.pivotsTheMatrixToOneTeacherOrRoom:30` (selects at `:37`, `:91`) | PASS |
| Trigger: inspector "Show week" | Current: `appliesLensesFromInspectorAndTeacherInvestigation:138` (`:144-156`). Solving: `:236`. Proposal: `:271`. Repair Draft: `RepairDraftBrowserIT.java:295` (runs and passes). Code: `app.js:1231`, `:1239` | PASS |
| Trigger: teacher investigation "Show only matches" | `MatrixLensBrowserIT:162-170`; `app.js:1509`; `AcceptedInspectionBrowserIT.java:413` | PASS |
| UC-1 main 1-2 (choose; clear the other type first) | `MatrixLensBrowserIT:91-95`, `:152-154`; `inspection-state.js:80-82` | PASS |
| UC-1 main 3 (one lens row group, header names the entity and type) | `assertLensRowGroup`. Week: `:39`, `:93`. Day: `:79`, `:108`. Headings at `:40`, `:80`, `:109` | PASS |
| UC-1 main 4 (every assignment in its period cell; Normative tile content) | Lesson sets: `:45`, `:81`, `:96`, `:110`. Tiles by value: teacher Week `:51`, teacher Day `:82`, room Week `:97`, room Day `:111`. Accessible-name additions: `:53-56`, `:113-116`; room kept only in the accessible name `:99` | PASS |
| UC-1 main 4 (unavailable empty cells) | `:59-61`, `:87` | PASS |
| UC-1 main 5 (narrowed label, entity, unique count, removable criterion) | `:65-69` | PASS |
| UC-1 main 6 | Reading only; covered by the assertions above | PASS |
| UC-1 extension 1a | Not triggered on desktop. The narrow path is unchanged (UC-4 scope); `AcceptedInspectionBrowserIT:68-72` passes | PASS (unchanged base) |
| UC-1 extension 4a | `rendersAnEmptyLensRowGroup:178`. A lens intersected down to zero lessons keeps its group (`:194-199`). Also `AcceptedInspectionBrowserIT:259` | PASS |
| UC-1 extension 4b | `stacksClashingLessonsInOneLensCell:208` (`:218-228`) | PASS |
| UC-1 extension 4c | `:101-103`, `:117` | PASS |
| UC-1 extension 5a | Selecting a tile in a lens opens the inspector (`:144-151`, `:250-254`). UC-3 behavior itself is not implemented | PASS (entry only) |
| UC-1 G1 same surface | `:73`, `:151`, `:254`, `:283` | PASS |
| UC-1 G2 completeness | `:45-47` | PASS |
| UC-1 G3 honesty | `:59-66`, `:102`, `:117` | PASS |
| UC-1 G4 non-mutation | Mutation log and stored document compared in all seven tests: `:122`, `:172`, `:202`, `:230`, `:264-266`, `:298`, `:337` | PASS |
| UC-1 success postcondition | `assertLensRowGroup` plus the tile assertions | PASS |
| UC-1 minimal guarantee | `refusesAnUndeclaredLensEntity:304` (`:313-315`) | PASS |
| State rule 4 (undeclared ID refused), rule 5 (reload resets to NONE) | `:318-336`; `:126-133` | PASS |
| All lifecycles | Current: `:30`. Manual Draft: `:208`. Solving: `:236`. Repair Proposal: `:271` and `ProposalReviewBrowserIT.retainsBothSidesInResourceLenses:66`. Repair Draft: `RepairDraftBrowserIT:295` | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Lenses are mutually exclusive in `inspection-state.js:80-82`; `resetFilters` and `clearNarrowing` clear them (`:85-92`); `MatrixLensBrowserIT:318-336` | PASS |
| RULE-2 | `persistPreference` is unchanged; `MatrixLensBrowserIT:126-133` checks the exact preference JSON, `sessionStorage`, the URL, and reload | PASS |
| RULE-3 | `week-renderer.js:2` and `day-renderer.js:2` take row groups; one call per range comes from `matrixArrangement` (`app.js:911`); `tileFields` (`app.js:954`) is used by all three tile builders | PASS |
| RULE-4 | `matrixArrangement` (`app.js:911`) builds proposal pairs; `displayedModel()` (`app.js:308`) supplies the draft source; `MatrixLensBrowserIT:224`, `:283`; `ProposalReviewBrowserIT:66` | PASS |
| RULE-5 | `app.js:947`; `MatrixLensBrowserIT:59-61`, `:87`, `:102`, `:117` | PASS |
| RULE-6 | See G4 | PASS |
| RULE-7 | New keys are in `messages.js`. Every `M.*` key resolves except the pre-existing `savingChanges` | PASS |
| RULE-8 | A grep for `data-open-focus`, `return-matrix`, `focusedEntry`, and `focused-entry` in the static sources returns nothing. Show week replaces the entry buttons in the lesson inspector and the Proposal inspector (C-1) | PASS |
| RULE-9 | Every removed desktop focused assertion has a lens replacement (see the first submission's list). Blocks that cannot run are covered by the new `MatrixLensBrowserIT` tests (G-1) | PASS |
| RULE-10 | `applyLens` (`app.js:964`) only calls `renderWholeSchool()`; there is one pass in `matrixArrangement`; `ScaleTimingBrowserIT` is green | PASS |

## Validation

- Focused command:
  `./mvnw -q -pl timetable-workspace -am '-Dit.test=MatrixLensBrowserIT' -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false verify`.
  Result: 7 run, 0 failures, 0 errors.
- Full relevant suite: `./mvnw -q -pl timetable-workspace -am verify`. Integration tests: 113 run, 2 failures,
  9 errors, 0 skipped. Unit tests all pass. The failing set is identical to baseline `064bf64` (106 run, 2 failures,
  9 errors), confirmed with `diff`. All of those are pre-existing repair pin and inspector geometry failures after
  9294e54, as the first convergence also confirmed. `MatrixLensBrowserIT` passes 7/7 and `WorkspaceManualDraftIT`
  passes 11/11.
- Working tree impact from tests: none.
- Changed files in revision 1: `app.js` (Proposal Show week, single editor binding), `MatrixLensBrowserIT.java`,
  `checkpoints/UC-1.md`, `status.md`.

## Notes

- Rewritten regression blocks that do run:
  - `AcceptedInspectionBrowserIT` (all of them)
  - `ProposalReviewBrowserIT.retainsBothSidesInResourceLenses`
  - `RepairDraftBrowserIT:295-299`
  - `RepairCompletionBrowserIT:47-49`, before that test's pre-existing failure at `:69`
  - `ScaleTimingBrowserIT:119-121`

  Rewritten blocks that do not run, because their tests fail earlier on pre-existing defects:
  - `RepairDraftBrowserIT:139-146`, after the failure at `:56`
  - `RepairRunBrowserIT:164-168`, after the failure at `:133`
  - `ProposalReviewBrowserIT:250-260`, after the failure at `:138`

  The new `MatrixLensBrowserIT` Solving and Proposal tests cover their lens behavior.
- Fix outside the lens code that RULE-4 requires: `applyFiltersInPlace`, `filteredAssignments`, and
  `representedAssignment` now match against `displayedModel()`, which means manual-draft assignments in manual Draft
  mode. Before this, they used the accepted model.
- Second fix outside the lens code, found by the revision-1 full run: `selectLesson` bound the inline manual editor
  twice, once directly and once through `bindCloseDetails`. As a result, every edit sent two
  `PATCH /api/manual-draft` requests, and the second one landed after the test's draft snapshot. `bindManualEditor`
  now binds each form once (a `WeakSet` guard). The ext 4b test now also asserts that the setup edit issues exactly
  one `PATCH`. UC-3 ("exactly the manual-draft `PATCH` requests that the edits require") depends on this.
- Interpretation D-1, to confirm in the walkthrough: while "Show only matches" holds the investigated teacher's lens,
  changing the investigated teacher moves the lens to the new teacher, and clearing the investigation clears the lens
  (`app.js:1537`). The checkbox shows as checked whenever the Teacher filter equals the investigated teacher
  (`app.js:874`). This preserves the approved behavior of the old teacher-only mode.
- Proposal-mode Show week only lists teachers and rooms that the accepted definition declares, because those are the
  only IDs the lens state accepts (state rule 4). When both sides share a teacher or room, only one action is shown.

READY FOR CONVERGENCE: UC-1
