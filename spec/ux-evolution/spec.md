# Timetable Workspace UX Evolution Specification

## Status and relationship

Draft behavioral specification for the proposal in [`proposal.md`](proposal.md). It is not
part of the approved timetable-workspace UC-1 through UC-8 baseline until its decisions and dependent kernel contract
changes are accepted.

Existing lifecycle, safety, lineage, process, persistence, and acceptance rules remain authoritative. Where this draft
changes presentation assumptions—especially fixed density and whole-week inspection—it is a candidate successor to the
corresponding UC-3 behavior and decisions D032, D035, and D038 rather than a silent reinterpretation of them.

## Actors

- **School timetable administrator:** inspects the complete school, stages repair intent, protects assignments, and
  decides whether proposals become current.
- **School Kernel:** validates and schedules complete definitions, including any future approved group metadata.
- **Local durable storage:** retains accepted state, workspace intent, presentation preference, and policy provenance.

## Terms

- **Time range:** either one selected weekday (`Day`) or the complete recurring week (`Week`).
- **Compact lesson tile:** the single operational lesson representation; there is no density setting.
- **Class:** administrator-facing term for a kernel cohort.
- **Partition:** a declared way in which one class may be divided into mutually disjoint groups.
- **Group:** one declared subset within a partition.
- **Split slot:** one class-period area containing two or three simultaneous group lesson tiles of equal width.
- **Subject highlight:** a non-destructive emphasis applied to every visible lesson with the selected subject.
- **Teacher highlight:** a distinct non-destructive emphasis applied to every visible lesson with the selected teacher.
- **Primary-room policy:** persistent intent that locks applicable class lessons to a room except for explicit
  exceptions.
- **Draft conflict:** an accepted assignment that contradicts staged intent; it is not a claim that the accepted
  baseline has already changed or become invalid.

## Global invariants

1. Inspection controls never mutate accepted, draft, proposal, or policy state.
2. Current, draft, and proposal labels remain visible whenever their overlays are shown.
3. Day/Week switching preserves exact lesson selection when the selected lesson remains representable.
4. Subject and teacher highlighting never removes lessons or changes totals.
5. Every occupied tile exposes subject, teacher, room, class, period, lesson identity, and group when applicable through
   visible text, selection details, or an accessible label.
6. Room is visibly present in the lower-right corner of every occupied tile in both time ranges.
7. Color is never the only indication of highlight, conflict, protection, direct change, ripple change, or selection.
8. The UI never infers group membership, subject abbreviations, weekday, period order, or display names from opaque IDs.
9. Accepted assignment JSON is never edited directly. Draft changes compile into a complete successor definition.
10. Only explicit, durably successful proposal acceptance advances the current timetable.

## Workbench layout

The desktop workbench contains:

1. school identity and current/draft/proposal state;
2. Day/Week control;
3. subject and teacher highlight controls;
4. the whole-school timetable canvas;
5. one contextual inspector for selected lesson, staged rule, room policy, or proposal change;
6. the accepted revision context.

The timetable is the dominant surface. Export, import, diagnostics, and destructive draft actions must not permanently
occupy the primary scanning area.

## Compact tile behavior

### Visible content

- Subject is the leading label.
- Room is placed in the lower-right corner and remains visible without hover.
- Group label is visible when the lesson targets a group.
- Day view may show the teacher inside the tile when space permits.
- Week view may move teacher to selection details, but its accessible name must still contain the teacher.
- Full subject, teacher, room, class, period, group, and lesson identity appear in the inspector after selection.

### Text fitting

- Use an authoritative `shortDisplayName` when supplied.
- Otherwise clamp or elide the display name and expose the complete name through selection and accessible labeling.
- Do not derive an abbreviation from an identifier.
- Supporting room/group text must remain legible at the validated desktop sizes.

### Interaction

- The complete tile is one native button.
- Keyboard and pointer selection produce the same inspector state.
- Selection is distinct from subject highlight, teacher highlight, conflict, and proposal-change states.

## Day view

- Classes are rows.
- Definition-ordered periods for the selected weekday are columns.
- Empty cells remain visible.
- An ordinary lesson occupies the full class-period cell.
- Two or three simultaneous declared group lessons divide the cell evenly.
- The selected weekday is explicit and keyboard-operable.

## Week view

- Classes are rows.
- Weekdays are columns in definition order.
- Each class-day cell contains the definition-ordered period sequence.
- Every occupied period uses the compact tile.
- Simultaneous group lessons divide the period slot evenly.
- The complete week is one continuous timetable surface; horizontal or vertical scrolling may be used at realistic
  school scale without changing the semantic population.
- The UI must not call a subset complete when virtualization or filtering has removed rows from the represented
  population.

## Highlight behavior

### Subject

