# Use-Case Checkpoint: UC-1 - See one teacher's or room's schedule in the matrix

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: 064bf64
- Submission commit: HEAD at convergence
- Relations verified: UC-1 is the primary root (no Requires/Includes/Extends). The extension points 1a (UC-4) and 5a
  (UC-3) are untouched. The narrow path still renders the focused agenda until UC-4, and tile selection opens the
  unchanged inspector.

Paths below are relative to `timetable-workspace/src`. `MatrixLensBrowserIT` is
`test/java/org/schoolkernel/workspace/MatrixLensBrowserIT.java`, and `app.js` is `main/resources/static/workspace/app.js`.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Trigger: Filters disclosure teacher/room | `MatrixLensBrowserIT.pivotsTheMatrixToOneTeacherOrRoom` (`:30`, filter selects at `:37`, `:90`) | PASS |
| Trigger: inspector "Show week" | `MatrixLensBrowserIT.appliesLensesFromInspectorAndTeacherInvestigation` (`:122`); `app.js:1229` `showWeekActions`, `:1234` `bindShowWeek` | PASS |
| Trigger: teacher investigation "Show only matches" | same test, `#teacher-only` check/uncheck; `app.js:1504`; `AcceptedInspectionBrowserIT.java:413` | PASS |
| UC-1 main 1-2 (choose; clear other type first) | `MatrixLensBrowserIT:90-94` asserts that the room lens clears `#teacher-filter` and the teacher criterion; `inspection-state.js:80-82` | PASS |
| UC-1 main 3 (one lens row group; header names entity and type) | `assertLensRowGroup` for Week (`:38`, `:92`) and Day (`:79`); header "Teacher"/"Room" at `:39`, `:80` | PASS |
| UC-1 main 4 (every assignment in its period cell; Normative tile content) | Visible lesson set equals lessons 960-999 (`:45`); Week tile `[subject, room, class]` (`:51`) and Day tile (`:82`); room-lens tile `[subject, teacher, class]` with the room kept only in the accessible name (`:96-99`); accessible name adds teacher, weekday, period, and ID (`:53-56`) | PASS |
| UC-1 main 4 (unavailable empty cells) | Exactly periods 41-59 are unavailable, period 40 is ordinary (`:59-60`), text cue and title (`:61`), all of Friday is unavailable in Day (`:87`) | PASS |
| UC-1 main 5 (narrowed label, entity, unique count, removable criterion) | `:65-69` (`#filter-title`, `#matrix-summary` "Lens: Teacher · …", "Represented lessons: 40", remove-control aria-label) | PASS |
| UC-1 main 6 | Read-only reading; covered by the rendered assertions above | PASS |
| UC-1 extension 1a | Not triggered on desktop. The narrow path is unchanged (UC-4 scope). `AcceptedInspectionBrowserIT:68-72` still passes | PASS (unchanged base) |
| UC-1 extension 4a | `MatrixLensBrowserIT.rendersAnEmptyLensRowGroup` (`:162`): room-99 has a full 60-cell empty group, zero lessons, and reset offered. A lens intersected to zero keeps its group (`:178-182`). `AcceptedInspectionBrowserIT:259` | PASS |
| UC-1 extension 4b | `MatrixLensBrowserIT.stacksClashingLessonsInOneLensCell` (`:192`): a manual-draft `ROOM_CLASH` stacks lesson-0 and lesson-960 in the period-0 room-16 cell, both `.conflicting` with an indicator, and the overlay shows `ROOM_CLASH` | PASS |
| UC-1 extension 4c | `:101-103`: room-16 without `availablePeriodIds` renders only ordinary empty cells | PASS |
| UC-1 extension 5a | Tile selection opens the existing inspector inside the lens (`:128-136`). Full UC-3 behavior is not implemented here | PASS (entry only) |
| UC-1 G1 same surface | `:73` has no `.focused-schedule`, `#return-matrix`, `[data-open-focus]`, or `.focused-entry`. The inspector stays open with the selection across Show week (`:133`) | PASS |
| UC-1 G2 completeness | `:45-47`: the set is identical with the matrix scrolled to the start and the end | PASS |
| UC-1 G3 honesty | Narrowed title, lens summary, text-cued unavailability (`:59-66`), no claim without declared availability (`:102`) | PASS |
| UC-1 G4 non-mutation | `recordMutations()` is empty and `storedDocument()` is equal in all five tests (`:106-107`, `:156`, `:186`, `:214`, `:253`) | PASS |
| UC-1 success postcondition | Exactly one lens row group with lens tiles (`assertLensRowGroup` plus the tile assertions) | PASS |
| UC-1 minimal guarantee | `MatrixLensBrowserIT.refusesAnUndeclaredLensEntity` (`:220`): an injected undeclared teacher option is refused. The room lens, selects, and rows are unchanged | PASS |
| State model rule 4 (undeclared refused), rule 5 (reload → NONE) | `:238-251` (real module in the browser); `:112-116` (reload) | PASS |
| All lifecycles | Current (above), Manual Draft (`:192`), Repair Proposal (`ProposalReviewBrowserIT.retainsBothSidesInResourceLenses:66`), Solving (`RepairRunBrowserIT.java:164`, see Notes), Draft (`RepairDraftBrowserIT.java:139`, `:295`, see Notes), scale Proposal (`ScaleTimingBrowserIT.java:119-121`) | PASS / see Notes |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 lens is the filter state | `inspection-state.js:80-82` (mutual exclusion); `resetFilters`/`clearNarrowing` clear both (`:85-92`); `MatrixLensBrowserIT:238-251`; no parallel lens field | PASS |
| RULE-2 never persisted | `persistPreference` is unchanged (`inspection-state.js:140`); `MatrixLensBrowserIT:110-116` checks the exact preference JSON, empty `sessionStorage`, and a clean URL | PASS |
| RULE-3 one renderer path | `week-renderer.js:2`, `day-renderer.js:2` take `rows`/`cellItems`/`emptyMarkup`; a single call per range in `renderWholeSchool` (`app.js` `renderWholeSchool`, spread of `matrixArrangement`); tiles use `tileFields` (`app.js:954`) in all three tile builders | PASS |
| RULE-4 population | `app.js:911` `matrixArrangement` (entity assignments, proposal pairs via `comparison.entries`); visibility via `isRepresented` in `applyFiltersInPlace`; draft source via `displayedModel()` (`app.js:308`); `ProposalReviewBrowserIT:66` both sides under a teacher and a room lens | PASS |
| RULE-5 availability | `app.js:947` `emptyCellMarkup`, `availability` in `matrixArrangement`; `MatrixLensBrowserIT:59-61`, `:87`, `:102` | PASS |
| RULE-6 no mutation | See G4 | PASS |
| RULE-7 localization | New keys in `messages.js` (`lensSummary`, `unavailableCell`, `outsideAvailability`, `showWeek`, `showWeekOf`, `removeCriterion`); every `M.*` key used by `app.js` resolves (`node` check, except the pre-existing `savingChanges`) | PASS |
| RULE-8 removal split | `grep -rn "data-open-focus\|return-matrix\|focusedEntry\|focused-entry" main/resources/static` returns nothing; `renderFocused()` is reachable only on `view.narrow` paths | PASS |
| RULE-9 tests rewritten | Every removed desktop focused assertion has a lens replacement: `AcceptedInspectionBrowserIT:61-66`, `:147-151`, `:259`, `:267-270`, `:413`, `:465-480`, `:492`, `:513`; `ProposalReviewBrowserIT:66`, `:250-260`; `RepairCompletionBrowserIT:47-49`; `RepairDraftBrowserIT:139-146`, `:295-299`; `RepairRunBrowserIT:164-168`; `ScaleTimingBrowserIT:119-121`. `Workbench.openFocus`/`returnToMatrix` were removed; `showWeek`/`removeLens` were added | PASS |
| RULE-10 scale | Lens transitions call only `renderWholeSchool()` (`app.js:964` `applyLens`); a single pass over the assignments in `matrixArrangement`; `ScaleTimingBrowserIT` 3/3 green | PASS |

