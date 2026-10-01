# Convergence: UC-1 - See one teacher's or room's schedule in the matrix

## Summary

- Submission: `spec/timetable-matrix-lenses/checkpoints/UC-1.md` at `e840460` (base `064bf64`)
- Verdict: REJECT
- Findings: 1 critical, 1 gap, 0 protocol, 1 drift, 1 cosmetic
- Suite: focused `MatrixLensBrowserIT` 5 run / 0 failed / 0 errors / 0 skipped. Full `timetable-workspace` verify:
  unit 72 / 0 / 0 / 0; integration 111 / 2 failures / 9 errors / 0 skipped. The 11 failing IDs match the `064bf64`
  baseline exactly (106 / 2 / 9 / 0), and each one fails at the same pre-existing step.
- Working tree impact from verification: none. `git status --short` was empty before and after every run. Verifier
  probes ran in a scratch export (`/tmp/lens-verify`, `git archive e840460`) and are not in the repository.

Paths: `app.js` is `timetable-workspace/src/main/resources/static/workspace/app.js`. Test classes are in
`timetable-workspace/src/test/java/org/schoolkernel/workspace/`.

## Protocol Gate

1. One target. UC-1 is `READY_FOR_CONVERGENCE` in `status.md`. Pass.
2. The checkpoint is committed in `e840460` together with the implementation, so the submission boundary is immutable.
   Pass.
3. UC-1 has no Requires, Includes, or Extends dependency. Pass.
4. No other UC is `IN_PROGRESS` or `READY_FOR_CONVERGENCE` (UC-2 to UC-4 are `NOT_STARTED`). Pass.
5. The checkpoint has rows for the main scenario, every extension and guarantee, both postconditions, RULE-1 to
   RULE-10, commands, changed files, and baseline regression. Pass. Some row content is inaccurate; see K-1.
6. The diff is attributable to UC-1 plus enabling changes. These are the `displayedModel()` source fix that RULE-4
   needs, the removal of `teacherOnly` (resolved decision 5), and the desktop focused removal that RULE-8 requires. No
   UC-2, UC-3, or UC-4 behavior is completed: there is no scroll restoration, no lens-departure announcement, and the
   narrow agenda is unchanged. Pass.

## Runtime Reproduction