1. The administrator selects one subject or `All subjects`.
2. Every visible lesson with that exact subject identity receives the subject cue.
3. Non-matching lessons remain in place and may be dimmed.
4. The workbench exposes the number of matches in the represented range.

### Teacher

1. The administrator selects one teacher or `All teachers`.
2. Every visible lesson with that exact teacher identity receives a distinct teacher cue.
3. Empty periods remain readable so teacher availability can be inferred from absence of highlighted lessons.
4. Non-matching lessons remain in place and may be dimmed.

### Combined highlights

- Both controls may be active simultaneously.
- A lesson matching both carries both cues.
- Clearing one control preserves the other.
- Draft conflicts, proposal changes, and current selection retain priority and remain legible.
- Highlight state is local presentation state and is not persisted in the workspace aggregate.

## Split-group contract

The following is the recommended contract shape and remains subject to a separate kernel proposal.

### Definition metadata

A cohort may declare partitions. Each partition contains:

- stable `id` and nonblank `displayName`;
- the parent `cohortId`;
- two or more groups, each with stable `id` and nonblank `displayName`.

A lesson either:

- omits both group fields and targets the complete cohort; or
- supplies both `cohortPartitionId` and `cohortGroupId`, where the partition belongs to the lesson's cohort and the
  group belongs to that partition.

### Validation

- Supplying only one group field is invalid.
- Referencing an unknown partition or group is invalid.
- Referencing a partition owned by another cohort is invalid.
- Duplicate partition IDs or duplicate group IDs inside a partition are invalid.
- A partition with fewer than two groups is invalid.

### Hard period conflicts

For two lessons of the same parent cohort:

- a complete-cohort lesson conflicts with every other lesson in that period;
- lessons targeting the same group conflict;
- lessons targeting different groups in the same partition may share a period;
- lessons targeting different partitions conflict conservatively in the first version.

Teacher-period and room-period hard constraints continue to apply independently. Different groups do not permit the
same teacher or room to be double-booked.

### Result and change identity

Assignments continue to reference stable lesson IDs. Group identity is recovered from the exact definition used by
the result. Proposal change categories remain lesson-based; the workspace adds group display context without changing
kernel category membership.

## Split-slot presentation

1. The workspace groups accepted assignments by parent cohort and period.
2. If one lesson is present, it occupies the full slot.
3. If two or three lessons are present and their contract metadata permits coexistence, each receives equal width.
4. Each sub-tile retains its own subject, teacher, room, group, selection, protection, draft-conflict, and proposal
   state.
5. If sibling groups occur in different periods, each is shown only in its assigned period.
6. If persisted data contains simultaneous same-cohort lessons not justified by approved group metadata, the workspace
   reports an integrity problem rather than inventing a split.

## Primary-room policy

### Intent

A policy contains:

- parent cohort ID;
- required room ID;
- hard enforcement mode;
- zero or more subject, group, or lesson exceptions;
- stable policy provenance.

### Preview

Before staging the policy, the server derives and returns:

- applicable lesson IDs;
- lessons already assigned to the room;
- room locks that would be introduced;
- excluded lessons and the exception that excluded each one;
- conflicts with availability, existing persistent locks, required moves, or attempt-scoped pins;
- the exact accepted and intent revision on which the preview is based.

Confirmation is refused when the preview is stale or altered.

### Compilation

- Applicable lessons receive persistent `roomLock` values in the complete successor definition.
- Exceptions receive no lock from that policy source.
- Other independent persistent locks and attempt-scoped pins are preserved.
- Removing a policy removes only locks whose provenance is that policy.
- A policy becomes part of the accepted baseline only through a feasible proposal and explicit acceptance.

## Draft-conflict overlays

- Staging teacher or room unavailability recomputes directly affected lessons against the accepted timetable.
- Conflicting tiles are marked in Day and Week views immediately after durable draft autosave.
- Split slots mark only the affected group lesson unless the resource rule affects multiple sub-lessons.
- Highlight dimming never obscures a draft conflict.
- Selecting a conflict explains the accepted assignment and the staged rule it contradicts.
- Solving is refused while draft instructions contradict an active pin or persistent policy lock.

## Proposal overlays

- The workbench renders accepted and proposed placement for every changed lesson.
- Direct effects and ripple effects have distinct text and non-color cues.
- Split-group identity remains visible on both before and after states.
- Protected assignments show that their protected dimensions did not change.
- The inspector exposes exact old and proposed subject, teacher, class, group, weekday, period, and room.
- Acceptance retains the existing complete identity revalidation and atomic persistence behavior.

## Candidate implementation use cases

### UX-1 — Inspect a compact day or week

**Goal:** Understand the whole school's accepted timetable at the needed time range.

Main success scenario:

