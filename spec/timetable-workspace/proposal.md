# School Timetable Operations Workspace

## Proposal

School Kernel should gain an operator-facing workspace for the person responsible for maintaining a school's timetable.
Its primary purpose is not to display one class or teacher schedule. It is to let a school administrator understand the
whole school, describe an operational disruption, control what may change, and decide whether a proposed repair should
become the current timetable.

This is the next product-discovery slice after the headless scheduling kernel. It should validate the difficult
administrator workflow before the project invests in accounts, multi-school hosting, integrations, or a comprehensive
school-data editor.

## Problem

The kernel can generate and minimally replan a timetable, but using it currently requires manually prepared JSON,
command-line invocations, and separate inspection of the result. That is not a usable workflow for a school
administrator.

Operational replanning also requires more intent than "run the solver again." The administrator needs to understand:

- what became impossible or undesirable;
- which lessons are directly affected;
- which parts of the accepted timetable must not move;
- how far a proposed repair ripples across the school;
- whether the repair is safe to accept.

A class, teacher, or room timetable answers a local viewing question. It does not provide the whole-school context
needed to make and approve those decisions.

## Product hypothesis

A school administrator can safely operate School Kernel when the product provides one workspace that closes this loop:

```text
Observe the whole school
        ↓
Describe a disruption or required move
        ↓
Protect selected assignments
        ↓
Generate a repair proposal
        ↓
Inspect its full impact
        ↓
Accept or discard it
```

The accepted timetable remains the current state until the administrator explicitly accepts a repair proposal.

## Primary actor

The primary actor is the **school timetable administrator**: the person responsible for creating, maintaining, and
approving the operational timetable for the whole school.

Teachers, students, and class coordinators are consumers of published schedules. Their class-, teacher-, and room-level
views are separate use cases and are not substitutes for the administrator workspace.

## Proposed experience

### Whole-school timetable

The administrator starts from a dense but navigable view of the complete school timetable. It shows all classes across
the recurring week and makes teacher and room assignments available without requiring the administrator to leave the
whole-school context.

The administrator can filter, search, zoom, and focus on a class, teacher, room, day, or period. These actions narrow the
presentation but do not turn the administrator's primary workspace into a single-resource timetable.

The product also provides dedicated class, teacher, and room schedules for focused inspection and later publication.

### Operational changes

The administrator can describe a concrete reason for replanning, initially including:

- a teacher becoming unavailable for selected periods;
- a room becoming unavailable for selected periods;
- a class becoming unavailable for selected periods;
- a lesson being cancelled;
- a lesson being required in a different period or room;
- a lesson receiving a different preassigned teacher.

Before replanning, the workspace highlights the lessons directly invalidated or changed by the new information.

### Pinning and repair scope

The administrator can pin the period, the room, or both dimensions of an existing lesson. A pin is a hard instruction:
the pinned dimension must remain unchanged in the repair proposal. Pins can be applied to one lesson or in bulk to a
meaningful selection such as a day, class, or all currently unaffected lessons.

Unpinned lessons may move, but the kernel continues to prefer the smallest disruption to the accepted timetable:
period changes are minimized before room-only changes, followed by ordinary timetable preferences.

The interface must distinguish clearly between:

- a pin that preserves an existing value;
- a required new value that deliberately forces a move;
- an unpinned assignment that the solver may change if necessary.

The need for an intermediate "protect if possible" priority is a product question to validate with timetable
administrators. It is not assumed in the initial slice because the current kernel supports absolute locks and global
minimal-change replanning, but not user-selected stability tiers.

### Repair proposal

Replanning produces a draft repair proposal, never an immediate replacement of the accepted timetable. The workspace
shows:

- lessons directly affected by the operational change;
- every added, cancelled, teacher-changed, forced, period-moved, and room-only-moved lesson;
- old and proposed values for every change;
- totals grouped by class, teacher, room, and day;
- constraint or feasibility diagnostics when no repair can be produced.

The administrator can inspect any changed lesson in both whole-school and focused views. Unchanged lessons remain
visually quiet so that the ripple effect is legible.

### Decision and continuity

The administrator can accept or discard the proposal. Accepting it makes the proposed timetable the new current
baseline for the next repair. Discarding it leaves the accepted timetable unchanged and allows the administrator to
adjust changes or pins and try again.

The accepted timetable and its focused class, teacher, and room schedules can be exported. Publication and automatic
distribution to teachers, students, or external systems are outside this first slice.

## Initial customer scenario

The first end-to-end scenario should be deliberately narrow and realistic:

1. A school has an accepted timetable.
2. A teacher becomes unavailable for several periods on one day.
3. The administrator sees the affected lessons in the whole-school timetable.
4. The administrator pins assignments that must not be disturbed.
5. The administrator requests a repair.
6. The workspace presents the proposed changes and their ripple effects.
7. The administrator accepts the repair or revises the pins and tries again.
8. The accepted repair becomes the baseline for a subsequent replanning request.

A room-unavailability scenario should be used as the second validation case because it exercises independent period and
room decisions.

## Initial scope

The first slice should include:

1. One school and one timetable administrator.
2. Import of a valid School Kernel definition and, when available, an accepted current timetable.
3. A complete-school timetable as the administrator's primary view.
4. Dedicated class, teacher, and room views as secondary use cases.
5. Entry of the operational changes listed above.
6. Individual and bulk period and room pins.
7. Replanning through the existing School Kernel behavioral contract.
8. A complete before-and-after change review.
9. Explicit acceptance or rejection of a repair proposal.
10. Retention and export of the accepted timetable as the next replanning baseline.

The slice may initially run locally or in a controlled single-user environment. Its purpose is to validate the
administrator's decisions and the correspondence between UI intent and kernel input/output before selecting a
production deployment model.

## Out of scope

The first slice does not include:

- authentication, authorization, multi-user editing, or multi-school tenancy;
- automatic publication, notifications, or student and parent access;
- a comprehensive school-definition editor or integration with a student information system;
- concurrent proposals, approval chains, or collaborative conflict resolution;
- historical analytics, audit reporting, or long-term event storage;
- new scheduling rules that are outside the current School Kernel constraint catalog;
- dated calendars, substitutions, variable-length lessons, or other domain extensions already excluded from kernel v1;
- an intermediate user-defined stability priority unless customer validation shows that absolute pins and global
  minimal-change replanning are insufficient.

School-data onboarding remains a necessary product capability, but it should be proposed separately after this slice
establishes what data administrators need to see and manipulate during real replanning work.

## Validation

The proposed workflow should be tested with realistic school-scale data rather than only the small demonstration
timetable. At least the teacher- and room-unavailability scenarios should be walked through with people who have real
timetable responsibility.

The hypothesis is supported when administrators can, without using JSON or the command line:

- find all lessons affected by a disruption;
- express which period and room assignments must not move;
- request a repair and distinguish the draft from the accepted timetable;
- identify every proposed change and understand its wider impact;
- reject or accept the proposal with confidence;
- continue from the accepted result when the next disruption occurs.

The validation must also determine whether the whole-school view remains understandable at approximately 1,000
lessons, 100 teachers, 60 classes, 100 rooms, and 60 weekly periods, and whether administrators need a softer protection
level between an absolute pin and an ordinary movable assignment.

## Relationship to School Kernel

The workspace is a client of the existing scheduling runtime rather than a replacement for it. The kernel remains the
authority for input validation, hard feasibility, minimal-change ordering, revisions, and change classification.

The product must preserve the kernel's safety boundary: invalid input or an unsuccessful search must not expose a
partial replacement timetable, and the currently accepted timetable must remain intact until a complete feasible repair
is explicitly accepted.

