# School setup editor

## Purpose and scope

Give school administrators a clear way to create a school definition from an empty school, then generate a timetable
from it. The main work is arranging the school week, entering subjects, teachers, classes and rooms, and defining
each class's weekly teaching requirements. Administrators should be able to understand and complete this work
without editing JSON.

The product direction was confirmed on 2026-10-17. This is a fresh proposal for the public demo. The earlier
[school definition authoring proposal](../school-definition-authoring/proposal.md) remains historical reference;
its navigation, lifecycle, full-contract coverage goal and implementation approvals do not define this editor.

The first delivery includes the structured editor and new kernel support for explicitly required consecutive
lesson blocks of two or more lessons. Real-product definition versioning and activation are future requirements
described below. The consecutive-lesson direction was refined on 2026-10-17: adjacency is forbidden by default,
not merely discouraged by a spreading preference.

## Current demo context

The demo opens the timetable and offers school and timetable selectors. Utilities contains a JSON definition
editor, saving a newly named school copy, and timetable generation. Local schools, timetables and unfinished work
are retained in the browser. The new editor joins this timetable-first experience.

JSON remains a separate advanced tool. Moving between structured authoring and JSON uses explicit import or
export actions; the two editors are not continuously synchronized views of one working buffer.

## Entry and setup journey

A vertical **School setup** label near the upper-left side of the timetable opens the selected school's editor.
The editor slides from left to right over the timetable and covers the full application area. Closing it returns
to the timetable. Starting a new school opens an empty school draft in the same editor.

The suggested path is **Timetable grid → Subjects → Classes → Teachers → Rooms → Review**. The grid is the first
step. Sections remain freely navigable, and administrators can leave incomplete records and return to them.
The school name is available in the editor's header rather than becoming a large preliminary form.

Each section shows the work that remains. Autosave has a clear saving, saved or failed status. Closing and
reopening the editor or reloading the page restores unfinished work. Generation is a separate explicit action.

Use searchable browsing and focused editing for lists of teachers, classes, subjects and rooms. The interface
must remain practical with dozens of teachers and multiple parallel classes. Use familiar labels such as
**Teachers**, **Classes** and **Rooms**.

## Define the school week first

The administrator builds a day pattern, explicitly copies it to selected weekdays, and adjusts individual days.
Copied days are independent: changing Friday does not silently change Monday or a shared template.

The day editor arranges timed intervals for ordinary lessons, breaks, lunch and reserved teaching slots. It supports
different start and end times and different rhythms on different days. Overlapping or invalid intervals produce
issues that can be corrected while the draft remains saved.

Ordinary breaks and school-wide lunch intervals are non-teaching time. Reserved slots are teaching opportunities
for permitted subjects. For example, a school can start ordinary lessons at 08:45 while offering a reserved
08:00 slot for special lessons. Permitting a subject grants access to all reserved slots, as in the current kernel;
it does not require that subject to use them.

Existing subject controls can limit reserved occurrences per class and week, limit occurrences per day, and
require a lesson to sit at the edge of the class's occupied day. These controls can express the earlier special
lesson case: permission to use an early reserved slot at most once per week, with another occurrence eligible for
the end of a class day. They permit that arrangement rather than guaranteeing it.

Classes share this school grid. A class may additionally block particular teaching periods for its lunch, allowing
5A to have lunch during period 4 while 8A teaches then and has lunch during period 5. These blocks make the
affected class unavailable; they do not change the bell times for other classes. Class lunch blocks remain visible
when inspecting that class's schedule.

## Class plans own teaching allocations

A class's teaching plan owns the required number of weekly lessons and their actual teacher allocations. The
ordinary row is a subject, a weekly count and a teacher. For example:

| Class | Subject | Required each week | Teacher allocation |
| --- | --- | --- | --- |
| 5A | Mathematics | 5 | Mari 5 |
| 5B | Mathematics | 5 | Mari 3, Jaan 2 |

