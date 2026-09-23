# Timetable UX Polish Specification

## Feature summary

This feature turns the accepted-timetable, repair-draft, solving, and repair-proposal presentations into one coherent
desktop workbench. The complete whole-school timetable remains the dominant surface while the administrator inspects
the accepted baseline, stages repair intent, monitors a solve, reviews a proposal, and decides whether that proposal
becomes current.

The feature changes presentation and navigation only. The accepted baseline, repair intent, proposal identity, kernel
outcomes, workspace lifecycle, persistence, optimistic concurrency, cancellation, recovery, and atomic acceptance
behavior remain authoritative under the existing timetable-workspace contract.

## Scope and resolved decisions

- The production workbench follows the approved prototype reference named in the proposal without treating its
  illustrative lessons, groups, abbreviations, or states as authoritative data.
- Current, Draft, Solving, and Proposal are presentation modes over one workbench. The authoritative lifecycle state
  determines which modes exist; choosing a mode never changes lifecycle or durable state.
- Current, Draft, and Proposal are directly selectable whenever their corresponding data exists. Solving is available
  only while a repair run exists. On entering a lifecycle state, its corresponding mode is selected by default.
- The complete whole-school Day/Week timetable is the dominant desktop surface in every available mode. Class,
  teacher, and room schedules remain secondary drill-downs.
- The workbench uses the accepted inspection contract's single Compact density, Week/Day transitions, subject and
  teacher investigation, search, explicit filters, counts, teacher availability, lesson selection, and focused
  schedules.
- Proposal comparison defaults to one combined overlay. Accepted origins and proposed destinations coexist on the
  same timetable, while the inspector exposes exact before/after values.
- The contextual inspector has one fixed open width on supported desktop layouts and can be collapsed. It is open by
  default, is not resizable, and its collapsed state is presentation-only and resets on reload.
- Explicitly selecting a lesson, draft conflict, pin, diagnostic, or proposal change opens the inspector. Collapsing it
  retains the current selection and context, expands the timetable area, and leaves a visible control and summary for
  reopening it.
- Export and applicable diagnostics live in a compact header `Utilities` disclosure. Import remains on the empty
  workspace journey. Draft discard, cancellation, proposal revision, proposal discard, and proposal acceptance remain
  with the relevant mode context rather than in the utility disclosure.
- The existing local single-administrator access boundary remains unchanged. This feature adds no account, role,
  login, logout, hosted-school switcher, or new access outcome.
- Existing user-visible English vocabulary remains in force. School-controlled display names and stable identities are
  authoritative; the workbench never derives display meaning from opaque identifiers.
- Narrow screens remain read-only Day or focused agendas. They identify the authoritative lifecycle state but expose
  no repair mutation, run cancellation, proposal revision, proposal discard, or proposal acceptance action and do not
  claim to provide the complete desktop workbench.
- Initial import, initial planning, and initial-proposal presentation are not redesigned by this feature. The polished
  workbench begins once a verified accepted baseline exists.

## Actors and domain terms

### Actors

- **School timetable administrator:** the local operator who inspects, repairs, reviews, and accepts the school's
  recurring timetable.
- **School Kernel:** the existing supporting scheduler that produces authoritative repair outcomes and change
  classification.
- **Local durable storage:** the existing supporting boundary that retains accepted bundles, repair drafts, runs, and
  proposals and advances an accepted baseline atomically.

### Domain terms

- **Authoritative lifecycle state:** one of Accepted baseline, Repair draft, Solving repair, or Repair proposal. It
  determines which data and actions exist and is never changed by presentation navigation.
- **Presentation mode:** Current, Draft, Solving, or Proposal; one view of data already available in the authoritative
  lifecycle state.
- **Current mode:** the verified accepted definition/result pair currently in force.
- **Draft mode:** the accepted timetable with staged repair intent, direct effects, conflicts, and protection overlaid;
  it is not a candidate timetable.
- **Solving mode:** the accepted timetable and frozen draft context while one repair run is active.
- **Proposal mode:** the accepted timetable and one verified feasible proposal shown as a combined comparison; the
  proposal is not current.
- **Persistent canvas:** the whole-school Week or Day timetable whose range, investigation context, selection, and
  representable scroll context remain stable when modes change.
- **Combined proposal overlay:** a unique-lesson comparison that keeps accepted origins and proposed destinations
  distinguishable on one canvas and exposes exact before/after values in the inspector.
- **Contextual inspector:** the fixed-width collapsible desktop region that explains the current selection or
  mode-specific context and contains applicable actions.
