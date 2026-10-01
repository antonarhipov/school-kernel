# Convergence: UC-1 - See one teacher's or room's schedule in the matrix

History: revision 0 (`e840460`) was rejected in `69ac6e4` with C-1 (no Show week in the Proposal comparison
inspector), G-1 (Solving lens, Day room-lens tile, and Proposal review-target lens navigation had no executable
committed evidence), D-1 (lens follows a changed teacher investigation), and K-1 (checkpoint understated executed
evidence). This report grades revision 1.

## Summary

- Submission: `spec/timetable-matrix-lenses/checkpoints/UC-1.md` at `cba775d` (base `064bf64`; revision delta
  `69ac6e4..cba775d`)
- Verdict: PENDING WALKTHROUGH
- Findings: 0 critical, 0 gap, 0 protocol, 1 drift (D-1, carried, needs user confirmation), 2 cosmetic (K-2, K-3).
  Prior C-1, G-1, and K-1 are resolved.
- Suite: focused `MatrixLensBrowserIT` 7 run / 0 failed / 0 errors / 0 skipped. Full `./mvnw -q -pl
  timetable-workspace -am verify`: timetable-workspace unit 26 / 0 / 0 / 0; integration 113 / 2 failures / 9 errors /
  0 skipped. The 11 failing IDs are identical to the `064bf64` baseline (106 / 2 / 9 / 0), and each fails at the same
  step (line offsets equal the hunk shifts). The +7 tests are `MatrixLensBrowserIT`.
- Working tree impact from verification: none. `git status --short` was empty before the focused run, after it, and
  after the full run. Verifier probes ran in a scratch export (`/tmp/lens-verify-r1`, `git archive cba775d`) and are
  not in the repository.

Paths: `app.js` is `timetable-workspace/src/main/resources/static/workspace/app.js`. Test classes are in
`timetable-workspace/src/test/java/org/schoolkernel/workspace/`.

## Protocol Gate

1. One target. UC-1 is `READY_FOR_CONVERGENCE` (revision 1) in `status.md`. Pass.
2. The revised checkpoint is committed in `cba775d` with the code, so the submission boundary is immutable. Pass.
3. UC-1 has no Requires, Includes, or Extends dependency. Pass.
4. No other UC is `IN_PROGRESS` or `READY_FOR_CONVERGENCE` (UC-2 to UC-4 are `NOT_STARTED`). Pass.
5. The checkpoint has a Revision 1 table plus rows for the main scenario, every extension and guarantee, both
   postconditions, RULE-1 to RULE-10, commands, changed files, and baseline regression. Pass.
6. The revision delta touches `app.js` (Proposal Show week, single manual-editor binding), `MatrixLensBrowserIT`, the
   checkpoint, and `status.md`. The binding fix is disclosed shared infrastructure: it is pre-existing at `064bf64`
   (`bindManualEditor` was called from both `selectLesson` and `bindCloseDetails`) and made the ext 4b evidence
   nondeterministic. No UC-2, UC-3, or UC-4 behavior is completed. Pass.

## Runtime Reproduction

