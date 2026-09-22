# Timetable Workspace UX Evolution

## Status

Draft for product and contract review. This proposal does not change the approved UC-1 through UC-8 implementation
or its status ledger.

The interaction reference is [`ui/timetable-administrator-workbench-prototype.html`](../../ui/timetable-administrator-workbench-prototype.html).

## Purpose

Evolve the first running timetable workspace into the administrator's primary operational workbench. The next UX
increment should make the complete school easier to scan, make the complete recurring week available without leaving
the workspace, represent simultaneous split-group teaching honestly, and keep repair intent and proposal impact on the
same timetable canvas.

This proposal revisits two assumptions from the first validation increment:

- the primary view no longer has to be day-only; the administrator can switch between a compact day view and a
  compact whole-week view;
- a class is not always visually or operationally indivisible; two or three declared groups may be taught
  simultaneously and must share the class-period cell.

The accepted timetable remains immutable until an explicitly reviewed proposal is accepted.

## Product hypothesis

An administrator can understand workload, availability, constraints, and repair impact faster when:

1. one persistent timetable canvas remains visible through current, draft, and proposal states;
2. lesson tiles have one fixed compact density instead of a presentation setting;
3. day and week are alternate time ranges over the same accepted timetable;
4. subject and teacher selections highlight rather than remove surrounding school context;
5. simultaneous lessons for declared class groups divide one cell evenly;
6. staged constraints and proposed changes appear as overlays on the timetable rather than as a separate form or
   report.

## Primary experience

### Persistent workbench

The application keeps the timetable as the dominant surface. Current, draft repair, and proposal are explicit states,
but switching state does not replace the timetable with an unrelated page. A side inspector holds the active rule,
selected lesson, room policy, or proposal decision.

### Fixed compact lesson tiles

There is no comfortable/compact control. The operational view uses one compact tile:

- subject is the primary label;
- group is shown when the lesson targets a subgroup;
- room is always visible in the lower-right corner, using the smallest accessible supporting text;
- teacher and full lesson identity remain available through selection and accessible labeling;
- a definition-provided short subject label may be used in the week view; the UI does not derive display meaning from
  an opaque identifier.

### Day and week ranges

Day view keeps classes as rows and periods as columns. Week view keeps classes as rows and weekdays as columns. Each
weekday cell contains the definition-ordered period sequence. This avoids a 35- or 60-column wall while still showing
the complete week on one continuous surface.

The view switch changes presentation only. It does not alter accepted data, draft intent, selection identity, or
proposal eligibility.

### Split-group lessons

When two or three declared groups of the same class have assignments in the same period, the class-period slot is
divided into equal-width lesson tiles. Each tile identifies its group and room. Selecting one tile inspects only that
group's lesson.

The UI does not infer groups from lesson names, teachers, subjects, or coincident times. Split rendering requires
explicit contract metadata. Lessons for different groups that occur in different periods remain in their respective
periods rather than being visually joined.

### Subject and teacher highlighting

Subject and teacher highlights are independent and may be active together:

- a subject selection highlights every matching lesson across the visible day or week;
- a teacher selection uses a second visual cue to reveal that teacher's load and free periods;
- a lesson matching both selections carries both cues;
- non-matching lessons remain present and may be dimmed;
- draft conflicts and proposal changes never become too faint to identify;
- clearing either selection immediately restores the remaining highlight.

Highlighting is not filtering. The surface continues to represent the whole school and does not need a misleading
"filtered timetable" label.

### Primary-room policies

A class may have a persistent primary-room policy with explicit exceptions for subjects, declared groups, or lessons.
The administrator sees a preview before applying it: lessons already compliant, new room locks, exceptions, and
conflicts. Persistent room policies are distinct from attempt-scoped pins and retain provenance after acceptance.

The initial policy mode is hard protection because the described use case is a room pin. A softer preference remains a
separate product decision rather than an implicit weakening of the rule.

### Repair and proposal overlays

Staging teacher or room unavailability immediately marks accepted lessons that contradict the draft rule. The UI must
say that these lessons conflict with the draft, not that the accepted timetable is already invalid.

Proposal review uses the same canvas:

- the old placement remains identifiable;
- the proposed placement is visible in context;
- direct effects and solver ripple effects have distinct text and visual cues;
- protected lessons remain visible;
- selection opens exact before/after teacher, period, room, class, subject, and group details;
- only explicit acceptance advances the current timetable.

## Contract implications

### Presentation-only work

The following can be built against the existing accepted snapshot without changing scheduling semantics:

