# School Definition Authoring

## Status

Draft proposal for specification. Revised on 2026-10-11 after proposal grilling and confirmation of the shared
understanding. The [confirmed product decisions](#confirmed-product-decisions) supersede the narrower scope and
proposed defaults from the 2026-10-01 review. This proposal does not claim implementation or replace a use-case
specification.

## Purpose

Allow a timetable administrator to create a school from an empty workspace and maintain its complete School Kernel
definition through forms. The administrator can configure the school, its weekly period grid, subjects, cohorts,
teachers, rooms, teaching requirements, and supported scheduling settings without editing JSON.

The same authoring capabilities serve initial setup and changes to an existing school: a new teacher, a changed
curriculum, a different weekly teaching load, a room policy, or new bell times. After a timetable has been accepted,
authored changes produce a successor definition for repair and explicit acceptance. The accepted timetable remains
current throughout that work.

The `timetable-workspace` proposal deferred school-data onboarding until the repair workflow had shown which data
administrators need to see and manipulate. Repair, inspection, and manual timetable editing now provide the workflows
that full definition authoring will connect to.

## Primary outcome

An administrator can open a durable definition draft from `EMPTY`, `INITIAL_DRAFT`, or `ACCEPTED_BASELINE`. Guided setup
provides an order for creating a new school, while freely navigable sections support incremental changes to existing
schools. Every setting supported by the current kernel contract is available through forms, subject to the draft's
catalog version and an explicit upgrade when needed.

Weekly teaching requirements are the main lesson-entry method: "5A, Mathematics, Mari, five lessons per week" creates
individually identified occurrences in a lesson series. Occurrences remain inspectable and editable, including
exceptions to shared settings. Imported lesson structure is preserved.

The administrator explicitly saves forms and grouped operations. Incomplete or invalid definitions remain durable,
with issues beside the affected items. They can finish when workspace checks report zero blocking issues and School
Kernel verifies the complete definition. Known scheduling impossibilities remain visible advisories; feasibility is
established by subsequent planning or repair.

Before a baseline exists, finishing creates or replaces the initial definition used for planning. After a baseline
exists, finishing opens repair with the complete successor definition. The accepted timetable stays current until the
repair proposal is explicitly accepted. Discarding a seeded repair returns to the authored draft with its changes
intact.

## Scope

- Definition draft lifecycle:
  - Use the durable `DEFINITION_DRAFT` workspace state, opened from `EMPTY`, `INITIAL_DRAFT`, or `ACCEPTED_BASELINE`.
  - Record the source state and, where one exists, the source definition and revision. Keep that source unchanged
    while authoring; accepted definitions and assignments change only on explicit repair acceptance.
  - Persist saved content and validation issues across reloads and application restarts.
  - Show the open draft and its workflow lock throughout the workspace.
- Complete supported definition authoring:
  - School identity and display name, with controlled catalog upgrades and workspace-managed schema/revision fields.
  - Subjects and every supported subject constraint.
  - Periods, optional bell times, and reserved-period membership.
  - Cohorts, teachers, and rooms, including every supported attribute, availability and undesirable periods where
    applicable, qualifications, curator and home-room links, and room capabilities.
  - Lessons and their supported fields: subject, cohort, teacher, series membership, required room capabilities,
    preferred rooms, undesirable periods, and period/room locks.
  - Room-assignment rules, including subject and optional teacher scope, allowed rooms, and home-room policies.
  - Supported soft-constraint weight overrides.
  - The specification will enumerate the supported attributes and catalog requirements in a normative attribute
    table. Authoring uses the current kernel's teaching model.
- Guided setup and editing:
  - Freely navigable sections for school identity, period grid, subjects, teachers, classes, rooms, room policies, weekly requirements, and review.
  - A suggested setup order and visible incomplete work; existing schools use the same sections directly.
  - Initial grid generation from teaching weekdays, periods per day, and optional bell times, followed by individual
    adjustments. Existing grids open directly for editing.
  - Explicit saves per form or operation, with saved/unsaved status and a prompt before leaving unsaved edits.
  - Less common scheduling controls available in advanced sections.
- Weekly teaching requirements:
  - Generate individual occurrences with stable identifiers and a series for each new weekly requirement.
  - Shared cohort and subject, common teacher and scheduling settings, and clearly marked occurrence exceptions.
  - Increasing a count adds occurrences; decreasing it requires choosing which occurrences to remove.
  - Shared-setting edits preserve exceptions by default, with a preview and an explicit option to apply to all.
  - Preserve imported series and keep ungrouped lessons individually editable until explicitly grouped.
- Guided removal and source-based reverts:
  - List every dependent reference before removal and let the administrator explicitly resolve it through
    replacements, removal of dependent settings, or selected lesson cancellations.
  - Apply the selected resolutions and removal together once no dependent references remain unresolved.
  - Revert an existing item or individual attribute to its source-definition value, and restore deleted source items.
  - Remove newly added items through guided removal. Reverts undergo the same validation as other edits.
- Validation and change review:
  - Immediate workspace checks on saves, with incomplete and invalid definitions persisted.
  - A complete issue list linked to the affected items and settings. Where an edit causes an issue elsewhere, show
    that relationship, such as a qualification change invalidating a lesson's teacher.
  - Necessary-condition feasibility advisories, including excessive load for available periods and missing suitable
    room capacity or capabilities.
  - Review additions, removals, changed attributes, occurrence exceptions, lesson cancellations and reassignment,
    room-policy changes, catalog adjustments, and preference changes against the source.
  - Review period weekday, position, and bell-time changes with the affected lessons even when period identifiers
    remain unchanged and the kernel reports no identifier-based move.
  - Authoritative kernel definition verification before finishing.
- Finish, repair, and discard:
  - From an empty source, finish produces `INITIAL_DRAFT`; from an initial source, it replaces the initial definition.
  - From an accepted source, finish opens repair seeded with the successor definition and retains the authored draft.
  - Seeded repair permits additional availability staging and assignment pins; review shows the combined changes.
  - Discarding seeded repair drops its staging and pins and restores the authored draft unchanged. Accepting repair
    publishes the successor and closes the retained authored draft.
  - Discarding authoring requires confirmation and returns to the unchanged source state, including `EMPTY` for a
    newly created school.
- Teacher-change routing:
  - Teacher changes use definition authoring and repair after acceptance.
  - Replace the manual timetable editor's teacher-changing control with guidance to authoring, respecting the
    single-draft lock.

## Confirmed product decisions

These decisions follow the numbered questions in the 2026-10-11 grilling session.

1. **Complete creation from `EMPTY`.** An administrator can create a definition ready for first planning entirely
   through forms, including the period grid, subjects, resources, and lessons. A skeleton import is not a prerequisite.
2. **The same capabilities throughout.** Full authoring applies to new, imported initial, and accepted schools.
   Every post-baseline change, including a display-name correction, requires repair and explicit acceptance. The
   successor's `basedOnRevision` is the accepted result's `inputRevision`.
3. **Keep the single-draft lock.** An open authoring draft blocks planning, repair, manual editing, imports, clearing,
   and workspace replacement. The accepted timetable remains inspectable and exportable. Every refusal names the
   draft and provides a route to it. Finish or discard releases authoring's lock into the appropriate next state.
4. **Weekly requirements are the primary lesson-entry method.** Enter cohort, subject, teacher, and weekly count;
   generate individual occurrences that remain available for inspection and detailed editing.
5. **Choose occurrences when reducing counts.** Show current placements and individual constraints where available,
   and require the administrator to select the occurrences to remove. Surviving occurrences retain their identifiers.
   Increasing the count adds new occurrences.
6. **Support occurrence exceptions.** A requirement shares a cohort and subject and offers common teacher and
   scheduling settings. Individual occurrences can override those settings; exceptions are clearly marked.
7. **Preserve exceptions during shared edits.** Update occurrences following the shared setting and preserve explicit
   exceptions by default. Preview affected occurrences. Replacing exceptions requires an explicit "apply to all"
   choice.
8. **Author the full supported contract.** Include subject constraints, reservations, lesson requirements and
   preferences, lesson locks, room-assignment rules, and soft-constraint weights alongside the core school data.
   Advanced sections keep less common controls accessible.
9. **Generate the initial grid, then adjust it.** Teaching weekdays, daily period counts, and optional bell times
   produce the initial weekly grid. Individual periods and days remain editable, including shorter days and reserved
   periods. Existing schools open their current grid directly.
10. **Identifiers remain stable.** School, subject, resource, period, lesson, and room-rule identifiers are immutable
    after creation. Display names remain editable. Renaming a display name leaves identity and references intact.
11. **Removal is guided and explicit.** Show every dependency and let the administrator choose valid replacements or
    remove dependent settings and explicitly selected lessons. Apply the choices together only when no references
    remain unresolved. Nothing cascades implicitly. Historical references in the accepted timetable do not block
    successor-definition removal; their effects go through repair and review.
12. **Period timing can change in place.** Weekday, position, and bell times remain editable while retaining the period
    identifier. Authoring and repair review show old/new timing and every affected lesson. Identifier-based movement
    reports alone are insufficient for this review.
13. **Availability keeps its two meanings.** "All periods" includes newly added periods and is stored by omitting
    `availablePeriodIds`. An explicit selection excludes new periods until enabled. The editor names both modes and
    previews the impact of grid additions.
14. **Offer explicit catalog upgrades.** Existing schools can upgrade to the latest supported catalog, with newly
    available settings and changed defaults explained before confirmation. Required repair upgrades receive the same
    explanation. New schools use the latest supported catalog.
15. **Preserve imported lesson structure.** Existing series appear as weekly requirements; ungrouped lessons remain
    individually editable. Opening authoring does not regroup lessons. Explicit grouping explains any scheduling
    effect of changed series membership.
16. **Upgrades preserve effective scheduling behavior.** Where newer defaults differ, write explicit settings to
    preserve the prior constraints and preference weights and show those adjustments in the upgrade preview. Apply
    this policy to required upgrades in authoring as well. Administrators can deliberately adopt newer defaults
    afterward. New schools start with current defaults.
17. **Optional controls express intent.** Offer "Use catalog default," "Set a value," and "No limit" where supported,
    showing the effective default. Choosing the catalog default preserves omission. In particular, omitting a current
    daily gap limit means zero gaps, so unrestricted gaps require an explicit supported value.
18. **Finish verifies the definition.** Saved drafts may be incomplete or invalid. Finishing requires zero blocking
    workspace issues and successful kernel definition verification. Necessary-condition feasibility advisories remain
    visible and nonblocking; they never promise planning success. Subsequent planning or repair establishes whether
    a timetable can be produced.
19. **Zero-lesson definitions may finish.** Review clearly states that no lessons are configured and planning will
    produce an empty timetable. This supports incremental setup and deliberate cancellation of all existing lessons.
    The new-school acceptance journey nevertheless demonstrates populated requirements and a useful first timetable.
20. **Save explicitly per form or operation.** Show saved and unsaved work, allow incomplete definitions to be saved,
    and prompt before leaving unsaved edits. Saved content and issues survive reloads and application restarts.
21. **Discarding seeded repair restores authoring.** Preserve the authored changes while repair is open. Discard its
    staging and pins and return to the authored draft unchanged. Accepting repair publishes the successor and closes
    the retained draft. Fully abandoning the work requires a separate explicit authoring discard.
22. **Keep general copying and bulk editing deferred.** Grid generation, shared weekly-requirement edits, guided
    removal, and catalog upgrades are the agreed grouped operations. Arbitrary cross-resource copying and bulk
    application of settings remain outside this feature.
23. **Teacher changes use authoring.** The definition is the source of truth for a lesson's teacher. Manual timetable
    editing directs teacher changes to authoring instead of offering a teacher-changing control that cannot publish
    successfully. The prior review's manual-publication premise is corrected below; manual-teacher adoption is dropped.
24. **Guide setup without enforcing a rigid sequence.** Offer freely navigable school, grid, subject, resource,
    requirement, and review sections. Suggest an order and show incomplete work. Existing schools use the same editor.
    Verification governs finish readiness.
25. **Revert against the source.** Restore source items or individual attributes and restore deleted source items.
    Newly added items can be removed through guided removal. Reverts trigger validation. Discarding the draft returns
    to its unchanged source. Persistent operation undo/redo is deferred.
26. **New requirements create lesson series.** The configured series spreading preference applies to generated
    occurrences. This is a soft preference; hard daily limits use supported subject constraints. Series grouping
    guarantees neither one lesson per day nor double-period adjacency.
27. **Suggest readable identifiers.** Derive suggestions from display names using a fixed transliteration rule:
    strip diacritics (`Õ` to `o`, `ä` to `a`, `š` to `s`), lowercase, and replace other disallowed characters with `-`.
    Suggestions must be valid and unique. Allow an advanced override before first save, then keep the identifier
    immutable. Generate occurrence identifiers automatically and preserve them through subsequent edits.
28. **Repair staging and authoring coexist.** Seeded repair can stage additional unavailability and assignment pins on
    top of the authored definition. Review exposes the combined changes. Discarding repair drops those additions and
    restores the retained authored draft unchanged.
29. **Use two complementary acceptance journeys.** Demonstrate a small complete school created through forms and
    accepted after first planning, plus authoring and repair of the real MVK school. The specification separately
    covers advanced controls, validation, recovery, and preservation of imported definitions.

## Correction to the earlier teacher-publication assumption

The 2026-10-01 review stated that manual editing could publish an assignment teacher different from the definition and
that later repair would silently revert it. Current code does not support that premise. Manual draft edits change the
assignment, but [publication](../../timetable-workspace/src/main/java/org/schoolkernel/workspace/ManualDraftService.java)
verifies the candidate result against the unchanged definition.
[Kernel baseline verification](../../kernel-cli/src/main/java/org/schoolkernel/domain/BaselineVerifier.java) rejects an
assignment whose teacher differs from its lesson's teacher. A divergent baseline would also fail repair's baseline
verification.

Teacher changes therefore follow the supported definition-authoring path. The proposal replaces the manual editor's
teacher-changing control with guidance to authoring and removes the proposed adoption of manually published teacher
differences.

## Acceptance journeys

### Create and accept a small school

Start from `EMPTY` and create the school entirely through forms. Set its identity, generate and adjust its weekly grid,
create subjects and resources with suitable qualifications and rooms, and enter populated weekly requirements. Save
partial work and demonstrate that it survives a reload. Review and verify the complete definition, finish into
`INITIAL_DRAFT`, plan, inspect the resulting timetable, and explicitly accept the first baseline.

### Author and repair the accepted MVK school

Start from the accepted MVK baseline (`examples/mvk.json`, catalog 9). Cohort 6C currently lacks a home room. Set a
suitable home room through authoring, make a curriculum change through a weekly requirement, and change an existing
period's bell time while retaining its identifier. Review the definition changes and the lessons affected by the time
change, including those without identifier-based moves.

Finish into seeded repair. Demonstrate that discarding it restores the authored changes, then finish again, prepare
and plan repair, review the combined definition and timetable effects, and explicitly accept the successor. The old
accepted definition and timetable remain current until acceptance.

The specification must also cover individual advanced settings, imported initial sources and lesson structure,
occurrence exceptions and count changes, catalog upgrades, availability semantics, guided removal, source-based
reverts, invalid drafts and issue attribution, zero-lesson outcomes, save failures, reload/restart recovery, and source
immutability. These journeys are acceptance requirements for the proposed implementation, not claims of completed
verification.

## Deferred

- Publishing post-baseline definition changes without repair planning, including changes that move no assignments.
- Parking an authoring draft while another workflow runs, with subsequent reconciliation or rebasing.
- General copying of resource settings and arbitrary bulk resource edits beyond the agreed grouped operations.
- Persistent operation undo/redo history.
- Bulk import from spreadsheets or student-information systems.
- Multi-user concurrent authoring, accounts, and approval chains.

## Non-goals

- Adding kernel teaching models absent from the current contract: variable-duration or adjacent double-period
  lessons, overlapping/split student groups, merged-cohort lessons, team teaching, multi-room lessons, or
  solver-selected teachers.
- Renaming identifiers of existing items.
- Adding a manual-editor handoff that transfers teacher edits and pending timetable edits into authoring.
- Publishing a post-baseline definition directly as accepted without repair planning and explicit acceptance.