Headless Chrome through Playwright against an ephemeral Spring Boot server and PostgreSQL 18.6 Testcontainers.

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Main 1-5, Filters teacher and room, Week and Day (Current) | PASS | `pivotsTheMatrixToOneTeacherOrRoom` green (focused and full) |
| Administrator | Day room-lens tile and header (former G-1) | PASS `:105-119` | Green. Asserts header `Room · <name>`, heading `Room`, Monday set 960-971, tile `[subject, teacher, class]`, accessible-name parts, no availability claim |
| Administrator | Inspector Show week (Current), teacher investigation | PASS | `appliesLensesFromInspectorAndTeacherInvestigation` green |
| Administrator | Inspector Show week in Repair Proposal PROPOSAL mode (former C-1) | PASS `:271` | Committed test green. Probe `probeProposalShowWeekAcrossSides`: room-only move `lesson-100` offers `TEACHER:teacher-1`, `ROOM:room-1`, `ROOM:room-2` (one action per distinct declared entity across sides). `room-2` lens keeps the combined tile, `Represented lessons: 61`, inspector open. Switching ROOM room-1 then TEACHER clears the room lens. Addition `lesson-added` offers teacher-0/room-0; cancellation `lesson-101` offers teacher-1/room-1. No mutation, document equal |
| Administrator | Proposal review target outside / inside a lens (former G-1) | PASS `:288-298` | Green. Outside clears the teacher filter, restores 60 CLASS groups, announces `Cleared Teacher filter`. Inside keeps `teacher-1`, selects the proposed `lesson-61` tile, empty notice |
| Administrator | Lens in SOLVING_REPAIR (former G-1) | PASS `:236` | Green via `processes.blockReplan`, not the pin steps. `#cancel-run` and inspector retained, selection kept, Day lens row, removal restores class rows, empty mutation log, equal document, lifecycle still `SOLVING_REPAIR` |
| Administrator | Lens in Repair Draft | PASS `RepairDraftBrowserIT:295` | Green in `retainsAcceptedCanvasThroughTeacherDraftModesAndReload` (`:254`) |
| Administrator | Ext 4b manual-draft clash, one PATCH per edit | PASS `:208`, `:233` | Green. `edit.requests()` is read at the end of the test, so a late second PATCH would be caught |
| Administrator | Manual editor binding regression (approved `timetable-manual-editing`) | "binds once per form" | Probe `probeManualEditorSavesOncePerEdit`, green. Seven edits, seven PATCHes, each with the right payload and stored result: first edit (creates draft), second edit on the re-rendered form, `REVERT_LESSON`, edit after collapse/reopen, edit after selecting the same lesson twice (previous lesson untouched), edit inside a room lens, cross-day period edit in Day range |
| Administrator | Minimal guarantee (undeclared entity) | PASS | `refusesAnUndeclaredLensEntity` green |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Trigger: Filters disclosure | `MatrixLensBrowserIT:37`, `:91` | Real select change events reach `applyLens` (`app.js:965`). Green | STRONG | yes |
| Trigger: inspector Show week | Current `:144-157`, Solving `:236`, Proposal `:271`, Repair Draft `RepairDraftBrowserIT:295` | Lesson inspector `app.js:1174`; comparison inspector `app.js:1103` with `showWeekActions(...sides)` `app.js:1232`; binding `app.js:1240`. Committed tests green in every lifecycle mode; Proposal sides confirmed by probe | STRONG | yes |
| Trigger: teacher investigation Show only matches | `:161-170` | Check applies teacher-16 lens with 40 highlighted matches; uncheck clears lens and keeps investigation. Green | STRONG | yes |
| Main 1-2: choose, clear the other type first | `:91-95`, `:154-157` | `inspection-state.js:76-83`; room after teacher leaves `#teacher-filter` empty; Proposal probe shows TEACHER after ROOM clears room | STRONG | yes |
| Main 3: one lens row group, header names entity and type | `assertLensRowGroup` | Week teacher `:39`, Week room `:93`, Day teacher `:79`, Day room `:108`; headings `:40`, `:80`, `:109` by value | STRONG | yes |
| Main 4: every assignment in its period cell | `:45`, `:81`, `:96`, `:110` | Visible ID sets equal fixture-derived sets (lessons 960-999; Monday 960-971) | STRONG | yes |
| Main 4: Normative tile content | `:51`, `:82`, `:97`, `:111` | All four lens/range cells of the table asserted by value; accessible-name additions `:53-56`, `:113-116`; room label dropped visibly but kept accessibly `:99` | STRONG | yes |
| Main 4: unavailable empty cells | `:59-61`, `:87` | Exactly periods 41-59 unavailable, 40 ordinary; Friday 48-59; text cue and title by value | STRONG | yes |
| Main 5: narrowed label, entity, unique count, removable criterion | `:65-69` | By value. Proposal count uniqueness: probe `Represented lessons: 61` | STRONG | yes |
| Main 6: reads | rendered assertions | Covered by main 3-5 assertions | STRONG | yes |
| Ext 1a (narrow, UC-4) | unchanged base | Narrow path only under `view.narrow`; `AcceptedInspectionBrowserIT` 11/11 green | STRONG (branch point) | yes |
| Ext 4a: empty entity | `:178-204` | room-99: one 60-cell group, 0 lessons, `#no-matches` with reset; an intersected-to-zero lens keeps its group | STRONG | yes |
| Ext 4b: several assignments in one cell, conflict cues | `:208-234` | Real edit causes clash; both tiles in period-0 cell with `.conflicting` and indicator; overlay shows `ROOM_CLASH`; lens issues no mutation; setup edit issues exactly one PATCH | STRONG | yes |
| Ext 4c: no declared availability | `:101-103`, `:117` | Week and Day room lens: zero unavailable cues | STRONG | yes |
| Ext 5a: tile selection enters UC-3 | `:144-151`, `:252-257` | Inspector opens inside the lens (entry point only) | STRONG (entry) | yes |
| G1 same surface | `:73`, `:151`, `:256`, `:285` | No focused surface or entry/return control; inspector and selection persist across Show week in Current, Solving, Proposal; grep of production static finds no `data-open-focus`, `return-matrix`, `focusedEntry`, `focused-entry` | STRONG | yes |
| G2 completeness | `:45-47` | Same ID set at scroll start and end; one pass over `displayed.assignments` (`app.js:912-936`) | STRONG | yes |
| G3 honesty | `:59-66`, `:102`, `:117` | Narrowed title, named entity, unavailability only where declared | STRONG | yes |
| G4 non-mutation | all seven tests | Empty mutation log and equal stored document at `:122-123`, `:172-173`, `:202-203`, `:231-232`, `:266-268`, `:300-301`, `:339-340`; also in both verifier probes | STRONG | yes |
| All lifecycle modes (precondition) | Current, Manual Draft, Solving, Proposal, Repair Draft | Committed, executing tests for each: `:30`, `:208`, `:236`, `:271` plus `ProposalReviewBrowserIT.retainsBothSidesInResourceLenses`, `RepairDraftBrowserIT:295` | STRONG | yes |
| Success postcondition | `assertLensRowGroup` plus tiles | Exactly one lens row group with lens tiles | STRONG | yes |
| Minimal guarantee | `:306-317` | Injected undeclared teacher refused; room lens, selects, summary, single ROOM group unchanged | STRONG | yes |
| State rule 4, rule 5 | `:319-337`, `:126-133` | Real module in browser; reload gives 60 CLASS groups and empty selects | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | "Setting either one to a declared ID MUST clear the other … undeclared ID MUST return `changed: false` … MUST NOT add a second, parallel lens field" | `inspection-state.js:76-91`; `teacherOnly` removed; transition JSON `MatrixLensBrowserIT:319-337` | Pass |
| RULE-2 | "The local preference record MUST keep exactly `{ version, range, weekdayId }`" | `:126-129` exact JSON (now `MONDAY`, after the Day room-lens step), `sessionStorage.length` 0, empty search and hash | Pass |
| RULE-3 | "`renderWeekMatrix` and `renderDayMatrix` MUST be the only functions that produce matrix markup … take row groups … and a cell lookup" | `week-renderer.js:2`, `day-renderer.js:2`; one arrangement from `matrixArrangement` (`app.js:912`); `tileFields` (`app.js:955`) in all three tile builders | Pass |
| RULE-4 | "visible lens tiles MUST be exactly those satisfying the existing `isRepresented` predicate … In `PROPOSAL` mode, every comparison representation … In `MANUAL_DRAFT` Draft mode, the manual-draft model MUST be the source" | `app.js:912-936`, `displayedModel()` `app.js:309`; Proposal both sides `:285`, `ProposalReviewBrowserIT:66`; draft source `:224` | Pass |
| RULE-5 | "marked unavailable only when the lens entity has an `availablePeriodIds` array and the cell's period ID is absent … text cue and a CSS class" | `app.js:931-935`, `:948-952`; `:59-61`, `:87`, `:102`, `:117` | Pass |
| RULE-6 | "MUST NOT issue any non-GET request … byte-identical" | G4 row; Proposal and Solving included | Pass |
| RULE-7 | "Every new user-visible or accessible string MUST be a `messages.js` entry" | Revision adds no strings. Show week labels use `M.showWeek`, `M.showWeekOf`, `M.teacher`, `M.room` | Pass |
| RULE-8 | "UC-1 MUST remove the desktop 'Focused schedules' entry buttons … MUST add the inspector 'Show week' actions that replace them" | Grep clean. Show week now present in the lesson inspector and the Proposal comparison inspector (`app.js:1103`), which is where base `064bf64` rendered `focusedEntry()` in Proposal mode | Pass (C-1 resolved) |
| RULE-9 | "Each existing browser-IT assertion that depends on a removed focused element MUST be replaced … The full `timetable-workspace` verify suite passes" | Every removed focused assertion has a lens counterpart. The three unreachable rewritten blocks (`RepairDraftBrowserIT:139-146`, `RepairRunBrowserIT:164-168`, `ProposalReviewBrowserIT:250-260`) now have executing equivalents in `MatrixLensBrowserIT` (`:236`, `:271`, `:288-298`). Suite failures are exactly the pre-existing baseline set | Pass (G-1 resolved) |
| RULE-10 | "MUST NOT rebuild the accepted model or re-fetch … single linear pass" | `applyLens` (`app.js:965`) calls `renderWholeSchool()` only; one loop in `matrixArrangement`; `ScaleTimingBrowserIT` 3/3 green | Pass |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| This feature UC-2 to UC-4 | Not approved; no baseline | n/a | n/a |
| `timetable-manual-editing` (approved) | Inline editor binding changed (`app.js:1711-1715`) | `WorkspaceManualDraftIT` 11/11. Verifier probe: seven distinct edit paths each issue exactly one PATCH with the correct payload and stored result. Handlers read `view.selectedLessonId` and select values at event time, and each re-render creates a new form element that is bound once, so no path loses its binding | Pass |
| `timetable-inspection-ux` (approved) | Filters, investigation, narrow agenda | `AcceptedInspectionBrowserIT` 11/11 | Pass |
| Proposal and repair review (approved) | Comparison inspector gained Show week | `ProposalReviewBrowserIT` 4/6; the 2 failures are baseline (`:138`/`:370` vs base `:141`/`:374`, same pin step) | No regression |
| Repair draft, run, completion (approved) | Inspector, task areas | 9 baseline failures, same IDs and steps (`RepairDraftBrowserIT` +1 line from the hunk; `RepairRunBrowserIT` identical lines; `RepairCompletionBrowserIT:69` vs base `:70`) | No regression |
| Import, initial planning, repair planning, scale | Shared workspace | `ImportAndInitialPlanningBrowserIT` 7/7, `WorkspaceImportIT` 25/25, `WorkspaceInitialPlanningIT` 8/8, `WorkspaceRepairDraftIT` 11/11, `WorkspaceRepairPlanningIT` 10/10, `FlywayFailureIT` 2/2, `ScaleTimingBrowserIT` 3/3 | Pass |

