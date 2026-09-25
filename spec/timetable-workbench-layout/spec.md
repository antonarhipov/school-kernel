# Timetable Workbench Layout Specification

## Feature summary

This feature makes the existing school timetable workbench a compact, wide desktop surface. The complete
whole-school Week or Day timetable remains visible while the administrator inspects Current, prepares a repair,
follows a solve, reviews a proposal, and deliberately accepts a new Current. A small contextual inspector explains
the selected lesson. A separate, wide task area holds repair entry, protection, diagnostics, comparison, and
decisions that do not fit comfortably in the inspector.

This is a presentation successor to `timetable-ux-polish`. It changes the placement and visibility of existing
controls, not the accepted timetable, repair lifecycle, scheduling rules, verification, or authority to mutate
workspace state. Its use cases are new units of work in this feature directory; prior use-case verdicts and the
pending administrator walkthrough remain recorded in their own feature.

## Scope and resolved decisions

- The operational desktop presents school identity, accepted revision, lifecycle state, and available Current,
  Draft, Solving, or Proposal modes once in a compact header. The whole-school canvas begins immediately below a
  compact inspection toolbar and occupies the dominant region. At the 1600 CSS px validation width, the operational
  workbench uses at least 95 percent of the viewport width. The header and toolbar do not scroll with the matrix.
- Week means every definition-declared class and every definition-ordered weekday and period in one scrollable
  surface when no explicit narrowing is active. Day means every declared class and period for the selected weekday.
  “Complete” refers to represented school data, not to all rows fitting in the viewport at once. Class, teacher,
  and room schedules remain drill-downs with a return path to the prior whole-school context.
- The existing single Compact density remains the only lesson density. Subject leads each occupied tile and room is
  visibly present. Teacher, complete names, period, class, and stable lesson identity remain available through the
  selected details and accessible name. No opaque ID is interpreted as a display abbreviation or group label.
- Day/Week, selected weekday, search, subject highlight, and teacher highlight remain directly visible. Search and
  subject/teacher selection highlight without narrowing. A labelled Filters disclosure holds explicit class,
  teacher, room, period, and “show only subject/teacher matches” narrowing. Active narrowing names and represented
  counts remain visible when it is closed; a direct clear action is available. Filters intersect and a narrowed
  population is never labelled complete.
- On viewports at least 1280 CSS px wide, a stable-width, collapsible contextual inspector stays beside the canvas
  even when the task area is open. It shows selected-lesson details and concise state cues. At widths from 701 to
  1279 CSS px, the inspector becomes a collapsible context region beneath the canvas and above the task area, so it
  reserves no permanent column beside the canvas. Explicit selection opens it in either layout; collapsing it
  retains selection and expands the canvas.
- The task area spans the available workbench width beneath the canvas. It never overlays timetable cells. At the
  operational desktop validation sizes, its open height is at most 35 percent of the viewport; its content may
  scroll independently, and the canvas retains its headers and at least one complete class row in view. No control
  needed to finish the active task is clipped or requires the browser window to be resized.
- At 1600 x 900 CSS px, the five weekdays of the normative validation school fit side by side within the canvas
  while the inspector is open, without horizontal scrolling inside the timetable. At 1280 x 800 CSS px, horizontal
  scrolling may occur inside the canvas, but never at page level, and the open task area and canvas remain visible
  together. At 701-1279 CSS px, task sections can stack across the task area's width and the canvas may scroll
  internally. At 700 CSS px or narrower, the existing read-only Day or focused agenda replaces the editing
  workbench.
- Current opens with the task area closed. Requesting repair setup opens it before any durable change. Draft and
  Proposal open their task area on entry or reload. Solving keeps a compact status and cancellation area visible;
  its draft controls are frozen. The administrator may collapse Draft or Proposal task content to scan more of the
  canvas. Open/collapsed state is retained separately for each mode during the page session and resets to these
  defaults on reload. It is not stored with school data.
- The inspector remains open initially at widths at least 1280 CSS px and starts collapsed at 701-1279 CSS px.
  Filters and Utilities disclosures start closed on reload. Only the existing school-scoped Week/Day and weekday
  preference survives reload; search, highlights, narrowing, selection, focus, inspector, task-area, mode, and
  disclosure states do not become durable workspace data.
- Accepted export and applicable diagnostics remain in Utilities. Draft discard, run cancellation, proposal
  revision/discard, and proposal acceptance remain visible in the relevant task area. Initial import and initial
  planning retain their existing separate presentation.
- Existing English workbench vocabulary remains in force. New visible, accessible, status, and error text uses the
  same message catalog and authoritative school-controlled display names; this feature adds no locale choice.
- The current accepted definition/result pair remains authoritative throughout Draft, Solving, and Proposal. A
  proposal is not Current merely because it is feasible. Only explicit, successful durable acceptance advances
  Current. All existing mutation refusals, version checks, process failures, recovery, and atomic acceptance
  behavior continue to apply.
- The proposal's four open layout decisions are resolved here: the task area uses the height and visibility bounds
  above; the inspector remains beside the canvas at both target desktop widths; the named filters are in the
  disclosure with an always-visible active summary; and proposal navigation offers explicit accepted-origin and
  proposed-destination targets for a moved lesson. A target outside the represented Day or narrowed/focused context
  is brought into view by changing only the necessary presentation context, with an explanation and one retained
  lesson identity. These defaults prioritize the whole-school canvas and preserve existing inspection semantics.

## Actors and domain terms

