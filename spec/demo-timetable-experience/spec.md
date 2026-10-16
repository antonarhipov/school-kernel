# Timetable-first demo

## Feature summary

Visitors immediately explore realistic sample timetables, try changes in a lesson inspector, keep independently
named alternatives in their browser, and optionally generate timetables for a school copy.

## Scope and resolved decisions

- Only the public demo changes. The existing normal-profile workspace retains its contract.
- School selection is navigation. It never discards drafts, confirms replacement, or starts computation.
- Schools group timetables sharing a resource/subject/class catalog. Each saved version preserves its matching
  definition and assignments, including version-specific teaching responsibility. Existing versions are immutable.
- Different schools clear entity filters and selected lessons. Timetables within one school preserve filters.
- Browser-local saved timetables and drafts survive visits, session expiry, restart and deployment. They belong to
  this origin/browser/device. Cross-device accounts, sharing and database persistence are outside this feature.
- Changes are manual and lesson-specific. No other lessons move automatically. Intermediate conflicts are allowed.
- All mandatory definition and scheduling rules participate in validation. Preferences are advisory, never blockers.
- Utilities contains the definition editor, related generation controls and local-artifact cleanup. Repair/setup, imports/exports,
  reopen-inspector, separate teacher-change and teacher-route controls are absent from this demo interface.
- Saving definition edits creates a school copy, never a mutation of a school with existing timetables.
- Existing demo access, request-size, complexity, solver queue, allowance and cleanup rules continue to apply.
- Agent-run real-browser walkthroughs own approval; no additional human gate is required.
- Preserve the established compact retro matrix styling: paper background, small-radius borders, monospace time
  labels, original subject palette and fixed compact density. Week tiles show the subject and tiny visible room;
  Day tiles and lesson inspection expose teacher details. Do not introduce tall cards or a density switch.

## Actors and domain terms

- Visitor: an anonymous person using the demo in one browser.
- School: a named definition/resource catalog and its collection of timetables.
- Saved timetable: a named complete valid definition/assignment pair associated with one school.
- Draft: unfinished changes attached to one source timetable, including conflicting edits.
- Must fix: a mandatory-rule violation with affected lessons, explanation and correction guidance.
- Could be improved: an advisory preference issue, distinguished from mandatory violations.
- Ready to save: the complete current draft has passed validation; neither pending nor failed checks imply validity.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Explore and switch realistic school timetables (primary) | Visitor | None |
| UC-2 | Adjust lessons and save a valid timetable alternative | Visitor | Requires UC-1 |
| UC-3 | Create a school copy and generate its timetable | Visitor | Requires UC-1; includes UC-2 at step 6 |
| UC-4 | Delete this browser's locally stored school artifacts | Visitor | Requires UC-1 |

## State models

A timetable is saved with no draft, has a pending draft, has a checked draft with mandatory errors, has a checked
valid draft (possibly with warnings), or has a draft whose check failed. Any assignment edit creates a pending
draft. Only the matching complete check may change its validation state. Another edit invalidates earlier check
results. Saving a checked valid draft atomically adds a named saved version and removes its source draft. Discarding
removes the source draft. Reverting every change removes the draft and toolbar. Navigation preserves drafts.
All other transitions are refused without changing any saved version or another draft.

A school copy is saved without a timetable, generating, has an unsuccessful generation, or has a generated result
awaiting a timetable name. Generation is explicit. Failure retains the school definition and existing data. A valid
result can be saved through UC-2; a run never silently replaces an existing named timetable. Drafts and saved
versions are isolated by school and timetable identities; labels are not identity or computation-cache keys.

## UC-1 - Explore and switch realistic school timetables (primary)

- Goal: Inspect schools and compare timetable arrangements immediately without losing work.
- Primary actor: Visitor
- Supporting actors: none
- Trigger: The visitor opens the demo or selects a school/timetable.
- Preconditions: The demo is available and its bundled timetables passed startup verification.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor opens the demo.
2. The full Estonian school's Variant A opens as the whole-school matrix, with school and timetable selectors at
   top-left, inspection filters above the matrix, and Utilities containing the definition editor and local-data cleanup.
3. The visitor filters the matrix by class, teacher, room, subject/search, day, or time slot, and opens lesson details.
4. The visitor selects another timetable of the same school. The matrix changes immediately with filters retained.
5. The visitor selects another school. Its timetable opens immediately with filters and lesson selection cleared.
6. The visitor returns to a previously viewed timetable and sees any unfinished edits restored.
7. On a later visit, saved school copies, named timetables and unfinished edits from this browser are available.

### Extensions

- 1a. If the browser's saved data has unreadable entries, readable entries remain available; the visitor receives
  recovery feedback and can continue with the bundled schools without silently deleting stored data; continue at step 2.