Baseline failing set (both runs): `ProposalReviewBrowserIT.{reviewsIndependentlyVerifiedNormativeRepair,
revisesAndDiscardsVerifiedNormativeRepair}`, `RepairCompletionBrowserIT.completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessor`,
`RepairDraftBrowserIT.{preparesProtectedRepairDraftWithKeyboard, preparesWideProtectedDraftAtNormativeScale,
refusesSolveAfterRealDraftPersistenceFailure, resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources}`,
`RepairRunBrowserIT.{followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshot,
generatesAndAcceptsSuccessiveRepairProposals, inspectsFrozenRepairRunAndRecoversWithoutPublishing,
showsFailedRepairEvidenceAndGatedRetry}`. Set difference in both directions is empty.

## Prior Findings

| Finding | Resolution verified |
|---|---|
| C-1 | Resolved. `app.js:1103` renders `showWeekActions` for the accepted and proposed sides; committed test `:271-286` and probe across move, addition, and cancellation shapes |
| G-1 | Resolved. Solving `:236-269`, Day room lens `:105-119`, Proposal review navigation `:288-298`, all executing and green, none on the failing pin steps |
| D-1 | Unchanged by design and disclosed in checkpoint Notes. Carried below for walkthrough confirmation |
| K-1 | Resolved. Notes list exactly which rewritten blocks run; I confirmed each against the failing line of its test (`:56`, `:133`, `:138`, `:69`) and `RepairDraftBrowserIT:295` inside the passing `:254` test |