### Actors

- **School timetable administrator:** the local operator who scans the school, stages supported teacher or room
  unavailability, protects accepted assignments, follows a repair, and decides whether to accept a proposal.
- **School Kernel:** the existing supporting scheduler and verifier for complete repair outcomes.
- **Local durable storage:** the existing supporting boundary that retains accepted bundles, drafts, runs, and
  proposals and advances Current atomically.
- **Local presentation preference store:** retains only the existing school-scoped Day/Week and weekday choice on
  this device; it is not authoritative timetable storage.

### Domain terms

- **Accepted baseline / Current:** the verified definition/result pair currently in force. It is never an editable
  assignment canvas.
- **Repair draft / Draft:** durable staged weekly unavailability and protection, shown against accepted assignments;
  it is not a candidate timetable.
- **Solving:** one active repair run from a frozen, conflict-free durable draft; its partial or failed work is never
  a proposal.
- **Repair proposal / Proposal:** one independently verified feasible successor shown in comparison with Current;
  it remains unaccepted until the explicit durable acceptance succeeds.
- **Whole-school canvas:** one Week or Day representation of every declared class and period position in the
  represented range, subject only to explicitly labelled narrowing.
- **Contextual inspector:** the collapsible region for one selected lesson and concise mode context. It does not
  become a full repair form or proposal report.
- **Task area:** the wide, collapsible work region beneath the canvas for repair setup, Draft editing, Solving status,
  or Proposal review and decisions. Its visibility is presentation state.
- **Direct effect:** an accepted lesson implicated by staged repair intent. **Ripple effect:** a change classified
  by the scheduler as necessary beyond that direct effect. **Protected assignment:** accepted period, room, or
  both guarded by an existing policy lock or an attempt-scoped pin.
- **Represented population:** the unique lesson identities included in the selected range after explicit narrowing;
  scrolling and highlighting do not reduce this population.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Scan and inspect the complete school in the compact workbench | School timetable administrator | None |
| UC-2 | Prepare a protected repair in the wide task area | School timetable administrator | Requires UC-1; Extends UC-1 at 5a |
| UC-3 | Follow repair generation while retaining timetable context | School timetable administrator | Requires UC-2 |
| UC-4 | Explain and decide a proposal beside the timetable | School timetable administrator | Requires UC-3 |
| UC-5 | Complete a repair in the wide workbench (primary) | School timetable administrator | Requires UC-1, UC-2, UC-3, UC-4; Includes UC-1 at step 1, UC-2 at step 2, UC-3 at step 3, UC-4 at step 4 |

## State models

### Authoritative repair lifecycle

This feature adds no durable state or transition:

```text
Accepted baseline -- stage supported repair ------------------> Repair draft
Repair draft     -- edit, pin, undo, or recover ---------------> Repair draft
Repair draft     -- request repair from saved ready intent ----> Solving repair
Repair draft     -- confirm draft discard ---------------------> Accepted baseline
Solving repair   -- verified complete feasible result --------> Repair proposal
Solving repair   -- cancel, fail, reject, timeout, or stop ----> Repair draft
Repair proposal  -- explicit durable acceptance --------------> Accepted baseline
Repair proposal  -- revise or discard proposal ---------------> Repair draft
Repair proposal  -- failed durable acceptance -----------------> Repair proposal
```

The accepted baseline stays Current in Repair draft, Solving repair, and Repair proposal. Every other requested
transition is refused without modifying accepted data, draft, run, proposal, or version. Repair setup in Current,
mode selection, inspection, inspector/task-area/disclosure changes, and comparison navigation are presentation
actions, not lifecycle transitions.

### Available modes and task-area defaults

This table has exactly the four repair-lifecycle rows governed by this feature—no more and no fewer.

| Authoritative state | Available presentation modes | Default mode on entry or reload | Task area in the default mode |
|---|---|---|---|
| Accepted baseline | Current | Current | Closed; repair setup opens on request |
| Repair draft | Current, Draft | Draft | Open for editable intent and protection |
| Solving repair | Current, Draft, Solving | Solving | Compact run status and cancellation visible |
| Repair proposal | Current, Draft, Proposal | Proposal | Open for comparison and decisions |

Selecting an available mode changes presentation only. An unavailable mode cannot be selected. When a transition
removes a mode, the new authoritative state's default is selected. Current mode closes the task area even while
a Draft or Proposal exists; returning to Draft or Proposal restores that mode's page-session open/collapsed choice.
Reload uses the table defaults. Solving's status and cancellation remain visible even when its detail is collapsed.

### Presentation-state retention

| Dimension | Retention and reset |
|---|---|
| Week/Day and selected weekday | Retained across modes; only the existing valid school/device preference may survive reload |
| Search, subject and teacher highlight, explicit filters | Retained across modes while identities remain valid; reset on reload |
| Selected lesson identity | Retained across modes when representable; otherwise cleared with an explanation; selection of either comparison side uses one lesson identity |
| Focused class, teacher, or room schedule | Return restores the prior whole-school range, investigation, filters, selection when representable, and scroll context |
| Canvas scroll | Retained across mode and task-area changes when its represented position still exists |
| Inspector, task area, Filters, Utilities | Presentation-only page-session state; default on reload as stated above |

Opening or closing an inspector, task area, filter, or utility disclosure causes no durable workspace mutation.
When a review target lies outside the current Day or narrowed/focused context, the workbench returns to the
whole-school canvas if needed, selects the target's weekday, clears only explicit narrowing criteria that exclude
the target, and announces each change. It keeps subject/teacher highlights and search, which do not narrow. The
selected lesson identity and exact accepted/proposed relationship remain intact.

