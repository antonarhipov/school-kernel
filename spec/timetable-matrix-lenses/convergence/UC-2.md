# Convergence: UC-2 - Change or clear the lens

## Summary

- Submission: `spec/timetable-matrix-lenses/checkpoints/UC-2.md` at `013c12a` (base `002cc2f`, UC-1 APPROVED)
- Verdict: REJECT
- Findings: 0 critical, 2 gap (G-1, G-2), 0 protocol, 1 drift (D-1, needs a user decision), 0 cosmetic. The behavior
  I could reproduce is correct. The gaps are in the committed evidence: two contract clauses can be broken without
  any committed test failing.
- Suite: focused `MatrixLensBrowserIT` 11 run / 0 failed / 0 errors / 0 skipped. Full `./mvnw -q -pl
  timetable-workspace -am verify`: timetable-workspace unit 26 / 0 / 0 / 0; integration 117 / 2 failures / 9 errors /
  0 skipped. The 11 failing IDs are identical to the baseline (`/tmp/lens-base`, 106 / 2 / 9 / 0); the set difference
  is empty in both directions. The +11 are `MatrixLensBrowserIT`.
- Working tree impact from verification: none. `git status --short` was empty before the full run, after it, and
  after the focused run. Probes and mutation checks ran in scratch exports outside the repository
  (`/tmp/lens-verify-uc2`, `/tmp/lens-mut-a`, `/tmp/lens-mut-bc`, all `git archive 013c12a`).

Paths: `app.js`, `inspection-state.js`, and `messages.js` are in
`timetable-workspace/src/main/resources/static/workspace/`. `MatrixLensBrowserIT` is
`timetable-workspace/src/test/java/org/schoolkernel/workspace/MatrixLensBrowserIT.java`.

## Protocol Gate

1. One target. UC-2 is `READY_FOR_CONVERGENCE` in `status.md`. Pass.
2. `checkpoints/UC-2.md` is committed in `013c12a` together with the code. Pass.
3. Requires UC-1, which is `APPROVED` (`002cc2f`). Pass.
4. UC-3 and UC-4 are `NOT_STARTED`. Nothing else is in progress. Pass.
5. The checkpoint has rows for main 1-5, extensions 1a, 1b, 1c, and 3a, G1, G2, both postconditions, state rules 1-3,
   Requires UC-1, RULE-1/2/3/4/6/7/10, commands, changed files, and the regression. Pass. Its accuracy is graded below.
6. The diff touches `app.js` (scroll capture and restore, the lens-specific 3a notice, reset and clear-narrowing
   transitions), `inspection-state.js` (`leaveOrEnterLens`, `lensScrollContext`), `messages.js` (one message), the
   test class, the checkpoint, and `status.md`. All of it belongs to UC-2. The review-target and draft-navigation paths
   (`app.js:418-428`, `:818`) are untouched and only drop the recorded position, which belongs to UC-3. No UC-3 or
   UC-4 behavior is completed. Pass.

## Runtime Reproduction

Headless Chrome through Playwright, against an ephemeral Spring Boot server and PostgreSQL Testcontainers. The probe
class was `UC2VerifierProbeBrowserIT`, scratch only.

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Main 1-5 via × and "All teachers" (Week) | PASS `:345` | Green. 2400 is restored after teacher→teacher→room. 1800 is restored via "All teachers". Status reads "Complete school population" |
| Administrator | Main 2-5 with context (Day, search, highlights, period, selection) | PASS `:384` | Green. 17 class rows. Every context value is kept. The selected lesson is in view. Status is narrowed, with the period criterion only |
| Administrator | Ext 1a / 1b / 3a | PASS `:419` | Green. Probe `probeSameTypeChangeRendersNewEntityOnly`: after teacher-16→teacher-3, all 60 visible tiles are Teacher 3's and none are Teacher 16's. "Represented lessons: 60", header "Teacher · Teacher 3" |
| Administrator | Ext 1c Reset / `#reset-empty` | PASS `:455` | Green. Recorded 2000 and 1500 are restored. Reset clears search; `#reset-empty` keeps it |
| Administrator | Ext 1c `#clear-filters` with a teacher lens from "Show only matches" plus subject "Show only matches" plus a class filter | not claimed | Probe: lens, class, and both filter modes are cleared. `teacher-investigation` stays `teacher-16` and `subject-investigation` stays `subject-0`, both unchecked, with 40 teacher and 61 subject highlights. Search "Sixteen" is kept. Scroll 2000 is restored. Status is complete |
| Administrator | Ext 1c Reset, same configuration plus period | not claimed | Probe: the same highlight-mode return. Search and period are cleared. Scroll 1500 is restored |
| Administrator | Lens from Show week, selection cleared by 1a, then lens removed | not claimed | Probe: entry 4948 is restored exactly, and no selection remains |
| Administrator | Lens from Show week, selection represented after clearing | not claimed | Probe: recorded 3000. After clearing, `lesson-0` is in view (scrollTop 0), so the selection wins over the position (state rule 2) |
| Administrator | Day: horizontal and vertical restore (900 px viewport) | not claimed | Probe: (left 15, top 1200) restored exactly |
| Administrator | Weekday changed while lens active (Day) | not claimed | Probe: Monday 900, then room lens, Tuesday, "All rooms". Tuesday class rows at 900 |
| Administrator | Range changed while lens active | not claimed | Probe: Week class rows 2400 (max 18960), teacher lens, Day, ×. Day class rows at 2400 (max 2897). See D-1 |
| Administrator | Review target out of a lens (UC-3 1a) after UC-2 | UC-1 test `:271` | Green. Probe: a Proposal lens entered at 1600; the review target `lesson-60` clears the lens with "Cleared Teacher filter to show the review target." and scrolls the target into view (284), not to the stale 1600. A later lens entry and clear follows rule 2 (the selection `lesson-60` stays in view) |
| Administrator | Non-mutation | `:345`, `:384`, `:419`, `:455` | Empty mutation log and equal stored document in all four committed tests and in every probe that checks it |

