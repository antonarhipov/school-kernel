# School Timetable Operations Workspace

## Feature summary

The School Timetable Operations Workspace is a local, single-administrator product for maintaining one school's
recurring weekly timetable. It places the complete school in view, lets the administrator record a teacher or room
unavailability and hard protections, obtains a draft repair from School Kernel, explains every resulting change, and
advances the accepted timetable only after explicit confirmation.

The workspace is a client of School Kernel. The kernel remains authoritative for definition and result validity,
lineage, hard feasibility, stability ordering, revisions, diagnostics, and change classification. The workspace owns
the administrator's local draft, proposal lifecycle, accepted-bundle persistence, and presentation of those kernel
outcomes.

## Scope and resolved decisions

### In scope

- One local school workspace operated by one timetable administrator, without accounts or concurrent editing.
- Import of either an initial school definition or a verified definition/result baseline bundle.
- Initial planning as a proposal that must be accepted before it becomes the first accepted baseline.
- A desktop whole-school day matrix, with secondary class, teacher, and room schedules for focused inspection.
- Recurring weekly teacher and room unavailability for selected declared periods.
- Individual and previewed bulk pins for an accepted lesson's period, room, or both.
- Background repair generation through the School Kernel public contract, with cancellation and one longer retry preset.
- Complete proposal review, explicit acceptance or discard, and a subsequent repair from an accepted result.
- Recovery of draft changes and attempt-scoped pins after restart.
- Export and verified re-import of the accepted baseline bundle.

### Resolved behavioral boundaries

- Every day mentioned by this feature is a weekday in the recurring weekly definition. The workspace uses the word
  `weekly` when unavailability is entered and shows the affected weekday explicitly.
- The first validation increment supports only teacher unavailability, room unavailability, and period/room pins.
  Class unavailability, cancellation, required moves, and teacher reassignment remain planned follow-on behavior.
- The solver never invents a substitute teacher. The initial increment offers no substitute-teacher action.
- A pin preserves an accepted value as a hard instruction. A required value would force a different value, but required
  moves are not offered in this increment. An unpinned dimension remains solver-controlled.
- There is no user-selected soft-protection tier. The only protection choices are a hard pin or ordinary kernel
  stability ordering.
- The desktop workflow is the validation target. A narrow screen may show read-only focused schedules, but it does not
  offer editing, proposal acceptance, or a claim of complete whole-school operation.
- An accepted baseline is the inseparable exact school definition and matching verified `FEASIBLE` result. A result by
  itself never establishes accepted state.
- The workspace has at most one working draft and one generated proposal. Starting another solve or changing the draft
  removes the prior proposal's eligibility immediately.
- Import is available only when the workspace has no accepted baseline. An existing local baseline is reopened rather
  than silently replaced; switching or resetting schools is outside this slice.
- If the application stops while solving, restart restores the auto-saved draft and no proposal. An unaccepted
  proposal is not made eligible merely by recovery.
- If durable acceptance fails, the old accepted bundle remains current and the unchanged proposal remains reviewable
  for an explicit retry. This lets the administrator recover without treating a failed write as either acceptance or
  discard.
- The local data surface is reachable only on the same machine, makes no outbound data requests, and never requires the
  administrator to prepare JSON or invoke a command during the supported journey.

### Validation boundary

Runtime correctness is exercised with isolated workspace state and the packaged School Kernel boundary. Safety checks
must cover rejected import, invalid input, unsuccessful search, cancellation, restart, discarded and stale proposals,
and failed acceptance persistence while proving that an existing accepted bundle remains byte-for-byte unchanged.

Interaction validation uses a complete synthetic or properly anonymized school of approximately 1,000 lessons, 100
teachers, 60 classes, 100 rooms, and 60 weekly periods. After initial load on the reference validation machine, search,
filtering, day changes, lesson selection, and pin feedback must complete within 250 ms at the 95th percentile; opening
proposal review must complete within one second. Solver time is recorded separately.

Before the discovery increment is considered successful, at least five people with real timetable responsibility from
at least three schools perform the recurring teacher-unavailability journey, the room-unavailability journey, and a
second repair from an accepted first result. At least four of five complete the complete journey without JSON,
command-line use, facilitator correction, missed or invented direct effects, or misunderstood move totals. All five
must identify the currently accepted timetable correctly at every decision point. A soft-protection tier is considered
only if at least two participants independently need it for a real scenario or a core task fails specifically because
hard pin versus ordinary movement is insufficient.

## Actors and domain terms

### Actors

- **School timetable administrator:** The sole primary actor, responsible for understanding, repairing, and accepting
  the school's operational recurring timetable.
- **School Kernel:** Supporting scheduling system that validates complete inputs and produces authoritative structured
  outcomes.