All runs used headless Chrome through Playwright against an ephemeral Spring Boot server and PostgreSQL 18.6
Testcontainers.

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Main 1-5, Filters teacher and room, Week and Day (Current) | PASS | `MatrixLensBrowserIT` 5/5 green in my focused and full runs |
| Administrator | Trigger: inspector Show week (Current) | PASS | Green. `appliesLensesFromInspectorAndTeacherInvestigation` |
| Administrator | Trigger: inspector Show week (Repair Proposal, PROPOSAL mode) | Not offered (Notes) | Probe: after a tile is selected in Proposal mode, `#workbench-inspector [data-show-week]` count = **0** (C-1) |
| Administrator | Lens in SOLVING_REPAIR | "no direct evidence" | Probe `probeSolvingLens` (blocked replan), green. Show week is present. The lens row is `teacher-alex` in Week and Day. Tile = `Mathematics \| Room 102 \| Year 7A`. Lessons = `lesson-math-1, lesson-science-1`. `#cancel-run` is retained, the inspector stays open, and the selection is kept. `recordMutations()` = []. Stored document is equal |
| Administrator | Lens in Repair Draft (DRAFT mode) | "not executed" | `RepairDraftBrowserIT.retainsAcceptedCanvasThroughTeacherDraftModesAndReload:295-299` **is executed** and passes at HEAD (K-1) |
| Administrator | Lens in Proposal, unique counts | PASS | Probe: teacher-0 lens gives `Represented lessons: 61`, which equals the 61 unique visible IDs |
| Administrator | Review target outside a lens (Proposal) | Rewritten, unexecuted | Probe: clicking a `lesson-60` review target under the teacher-0 lens clears `#teacher-filter`, restores 60 CLASS row groups, and announces `Cleared Teacher filter` |
| Administrator | Day room lens tile and header | Not asserted | Probe `probeDayRoomLensTile`, green. Tile = `[Subject Zero…, Teacher Sixteen…, Class Sixteen…]`. The accessible name has the room, Monday, `Declared period 0`, and `lesson-960`. Header = `Room · Room Sixteen…`, heading `Room`. 0 unavailable cues |
| Administrator | Minimal guarantee (undeclared entity) | PASS | Green. `refusesAnUndeclaredLensEntity` |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Trigger: Filters disclosure | `MatrixLensBrowserIT:37`, `:91` | Real `#teacher-filter` and `#room-filter` change events lead to `applyLens` (`app.js:964`, bound at `:1487-1489`). Green | STRONG | yes |
| Trigger: inspector Show week | `MatrixLensBrowserIT:128-141` | Green in Current. Executed in Repair Draft (`RepairDraftBrowserIT:295`) and RepairCompletion (`:47-49`). SOLVING probe green. **Absent in Repair Proposal PROPOSAL mode**: `selectedLessonDetails` returns `comparison-details` (`app.js:1094`) without `showWeekActions` (only `app.js:1172`) | ABSENT (Proposal mode) | no (C-1) |
| Trigger: teacher investigation Show only matches | `MatrixLensBrowserIT:145-154` | Checking applies the teacher-16 lens and unchecking clears it (`app.js:1504-1507`). Green | STRONG | yes |
| Main 1-2: choose, clear the other type first | `:90-95`, `inspection-state.js:80-82` | Room after teacher leaves `#teacher-filter` empty and no `Teacher:` criterion. Also asserted in the module transition test (`:233-251`) | STRONG | yes |
| Main 3: one lens row group, header names the entity and type | `assertLensRowGroup` | Exactly one `tbody` or `tr` with `data-row-kind`, `data-row-id`, and header text by value. The heading cell is `Teacher` or `Room`. Week teacher, Week room, Day teacher (suite). Day room (probe) | STRONG | yes |
| Main 4: every assignment in its period cell | `:45`, `:96` | Visible ID set = lessons 960-999, taken from fixture construction (`WorkbenchFixtures.java:107-116`: lessons `i/60 = 16`). Day Monday = 960-971 | STRONG | yes |
| Main 4: Normative tile content (6 cells) | `:51`, `:82`, `:97` | Teacher Week, teacher Day, and room Week are asserted by value in the suite. Room Day is asserted by the verifier probe only. Accessible names contain the required additions | STRONG (room Day: probe only, see G-1) | yes |
| Main 4: unavailable empty cells | `:59-61`, `:87` | Exactly periods 41-59 are unavailable and 40 is ordinary. The text cue `Unavailable` plus the title. Friday 48-59 | STRONG | yes |
| Main 5: narrowed label, entity, unique count, removable criterion | `:65-69` | `#filter-title`, `Lens: Teacher · …`, `Represented lessons: 40`, remove control aria-label, all by value. Proposal uniqueness confirmed by probe (61/61) | STRONG | yes |
| Main 6: reads | rendered assertions | Covered by the main 3-5 assertions | STRONG | yes |
| Ext 1a (narrow, UC-4) | unchanged base | Narrow path still `renderFocused()` only when `view.narrow` (`app.js:245`, `:508`, and the narrow branches). `AcceptedInspectionBrowserIT` 11/11 green | STRONG (branch point only) | yes |
| Ext 4a: empty entity | `:162-188` | room-99: one 60-cell group, 0 lessons, `#no-matches` with `#reset-empty`. An intersected-to-zero lens keeps its group | STRONG | yes |
| Ext 4b: several assignments in one cell, conflict cues | `:192-215` | A real manual-draft edit causes `ROOM_CLASH`. Both tiles are in the `Declared period 0` cell, `.conflicting` with an indicator. The overlay opens with `ROOM_CLASH`. No lens mutation | STRONG | yes |
| Ext 4c: no declared availability | `:101-103` | room-16 has 0 unavailable cues and ordinary empty cells 40-59 (Week). Day: 0 cues (probe) | STRONG | yes |
| Ext 5a: tile selection enters UC-3 | `:128-136` | The inspector opens with lesson 960 inside the lens (entry point only; UC-3 not graded) | STRONG (entry) | yes |
| G1 same surface | `:73`, `:135` | No `.focused-schedule`, `#return-matrix`, `[data-open-focus]`, or `.focused-entry`. The inspector stays open across Show week. Grep of production static finds no `data-open-focus`, `return-matrix`, or `focusedEntry` | STRONG | yes |
| G2 completeness | `:45-47` | Same ID set at scroll start and end. Lens cells come from one pass over `displayed.assignments` (`app.js:911-934`), with visibility from `isRepresented` | STRONG | yes |
| G3 honesty | `:59-66`, `:102` | Narrowed title, named entity, text-cued unavailability only where declared | STRONG | yes |
| G4 non-mutation | all five tests | `recordMutations()` = [] and stored document equal in each test. Also in the SOLVING and Proposal probes | STRONG | yes |
| All lifecycle modes (precondition) | Current, Manual Draft, Proposal, Repair Draft, Solving | Current and Manual Draft: suite. Proposal: `ProposalReviewBrowserIT.retainsBothSidesInResourceLenses` green, `ScaleTimingBrowserIT` green. Repair Draft: `RepairDraftBrowserIT:295` green. Solving: verifier probe only | STRONG at runtime; Solving committed evidence ABSENT | partial (G-1) |
| Success postcondition | `assertLensRowGroup` plus tiles | Exactly one lens row group with lens tiles | STRONG | yes |
| Minimal guarantee | `:220-231` | An injected undeclared teacher option is refused. Room lens, selects, and summary are unchanged, and there is one ROOM group | STRONG | yes |
| State rule 4 (refuse undeclared), rule 5 (reload resets) | `:238-251`, `:110-117` | Real module in the browser. Reload gives 60 CLASS groups and empty selects | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | "Setting either one to a declared ID MUST clear the other … undeclared ID MUST return `changed: false` … MUST NOT add a second, parallel lens field" | `inspection-state.js:80-82`. `resetFilters` and `clearNarrowing` clear both. `teacherOnly` removed. The transition JSON is asserted at `MatrixLensBrowserIT:248-251` | Pass |
| RULE-2 | "The local preference record MUST keep exactly `{ version, range, weekdayId }`" | `MatrixLensBrowserIT:110-113`: exact JSON, `sessionStorage.length` 0, empty search and hash | Pass |
| RULE-3 | "`renderWeekMatrix` and `renderDayMatrix` MUST be the only functions that produce matrix markup … take row groups … and a cell lookup" | `week-renderer.js:2`, `day-renderer.js:2` take `rows`, `cellItems`, `emptyMarkup`. One call per range (`app.js:885`, `:895`). Tiles via `tileFields` (`app.js:954`) in all three builders. Pre-existing unused `matrix()` (`app.js:1008`, already at base `:969`) is not a lens path | Pass |
| RULE-4 | "visible lens tiles MUST be exactly those satisfying the existing `isRepresented` predicate … In `PROPOSAL` mode, every comparison representation … In `MANUAL_DRAFT` Draft mode, the manual-draft model MUST be the source" | `app.js:911-934`, `displayedModel()` `app.js:308`. The proposal both-sides test is green. Manual-draft source asserted at `MatrixLensBrowserIT:207` | Pass |
| RULE-5 | "marked unavailable only when the lens entity has an `availablePeriodIds` array and the cell's period ID is absent … text cue and a CSS class" | `app.js:930`, `:947-951`. The suite asserts by value. Class rows pass `emptyCellMarkup(false, …)` | Pass |
| RULE-6 | "MUST NOT issue any non-GET request … byte-identical" | Every lens test and probe has an empty mutation log and an equal stored document | Pass |
| RULE-7 | "Every new user-visible or accessible string MUST be a `messages.js` entry" | `messages.js` adds `lensSummary`, `unavailableCell`, `outsideAvailability`, `showWeek`, `showWeekOf`, `removeCriterion`. Row type labels reuse `M.teacher` and `M.room`. Notices go to `#inspection-notice` | Pass |
| RULE-8 | "UC-1 MUST remove the desktop 'Focused schedules' entry buttons … 'Return to whole school' … MUST add the inspector 'Show week' actions that replace them" | Grep is clean. Desktop `renderFocused()` is reachable only under `view.narrow`. Show week was added to the lesson inspector but **not to the Proposal-mode comparison inspector**, which had the focused entry before (`064bf64` `renderRepairProposal` appended `focusedEntry()`) | Fail in Proposal mode (C-1) |
| RULE-9 | "Each existing browser-IT assertion that depends on a removed focused element MUST be replaced in the same UC commit with an assertion of the lens behavior … Verification: … The full `timetable-workspace` verify suite passes" | Every removed focused assertion has a lens counterpart in the diff. `Workbench.openFocus` and `returnToMatrix` were removed; `showWeek` and `removeLens` were added. `focusEntity` stays for the narrow tests. Three rewritten blocks never execute: `RepairDraftBrowserIT:139-146`, `RepairRunBrowserIT:164-168`, `ProposalReviewBrowserIT:250-260`. Their tests fail earlier on pre-existing pin or geometry steps, so the declared verification is not met | Constraint met; verification ABSENT for 3 blocks (G-1) |
| RULE-10 | "MUST NOT rebuild the accepted model or re-fetch … single linear pass" | `applyLens` calls `renderWholeSchool()` only (`app.js:964-972`). There is one loop in `matrixArrangement`. `ScaleTimingBrowserIT` 3/3 green | Pass |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| This feature UC-2 to UC-4 | Not approved; no regression baseline | n/a | n/a |
| `timetable-inspection-ux` (approved) | Filters, focused schedules superseded, narrow agenda | `AcceptedInspectionBrowserIT` 11/11 green at HEAD and baseline | Pass |
| `timetable-repair-review` and proposal review (approved) | Proposal comparison, review-target navigation | `retainsBothSidesInResourceLenses` green. The two failing tests fail at the same `Workbench.pin` step as baseline (`:138`/`:370` at HEAD vs `:141`/`:374` at base; the line shift equals the hunk size). The review-target-outside-lens behavior was reproduced by the verifier probe | No regression |
| Repair draft, run, completion (approved) | Inspector, task areas, lens in draft and solving | 9 failures with identical IDs and steps at baseline: pin `setChecked`/`click` timeouts and draft-task or utilities geometry. `RepairCompletion` passes its rewritten lens step (`:47-49`) and fails later at the same pin step (`:69` vs base `:70`) | No regression |
| `timetable-manual-editing` (approved) | Conflict overlays, manual-draft source | `WorkspaceManualDraftIT` 11/11 green. Lens ext 4b green | Pass |
| Scale timing | 1,000-lesson render | `ScaleTimingBrowserIT` 3/3 green | Pass |