Mutation checks, run on the committed `MatrixLensBrowserIT` in scratch copies:

| Mutation | Result |
|---|---|
| A: `settleMatrixScroll` is a no-op (`app.js:983`) | 3 tests fail (`clearsALensBackToTheRecordedScroll`, `resetsTheLensWithEveryNarrowingCriterion`, `clearsALensRetainingTheInvestigativeContext` at `:407`). This confirms the executor's report |
| B: inverted precedence. The recorded position wins over a represented selection (`app.js:985-986` swapped) | **Survives.** All 11 green. See G-2 |
| C: lens→lens transitions overwrite the recorded position (`inspection-state.js:34`) | Caught by `clearsALensBackToTheRecordedScroll` (expected 2400) |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Trigger / precondition: a lens is active, entered via UC-1 | `:345-361`, `:394-396`, `:424-426`, `:465-466` | Filters selects, inspector Show week, and teacher "Show only matches" (in the probe) all enter through `applyLens` (`app.js:965`) | STRONG | yes |
| Main 1: remove criterion × / "All teachers" | `:362`, `:373`, `:400` | Real × click (`removeLens`) and select change both reach `applyLens(…, null)` (`app.js:1507`, `:1510`) | STRONG | yes |
| Main 2: class rows under remaining filters | `:364`, `:375`, `:401-402` | 60 CLASS groups. 17 visible rows under period-0, by value | STRONG | yes |
| Main 3: retain range, weekday, search, highlights, class and period filters, selection | `:403-406`; `AcceptedInspectionBrowserIT:147-151` | Every dimension asserted by value after the lens is cleared. Class filter kept in `AcceptedInspectionBrowserIT` | STRONG | yes |
| Main 4: restore the recorded scroll | `:364`, `:375`, `:471`, `:483` | Exact restore. Mutations A and C caught. Day and horizontal restore confirmed by probe | STRONG | yes |
| Main 4 / state rule 2: bring a represented selection into view *instead* | `:407-408` | Assertion passes under mutation B, because the restored position clamps onto the tile in this fixture. Correct production behavior only confirmed by verifier probe | WEAK | no (G-2) |
| Main 5: complete or narrowed | `:365-366`, `:409-411`, `:472` | By value | STRONG | yes |
| Ext 1a: same type, new entity; selection only if it belongs | `:357-359`, `:435-440` | Header, selection cleared with notice. Probe confirms the tile set is the new entity's only | STRONG | yes |
| Ext 1b: other type | `:360-361`, `:428-433`, `:442-447` | Other select emptied, row group replaced, selection kept or cleared correctly, no stray notice | STRONG | yes |
| Ext 1c: reset clears lens and every narrowing criterion per inspection-ux rule 7; resume at 4 | `:455-484` | Lens, class, period, and search clearing, plus scroll restore: STRONG. Rule 7's "Subject and teacher selections return to highlight mode": never exercised. `subjectOnly` is never set (`:460`), and no test resets a teacher lens held by "Show only matches". The `#clear-filters` control is not driven in any UC-2 test | WEAK | no (G-1) |
| Ext 3a: unrepresented selection cleared and announced | `:436-440`, `:445-447` | Notice by value, inspector hidden, no `.selected` | STRONG | yes |
| G1 context retention | `:403-404` | As main 3 | STRONG | yes |
| G2 no surface change | `:377` | No `.focused-schedule` or `#return-matrix`. Grep of production static finds neither | STRONG | yes |
| Success postcondition | assertions above | New lens or class rows with retained context | STRONG | yes |
| Minimal guarantee | `:431-433`, `:403`, `:473`; mutation log | Selects, criteria, and rows agree after each transition. Empty mutation log and equal document in all four tests | STRONG | yes |
| State rule 1 | `:403-406` | As main 3 | STRONG | yes |
| State rule 3 | `:470-474` | Reset clears lens and criteria | STRONG | yes |
| Requires UC-1 | UC-1 tests unchanged and green | All 7 UC-1 tests green (focused and full). UC-2 consumes UC-1's lens row group | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | "`resetFilters` and `clearNarrowing` MUST clear both. The module MUST NOT add a second, parallel lens field" | `inspection-state.js:94-97` clear `teacherFilterId` and `roomId` through `leaveOrEnterLens` (`:30-36`). `lensScrollContext` is a scroll position, not a lens value. It is derived from the filter values and cannot disagree with them | Pass |
| RULE-2 | "Lens values and the lens-entry scroll context MUST NOT be written to `localStorage`, `sessionStorage`, the URL, or the server" | `persistPreference` (`inspection-state.js:143-149`) is unchanged and writes `{version, range, weekdayId}`. `MatrixLensBrowserIT:126-133` green | Pass |
| RULE-3 | "MUST NOT branch into a separate lens renderer" | No renderer change. Clearing renders through `renderWholeSchool()` | Pass |
| RULE-4 | "visible lens tiles MUST be exactly those satisfying the existing `isRepresented` predicate" | 1a/1b/3a selection uses `clearSelectedLessonOutsideRepresentation` (`app.js:1932`), which uses `isRepresented` | Pass |
| RULE-6 | "MUST NOT issue any non-GET request … byte-identical" | Empty `recordMutations()` and equal `storedDocument()` in `:378-379`, `:413-414`, `:449-450`, `:486-487`, and in probes | Pass |
| RULE-7 | "Every new user-visible or accessible string MUST be a `messages.js` entry … written to the existing `#inspection-notice`" | `messages.js:322`, `app.js:971-972`. Notice asserted by value `:437`, `:446` | Pass |
| RULE-10 | "MUST NOT rebuild the accepted model or re-fetch the workspace" | `applyLens`, `resetView`, and `clearNarrowing` call `renderWholeSchool()` and `settleMatrixScroll` only (`app.js:965-974`, `:1896-1908`). `ScaleTimingBrowserIT` 3/3 | Pass |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 (approved) | Requires; same `applyLens`, state module, test class | 7/7 UC-1 tests green in focused and full runs | Pass |
| UC-3 (not started) | Review-target lens clearing (UC-3 1a) | `appliesAndLeavesLensesDuringProposalReview` green. Probe: the target is scrolled into view and no stale position is applied | No break |
| `timetable-inspection-ux` (approved) | Reset, clear filters, investigation | `AcceptedInspectionBrowserIT` 11/11 | Pass |
| Repair, proposal, completion (approved) | Shared workbench | Same 11 baseline failures, no new IDs | No regression |
| Import, planning, manual draft, scale | Shared workspace | `ImportAndInitialPlanningBrowserIT` 7/7, `WorkspaceImportIT` 25/25, `WorkspaceInitialPlanningIT` 8/8, `WorkspaceManualDraftIT` 11/11, `WorkspaceRepairDraftIT` 11/11, `WorkspaceRepairPlanningIT` 10/10, `FlywayFailureIT` 2/2, `ScaleTimingBrowserIT` 3/3 | Pass |

