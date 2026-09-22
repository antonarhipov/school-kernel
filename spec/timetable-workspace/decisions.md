# School Timetable Operations Workspace: Selected Design Decisions

Status: Accepted for specification

Decision date: 2026-09-20

Source: Design grilling of [`proposal.md`](proposal.md)

Selection mode: The recommended option was selected automatically for every decision, as requested.

On 2026-09-21 the user explicitly selected the recommended resolution of UC-1 convergence finding C-1:

- **D056 — School display name:** The kernel definition contract carries a required nonblank school-level
  `displayName`. The workspace displays that value and never substitutes `schoolId` as the school heading.

This document resolves the proposal's open product decisions and contradictions. It is normative for the subsequent
specification. The workspace remains a client of School Kernel; these decisions do not weaken or redefine the kernel's
existing contracts.

## Product boundary

- **D001 — Recurring-week semantics:** The workspace changes the recurring weekly timetable. “Unavailable on one day”
  means unavailable on that weekday every week represented by the current definition. Dated absences, one-off lesson
  cancellations, cover teaching, and substitutions remain out of scope. The UI must say “weekly” and show the affected
  weekday explicitly wherever an operational change is entered.
- **D002 — No automatic substitute teacher:** The kernel continues to choose only periods and rooms. If an unavailable
  teacher cannot teach a lesson in another available period, the administrator must explicitly assign a different
  qualified teacher or cancel the recurring lesson in a later increment; the solver never invents a substitute.
- **D003 — First validation increment:** The first usable increment includes teacher unavailability, room
  unavailability, individual and bulk period/room pins, repair generation, complete impact review, accept/discard, and a
  second repair from the accepted result. Class unavailability, lesson cancellation, required moves, and teacher
  reassignment follow only after the first administrator study. They remain planned change types, not prerequisites for
  testing the core hypothesis.
- **D004 — Single local operator:** The initial product is one local workspace for one school and one timetable
  administrator. There are no accounts, roles, concurrent editors, remote access, or multi-school switching in this
  slice.
- **D005 — Desktop operational workflow:** Editing, dense whole-school inspection, proposal review, and acceptance target
  laptop and desktop screens. Narrow screens may retain read-only focused schedules, but mobile editing or acceptance is
  not part of the validation claim.

## Runtime and ownership boundary

- **D006 — Local companion application:** A local, single-user controller serves the workspace, persists its state, and
  invokes the packaged School Kernel through its public file-and-CLI contract. Administrators never prepare JSON or run
  commands during the supported journey. The kernel itself remains stateless and headless.
- **D007 — Local-only data surface:** The controller binds only to the local machine, makes no outbound data requests,
  and does not expose school data to a remotely reachable service. Deployment, authentication, and hosted operation are
  separate decisions for a later production slice.
- **D008 — Kernel authority:** The workspace may preflight user intent for fast feedback, but the kernel remains
  authoritative for schema and semantic validation, lineage, feasibility, scoring priority, revisions, and change
  classification. Client-side checks must never turn a kernel failure into a proposal.

## Accepted state, import, and lineage

- **D009 — Accepted baseline bundle:** An accepted baseline is an inseparable pair: the exact complete school definition
  and its matching verified `FEASIBLE` result. A result alone is insufficient because it omits names, period metadata,
  availability, room capabilities, and other definition data needed by the workspace.
- **D010 — Two explicit import modes:** Importing only a valid initial definition creates an initial-planning draft.
  Importing a definition together with a matching `FEASIBLE` result restores an accepted baseline. Status, school ID,
  input revision, timetable revision, assignment integrity, and supported schema/catalog versions must all validate
  before an accepted state is created.
- **D011 — Initial planning is still a proposal:** A definition-only import may be planned through the kernel, but its
  feasible result remains an initial proposal until the administrator accepts it. Failure leaves the workspace without
  an accepted timetable.
- **D012 — Exact successor lineage:** Every repair definition is a complete successor definition whose
  `basedOnRevision` equals the accepted result's `inputRevision`, not its `timetableRevision`. A mismatched definition
  and result pair is rejected; the workspace never guesses ancestry or silently rebases it.