### Combined proposal presentation

- Unchanged lessons remain in one accepted placement and visually quiet.
- A period move has a labelled accepted origin and proposed destination on the same canvas. Both select the same
  stable lesson identity and count as one changed lesson.
- A teacher or room change that stays in one class-period position has one combined representation with both old
  and proposed values. An addition has only a proposed representation; a cancellation has only an accepted one.
- Direct and ripple explanations may overlap. They are not added together to invent a unique changed-lesson total.
- Proposal review matches a lesson when either accepted or proposed side satisfies a search or explicit filter;
  both sides needed to explain that lesson remain available and the matching side is identified.
- Current, Draft, and Solving show accepted-definition availability. Proposal labels proposed availability from
  the verified successor separately. Exact values and stable IDs are never inferred from an opaque identifier.

## UC-1 - Scan and inspect the complete school in the compact workbench

- Goal: Find and understand an exact accepted lesson while keeping the full school in view.
- Primary actor: School timetable administrator
- Supporting actors: Local presentation preference store
- Trigger: The administrator opens an operational workspace with a verified accepted baseline or selects Current
  from another available presentation mode.
- Preconditions: A verified accepted definition/result pair exists; extension 1a covers a purported pair refused
  before that precondition can be established.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator opens the workbench at an operational desktop width.
2. The workbench shows the school, accepted revision, authoritative lifecycle state, available modes, Current as
   accepted, and the represented Week or Day above a dominant whole-school canvas. The task area is closed in
   Current and the contextual inspector has only concise context until a lesson is selected.
3. The administrator changes Day/Week or weekday, searches, highlights a subject or teacher, opens Filters, applies
   an explicit narrowing criterion, or clears it.
4. The workbench updates the same timetable without changing accepted data. Search and highlights preserve the
   represented population; explicit narrowing intersects and remains named with an exact unique-lesson count even
   when Filters is closed. Teacher availability uses the accepted definition and assignments.
5. The administrator selects a lesson, may continue with UC-2 at extension point 5a, or opens a class, teacher, or
   room schedule and returns to the whole school.
6. The workbench opens the inspector with exact accepted subject, class, teacher, weekday, period, room, stable
   lesson identity, and accepted status. Returning from focus retains the representable whole-school context.
7. The administrator collapses and reopens the inspector or opens Utilities.
8. The workbench preserves selection while the inspector is collapsed, expands the canvas, and restores the same
   details on reopening. Utilities offer applicable export or diagnostics without moving lifecycle decisions there.

### Extensions

- 1a. If a purported accepted pair is rejected for missing display metadata, an unmappable assignment, or another
  invalid input, the workspace reports the refusal through the existing import journey, presents no Current lesson
  or operational inspector, and leaves authoritative state unchanged; end.
- 1b. If the viewport is 700 CSS px or narrower, the workspace shows a read-only Day or focused agenda and the
  authoritative lifecycle state, exposes no repair mutation or proposal decision, and makes no complete-desktop
  claim; end.
- 2a. If the accepted timetable has no assignments, the whole-school canvas still shows every declared class,
  weekday, period, and empty position, with no invented lesson; resume at step 3.
- 3a. If a stored range preference or selected presentation identity is invalid or unavailable, the workbench
  applies the existing safe range fallback, explains any cleared context, and changes no authoritative state;
  resume at step 3.
- 4a. If explicit filters match no lessons, the workbench states that the represented set is empty, retains the
  declared time structure and a direct clear action, and does not call that result the complete school;
  resume at step 3.
- 5a. If the administrator chooses a supported repair from the whole-school or selected context, continue with
  UC-2; end.
- 6a. If a focused class, teacher, or room has no accepted lesson, the workbench identifies that entity and its
  empty recurring schedule without inventing an assignment; resume at step 5.
- 8a. If accepted export fails, Utilities report that no bundle was produced; the canvas, selection, accepted
  baseline, draft, run, and proposal remain unchanged; end.

### Guarantees

- G1. On the 1600 x 900 normative viewport, all five definition-declared weekdays in the validation school fit
  across the Week canvas with the inspector open and no internal horizontal scroll, and the operational shell uses
  at least 95 percent of the viewport width. On the 1280 x 800 viewport, page-level horizontal scroll and clipped
  controls are absent; the canvas itself may scroll horizontally. At 701-1279 CSS px the inspector occupies a
  collapsible region beneath the canvas rather than a permanent side column.
- G2. One fixed Compact density retains visible room labels, subject-first occupied tiles, full authoritative
  details on selection, and definition-ordered class/time structure. At 1600 x 900 an ordinary occupied Week
  period slot without extra state cues is at most 36 CSS px high and an ordinary occupied Day cell at most 60 CSS
  px high; long names or multiple cues may increase height rather than clip content. Long names may elide in a
  tile but remain complete in the inspector and accessible name; opaque IDs are never used to guess display meaning.
- G3. Header and controls remain outside the matrix's own scroll region; class and time headings remain available
  while the matrix scrolls. A focused schedule is a drill-down and cannot be described as the whole school.
- G4. Mode, range, preference, highlight, search, filter, focus, inspector, task-area, utility, and scroll actions
  never modify accepted, draft, run, proposal, or policy state. A storage failure for the optional local range
  preference does not prevent the current inspection action.