- **Utility disclosure:** the compact header control containing secondary, non-destructive workbench utilities such as
  accepted export and available run diagnostics.
- **Direct effect:** an accepted lesson implicated by administrator repair intent.
- **Ripple effect:** a kernel-classified change needed for feasibility but not directly caused by the staged intent.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Inspect the accepted timetable in the persistent workbench | School timetable administrator | None |
| UC-2 | Prepare a repair without leaving the timetable canvas | School timetable administrator | Requires UC-1; extends UC-1 at 5a |
| UC-3 | Follow repair generation without losing timetable context | School timetable administrator | Requires UC-2 |
| UC-4 | Review and decide a proposal on the combined timetable canvas | School timetable administrator | Requires UC-3 |
| UC-5 | Complete a repair while retaining operational context (primary) | School timetable administrator | Requires UC-1, UC-2, UC-3, and UC-4; Includes UC-1 at step 1, UC-2 at step 2, UC-3 at step 3, and UC-4 at step 4 |

## State models

### Authoritative repair lifecycle

This feature does not add or reinterpret a lifecycle transition.

```text
Accepted baseline
  +-- start repair --------------------------------------------> Repair draft

Repair draft
  +-- edit, pin, undo, recover --------------------------------> Repair draft
  +-- request repair ------------------------------------------> Solving repair
  +-- discard draft -------------------------------------------> Accepted baseline

Solving repair
  +-- complete verified FEASIBLE result -----------------------> Repair proposal
  +-- cancel, fail, reject output, or stop --------------------> Repair draft

Repair proposal
  +-- accept durably ------------------------------------------> Accepted baseline
  +-- revise or discard proposal ------------------------------> Repair draft
  +-- fail durable acceptance ---------------------------------> Repair proposal
```

The accepted baseline remains current in Repair draft, Solving repair, and Repair proposal. Every lifecycle transition
not shown above is refused without changing the accepted bundle, draft, run, or proposal.

### Available presentation modes

The table contains exactly the repair-lifecycle rows governed by this feature—no more and no fewer.

| Authoritative lifecycle state | Available modes | Default mode on entry or reload |
|---|---|---|
| Accepted baseline | Current | Current |
| Repair draft | Current, Draft | Draft |
| Solving repair | Current, Draft, Solving | Solving |
| Repair proposal | Current, Draft, Proposal | Proposal |

Selecting any available mode changes presentation only. An unavailable mode is not selectable. If a lifecycle
transition removes the selected mode, the workbench selects the new lifecycle state's default mode. Reload selects the
default mode from this table rather than persisting a prior mode.

### Workbench presentation state

| Dimension | Values | Transition and retention behavior |
|---|---|---|
| Time range | Week or one definition-declared Day | Existing per-school range/weekday preference remains authoritative; mode changes retain it |
| Investigation | Subject, teacher, search, and explicit filter state | Retained across modes while identities remain available; reset on reload under the accepted inspection contract |
| Lesson selection | None or one stable accepted lesson identity | Retained across modes when that lesson remains representable; otherwise cleared with an explanation |
| Focused context | Whole school or one class/teacher/room schedule | Retained across modes when the entity exists; return restores the prior whole-school context |
| Inspector | Open or collapsed | Open initially; explicit contextual selection opens it; user collapse is retained only for the current page session |
| Utility disclosure | Closed or open | Closed initially and after reload; opening or closing changes no workbench data |

Mode changes preserve representable Day/Week, weekday, investigation, lesson selection, focused entity, and scroll
context. If the selected context cannot be represented in the destination mode, the workbench clears only that context,
announces why, and preserves the remaining presentation state. All other presentation transitions are refused or
ignored without changing authoritative workspace data.

### Combined proposal overlay

- An unchanged lesson has one accepted placement and remains visually quiet.
- A period move keeps an accepted-origin representation in the old period and adds a proposed-destination
  representation in the new period. Both map to one stable lesson identity and one unique changed-lesson count.
- A teacher or room change that remains in the same class-period slot uses one combined changed representation. Its
  accepted and proposed values are both available in the inspector; it is not duplicated merely to show a non-period
  change.
- An addition has only a proposed representation and an explicit addition cue.
- A cancellation has only an accepted representation and an explicit cancellation cue.
- A lesson that is both a direct effect and a kernel-classified change carries both explanations without becoming two
  unique lessons.
- Selecting any representation selects the stable lesson identity and opens exact accepted/proposed details. Missing
  display metadata is shown using the stable ID plus an explicit unavailable-name cue; it is never inferred.