- 1b. If browser storage is unavailable, the visitor receives a persistent storage explanation, can inspect samples,
  and is never told unsaved work is durable; continue at step 2.
- 3a. At a narrow viewport the matrix remains inspectable, selectors/filters remain usable, and lesson details and
  editing controls fit the viewport; resume at step 3.
- 4a. If the selected timetable has a draft, its current draft is displayed with its pending/error/warning state;
  continue at step 5.
- 5a. If a saved school has no timetable, the visitor sees that fact and an editor/generation handoff; end.
- 7a. If a stored timetable no longer matches supported rules, preserve it and explain the check/recovery failure;
  never mark an unchecked draft ready to save; end.

### Guarantees

- G1. The bundled schools are exactly the four rows in Bundled schools, each with exactly two bundled variants.
  Both variants preserve identical requirements, teacher assignments, availability and resource catalogs and have
  different period placements for at least four lessons on at least two weekdays. Every pair has matching complete
  verified feasible assignments and current scoring evidence. Small school is absent from the public catalog.
- G2. Selection performs no solve or verification process and requires no replacement confirmation. Once catalog
  data is loaded, switching renders from browser-held data without waiting for a network response.
- G3. Saved versions/drafts preserve school/timetable identity, names, exact complete definition, complete assignment
  fields, source identity, optional fields and provenance. Unmappable data is explained and retained, not silently
  relabeled, overwritten or accepted as valid.
- G4. Utilities has the definition editor, related generation controls and local-artifact cleanup. The protected-repair entry,
  reopen-inspector button, separate teacher-change section and inspector JSON-route button are absent.
- G5. Rendering uses names and time metadata from the definition; identifiers never determine weekdays or labels.
  Matrix overflow remains inside its scroll container, controls wrap at 390px, and keyboard users can operate all
  selectors and lesson details with visible focus. There are no browser runtime errors in the accepted journey.
- G6. No visitor changes packaged artifacts or another browser's data. Browser-local persistence never implies
  account-based recovery or cross-device access. Storage write failures do not destroy previous complete values.

### Postconditions

- Success: The selected timetable/draft is visible, with correct filter behavior and preserved independent work.
- Minimal guarantee: Failed navigation or persistence never overwrites a saved timetable or unrelated draft.

## UC-2 - Adjust lessons and save a valid timetable alternative

- Goal: Try lesson changes, understand their consequences and keep a separately named valid timetable.
- Primary actor: Visitor
- Supporting actors: none
- Trigger: The visitor opens a lesson and changes its teacher, room or time slot.
- Preconditions: UC-1 provides a complete timetable.
- Relations:
  - Requires: UC-1, because a source timetable and school exist
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor opens a lesson in its inspector and changes teacher, room or time slot.
2. The complete draft and matrix reflect the edit; all other assignments remain unchanged. The draft is retained in
   this browser and the bottom toolbar shows the changed-lesson count and validation-in-progress state.
3. The complete latest draft is checked against every mandatory definition/scheduling rule and applicable preferences.
4. Mandatory conflicts appear as Must fix, highlight all affected lessons, explain names/times and offer correction
   guidance. Advisory issues appear separately as Could be improved. The toolbar summarizes the current state.
5. The visitor follows a conflict to an affected lesson, fixes it, and repeats validation until the draft is valid.
6. The visitor enters a timetable name and saves a new version.
7. The new complete valid version is durably stored in this browser and selected. The original is intact; the source
   draft is cleared. Both versions remain independently selectable.

### Extensions

- 1a. If a teacher is not qualified for this subject, the inspector explains eligibility and the draft cannot be saved
  with that assignment. A teacher who is qualified but busy remains selectable for intermediate swaps; resume at step 1.
- 2a. If persistence fails, explain it, preserve the last durable value and the in-memory edit, and do not claim a
  successful save; resume at step 2.
- 3a. If validation fails, is busy, expires, or returns incomplete evidence, retain the draft, explain retry, and keep
  Save disabled; resume at step 3.
- 3b. If another edit or navigation occurs during validation, an old response cannot replace another draft's state or
  validate newer edits. Validate the latest relevant draft; resume at step 3.
- 4a. If filters hide an affected lesson, the global conflict list remains available; following its lesson clears the
  necessary filters and opens its inspector; resume at step 5.
- 4b. If the draft is valid with advisory warnings, Save is available and the warnings remain distinguishable from
  errors; continue at step 6.
- 5a. The visitor discards changes or reverts all changed values. The source timetable returns exactly and the toolbar
  disappears; end.
- 5b. The visitor switches schools/timetables or reloads. Restore the exact draft on return; resume at step 3.
- 6a. If the name is blank or duplicates another version in this school, explain the name error and retain the draft;
  resume at step 6.
- 6b. If any mandatory error, pending check or check failure exists, Save is refused without adding a version; resume
  at step 3.