- G5. State, complete-versus-narrowed population, selected lesson, highlights, and accepted status have textual or
  structural cues as well as color. Every action in this scenario has a visible pointing-device control and native
  keyboard operation; no custom matrix keyboard-navigation system is required. New visible and accessible labels
  use existing English workbench vocabulary and authoritative school-controlled names.
- G6. Existing local-only access and safe display remain unchanged. No role, login, logout, remotely reachable
  school-data route, or additional disclosure is introduced; a denied request exposes no timetable data and changes
  no workspace state.
- G7. The real-browser evidence uses the normative snapshot and viewports, checks Week/Day, focused return,
  inspector and Filters collapse/reopen, Utilities, narrow read-only behavior, and exact stored state before and
  after presentation actions.

### Postconditions

- Success: The administrator can inspect an exact accepted lesson in a compact complete-school context and return
  from focused schedules without losing representable context.
- Minimal guarantee: An invalid preference, empty or unmappable selection, failed utility action, or narrow
  viewport invents no lesson, discloses no unauthorized data, and changes no accepted or other authoritative state.

## UC-2 - Prepare a protected repair in the wide task area

- Goal: Stage supported recurring unavailability and protection while seeing its effect on the accepted school.
- Primary actor: School timetable administrator
- Supporting actors: Local durable storage
- Trigger: From UC-1 Current, the administrator requests repair setup for an unavailable teacher or room, or
  returns to an editable Draft after proposal revision.
- Preconditions: UC-1's accepted workbench exists, the existing lifecycle permits a new or revised repair draft,
  and no repair run is active.
- Relations:
  - Requires: UC-1, because repair intent overlays its accepted whole-school canvas
  - Includes: none
  - Extends: UC-1 at extension point 5a

### Main success scenario

1. The administrator requests repair setup. The wide task area opens with resource and recurring weekday/period
   choices; Current remains accepted and merely opening the area saves nothing.
2. The administrator names one supported teacher or room and the weekly periods of unavailability, then stages the
   intent.
3. The workspace durably enters Repair draft, selects Draft, opens the editable task area, and marks direct effects
   and blocking conflicts on the accepted canvas while stating that accepted assignments remain Current.
4. The administrator inspects an affected or conflicting lesson, resolves any contradictory instruction, and
   protects its accepted period, room, or both when appropriate.
5. The administrator previews the exact lesson set and dimensions of an applicable bulk pin, confirms the preview,
   or undoes a previously confirmed bulk action.
6. The task area shows the saved weekly intent, direct effects, conflicts, individual and bulk protection, readiness,
  and solve/discard actions in usable sections. The administrator may collapse it, inspect Current, use a focused
   schedule, and return to Draft without losing representable range, investigation, selection, or that mode's
   page-session open/collapsed choice.
7. The administrator leaves one saved conflict-free Draft ready for repair generation.

### Extensions

- 1a. If the administrator closes repair setup before staging intent, the task area closes, Current remains the only
  authoritative repair state, and no draft or version change is made; end.
- 1b. If the viewport is 700 CSS px or narrower, the workspace identifies Current or Draft read-only, exposes no
  setup, stage, pin, bulk, solve, or discard action, and changes no state; end.
- 1c. If the administrator returns from Proposal revision to an existing durable Draft, the workbench opens its
  saved intent and protection in the wide task area without creating a second Draft, and keeps Current accepted;
  resume at step 4.
- 2a. If the resource or period selection is invalid, the workspace explains the invalid choice, creates no new
  Draft, starts no run, and keeps Current unchanged; resume at step 2.
- 2b. If a draft write fails or is stale, the workspace reports that the requested revision was not saved, keeps
  the prior accepted bundle and last durable Draft exact, and prevents repair generation from an unsaved revision;
  resume at step 2.
- 3a. If no accepted lesson is directly affected, Draft states that the weekly rule currently contradicts no
  accepted assignment, keeps the staged intent, and invents no direct effect; resume at step 4.
- 4a. If a pin conflicts with staged unavailability or an existing lock, the exact conflict remains visible in the
  task area and on the canvas, no instruction silently wins, and solving remains unavailable; resume at step 4.
- 4b. If a selected conflict or protected lesson is outside the represented Day or explicit filters, the workbench
  brings it into view under the presentation-state retention rule, explains any change in view, and keeps the
  saved Draft and accepted baseline unchanged; resume at step 4.
- 5a. If the administrator cancels a bulk preview, no lesson in that preview becomes pinned and the prior Draft
  remains exact; resume at step 5.
- 5b. If bulk confirmation or undo cannot be saved, the workspace reports the refusal, keeps the last durable
  protection and accepted baseline exact, and does not enable solving from an unpersisted change; resume at step 5.
- 6a. If the administrator explicitly confirms Draft discard, the workspace removes staged intent and
  attempt-scoped pins, returns to Accepted baseline and Current, and closes the task area; end.
- 6b. If the application reloads, the durable Draft remains, Draft is selected by default, the task area opens,
  and only the existing Week/Day preference may be restored; resume at step 6.
- 7a. If a blocking conflict or unsaved revision remains when generation is requested, the workspace refuses the
  run, identifies the blocking reason, leaves Current and the last durable Draft exact, and starts no scheduler;
  resume at step 4.

### Guarantees

- G1. Draft overlays accepted assignments. It never edits accepted assignment data or calls the Draft a current
  candidate timetable. Period/room pins protect existing accepted values and remain attempt-scoped under the
  existing contract.
