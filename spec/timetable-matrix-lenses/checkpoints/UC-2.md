# Use-Case Checkpoint: UC-2 - Change or clear the lens

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: 002cc2f (UC-1 APPROVED)
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-1. The UC-2 tests enter lenses through the approved UC-1 paths (the Filters selects,
  the inspector Show week, and the lens criterion), so they consume UC-1's postcondition of one lens row group.
  `MatrixLensBrowserIT`'s UC-1 tests pass unchanged.

Paths are relative to `timetable-workspace/src`. `MatrixLensBrowserIT` is
`test/java/org/schoolkernel/workspace/MatrixLensBrowserIT.java`.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-2 main 1 (remove the criterion, or choose "All teachers"/"All rooms") | Criterion ×: `MatrixLensBrowserIT.clearsALensBackToTheRecordedScroll:362`. "All teachers" select: `:373`, `:400` | PASS |
| UC-2 main 2 (class rows under the remaining filters) | `:364`, `:375` (60 class row groups); `:401-402` (17 visible class rows under the retained period filter) | PASS |
| UC-2 main 3 (retain range, weekday, search, highlights, class and period filters, selection) | `clearsALensRetainingTheInvestigativeContext:384`, assertions at `:403-406`. The class filter is retained in `AcceptedInspectionBrowserIT.clearsAnEmptyRoomLensToTheRetainedContext` and in the rewritten `:147-151` | PASS |
| UC-2 main 4 (restore the entry scroll, or bring a represented selection into view) | Entry scroll restored exactly: `:364`, `:375`, `:471`, `:483` (`assertClassRowsRestoredAt:490`). Selection brought into view with `scrollTop > 0` and the tile inside the matrix viewport: `:407-408` | PASS |
| UC-2 main 5 (complete or narrowed status) | Complete: `:365-366`, `:472-473`. Narrowed with the remaining period criterion only: `:409-411` | PASS |
| UC-2 extension 1a (same type, different entity) | `:357-359` (teacher-16 → teacher-3, scroll entry kept); `changesTheLensEntityOrType:419` (room-16 → room-3, lesson not in the room, selection cleared) | PASS |
| UC-2 extension 1b (other type) | `:360-361`; `:430-433` (the teacher lens is replaced by a room lens that keeps a lesson belonging to it, with no notice); `:442-447` (room → teacher) | PASS |
| UC-2 extension 1c (reset filters) | `resetsTheLensWithEveryNarrowingCriterion:455`. Reset clears the lens, class, period, and search, keeps the range, returns the subject to highlight mode (inspection-ux rule 7), and restores the scroll (`:470-474`). Clear narrowing through the empty-lens reset offer keeps the search (`:476-484`) | PASS |
| UC-2 extension 3a (unrepresented selection cleared and announced) | `:435-440` ("The selected lesson is not in the Room · Room 3 lens, so its details were closed."); `:445-447` | PASS |
| UC-2 G1 context retention | `:403-404` | PASS |
| UC-2 G2 no surface change | `:377`. No return control or focused surface exists (RULE-8 grep, unchanged since UC-1) | PASS |
| UC-2 success postcondition | New lens or class rows with the retained context: the assertions above | PASS |
| UC-2 minimal guarantee | Self-consistent state: lens selects, criteria, and rows agree after every transition (`:431-433`, `:403`, `:473`). Authoritative data untouched: mutation log empty and stored document equal in all four tests | PASS |
| State model rules 1-3 | Rule 1: `:403-406`. Rule 2: `:364`, `:407-408`. Rule 3: `:470-474` | PASS |
| Requires UC-1 | Lenses are entered through UC-1 triggers; the UC-1 tests in the same class stay green | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `inspection-state.js:30-37`: `leaveOrEnterLens` is the single transition helper for `selectFilter`, `resetFilters`, and `clearNarrowing` (`:92-97`). There is still no parallel lens field. `lensScrollContext` is the lens-entry scroll position that the design overview names | PASS |
| RULE-2 | `persistPreference` is unchanged and writes only `{version, range, weekdayId}`. `lensScrollContext` is never stored. `MatrixLensBrowserIT:126-133` passes | PASS |
| RULE-3 | No renderer change; class rows and lens rows still share `renderWeekMatrix` and `renderDayMatrix` | PASS |
| RULE-4 | The selection rule in 1a uses the existing `clearSelectedLessonOutsideRepresentation` (`isRepresented`) | PASS |
| RULE-6 | `recordMutations()` is empty and `storedDocument()` is equal in `:345`, `:384`, `:419`, `:455` | PASS |
| RULE-7 | New message `selectionOutsideLens` (`messages.js:322`), written to `#inspection-notice` (`app.js:971-972`) | PASS |
| RULE-10 | Clearing renders through `renderWholeSchool()` once and then `settleMatrixScroll` (`app.js:983`); there is no refetch. `ScaleTimingBrowserIT` result is in `status.md` | PASS |

## Validation

- Focused command:
  `./mvnw -q -pl timetable-workspace -am '-Dit.test=MatrixLensBrowserIT' -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false verify`.
  Result: 11 run, 0 failures, 0 errors.
- Mutation check: with `settleMatrixScroll` turned into a no-op, the UC-2 tests fail. There are two failures at
  `assertClassRowsRestoredAt`, and `clearsALensRetainingTheInvestigativeContext` fails at its in-view assertion
  (`:407`). The production file was restored afterwards, and `git diff` confirms it.
- Full relevant suite: `./mvnw -q -pl timetable-workspace -am verify`. The result is in `status.md`.
- Working tree impact from tests: none.
- Changed files: `main/resources/static/workspace/inspection-state.js`, `app.js`, `messages.js`;
  `test/java/org/schoolkernel/workspace/MatrixLensBrowserIT.java`; `spec/timetable-matrix-lenses/checkpoints/UC-2.md`,
  `status.md`, plus the UC-1 approval record committed before this work in 002cc2f.
- Approved UCs regression-tested: UC-1, through all seven of its `MatrixLensBrowserIT` tests and the rewritten
  regression ITs in the full suite.

## Notes

- Scroll restoration covers the matrix's own scroll container, `.matrix-wrap`. A represented selection takes priority
  over the recorded position, as state rule 2 says.
- Lens-to-lens changes (1a and 1b) keep the position recorded when the first lens was entered from class rows. Review
  targets that clear a lens (UC-3 extension 1a) drop the recorded position, because the target is scrolled into view
  instead.
- Reset keeps its approved behavior from `timetable-inspection-ux` and also clears the search. The empty-lens reset
  offer (`#reset-empty`) and "Clear filters" keep the search.

READY FOR CONVERGENCE: UC-2