Baseline failing set, identical in this run: `ProposalReviewBrowserIT.{reviewsIndependentlyVerifiedNormativeRepair,
revisesAndDiscardsVerifiedNormativeRepair}`, `RepairCompletionBrowserIT.completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessor`,
`RepairDraftBrowserIT.{preparesProtectedRepairDraftWithKeyboard, preparesWideProtectedDraftAtNormativeScale,
refusesSolveAfterRealDraftPersistenceFailure, resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources}`,
`RepairRunBrowserIT.{followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshot,
generatesAndAcceptsSuccessiveRepairProposals, inspectsFrozenRepairRunAndRecoversWithoutPublishing,
showsFailedRepairEvidenceAndGatedRetry}`.

## Findings

### Gap (blocking)

**G-1. Extension 1c's rule-7 clause has no evidence: reset never returns a filter-mode investigation to highlight mode.**
- Reference: UC-2 ext 1c: "the system clears the lens and every other narrowing criterion according to
  `timetable-inspection-ux` rule 7". Rule 7 says: "Subject and teacher selections return to highlight mode rather than
  being silently discarded." Under resolved decision 5, the teacher lens *is* teacher investigation's filter mode, so
  resetting from a "Show only matches" lens is exactly the interaction UC-2 introduces.
- Evidence: `MatrixLensBrowserIT.java:460` selects `subject-4` but never checks `#subject-only`. The assertion at
  `:472-473` that `subject-investigation` is still `subject-4` therefore holds for any implementation, and the
  checkpoint claim "returns the subject to highlight mode" is vacuous. No committed test resets or clears filters while
  a teacher lens is held by `#teacher-only`. `AcceptedInspectionBrowserIT:441` and `:385` reset only after
  `#clear-teacher`. The `#clear-filters` control (`app.js:1544`) is not driven in any UC-2 test; only `#reset-empty`
  is (`:481`).
