# Timetable Manual Editing

## Status

Approved proposal for specification.

## Purpose

Allow a timetable administrator to manually adjust the schedule by changing a lesson's slot (period), room, or teacher directly in the timetable workspace. Validation runs automatically to identify conflicts (such as teacher unavailability or clashes, and room occupancy or clashes), highlighting conflicting cells with an explanatory overlay. The system allows persisting this working state even when conflicts exist so the administrator can resolve conflicts iteratively before publishing an updated accepted baseline.

## Primary outcome

An administrator can select any scheduled lesson in the timetable, reassign its period (slot), room, or teacher, immediately see visual indicators and explanations for any resulting conflicts, and save this state across sessions to resume or continue manual conflict resolution until ready to publish.

## Scope

- Manual Draft Lifecycle:
  - Starting manual editing transitions the workspace from `ACCEPTED_BASELINE` into a durable working draft (`MANUAL_DRAFT`).
  - Draft changes and detected conflicts persist across page reloads and server restarts.
  - The previous `ACCEPTED_BASELINE` remains unchanged and available for inspection or rollback until explicit publication.
- Lesson Editing:
  - Selection of any scheduled lesson on the timetable canvas.
  - Reassignment of period (slot), room, and teacher for the selected lesson.
  - Reverting individual lesson adjustments back to the accepted baseline assignment.
- Automatic Validation & Conflict Detection:
  - Teacher double-booking (clash across multiple lessons in the same period).
  - Teacher unavailability (lesson placed in a period marked unavailable for that teacher).
  - Room double-booking (multiple lessons placed in the same room and period).
  - Room unavailability (lesson placed in a period marked unavailable for that room).
  - Class / cohort double-booking (multiple lessons for the same class in the same period).
  - Room compatibility / policy violations (e.g. required room assignment rules).
- Visual Presentation:
  - Distinct visual highlighting for cells and lessons involved in conflicts.
  - Conflict overlay / popover detailing the exact cause, resources involved, and conflicting lesson identities.
  - Clear summary indicator of total active conflicts and readiness to publish.
- Lifecycle Controls:
  - Discarding the draft restores the untouched `ACCEPTED_BASELINE`.
  - Publishing the draft promotes the working schedule to a new `ACCEPTED_BASELINE` version once all blocking conflicts are resolved.

## Confirmed Product Decisions

1. **Working Draft mode:** Manual adjustments do not mutate the accepted baseline directly. They are saved in a durable working draft state.
2. **Hard conflict resolution before publication:** A draft containing unresolved hard conflicts cannot be published as an accepted baseline. The administrator must resolve all blocking conflicts before publishing.
3. **Draft persistence with conflicts:** The system fully persists draft state containing unresolved conflicts to support iterative manual work.

## Non-goals

- Automatic automated re-solving of the rest of the schedule unless explicitly triggered through a separate repair flow.
- Multi-user concurrent editing.
- Creating or deleting lessons or changing school definitions (e.g. adding teachers, rooms, or courses) within this feature.