- G2. The task area holds the period picker, conflict list, bulk preview/history, readiness, and lifecycle actions
  across its width; the inspector holds selected-lesson details and concise cues. At both target desktop sizes,
  the open task area and at least one complete class row with time headings are concurrently visible, no task
  control is clipped, the area occupies at most 35 percent of viewport height, and it never overlays timetable
  cells.
- G3. Task-area, inspector, mode, filter, range, focus, and scroll changes save nothing. Only explicit, authorized
  draft actions may alter the durable Draft; every such action retains the existing version and local-access
  refusal boundary.
- G4. Direct effect, blocking conflict, individual pin, bulk pin, persistent policy lock, and unpinned dimension
  remain distinguishable without color alone. Exact lesson and resource identities, weekly periods, pin dimensions,
  and conflict reasons are preserved. Invalid or unmappable resource references are refused before a new Draft is
  presented; an optional diagnostic name that is unavailable carries its stable ID and an unavailable-name cue.
  New instruction, status, and error text uses the existing English workbench vocabulary.
- G5. A bulk preview changes no durable intent; only confirmation applies the exact previewed lesson IDs and
  dimensions, and undo identifies the action it reverses. A failed or stale save cannot silently change a pin or
  make a Draft eligible for solving.
- G6. Narrow screens remain read-only. At intermediate desktop widths the task area remains wide, sections may
  stack, and no repair control is moved back into a permanently narrow inspector.
- G7. Real-browser evidence at normative scale exercises teacher and room intent, no-effect intent, conflict,
  individual and bulk protection, preview cancel/confirm/undo, save failure, collapse/reopen, focused return,
  reload, discard, narrow refusal, and exact accepted/Draft state.

### Postconditions

- Success: A saved, conflict-free repair Draft is ready to generate while the same accepted whole-school timetable
  remains visible and Current.
- Minimal guarantee: A refused, conflicting, cancelled, stale, or unsaved draft action leaves the last durably
  accepted baseline exact, starts no ineligible run, and never claims an unsaved change was applied.

## UC-3 - Follow repair generation while retaining timetable context

- Goal: Monitor or cancel a repair run without losing the accepted timetable or frozen intent.
- Primary actor: School timetable administrator
- Supporting actors: School Kernel and local durable storage
- Trigger: The administrator requests generation from a saved, conflict-free UC-2 Draft.
- Preconditions: UC-2 produced a ready durable Draft, no repair run is active, and the existing lifecycle permits
  generation.
- Relations:
  - Requires: UC-2, because the run consumes its saved intent and accepted canvas
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests repair generation from the Draft task area.
2. The workspace enters Solving repair, selects Solving, freezes Draft mutation, keeps the accepted canvas usable,
  and presents frozen intent, run limit, status, and cancellation in a compact visible task area.
3. The administrator inspects an accepted lesson, changes range or focus, or selects Current or frozen Draft while
  the run continues.
4. The workbench preserves representable context, keeps Current labelled accepted, and identifies the running
  outcome as neither Current nor a verified proposal.
5. School Kernel produces a complete feasible result for the exact frozen Draft.
6. The workspace independently verifies it, enters Repair proposal, removes Solving, selects Proposal, opens its
  review task area, and retains the representable canvas context for UC-4.

### Extensions

- 1a. If Draft is stale, conflicting, or not durably saved, the workspace refuses generation, starts no run,
  preserves Current and the last durable Draft, and returns to editable Draft; end.
- 2a. If the administrator cancels, the workspace stops the run under the existing cancellation boundary,
  returns to Repair draft and Draft mode with its editable task area open, creates no Proposal, and preserves
  Current; end.
- 2b. If the application stops during solving, reopening restores the durable Draft and editable task area,
  records the safe terminal outcome under the existing recovery contract, and makes no Proposal eligible; end.
- 3a. If a selected context cannot be represented in another available mode, the workbench clears only that
  context with an explanation and leaves the run, Current, and remaining presentation state unchanged;
  resume at step 3.
- 5a. If the run finds no feasible result within its limit, fails, times out, or reports safe diagnostics, the
  workspace returns to editable Draft, displays safe outcome and applicable diagnostics in the task area and
  Utilities, exposes no candidate, and preserves Current and Draft; end.
- 5b. If no feasible repair was found within the run limit and intent remains unchanged, the administrator may
  request the existing two-minute retry preset; the workbench starts a new run from the same durable Draft at
  step 2. Cancellation and other failures do not make that unchanged-intent retry available, and a prior failed
  result never appears as Proposal; resume at step 2.
- 6a. If a returned result is incomplete, stale, mismatched, non-feasible, or rejected by independent
  verification, the workspace rejects it, returns to Draft with safe diagnostics, exposes no candidate tiles or
  Proposal mode, and preserves Current and Draft; end.

### Guarantees

- G1. Draft editing, pinning, discard, and proposal decisions are unavailable during Solving. Cancellation is
  the only lifecycle-changing administrator action presented for the active run.
- G2. The compact status and cancellation remain visible at both desktop validation sizes while the canvas
  retains its headers and at least one complete class row; the task area does not cover cells or become a
  narrow inspector form. It occupies at most 35 percent of viewport height, and run status and cancellation
  stay visible when its secondary detail is collapsed.
- G3. Current, frozen Draft, and Solving describe the same accepted lesson identities and retained presentation
  context. Switching modes, range, inspector, task detail, or focus neither restarts nor cancels the run and
  changes no accepted or Draft data.
