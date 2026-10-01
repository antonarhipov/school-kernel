# Use-Case Checkpoint: UC-3 - Inspect and edit lessons within a lens

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: fee1bb5 (UC-2 revision 1)
- Submission commit: HEAD at convergence
- Relations verified: UC-3 requires UC-1 (APPROVED) and extends UC-1 at 5a. Every UC-3 test enters through an approved
  UC-1 lens path (the Filters selects or Show week) and then selects a tile, which is the 5a branch point. The UC-1
  tests in `MatrixLensBrowserIT` stay green, so the base UC is unchanged.
- Process deviation, approved by the user on 2026-10-01: UC-3 started while UC-2 revision 1 had not yet converged.
  The user then stopped the UC-2 reconvergence run, so UC-2 has no automated revision-1 verdict yet. The user
  reported the UC-2 walkthrough as good.

Paths are relative to `timetable-workspace/src`. `MatrixLensBrowserIT` means
`test/java/org/schoolkernel/workspace/MatrixLensBrowserIT.java`, and `app.js` means
`main/resources/static/workspace/app.js`.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-3 main 1-2 (select a lens tile; full details; Show week for teacher, room, class) | `MatrixLensBrowserIT.editsALessonInsideItsLens:582`, at `:590-592`. Class action: `app.js:1268` | PASS |
| UC-3 main 3-4 (edit through the existing editor; apply, validate, persist, re-render) | `:596-601`: exactly one `PATCH /api/manual-draft`, lifecycle `MANUAL_DRAFT`, stored draft assignment `period-40` | PASS |
| UC-3 main 5 (still represented; new lens cell; selection retained) | Week: `:597-598`, where the tile is in the `Declared period 40` cell with `aria-pressed=true`, the inspector is open, and the lens is unchanged. Day, editing across weekdays: `:604-611`, where the Day follows to Thursday inside the lens through `inspectionState.selectDay` (`app.js:1789`) | PASS |
| UC-3 extension 1a (review target, diagnostic, or draft link outside the lens) | Draft direct-effect link: `leavesTheLensForAClassWeekOrADraftLink:662`, at `:684-686`. The room lens is cleared, the notice names "Cleared Room filter", and the target is selected (`app.js:825`). Proposal review target: `appliesAndLeavesLensesDuringProposalReview` (UC-1 suite) clears with "Cleared Teacher filter". The conflict overlay has no lesson links, because competing lessons are listed as IDs only, so that trigger cannot arise | PASS |
| UC-3 extension 2a (teacher/room Show week continues with UC-2 1a/1b; class Show week clears the lens, applies the class filter, keeps the selection) | Class: `:671-674` (`app.js:1275` `showClassWeek`). Teacher/room: `changesTheLensEntityOrType` (UC-2 suite) and `appliesLensesFromInspectorAndTeacherInvestigation` (UC-1 suite) | PASS |
| UC-3 extension 5a (reassigned out of the lens) | `keepsALessonEditedOutOfTheLens:617`. Teacher reassignment (`:625-629`): the tile is removed from the lens, the lesson stays selected and inspected (the popover is anchored to the canvas, `app.js:1694`), and the notice names the new teacher ("…its teacher is now Teacher 17…"). The lens stays `teacher-16`. Room reassignment: `:633-637`. Code: `app.js:174`, `:948` | PASS |
| UC-3 extension 5b (edit clashes with another lesson of the lens entity) | `stacksALensClashCausedByAnEdit:644`: lesson-963 moved to period-4 shares the cell with lesson-964. Both carry `.conflicting` and an indicator, and the overlay shows `TEACHER_CLASH` (`:651-656`) | PASS |
| UC-3 G1 parity | The same tile, inspector, editor, conflict indicator, and overlay code runs in a lens as on class rows (RULE-3). The behavior is shown by `:597`, `:652-655`, and the UC-1 ext 4b test | PASS |
| UC-3 G2 no silent disappearance | `:626-629`, `:636-637` | PASS |
| UC-3 G3 non-mutation by presentation | Selecting and inspecting: `:593`, no requests. Class Show week: `:675-676`, no requests and the document is unchanged. Edits issue exactly one `PATCH` each (`:599`, `:612`, `:639`) | PASS |
| UC-3 success postcondition | Inspection and editing are done without leaving the matrix (all of the above) | PASS |
| UC-3 minimal guarantee | Manual-draft persistence is unchanged: `WorkspaceManualDraftIT` 11/11 (see Validation) | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | The lens stays in `teacherFilterId`/`roomId`. Class Show week clears the lens filters through `selectFilter` (`app.js:1275`) | PASS |
| RULE-3 | No new renderer or tile builder. The detached lesson reuses the existing inspector, anchored differently | PASS |
| RULE-4 | Draft-mode lens cells come from `displayedModel()`. The edited lesson leaves or enters cells according to the persisted draft (`:597`, `:626`) | PASS |
| RULE-6 | Presentation actions issue no requests. Only edits do, one `PATCH` each | PASS |
| RULE-7 | `lessonLeftLens` (`messages.js:323`). Draft-link clearing reuses `clearedReviewFilter` with the existing filter labels. Both write to `#inspection-notice` | PASS |