## Findings

### Drift (non-blocking)

**D-1. A teacher investigation change moves a matching teacher lens.**
- Reference: resolved decision 5: "Checking 'Show only matches' under teacher investigation applies the teacher lens
  for the investigated teacher. Unchecking it clears that lens." The spec is silent on changing the investigated
  teacher while checked.
- Evidence: `app.js:1541-1545` (`lensFollows`). `#teacher-only` renders checked whenever the Teacher filter equals the
  investigated teacher (`app.js:875`), including when the lens came from the Filters select.
- Note: preserves the approved old teacher-only behavior. The user confirms or rejects it in walkthrough step 11.

### Cosmetic (non-blocking)

**K-2. `status.md` and checkpoint line references are partly stale.**
- Evidence: `status.md` "Commands and results" still reports the first submission (5 run; 111 integration), and its
  contract table cites `MatrixLensBrowserIT.java:162`, `:192`, `:220` (now `:178`, `:208`, `:306`). Checkpoint G4 row
  cites `:230`, `:298`, `:337` (now `:231`, `:300`, `:339`).
- Revision outcome: refresh at the next executor touch. No behavior or proof depends on it.

**K-3. Proposal Show week names an entity from its side's definition, the lens header from the accepted one.**
- Evidence: `app.js:1236` uses `entityName(side[entity], …)`; `lensEntity` (`app.js:943`) prefers the accepted map.
  With the fixture's proposal definition stripping `room-2`'s display name, the action reads "Show week for room room-2
  (Name unavailable)" while the resulting header reads "Room · Room 2". Real repairs keep entity names, so this shows
  only in the fixture.