- Proposal investigation and explicit filters include a changed lesson when either its accepted or proposed side
  matches. Cues and the inspector identify which side matched; all totals count unique lesson identities rather than
  rendered representations.
- In Current, Draft, and Solving modes, teacher availability and investigation describe the accepted definition and
  assignments. In Proposal mode, proposal-side availability is derived from the proposal definition and is explicitly
  labelled proposed; accepted assignments remain distinguishable in the combined comparison.

## UC-1 - Inspect the accepted timetable in the persistent workbench

- Goal: Understand the complete accepted timetable and retain operational context in the polished workbench.
- Primary actor: School timetable administrator
- Supporting actors: none
- Trigger: The administrator opens a workspace with a verified accepted baseline or selects Current from another
  available mode.
- Preconditions: A verified accepted definition/result pair exists for the main scenario; extension 1a covers a
  purported accepted pair rejected before that precondition can be established.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator opens the operational workspace on a supported desktop.
2. The workbench identifies the school, authoritative lifecycle state, accepted revision, available modes, selected
   Current mode, and represented Week or Day while presenting the whole-school timetable as the primary surface.
3. The administrator changes Week or Day, investigates a subject or teacher, searches, applies an explicit filter, or
   navigates to a class, teacher, or room schedule.
4. The workbench applies the accepted inspection semantics, labels complete versus narrowed population honestly, and
   retains a return path to the prior whole-school context.
5. The administrator selects an accepted lesson and may continue with UC-2 at extension point 5a.
6. The workbench opens the contextual inspector and shows exact accepted subject, class, teacher, weekday, period,
   room, lesson identity, and current-baseline status without displacing the timetable.
7. The administrator collapses and reopens the inspector or opens the Utilities disclosure.
8. The workbench preserves selection while collapsed, expands the timetable area, restores the same inspector context
   when reopened, and exposes accepted export and applicable diagnostics without placing destructive lifecycle actions
   in Utilities.

### Extensions

- 1a. If an administrator attempts to import a purported accepted definition/result pair with a missing or blank
  display name or an unmappable assignment reference, verification refuses the pair before acceptance, reports the
  invalid input without presenting a Current lesson or inspector, and leaves authoritative workspace state unchanged;
  end.
- 2a. If the accepted timetable has no assignments, the workbench still renders every declared class, weekday, period,
  and empty position, identifies Current as accepted, and invents no lesson; resume at step 3.
- 2b. If the viewport is narrow, the workbench presents the existing read-only Day or focused agenda, identifies the
  authoritative lifecycle state, withholds all mutation and acceptance actions, and makes no complete-desktop claim;
  end.
- 3a. If a requested presentation identity or stored range preference is missing or invalid, the workbench applies the
  accepted inspection fallback, explains any cleared context, and changes no authoritative data; resume at step 3.
- 4a. If a focused class, teacher, or room has no accepted lesson, the workbench identifies the entity and its empty
  recurring schedule without inventing an assignment; resume at step 4.
- 5a. If the administrator chooses to stage a supported repair from the selected or whole-school context, continue
  with UC-2; end.
- 8a. If accepted export fails, the workbench reports that no bundle was produced, preserves Current and the retained
  canvas context, and changes no accepted, draft, run, or proposal data; end.

### Guarantees

- G1. The header and toolbar remain outside the timetable's own scroll region; the class-name context and time headers
  remain available under the accepted inspection contract while the timetable scrolls.
- G2. Export and applicable diagnostics are secondary utilities. Import is not shown after an accepted baseline exists,
  and draft discard, run cancellation, proposal revision/discard, and proposal acceptance never appear in Utilities.
- G3. The open inspector has one stable width, cannot be resized, and does not create a second timetable model.
  Collapsing or reopening it persists no value and causes no workspace request.
- G4. Range, investigation, narrowing, selection, focused schedules, inspector state, utilities, and presentation-mode
  navigation never mutate accepted, draft, run, proposal, or policy state.
- G5. State, selection, highlights, narrowing, and inspector context remain distinguishable without color alone. All
  scenario actions have visible pointing-device controls and retain native keyboard operation; no custom matrix
  keyboard-navigation guarantee is introduced.
- G6. The feature adds no role, identity, login, logout, remote resource, publication, or access path. Existing local
  workspace access and disclosure boundaries remain unchanged.
- G7. Real-browser verification uses the normative validation snapshot, exercises Current, Week/Day, focused return,
  inspector collapse/reopen, Utilities, narrow read-only behavior, and exact durable state before and after.

### Postconditions