- G4. Only an independently verified complete feasible result creates Proposal. Partial, unsuccessful, late,
  invalid, or rejected output is not rendered as a candidate and never becomes Current.
- G5. The run limit, status, cancellation, safe outcome, and diagnostics use text as well as visual cues and
  remain accessible by native controls and use existing English workbench vocabulary. A bounded result is never
  described as globally optimal.
- G6. Narrow screens identify the authoritative Solving state but expose no cancellation or other mutation.
  Existing local access, bounded execution, identity, cancellation, recovery, and refusal guarantees remain.
- G7. Real-browser evidence at normative scale covers inspection during a live run, mode and range changes,
  cancellation, actual restart recovery, unsuccessful and rejected outcomes, eligible retry, verified transition
  to Proposal, and exact accepted/Draft/run state.

### Postconditions

- Success: One independently verified Proposal is available for review on the retained canvas while the prior
  accepted baseline is still Current.
- Minimal guarantee: Cancellation, restart, failure, timeout, or rejected output leaves the accepted baseline
  and durable Draft exact and exposes no unverified Proposal.

## UC-4 - Explain and decide a proposal beside the timetable

- Goal: Understand whole-school proposal impact in a wide review area and deliberately accept or reject it.
- Primary actor: School timetable administrator
- Supporting actors: Local durable storage
- Trigger: The workspace enters Repair proposal or the administrator selects available Proposal mode.
- Preconditions: UC-3 produced one independently verified feasible proposal matching the accepted baseline and
  retained Draft.
- Relations:
  - Requires: UC-3, because review consumes its verified proposal and accepted comparison context
  - Includes: none
  - Extends: none

### Main success scenario

1. Proposal mode shows the accepted and proposed placements on the same whole-school canvas, labels Current as
  accepted and Proposal as not current, and opens the wide review task area.
2. The task area presents the authoritative unique changed-lesson total, category and grouping counts, direct and
  ripple effects, protection, and safe run evidence. The inspector remains concise until a lesson is selected.
3. The administrator selects a changed lesson in a category or on the canvas and, for a period move, chooses its
  accepted origin or proposed destination.
4. The workbench brings that representation into view, keeps one stable lesson identity selected, and shows exact
  accepted and proposed subject, class, teacher, weekday, period, room, changed dimensions, effect explanation,
  and protection in the inspector and review area.
5. The administrator inspects other changes, may switch among Current, Draft, and Proposal or a focused schedule,
  confirms which timetable is accepted, and explicitly confirms acceptance of the verified Proposal.
6. The workspace revalidates identity and feasibility, durably accepts the complete successor, removes Draft and
  Proposal, makes only Current available, closes the task area, and shows the newly accepted whole-school timetable
  in its retained representable context. If the selected lesson no longer appears in that context, the workbench
  clears only that selection and explains why.

### Extensions

- 1a. If a proposal has become ineligible or a candidate has missing required names, unmappable references, or
  another failed verification, Proposal mode and proposed placements are unavailable; Current remains accepted
  and the workspace shows the applicable Draft or safe refusal; end.
- 1b. If the viewport is 700 CSS px or narrower, the workspace identifies Repair proposal and shows the existing
  read-only accepted/proposed agenda without revise, discard, or accept actions or a claim of complete desktop
  review; end.
- 2a. If a category or grouping has no lessons, the task area shows zero and its label rather than silently
  omitting the category; resume at step 3.
- 3a. If the chosen side is outside the represented Day or a narrowing/focused context, the workbench changes
  only the necessary presentation context under the retention rule, explains the adjustment, and keeps highlights,
  search, and lesson identity; resume at step 4.
- 3b. If teacher or room changes within one class-period slot, the canvas shows one combined changed tile while
  the review area still exposes old and proposed values; resume at step 4.
- 3c. If a lesson is added or cancelled, only its existing proposed or accepted side appears, explicitly marked
  addition or cancellation; resume at step 4.
- 4a. If an unchanged lesson is selected, the inspector identifies its accepted values and unchanged status,
  and does not add it to any change total; resume at step 5.
- 5a. If the administrator chooses to revise intent, Proposal eligibility ends, the workspace returns to the
  retained Draft and its editable task area, and Current stays unchanged; continue with UC-2.
- 5b. If the administrator discards the Proposal, the workspace removes only the Proposal, returns to the
  retained Draft with its task area open, and keeps Current and Draft exact; end.
- 6a. If accepted or proposal identity is stale or mismatched, acceptance is refused, no accepted data changes,
  the proposal is invalidated under the existing lifecycle, and the workbench returns to Draft with an explanation;
  end.
- 6b. If durable acceptance fails, the workbench reports that Current did not advance, retains the exact old
  accepted bundle and reviewable Proposal for explicit retry, and remains in Proposal with its task area open; end.

### Guarantees

- G1. A period move's origin and destination, a same-slot change, an addition, a cancellation, and an unchanged
  lesson map to stable lesson identities. Tile count is never used as the unique lesson or changed-lesson count.
  Overlapping categories and direct/ripple membership do not create a false sum.
- G2. Review navigation and explicit filters match either accepted or proposed side and retain both sides needed
  to explain one changed lesson. The review area names the target side; exact accepted/proposed fields and stable
  IDs remain distinguishable even when labels are long. A candidate missing required display metadata is refused
  before Proposal is shown; unavailable optional labels use a stable ID and explicit unavailable-name cue.