- **D013 — Immutable accepted baseline:** Draft edits, solving, failed searches, and feasible proposals do not mutate the
  accepted bundle. Acceptance first durably stores a complete new definition/result bundle and only then advances the
  current-baseline reference. A crash cannot expose half of a new accepted baseline.
- **D014 — One draft and one proposal:** The workspace has at most one working change set and one generated proposal.
  Any edit after generation invalidates the proposal immediately. Concurrent proposals, branches, comparisons, and
  approval queues are deferred.
- **D015 — Draft recovery:** Draft changes and pins are auto-saved locally and survive restart. Discarding a draft is an
  explicit action. Recovery never promotes a draft or proposal to accepted state.

The actor-visible state model is therefore:

```text
No baseline -> Initial draft -> Solving
                  ^            +-- failure or cancel -> Initial draft
                  |            +-- FEASIBLE -> Initial proposal
                  |                              +-- revise or discard -> Initial draft
                  |                              +-- accept -> Accepted baseline
                  +-- edit ----------------------+

Accepted baseline -- start repair -> Repair draft -> Solving
                                      ^              +-- failure or cancel -> Repair draft
                                      |              +-- FEASIBLE -> Repair proposal
                                      |                                +-- revise or discard -> Repair draft
                                      |                                +-- accept -> Accepted baseline
                                      +-- edit ------------------------+

Repair draft -- discard draft -> Accepted baseline
```

## Change intent and repair controls

- **D016 — Workspace intent overlay:** The workspace records administrator intent separately from kernel documents, then
  compiles it into a complete successor definition for each invocation. This local overlay is needed to explain why a
  lesson is directly affected; it does not become a second public kernel change-command format.
- **D017 — Contract mapping:** Resource unavailability changes `availablePeriodIds`; cancellation removes a lesson;
  teacher reassignment changes `teacherId`; and required periods, required rooms, and pins use `periodLock` and
  `roomLock`. When availability was omitted, staging an unavailability must materialize the remaining available period
  IDs because omission means “available in every declared period.”
- **D018 — No timetable mutation:** The workspace never edits accepted assignment JSON. A drag, drop, or picker that
  requests another period or room stages a required lock in the successor definition; direct assignment mutation would
  invalidate provenance.
- **D019 — Directly affected definition:** Before solving, a lesson is “directly affected” only when a staged resource
  availability change contradicts its accepted assignment or a staged lesson edit targets that lesson. Pins do not make
  a lesson directly affected. “All currently unaffected lessons” is the complement of this preflight set, evaluated
  against the accepted baseline.
- **D020 — Pin meaning:** A pin copies the accepted period, room, or both into a hard lock. A required move locks a
  dimension to a different value. An unpinned dimension remains solver-controlled. Labels, icons, and review text must
  distinguish all three states without relying on color alone.
- **D021 — Attempt-scoped pins:** Pins created in the workspace protect the current repair attempt. They are retained
  while the administrator revises and retries that draft, but they are cleared when the next repair draft starts unless
  explicitly reapplied. Pre-existing locks imported as school policy remain persistent. The workspace must track this
  distinction because both serialize as kernel locks. Clearing a transient pin is a change in the next successor
  definition; it never rewrites the accepted definition. Locks in an imported raw definition are treated as persistent
  policy when no workspace manifest says otherwise.
- **D022 — Conflicts have no implicit winner:** A pin that contradicts staged unavailability or a required new value is
  shown as a blocking conflict. The administrator must remove or change one instruction before solving; pins are never
  silently dropped and operational changes never silently override them.
- **D023 — Bulk pinning is a snapshot action:** A bulk action applies to the exact lesson set shown in a preview at that
  moment. The preview states the selected dimension and lesson count, reports conflicts, requires confirmation, and is
  undoable before solving. No lesson is automatically pinned merely because it is currently unaffected.
- **D024 — No soft protection tier yet:** The first increment provides hard pins and the kernel's global stability
  ordering only. “Protect if possible” must not be simulated with hidden weights or UI-only promises.
- **D025 — Qualified teacher choices:** When teacher reassignment is added, the picker offers only teachers qualified for
  the subject and explains remaining availability conflicts before solving. Selection is always explicit.

