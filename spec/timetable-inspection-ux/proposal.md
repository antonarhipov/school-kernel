# Timetable Inspection UX

## Status

Focused proposal derived from the forward-looking [`ux-evolution`](../ux-evolution/proposal.md) roadmap and its
completed product grilling. This proposal is a candidate presentation successor to the accepted whole-school
inspection behavior in timetable-workspace UC-3. It does not change the approved UC-1 through UC-8 baseline until this
feature is specified, ruled, implemented, converged, and explicitly approved.

## Purpose

Make the complete accepted school timetable easier to inspect across its recurring week without losing the precise Day
view needed for operational work. Preserve the existing ability to search, narrow, and open class, teacher, and room
schedules while adding a complete Week overview, independent subject and teacher highlighting, and an honest teacher
availability summary.

## Primary outcome

A school timetable administrator can begin with the whole recurring week, locate subject and teacher activity without
removing surrounding school context, move into an exact Day view, and inspect any accepted lesson while always knowing
which timetable and population are represented.

## Scope

- Week is the default desktop time range when the device has no valid preference for this school.
- Week is a complete-data overview; Day remains the precise operational matrix.
- Compact tiles are the default. Existing density or zoom accommodation remains available until representative
  validation proves it unnecessary.
- Day tiles visibly retain subject, teacher, and room. Week tiles keep subject and room visible while the exact teacher
  remains available through accessible text and selection details.
- Subject and teacher selectors highlight by default. An explicit `Show only matches` action converts the selected
  identity into a clearly disclosed filter.
- A teacher ribbon distinguishes assigned, available-but-unassigned, and unavailable periods from authoritative
  definition data; timetable emptiness is never called teacher availability.
- Search, class/room/period narrowing, one-action filter reset, and focused class/teacher/room schedules remain
  available.
- Selection, range switching, counts, scrolling, virtualization, keyboard operation, and narrow-screen behavior have
  deterministic semantics.
- Inspection remains presentation-only and cannot mutate the accepted baseline, repair draft, proposal, or policy
  state.

## Product decisions

- The last desktop Day/Week choice and weekday are retained locally per school and device. Other investigative state is
  reset on reload.
- Day-to-Week retains lesson selection. Week-to-Day opens the selected lesson's weekday, or the last weekday when no
  lesson is selected. Manually leaving a selected lesson's weekday clears the selection with an announcement.
- Counts describe the complete represented population and time range, not merely the rendered viewport.
- Positive cues identify matches; nonmatches are not dimmed by default.
- Authoritative full subject names are clamped when necessary and remain complete in accessible text and the inspector.
  This feature does not add `shortDisplayName` to the kernel contract.
- Empty Week slots remain structurally visible but do not imply teacher availability.
- Narrow screens open a read-only Day or focused agenda and do not claim to provide the desktop operational workbench.
- Accepted definitions without authoritative group metadata render ordinary lessons and expose no disabled or invented
  group controls.

## Validation

Use a complete synthetic or properly anonymized school with at least 60 classes, 100 teachers, 100 rooms, and between
900 and 1,100 accepted lessons. Five timetable professionals from at least three schools perform the critical inspection
tasks. At least four of five must complete every task without a serious error, all five must identify the current
accepted timetable correctly, and median completion time for behavior shared with the accepted UI must be lower than
the accepted UI baseline.

Record real-browser behavior, accessibility, complete-population counts, raw interaction timings, and exact durable
state before and after inspection. Raw timings beyond the comparative usability result remain diagnostic evidence, not
standalone release thresholds.

## Non-goals

- persistent Draft or Proposal canvas integration;
- cohort partitions, group targets, or split-slot rendering;
- primary-room policies or soft room preferences;
- kernel contract or scheduling-semantic changes;
- mobile editing, repair generation, or proposal acceptance;
- changing accepted assignments through inspection;
- authentication, multi-school hosting, publication, or substitute-teacher workflows.