Baseline comparison. `/tmp/lens-base/timetable-workspace/target/failsafe-reports` (106 / 2 / 9 / 0) and the HEAD run
(111 / 2 / 9 / 0) contain the same 11 failing test IDs with the same failure kinds:
`ProposalReviewBrowserIT.{reviewsIndependentlyVerifiedNormativeRepair, revisesAndDiscardsVerifiedNormativeRepair}`,
`RepairCompletionBrowserIT.completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessor`,
`RepairDraftBrowserIT.{preparesProtectedRepairDraftWithKeyboard, preparesWideProtectedDraftAtNormativeScale,
refusesSolveAfterRealDraftPersistenceFailure, resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources}`, and
`RepairRunBrowserIT.{followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshot,
generatesAndAcceptsSuccessiveRepairProposals, inspectsFrozenRepairRunAndRecoversWithoutPublishing,
showsFailedRepairEvidenceAndGatedRetry}`. None fails at or after a lens step. The difference of 5 tests is the new
`MatrixLensBrowserIT`. None of the 11 is caused by UC-1.

## Findings

### Critical

**C-1. The inspector "Show week" trigger is absent in Repair Proposal PROPOSAL mode.**
- Reference: UC-1 Trigger (`spec.md:108`): "activates 'Show week' for a teacher or room in the inspector". Preconditions
  (`spec.md:110`): "in any lifecycle mode". Scope (`spec.md:18`, `:21`): "in every lifecycle mode that renders the
  whole-school matrix", "Lens entry … from the inspector". RULE-8: "MUST add the inspector 'Show week' actions that
  replace them".