- Success: The administrator can inspect an exact accepted lesson and whole-school context in the persistent workbench,
  use focused drill-downs, and return without losing representable operational context.
- Minimal guarantee: Failure to represent a local preference, selection, utility outcome, or focused entity invents no
  lesson, discloses no unauthorized data, and changes no authoritative workspace state.

## UC-2 - Prepare a repair without leaving the timetable canvas

- Goal: Stage supported repair intent and protection while keeping the accepted whole-school timetable in context.
- Primary actor: School timetable administrator
- Supporting actors: Local durable storage
- Trigger: From Current, the administrator starts a supported repair for one teacher or room and selected weekly
  periods.
- Preconditions: UC-1's accepted workbench exists, no repair run is active, and the existing workspace lifecycle permits
  a repair draft.
- Relations:
  - Requires: UC-1, because the repair overlays and inspector consume its accepted canvas and presentation state
  - Includes: none
  - Extends: UC-1 at extension point 5a

### Main success scenario

1. The administrator describes recurring teacher or room unavailability for explicitly named periods from the current
   timetable context.
2. The workspace durably stages the intent, enters Repair draft, makes Current and Draft available, selects Draft, and
   retains the representable range, investigation, selection, focused entity, and scroll context.
3. The same timetable canvas marks directly affected lessons and blocking conflicts while stating that the accepted
   timetable remains current.
4. The administrator selects an affected lesson, inspects its accepted assignment and staged rule, and protects its
   accepted period, room, or both; the administrator may also preview and confirm an existing supported bulk pin.
5. The inspector distinguishes direct effect, blocking conflict, attempt-scoped pin, persistent policy lock, and
   unpinned dimensions and shows the draft's exact intent, effects, pins, and readiness.
6. The administrator switches to Current and back to Draft or opens a focused schedule.
7. The workbench changes presentation only, restores the retained Draft overlay and inspector context, and leaves the
   draft ready for repair generation when no conflict remains.

### Extensions

- 2a. If durable draft save fails, the workbench reports that the latest intent is not recoverable, prevents solving
  from that unpersisted revision, keeps the prior accepted baseline current, and resumes at step 1.
- 3a. If no accepted lesson is directly affected, Draft states that the rule currently conflicts with no accepted
  assignment and retains the staged intent; continue at step 4.
- 4a. If a proposed pin contradicts staged unavailability, an existing lock, or another instruction, the workbench
  marks the exact blocking conflict, chooses no winner, and continues at step 5.
- 4b. If the administrator cancels a bulk preview, no lesson in that preview is pinned and Draft returns to the prior
  inspector context; resume at step 4.
- 5a. If a blocking conflict remains, repair generation is unavailable, the conflict stays navigable on the canvas,
  and the administrator resumes at step 4.
- 6a. If the selected lesson or focused entity is not representable after switching modes, the workbench clears only
  that context, explains why, and preserves the range and remaining investigation state; resume at step 6.
- 7a. If the administrator explicitly discards the draft, the workspace confirms the destructive action in Draft,
  removes staged intent and attempt-scoped pins, returns to Accepted baseline and Current, and ends.
- 7b. If the application reloads, the workspace restores the durable Repair draft, selects Draft by default, restores
  only presentation preferences allowed by the accepted inspection contract, and changes no accepted data; resume at
  step 3.

### Guarantees

- G1. Draft is an overlay on the accepted timetable, never a claim that accepted assignments already changed or became
  invalid.
- G2. Current and Draft consume one accepted lesson identity model. Switching modes cannot duplicate or reinterpret
  accepted lessons, draft intent, direct-effect membership, conflicts, or pins.
- G3. Draft conflict, direct effect, policy lock, period pin, room pin, combined pin, selection, and investigation cues
  use text, icon, outline, or pattern distinctions in addition to color.
- G4. Inspector collapse, mode switching, range changes, investigation, search, filters, and focused schedules do not
  save, discard, or alter repair intent. Only explicit existing draft actions may do so.
- G5. Staging, pinning, undo, discard, reload, focused navigation, and presentation changes never mutate the accepted
  definition/result pair.
- G6. Narrow screens expose Draft status and read-only accepted context but no intent, pin, discard, or solve action.
- G7. Existing local access, safe-display, lifecycle, version, and refusal behavior remain unchanged. A presentation
  action never bypasses an existing stale-state or persistence check.
- G8. Real-browser verification covers individual and bulk protection, no-effect intent, blocking conflict, Current ↔
  Draft switching, focused return, inspector collapse/reopen, reload, discard, narrow behavior, and exact accepted
  state before and after.