- fixed compact day rendering;
- day/week switching;
- room placement inside every lesson tile;
- independent subject and teacher highlighting;
- retained selection and inspector behavior;
- current/draft/proposal overlays for already supported lessons.

### Split-group contract work

The current kernel contract intentionally defines a cohort as indivisible. A correct implementation therefore needs a
kernel contract extension before the workspace claims split-group support.

The recommended minimal extension is an explicit partition and group model:

- a cohort may declare zero or more partitions;
- each partition declares two or more disjoint groups;
- a lesson either targets the complete cohort or one group in one declared partition;
- complete-cohort lessons conflict with every group of that cohort;
- lessons in the same partition may share a period only when they target different groups;
- lessons in different partitions conservatively conflict unless a later model can prove their student sets are
  disjoint.

This keeps the public semantics deterministic without pretending that all possible school grouping schemes are known.
It requires a versioned kernel proposal, schema/domain changes, hard-constraint changes, fixtures, and packaged CLI
journeys before the workspace consumes it.

### Short labels

The week view benefits from an optional subject `shortDisplayName`. If this is added, it belongs to authoritative
definition metadata. Until then, the UI clamps the full display name and exposes it through selection; it never
abbreviates an opaque subject ID heuristically.

### Room-policy provenance

Primary-room policies are workspace intent compiled into explicit persistent room locks in a successor definition.
The workspace manifest must retain the policy source and exceptions so later imports distinguish persistent school
policy from attempt-scoped pins.

## Recommended implementation sequence

### Increment UX-1 — Compact day/week inspection

Build the fixed compact tile, Day/Week switch, room corner, subject highlight, teacher highlight, and retained selection
using the current accepted snapshot. Do not change kernel contracts or repair semantics.

### Increment UX-2 — Split-group kernel contract

Specify and implement cohort partitions/groups in School Kernel. Prove hard-conflict semantics through schema,
validation, solver, result, plan, replan, and verify journeys. Do not add workspace editing until the kernel slice is
approved.

### Increment UX-3 — Split-group workspace rendering

Consume the approved kernel metadata, render two- and three-way simultaneous splits in Day and Week views, and keep
selection, highlighting, draft conflicts, and proposal differences group-specific.

### Increment UX-4 — Primary-room policies

Add previewed persistent class-room policies with subject/group/lesson exceptions, compile them into successor locks,
and preserve their provenance across acceptance and export.

### Increment UX-5 — Persistent-canvas repair review

Refactor supported unavailability, pinning, generation, and proposal review into the same workbench without changing
the approved lifecycle, ETag, kernel-process, or atomic-acceptance boundaries.

Each increment is implemented and converged independently. UI-facing increments require an administrator walkthrough
before approval or progression to the next increment.

## Validation

Use a realistic school fixture containing:

- at least 60 classes, 100 teachers, 100 rooms, and approximately 1,000 lessons;
- at least one two-group split and one three-group split in the same period;
- at least one group whose lesson occurs at a different period from its sibling group;
- a primary-class room policy with subject and group exceptions;
- a teacher-unavailability draft with direct conflicts and a feasible proposal with ripple effects.

The administrator must be able to:

- switch Day/Week without losing selection, highlights, or workspace state;
- find every lesson for a selected subject;
- see a selected teacher's load and free periods without removing the rest of the school;
- identify the teacher, room, class, and group for every split tile;
- explain why a red lesson conflicts with draft intent;
- distinguish direct changes, ripple changes, and protected assignments;
- identify the accepted timetable correctly before and after proposal acceptance.

Record interaction samples at validation scale, but keep the existing rule that diagnostic latency references do not
replace functional or administrator acceptance.

## Non-goals

- inferring student groups from imported lesson labels;
- student-level enrollment or attendance management;
- arbitrary overlapping student sets in the first group model;
- team teaching or multiple teachers on one lesson;
- mobile editing or proposal acceptance;
- changing the accepted timetable directly through drag and drop;
- combining this work with accounts, multi-school hosting, publication, or substitute-teacher workflows.

## Decisions still to confirm

1. Whether a fresh workspace opens Day or Week by default; the recommendation is to restore the last local choice and
   use Day when no preference exists.
2. Whether week-view short subject labels justify a kernel metadata addition immediately or can wait until real school
   names prove truncation insufficient.
3. Whether primary-room policies are always hard locks or need a separately named preference mode in a later increment.
4. Whether conservative conflict between different group partitions is sufficient for the first contract or real
   schools require explicit overlap sets immediately.