## Validation

- Focused commands: `./mvnw -q -pl timetable-workspace -am '-Dit.test=MatrixLensBrowserIT' ... verify`. MatrixLensBrowserIT
  5 run, 0 failures, 0 errors.
- Full relevant suite: `./mvnw -q -pl timetable-workspace -am verify`. Integration tests: 111 run, 2 failures,
  9 errors, 0 skipped. Unit tests all pass.
- Baseline at 064bf64, same command in a clean worktree: 106 run, 2 failures, 9 errors. The failing test set is
  byte-identical, confirmed with `diff`:
  - `ProposalReviewBrowserIT.reviewsIndependentlyVerifiedNormativeRepair`
  - `ProposalReviewBrowserIT.revisesAndDiscardsVerifiedNormativeRepair`
  - `RepairCompletionBrowserIT.completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessor`
  - `RepairDraftBrowserIT.preparesProtectedRepairDraftWithKeyboard`
  - `RepairDraftBrowserIT.preparesWideProtectedDraftAtNormativeScale`
  - `RepairDraftBrowserIT.refusesSolveAfterRealDraftPersistenceFailure`
  - `RepairDraftBrowserIT.resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources`
  - `RepairRunBrowserIT.followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshot`
  - `RepairRunBrowserIT.generatesAndAcceptsSuccessiveRepairProposals`
  - `RepairRunBrowserIT.inspectsFrozenRepairRunAndRecoversWithoutPublishing`
  - `RepairRunBrowserIT.showsFailedRepairEvidenceAndGatedRetry`

  All of them fail on repair pin controls (`[name=lesson-dimension]`, `#apply-pin`) that are not visible, or on
  inspector geometry, after the inspector-popover redesign in 9294e54. None of them reaches a lens step before failing.