- **Local durable storage:** Supporting boundary that retains drafts and atomically advances complete accepted bundles.

### Domain terms

- **Class:** Administrator-facing name for the kernel contract's `cohort`.
- **Accepted baseline:** The exact complete school definition and its matching verified `FEASIBLE` result currently in
  force.
- **Initial draft:** A verified initial definition for which no timetable has yet been accepted.
- **Repair draft:** Administrator intent layered over an accepted baseline, consisting of staged weekly resource
  unavailability and attempt-scoped pins.
- **Persistent policy lock:** A period or room lock contained in the accepted definition as school policy. An imported
  lock is persistent when no workspace manifest identifies it as attempt-scoped.
- **Attempt-scoped pin:** A hard period, room, or combined lock copied from an accepted assignment for the current repair
  attempt. It survives revision and retry of that draft but is cleared when a later repair draft starts.
- **Required value:** A hard lock to a value different from the accepted assignment. It is distinct from a pin and is
  not offered in this increment.
- **Directly affected lesson:** Before solving, a lesson whose accepted assignment contradicts staged resource
  availability, or whose lesson definition is explicitly edited. In this increment only the availability condition can
  make a lesson directly affected. Pins alone do not.
- **Proposal:** A complete verified `FEASIBLE` kernel result tied to the exact baseline, successor definition,
  workspace intent, complete kernel result document, and proposed timetable revision that produced it. It is never
  current until accepted.
- **Ripple effect:** A kernel-classified change not directly caused by staged intent but needed to produce a feasible
  timetable under the complete successor definition.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Import school data into an empty workspace | School timetable administrator | None |
| UC-2 | Establish the first accepted timetable | School timetable administrator | Requires UC-1 |
| UC-3 | Understand the accepted whole-school timetable | School timetable administrator | Requires UC-1 |
| UC-4 | Prepare a protected repair draft | School timetable administrator | Requires UC-1 |
| UC-5 | Obtain a feasible repair proposal | School timetable administrator | Requires UC-4 |
| UC-6 | Decide whether a proposal becomes current | School timetable administrator | Requires UC-5 |
| UC-7 | Keep the weekly timetable operational after a disruption | School timetable administrator | Requires UC-1; Includes UC-3 at step 1, UC-4 at step 3, UC-5 at step 4, and UC-6 at step 5 |
| UC-8 | Export the accepted baseline | School timetable administrator | Requires UC-1 |

## State models

### Workspace lifecycle

```text
Empty
  +-- import valid initial definition --------------------------> Initial draft
  +-- import verified accepted bundle --------------------------> Accepted baseline

Initial draft
  +-- request planning -----------------------------------------> Solving initial
  +-- replace draft definition ---------------------------------> Initial draft

Solving initial
  +-- complete FEASIBLE result ---------------------------------> Initial proposal
  +-- cancel, fail, or stop ------------------------------------> Initial draft

Initial proposal
  +-- accept durably -------------------------------------------> Accepted baseline
  +-- revise or discard proposal -------------------------------> Initial draft

Accepted baseline
  +-- start repair ---------------------------------------------> Repair draft
  +-- export ----------------------------------------------------> Accepted baseline

Repair draft
  +-- request repair -------------------------------------------> Solving repair
  +-- edit, undo, or recover -----------------------------------> Repair draft
  +-- discard draft --------------------------------------------> Accepted baseline

Solving repair
  +-- complete FEASIBLE result ---------------------------------> Repair proposal
  +-- cancel, fail, or stop ------------------------------------> Repair draft

Repair proposal
  +-- accept durably -------------------------------------------> Accepted baseline
  +-- revise or discard proposal -------------------------------> Repair draft
```

Starting any solve invalidates an older unaccepted proposal. Editing intent after proposal generation also invalidates
that proposal immediately. Failed durable acceptance leaves the state at the same proposal with the prior accepted
baseline unchanged. Application restart from either solving state returns to its corresponding draft. Restart from a
stable draft or accepted state restores that state; restart never converts a draft or proposal into an accepted
baseline. Every transition not shown above is refused without changing the accepted bundle, draft, or proposal.

### Definition and result lineage

An initial definition contains no `basedOnRevision`. Every repair definition is a complete successor whose
`basedOnRevision` equals the accepted result's `inputRevision`, never its `timetableRevision`. A proposal records the
exact accepted timetable revision or, for initial planning, the explicit absence of an accepted baseline; the complete
definition revision; workspace-intent revision; complete kernel result document; and proposed timetable revision. The
workspace never invents a public whole-result revision, guesses ancestry, rebases a draft silently, or mutates accepted
assignment JSON.

## UC-1 - Import school data into an empty workspace