## Solving and failure behavior

- **D026 — Bounded background solve:** Repair runs without freezing navigation. The default is the kernel's 30-second
  time limit, with an explicit cancel action. Draft controls are frozen for that run; the administrator cancels before
  editing. After an unsuccessful search, unchanged intent may be retried with one clearly labeled two-minute preset.
  Seed and deterministic step controls are not exposed in the operator UI.
- **D027 — Only feasible output becomes a proposal:** `FEASIBLE` with a complete timetable creates a reviewable proposal.
  `INVALID_INPUT`, `NO_FEASIBLE_SOLUTION_FOUND`, interruption, transport failure, and `INTERNAL_ERROR` leave the accepted
  baseline unchanged and return the administrator to the same editable draft.
- **D028 — Feasible is not “best”:** The UI describes stability as the kernel's priority ordering: period changes before
  room-only changes before ordinary preferences. It never claims that a time-limited result is globally minimal or
  optimal. The termination reason and limit are visible in proposal details.
- **D029 — Honest failure language:** An unsuccessful search is reported as “no feasible repair found within this run,”
  not “impossible.” Constraint IDs and involved entities are translated into plain-language, navigable diagnostics, but
  they are not presented as a proof or a guaranteed recipe for repair.
- **D030 — Proposal identity:** Every proposal is tied to the exact accepted timetable revision, successor-definition
  revision, workspace-intent revision, complete kernel result document, and proposed timetable revision that produced
  it. The workspace does not invent a public whole-result revision absent from the kernel contract. Acceptance is
  refused if any authoritative identity or the revalidated result no longer matches current state.
- **D031 — Rerun replacement:** Starting a replan immediately invalidates the prior unaccepted proposal. A new feasible
  run replaces it; cancelling or failing the run returns to the draft with no proposal eligible for acceptance.

## Whole-school workspace

- **D032 — Primary day matrix:** The primary whole-school view shows every class as rows and the selected day's periods
  as columns. Day navigation and a compact week summary keep the entire recurring week accessible without attempting an
  unreadable 60-column weekly wall. “Whole-school” means the complete school population remains in context, not that all
  weekdays must fit on one screen.
- **D033 — Assignment content:** Each occupied cell leads with subject and shows teacher and room compactly. Empty cells
  remain visible. Selecting a lesson opens its accepted assignment, staged intent, pin state, and any proposal change
  without leaving the whole-school context.
- **D034 — Definition metadata is authoritative:** Display names, weekday, period order, and optional times come from the
  imported definition. The workspace never derives meaning from ID spelling such as `mon-1`; valid kernel IDs are
  opaque.
- **D035 — Search and filters preserve context:** Search highlights matching lessons. Filters for class, teacher, room,
  day, period, affected state, and changed state narrow the matrix and always show an active-filter summary plus a
  one-action reset. A focused filter must not be mislabeled as the complete school.
- **D036 — Secondary focused views:** Dedicated class, teacher, and room schedules remain secondary inspection views.
  The existing focused viewer may inform them, but it does not satisfy the whole-school requirement by itself.
- **D037 — UI terminology:** Use “Class” in administrator-facing text and map it explicitly to the kernel's `cohort`
  contract term. Stable IDs remain available in technical details but are not used as display names.
- **D038 — Accessible state cues:** Accepted, draft, pinned, required, directly affected, solver-moved, and failed states
  use text or icons as well as color. Core navigation, selection, pinning, proposal inspection, and confirmation must be
  keyboard-operable.

## Proposal review and acceptance

- **D039 — Two kinds of impact:** Review separates direct effects of administrator intent from solver ripple effects.
  A lesson can appear in both sections when appropriate, but each kernel change category remains intact.
- **D040 — Contract-bounded categories:** The review uses exactly the observable kernel categories: additions,
  cancellations, teacher changes, forced moves, period moves, and room-only moves. It does not imply that the change
  report enumerates every edited definition field.
- **D041 — Before-and-after fidelity:** Every changed lesson shows old and proposed subject/class/teacher/period/room
  context, highlights only changed dimensions, and links back to both the whole-school and focused views. Unchanged
  lessons stay available but visually quiet.