- Working tree impact from tests: none. `git status --short` is the same before and after the runs.
- Runtime evidence: headless Chrome through Playwright against an ephemeral Spring Boot server and PostgreSQL 18.6
  Testcontainers, as described above.
- Changed files:
  - `spec/timetable-matrix-lenses/rules.md`, `status.md`, `checkpoints/UC-1.md`
  - `timetable-workspace/src/main/resources/static/workspace/app.js`, `inspection-state.js`, `week-renderer.js`,
    `day-renderer.js`, `focused-renderer.js`, `messages.js`, `styles.css`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/MatrixLensBrowserIT.java` (new),
    `AcceptedInspectionBrowserIT.java`, `ProposalReviewBrowserIT.java`, `RepairCompletionBrowserIT.java`,
    `RepairDraftBrowserIT.java`, `RepairRunBrowserIT.java`, `ScaleTimingBrowserIT.java`, `Workbench.java`
- Approved UCs regression-tested: no UC of this feature is approved yet. Other features are covered by the full suite
  above.

## Notes

- Rewritten regression assertions that do not run: in `RepairDraftBrowserIT` (`:139`, `:295`), `RepairRunBrowserIT`
  (`:164`), and `ProposalReviewBrowserIT.reviewsIndependentlyVerifiedNormativeRepair` (`:250-260`), the tests fail
  earlier on the pre-existing pin/geometry defects listed above. The rewritten lens steps in those tests are therefore
  not executed. The lens behavior they cover is exercised by `MatrixLensBrowserIT`, which covers Current and Manual
  Draft, and by `ProposalReviewBrowserIT.retainsBothSidesInResourceLenses` and `ScaleTimingBrowserIT`, which cover
  Proposal. There is no direct Repair Draft or Solving lens evidence until the pre-existing failures are fixed.
- Fix outside the lens code, needed by RULE-4: `applyFiltersInPlace`, `filteredAssignments`, and `representedAssignment`
  used to match filters against the accepted model in manual Draft mode. They now use `displayedModel()`. Without this,
  a lens or filter hid lessons that the draft had moved into the entity.
- Interpretation of resolved decision 5: while "Show only matches" holds the investigated teacher's lens, changing the
  teacher investigation moves the lens to the new teacher, and clearing it clears the lens (`app.js:1532`). This
  keeps the approved behavior of the old teacher-only filter mode and never leaves the checkbox unchecked while its lens
  is still active.
- Show week is offered in the lesson inspector. It is not offered in the Proposal-mode comparison inspector, which
  shows two sides with possibly different teachers and rooms. The Filters disclosure still applies lenses in Proposal
  mode.
- The Day renderer's class-row markup is unchanged. The Week renderer adds a hidden empty-cell span only to lens rows.
  This lets a cell whose tiles are all filtered out show its empty or unavailable cue without changing class-row
  counts.

READY FOR CONVERGENCE: UC-1