Single-teacher allocation is the normal entry. Optional split teaching lets the administrator divide the weekly
count between teachers. Show required, allocated and unassigned counts together, so a row can clearly read
**Mathematics: 5 required, 3 allocated, 2 unassigned**. Unassigned demand remains valid draft work and blocks
generation until it is resolved.

A class may have an optional curator. One teacher can curate several classes. Nominating a curator does not
create or allocate a lesson; curator lessons follow their explicit teaching requirements and the kernel's curator
rules.

Copying a class plan copies its subjects and required weekly counts. The destination class receives an independent
plan in which teachers are assigned separately. This is a copying action, without a linked grade curriculum.

## Teachers own eligibility and availability

A teacher profile records the subjects the teacher can teach and the classes for which they are eligible for each
subject. Eligibility is a relationship such as **Mathematics → 5A, 5B; Physics → 8A**, rather than a combination of
two unrelated subject and class lists.

Teacher profiles show actual workload derived from class plans. They do not contain a second editable weekly
allocation. Changes to a class plan therefore appear in the teacher's workload without requiring duplicate entry.

When assigning a teacher in a class plan, the administrator can explicitly extend eligibility through an action
such as **Allow Mari to teach Mathematics in 5A**. Selecting a teacher alone must not silently change qualification
or class eligibility. Eligibility changes that conflict with existing allocations produce visible issues rather
than silently reassigning lessons.

New teachers start available in all teaching slots. Availability is edited on a weekly grid using clearly labelled
marking tools:

- **Unavailable** is a hard restriction, such as part-time working limits.
- **Prefer to avoid** is a soft preference that generation may leave unsatisfied.
- **Available** clears the marking.

The grid uses the school's periods, so administrators can mark a whole day, an afternoon or individual slots.
Availability in a reserved period still requires the subject's reserved-slot permission.

## Rooms and placement requirements

Generation normally chooses a suitable room, respecting capacity, capabilities, availability and all hard room
requirements. A class can prefer a home room, while a subject can be restricted to selected rooms. For example,
5A prefers room 102 and Chemistry must use lab 201 or 202.

A class-subject row may require one fixed room. That requirement applies to every weekly occurrence in the row.
The first demo does not divide one row's occurrences into separate room requirements such as one lab lesson and
two ordinary-room lessons.

Room preferences and hard requirements must be visibly distinct. Hard requirements apply together: a fixed room
must also satisfy any applicable subject or teacher-subject room policy. Contradictory rules are issues to resolve,
not an implicit precedence hierarchy.

Rooms have names, capacities, capability information and availability. Less common placement controls belong
beside the relevant subject, class or teaching requirement, under expandable scheduling settings.

## Weekly distribution and explicit consecutive blocks

Ordinary weekly lessons prefer to spread across days. Five Math lessons do not automatically impose a hard
one-per-day rule. However, the same class cannot have consecutive lessons in a subject unless its plan explicitly
requires that consecutive block. An administrator can separately set a supported subject daily maximum.

Consecutive lessons are an explicit hard constraint on a class's subject plan. No permission for consecutive
lessons is created automatically. Examples are:

> 5A → Mathematics → 5 lessons per week → Require exactly 1 block of 2 consecutive lessons per week.
>
> 8A → Technology → 4 lessons per week → Require exactly 1 block of 3 consecutive lessons per week.

Each rule records the block length (an integer of at least two), its weekly block count, and whether that count is
**at least** or **exactly**. Block length and weekly frequency are separate. A block consumes that many of the
required weekly lessons; it does not add lessons to the total. Different lengths require separate explicit rules.
Remaining occurrences retain the normal spreading preference and cannot form an undeclared consecutive block.

Every occurrence in a block uses the same teacher and room. With split teaching, a whole block belongs to one
teacher's allocation. Every occurrence must satisfy that teacher's and class's availability, room restrictions,
reservation permissions and other applicable constraints.

Consecutive means neighboring teaching periods on the same day. An ordinary break is allowed between them;
lunch interrupts the block. Adjacency is based on the declared grid, not on the nearest occupied lessons after
skipping unavailable or unused periods.