- Evidence: `selectedLessonDetails` returns the comparison inspector (`app.js:1094`) with no `showWeekActions`.
  `showWeekActions` is only called from `lessonDetails` (`app.js:1172`). The verifier probe observed 0
  `[data-show-week]` buttons after a tile was selected in PROPOSAL mode. Base `064bf64` rendered `focusedEntry()` in
  `renderRepairProposal`, so Proposal mode lost its inspector-adjacent entry with no replacement. The checkpoint
  discloses this as an interpretation (`checkpoints/UC-1.md:107`), while `status.md` says "Deviations: none".
- Why it fails: one of the three declared triggers does not exist in one lifecycle mode that the precondition
  includes.
- Revision outcome: offer teacher and room Show week actions in the comparison inspector. Use each distinct teacher
  and room across the accepted and proposed sides, because RULE-4 already defines either-side lens membership. Add a
  browser assertion that applies a lens from it in PROPOSAL mode with no mutation. If the product owner instead wants
  Proposal mode excluded, that decision must first be written into `spec.md`, which turns this into a `BLOCKED` item.

### Gap

**G-1. The committed suite has no executable evidence for several lens surfaces that the verifier could only confirm
by probe.**
- Reference: RULE-9 Verification: "The full `timetable-workspace` verify suite passes". The Normative tile table row
  "Room lens … Day tile … Subject · teacher · class". UC-1 Preconditions "any lifecycle mode".