- Why it fails: a wrong implementation would pass every committed test. For example, `resetFilters` discarding
  `teacherId`/`subjectId` when they were in filter mode, or leaving `#teacher-only` checked. My probe shows production
  is currently correct (investigations kept, both checkboxes unchecked, 40 teacher and 61 subject highlights), but
  that is verifier evidence, not committed proof.
- Revision outcome: add a committed browser assertion. Investigate a teacher and check "Show only matches" (teacher
  lens), and investigate a subject and check its "Show only matches". Then use Reset and, separately, `#clear-filters`.
  Assert by value: the lens is cleared, both investigation selects are kept, both checkboxes are unchecked, and
  highlight cues are present on class rows (`.teacher-match`, `.subject-match`). Also assert the search: cleared by
  Reset, kept by Clear filters. Finally, assert the recorded scroll is restored.

**G-2. The state-rule-2 precedence assertion cannot tell "selection into view" from "restore the recorded position".**
- Reference: UC-2 main 4: "restores the scroll position recorded at lens entry, or brings the selected lesson into
  view if one is represented". State rule 2: "If the selected lesson is represented, it is scrolled into view
  instead."
- Evidence: `MatrixLensBrowserIT.java:407-408` asserts `scrollTop > 0` and the tile inside the viewport. With
  `app.js:985-986` swapped so the recorded position wins (mutation B), all 11 tests stay green. The recorded Day
  position, taken when `lesson-960` was clicked, clamps onto the same tile once the period filter shortens the matrix.
- Why it fails: the committed assertion passes for the wrong precedence. Only the verifier probe (selection
  `lesson-0` at the top, recorded position 3000, tile in view at 0 after clearing) discriminates.
- Revision outcome: make the recorded position and the selected tile's position incompatible. One way: select a lesson
  near the top, scroll the class rows far away, enter the lens, clear it, and assert the tile is in view and
  `scrollTop` is not the recorded value. Confirm by re-running mutation B.

### Drift (non-blocking; needs a user decision)

**D-1. After a range change inside a lens, the Week scroll offset is applied to Day class rows.**
- Reference: state rule 2: "Clearing the lens restores class rows and the matrix scroll position recorded when the
  lens was entered." The spec does not say what happens when the range changes while the lens is active.
- Evidence: `inspection-state.js:34` keeps `lensScrollContext` across `selectRange` and `selectDay`. Probe: Week
  class rows at 2400 (max 18960), then teacher lens, Day, ×. Day class rows open at 2400 of a maximum of 2897, a
  different class from the one on screen at entry. A weekday change within Day restores 900 on the new day, which is
  geometrically sound.
- Note: this is the literal contract, but probably not the intended experience. The user decides: keep the literal
  restore, or drop the recorded position when the range changes during the lens and start at the top. If the contract
  changes, `spec.md` is amended first.

## Walkthrough

Deferred. UC-2 is a UI use case, but the automated gate does not pass (G-1, G-2). The walkthrough script will be issued
with the PENDING WALKTHROUGH verdict after revision. It will reuse the UC-1 launch and `/tmp/mv5-lens.json` recipe and
include D-1 as a decision step.

## Status Update

UC-2: `READY_FOR_CONVERGENCE` -> `NEEDS_REVISION` (G-1, G-2; D-1 for user decision). Next eligible: UC-2 revision
only. UC-3 and UC-4 wait for UC-2 approval.

## Response to execute

REVISE UC-2: G-1 (commit browser evidence that Reset and #clear-filters return a "Show only matches" teacher lens and subject filter mode to highlight mode, with the investigations, highlights, search rule, and scroll restore asserted by value), G-2 (make the state-rule-2 precedence test fail when the recorded position wins over a represented selection); D-1 (Week scroll offset reused on Day class rows after a range change in a lens) awaits the user's decision.