The timetable displays separate lesson cells with intervening breaks visible. It does not merge their appearance
into a single session spanning the breaks.

Blocks are maximal runs of that class's subject in adjacent teaching periods. A run of three is one three-lesson
block, not a double and a single; a run of four is not two doubles. The run must match an explicitly required
length, its occurrences must share a teacher and room, and its weekly count must satisfy the corresponding rule.
One occurrence cannot satisfy multiple blocks. A timetable violating these rules or containing an undeclared
consecutive block cannot be reported as valid. Required grouping is not itself penalized by the spreading
preference; extra clustering allowed by an **at least** rule can still affect quality.

## Review readiness and save a school copy

Review brings together the remaining setup work, weekly lesson totals, allocations and scheduling rules. It links
issues to their class, teacher, subject, room or grid interval so an administrator can correct them directly.
Regular teaching capacity and reserved capacity must be distinguishable; reserved slots are not freely available
to every subject.

Generation requires complete teacher allocations and a valid kernel definition. Necessary-condition checks can
identify excessive load, missing room options, incompatible hard rules or a block with no available consecutive
slots. Passing those checks does not establish that a complete timetable exists. Soft preferences remain visibly
different from blocking definition issues and hard constraints.

An incomplete draft remains saved and editable. A school definition can be saved without generating a timetable.
In the demo, editing an existing school and saving the result creates a newly named school copy. Draft autosave
does not create a new named copy after every edit. The copy retains the structured authoring information as well
as the complete kernel definition when it is ready for generation.

The administrator explicitly generates a timetable from a saved, ready definition. Generation uses that saved
snapshot and identifies the school copy to which its result belongs. Existing school definitions and timetables
retain their own data. A failure to generate keeps the saved school and its draft available for correction.

The editor uses the demo's browser-local persistence and existing generation limits. Save failures must be visible;
the interface must not claim that work has been retained when storage failed.

## Authoring data and kernel responsibilities

The structured editor needs durable authoring data beyond the current kernel JSON. That includes unassigned weekly
demand, eligibility by teacher, subject and class, named break and lunch intervals, and class lunch markings.
This information belongs with the authored school and its restorable draft, rather than only in a temporary JSON
editor buffer. Timetable inspection must retain access to the relevant grid information.

The current JSON schema rejects unknown properties and requires a teacher for every lesson. It has subject
qualifications but no subject-class eligibility field. The editor must therefore retain authoring intent separately
and produce kernel lessons from completed class allocations. Teacher eligibility guides authoring; the kernel
continues to receive explicitly assigned teachers.

Explicit JSON import and export must explain their scope. Kernel JSON alone cannot restore all authoring intent.
Importing a definition must preserve its scheduling requirements and clearly handle information that cannot be
reconstructed; it must not invent broader eligibility or quietly discard existing constraints. Kernel JSON exports
must not be presented as lossless backups of unfinished authoring data.

Existing kernel availability is a whitelist: omission means all declared periods, while an empty list means none.
The editor's blockout presentation must translate correctly and preserve the intended effect when the grid changes.
Subjects have no general availability matrix in the current contract; contextual subject controls must use actual
supported rules rather than inventing a blocked-period field.

The current kernel has no flexible consecutive-block requirement or default hard prohibition of undeclared
consecutive lessons. A daily maximum neither requires a block nor makes lessons adjacent, and a series currently
supplies only a soft spreading preference. Locking lessons to administrator-selected times is not a substitute
for letting the solver choose a valid block.

Consequently, this feature requires a declared kernel representation of block requirements and valid adjacency,
solver enforcement, result verification and actionable failure explanations. Lunch boundaries must reach that
model even though the current solver period value does not carry break or lunch semantics. Existing definitions
and saved timetables retain their original versioned scheduling behavior. Adopting the new contract explicitly
introduces the default prohibition and any declared blocks; a legacy definition is not silently upgraded.

## Future product definition versions

In the real product, edits belong to the same school through versioned definitions. A definition version can be
saved without a timetable, and selecting the active version is explicit. Timetables retain the exact definition
version against which they were created and validated.