## Validation

- Focused command:
  `./mvnw -q -pl timetable-workspace -am "-Dit.test=MatrixLensBrowserIT#editsALessonInsideItsLens+keepsALessonEditedOutOfTheLens+stacksALensClashCausedByAnEdit+leavesTheLensForAClassWeekOrADraftLink" ... verify`.
  Result: 4 run, 0 failures, 0 errors.
- Mutation checks. The production file was restored afterwards and confirmed with `cmp`.
  - A: with the lens departure disabled, `keepsALessonEditedOutOfTheLens` fails.
  - B: with the cross-day fix reverted to direct `view.day` mutation, `editsALessonInsideItsLens` fails ("Browser did
    not render: Day · Thursday").
- Whole class: `./mvnw -q -pl timetable-workspace -am '-Dit.test=MatrixLensBrowserIT' -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false verify`.
  Result: 18 run, 0 failures, 0 errors, 0 skipped.
- Full relevant suite: `./mvnw -pl timetable-workspace -am verify`. Result: 124 integration tests, 4 failures, 9 errors,
  0 skipped.
  - The 11 baseline failures are unchanged: `RepairRunBrowserIT` 4, `RepairCompletionBrowserIT` 1,
    `ProposalReviewBrowserIT` 2, and `RepairDraftBrowserIT` 4.
  - Two UC-2 tests are flaky on their first render: `resetsTheLensWithEveryNarrowingCriterion` and
    `returnsShowOnlyMatchesModesToHighlightsOnReset`. Both time out at their first
    `open().awaitText("Showing 60 of 60 classes")` (`:458`, `:495`), before any lens or UC-3 code runs. The page
    dump taken after the timeout already contains that text, so the 1,000-lesson first render is just over the
    10 s Playwright limit.
  - The same flake reproduces on clean `fee1bb5`, without UC-3. In a scratch worktree,
    `resetsTheLensWithEveryNarrowingCriterion` failed once at `:458` and passed once in two runs. Both tests pass in
    the isolated whole-class run.
- `AcceptedInspectionBrowserIT` 11/11, `ScaleTimingBrowserIT` 3/3, `WorkspaceManualDraftIT` 11/11.
- Working tree impact from tests: none.
- Changed files: `main/resources/static/workspace/app.js`, `messages.js`; `MatrixLensBrowserIT.java`;
  `checkpoints/UC-3.md`; `status.md`.
- Approved UCs regression-tested: UC-1, through every UC-1 test in `MatrixLensBrowserIT` plus the rewritten
  regression ITs in the full suite. UC-2 (pending) also has its tests in the same class.

## Notes

- Fixed a pre-existing defect that UC-3 main 5 needed. A cross-day edit or revert in Day used to set `view.day`
  directly, so the next render reset it from the state module. The view stayed on the old day while the lesson had
  moved. Both paths now go through `inspectionState.selectDay`, and the weekday preference is persisted the same way
  as manual day changes.
- 5a only covers lens departures that an edit causes. `editedLessonId` marks the lesson whose save is in flight. A
  lesson that leaves the representation for another reason (for example a period filter) keeps the existing
  "outside the active filters" behavior.
- A detached selection (edited out of the lens) has no tile to anchor the inspector popover. The popover opens at
  the canvas's top-right instead, and is still dismissible as usual.
- The UC-2 first-render flake described under Validation exists before UC-3. Raising the initial-render timeout in
  `Workbench` would fix it, but that is outside UC-3 and has not been done.
- Process hygiene incident: for about 20 minutes, the uncommitted UC-3 edits sat in the repository working tree
  while a UC-2 verifier run was active. The user stopped that run before it reported, so no evidence was graded
  from the mixed tree.

READY FOR CONVERGENCE: UC-3
