# School Definition Authoring

## Status

Draft proposal for specification. Revised on 2026-10-01 after a critical review. The review's decisions are recorded
under [Review decisions](#review-decisions).

## Purpose

Allow a timetable administrator to add and configure the school's cohorts, teachers, and rooms, including every
constraint the definition contract attaches to them, directly in the timetable workspace. Today the only way to change
these resources is to edit School Kernel JSON by hand and re-import it. That excludes the administrator the workspace
was built for, and it cannot express the most common mid-year changes ("a new teacher starts next week", "a teacher
leaves") without abandoning the accepted timetable.

The `timetable-workspace` proposal deferred school-data onboarding until the repair workflow had shown which data
administrators need to see and manipulate. That workflow now exists, together with inspection and manual editing, so
authoring is the next missing link.

## Primary outcome

An administrator can open a durable definition draft from either an initial draft or the accepted baseline. In it they
can add, configure, or remove cohorts, teachers, and rooms using form controls instead of JSON, and move lessons from
one teacher to another. They see every validation issue, and every obvious scheduling impossibility, next to the
resource it concerns. They can finish the draft once School Kernel accepts the complete definition. Before a baseline
exists, finishing replaces the initial definition used for planning. After a baseline exists, finishing hands a
complete successor definition to the existing repair workflow. The accepted timetable stays current until a repair
proposal is explicitly accepted, and discarding that repair draft returns the administrator to their authored changes
instead of throwing them away.

## Scope

- Definition draft lifecycle:
  - A new durable `DEFINITION_DRAFT` workspace state, opened from `INITIAL_DRAFT` or `ACCEPTED_BASELINE`.
  - The draft records its source state and source definition revision. The source remains unchanged until the draft
    is finished.
  - Draft content and validation issues persist across page reloads and application restarts.
  - While the draft is open, every workspace view says so and names what it blocks.
- Resource authoring for cohorts, teachers, and rooms:
  - Adding a resource with an immutable identifier and a display name. The suggested identifier is derived from the
    display name by a fixed transliteration rule.
  - Editing every contract attribute of that resource type that the draft's catalog version supports, as listed in
    the specification's normative attribute table.
  - Weekly period availability and undesirable periods, edited on the school's declared period grid with reserved
    periods marked.
  - Cross-resource links owned by these resources: teacher subject qualifications, cohort curator, and cohort home
    room.
  - Reverting a single resource, or a single attribute of it, to its source-definition value.
  - Removing a resource, refused while any other part of the definition still references it.
- Teacher reassignment:
  - Moving selected lessons and teacher-scoped room-assignment rules from one teacher to another. This changes
    `teacherId` only and is the one edit to lessons and room-assignment rules in this feature.
- Validation:
  - Immediate workspace-side checks on every save. Invalid drafts are persisted, not rejected.
  - Issues caused in lessons or room rules by a resource edit (for example, removing a qualification a lesson depends
    on) are attributed back to the resource edit that caused them.
  - Advisory feasibility checks for conditions that make planning certain to fail: more lessons than available
    periods, a cohort larger than every room, and a required room capability that no room provides.
  - For an accepted source, advisories when the catalog raise changes the meaning of an omitted cohort attribute, and
    when an accepted assignment's teacher differs from its lesson's teacher.
  - Authoritative School Kernel definition verification before the draft can be finished.
- Change review:
  - A summary of added, changed, and removed resources, changed attributes, and reassigned lessons and rules against
    the source definition.
- Lifecycle controls:
  - Discard, with confirmation, returning to the unchanged source state.
  - Finish: replace the initial definition (pre-baseline) or open a repair draft seeded with the successor definition
    (post-baseline).
  - Discarding a repair draft that was seeded by authoring returns to the definition draft with its content intact.

## Confirmed product decisions

1. **Pre- and post-baseline authoring:** Authoring is available both before the first timetable is accepted and after.
   Before, it edits the definition that initial planning uses. After, it produces a complete successor definition
   whose `basedOnRevision` is the accepted result's `inputRevision`, and that definition must go through repair
   planning and explicit acceptance. Authoring never mutates an accepted baseline or its assignments.
2. **Blocked deletion:** A resource that is still referenced (by a lesson, a cohort curatorship or home room, or a
   room-assignment rule) cannot be removed. The workspace lists every dependent and says which of them can be resolved
   in this feature. The administrator resolves them first. Nothing is ever cascaded implicitly.
3. **Persist invalid, gate on finish:** Drafts are saved even when incomplete or invalid, so that multi-step edits
   (for example, adding a teacher and then naming them as a cohort's curator) are possible. A draft can be finished only
   when workspace checks report zero blocking issues and School Kernel verification of the complete definition
   succeeds.

## Review decisions

These resolve the questions raised in the 2026-10-01 review.

4. **Teacher reassignment is in scope; curriculum authoring is not.** Without it, an added teacher teaches nothing and
   a departing teacher cannot be removed, because every lesson still references them. Reassignment changes only
   `teacherId` on selected lessons and on room-assignment rules that name the teacher. Creating, deleting, or otherwise
   editing lessons stays out of scope. This answers the earlier open question.
5. **The definition is the source of truth for who teaches a lesson.** Manual editing changes an accepted assignment's
   teacher but not `lessons[].teacherId`, and the kernel builds every new result from `lessons[].teacherId`
   (`ResultFactory`). Repair planning would therefore quietly revert a manual teacher change. Authoring checks
   qualification against `lessons[].teacherId`, reports each divergent assignment as an advisory, and offers
   reassignment to adopt the manual teacher in the definition. Changing manual publication itself is outside this
   feature.
6. **The single-draft lock stays, and it is loud.** While `DEFINITION_DRAFT` is open, planning, repair, manual
   editing, and imports are refused. Every view shows that the draft is open, and every refusal names the draft and
   links to it. Parking a draft while a repair runs, with rebasing afterwards, is deferred until actual use shows the
   lock is a problem.
7. **Discarding a seeded repair draft returns to the definition draft.** Finishing post-baseline keeps the authored
   draft with the repair draft. Discarding the repair draft discards its staging and pins and restores the definition
   draft, so the administrator can revise and finish again. Accepting the repair proposal drops the kept draft. A full
   abandonment is two explicit discards.
8. **Every post-baseline change goes through repair,** including one that moves no lesson, such as a display-name fix.
   Publishing without solving would need a kernel mode that re-verifies accepted assignments against a successor
   definition. That mode does not exist, and this feature does not add it.
9. **Feasibility advisories are necessary conditions only.** They flag only situations in which planning cannot
   succeed. They never block finishing, and they never claim that planning will succeed.
10. **Repair staging and authoring coexist.** Repair staging remains the quick path for absences discovered during
    repair. Authoring is the path for every other resource change. Both end in the successor definition, and a repair
    draft seeded by authoring can still stage unavailability and pins on top.
11. **The first real use is the acceptance scenario.** MVK (catalog 9) still lacks a home room for 6C. Setting it
    through a definition draft opened from the MVK accepted baseline, finishing, repairing, and accepting is the
    feature's end-to-end acceptance journey.

## Proposed defaults (to confirm during specification review)

- **Starting from `EMPTY` is out of scope.** Because periods and subjects cannot yet be authored, a school must first
  be imported (a skeleton definition with periods and subjects is enough) before its resources can be authored.
- **Identifiers are immutable.** An ID is chosen when the resource is added and never changes afterwards; display names
  stay editable. Renaming an ID means removing the resource and adding a new one. This keeps lineage, assignments, and
  exports unambiguous. The suggested ID strips diacritics (`Õ` to `o`, `ä` to `a`, `š` to `s`), lowercases, and
  replaces every other disallowed character with `-`.
- **Catalog version is not upgraded by authoring.** An attribute the draft's catalog does not support is shown as
  unavailable, with the catalog version that introduces it. Post-baseline drafts use the catalog version the repair
  workflow would produce anyway (at least `8`). When that raises the catalog, the draft says so and reports every
  cohort whose omitted attribute now has a constraining default (catalog 5, for example, makes cohort gaps hard).
- **Omission is preserved.** "Available in every period" is stored as an omitted `availablePeriodIds`, not as an
  explicit list of every period. Optional numeric limits that are cleared are omitted, not written as defaults.

## Deferred

- A no-solve publication path for post-baseline changes that move no accepted assignment (decision 8).
- Parking a definition draft while another workflow runs (decision 6).
- Lesson and curriculum authoring beyond teacher reassignment.

## Non-goals

- Authoring subjects, periods, reserved periods, lessons, room-assignment rules, or soft-constraint weights (all
  read-only here and usable only as reference targets), except the `teacherId` reassignment above.
- Creating a school from an empty workspace.
- Changing identifiers of existing resources.
- Upgrading a definition's catalog version.
- Publishing a post-baseline definition change as accepted without repair planning, even when the accepted timetable
  happens to remain valid.
- Copying availability or other attributes between resources, and other bulk edits.
- Bulk import from spreadsheets or student-information systems.
- Changing how manual editing publishes teacher changes.
- Multi-user concurrent authoring, accounts, or approval chains.