- G3. The wide review area contains navigable categories, groupings, before/after detail, and decision actions.
  At both desktop validation sizes it remains usable with the canvas, its time headings, and at least one complete
  class row concurrently visible, occupies at most 35 percent of viewport height, and never overlays cells.
  Collapsing and reopening it retains selected change, counts, and representable canvas scroll. No decision
  control is clipped or relegated to a long 300 px inspector report.
- G4. Current, Draft, and Proposal mode, range, focus, selection, inspector, task-area, search, and filter actions
  do not mutate accepted, Draft, or Proposal state. Only explicit existing revise, discard, and confirmed
  acceptance actions may change durable state.
- G5. Accepted origin, proposed destination, combined change, addition, cancellation, direct effect, ripple
  effect, protection, selection, and unchanged status use text or structural cues in addition to color. Proposal
  availability is labelled proposed and derived from the verified successor; Current availability remains
  accepted. New comparison, status, and decision text uses existing English workbench vocabulary.
- G6. Only explicit confirmed and durably successful acceptance advances Current. Stale identity, failed
  verification, or failed storage leaves the old accepted bundle exact and never presents an unaccepted result
  as Current. Existing local access and atomic refusal boundaries remain.
- G7. Real-browser evidence at normative scale covers every comparison shape, one-sided narrowing, empty and
  overlapping counts, protected/unchanged lessons, both origin/destination targets, focused return, task-area
  collapse/reopen, revise, discard, stale refusal, failed and successful acceptance, narrow read-only behavior,
  and exact durable state.

### Postconditions

- Success: The exact verified successor is Current, only Current mode remains, and the administrator can inspect
  its complete whole-school context without a residual repair task area.
- Minimal guarantee: Review, navigation, refusal, revision, discard, or failed acceptance never partially
  advances or mislabels Current; the last durably accepted baseline remains exact.

## UC-5 - Complete a repair in the wide workbench (primary)

- Goal: Carry a supported weekly disruption through one repair and return to a trusted Current timetable without
  losing whole-school visual context.
- Primary actor: School timetable administrator
- Supporting actors: School Kernel and local durable storage
- Trigger: A supported recurring teacher or room unavailability requires repair.
- Preconditions: A verified accepted baseline exists in the operational workbench.
- Relations:
  - Requires: UC-1, UC-2, UC-3, and UC-4 through their included successful goals
  - Includes: UC-1 at step 1, UC-2 at step 2, UC-3 at step 3, and UC-4 at step 4
  - Extends: none

### Main success scenario

1. The administrator uses UC-1 to locate the affected lesson and accepted whole-school context in the compact
   Current workbench.
2. The administrator uses UC-2 to stage the disruption, inspect its direct effects, resolve conflict, protect
   accepted values in the wide task area, and leave a saved conflict-free Draft.
3. The administrator uses UC-3 to run a repair while the accepted canvas, frozen intent, run status, and
   cancellation remain understandable and responsive.
4. The administrator uses UC-4 to inspect accepted and proposed impact across the canvas and review area,
   identifies Current correctly, and explicitly accepts the verified Proposal.
5. The workbench presents only the newly accepted Current, retains representable Week/Day and inspection context,
   closes the task area, and permits a later repair from that exact accepted successor.

### Extensions

- 2a. If Draft remains conflicting or a requested change is not durable, the workbench retains navigable
  explanations in the task area, starts no run, and leaves Current exact; end.
- 3a. If the run is cancelled, interrupted, unsuccessful, or rejected, the workbench returns to the exact durable
  Draft, presents no Proposal, and leaves Current exact; end.
- 4a. If the administrator revises or discards Proposal, the workbench returns to the retained Draft and old
  Current without implicit acceptance; end.
- 4b. If acceptance is stale or storage fails, the workbench applies UC-4's refusal or retryable-failure path,
  identifies the old Current correctly, and never shows a partial new baseline; end.
- 5a. If the administrator begins another supported repair, the new Draft is based on the newly accepted bundle,
  and carries no prior attempt-scoped pins unless explicitly reapplied; continue with UC-2.

### Guarantees

- G1. The same accepted identities, authoritative school-controlled names, definition-ordered time model, and
  selected-lesson relationship support all four presentation modes, both ranges, task areas, inspector, and
  focused schedules. A mode change cannot create a second timetable authority.
- G2. At every decision point the header, active mode, task area, and canvas cues distinguish accepted Current,
  staged Draft, running accepted view, and non-current Proposal. Neither compactness nor a collapsed task area
  hides the authoritative state or a blocking reason.
- G3. At both operational desktop sizes the task area and at least one complete class row remain visible together
  during Draft, Solving, and Proposal. Narrow view remains read-only. Layout changes never broaden the existing
  lifecycle, local-access, version, process, verification, or acceptance authority.
- G4. Failure at any step preserves the last durably accepted bundle. Draft intent, failed output, a running
  candidate, and an unaccepted Proposal are never displayed as Current.
- G5. The complete journey is exercised in a real browser with an isolated durable workspace, a packaged
  scheduling-process boundary, and the normative school. Exact accepted, Draft, run, Proposal, and version values
  are compared after consequential actions; all shared browser regressions and the human feature gate below
  remain required.

### Postconditions

- Success: A deliberately accepted repair is Current, its whole-school impact was reviewable with a wide task
  area and continuously visible canvas, and the next repair can start from that exact successor.
- Minimal guarantee: If inspection, staging, solving, review, or acceptance cannot complete, the last durable
  accepted baseline remains exact and identifiable as Current.