- **D042 — Unambiguous totals:** The headline total counts unique changed lessons. Category totals follow the kernel
  report. Groupings by class, teacher, room, and day label whether the group is based on the old assignment, proposed
  assignment, or both; cross-group totals are not summed as though they were disjoint.
- **D043 — Explicit acceptance:** Acceptance requires a summary showing proposal status, unique changed lessons, period
  moves, room-only moves, forced changes, and the fact that the current baseline will advance. Only the explicit confirm
  action changes accepted state.
- **D044 — Discard semantics:** Discarding a proposal removes only the proposal. The accepted baseline remains unchanged,
  and the administrator may either keep the draft intent for revision or explicitly discard the draft as a separate
  action.

## Export boundary

- **D045 — Accepted bundle export:** The first slice exports one ZIP archive containing `school-definition.json`,
  `timetable-result.json`, and `workspace-manifest.json`. The manifest records schema/catalog versions, revisions, and
  the workspace provenance needed to distinguish persistent policy locks from attempt-scoped pins. Importing the archive
  must reproduce the same accepted baseline after verification.
- **D046 — Focused-format export deferred:** CSV, PDF, print-layout contracts, and publication-ready class/teacher/room
  schedules are deferred. Browser printing may be convenient but is not an accepted export contract for this slice.
- **D047 — Accepted data only:** Export never labels a draft or unaccepted proposal as current. Publication,
  notifications, and automatic distribution remain outside the workspace.

## Validation and release gates

- **D048 — Realistic scale fixture:** UI validation uses a complete synthetic or properly anonymized school containing
  approximately 1,000 lessons, 100 teachers, 60 classes, 100 rooms, and 60 periods. The existing solver benchmark does
  not substitute for workspace-scale interaction testing.
- **D049 — Administrator study:** At least five people with real timetable responsibility from at least three schools
  perform the recurring teacher-unavailability scenario; each also performs the room-unavailability case and a second
  repair from an accepted first result.
- **D050 — Hypothesis success threshold:** At least four of five participants, after a short orientation, must complete
  the end-to-end journey without JSON, command-line use, or facilitator correction: identify every direct effect, create
  the intended pins, request repair, distinguish proposal from accepted state, account for every proposed change, and
  accept or discard deliberately. All five must correctly identify which timetable is currently accepted at every
  decision point.
- **D051 — Whole-school comprehension measure:** On the scale fixture, at least four of five participants must find all
  lessons directly affected by each scenario and explain the period-move versus room-only ripple totals without missing
  or inventing a change. Preference surveys alone do not establish usability.
- **D052 — Interaction performance evidence (revised 2026-09-22):** After initial load on the reference validation
  machine, record raw samples and 95th-percentile results for search, filters, day changes, selection, pin feedback, and
  proposal-review opening. The former 250 ms interaction and one-second review values are diagnostic references, not
  release or convergence gates. Solver time is measured separately and is not disguised as UI latency. This revision
  prioritizes functional delivery now and leaves observed performance issues for explicit follow-up work.
- **D053 — Solver evidence, not a fixed promise:** Record time limit, termination reason, whether a feasible result was
  found, and change counts for every validation run. The feature must handle slow or unsuccessful searches safely, but
  the discovery study does not invent a universal 30-second feasibility guarantee.
- **D054 — Soft-protection trigger:** Add a user-selected “protect if possible” tier only if at least two study
  participants independently need it to express an actual scenario, or if a core task fails specifically because hard
  pin versus ordinary movement is insufficient. Feedback that it merely sounds useful is not enough.
- **D055 — Safety regression gate:** Automated journey tests must prove that import rejection, invalid input,
  unsuccessful search, interruption, restart, discarded proposals, stale proposals, and failed acceptance persistence
  all preserve the previously accepted bundle byte-for-byte.

## Deliberate consequences

These selections narrow the first validation increment while preserving the larger product direction. In particular,
the first increment is not a dated absence-management or substitution system, the focused viewer is not promoted into a
whole-school workspace, and the UI does not pretend that temporary pins, feasible search results, or failed-search
diagnostics mean more than the kernel contract can prove.