The intended activation workflow is **save a definition version → generate or review a compatible timetable →
activate the compatible pair**. Keep the existing active definition and timetable available while a successor is
being prepared.

The kernel can already review existing assignments against a supplied definition without solving again. Such a
review checks whether that arrangement satisfies the new definition. Added or removed lessons, changed identities,
and removed rooms or periods require explicit mapping or unresolved-placement handling before review.

Hard violations make an arrangement invalid; unmet soft preferences affect its quality. An invalid old arrangement
does not prove that no timetable can satisfy the new definition. Likewise, a bounded solve that finds no timetable
is not proof of impossibility. Version storage, mapping and activation belong to the future product delivery.

## Scope boundaries

The primary onboarding journey starts with an empty school. Arbitrary school spreadsheets are not a supported
import contract. The editor may use the existing JSON route explicitly, but administrators do not need a prepared
file to begin.

Merged classes, parallel elective streams, ordered sequences of different subjects, automatic swap chains and a
new analytics dashboard remain outside this delivery. Same-subject consecutive blocks of two or more lessons are
the specifically agreed kernel extension. This proposal does not require rebuilding post-generation timetable editing.

## Acceptance journeys for the specification

The use-case specification should make these administrator journeys observable:

1. Create a useful school from empty through forms, starting with a day pattern, copied weekdays, ordinary breaks,
   lunch and a reserved early slot. Save and reopen unfinished work, including an unassigned class requirement.
2. Assign class requirements from eligible teachers, explicitly extend eligibility from a class, split a weekly
   allocation, and see the correct derived workload in teacher profiles. Nominate the same curator for two classes.
3. Copy a class's subjects and weekly counts, assign its teachers independently, and demonstrate that later edits
   do not change the source class.
4. Configure strict teacher blockouts, softer avoidance preferences and staggered class lunches. Explain each
   marking consistently in authoring, review and the timetable.
5. Prefer a class home room, restrict a subject to specialist rooms, and require one room for a full class-subject
   allocation. Surface a conflicting hard room requirement instead of overriding another rule silently.
6. Generate with consecutive lessons forbidden by default, then explicitly require blocks of two and three lessons
   and satisfy their lengths, weekly counts, adjacency, same-teacher and same-room requirements. Exercise minimum
   and exact counts, an allowed ordinary break, a prohibited lunch crossing and rejected undeclared longer runs.
   Show individual lesson cells and retain the configured total weekly count.
7. Save a newly named demo school copy without generation, reopen its full authoring information, then generate
   from its saved snapshot. Preserve the source school and existing timetables throughout.
8. Exercise the explicit JSON boundary and recovery from save or generation failure. Browse and edit a school
   with a realistic number of teachers and classes, as well as the small school created from empty.

Specification should separate the kernel contract and verification work from editor journeys so a visual control
cannot be accepted as evidence that required blocks are enforced. Detailed block-counting scenarios, imported-data
preservation, grid-change effects and draft recovery belong in those use cases before implementation.

## Source references

- [Current demo product direction](../demo-timetable-experience/proposal.md) and
  [demo entry and persistence flow](../../timetable-workspace/src/main/resources/static/workspace/demo-workbench.js).
- [Current definition schema](../../kernel-contract/src/main/resources/schema/school-definition-v1.schema.json),
  including qualifications, availability, lessons, room policies and subject placement rules.
- [Definition validation and effective defaults](../../kernel-cli/src/main/java/org/schoolkernel/domain/DefinitionValidator.java).
- [Current scheduling constraints](../../kernel-cli/src/main/java/org/schoolkernel/solver/SchoolConstraintProvider.java)
  and [solver period values](../../kernel-cli/src/main/java/org/schoolkernel/solver/PeriodValue.java).
- [Assignment review against a definition](../../kernel-cli/src/main/java/org/schoolkernel/application/ReviewService.java).
- [Existing reserved-slot and day-edge requirement](../kernel-day-edges/proposal.md).
- [Earlier authoring proposal](../school-definition-authoring/proposal.md), retained as historical reference.