- Evidence:
  - The rewritten lens blocks never run. `RepairDraftBrowserIT:139-146` sits in a test that fails at `:56`.
    `RepairRunBrowserIT:164-168` (the only SOLVING lens step) sits in a test that fails at `:133`.
    `ProposalReviewBrowserIT:250-260` (an in-lens accepted origin keeps its selected cue, and review targets clear the
    lens) sits in a test that fails at `:138`.
  - No committed test asserts the Day room-lens tile or header (`MatrixLensBrowserIT:76-103` covers Day for the
    teacher lens and Week for the room lens only).
  - The verifier probes (`probeSolvingLens`, `probeProposalLensAndInspector`, `probeDayRoomLensTile`) show the
    behavior itself is correct. Coverage relative to baseline is not reduced: the replaced focused assertions were
    also unreached at `064bf64`.
- Why it fails: the evidence for these elements exists only in verifier-run scratch code. The repository would not
  catch a regression in them.
- Revision outcome: add pin-independent assertions to `MatrixLensBrowserIT`:
  - A SOLVING lens through `processes.blockReplan`, with Show week, `#cancel-run` retained, and no mutation.
  - A Day room-lens tile and header by value.
  - Proposal review-target navigation into and out of a lens.

  These must not rely on the pre-existing failing pin steps.

### Drift (non-blocking)

**D-1. A teacher investigation change moves a matching teacher lens.**
- Reference: resolved decision 5 covers checking and unchecking only.
- Evidence: `app.js:1532-1539` (`lensFollows`). `#teacher-only` renders checked whenever
  `view.teacherId === view.teacherInvestigationId`, including when the lens came from the Filters select
  (`app.js:874`).
- Note: the spec does not cover this case. Confirm the behavior in the walkthrough. It is recorded in the checkpoint
  Notes.

### Cosmetic (non-blocking)

**K-1. The checkpoint understates the executed evidence and says "Deviations: none".**
- Evidence: `checkpoints/UC-1.md:95-99` lists `RepairDraftBrowserIT:295` as not executed and says there is "no direct
  Repair Draft … lens evidence". `retainsAcceptedCanvasThroughTeacherDraftModesAndReload` runs `:295-299` and passes,
  and `RepairCompletionBrowserIT:47-49` also runs before its pre-existing failure. `status.md` "Deviations: none"
  contradicts the Proposal-mode restriction in the Notes.