### Postconditions

- Success: One durable, conflict-free repair draft is ready to solve and remains explainable on the same whole-school
  timetable whose accepted baseline is still current.
- Minimal guarantee: The accepted baseline remains byte-for-byte unchanged, and no conflicting or unpersisted draft is
  eligible for repair generation.

## UC-3 - Follow repair generation without losing timetable context

- Goal: Monitor or cancel repair generation while continuing to understand the accepted timetable and frozen intent.
- Primary actor: School timetable administrator
- Supporting actors: School Kernel and local durable storage
- Trigger: The administrator requests repair generation from the ready Draft context.
- Preconditions: UC-2 produced a durable repair draft with no blocking conflict and no repair run is active.
- Relations:
  - Requires: UC-2, because the run consumes its exact durable intent and persistent canvas context
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests repair generation.
2. The workspace enters Solving repair, makes Current, Draft, and Solving available, selects Solving, freezes draft
   mutation, keeps the accepted timetable responsive, and presents run state and cancellation in the inspector.
3. The administrator inspects accepted lessons, changes the represented range, or switches among Current, frozen Draft,
   and Solving while the run continues.
4. The workbench retains representable presentation context, keeps the accepted baseline labelled current, and never
   describes an in-progress or time-limited result as accepted, optimal, or globally minimal.
5. School Kernel returns a complete feasible outcome for the exact frozen draft.
6. The workspace independently verifies it, enters Repair proposal, removes Solving, makes Current, Draft, and Proposal
   available, selects Proposal, and retains the representable canvas context for UC-4.

### Extensions

- 2a. If the administrator cancels, the workspace terminates the run, returns to Repair draft and Draft mode, restores
  mutable draft controls, creates no proposal, and preserves the accepted baseline; end.
- 2b. If the application stops during solving, reload restores Repair draft and Draft mode with no eligible proposal;
  end.
- 3a. If a selected context is not representable in another available mode, the workbench clears only that context,
  explains why, and leaves the run and remaining presentation state unchanged; resume at step 3.
- 5a. If no feasible repair is found, the run fails, or safe validation diagnostics exist, the workspace returns to
  Repair draft and Draft mode, opens the applicable diagnostic context in the inspector and Utilities, creates no
  proposal, and ends.
- 5b. If the administrator requests an allowed retry without changing intent, the workspace starts a new Solving
  repair from step 2 and never exposes the prior unsuccessful outcome as a proposal.
- 6a. If the returned candidate is incomplete, non-feasible, stale, mismatched, or fails independent verification, the
  workspace rejects it, returns to Repair draft and Draft mode with safe diagnostics, exposes no candidate assignments
  on the canvas, and ends.

### Guarantees

- G1. Draft mutation, destructive draft actions, and proposal actions are unavailable during Solving. Cancellation is
  the only lifecycle-changing administrator action presented for the run.
- G2. Current, frozen Draft, and Solving use the same accepted timetable identity and retained presentation context;
  switching modes neither cancels nor restarts the run.
- G3. Only a complete independently verified feasible result makes Proposal mode available. Failed or rejected output
  never appears as a candidate placement or review overlay.
- G4. The accepted bundle and durable repair draft remain unchanged through mode switching, inspection, cancellation,
  failure, rejected output, and feasible proposal creation.
- G5. Run limit, status, cancellation, safe outcome, and available diagnostics remain distinguishable without color and
  operable by visible native controls.
- G6. Narrow screens identify Solving and show read-only accepted context but expose no cancellation or other mutation
  action.
- G7. Existing local access, kernel invocation, cancellation, timeout, recovery, identity, and safe-diagnostic
  boundaries remain unchanged.
- G8. Real-browser verification covers responsive accepted inspection during a run, all available mode changes,
  cancellation, restart recovery, unsuccessful and rejected outcomes, successful transition to Proposal, diagnostics,
  narrow behavior, and exact accepted/draft state.

### Postconditions

- Success: One verified repair proposal is available in Proposal mode on the retained canvas, while the prior accepted
  baseline remains current and the originating draft remains identifiable.
- Minimal guarantee: Cancellation, restart, failure, or rejected output returns to the unchanged durable draft, changes
  no accepted data, and makes no proposal presentation available.

## UC-4 - Review and decide a proposal on the combined timetable canvas

- Goal: Account for proposal impact in whole-school context and deliberately accept, revise, or discard it.
- Primary actor: School timetable administrator
- Supporting actors: Local durable storage
- Trigger: The workspace enters Repair proposal or the administrator selects Proposal while it is available.
- Preconditions: UC-3 produced one independently verified feasible proposal whose identity still matches the accepted
  baseline and retained repair draft.