- 6c. If browser storage cannot save, retain the source, draft and all previous versions, explain recovery, and allow
  retry; resume at step 6.

### Guarantees

- G1. Every edit checks the entire candidate, including eligibility, teacher/class/room collisions and availability,
  room capacity/capabilities/assignment/home-room rules, reserved periods, policy locks, class gaps/start/spread and
  subject day/daily/reserved rules supported by the current catalog. Zero partial inline conflicts is not validity.
- G2. Every Must fix item identifies the rule, affected lesson identities, names/context, explanation and actionable
  correction guidance. All affected lessons are marked; conflicts remain globally reachable independently of filters.
  The response exposes whether examples are truncated; no unreported violation is silently treated as passed.
- G3. Teacher changes affect only that candidate's complete definition and assignments, preserving qualification
  catalogs and other timetable versions. No lesson other than the explicitly edited lesson changes automatically.
- G4. Validation status belongs to an exact candidate/build, not a label or browser-supplied validity flag. A saved
  version preserves complete matching definition/result, revisions, actual score and advisory consequences.
- G5. The bottom toolbar is visible whenever a draft differs, contains name, Save as new timetable and Discard changes,
  shows pending/error/valid state and changed-lesson count, and never obscures the last matrix rows or inspector.
- G6. Mandatory rules block saving; soft preferences never block it. Existing baseline preference costs are not
  misrepresented as newly introduced editing errors. Preference explanations use names and understandable terms.
- G7. Browser expiry/restart/deployment does not erase saved versions or drafts. CSRF, host/origin, session isolation,
  bounded input, complexity, physical-process concurrency and kernel-time allowance guards remain applicable.

### Postconditions

- Success: A separately named, fully valid timetable is stored and selected, with the original unchanged.
- Minimal guarantee: Invalid, stale, failed or interrupted checks never authorize saving or corrupt prior data.

## UC-3 - Create a school copy and generate its timetable

- Goal: Explore an edited definition as a separate school and keep a generated timetable.
- Primary actor: Visitor
- Supporting actors: none
- Trigger: The visitor opens the school-definition editor under Utilities.
- Preconditions: UC-1 provides the school catalog.
- Relations:
  - Requires: UC-1, because the source school/catalog is browsable
  - Includes: UC-2 at step 6, to name and save the generated valid timetable
  - Extends: none

### Main success scenario

1. The visitor opens the school-definition editor in Utilities with the selected school's definition.
2. The visitor edits the definition and supplies a name for a school copy.
3. The complete definition is checked and the separately identified school copy is stored in this browser.
4. The visitor explicitly generates a timetable using Quick or Thorough. Run status and cancellation remain visible.
5. A complete valid generation result opens in that school's matrix as a candidate awaiting a timetable name.
6. The visitor performs UC-2's naming/saving journey and obtains a selectable saved timetable for the school copy.

### Extensions

- 2a. Invalid JSON is located without replacing any school/definition or losing the text; resume at step 2.
- 3a. If mandatory definition validation or limits fail, report located errors and retain text; resume at step 2.
- 3b. If the school name is blank/duplicate or browser storage fails, explain it and retain all original data/text;
  resume at step 2.
- 4a. If capacity, allowance, session or state prevents generation, explain the refusal and retain the saved school
  and all other work; resume at step 4.
- 4b. If cancelled, interrupted or unsuccessful, retain the school and existing timetables and explain the result;
  resume at step 4.
- 4c. If the visitor switches away during generation, navigation does not overwrite another school's timetable or
  draft; the originating school retains the run/result status; continue at step 5 when available.
- 5a. If output cannot be completely validated/mapped, retain the school and refuse to present it as saveable;
  resume at step 4.

### Guarantees

- G1. Existing schools, drafts and timetables are unchanged by definition editing or generation. School selection
  never auto-generates. Saving a definition alone never starts the solver.
- G2. The definition editor retains the complete document, exact optional values and unmappable-error locations.
  Generated candidates preserve the same complete field fidelity as UC-2 G4.
- G3. Existing queue, Quick/Thorough budgets, complexity limits, reuse, cancellation and verification contracts apply.
  No additional main-view generation or repair controls are introduced. A result is never auto-saved under an
  existing timetable's name or silently published over another version.

### Postconditions

- Success: A separate school copy and its named generated timetable are available in this browser.
- Minimal guarantee: Failure retains source data, editor text and previously saved work; no invalid result is saved.

## UC-4 - Delete this browser's locally stored school artifacts

- Goal: Remove saved school definitions, timetables and unfinished work from this browser.
- Primary actor: Visitor
- Supporting actors: none
- Trigger: The visitor requests local-data cleanup in Utilities.
- Preconditions: UC-1's bundled catalog is available; saved browser data may be empty or unreadable.
- Relations:
  - Requires: UC-1, because the bundled catalog remains available after cleanup
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor opens Utilities and reads that cleanup deletes only school definitions, timetables and unfinished
   edits stored in this browser on this device, keeps bundled examples available, and cannot be undone.