- Revision outcome: correct the Notes and record any remaining restriction as a deviation.

## Walkthrough

The walkthrough has not been requested, because the automated gate failed. This script, derived from UC-1, is for the
resubmission.

Launch (README "Operations workspace"):

```bash
./mvnw -q -pl timetable-workspace -am package
java -jar timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar
```

Open <http://localhost:8080/workspace/> in a desktop window wider than 700 px.

Data: none of the files in `examples/` declares `availablePeriodIds`. Copy `examples/mv5.json` (or your own school).
Add `"availablePeriodIds": [...]` to one teacher, listing only some period IDs. Leave at least one teacher and every
room without it. Import the definition, generate the initial proposal, and accept it. For the proposal and
manual-draft steps, start a repair or a manual edit from the accepted workspace.

1. **Main 1-3, Filters teacher (Week).** Open Filters and choose the availability teacher. Expect one row group headed
   "Teacher · <name>", with the column heading "Teacher" instead of "Class".
2. **Main 4.** Every lesson of that teacher appears in its period cell. Each tile shows subject · room · class, with
   long names clamped. Hover or inspect: the full names are present.
3. **Main 4 / G3.** Empty cells for periods outside the declared set read "Unavailable" (patterned, with the tooltip
   "Outside declared availability"). Empty cells inside the set are ordinary "Empty".
4. **Main 5.** The status reads "Filtered whole-school matrix" and "Lens: Teacher · <name>", and "Represented
   lessons" equals that teacher's lesson count. Active filters shows "Teacher: <name>" with a × remove control.
5. **Day.** Switch to Day and pick a weekday. Expect one row with that day's periods and the same tile content.
6. **Main 2, room.** Choose a room in Filters. The teacher select resets to "All teachers". The single row group is
   "Room · <name>" and tiles show subject · teacher · class.
7. **Ext 4c.** The room has no declared availability, so no cell says "Unavailable".
8. **Ext 4a.** Choose a room with no lessons. You see the full empty row group, "Represented lessons: 0", and a
   "Reset view" offer. No invented lesson appears.
9. **Inspector trigger / G1.** Reset, select any lesson, and use "Show week" for its teacher, then for its room. The
   matrix pivots in place, the inspector stays open, and the lesson stays selected.
10. **Teacher investigation trigger.** Reset, choose a teacher under teacher investigation, and check "Show only
    matches". The teacher lens applies and highlights remain. Uncheck it: class rows return and the investigation
    stays. Also check D-1: change the investigated teacher while the box is checked and confirm that the lens following
    the investigation is what you want.
11. **Ext 4b (manual draft).** In a manual draft, move a lesson into a room and period already used. Apply that room's
    lens. Both tiles appear in the same cell with the conflict highlight and ⚠️, and the indicator opens the overlay.
12. **Proposal mode (after C-1 is fixed).** With a repair proposal in Proposal mode, apply a teacher lens and a room
    lens from Filters and from the inspector. A moved lesson keeps both its accepted and proposed tiles.
13. **G4 / reload.** Reload. Class rows return with no lens. Range and weekday are kept. Nothing about the accepted
    timetable, draft, or proposal changed.
14. **Minimal guarantee.** This is not reachable through ordinary controls, because the selects list only declared
    entities. It is covered by the automated test.

Do not run 5a (UC-3) or the ≤ 700 px narrow view (UC-4).

User result: not performed.

## Status Update

UC-1: `READY_FOR_CONVERGENCE` -> `NEEDS_REVISION` (C-1, G-1). Next eligible: UC-1 revision only. UC-2 to UC-4 stay
blocked until UC-1 is approved.

## Response to execute

REVISE UC-1: C-1 add teacher/room "Show week" to the Proposal-mode comparison inspector (or obtain a written spec exclusion); G-1 add pin-independent MatrixLensBrowserIT coverage for the SOLVING lens, the Day room-lens tile/header, and Proposal review-target navigation in and out of a lens; K-1 correct checkpoint Notes and Deviations.
