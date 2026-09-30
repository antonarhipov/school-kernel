# Timetable Matrix Lenses

## Status

Draft proposal for product review. This is a presentation successor to the focused class, teacher, and room schedules
specified in [`timetable-inspection-ux`](../timetable-inspection-ux/spec.md) UC-3 and carried forward by
[`timetable-workbench-layout`](../timetable-workbench-layout/spec.md). It does not change their recorded use-case
verdicts until this feature is specified, ruled, implemented, converged, and explicitly approved.

## Purpose

Today the workspace has two ways to look at one teacher, room, or class, and they disagree.

- The whole-school matrix has Class, Teacher, and Room filters. They hide non-matching lessons but keep classes as
  rows. A teacher filter therefore produces a sparse scatter of tiles across many class row groups. Reading
  "what does this teacher do on Tuesday?" means scanning the whole school.
- The focused schedule answers that question. It does so on a separate surface: a per-day list with its own layout, its
  own tile markup, and a "Return to whole school" button. The administrator loses the grid, the inspector, and the
  editing affordances they were just using.

The administrator should not have to switch surfaces to change perspective. This feature makes the teacher and room
filters **lenses** on the same Week/Day matrix. Selecting a teacher or room replaces the class row groups with a single
row group for that teacher or room. The grid shape, tiles, inspector, highlights, conflict overlays, and manual-editing
controls stay the same. The focused schedule surface is removed.

## Primary outcome

A school timetable administrator selects a teacher or a room from the matrix filters and immediately sees that
teacher's or room's complete recurring week (or selected day) in the same grid as the whole school, with each tile
naming the subject, the class, and the other resource (room for a teacher, teacher for a room). They can select,
inspect, and edit lessons there, then clear the lens to return to the whole school without losing range, day,
highlights, or scroll position.

## Scope

- **Lens as a filter.** The existing Teacher and Room filters become lenses. Selecting one pivots the matrix rows from
  classes to that single teacher or room. Clearing it restores class rows.
- **One lens at a time.** Teacher and room lenses are mutually exclusive. Choosing one clears the other. Class, period,
  subject-only, and search behave as today and intersect with the active lens.
- **Same matrix, both ranges.** Week keeps weekdays as columns and period orders as rows. Day keeps the selected
  weekday's periods as columns. In a lens, the row header names the teacher or room instead of a class.
- **Lens-aware tiles.**
  - Class rows (no lens): unchanged. Week shows subject and room; Day shows subject, teacher, and room.
  - Teacher lens: subject, room, and class label.
  - Room lens: subject, teacher, and class label. The constant room label is dropped.
- **Honest empty cells.** In a teacher or room lens, an empty cell whose period is outside the resource's declared
  availability is marked unavailable. An empty cell is never called "free" unless availability is declared.
- **Clashes become visible.** Two lessons that share a teacher or room in one period appear stacked in the same lens
  cell. In a manual draft, their conflict highlight and overlay work as they do on class rows.
- **Entry from the inspector.** The selected lesson's teacher, room, and class names in the inspector offer
  "Show week" actions that apply the corresponding lens or class filter. These replace the "Focused schedules" buttons.
- **All lifecycles.** Lenses work in Current, Draft, Proposal, and Solving modes with the same represented-population
  and review-target rules that apply to filters today.
- **Narrow screens.** The read-only narrow view uses the same Day matrix narrowed to one class or one lens, instead of the
  focused agenda list.
- **Removal.** The focused schedule surface, its entry buttons, its return button, and its renderer are removed.

## Confirmed product decisions

1. **Auto-pivot, single selection.** Selecting a teacher or room filter pivots the matrix to one row group for that
   entity. Multi-entity comparison is not part of this feature.
2. **One lens at a time.** Teacher and room lenses are mutually exclusive; picking one clears the other.
3. **Room-lens tiles show subject, teacher, and class.** The teacher lens shows subject, room, and class.
4. **The focused schedule is removed everywhere**, including the narrow read-only path.

## Proposed decisions for review

These follow from the confirmed decisions but have not been explicitly confirmed.

1. **Teacher investigation "Show only matches" applies the teacher lens.** The teacher investigation highlight stays
   as a whole-school tool. Its separate filter mode is redundant once a teacher lens exists, so the checkbox applies the
   lens for the same teacher. The subject investigation is unchanged.
2. **The class filter does not pivot.** It already narrows the class rows to one row group, which is the class's week.
   It stays a plain filter and can intersect with a lens ("this teacher's lessons with 7B").
3. **Editing out of the lens does not follow the lesson.** If a manual edit reassigns the selected lesson to another
   teacher or room, the lesson leaves the lens. The workspace announces this, keeps the lesson open in the inspector, and
   does not switch the lens.
4. **The lens is ephemeral.** Like other filters, it resets on reload. Only range and weekday persist locally.

## Non-goals

- Comparing several teachers or rooms side by side, or a "rows by" axis switch showing all teachers as rows.
- New scheduling semantics, new conflict codes, or any change to the kernel contract.
- Searchable or type-ahead entity pickers. Native selects remain.
- Keyboard-only completion guarantees beyond what the current workbench provides.
- Printing or exporting a per-teacher or per-room timetable.

## Validation

Use the validation-scale snapshot from `timetable-inspection-ux`: at least 60 classes, 100 teachers, 100 rooms, and
900–1,100 accepted lessons. The critical tasks are:

- "What does teacher T teach on Wednesday, and where?" Measure from the whole-school Week, with no surface change.
- "Is room R double-booked anywhere in this draft?" Measure in a manual draft containing one seeded `ROOM_CLASH`.
- "Move one of T's lessons and confirm T has no clash." Measure end to end within the teacher lens.

Success means the task finishes without leaving the matrix and the administrator can say which population is
represented at every step.
