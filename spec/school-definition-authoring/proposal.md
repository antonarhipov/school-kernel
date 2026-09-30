# School Definition Authoring

## Status

Draft proposal for specification.

## Purpose

Allow a timetable administrator to add and configure the school's cohorts, teachers, and rooms, including every
constraint the definition contract attaches to them, directly in the timetable workspace. Today the only way to change
these resources is to edit School Kernel JSON by hand and re-import it. That excludes the administrator the workspace
was built for, and it cannot express the most common mid-year change ("a new teacher starts next week") without
abandoning the accepted timetable.

The `timetable-workspace` proposal deferred school-data onboarding until the repair workflow had shown which data
administrators need to see and manipulate. That workflow now exists, together with inspection and manual editing, so
authoring is the next missing link.

## Primary outcome

An administrator can open a durable definition draft from either an initial draft or the accepted baseline, add,
configure, or remove cohorts, teachers, and rooms using form controls instead of JSON, see every validation issue next
to the resource it concerns, and finish the draft once School Kernel accepts the complete definition. Before a
baseline exists, finishing replaces the initial definition used for planning. After a baseline exists, finishing hands
a complete successor definition to the existing repair workflow, and the accepted timetable stays current until a
repair proposal is explicitly accepted.

## Scope

- Definition draft lifecycle:
  - A new durable `DEFINITION_DRAFT` workspace state, opened from `INITIAL_DRAFT` or `ACCEPTED_BASELINE`.
  - The draft records its source state and source definition revision. The source remains unchanged until the draft
    is finished.
  - Draft content and validation issues persist across page reloads and application restarts.
- Resource authoring for cohorts, teachers, and rooms:
  - Adding a resource with an immutable identifier and a display name.
  - Editing every contract attribute of that resource type that the draft's catalog version supports, as listed in
    the specification's normative attribute table.
  - Weekly period availability and undesirable periods, edited on the school's declared period grid with reserved
    periods marked.
  - Cross-resource links owned by these resources: teacher subject qualifications, cohort curator, and cohort home
    room.
  - Reverting a single resource to its source-definition value.
  - Removing a resource, refused while any other part of the definition still references it.
- Validation:
  - Immediate workspace-side checks on every save. Invalid drafts are persisted, not rejected.
  - Issues caused in lessons or room rules by a resource edit (for example, removing a qualification a lesson depends
    on) are attributed back to the resource edit that caused them.
  - Authoritative School Kernel definition verification before the draft can be finished.
- Change review:
  - A summary of added, changed, and removed resources and changed attributes against the source definition.
- Lifecycle controls:
  - Discard, with confirmation, returning to the unchanged source state.
  - Finish: replace the initial definition (pre-baseline) or open a repair draft seeded with the successor definition
    (post-baseline).

## Confirmed product decisions

1. **Pre- and post-baseline authoring:** Authoring is available both before the first timetable is accepted and after.
   Before, it edits the definition that initial planning uses. After, it produces a complete successor definition
   whose `basedOnRevision` is the accepted result's `inputRevision`, and that definition must go through repair
   planning and explicit acceptance. Authoring never mutates an accepted baseline or its assignments.
2. **Blocked deletion:** A resource that is still referenced (by a lesson, a cohort curatorship or home room, or a
   room-assignment rule) cannot be removed. The workspace lists every dependent. The administrator resolves them first.
   Nothing is ever cascaded implicitly.
3. **Persist invalid, gate on finish:** Drafts are saved even when incomplete or invalid, so that multi-step edits
   (for example, adding a teacher and then naming them as a cohort's curator) are possible. A draft can be finished only
   when workspace checks report zero blocking issues and School Kernel verification of the complete definition
   succeeds.

## Proposed defaults (to confirm during specification review)

- **Starting from `EMPTY` is out of scope.** Because periods and subjects cannot yet be authored, a school must first
  be imported (a skeleton definition with periods and subjects is enough) before its resources can be authored.
- **Identifiers are immutable.** An ID is chosen when the resource is added and never changes afterwards; display names
  stay editable. Renaming an ID means removing the resource and adding a new one. This keeps lineage, assignments, and
  exports unambiguous.
- **Catalog version is not upgraded by authoring.** An attribute the draft's catalog does not support is shown as
  unavailable, with the catalog version that introduces it. Upgrading silently would change scheduling meaning (catalog
  5, for example, makes cohort gaps hard). Post-baseline drafts use the catalog version the repair workflow would produce
  anyway (at least `8`).
- **Omission is preserved.** "Available in every period" is stored as an omitted `availablePeriodIds`, not as an
  explicit list of every period. Optional numeric limits that are cleared are omitted, not written as defaults.

## Open question

A newly added cohort has no lessons, so it adds nothing to schedule until lessons exist. Lesson and curriculum
authoring, subjects, periods, and room-assignment rules are outside this proposal. Should lesson authoring become the
immediate follow-up feature, or be pulled into this one?

## Non-goals

- Authoring subjects, periods, reserved periods, lessons, room-assignment rules, or soft-constraint weights (all read-only
  here and usable only as reference targets).
- Creating a school from an empty workspace.
- Changing identifiers of existing resources.
- Upgrading a definition's catalog version.
- Publishing a post-baseline definition change as accepted without repair planning, even when the accepted timetable
  happens to remain valid.
- Bulk import from spreadsheets or student-information systems.
- Multi-user concurrent authoring, accounts, or approval chains.