- Revision outcome: optional. Resolve the label through `lensEntity` for consistency.

## Walkthrough

The automated gate passes. UC-1 is a UI use case, so approval waits for the user to run or confirm this script.

**Launch** (README "Operations workspace"), from the repository root:

```bash
./mvnw -q -pl timetable-workspace -am package
java -jar timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar
```

Open <http://localhost:8080/workspace/> in a desktop window wider than 700 px. If the walkthrough database already
holds a workspace, clear it with Utilities, then "Clear workspace" (or stop the app and run
`docker compose down --volumes`).

**Data.** No file in `examples/` declares `availablePeriodIds`. Create a walkthrough copy of MV5. It gives Vaike Antsov
(10 lessons) declared availability without `mon-6` and `fri-3`-`fri-6`, and adds a teacher with no lessons:

```bash
python3 - <<'EOF'
import json
d = json.load(open('examples/mv5.json'))
excluded = {'mon-6', 'fri-3', 'fri-4', 'fri-5', 'fri-6'}
for t in d['teachers']:
    if t['id'] == 'vaike-antsov':
        t['availablePeriodIds'] = [p['id'] for p in d['periods'] if p['id'] not in excluded]
d['teachers'].append({'id': 'walkthrough-idle', 'displayName': 'Walkthrough Idle Teacher', 'qualifiedSubjectIds': ['int-o']})
json.dump(d, open('/tmp/mv5-lens.json', 'w'), ensure_ascii=False, indent=2)
EOF
```

The verifier planned this file with the packaged kernel: `FEASIBLE`, with every Vaike Antsov lesson inside her
declared set. Import `/tmp/mv5-lens.json`, generate the initial proposal, and accept it. No room in MV5 declares
availability.

**Current (accepted) mode**

1. **Main 1-3, Filters teacher (Week).** Open Filters, choose "Vaike Antsov". Expect one row group headed
   "Teacher · Vaike Antsov", the column heading "Teacher" instead of "Class", and no class rows.
2. **Main 4.** All 10 of her lessons appear, each in its period cell. Each tile shows subject · room · class. Hover or
   inspect a tile: the accessible name also has the teacher, weekday, period, and lesson ID.