- Goal: Begin operating one school from either a valid initial definition or a complete verified accepted baseline.
- Primary actor: School timetable administrator
- Supporting actors: Local durable storage
- Trigger: The administrator chooses to import school data into an empty workspace.
- Preconditions: The workspace has no accepted baseline or active draft.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator chooses either an initial definition or a definition together with its matching feasible result,
   optionally carried in an accepted-bundle archive.
2. The workspace identifies the import mode and validates the complete submitted content, supported versions, school
   identity, revisions, assignment integrity, and definition/result lineage.
3. The workspace shows the school by definition-supplied display name and states whether the outcome is an initial
   draft awaiting planning or an already accepted baseline.
4. The workspace durably stores the verified state and opens the corresponding workspace state.

### Extensions

- 1a. If only a result is supplied, the workspace explains that the matching complete definition is required, imports
  nothing, and resumes at step 1.
- 1b. If an archive is supplied, the workspace accepts only the entries under [Accepted-bundle archive](#accepted-bundle-archive);
  an absent, duplicate, extra, unreadable, or unsafe entry causes rejection and resumes at step 1.
- 2a. If parsing, schema or catalog version, school identity, revision, assignment completeness, hard validity, or
  lineage verification fails, the workspace identifies the invalid document and safe reason, creates neither draft nor
  accepted state, discloses no candidate timetable from an unsuccessful result, and resumes at step 1.
- 2b. If a definition-only import contains `basedOnRevision`, the workspace rejects it as not being an initial
  definition, changes no state, and resumes at step 1.
- 2c. If a definition/result import does not contain a complete `FEASIBLE` result matching that exact definition, the
  workspace rejects the pair, creates no accepted baseline, and resumes at step 1.
- 4a. If durable storage fails, the workspace reports that import did not complete, exposes no accepted baseline, and
  resumes at step 1.

### Guarantees

- G1. The two import modes are explicit: definition only creates an initial draft; a verified definition/result pair
  creates an accepted baseline. Import never labels an initial draft or invalid pair as current.
- G2. Imported identifiers are preserved exactly, while user-facing names, weekdays, period order, and optional times
  come from definition metadata rather than interpretation of identifier spelling.
- G3. With no manifest identifying pin provenance, locks in an imported definition are treated as persistent policy.
- G4. Import validation is isolated from existing runtime or demonstration data and accepts no unsupported schema or
  catalog version.
- G5. No rejected or interrupted import persists partial state, changes an existing file as though acceptance occurred,
  or makes school data available beyond the local machine.

### Postconditions

- Success: The workspace contains either one durable initial draft eligible for UC-2 or one verified accepted baseline
  eligible for inspection, repair, and export.
- Minimal guarantee: The workspace remains empty and no candidate timetable is disclosed or accepted when import cannot
  complete.

## UC-2 - Establish the first accepted timetable

- Goal: Turn an imported initial definition into an explicitly accepted first timetable.
- Primary actor: School timetable administrator
- Supporting actors: School Kernel; local durable storage
- Trigger: The administrator requests planning from an initial draft.
- Preconditions: UC-1 produced a durable initial draft and there is no accepted baseline.
- Relations:
  - Requires: UC-1, because a verified initial definition must exist
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator reviews the initial school summary and requests a timetable proposal.
2. The workspace freezes draft replacement for the run, keeps navigation responsive, and asks School Kernel to plan
   the exact complete initial definition with the default 30-second limit.
3. The workspace receives a complete `FEASIBLE` result, verifies its identity and completeness, and labels it as an
   initial proposal rather than an accepted timetable.
4. The workspace shows proposal status, lesson count, termination reason, execution limit, timetable details, and the
   fact that no timetable is accepted yet.
5. The administrator explicitly confirms that the proposal should become the initial accepted baseline.
6. The workspace durably stores the exact definition/result bundle before advancing the current-baseline reference and
   then opens the accepted whole-school timetable.

### Extensions

- 1a. If the administrator chooses to replace the initial definition before planning, the workspace validates the new
  initial definition, atomically replaces the draft only after successful durable storage, and resumes at step 1. If
  validation or storage fails, the prior initial draft remains unchanged and the use case ends.
- 2a. If the administrator cancels, the workspace stops the run, returns to the unchanged initial draft, creates no
  proposal, and ends.
- 2b. If the application stops during planning, restart restores the initial draft with no proposal eligible for
  acceptance; end.
- 3a. If the kernel reports invalid input, no feasible timetable found within the run, interruption, transport failure,
  or internal error, the workspace shows honest safe diagnostics, returns to the unchanged initial draft, creates no
  proposal, and ends.
- 3b. If the kernel returns anything other than a complete verified `FEASIBLE` timetable for the exact definition, the
  workspace treats it as a failed run, discloses no partial timetable, and ends at the initial draft.
- 4a. If the administrator revises or discards the proposal, the workspace removes it, returns to the initial draft,
  and ends.
- 5a. If proposal identity no longer matches the initial draft or local state, acceptance is refused, the proposal is
  invalidated, and the workspace returns to the initial draft; end.
- 6a. If complete durable storage or current-reference advancement fails, the workspace reports that acceptance did not
  complete, retains no accepted baseline, keeps the unchanged proposal reviewable for retry, and ends.

### Guarantees

- G1. Draft replacement, failure, cancellation, and feasible output do not themselves create accepted state; only the
  explicit confirmation at main step 5 can do so.
- G2. A time-limited feasible result is described as feasible, never optimal, best, or globally minimal.
- G3. Failed search is described as `no feasible timetable found within this run`, never as proof that the school is
  impossible to schedule.
- G4. Seed and step controls are not exposed. Proposal details show the actual time limit and termination reason.
- G5. The administrator never has to inspect JSON or invoke School Kernel directly.
- G6. Acceptance is all-or-nothing: a crash or storage failure cannot expose half of an accepted baseline.

### Postconditions

- Success: One exact initial definition/result bundle is the accepted baseline and can be inspected, repaired, exported,
  and used as the parent of a direct successor repair.
- Minimal guarantee: When planning or acceptance cannot complete, no timetable is accepted and the imported initial
  draft remains available for a deliberate retry or replacement.

## UC-3 - Understand the accepted whole-school timetable

- Goal: Find lessons and understand the complete school's accepted recurring timetable without losing whole-school
  context.
- Primary actor: School timetable administrator
- Supporting actors: none
- Trigger: The administrator opens an accepted workspace or investigates a disruption.
- Preconditions: A verified accepted baseline exists, either from UC-1 bundle import or UC-2 acceptance.
- Relations:
  - Requires: UC-1, because imported school metadata and accepted state must exist
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator selects a weekday in the recurring week.
2. The workspace shows every class as a row and the selected day's definition-ordered periods as columns, including
   visible empty cells and occupied cells led by subject with teacher and room.
3. The administrator searches, filters, adjusts the matrix density or zoom, changes day or period focus, or selects a
   lesson while the complete-school context and current accepted-state label remain visible.
4. The workspace highlights matches, summarizes every active filter, offers one-action reset, and shows the selected
   lesson's accepted assignment without leaving the matrix.
5. The administrator optionally opens the dedicated class, teacher, or room schedule and returns to the retained
   whole-school context.

### Extensions

- 2a. If the accepted school contains no lessons, the workspace still shows the declared classes, weekdays, periods,
  and empty state rather than inventing assignments; continue at step 3.
- 3a. If filters narrow the population, the workspace labels the matrix as filtered and never presents it as the
  complete school; resume at step 3.
- 3b. If no lesson matches search or filters, the workspace shows the active criteria and a reset action while leaving
  the accepted timetable unchanged; resume at step 3.
- 5a. On a narrow screen, the workspace may show a read-only class, teacher, or room schedule but withholds editing and
  acceptance actions and does not claim to provide the desktop operational workflow; end.

### Guarantees

- G1. Definition display names, weekday, period order, and optional times are authoritative. IDs remain visible in
  technical details but are never converted into display meaning by parsing their spelling.
- G2. `Class` is used in administrator-facing text and explicitly corresponds to the kernel's `cohort` term in
  technical details.
- G3. Accepted, draft, pinned, required, directly affected, solver-moved, and failed states use text or icons in
  addition to color wherever they appear.
- G4. Day navigation, search, filters, reset, density or zoom controls, lesson selection, and entry to focused schedules
  are keyboard-operable.
- G5. On the validation-scale fixture, the post-load interactions named in the validation boundary meet the 250 ms
  95th-percentile target on the reference machine.
- G6. Inspection never mutates the accepted definition, assignments, draft, or proposal.

### Postconditions

- Success: The administrator can identify the accepted assignment and school-wide context relevant to a disruption and
  can continue into a repair draft.
- Minimal guarantee: Failed or empty search and presentation changes disclose no invented assignments and leave every
  workspace state unchanged.

## UC-4 - Prepare a protected repair draft

- Goal: Express recurring teacher or room unavailability and protect accepted lesson dimensions that must not move.
- Primary actor: School timetable administrator
- Supporting actors: Local durable storage
- Trigger: The administrator starts a repair from the accepted baseline.
- Preconditions: A verified accepted baseline exists and no solve is running.
- Relations:
  - Requires: UC-1, because an accepted definition/result bundle must exist
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator starts a repair and chooses either a teacher or room plus one or more explicitly named weekly
   periods in which that resource is unavailable.
2. The workspace stages the unavailability separately from accepted data, materializing remaining availability when the
   original resource omitted `availablePeriodIds`, and highlights every accepted lesson directly affected.
3. The administrator inspects accepted assignment, staged intent, persistent policy locks, and pin state for relevant
   lessons in the whole-school or focused view.
4. The administrator pins the accepted period, room, or both for individual lessons or requests a bulk pin for a day,
   class, or all currently unaffected lessons.
5. For a bulk pin, the workspace previews the exact lesson snapshot, selected dimensions, count, and conflicts; the
   administrator confirms that snapshot.
6. The workspace records the confirmed pins, distinguishes them from policy locks and unpinned dimensions, shows any
   blocking conflicts, and auto-saves the draft.
7. The administrator reviews a summary of the weekly changes, direct effects, pins, and conflicts and leaves the draft
   ready to solve.

### Extensions

- 1a. If the administrator selects a class unavailability, cancellation, required move, or teacher reassignment action,
  the workspace explains that it is outside this increment, stages nothing, and resumes at step 1.
- 2a. If no accepted lesson is directly affected, the workspace states that the resource change currently invalidates
  no accepted assignment while retaining the staged change; continue at step 3.
- 4a. If a pin would contradict staged unavailability, a persistent policy lock, or another required value, the
  workspace marks a blocking conflict and never chooses a winner silently; continue at step 6.
- 5a. If the administrator cancels the bulk preview, no lesson in that snapshot is pinned and the workspace resumes at
  step 4.
- 6a. If the administrator undoes a bulk action before solving, exactly that confirmed snapshot is unpinned except for
  persistent policy locks and independently applied pins; resume at step 4.
- 6b. If auto-save fails, the workspace warns that the latest draft is not recoverable, leaves the accepted baseline
  unchanged, prevents solving from an unpersisted intent revision, and resumes at step 3.
- 7a. If any blocking conflict remains, the workspace identifies the conflicting lesson and instructions, refuses to
  start solving, and resumes at step 3.
- 7b. If the administrator discards the repair draft, the workspace asks for explicit confirmation, removes staged
  intent and attempt-scoped pins, returns to the accepted baseline, and ends.
- 7c. If the application restarts, the workspace restores the auto-saved changes and attempt-scoped pins as a repair
  draft without changing the accepted baseline; resume at step 3.

### Guarantees

- G1. A pin copies the accepted value; it never mutates accepted assignment JSON. Staged intent is compiled only into a
  complete successor definition for kernel invocation.
- G2. `All currently unaffected lessons` means the complement of the direct-effect set against the accepted baseline at
  the moment of bulk preview. Later changes do not silently expand the confirmed snapshot.
- G3. Attempt-scoped pins survive revision and retry of this draft but are not carried automatically into the next
  repair after acceptance or draft discard. Persistent policy locks remain persistent.
- G4. Pin, policy lock, directly affected, conflicting, and unpinned states are distinguishable without color alone;
  selection, individual pinning, bulk preview, confirmation, and undo are keyboard-operable.
- G5. Draft edits, pins, undo, discard, and persistence never change the accepted bundle.
- G6. Pin feedback meets the interaction target in the validation boundary on the scale fixture.

### Postconditions

- Success: One durable repair draft contains the exact weekly resource changes, direct-effect set, and non-conflicting
  attempt-scoped pins to compile into a complete successor definition.
- Minimal guarantee: The accepted baseline remains byte-for-byte unchanged, and no conflicting or unpersisted draft can
  be submitted as a repair.

## UC-5 - Obtain a feasible repair proposal

- Goal: Obtain a complete, reviewable repair without replacing the accepted timetable.
- Primary actor: School timetable administrator
- Supporting actors: School Kernel
- Trigger: The administrator requests repair generation from a ready repair draft.
- Preconditions: UC-4 produced a durable repair draft with no blocking conflict, and no other solve is running.
- Relations:
  - Requires: UC-4, because exact repair intent and pins must exist
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests a repair with the default 30-second limit.
2. The workspace invalidates any older unaccepted proposal, freezes draft controls for this run, keeps inspection and
   navigation responsive, and offers a cancel action.
3. The workspace compiles the exact intent revision into a complete successor definition whose `basedOnRevision` is the
   accepted result's `inputRevision`, then submits it with the immutable accepted result to School Kernel.
4. School Kernel returns a complete `FEASIBLE` result and authoritative change report for the exact invocation.
5. The workspace verifies the result, records proposal identity and execution evidence, and presents a repair proposal
   while continuing to label the prior baseline as accepted.

### Extensions

- 2a. If the administrator cancels, the workspace stops the run, returns to the unchanged repair draft, creates no
  proposal, and ends.
- 2b. If the application stops during solving, restart restores the repair draft with no proposal eligible for
  acceptance; end.
- 3a. If the draft or accepted baseline identity changed before submission, the workspace refuses the run, identifies
  the stale state, returns to the repair draft, and ends.
- 4a. If the kernel reports `INVALID_INPUT`, the workspace translates safe validation details into navigable draft
  feedback, returns to the unchanged repair draft, creates no proposal, and ends.
- 4b. If no feasible repair is found within the run, the workspace states exactly that, translates constraint IDs and
  involved entities into plain-language navigable diagnostics, offers one clearly labeled two-minute retry for the
  unchanged intent, and returns to the repair draft with no proposal; end.
- 4c. If the administrator chooses the two-minute retry without changing intent, the workspace starts a new run at
  step 2 with that limit; the unsuccessful result is not retained as a proposal.
- 4d. If the run is interrupted or has a transport or internal failure, the workspace shows a safe outcome, returns to
  the unchanged repair draft, creates no proposal, and ends.
- 5a. If the returned result is incomplete, non-feasible, mismatched, stale, or fails independent verification, the
  workspace rejects it, exposes no candidate assignments as a proposal, returns to the repair draft, and ends.

### Guarantees

- G1. Draft controls cannot change during a run. The administrator cancels before editing.
- G2. Only a complete independently verified `FEASIBLE` result can become a proposal. Invalid, unsuccessful,
  interrupted, failed, or rejected output contains no reviewable candidate timetable.
- G3. The accepted bundle remains immutable through compilation, solving, cancellation, failure, and feasible proposal
  creation.
- G4. Proposal identity contains the exact accepted timetable revision, successor-definition revision,
  workspace-intent revision, complete revalidated kernel result document, and proposed timetable revision.
- G5. The workspace describes kernel priority as period stability before room-only stability before ordinary
  preferences. It never describes a time-limited proposal as optimal or globally minimal.
- G6. Proposal details preserve the actual limit, termination reason, elapsed time, and authoritative kernel change
  categories. Each validation run records that evidence and the change counts.
- G7. Diagnostics are evidence from this run, not proof of impossibility or a guaranteed repair recipe.

### Postconditions

- Success: One complete repair proposal is eligible for review and is tied to the unchanged accepted baseline and exact
  repair intent that produced it.
- Minimal guarantee: The accepted bundle and durable repair draft remain unchanged, and no prior or failed candidate is
  eligible for acceptance.

## UC-6 - Decide whether a proposal becomes current

- Goal: Account for the full impact of a repair and deliberately accept or discard it.
- Primary actor: School timetable administrator
- Supporting actors: Local durable storage
- Trigger: The administrator opens a repair proposal for review.
- Preconditions: UC-5 produced a complete proposal whose identity still matches the accepted baseline and draft.
- Relations:
  - Requires: UC-5, because a verified proposal must exist
  - Includes: none
  - Extends: none

### Main success scenario

1. The workspace separates direct effects of administrator intent from solver ripple effects and shows the exact
   categories under [Proposal change categories](#proposal-change-categories).
2. The administrator reviews the unique changed-lesson total, category totals, and groupings by class, teacher, room,
   and day, with every grouping labeled as old, proposed, or both.
3. The administrator inspects each changed lesson's old and proposed subject, class, teacher, period, and room, with only
   changed dimensions emphasized and links to whole-school and focused contexts.
4. The workspace shows proposal status, unique changed lessons, period moves, room-only moves, forced changes,
   termination details, and an explicit statement that acceptance will advance the current baseline.
5. The administrator explicitly confirms acceptance.
6. The workspace verifies proposal identity again, durably stores the complete successor definition/result bundle, and
   only then advances the current-baseline reference.
7. The workspace removes proposal and draft state, clears attempt-scoped pins, and shows the newly accepted timetable as
   the baseline for a later repair.

### Extensions

- 1a. If a lesson is both directly affected and present in a kernel change category, the workspace shows it in both
  explanatory sections without duplicating it in the unique changed-lesson total; continue at step 2.
- 2a. If a category or grouping is empty, the workspace shows a zero rather than hiding the category's meaning;
  continue at step 3.
- 3a. If the administrator inspects an unchanged lesson, the workspace shows it as accepted and visually quiet without
  adding it to change totals; resume at step 3.
- 5a. If the administrator discards the proposal, the workspace removes only the proposal, keeps the accepted baseline
  and repair draft unchanged, and offers either draft revision or separate draft discard; end.
- 5b. If the administrator chooses to revise intent, the workspace invalidates and removes the proposal immediately,
  returns to the retained repair draft, and continues with UC-4.
- 6a. If accepted baseline, definition, intent, result, or timetable identity is stale or mismatched, the workspace
  refuses acceptance, changes no accepted data, invalidates the proposal, and returns to the repair draft; end.
- 6b. If durable bundle storage or current-reference advancement fails, the workspace reports that acceptance did not
  complete, preserves the prior accepted bundle byte-for-byte, keeps the unchanged proposal reviewable for an explicit
  retry, and ends.

### Guarantees

- G1. The headline total counts unique changed lessons. Kernel category totals retain their contract meanings. Cross-
  group totals are never summed as if class, teacher, room, and day groups were disjoint.
- G2. Before-and-after values preserve exact lesson, subject, class, teacher, period, and room identity. Unmappable or
  missing display metadata is shown with its stable ID and an explicit unavailable-name cue; it is never guessed.
- G3. Additions, cancellations, teacher changes, forced moves, period moves, and room-only moves retain the kernel's
  overlap and non-overlap semantics. Review never claims that they enumerate definition-only changes.
- G4. Review navigation, changed-lesson selection, discard, acceptance confirmation, and return to context are
  keyboard-operable and distinguish state without color alone.
- G5. Opening proposal review meets the one-second interaction target on the validation-scale fixture.
- G6. Only explicit confirmed acceptance can change the current-baseline reference, and that reference never points at
  a partially stored bundle.

### Postconditions

- Success: The exact proposed definition/result bundle is the new accepted baseline, no proposal or repair draft
  remains, and the result can parent a subsequent direct repair.
- Minimal guarantee: Discard, revision, stale identity, or persistence failure leaves the prior accepted bundle
  byte-for-byte unchanged and never labels the proposal as current.

## UC-7 - Keep the weekly timetable operational after a disruption (primary)

- Goal: Safely replace the accepted weekly timetable after a real resource disruption and continue from the result.
- Primary actor: School timetable administrator
- Supporting actors: School Kernel; local durable storage
- Trigger: A teacher or room becomes unavailable in one or more recurring weekly periods.
- Preconditions: A verified accepted baseline exists.
- Relations:
  - Requires: UC-1, because the school and accepted baseline must already be established
  - Includes: UC-3 at step 1; UC-4 at step 3; UC-5 at step 4; UC-6 at step 5
  - Extends: none

### Main success scenario

1. The administrator uses UC-3 to locate the resource and understand its accepted assignments across the recurring
   week.
2. The workspace keeps the currently accepted timetable unmistakable throughout the operation.
3. The administrator uses UC-4 to stage the unavailability and protect accepted period or room assignments that must
   not move.
4. The administrator uses UC-5 to obtain a complete feasible repair proposal without replacing the accepted baseline.
5. The administrator uses UC-6 to account for every proposed change and explicitly accepts the repair.
6. The workspace shows the accepted result as current and makes it available as the baseline for another repair.
7. For a later teacher or room disruption, the administrator starts a new repair from that exact accepted result;
   attempt-scoped pins from the prior repair are absent unless explicitly reapplied.

### Extensions

- 3a. If pins conflict with the disruption, the administrator resolves the explicit conflict in UC-4 before the
  journey can resume at step 4.
- 4a. If no feasible repair is found in a run or the run is cancelled or fails, the accepted baseline remains current;
  the administrator revises or retries the repair draft through UC-4 and UC-5, then resumes at step 5 only after a new
  complete proposal exists.
- 5a. If the administrator discards the proposal, the accepted baseline remains current; the administrator revises the
  retained draft or explicitly discards it, and the use case ends.
- 5b. If acceptance cannot be durably completed, the prior baseline remains current and the administrator may retry the
  unchanged proposal through UC-6; resume at step 6 only after success.
- 7a. If the later disruption concerns a different room rather than a teacher, the same journey applies with room
  availability and independent period and room pin dimensions; end after the later decision.

### Guarantees

- G1. At every step, the workspace identifies exactly one of `no accepted timetable` or a specific accepted timetable;
  draft and proposal states cannot be mistaken for current state.
- G2. The complete journey requires no JSON handling or command-line use by the administrator.
- G3. Direct effects and solver ripple effects remain distinct from staging through final review.
- G4. A subsequent repair uses the newly accepted result's `inputRevision` as direct parent lineage and never silently
  rebases against an older or different timetable.
- G5. All failure paths preserve the previously accepted bundle byte-for-byte and disclose no partial replacement.

### Postconditions

- Success: A complete feasible repair is the accepted weekly timetable, its provenance is intact, and it is ready to be
  the baseline for the next disruption.
- Minimal guarantee: The last durably accepted baseline remains current and identifiable when any draft, solve,
  proposal, review, or acceptance step cannot complete.

## UC-8 - Export the accepted baseline

- Goal: Preserve or transfer the exact accepted workspace baseline in a form that can be verified and re-imported.
- Primary actor: School timetable administrator
- Supporting actors: Local durable storage
- Trigger: The administrator requests export of the current accepted baseline.
- Preconditions: A verified accepted baseline exists.
- Relations:
  - Requires: UC-1, because accepted school state must exist
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests accepted-bundle export.
2. The workspace identifies the exact currently accepted definition and result and shows their school and revisions.
3. The workspace creates one ZIP archive with exactly the entries under [Accepted-bundle archive](#accepted-bundle-archive).
4. The workspace verifies that the completed archive reproduces the same accepted definition, result, versions,
   revisions, and lock provenance when imported.
5. The administrator receives the completed archive while the workspace remains on the same accepted baseline.

### Extensions

- 1a. If no accepted baseline exists, the workspace refuses export, states that an initial proposal must first be
  accepted, exposes no draft or proposal as current, and ends.
- 3a. If archive creation or publication fails, the workspace reports that export did not complete, exposes no partial
  archive as successful, leaves the accepted baseline unchanged, and ends.
- 4a. If verification fails, the workspace rejects the archive, reports a safe integrity reason, leaves the accepted
  baseline unchanged, and ends.

### Guarantees

- G1. Export contains accepted data only. No repair draft, unaccepted proposal, temporary solve output, or unrelated
  local data is labeled or included as current.
- G2. Definition and result content, supported versions, revisions, and the provenance distinction between persistent
  policy locks and attempt-scoped pins are preserved exactly. Values that cannot be preserved cause export failure
  rather than silent omission or conversion.
- G3. Export makes no outbound request and does not publish or distribute the timetable automatically.
- G4. Export never mutates workspace state or the accepted bundle.

### Postconditions

- Success: One verified accepted-bundle archive exists and can reproduce the same accepted baseline through UC-1.
- Minimal guarantee: Failed or refused export produces no archive represented as complete and leaves the accepted
  baseline unchanged.

## Normative data

### Proposal change categories

Every repair proposal review contains exactly these categories - no more, no fewer. A category may be empty.

| Category | Observable meaning |
|---|---|
| Additions | Lesson IDs present only in the proposed complete timetable. |
| Cancellations | Lesson IDs present only in the accepted timetable. |
| Teacher changes | Common lesson IDs whose teacher changed, independently of assignment-move classification. |
| Forced moves | Common lesson dimensions changed by authoritative locks rather than solver-selected movement. |
| Period moves | Common, non-forced lesson assignments whose period changed, with old and proposed period and room. |
| Room-only moves | Common, non-forced lesson assignments whose period stayed and whose room changed. |

The initial increment can legitimately show zero additions, cancellations, teacher changes, and forced moves because it
does not offer those edit types. Their presence in an authoritative result is still shown rather than hidden.

### Accepted-bundle archive

An exported ZIP contains exactly these root entries - no more, no fewer.

| Entry | Required content |
|---|---|
| `school-definition.json` | The exact complete definition of the accepted baseline. |
| `timetable-result.json` | The exact matching verified `FEASIBLE` result. |
| `workspace-manifest.json` | Supported schema and catalog versions, accepted revisions, and workspace provenance needed to distinguish persistent policy locks from attempt-scoped pins. |

## Out of scope

- Authentication, authorization, accounts, remote access, hosted operation, multi-school tenancy, and concurrent or
  collaborative editing.
- Automatic publication, notifications, student or parent access, approval chains, and external-system distribution.
- Comprehensive school-data editing, student-information-system integration, and automatic onboarding.
- Dated calendars, one-off absence handling, cover teaching, substitutions, variable-length lessons, and every other
  domain extension excluded from School Kernel v1.
- First-increment class unavailability, lesson cancellation, required period or room moves, and teacher reassignment.
- Automatic substitute selection and a user-selected `protect if possible` stability tier.
- Parallel proposals, branches, proposal comparison, historical analytics, long-term audit reporting, and school reset
  or switching inside an occupied workspace.
- New scheduling rules outside the existing School Kernel constraint catalog.
- CSV, PDF, print-layout, or publication-ready class, teacher, and room exports. Browser printing is not an accepted
  export contract.
- Mobile editing, repair generation, proposal review, or acceptance.

## External dependencies

- The packaged School Kernel must continue to satisfy `spec/kernel-v1/spec.md` for supported schema/catalog versions,
  lineage, feasibility, scoring priority, revision integrity, diagnostics, change reporting, and failure safety.
- The administrator discovery study requires five qualified participants from at least three schools and a documented
  reference validation machine. Until that evidence exists, the workflow may be implemented and technically verified
  but the product hypothesis is not considered validated.