1. The administrator opens an accepted workspace.
2. The workspace renders the fixed compact timetable and visible room labels.
3. The administrator switches Day or Week.
4. The workspace preserves the exact accepted state, active highlights, and selected lesson where representable.
5. The administrator selects a lesson and sees its complete accepted details.

Guarantees:

- No density setting is shown.
- Range changes are presentation-only.
- The represented population and time range are always labeled accurately.
- No inspection action mutates workspace state.

### UX-2 — Highlight subject and teacher load

**Goal:** See where a subject is taught and where a teacher is occupied without losing school context.

Main success scenario:

1. The administrator selects a subject.
2. Every matching lesson receives the subject cue.
3. The administrator also selects a teacher.
4. Every teacher lesson receives the teacher cue and dual matches retain both.
5. The administrator switches Day/Week and the highlight identities remain active.

Guarantees:

- Nonmatches remain present.
- Match counts are exact for the represented range.
- Conflict and proposal states remain legible.
- Clearing one highlight does not clear the other.

### UX-3 — Inspect simultaneous class groups

**Goal:** Understand which groups of a class are taught simultaneously and where.

Precondition: the approved kernel result and definition contain valid partition/group metadata.

Main success scenario:

1. The workspace finds two or three group lessons for one parent class and period.
2. It divides the slot into equal sub-tiles.
3. Each sub-tile shows subject or short subject label, group, and room.
4. The administrator selects one group and sees that group's teacher and complete lesson identity.
5. Day/Week switching preserves the selected lesson.

Guarantees:

- Group identity is never inferred.
- Each tile maps to exactly one lesson ID.
- Teacher and room conflicts remain impossible under kernel hard constraints.
- Proposal changes remain group-specific.

### UX-4 — Stage a primary-room policy

**Goal:** Keep a class in its primary room except for explicit lessons, subjects, or groups.

Main success scenario:

1. The administrator chooses a class and primary room.
2. The administrator records explicit exceptions.
3. The workspace previews exact applicable, compliant, locked, excluded, and conflicting lessons.
4. The administrator confirms the version-bound preview.
5. The workspace autosaves policy intent and overlays resulting protections/conflicts.
6. A later feasible proposal and explicit acceptance make the policy persistent.

Guarantees:

- No accepted assignment is edited directly.
- Preview membership is server-derived and stale-safe.
- Policy locks are distinguishable from attempt-scoped pins.
- Exceptions remain explainable after export and re-import.

### UX-5 — Review repair impact on the timetable canvas

**Goal:** Decide whether a repair proposal is safe without leaving the operational timetable.

Main success scenario:

1. A verified feasible proposal opens in the same Day/Week workbench.
2. The workspace shows unique changes, direct effects, ripple effects, and protected assignments.
3. The administrator follows each change between old and proposed placement and inspects exact details.
4. The administrator explicitly accepts, revises, or discards the proposal.
5. Only successful acceptance advances the current state.

Guarantees:

- Accepted and proposed states are never confused.
- Category membership remains kernel-authored.
- Group context does not change unique lesson totals.
- Existing stale-proposal, rollback, recovery, and ETag guarantees remain unchanged.

## Verification requirements

### Presentation and browser

- Real-browser tests cover Day/Week switching at desktop widths.
- Every occupied tile has visible room text and an accessible complete name.
- Two-way and three-way splits are equal-width within tolerance and separately selectable.
- Subject-only, teacher-only, combined, clear-one, and clear-all highlight paths preserve the complete lesson set.
- Conflict overlays remain legible under active highlights.
- Keyboard selection and range/highlight controls match pointer behavior.
- Narrow screens remain read-only and do not claim the complete operational workflow.

### Kernel contract

- Schema and domain validation cover every partition/group invariant.
- Hard-constraint tests cover whole-cohort versus group, same group, different groups in one partition, different
  partitions, teacher collisions, and room collisions.
- Packaged plan, replan, and verify journeys contain two- and three-group cases.
- Existing indivisible-cohort definitions retain their prior behavior.

### Workspace and persistence

- HTTP/PostgreSQL tests prove inspection/highlighting make no aggregate change.
- Room-policy previews and confirmations are version-bound and server-derived.
- Draft autosave, failed solve, cancellation, stale proposal, and failed acceptance preserve the exact accepted bundle.
- Export/re-import retains group metadata and persistent room-policy provenance.

### Administrator walkthrough

An administrator walkthrough is required for each UI-facing increment. Approval requires the administrator to:

- find a subject across the whole week;
- explain a teacher's occupied and free periods;
- identify all members of a two- and three-way split slot;
- identify every room without opening each lesson;
- explain a draft conflict and a proposal ripple;
- confirm which timetable is current at every step.

Automated evidence does not promote the increment without explicit walkthrough confirmation.