3. **Main 4 / G3.** Empty cells at "Esmaspäev 6" (`mon-6`) and "Reede 3" to "Reede 6" (`fri-3` to `fri-6`) read "Unavailable" (patterned, with the
   tooltip "Outside declared availability"). Every other empty cell is an ordinary empty cell.
4. **Main 5.** The status reads "Filtered whole-school matrix" and "Lens: Teacher · Vaike Antsov". "Represented
   lessons: 10". Active filters shows "Teacher: Vaike Antsov" with a remove (×) control.
5. **G2.** Scroll the matrix to the far end and back. The lessons shown do not change.
6. **Day.** Switch to Day and step through the weekdays. Each day is one row with that day's periods and the same tile
   content. Friday shows "Unavailable" at Reede 3 to Reede 6.
7. **Main 2, room lens.** Back in Week, choose a room that holds lessons (B213 did in the verifier's plan) in Filters.
   The Teacher select resets to "All teachers". The single row group is "Room · <room>". Tiles show subject · teacher · class, and the room name is no longer printed
   on the tile.
8. **Ext 4c.** The room declares no availability, so no cell says "Unavailable", in Week or Day.
9. **Ext 4a.** Choose teacher "Walkthrough Idle Teacher". Expect the full empty row group under that name,
   "Represented lessons: 0", a "Reset view" offer, and no invented lesson.
10. **Inspector trigger / G1.** Reset, select any lesson, then use "Show week" for its teacher, then for its room. The
    matrix pivots in place each time, the inspector stays open, the lesson stays selected, and the room action clears
    the teacher lens.
11. **Teacher investigation trigger, and D-1.** Reset. Choose a teacher under teacher investigation and check "Show
    only matches": the teacher lens applies and the highlights remain. Uncheck it: class rows return and the
    investigation stays. Check it again, then change the investigated teacher. The lens follows to the new teacher.
    **Confirm whether that is the behavior you want (D-1).** Also note that choosing the investigated teacher in the
    Filters select shows "Show only matches" as checked.

**Manual draft**

12. **Ext 4b.** Select a lesson and note its period. Find the other class's lesson in the same period and note its
    room. In the inspector, change the first lesson's room to that room; the workspace enters a manual draft with a
    conflict. Apply that room's lens. Both tiles stand in the same cell, each with the conflict highlight and ⚠️.
    Activating ⚠️ opens the conflict overlay. The lens itself changes nothing in the draft.

**Repair modes**

13. **Repair Draft.** Discard the manual draft. Start a protected repair (for example, Vaike Antsov unavailable in one
    of her teaching periods). In the draft, apply a teacher lens from Filters and from the inspector Show week. The
    draft task area stays in place.
14. **Solving.** Solve the draft. While generation is running (MV5 may finish in seconds), apply a lens if you can:
    the run controls and inspector stay put.
15. **Repair Proposal (former C-1).** In Proposal mode, select a moved lesson. The inspector offers Show week for its
    teacher and room, and for both rooms if the room changed. Apply each: a moved lesson keeps both its accepted and
    proposed tiles in the lens. Then use a review-list entry for a lesson outside the lens: the lens clears with the
    notice "Cleared Teacher filter" (or Room).

**Reload and non-mutation**

16. **G4 / reload.** Reload. Class rows return with no lens; range and weekday are kept. Nothing about the accepted
    timetable, draft, or proposal changed because of a lens.
17. **Minimal guarantee.** Not reachable through ordinary controls, since the selects list only declared entities.
    It is covered by `refusesAnUndeclaredLensEntity`.

Do not test 5a (UC-3 editing within a lens) or the ≤ 700 px narrow view (UC-4).

User result: not yet performed.

## Status Update

UC-1: `READY_FOR_CONVERGENCE` (revision 1) -> `PENDING_WALKTHROUGH`. Next eligible: none. UC-2 to UC-4 stay blocked
until the user confirms the walkthrough, including D-1, and UC-1 is `APPROVED`.

## Response to execute

PENDING WALKTHROUGH: UC-1 automated gate passes (C-1, G-1, K-1 resolved); awaiting user walkthrough confirmation including the D-1 lens-follows-investigation interpretation; K-2 and K-3 are non-blocking cosmetic notes.