2. The visitor requests deletion and sees a confirmation repeating this scope.
3. The visitor confirms. The locally saved school definitions, timetable versions, drafts, editor work and related
   artifact-selection metadata are removed, including artifacts retained from earlier demo versions.
4. The clean bundled full Estonian timetable opens. Locally saved entries stay absent after reload; new local
   work can be saved normally.

### Extensions

- 2a. The visitor cancels or dismisses confirmation. All stored values and current work remain exact; end.
- 3a. Browser storage refuses deletion. The application explains failure, retains the exact prior raw data when
  restoration is possible, and permits retry. If restoration also fails, it explicitly explains that some data
  may have been deleted rather than claiming success or complete preservation; resume at step 2.
- 3b. Stored artifact data is unreadable. Explicit cleanup still removes it without requiring it to parse; continue
  at step 4.

### Guarantees

- G1. Cleanup affects only this demo's local school/timetable artifacts and related metadata. Bundled samples,
  unrelated browser storage/preferences, cookies, other browser owners and server state are unchanged. Deletion
  issues no server mutation request.
- G2. Cleanup requires explicit confirmation. Cancel is the initial confirmation focus. Controls and scope text
  remain readable and keyboard-operable at desktop and 390px widths.
- G3. Pending callbacks from this page and stale writes from another open timetable tab cannot restore the deleted
  artifacts. A refused/stale write does not publish a saved version.
- G4. Failure never claims completed deletion. Raw values are preserved without parsing, including unknown fields
  and malformed retained data, whenever a refused multi-artifact removal can be restored.

### Postconditions

- Success: Only bundled schools/timetables remain; deleted local artifacts stay absent after reload.
- Minimal guarantee: Cancel changes nothing. Failure is explained and never clears unrelated browser data.

## Bundled schools

Exactly these rows — no more, no fewer:

| Identifier | School | Curriculum and people | Scale | Bundled timetables |
|---|---|---|---|---|
| two-class | Ranniku õppehoone | Real Estonian subject/class labels; fictional Estonian staff | 5A and 5B; complete plausible weekly workloads | Variant A, Variant B |
| full | Kasevälja Põhikool | Real Estonian subjects; fictional Estonian staff | Grades 1–9 with parallel classes | Variant A, Variant B |
| star-wars | Coruscant Imperial Academy | Progressive English fictional curriculum; Star Wars specialists teach related subjects | 18 cohorts, 540 lessons, 29 teachers, 37 rooms, 35 periods | Variant A, Variant B |
| english | Meadowbrook Community School | Real English subject labels; fictional English-speaking staff; independently constructed curriculum and placements | Years 1–9; 22 classes, 614 weekly lessons, 41 teachers, 35 rooms, 30 periods | Variant A, Variant B |

Fixture identities and internal references are synthetic. Real source staff names, name-derived identifiers and
identifying policy labels must not enter packaged demo artifacts. The normal examples are not modified.

### Imperial academy progression

Two parallel cohorts per level, with six cohorts in each stage:

| Stage | Levels | Lessons/week | Curriculum emphasis |
|---|---|---|---|
| Junior Cadet | 1–3 | 25 | Galactic Basic and numeracy (5 each); history/science (3 each); droid literacy (2), fitness (3), Academy Citizenship (2), Teamwork and First Aid (2) |
| Intermediate Cadet | 4–6 | 30 | Communication (3), applied physics (4), astrogation/systems/flight simulation/tactical fundamentals/survival/fitness/command law (3 each), emergency medicine (2) |
| Senior Officer Cadet | 7–9 | 35 | Advanced astrogation (3); starfighter operations/fleet tactics/combined operations/mission planning (4 each); intelligence/engineering/leadership/fitness (3 each); combat medicine/officer briefing (2 each) |

Juniors have no advanced operational courses. Intermediate cohorts prepare through fundamentals and simulation;
senior cohorts spend most of their week applying skills to operations, planning, engineering and leadership.
Every course keeps one qualified instructor. Faculty expertise fits its subject group, and practical lessons use
matching specialist facilities. Instructor weekly loads are bounded at 26 lessons. Both variants have 5/6/7 lessons
per day by stage, no class gaps, a first-period start and at most one occurrence of any subject per day.
The definition and placements are independently constructed, rather than relabeling another school's curriculum.

## Out of scope

Accounts, cross-device synchronization, new billing/capacity qualification, automatic repair, timetable comparison
screens, drag-and-drop editing, mutable saved versions, and new export/import interfaces.

## External dependencies

Public redeployment requires the existing Railway credentials/network and service configuration. Local production
API/browser verification proceeds independently. Public launch capacity/billing approval belongs to demo-deployment.