- Relations:
  - Requires: UC-3, because the combined overlay consumes its verified proposal, accepted baseline, and retained draft
  - Includes: none
  - Extends: none

### Main success scenario

1. Proposal mode presents the accepted timetable and proposed placements using the exact [Combined proposal
   overlay](#combined-proposal-overlay), labels the proposal not current, and keeps Current and Draft directly
   selectable.
2. The inspector shows the unique changed-lesson total, authoritative change-category totals, direct and ripple
   effects, protected assignments, groupings, run evidence, and an explicit statement that acceptance advances
   Current.
3. The administrator selects each relevant origin, destination, combined changed tile, addition, cancellation, or
   unchanged lesson and may change Day/Week, investigation, filters, or focused context.
4. The inspector preserves exact accepted and proposed subject, class, teacher, weekday, period, room, lesson identity,
   changed dimensions, effect explanation, and protection state, while the canvas retains representable context.
5. The administrator switches among Current, Draft, and Proposal to confirm which timetable is accepted and then
   explicitly accepts the proposal.
6. The workspace revalidates proposal identity, durably advances the complete accepted bundle, removes draft and
   proposal state, makes only Current available, and renders the new accepted timetable on the retained representable
   range.

### Extensions

- 1a. If a changed lesson moves period, the canvas presents accepted origin and proposed destination as two labelled
  representations of one lesson identity and one unique count; resume at step 2.
- 1b. If a changed lesson remains in the same class-period slot, the canvas presents one combined changed tile and the
  inspector exposes both sides without duplicating the lesson; resume at step 2.
- 1c. If the proposal adds or cancels a lesson, the canvas presents only the existing proposed or accepted side with an
  explicit addition or cancellation cue; resume at step 2.
- 2a. If a category or grouping is empty, the inspector shows zero and retains its meaning rather than hiding it;
  continue at step 3.
- 3a. If a subject, teacher, search, or explicit filter matches only one comparison side, the workbench retains the
  unique lesson, identifies the matching side, and does not hide the other side needed to understand the change;
  resume at step 3.
- 3b. If an unchanged lesson is selected, the inspector identifies it as accepted and unchanged and does not add it to
  any change total; resume at step 3.
- 4a. If display metadata is unavailable on either side, the inspector shows the corresponding stable identity and an
  unavailable-name cue without guessing or merging distinct values; resume at step 4.
- 5a. If the administrator revises intent, the workspace explicitly removes proposal eligibility, returns to Repair
  draft and Draft mode, preserves the accepted baseline, and continues with UC-2.
- 5b. If the administrator discards the proposal, the workspace removes only the proposal, returns to Repair draft and
  Draft mode, keeps the accepted baseline and draft unchanged, and ends.
- 6a. If proposal or accepted identity is stale or mismatched, the workspace refuses acceptance, changes no accepted
  data, invalidates the proposal under the existing lifecycle contract, returns to Repair draft and Draft mode, and
  ends.
- 6b. If durable acceptance fails, the workspace reports that Current did not advance, preserves the prior accepted
  bundle byte-for-byte, keeps the unchanged proposal reviewable in Proposal mode for an explicit retry, and ends.
- 6c. If the viewport is narrow, the workbench identifies Repair proposal and provides a read-only accepted/proposed
  agenda without revise, discard, or accept actions and without claiming complete desktop review; end.

### Guarantees

- G1. Every rendered accepted origin, proposed destination, or combined tile maps to one stable lesson identity and the
  exact accepted/proposed assignment side. Rendered element count never becomes lesson or change count.
- G2. Unique changed-lesson, category, direct-effect, ripple-effect, protected, and grouping totals retain their
  authoritative meanings. Overlapping explanatory membership never duplicates the unique total or implies that
  non-disjoint group totals can be summed.
- G3. Current, Draft, and Proposal mode navigation changes no lifecycle state, accepted data, draft, proposal identity,
  comparison totals, or acceptance eligibility.
- G4. Accepted origin, proposed destination, same-slot change, addition, cancellation, direct effect, ripple effect,
  protection, selection, and unchanged state are distinguishable through text, icon, outline, position, or pattern in
  addition to color.
- G5. The inspector retains exact lesson, subject, class, teacher, period, room, changed-dimension, and effect identity.
  It never substitutes display text for stable identity or infers absent metadata.
- G6. Proposal review, investigation, filters, focused navigation, inspector collapse, Utilities, and mode switching
  never mutate authoritative state. Only existing explicit revise, discard, and confirmed acceptance actions may do so.
- G7. Only explicit confirmed and durably successful acceptance advances Current. Current never points to a partially
  stored or merely feasible proposal.
- G8. Existing local access, proposal identity, stale-state refusal, atomic acceptance, rollback, and recovery behavior
  remain unchanged.
- G9. Real-browser verification covers every combined-overlay shape, every change category, direct/ripple overlap,
  protected and unchanged lessons, comparison-side filtering, all modes, focused return, inspector behavior, revise,
  discard, stale refusal, failed acceptance, successful acceptance, narrow behavior, and exact durable state.

### Postconditions

- Success: The exact proposal bundle is the new accepted baseline, only Current remains available, and the administrator
  can inspect the new accepted timetable in the retained representable context.
- Minimal guarantee: Review, presentation changes, revise, discard, stale identity, or persistence failure never
  partially advances or mislabels Current and preserves the prior accepted baseline exactly.

## UC-5 - Complete a repair while retaining operational context (primary)

- Goal: Keep the weekly timetable operational through a complete repair without losing whole-school context or
  confusing accepted, draft, running, and proposed state.
- Primary actor: School timetable administrator
- Supporting actors: School Kernel and local durable storage
- Trigger: An operational disruption requires a supported teacher- or room-unavailability repair.
- Preconditions: A verified accepted baseline exists in the polished workbench.
- Relations:
  - Requires: UC-1, UC-2, UC-3, and UC-4 through the included goals
  - Includes: UC-1 at step 1, UC-2 at step 2, UC-3 at step 3, and UC-4 at step 4
  - Extends: none

### Main success scenario

1. The administrator uses UC-1 to inspect Current, locate the affected whole-school context, and select a relevant
   lesson or resource.
2. The administrator uses UC-2 to stage the supported disruption, understand direct effects, protect accepted
   dimensions, and leave a durable conflict-free Draft.
3. The administrator uses UC-3 to generate a verified proposal while retaining access to Current, frozen Draft, run
   status, and the same representable timetable context.
4. The administrator uses UC-4 to account for direct and ripple effects on the combined canvas, confirm which baseline
   is current, and explicitly accept the proposal.
5. The workbench shows the newly accepted timetable in Current with no residual Draft, Solving, or Proposal mode and
   makes it available as the baseline for another repair.

### Extensions

- 2a. If Draft cannot become conflict-free, the workbench preserves Current, retains navigable conflict explanations,
  starts no run, and ends.
- 3a. If the run is cancelled, fails, finds no feasible result, or returns rejected output, the workbench returns to the
  unchanged Draft with Current still accepted and ends.
- 4a. If the administrator revises or discards the proposal, the workbench returns to the retained Draft with Current
  unchanged and ends.
- 4b. If acceptance is stale or persistence fails, the workbench applies UC-4's minimal guarantee, identifies the prior
  accepted timetable as Current, and ends.
- 5a. If the administrator begins a second repair, the workbench starts from the newly accepted baseline, creates a new
  Draft with no attempt-scoped pins from the completed repair, and continues with UC-2.

### Guarantees

- G1. The same stable accepted lesson identities, authoritative display metadata, and definition-ordered time model
  support Current, Draft, Solving, Proposal, focused schedules, inspector details, and return navigation.
- G2. At every decision point the header and active mode identify the authoritative lifecycle state and whether the
  visible timetable is Current, Draft context, a running accepted view, or a non-current Proposal comparison.
- G3. Presentation continuity never broadens mutation authority. Existing lifecycle, version, kernel, persistence,
  cancellation, recovery, and acceptance boundaries govern every consequential action.
- G4. Failure at any step preserves the last durably accepted baseline and never presents draft intent, a running
  candidate, failed output, or an unaccepted proposal as Current.
- G5. The whole-school timetable remains primary. Focused class, teacher, and room schedules, the inspector, utilities,
  and diagnostics support it and never satisfy the complete-school outcome by themselves.
- G6. The complete journey is exercised in a real browser against isolated durable storage and the packaged scheduling
  boundary using the normative validation data, with exact accepted/draft/proposal state comparisons at each step.

### Postconditions

- Success: A deliberately accepted repair is Current, its whole-school impact was reviewable without leaving the
  persistent workbench, and a later repair can start from that exact baseline.
- Minimal guarantee: The last durably accepted baseline remains current and identifiable when staging, solving,
  review, navigation, or acceptance cannot complete.

## Normative validation data

### Validation snapshot

The complete generated or properly anonymized accepted definition/result pair used for this feature has:

- at least 60 definition-declared classes;
- at least 100 definition-declared teachers;
- at least 100 definition-declared rooms;
- between 900 and 1,100 accepted lesson assignments across the complete recurring week;
- every definition-declared weekday and period position needed to render honest empty structure;
- long school-controlled subject, teacher, class, and room names plus opaque identities;
- teachers with assigned, available-but-unassigned, and unavailable periods;
- populated and empty class, teacher, and room focused schedules;
- one supported teacher-unavailability draft and one supported room-unavailability draft;
- direct effects, at least one blocking pin conflict, individual and bulk period/room protection, and a conflict-free
  revision;
- one verified feasible proposal containing a period move, a room-only move, a direct-effect change, a ripple-effect
  change, a protected unchanged lesson, and at least one empty change category;
- isolated unsuccessful, cancelled, rejected-output, stale-acceptance, and failed-persistence outcomes.

Additional ordinary classes, resources, empty positions, accepted lessons, and changes are allowed within the stated
cardinality range. Unsupported group metadata, primary-room policies, or new repair instruction types are neither
required nor inferred.

### Administrator feature gate

Every participant performs exactly these rows—no more and no fewer for this feature gate:

| Task | Observable completion |
|---|---|
| Orient in Current | Identifies the school, accepted revision, complete Week or precise Day, and an exact accepted lesson without opening a separate report page |
| Prepare Draft | Stages the supplied disruption, explains a direct effect and blocking conflict, applies the required protection, and states that accepted data is unchanged |
| Retain context while solving | Starts repair, inspects Current and frozen Draft during the run, identifies cancellation, and never calls the running outcome current or optimal |
| Explain Proposal | Uses the combined overlay and inspector to distinguish accepted origin, proposed destination or same-slot change, direct effect, ripple effect, and protection |
| Navigate without confusion | Uses Current, Draft, Proposal, a focused schedule, inspector collapse/reopen, and return navigation while retaining or correctly clearing context |
| Decide and verify Current | Explicitly accepts the supplied proposal and identifies the newly accepted timetable as Current with no residual Draft or Proposal |

The gate requires:

1. five participants with real timetable responsibility from at least three schools;
2. at least four of five participants completing every row without serious error or facilitator correction;
3. all five participants identifying the accepted Current timetable correctly at every decision point;
4. per-participant task results, serious errors, corrections, school distribution, and Current-state answers recorded;
5. real-browser evidence at operational desktop widths and the narrow read-only boundary; and
6. exact durable-state evidence before and after inspection, failure, discard, and acceptance paths.

An unstructured `PASS`, `LGTM`, or `looks good` response is not participant evidence for this gate. Interaction timing
may be recorded diagnostically but is not an approval threshold.

## Out of scope

- initial import, initial planning, and initial-proposal presentation redesign;
- cohort partitions, group targets, split-slot rendering, or inferred groups;
- primary-room policies, policy exceptions, policy provenance editing, or soft room preferences;
- new repair instruction types, substitute-teacher workflows, or changed solver semantics;
- direct drag-and-drop mutation of accepted assignments;
- a new route, response field, database structure, lifecycle state, workspace field, or accepted-bundle format;
- accounts, roles, login, logout, multi-school hosting, publication, notifications, attendance, or substitution;
- a presentation density switch, custom application zoom, or persisted inspector/mode state;
- a resizable inspector, second utility drawer, side-by-side complete proposal canvases, or accepted/proposed toggle;
- mobile or narrow-screen repair editing, cancellation, proposal revision/discard, or acceptance;
- custom matrix keyboard navigation, application shortcuts, or a keyboard-only participant gate;
- a frontend framework, build pipeline, remote asset, offline cache, or new runtime dependency.

## External dependencies

- Implementation and convergence depend on `timetable-inspection-ux` UC-2 returning to `APPROVED` after the complete
  shared browser regression passes and its status accurately reports remaining blockers.
- Automated durable-state validation requires isolated durable storage, the production scheduling-process boundary,
  and a real browser. An unavailable environment is reported as a limit rather than replaced by a weaker pass.
- Administrator approval depends on the five timetable professionals from at least three schools and the recorded
  evidence defined under [Administrator feature gate](#administrator-feature-gate). Until it exists, automated
  convergence cannot be promoted as administrator approval.
- Validation data must be generated or properly anonymized with known provenance. Production school data must not be
  fabricated or used without appropriate authority.