## Normative validation data

### Validation school and repair journey

The generated or properly anonymized accepted definition/result pair used for this feature has:

- exactly five definition-declared weekdays for the desktop width claim, with all definition-declared periods
  in their authoritative order, exactly 12 periods per weekday (60 total), and at least one declared empty position;
- at least 60 classes, 100 teachers, and 100 rooms with opaque stable IDs and long school-controlled names;
- 900-1,100 accepted lesson assignments across the complete recurring week, verified against the matching
  accepted definition;
- subjects and teachers that support independent and simultaneous highlighting, a search hit, filters that
  narrow to matches and to zero, and labelled complete-versus-narrowed counts;
- teachers that are assigned, available but unassigned, and unavailable in selected periods;
- populated and empty class, teacher, and room focused schedules;
- a supported teacher-unavailability Draft, a supported room-unavailability Draft, a zero-direct-effect case,
  one blocking pin conflict, individual period/room pins, a bulk preview and confirmed/undone bulk pin, and a
  saved conflict-free Draft;
- one independently verified feasible Proposal with a period move, a same-slot room change, one direct effect,
  one ripple effect, a protected unchanged lesson, and at least one empty change category;
- separate cancelled, unsuccessful, rejected-output, stale-acceptance, and failed-persistence outcomes.

Additional ordinary classes, resources, lessons, and empty positions are allowed within the stated counts. A
supplementary presentation-only fixture may exercise addition and cancellation shapes that the current supported
repair journey cannot produce; it must not be represented as a verified candidate eligible for acceptance. Group
membership, primary-room policy editing, and new repair instruction types are not inferred from the prototype.

### Viewport evidence

This table has exactly the three required viewport rows for this feature—no more and no fewer.

| Viewport in CSS px | Observable check |
|---|---|
| 1600 x 900 | Workbench uses at least 95 percent of viewport width; five validation weekdays fit across Week with inspector open; ordinary Week/Day cells meet UC-1 G2; Draft and Proposal task area never overlays canvas, and a complete class row and time headings remain visible with it open |
| 1280 x 800 | Inspector and open task area coexist with visible canvas headings and one complete class row; no page-level horizontal scroll, clipped controls, or forced browser resize |
| 390 x 844 | Read-only Day or focused agenda identifies the lifecycle and exposes no repair edit, run cancel, or proposal decision |

Screenshots and browser observations use the same validation school at both desktop sizes and record visible class
rows, any internal scroll, label legibility, and control access. Exact viewport claims are tested with long names,
direct/conflict/pin/proposal cues, and the task area open, not only an empty Current screen.

### Administrator feature gate

Every participant performs exactly these six tasks—no more and no fewer for this feature gate:

| Task | Observable completion |
|---|---|
| Orient in Current | Identifies school, accepted revision, complete Week or precise Day, and one exact accepted lesson in the compact workbench |
| Prepare Draft | Stages the supplied disruption in the wide task area, explains a direct effect and blocking conflict, applies required protection, and states that accepted data is unchanged while canvas and task area remain visible |
| Retain context while solving | Starts repair, inspects Current and frozen Draft during the run, identifies cancellation in the compact status area, and never calls a running outcome Current or optimal |
| Explain Proposal | Uses the combined canvas and wide review area to distinguish accepted origin, proposed destination or same-slot change, direct effect, ripple effect, and protection |
| Navigate without confusion | Uses Current, Draft, Proposal, focused return, inspector and task-area collapse/reopen, Filters, and a review target outside the current Day or narrowing while retaining or correctly explaining context changes |
| Decide and verify Current | Explicitly accepts the supplied verified Proposal and identifies only the new accepted Current with no residual Draft, Proposal, or task area |

The feature gate requires five participants with real timetable responsibility from at least three schools. At
least four of five must complete every task without serious error or facilitator correction, and all five must
identify the accepted Current correctly at every decision point. Record each participant's school, task outcome,
serious errors, corrections, Current answers, and concrete layout friction such as obscured timetable context,
control clipping, or unnecessary navigation. Include browser evidence at all normative viewports and exact
durable-state evidence across inspection, failure, discard, and acceptance. A general approval response or an
automated pass alone does not satisfy this feature gate.

## Out of scope

- Initial import, initial planning, and initial-proposal presentation redesign.
- Cohort partitions, inferred or rendered split groups, group-target editing, and new kernel group semantics.
- Primary-room policy creation or exceptions, new repair instructions, substitutions, or solver changes.
- Direct mutation of accepted assignments, drag-and-drop repair, a second complete proposal canvas, or an
  accepted/proposed toggle that hides the combined comparison.
- New accounts, roles, login/logout, hosted-school switching, remote publication, or new school-data access paths.
- New durable workspace fields, database structures, routes, accepted-bundle formats, or browser storage for mode,
  filter, inspector, or task-area state.
- A density switch, custom application zoom, mobile repair editing/cancellation/acceptance, or custom keyboard
  navigation system for the matrix.

## External dependencies

- The new administrator feature gate depends on five timetable professionals from at least three schools and the
  per-participant evidence above. Until that evidence exists, automated convergence cannot be described as
  administrator approval.
- Real-browser validation requires isolated durable workspace data, the production scheduling-process boundary,
  a verified or properly anonymized validation school with known provenance, and the normative viewport evidence.
  An unavailable browser, scheduler, storage environment, or participant group is reported as a validation limit.
