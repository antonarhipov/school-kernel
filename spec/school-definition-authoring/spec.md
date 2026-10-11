# School Definition Authoring Specification

## Feature summary

A school timetable administrator creates or maintains a complete School Kernel definition through forms, starting
from an empty workspace, an initial definition, or an accepted baseline. Weekly teaching requirements generate stable
lesson occurrences; individual exceptions remain editable. Saved incomplete work is durable. Finishing verifies the
definition and supplies initial planning or a seeded repair. Only explicit timetable acceptance advances Current.

This is the behavioral contract for the confirmed [proposal](proposal.md), including its 29 decisions. It specifies
behavior to implement and verify; it records no implementation or convergence approval.

## Scope and resolved decisions

The feature covers school identity, the weekly grid and reservations, subjects, teachers, classes, rooms, weekly
requirements and individual lessons, room policies, preference weights, catalog upgrades, guided removal, source-based
reverts, review, and authoring's integration with planning and repair. “Full supported authoring” means exactly the
fields and meanings in the local Normative data tables. Schema version 1 and catalogs 1–9 are supported; new schools
use catalog 9. Definition/revision bookkeeping is system-managed.

The proposal's decisions remain in force. Smaller choices are resolved as follows:

| Choice | Decision and rationale | Governing contract |
|---|---|---|
| Access and language | Retain the single local administrator and English workspace controls; preserve school-entered names and diacritics. No new account, role, or language switch | UC-1 G4–G6 |
| Presentation | Desktop authoring above 700 CSS px; narrow inspection is read-only. New creation starts in School, existing/resumed work in Review; named sections reach every control | UC-1 main step 5, G4; UC-15 G4 |
| Imported shared settings | Infer a common value only when every occurrence agrees; mixed values are shown as mixed, without inventing a default or rewriting lessons | UC-8 G2; UC-9 G2 |
| Identifier reuse | Removed identifiers remain reserved within the current authoring lineage. Only source items can be explicitly restored; increasing a count creates fresh identities | UC-8 G1; UC-13 G2; Identifier rules |
| Source reverts across upgrades | Restore the source's effective scheduling value under the retained newer catalog, using explicit values where omission would change its meaning | UC-14 G2; Catalog upgrade adjustments |
| Earlier repair pins | Expire prior attempt-scoped locks in the working successor when authoring opens, explain the expiry, and preserve exact source provenance. Explicitly authored persistent locks remain policy even if equal to an old pin | UC-1 G1; UC-9 G4; UC-14 G2; UC-17 G3 |
| Proposal versus draft discard | Discarding a generated proposal retains repair intent; abandoning the seeded repair draft restores authoring and drops that repair intent | UC-17 extension 5b; UC-18 |
| Imported/unmappable data | Preserve all supported fields and omission distinctions. Refuse unsupported versions or unknown fields rather than silently drop or guess them | UC-1 extension 2b; UC-16 G2 |
| Verification evidence | Isolated workspace state, real browser, durable reload/restart observations, packaged kernel verification/planning, and automatic approval after an agent-run UI walkthrough | UC-1 G7; UC-16 G5; UC-17 G6 |

## Actors and domain terms

- **School timetable administrator:** the existing local operator; creates, edits, reviews, finishes, and abandons work.
- **School Kernel:** verifies complete definitions and candidate timetables, and produces initial or repair results.
- **Local durable workspace:** preserves saved work and atomically advances lifecycle state.
- **Class:** the administrator-facing term for a contract cohort.
- **Current / accepted baseline:** the exact accepted definition, matching verified result, and lock provenance.
- **Source:** the unchanged workspace state and definition from which authoring opened; an empty source has no definition.
- **Definition draft:** saved authored values, issue list, source identity, and editing context in DEFINITION_DRAFT.
- **Working form:** unsaved edits, visibly distinguished from the durable draft.
- **Weekly requirement:** editor-level shared cohort, subject, defaults, and count represented by individually identified
  lessons in a series; it adds no new kernel lesson type.
- **Occurrence:** one lesson occupying one period, with one cohort and one preassigned teacher.
- **Exception:** an explicit occurrence override of a shared setting, including an override currently equal to that default.
- **Series:** lesson membership that requires a common subject and cohort and participates in the configured soft
  same-day preference; teachers and other occurrence settings may differ.
- **Seeded repair:** repair whose base successor is the finished authored definition, with the authored draft retained.
- **Persistent policy lock:** a lesson period/room lock authored as school policy.
- **Attempt-scoped pin:** existing repair protection of an accepted placement, kept separate from persistent policy.
- **Timing impact:** a changed weekday, order, start time, or end time experienced by a lesson, even with the same period ID.
- **Blocking issue:** a field, reference, or contract violation that prevents finishing.
- **Feasibility advisory:** an explanation of a necessary scheduling condition that fails; it does not prevent finishing.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Open or resume a school-definition draft | School timetable administrator | None |
| UC-2 | Set school identity | School timetable administrator | Requires UC-1 |
| UC-3 | Configure the weekly period grid | School timetable administrator | Requires UC-1 |
| UC-4 | Configure subjects | School timetable administrator | Requires UC-1 |
| UC-5 | Configure teachers | School timetable administrator | Requires UC-1 |
| UC-6 | Configure classes | School timetable administrator | Requires UC-1 |
| UC-7 | Configure rooms and capabilities | School timetable administrator | Requires UC-1 |
| UC-8 | Set weekly teaching requirements | School timetable administrator | Requires UC-1 |
| UC-9 | Customize occurrences and teaching responsibility | School timetable administrator | Requires UC-1 |
| UC-10 | Configure hard room-assignment policies | School timetable administrator | Requires UC-1 |
| UC-11 | Configure ordinary scheduling preferences | School timetable administrator | Requires UC-1 |
| UC-12 | Enable a newer scheduling catalog | School timetable administrator | Requires UC-1 |
| UC-13 | Remove an item and resolve its dependencies | School timetable administrator | Requires UC-1 |
| UC-14 | Restore source values or deleted source items | School timetable administrator | Requires UC-1 |
| UC-15 | Review definition changes, issues, and timetable impact | School timetable administrator | Requires UC-1 |
| UC-16 | Finish a verified school definition | School timetable administrator | Primary; requires UC-1, UC-15; includes UC-15 at step 1 |
| UC-17 | Obtain, review, and accept an authoring-seeded repair | School timetable administrator | Requires UC-16 |
| UC-18 | Abandon seeded repair and resume authoring | School timetable administrator | Requires UC-16 |
| UC-19 | Discard authoring and return to its source | School timetable administrator | Requires UC-1 |
| UC-20 | Find the supported route for a teacher change | School timetable administrator | None |

The relationships express available actor outcomes, not a prescribed screen order. An imported valid definition can
finish without using every editor. UC-16 is primary because its successful postcondition supplies the verified
definition that both first planning and subsequent repair need.

## State models

### Complete workspace lifecycle

Exactly these state values are recognized: EMPTY, INITIAL_DRAFT, SOLVING_INITIAL, INITIAL_PROPOSAL, ACCEPTED_BASELINE,
DEFINITION_DRAFT, REPAIR_DRAFT, SOLVING_REPAIR, REPAIR_PROPOSAL, and MANUAL_DRAFT.

Exactly these transition rows are allowed; every other transition is refused with an explanation and no change to
durable state, source, accepted definition/result/provenance, saved draft, or proposal.

| From | Event | To | Owner / effect |
|---|---|---|---|
| EMPTY | Open authoring | DEFINITION_DRAFT | UC-1; empty source, catalog 9 |
| INITIAL_DRAFT | Open authoring | DEFINITION_DRAFT | UC-1; preserve initial source |
| ACCEPTED_BASELINE | Open authoring | DEFINITION_DRAFT | UC-1; preserve accepted source |
| DEFINITION_DRAFT | Save an edit, grouped operation, or upgrade | DEFINITION_DRAFT | UC-2–UC-14; explicit save only |
| DEFINITION_DRAFT | Review, navigate, inspect Current, reload/restart, or resume | DEFINITION_DRAFT | UC-1, UC-15; no definition mutation |
| DEFINITION_DRAFT | Finish verification fails | DEFINITION_DRAFT | UC-16; content unchanged, diagnostics may refresh |
| DEFINITION_DRAFT | Finish from empty or initial source | INITIAL_DRAFT | UC-16; verified initial definition, no basedOnRevision |
| DEFINITION_DRAFT | Finish from accepted source | REPAIR_DRAFT | UC-16; verified successor and retained authoring |
| DEFINITION_DRAFT | Confirm discard | Recorded source state | UC-19; EMPTY, INITIAL_DRAFT, or ACCEPTED_BASELINE |
| REPAIR_DRAFT with retained authoring | Confirm abandonment of repair draft | DEFINITION_DRAFT | UC-18; remove staging/pins, restore authored content |
| REPAIR_DRAFT | Stage availability or pins | REPAIR_DRAFT | UC-17; preserve seed and Current |
| REPAIR_DRAFT | Request repair | SOLVING_REPAIR | UC-17; freeze exact seed and intent |
| SOLVING_REPAIR | Receive and verify eligible feasible result | REPAIR_PROPOSAL | UC-17; retain authored draft |
| SOLVING_REPAIR | Cancel, fail, return no feasible result, or recover after restart | REPAIR_DRAFT | UC-17; retain seed, staging, pins, and authoring |
| REPAIR_PROPOSAL | Revise/discard generated proposal, or invalidate stale output | REPAIR_DRAFT | UC-17; retain repair intent and authoring |
| REPAIR_PROPOSAL | Explicitly accept verified successor/result durably | ACCEPTED_BASELINE | UC-17; publish successor; close retained authoring |
| REPAIR_PROPOSAL | Acceptance storage failure | REPAIR_PROPOSAL | UC-17; unchanged eligible proposal for retry |
| Stable repair state | Reload/restart | Same stable repair state | UC-17; verify eligibility before acceptance |
| EMPTY | Import verified initial / accepted data | INITIAL_DRAFT / ACCEPTED_BASELINE | Existing import behavior |
| INITIAL_DRAFT | Replace verified initial definition | INITIAL_DRAFT | Existing behavior outside authoring |
| INITIAL_DRAFT | Request initial planning | SOLVING_INITIAL | Existing planning; UC-16 G5 exercises integration |
| SOLVING_INITIAL | Verified feasible result | INITIAL_PROPOSAL | Existing behavior |
| SOLVING_INITIAL | Cancel, fail, unsuccessful search, or restart | INITIAL_DRAFT | Existing behavior; finished definition retained |
| INITIAL_PROPOSAL | Explicit durable acceptance | ACCEPTED_BASELINE | Existing behavior; authoring context retained |
| INITIAL_PROPOSAL | Revise/discard proposal or invalidate stale output | INITIAL_DRAFT | Existing behavior |
| INITIAL_PROPOSAL | Acceptance storage failure | INITIAL_PROPOSAL | Existing behavior; no accepted timetable |
| ACCEPTED_BASELINE | Start ordinary repair / manual editing | REPAIR_DRAFT / MANUAL_DRAFT | Existing behavior |
| Ordinary REPAIR_DRAFT | Confirm discard | ACCEPTED_BASELINE | Existing behavior; no retained authoring |
| MANUAL_DRAFT | Edit/save a period or room, or reload/restart | MANUAL_DRAFT | Existing behavior; teacher routing is UC-20 |
| MANUAL_DRAFT | Publish verified changes or confirm discard | ACCEPTED_BASELINE | Existing behavior |
| Any state outside DEFINITION_DRAFT | Confirm existing clear / verified reset-import | EMPTY / INITIAL_DRAFT / ACCEPTED_BASELINE | Existing reset behavior; any superseded running output becomes ineligible |
| Any state with Current | Inspect/export Current | Same state | Read-only, exact accepted data |
| EMPTY, INITIAL_DRAFT, INITIAL_PROPOSAL, ACCEPTED_BASELINE, or MANUAL_DRAFT | Reload/restart | Same stable state | No automatic acceptance |

Verification failure while finishing leaves DEFINITION_DRAFT and its content intact; diagnostics may be refreshed and
saved. Refused state/version/access changes and failed durable writes leave the last committed document unchanged.
A saved definition draft survives restart without becoming a proposal or Current.

### Form and review state

A form begins Saved, becomes Unsaved when edited, and becomes Saved only after explicit successful save. Failed save
leaves the working values Unsaved and the last durable values intact. Leaving Unsaved work offers save, discard those
working edits, or stay. Save failure keeps the administrator in the form. Cancelled confirmations leave both forms and
durable data unchanged. Reload/crash restores only saved values; working edits are never labelled durable.

Previews and reviews identify the exact saved draft they describe. A subsequent save makes an older confirmation or
preview stale. A stale operation reports the change and requires a new review; it never overwrites newer work.

## UC-1 - Open or resume a school-definition draft

- Goal: Start or recover durable authoring with a clear source and available sections.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests Create school, Author definition, or Resume authoring.
- Preconditions: The local workspace is available.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests authoring from EMPTY, INITIAL_DRAFT, or ACCEPTED_BASELINE.
2. The system identifies the source, explains the single-draft lock, and, for an accepted catalog below 8, shows the
   required upgrade and the behavior-preserving adjustments in Catalog upgrade adjustments. For an accepted source,
   it also identifies prior attempt-scoped pins that expire in the working successor.
3. The administrator confirms any required upgrade.
4. The system opens and durably saves the definition draft, preserving the source. EMPTY starts with catalog 9,
   empty collections, and identity still to be entered; existing sources retain their data and supported catalog,
   except for the explained required upgrade and prior attempt-pin expiry.
5. The system presents freely navigable School, Grid, Subjects, Teachers, Classes, Rooms, Room policies, Weekly requirements, and Review sections,
   incomplete counts, source identity, saved status, and draft/Current labels. New creation starts in School; existing
   and resumed drafts start in Review. Teachers, Classes, Rooms, and Room policies have dedicated tabs; School exposes
   catalog and advanced Preferences; Weekly requirements exposes series, ungrouped lessons, and occurrence details.
6. The administrator enters a section or inspects the unchanged Current timetable.

### Extensions

- 1a. If DEFINITION_DRAFT is already open, the system restores its saved content and context without creating or
  resetting it; resume at step 5.
- 1b. If another draft, solve, or proposal is active, the system refuses authoring, identifies that state, and explains
  how to finish or abandon it first; end.
- 2a. If the source is EMPTY, the system offers creation alongside the existing import entry and describes the draft
  as a new school with no Current; resume at step 4.
- 2b. If the source contains an unsupported version, unknown field, or unmappable value, the system names the affected
  location and refuses authoring without transforming the source; end.
- 3a. If the required-upgrade confirmation is cancelled, the source state remains unchanged; end.
- 4a. If storage or a stale workspace prevents opening, the system reports that no draft was opened and retains the
  source state; end.
- 5a. If authoring is reopened after reload/restart, saved values, omissions, exceptions, catalog, source, and issues
  reappear; resume at step 6.

### Guarantees

- G1. Opening from an accepted source copies its definition, not its timetable assignments as lesson defaults.
  Initial sources omit basedOnRevision; accepted-source successors use the accepted result's inputRevision.
  Prior period/room locks marked attempt-scoped in accepted provenance are omitted from the working draft and
  identified in review as expired pins. Persistent source locks remain. The exact source definition/result and
  provenance remain unchanged; reopening the same draft never expires newly authored persistent locks.
- G2. Saved authoring preserves source state/revisions, every supported field and optional presence, weekly defaults
  and inheritance/exception intent, current occurrence identities, removed identities, and diagnostics. Values that
  cannot be represented are named and never silently dropped, rounded, or replaced.
- G3. While authoring is open, planning, repair, manual editing, import, clear, and replacement requests are refused
  without mutation and identify the authoring draft. Current inspection/export remains available and exact.
- G4. At 1600×900 and 1280×800 the draft identity, section navigation, form, and save/error status are visible without
  page-level horizontal scroll or clipped controls. At 390×844 and all widths at most 700 CSS px, saved content/issues
  are read-only and mutation controls are absent; the system explains that editing requires a wider view. Resizing
  never saves or discards working edits; widening restores their unsaved form.
- G5. Controls and explanations use English; administrator-entered names remain in their original language, including
  diacritics. Names render as literal text. No new language selector or role-dependent screen is introduced.
- G6. Existing same-machine access applies. Requests outside the permitted local access boundary reveal no school or
  draft data; untrusted change requests are refused without mutation. No accounts or outbound data service is added.
- G7. Convergence for this UI goal exercises isolated real-browser entry/refusal/reload/restart and durable source
  fidelity. After the committed checkpoint and passing technical convergence, the agent performs the administrator
  journey through the real browser and records expected/observed results, screenshots and durable-state evidence.
  Passing walkthrough evidence permits automatic approval without a separate administrator confirmation. The same
  UI evidence boundary applies to every UC below: successful and refused paths are observed, not inferred from source
  alone. Missing evidence or a failed walkthrough blocks approval; isolation and behavioral guarantees remain intact.

### Postconditions

- Success: DEFINITION_DRAFT is durable and navigable, or the existing saved draft is resumed unchanged.
- Minimal guarantee: A refused or failed opening leaves source and Current unchanged and creates no partial draft.

## UC-2 - Set school identity

- Goal: Name the school and choose its stable identity.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator opens School and requests a name or new identity change.
- Preconditions: An authoring draft is open.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator enters or edits the display name and, for a new school before its first identity save, reviews
   the readable identifier suggestion or supplies an advanced override.
2. The system shows the working values, current catalog, and saved/unsaved status.
3. The administrator explicitly saves.
4. The system saves the display name and chosen stable identifier, shows all resulting issues, and marks the form Saved.

### Extensions

- 1a. If an identifier is already established, it is read-only; a rename request is refused without mutation; end.
- 3a. If a new identifier is invalid or collides under Identifier rules, the system explains the conflict and retains
  the unsaved form; resume at step 1.
- 3b. If supported non-identity values are incomplete or invalid, the system saves them and reports blocking issues;
  resume at step 4.
- 3c. If access, state, or version changed, or saving fails, the system reports no committed change, retains working
  values for retry, and leaves the durable draft and source unchanged; end.

### Guarantees

- G1. Root fields follow School fields. Display-name changes do not regenerate identity or change source metadata.
- G2. Schema/revision fields are system-managed; catalog changes use UC-12. Optional values and text are preserved.
- G3. Save is atomic, explicitly requested, and subject to UC-1's access and presentation boundaries. A failure never
  changes Current or falsely marks the form Saved.

### Postconditions

- Success: The school identity/name is saved in the draft and visible with its draft label.
- Minimal guarantee: The last saved identity and source remain intact; unsaved work stays distinguishable.

## UC-3 - Configure the weekly period grid

- Goal: Establish or adjust teaching slots, bell times, and reservations.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests grid generation or edits a period/reservation.
- Preconditions: An authoring draft is open.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. For an empty grid, the administrator chooses weekdays, counts per day, and optional bell times; for an existing
   grid, the administrator requests additions or edits directly.
2. The system previews explicit period rows with identifiers, names, weekday/order/times, reservations, and the effect
   on all-period versus selected availability.
3. The administrator adjusts individual rows, including shorter days, and explicitly saves the selected operation.
4. The system saves all selected rows together, retains existing IDs, reports issues, and shows any timing impacts.

### Extensions

- 1a. If a grid already exists, generation cannot silently replace it; the system offers additions and directs removal
  through UC-13; resume at step 2.
- 2a. If values violate Period fields or duplicate weekday/order or overlap times, the system identifies every affected
  row; the supported working definition may be saved with blocking issues; resume at step 3.
- 3a. If the administrator cancels the preview, no durable grid change occurs; end.
- 3b. If a deletion is requested, continue with UC-13.
- 3c. If save/guard/version checks fail, no part of the operation is committed, working rows remain available, and the
  system explains how to retry or reload the newer draft; end.

### Guarantees

- G1. Exactly the declared weekdays/periods are represented; bell times are optional paired values. Reservations are
  explicit membership, never inferred from names, IDs, order, or clock times.
- G2. Adding a slot includes it in omitted all-period availability and excludes it from explicit selections. The
  preview names affected resources. Reservations and availability remain distinct.
- G3. Weekday/order/start/end edits keep period identity. Review records old/new values and all source and proposed
  lessons affected, even without a kernel period-ID move. Untouched periods and optional time representations persist.
- G4. UI saves obey UC-1 G3–G7; invalid grids are labelled unfinishable, and failed/cancelled operations leave saved
  grid, source, and Current unchanged.

### Postconditions

- Success: The saved declared grid/reservations reflect the selected edits, with validation and timing impact visible.
- Minimal guarantee: No partial generation, replacement, or removal is committed.

## UC-4 - Configure subjects

- Goal: Add or maintain subjects and their supported placement constraints.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests a subject addition or edit.
- Preconditions: An authoring draft is open.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator enters a subject name and reviews its new identifier, or selects an existing subject.
2. The system presents every supported Subject fields control, distinguishing defaults, values, and unavailable
   catalog features.
3. The administrator changes ordinary or advanced settings and explicitly saves.
4. The system persists the subject and reports its issues and effects on lessons, qualifications, or room policies.

### Extensions

- 2a. If a setting requires a newer catalog, the system names that version and offers UC-12; end.
- 3a. If supported fields are incomplete or invalid, the system saves the work and shows blocking issues, including a
  reserved-weekly cap without permission to use reserved periods; resume at step 4.
- 3b. If removal or a source revert is requested, continue with UC-13 or UC-14 respectively.
- 3c. If identity, access/state/version, or storage checks fail, no change is committed and the working form remains
  available; end.

### Guarantees

- G1. Subject fields and default/omission distinctions are preserved; curator subjects and reserved/day-edge/daily
  rules are explained as supported hard rules. Editing a subject never implicitly changes qualifications or lessons.
- G2. Curator lessons use the cohort's curator/home-room obligations; the general qualification check respects the
  kernel's curator qualification exception. Violations remain explicit blockers.
- G3. Saves obey UC-1's presentation, access, fidelity, and evidence boundaries; source and Current remain unchanged.

### Postconditions

- Success: The subject's supported settings are durably saved and their consequences are visible.
- Minimal guarantee: No implicit reassignment, cascaded edit, or partial save occurs.

## UC-5 - Configure teachers

- Goal: Add or maintain teaching staff, qualifications, and weekly availability/preferences.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests a teacher addition or edit.
- Preconditions: An authoring draft is open.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator creates a teacher or selects an existing one.
2. The system shows Teacher fields, subject references, and the declared grid with reservations marked.
3. The administrator sets qualifications, all-period or selected availability, and undesirable periods, then saves.
4. The system persists the selected values and shows issues, including lessons affected by qualification changes.

### Extensions

- 3a. If supported values are invalid or references unresolved, save retains the work and reports every related
  blocking issue; resume at step 4.
- 3b. If teaching is to be reassigned, continue with UC-9; qualifications are not inferred or added automatically.
- 3c. If removal/revert is requested, continue with UC-13/UC-14 respectively.
- 3d. If identity, access/state/version, or storage checks fail, no change is committed and unsaved work remains
  available; end.

### Guarantees

- G1. All Teacher fields and omission/explicit-empty distinctions persist. Qualified-subject selections can be empty;
  that does not qualify the teacher for ordinary lessons.
- G2. Availability is evaluated on explicit grid IDs and remains distinct from reservation and undesirability.
  Unrelated teacher, lesson, and policy values are never changed by this save.
- G3. Source and Current remain exact. UI access, narrow read-only behavior, literal names, and convergence follow UC-1.

### Postconditions

- Success: The teacher's saved qualifications and availability/preferences are visible with complete issues.
- Minimal guarantee: No teacher reassignment, silent qualification grant, or partial save occurs.

## UC-6 - Configure classes

- Goal: Add or maintain class size, availability, daily constraints, curator, and home room.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests a class addition or edit.
- Preconditions: An authoring draft is open.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator creates a class or selects an existing one.
2. The system presents Cohort fields and their effective defaults, supported versions, teacher/room links, and grid.
3. The administrator changes selected values, using explicit default/value/no-limit choices where supported, and saves.
4. The system saves the class and shows affected lessons, validation, and feasibility advisories.

### Extensions

- 2a. If a setting requires a catalog upgrade, the system names that version and offers UC-12; end.
- 3a. If values or links are incomplete/invalid, the system saves them with blockers and linked lesson/policy issues;
  resume at step 4.
- 3b. If removal/revert is requested, continue with UC-13/UC-14 respectively.
- 3c. If identity, access/state/version, or storage checks fail, the saved class is unchanged and working edits remain;
  end.

### Guarantees

- G1. Every Cohort fields attribute persists, including omitted versus explicit numeric values. Default gap allowance
  is shown as zero in catalogs 5–9; “No limit” is an explicit unrestricted value, not an empty field.
- G2. Changing curator/home room never silently rewrites lessons or rules. Curator obligations and matched home-room
  policies are checked. The editor explains the home-room preference fallback for other eligible lessons.
- G3. Source/Current remain unchanged; saves and presentation obey UC-1's fidelity, access, and evidence guarantees.

### Postconditions

- Success: The class's supported settings are saved, with effective values and consequences visible.
- Minimal guarantee: No accepted assignment or dependent record changes implicitly.

## UC-7 - Configure rooms and capabilities

- Goal: Add or maintain rooms, capacity, capabilities, and weekly availability.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests a room addition or edit.
- Preconditions: An authoring draft is open.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator creates a room or selects an existing one.
2. The system presents Room fields, all-period/selected availability, and existing capability tokens from the school.
3. The administrator changes capacity, selects or enters capability tokens, adjusts availability, and explicitly saves.
4. The system saves the values and reports affected home-room links, lessons, and room-policy issues/advisories.

### Extensions

- 3a. If supported values are incomplete or invalid, the system saves them with issues; resume at step 4.
- 3b. If removing the last room supplying a required capability makes scheduling impossible, the system identifies
  the affected lessons as an advisory, or as a blocker where the definition's explicit policy/lock is invalid;
  resume at step 4.
- 3c. If removal/revert is requested, continue with UC-13/UC-14 respectively.
- 3d. If identity, access/state/version, or storage checks fail, the saved room is unchanged and working edits remain;
  end.

### Guarantees

- G1. All Room fields persist, including capacity zero, empty capabilities, and omitted/explicit availability.
- G2. Capability tokens are open contract identifiers, not references to an invented central catalog. Extra valid
  tokens are allowed; matching is by exact token. Removing one from a room never silently edits lesson requirements.
- G3. Saves preserve source/Current and follow UC-1's access, presentation, and real-browser evidence boundaries.

### Postconditions

- Success: Room settings are saved and their scheduling implications are visible.
- Minimal guarantee: No implicit lesson, home-room, or policy modification occurs.

## UC-8 - Set weekly teaching requirements

- Goal: Establish or change the number of weekly lessons for a cohort and subject.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator creates a requirement or changes its count/shared settings.
- Preconditions: An authoring draft is open; referenced items may still need completion.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator selects cohort and subject, common teacher/settings, and a nonnegative integer weekly count.
2. The system previews the requirement, its series, individual occurrences, inherited values, and explicit exceptions.
3. The administrator chooses which occurrences to remove when decreasing the count, or reviews additions when
   increasing it, and chooses the scope of any shared-setting change.
4. The system previews additions, selected cancellations, updates following shared defaults, and preserved exceptions,
   including current accepted placements where available.
5. The administrator explicitly saves the operation.
6. The system saves the complete operation atomically, reports issues, and shows the exact resulting count/occurrences.

### Extensions

- 2a. If the requirement came from imported data, existing series/IDs/names remain intact and ungrouped lessons remain
  separate; resume at step 3.
- 2b. If an imported shared field is mixed, the system shows Mixed. Establishing a default requires an explicit choice
  of which occurrences follow it; no majority or first occurrence is guessed; resume at step 3.
- 3a. If reducing the count, the selected removals must equal the reduction. Until that selection is complete, the
  system refuses the operation save, explains the missing choices, and preserves the working preview; resume at step 3.
- 3b. If the administrator chooses Apply to all, the system explicitly lists overwritten exceptions for confirmation;
  resume at step 4.
- 3c. If an occurrence needs a separate edit, continue with UC-9.
- 5a. If supported data is incomplete or violates definition rules, it is saved with blocking issues. A count that
  cannot be interpreted as a nonnegative integer cannot be expanded into occurrences; its entered value remains
  saved as incomplete requirement work with a blocker, and existing occurrences remain unchanged; resume at step 6.
- 5b. If the preview is cancelled/stale, identity/guard checks fail, or persistence fails, no partial occurrence change
  is committed; the system reports the cause and retains working choices for re-review; end.

### Guarantees

- G1. Remaining occurrence IDs/names/constraints are not regenerated or renumbered by count, teacher, or subject-name
  changes. Additions receive fresh unique IDs under Identifier rules; removed IDs are not silently reused. Decreasing
  then increasing a count does not resurrect removed constraints; source occurrences can be restored through UC-14.
  Removed newly added occurrences have no source-restoration path.
- G2. Only equal imported values can become inferred shared defaults. Explicit exception intent remains durable even
  when an exception equals a shared value. Ordinary shared edits affect followers; explicit Apply to all replaces the
  named exceptions. New occurrences follow the requirement's current defaults.
- G3. The requirement stores one shared cohort/subject. New requirements form series and use the configured
  soft.series-same-day preference; this is neither a hard one-per-day rule nor adjacency. Extra distinct requirements
  for the same cohort/subject are allowed. A zero-count requirement may remain as editing context and contributes no
  kernel lesson; incomplete context is visibly blocking until completed or explicitly removed.
- G4. Required room capabilities, preferences, undesirability, and policy locks can be common defaults or exceptions.
  Every generated occurrence preserves the resulting Lesson fields. Shared locks that conflict remain visible
  blockers; the editor never weakens them to make a count fit.
- G5. Shared-setting/exception intent survives reload/restart, finish, and acceptance in this local workspace, bound to
  its matching definition. External imports lacking that intent use the conservative inference in G2. The finished
  definition contains only native contract fields; no requirement metadata is disguised as an extra kernel field.
- G6. Saves obey UC-1's access/presentation/evidence boundaries and preserve source/Current. Partial generation or
  partial application is never labelled Saved.

### Postconditions

- Success: The saved requirement and occurrence count/settings reflect exactly the selected operation, or incomplete
  requirement work is durable with a clear blocker.
- Minimal guarantee: Existing occurrences and Current survive a failed or refused operation unchanged.

## UC-9 - Customize occurrences and teaching responsibility

- Goal: Set individual lesson details, regroup lessons deliberately, or transfer selected teaching responsibilities.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests an occurrence edit, explicit grouping, or selected teacher reassignment.
- Preconditions: An authoring draft is open and the selected occurrences exist.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator selects an occurrence or selected lessons to reassign.
2. The system shows every Lesson fields value, inherited/exception status, current source placement, and relevant
   teacher qualifications, policy locks, and teacher-scoped room rules.
3. The administrator chooses occurrence values or inheritance, or selects a replacement teacher and explicitly
   chooses any teacher-scoped room rules to transfer with the lessons.
4. The system previews the exact selected lesson/rule changes, exceptions created or cleared, and validation impact.
5. The administrator saves.
6. The system persists the changes together, preserving lesson identities, and shows the resulting details/issues.

### Extensions

- 1a. If the administrator groups ungrouped lessons or moves memberships, the system requires a common cohort/subject,
  offers an existing compatible series or a new stable series identity, and explains the spreading preference;
  resume at step 4.
- 1b. If grouping choices are incompatible or identity is invalid, the system refuses grouping without changing any
  membership and explains the conflict; end.
- 3a. If a setting differs from the common value, or is explicitly kept equal as an override, it is marked an exception;
  if Follow shared setting is chosen, that exception is cleared explicitly; resume at step 4.
- 3b. If a teacher is unqualified or other supported values are invalid, save retains the selected edits with blockers
  and names the affected lessons/rules; resume at step 6.
- 3c. If deleting an occurrence is requested, continue with UC-13.
- 5a. If confirmation is cancelled/stale or a guard/write fails, no selected subset is committed, working values stay
  available, and the system explains the refusal; end.

### Guarantees

- G1. Reassignment changes only the explicitly selected teacher references; unrelated cohorts, subjects, placements,
  qualifications, lessons, and room policies retain their values. No qualification is granted implicitly.
- G2. Imported occurrences without series are fully editable. Existing mixed-teacher series remain supported. Removing
  membership does not delete the lesson; assigning another series changes membership, not lesson identity.
- G3. Only group-wide operations change a requirement's shared cohort/subject; moving an individual to another
  cohort/subject detaches it from an incompatible series or requires explicit compatible regrouping in the preview.
  No invalid hidden grouping is emitted.
- G4. Every supported occurrence value and optional presence is preserved. The editor explains cohort home-room
  preference fallback: omitted and explicitly empty preferred rooms both use that fallback when applicable.
  Saving a period/room lock explicitly as policy establishes persistent provenance, including when its value equals
  an expired prior pin. A source revert does not establish that policy implicitly.
- G5. Source/Current and unrelated values remain intact; presentation, access, save failure, and evidence follow UC-1.

### Postconditions

- Success: The selected occurrence details, memberships, or teacher/rule references are saved exactly as reviewed.
- Minimal guarantee: No implicit teacher, qualification, series, or policy transfer occurs.

## UC-10 - Configure hard room-assignment policies

- Goal: Require particular rooms or cohort home rooms for a subject and optional teacher scope.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator adds or edits a room policy.
- Preconditions: An authoring draft is open.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator chooses a policy identity, subject, and optional teacher scope.
2. The system offers the Room-assignment fields targets: a nonempty allowed-room set or Use cohort home room.
3. The administrator chooses exactly one target and saves.
4. The system saves the policy and shows matched lessons, effective intersections, and related definition issues.

### Extensions

- 1a. If catalog is below 9, the system shows the setting as unavailable and offers UC-12; end.
- 3a. If supported values are incomplete/invalid, the system saves the work with blockers, including missing target,
  conflicting targets, unknown references, or incompatible matched lesson/home-room/lock requirements; resume at step 4.
- 3b. If removal/revert is requested, continue with UC-13/UC-14 respectively.
- 3c. If identity, state/access/version, or storage checks fail, no saved policy changes; end.

### Guarantees

- G1. Policies are hard requirements, not weighted preferences. All matching policies intersect; none overrides
  another or relaxes room capacity/capability/availability, a room lock, or curator home-room obligations.
- G2. Exact IDs, subject/teacher selectors, target choice, and room sets survive saving, finishing, repair, acceptance,
  and export of the resulting definition. Unmapped values are surfaced, not dropped.
- G3. Policy edits never infer mandates from source timetable repetition or automatically choose a missing home room.
  Source/Current are unchanged and UC-1's UI/access/evidence boundaries apply.

### Postconditions

- Success: The selected policy is saved with its matched population and every blocking issue visible.
- Minimal guarantee: No inferred mandate or partial target replacement is committed.

## UC-11 - Configure ordinary scheduling preferences

- Goal: Choose the strength of supported ordinary preferences.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator changes a preference weight.
- Preconditions: An authoring draft is open.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator reviews the Preference weights table for the draft catalog.
2. The system shows each effective weight, whether it is a catalog default or explicit override, and its meaning.
3. The administrator chooses the default or an explicit integer weight and saves.
4. The system persists the choice, shows the effective value and validation, and includes it in change review.

### Extensions

- 1a. If a preference belongs to a newer catalog, it is unavailable and UC-12 is offered; end.
- 3a. If a supported weight is invalid or the possible weighted score exceeds the supported range, save retains the
  entered work with a blocking issue; resume at step 4.
- 3b. If an unknown preference or hard/stability weight is requested, the system refuses the unsupported setting
  without changing weights and identifies the supported choices; end.
- 3c. If reverting is requested, continue with UC-14.
- 3d. If an access/state/version or write failure occurs, saved weights remain unchanged; end.

### Guarantees

- G1. Exactly the ordinary IDs in Preference weights are configurable, once each; default choice omits the override.
  Explicit zero disables that preference and remains distinguishable from default weight one.
- G2. Hard feasibility and period/room stability ordering cannot be changed by these controls.
- G3. Weight choice survives all draft/finish/repair paths without default materialization unless explicitly required
  by an upgrade. Source/Current and guarded UI behavior follow UC-1.

### Postconditions

- Success: Saved ordinary preference choices and effective weights are visible.
- Minimal guarantee: No hard/stability policy or unrelated weight changes.

## UC-12 - Enable a newer scheduling catalog

- Goal: Enable newer supported settings while preserving existing effective scheduling policy.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests Upgrade catalog or follows an unavailable-setting explanation.
- Preconditions: An authoring draft is open below catalog 9.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests the latest supported catalog.
2. The system previews current/target catalogs, newly enabled settings, changed omitted defaults, and every explicit
   compensating value from Catalog upgrade adjustments.
3. The administrator confirms the upgrade.
4. The system saves catalog 9 and the compensating values together, preserves existing explicit settings, recomputes
   issues, and shows the newly available controls.

### Extensions

- 1a. If already at catalog 9, the system explains that no upgrade is available and changes nothing; end.
- 1b. If an unsupported target or downgrade is requested, the system refuses without mutation; end.
- 2a. If the preview is stale, the system requires a refreshed preview of the current saved draft; resume at step 2.
- 3a. If the administrator cancels, draft catalog and values remain unchanged; end.
- 3b. If saving fails or access/state/version changed, no portion of the upgrade is committed; end.
- 4a. If the administrator subsequently chooses a newer default deliberately, that edit uses the corresponding
  field/preference UC and appears separately in review; end.

### Guarantees

- G1. Existing effective constraints and weights are preserved by the exact adjustment table. Already explicit values
  are not overwritten. Upgrading changes catalog/revision identity, not Current; it does not promise identical solver
  output.
- G2. No partial catalog-only write occurs. Required accepted-source upgrades when opening use the same preservation
  table and confirmation; cancelling those leaves the source state unchanged.
- G3. Omission remains preserved for unaffected fields. Unsupported fields stay unavailable until a completed upgrade.
  Source/Current remain intact and UC-1's UI/access/evidence boundary applies.

### Postconditions

- Success: The newer catalog and its behavior-preserving settings are durable and reviewable.
- Minimal guarantee: Failed/cancelled upgrades leave the prior catalog and saved values intact.

## UC-13 - Remove an item and resolve its dependencies

- Goal: Remove school data without leaving references to the removed identity or silently cancelling teaching.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests removal of a subject, resource, period, policy, occurrence, or requirement.
- Preconditions: An authoring draft is open and the selected item exists.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator identifies the item to remove.
2. The system shows every incoming dependency from Dependency references, owned membership affected, and historical
   Current placements separately.
3. The administrator explicitly chooses each replacement/removal of a dependent setting and any occurrences to cancel.
4. The system previews all resulting edits/cancellations, refuses unresolved dependencies, and names new issues.
5. The administrator confirms and saves the complete removal operation.
6. The system saves the chosen resolutions/removal together and updates issues and change review.

### Extensions

- 2a. If no dependencies exist, the system still previews the single removal for confirmation; resume at step 5.
- 3a. If a replacement is missing, unknown, or another dependency would still reference the removed target, the system
  refuses applying the operation and explains each unresolved dependency; resume at step 3.
- 3b. If removing a requirement, every removed occurrence must be explicitly selected, with placements/constraints
  shown; resume at step 4.
- 4a. If unrelated or newly exposed definition issues remain, the system identifies them; once references to the
  removed target are resolved, the operation can still save an invalid draft; resume at step 5.
- 5a. If cancelled, stale, guarded, or failed in storage, no chosen replacement, cancellation, or removal is committed;
  end.

### Guarantees

- G1. No implicit cascade occurs. Root reservation membership, explicit availability/preferences, qualifications,
  links, locks, lessons, and room rules require the explicit choices shown in the preview.
- G2. Current's historical references do not block successor removal and remain unchanged. Removed identities stay
  reserved within the lineage; only UC-14 restores the same source identity. Newly added items removed from the draft
  have no restoration history and any replacement receives a fresh identity.
- G3. Capability tokens have no central referential target; changing a room's tokens is UC-7 and may expose feasibility
  issues, rather than silently deleting requirements elsewhere.
- G4. The grouped operation preserves every unselected field and respects UC-1's presentation/access/evidence boundary.

### Postconditions

- Success: The selected item is removed, every incoming reference is resolved as chosen, and cancellations are visible.
- Minimal guarantee: No partial resolution or removal is committed.

## UC-14 - Restore source values or deleted source items

- Goal: Recover known source data without abandoning other authored changes.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests a source field/item revert or restoration.
- Preconditions: An authoring draft is open and the selected source value/item exists.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator chooses a source attribute, whole source item, or deleted source item.
2. The system previews the restored values/identity and effects on dependent lessons/settings, using effective source
   meaning where the draft catalog is newer.
3. The administrator explicitly saves the restoration.
4. The system restores exactly the selected scope, retains unrelated changes, and recomputes issues/review.

### Extensions

- 1a. If the item was newly added and has no source value, the system explains that it can be removed with UC-13;
  end.
- 1b. If the source is EMPTY, the system explains that no source definition exists and offers UC-19 for whole-draft
  abandonment; end.
- 2a. If restoration exposes contradictory order, links, or other supported values, the preview identifies them and
  the restoration saves with blockers rather than silently adjusting other items; resume at step 3.
- 3a. If cancelled, stale, guarded, or failed in storage, no selected restoration is committed; end.

### Guarantees

- G1. Original identifiers, field presence, names, memberships, and values return for the selected scope; unselected
  reassignments, cancellations, additions, and settings remain unchanged. Restoring a source occurrence restores its
  identity and constraints explicitly, subject to the source-provenance and catalog preservation in G2.
- G2. The enabled catalog is retained. A source omission with different effective meaning is represented explicitly
  under that catalog using Catalog upgrade adjustments. Review distinguishes this representation from a user-chosen
  new restriction. Workspace-managed lineage is not reverted. Source lock provenance is preserved: prior
  attempt-scoped pins remain expired in the working successor, with their source values and expiry explained in the
  preview. Restoring a source item or lock never promotes a temporary pin to persistent policy; the administrator
  must explicitly author a persistent lock through UC-8/UC-9 to retain that restriction for the successor.
- G3. Restoring an item does not automatically resurrect removed dependencies or reverse a whole guided-removal
  operation; resulting issues identify what must be resolved. There is no persistent operation undo/redo.
- G4. Access/presentation/evidence follow UC-1; source/Current remain exact and storage failure leaves the draft intact.

### Postconditions

- Success: Selected source values/items are restored and their effective meaning/issues are visible.
- Minimal guarantee: Unrelated authored changes and the source survive refusal or failure unchanged.

## UC-15 - Review definition changes, issues, and timetable impact

- Goal: Understand the exact authored changes and whether the definition is ready to finish.
- Primary actor: School timetable administrator
- Supporting actors: none
- Trigger: The administrator opens Review or follows an issue/change link.
- Preconditions: An authoring draft is open.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests review of saved work.
2. The system presents the source/current draft identity, catalog, saved revision, and additions, changes, removals,
   cancellations, memberships, common-setting edits, exceptions, policies, preference/upgrade changes, and prior
   attempt-pin expiry with its source provenance.
3. The system shows all current blockers and necessary-condition advisories, with affected fields/items, related
   lessons, and the causal edit when known.
4. For an accepted source, the system shows source and potential successor placements/timing effects separately,
   including every lesson affected by period weekday/order/time edits.
5. The administrator follows a named item/issue to its editor or proceeds to finish after reviewing the saved work.

### Extensions

- 1a. If unsaved working values exist, the system labels them Unsaved and offers save, discard working edits, or stay;
  saved review is not represented as including unsaved work; resume at step 1.
- 3a. If there are zero lessons, the system explicitly states “No lessons configured; planning will produce an empty
  timetable”; resume at step 4.
- 3b. If no blockers remain, the system states that definition verification is still required and does not claim a
  feasible timetable; resume at step 4.
- 4a. If no accepted source exists, no placement is invented; new data is reviewed against the empty/initial source;
  resume at step 5.
- 5a. If a linked item was removed or the view became stale, the system names the change and refreshes the saved review
  rather than opening an unrelated record; resume at step 2.

### Guarantees

- G1. Issues follow Validation meanings. The same saved draft yields the same ordered issue population across reload
  and restart: blockers before advisories, then owner type, identifier, field, and message. Lists/counts are complete
  regardless of filtering, collapsed sections, or viewport; narrowed views show their scope and total.
- G2. Issue records preserve severity, field location, owner identity/type, explanation, related identities, and known
  causal edit. Multiple affected lessons remain individually reachable; unmappable references display their literal
  ID and issue instead of disappearing from selectors/review.
- G3. Timing-impact rows show lesson ID/name and old/new weekday, order, and supplied times, including added/cancelled
  one-sided placements. They are visibly distinct from period-ID and room moves; no changed teaching time is labelled
  Unchanged solely because an identifier matched.
- G4. Review/navigation/filtering/collapse and Current inspection make no durable changes. Draft and Current are
  explicitly labelled and never substituted for one another. Desktop/narrow layout and English/literal-text rules
  are UC-1 G4–G5; narrow views remain read-only.
- G5. Existing accepted bundles remain exact. UI convergence observes issue attribution, source comparison, timing
  changes without ID moves, narrowed/empty views, and agent-run walkthrough under UC-1 G7.

### Postconditions

- Success: The administrator sees a complete, revision-bound saved review and can locate every change/issue.
- Minimal guarantee: Reading, failed navigation, or stale review never changes saved data or Current.

## UC-16 - Finish a verified school definition (primary)

- Goal: Supply a complete verified definition to first planning or to repair without prematurely changing Current.
- Primary actor: School timetable administrator
- Supporting actors: School Kernel; Local durable workspace
- Trigger: The administrator requests Finish from the saved review.
- Preconditions: An authoring draft and its current saved review exist; working edits are saved or explicitly discarded.
- Relations:
  - Requires: UC-1 supplies the draft; UC-15 supplies its saved review
  - Includes: UC-15 at step 1, to review the exact candidate being finished
  - Extends: none

### Main success scenario

1. The system performs UC-15 for the exact saved candidate, showing issues, source changes, and any zero-lesson outcome.
2. The administrator confirms finishing that candidate.
3. The system verifies that there are zero blocking workspace issues and obtains authoritative kernel definition
   verification of the complete initial or successor definition.
4. The system durably finishes in one transition: empty/initial source becomes INITIAL_DRAFT; accepted source becomes
   REPAIR_DRAFT seeded with the verified successor and a retained copy of the authored draft.
5. The system shows the resulting initial planning or seeded repair state and its verified definition identity.
6. The administrator can request existing initial planning or continue with UC-17 for repair.

### Extensions

- 1a. If working edits remain, the system offers save, discard working edits, or stay; no finish occurs; resume at step 1.
- 2a. If confirmation is cancelled, the draft remains open and unchanged; end.
- 3a. If blockers exist, the system lists them and their editors, retains the entire draft, and refuses finish; end.
- 3b. If kernel verification rejects the candidate, the system displays every returned issue and affected location,
  preserves definition content, and may durably refresh only diagnostics in DEFINITION_DRAFT; end.
- 3c. If the kernel is unavailable or produces incomplete/unmappable verification evidence, the system reports that
  verification did not complete and retains the draft without claiming success; end.
- 3d. If known feasibility advisories or zero lessons are present, the system keeps them visible and allows the
  reviewed valid candidate to proceed; resume at step 4.
- 4a. If source/version/review eligibility changed or saving the transition fails, the system reports that finish was
  not committed and keeps the last saved authoring/source state intact; end.

### Guarantees

- G1. Initial candidates have schemaVersion 1 and no basedOnRevision. Accepted-source successors retain schoolId and
  bind basedOnRevision to the source accepted result's inputRevision, not timetableRevision.
- G2. Every authored field in School fields and the entity tables, plus its supported optional presence/value,
  survives finish; system-managed lineage follows G1.
  Weekly editing context is retained for subsequent authoring but is not emitted as extra kernel fields. Unresolved
  text inputs or unknown/unmappable values block finish rather than being dropped/coerced.
- G3. Finish never solves or publishes a timetable. All post-baseline changes, including names and zero-movement
  changes, go through seeded repair. The source accepted definition/result/provenance and Current metadata remain
  exact until explicit acceptance.
- G4. The transition is atomic; failed verification/storage leaves no partial initial definition or repair seed.
  Retained authoring is the exact completed draft, including exception intent, issues, source and identifiers.
- G5. Convergence includes the exact Small-school acceptance data through real forms, reload of partial saved work,
  initial grid generation followed by individual period adjustment, packaged kernel definition verification, initial
  planning and result verification, and explicit first acceptance.
  The administrator identifies the draft/proposal/Current distinction; the nine configured lessons are represented
  once each in the accepted timetable. No JSON preparation or command invocation is required of the administrator.
- G6. Validation uses isolated workspace state and does not alter the operator's runtime school. Evidence covers
  initial/accepted sources, invalid and zero-lesson candidates, verification failure, stale confirmation and failed
  storage, with exact source/Current preservation. UI approval needs the passing agent-run walkthrough in UC-1 G7.

### Postconditions

- Success: A verified initial definition is available for first planning, or a seeded repair draft and exact retained
  authoring exist while the prior baseline remains Current.
- Minimal guarantee: Unsuccessful finish retains all saved authoring and source data and publishes no timetable.

## UC-17 - Obtain, review, and accept an authoring-seeded repair

- Goal: Produce and decide a successor timetable for authored school changes.
- Primary actor: School timetable administrator
- Supporting actors: School Kernel; Local durable workspace
- Trigger: The administrator continues from the seeded repair produced by finishing authoring.
- Preconditions: REPAIR_DRAFT contains a verified authored seed, retained authoring, and its unchanged accepted source.
- Relations:
  - Requires: UC-16, whose accepted-source success postcondition supplies seeded repair
  - Includes: none
  - Extends: none

### Main success scenario

1. The system shows Current, authored changes, the successor grid/resources, and any additional repair intent.
2. The administrator optionally stages weekly teacher/room unavailability and pins surviving accepted placements.
3. The administrator requests repair; the system freezes the seed, intent, pins, source and run identity and shows
   SOLVING_REPAIR with status/cancellation while Current remains inspectable.
4. The kernel produces a complete feasible result, and the system independently verifies and presents an eligible
   REPAIR_PROPOSAL bound to that exact successor/source.
5. The administrator reviews combined definition and timetable changes, direct effects, ripple effects, additions,
   cancellations, teacher changes, room changes, and same-ID timing impacts.
6. The administrator explicitly confirms acceptance.
7. The system re-verifies eligibility and the definition/result pair and durably publishes the whole successor as
   ACCEPTED_BASELINE, updates Current identity/display metadata, and closes repair and retained authoring.

### Extensions

- 2a. If additional intent references a removed entity/period, contradicts a persistent lock, or tries to pin a new
  lesson with no accepted placement, the system explains the conflict and refuses that intent without changing the
  seed or existing staging; resume at step 2.
- 2b. If the administrator wishes to revise the authored definition, continue with UC-18.
- 3a. If conflicting intent prevents planning, the system lists the conflicts and remains in REPAIR_DRAFT;
  resume at step 2.
- 3b. If planning is cancelled, fails, stops without a feasible result, or is interrupted by restart, the system
  returns to REPAIR_DRAFT, preserves seed/staging/pins/authoring, and shows the actual outcome without a proposal;
  resume at step 2.
- 4a. If output is invalid, partial, unmappable, or stale, the system rejects it and returns to REPAIR_DRAFT without
  changing Current; resume at step 2.
- 5a. If the administrator chooses Revise, the proposal loses eligibility and REPAIR_DRAFT retains its intent;
  resume at step 2.
- 5b. If the administrator discards only the generated proposal, the system returns to REPAIR_DRAFT with seed,
  staging, pins, and retained authoring intact; end.
- 6a. If acceptance is cancelled, the eligible proposal remains reviewable and Current unchanged; end.
- 7a. If the source, intent, result, or verification identity no longer matches, the system rejects acceptance,
  invalidates the stale proposal, and returns to REPAIR_DRAFT with its saved work intact; end.
- 7b. If verification cannot complete, the system does not accept and preserves Current and reviewable saved work;
  end.
- 7c. If durable acceptance fails, Current remains exact and the unchanged eligible proposal remains for explicit
  retry in REPAIR_PROPOSAL; end.

### Guarantees

- G1. Repair compiles from the complete authored seed, not from an unmodified source definition. Additional intent
  layers onto that seed without discarding authored subjects, periods, lessons, rules, preferences, or optional values.
- G2. The source/result identity is checked before solving and accepting. Source data and export stay exact until
  step 7 succeeds; acceptance publishes definition, result, provenance, revisions, and authoring context together.
- G3. Persistent authored policy locks and attempt-scoped pins retain their distinct meaning/provenance. Pins do not
  silently overwrite conflicting policy. Only surviving accepted placements can be pinned; new lessons are additions.
  Earlier attempt pins have already expired in the authored seed. Repair does not reintroduce them or strip an
  explicitly authored persistent lock merely because it matches an earlier pin's value. New repair pins expire for
  later attempts according to their temporary provenance.
- G4. Proposal review contains all definition changes, even with zero reported moves. Timing changes are computed from
  old/new definitions and assignment sides, and show every affected lesson. New/cancelled sides are labelled, not
  invented. Period-ID movement counts remain distinguishable from timing-impact counts.
- G5. A FEASIBLE result at a time limit is usable after verification and is labelled with its actual termination
  reason. No unsuccessful run, restart, preview, or feasible proposal alone becomes Current or is called optimal.
- G6. Convergence exercises the exact MVK acceptance data in an isolated workspace through authoring, seeded-draft
  abandonment/restoration with UC-18, re-finish, real packaged repair/verification, review and explicit acceptance.
  Evidence observes the selected lesson cancellation, same-ID time impacts, and unchanged prior Current until accept.
  Real-browser wide/narrow observations and the passing agent-run walkthrough under UC-1 G7 are required.
- G7. Stable-state reload/restart restores seed, staging, pins, retained authoring and any saved proposal; acceptance
  still requires current eligibility and verification. Failed/unauthorized/stale actions preserve saved work as stated
  above. Narrow screens remain read-only and show authoritative lifecycle/Current labels.

### Postconditions

- Success: The complete verified successor is Current, with matching metadata/provenance and no retained working draft.
- Minimal guarantee: Until atomic acceptance, the exact prior baseline stays Current and saved authored work survives.

## UC-18 - Abandon seeded repair and resume authoring

- Goal: Revise authored changes after deciding not to continue the current repair attempt.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests abandonment of an authoring-seeded repair draft.
- Preconditions: REPAIR_DRAFT has retained authoring; no solve is active.
- Relations:
  - Requires: UC-16, whose accepted-source success postcondition supplies retained authoring
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests Discard repair draft and return to authoring.
2. The system explains that repair staging and pins will be removed while authored changes will remain.
3. The administrator confirms.
4. The system atomically removes repair intent and restores DEFINITION_DRAFT with the retained authored values/context.
5. The system shows the restored draft, issues and source; the administrator can revise and finish again.

### Extensions

- 1a. If a solve is active, the system refuses abandonment until existing cancellation has restored REPAIR_DRAFT; end.
- 1b. If REPAIR_PROPOSAL is displayed, the system explains proposal-only discard first, as UC-17 extension 5b; end.
- 1c. If repair has no authored seed, existing ordinary repair discard returns to ACCEPTED_BASELINE; no authoring is
  invented; end.
- 3a. If confirmation is cancelled, all repair and authored work remains unchanged; end.
- 4a. If state/access/version changed or saving fails, the system reports that abandonment was not committed and keeps
  the repair draft, staging, pins, retained authoring, and Current unchanged; end.

### Guarantees

- G1. The restored draft is the retained authored draft, including stable identifiers, defaults, exceptions,
  omissions, source revisions, and issues. Repair-only availability changes/pins are not copied into it.
- G2. Current remains exact. Fully abandoning authored changes is a subsequent UC-19 action with a separate confirmation.
- G3. Restore is atomic and obeys UC-1's access, desktop/narrow presentation, and walkthrough evidence boundaries.

### Postconditions

- Success: Authoring is active with its saved changes intact and the abandoned repair's staging/pins absent.
- Minimal guarantee: Failed/cancelled abandonment loses no saved work and changes no Current data.

## UC-19 - Discard authoring and return to its source

- Goal: Abandon authored changes and recover the unchanged source workspace.
- Primary actor: School timetable administrator
- Supporting actors: Local durable workspace
- Trigger: The administrator requests Discard authoring.
- Preconditions: DEFINITION_DRAFT is active.
- Relations:
  - Requires: UC-1, whose successful postcondition supplies the draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests whole-draft discard.
2. The system names the source state, describes the saved changes being abandoned, and explains EMPTY restoration
   for a newly created school.
3. The administrator confirms discard.
4. The system atomically removes authored work and returns to the recorded source state and exact source content.
5. The system presents the restored empty, initial, or Current workspace and its available ordinary actions.

### Extensions

- 3a. If confirmation is cancelled, saved draft and unsaved working values remain unchanged; end.
- 4a. If access/state/version changed or storage fails, no discard is committed and the saved draft/source stay intact;
  end.
- 1a. If seeded repair is active, the system directs the administrator to UC-18 first rather than combining both
  abandonments; end.

### Guarantees

- G1. EMPTY restoration leaves no new school definition. Initial restoration recovers its exact definition/revision;
  accepted restoration recovers the exact accepted definition/result/provenance, including pre-upgrade catalog.
- G2. Discard never adopts authored names, defaults, identifiers, or values into the source. No silent source rebase occurs.
- G3. Confirmation, access, atomicity, desktop/narrow display and evidence boundaries are UC-1's. Failed discard never
  reports a restored source while a draft remains committed.

### Postconditions

- Success: Authoring is closed and its original source state/content is restored.
- Minimal guarantee: Refused/cancelled/failed discard leaves the saved draft and source unchanged.

## UC-20 - Find the supported route for a teacher change

- Goal: Reach definition authoring when changing who teaches a lesson.
- Primary actor: School timetable administrator
- Supporting actors: none
- Trigger: The administrator inspects teacher-change guidance in manual editing or requests a manual teacher change.
- Preconditions: A local timetable or manual draft is being inspected.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator seeks to change a lesson's teacher.
2. The system explains that teaching responsibility belongs to the school definition and requires authoring and,
   after acceptance, repair/explicit acceptance; the manual editor has no teacher-changing control.
3. If no other draft/proposal/solve is active, the administrator follows the authoring entry to UC-1.
4. The administrator can edit shared teaching settings or selected occurrences through UC-8/UC-9.

### Extensions

- 2a. If a manual draft without teacher differences is open, the system names that draft and explains explicit
  publication of supported period/room edits or explicit discard before authoring. It transfers no pending changes
  automatically; end.
- 2b. If another draft/proposal/solve is active, the system names the blocking state and its return/finish route; end.
- 2c. If a non-UI manual teacher mutation is requested, it is refused with the same authoring guidance and no assignment
  or definition change; end.
- 2d. If an already saved legacy manual draft contains teacher differences, the system identifies those lessons and
  explains that the mismatch blocks publication. The administrator can explicitly revert each affected lesson to
  its full accepted assignment, including period, room, and teacher, then use the existing publication/discard path,
  or explicitly discard the manual draft before authoring. No teacher change is transferred or normalized
  automatically; end.
- 3a. If the administrator chooses not to leave the current view, no mutation occurs; end.

### Guarantees

- G1. Manual period/room editing keeps its existing behavior. No manual teacher-publication/adoption path is claimed:
  a mismatch between an assignment teacher and its lesson is invalid under current kernel verification.
- G2. Guidance never opens a second draft, silently abandons a manual draft, changes a qualification, or alters Current.
  Direct requests and visible controls share the same boundary.
- G3. Guidance is available in English and narrow read-only inspection, renders school names literally, and is
  exercised in the real browser with the access and walkthrough boundary of UC-1.

### Postconditions

- Success: The administrator knows and can enter the supported teacher-change path when the lifecycle permits it.
- Minimal guarantee: Existing assignments, manual work, source, and Current remain unchanged.

## Normative data

The field tables below contain exactly these rows—no more, no fewer—for the supported contract. They are the complete
meaning of full authoring. “Since” is the first supported catalog; an unavailable field is not emitted even with a
neutral value. Ranges constrain finishable definitions, not the ability to save incomplete supported draft inputs.
Required identities are chosen validly before first item save because they become immutable. Other supported blanks,
invalid numbers, or unresolved values remain visible saved work with blockers until mapped without loss.

Optional absence and an explicitly empty array are distinct and preserved. Required collection arrays may be empty.
Reference arrays contain unique IDs. Names are nonblank strings. IDs match
`^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$`. Numeric maximum 2147483647 below is the explicit unrestricted sentinel for the
listed optional bounds, not an extra visible “magic number”; the UI names the no-limit mode.

### School fields

Exactly these 14 rows—no more, no fewer:

| Field | Since | Required / control | Meaning and fidelity |
|---|---:|---|---|
| schemaVersion | 1 | Required; system-managed | Exactly 1 |
| catalogVersion | 1 | Required; controlled upgrade | Supported integers 1–9; new schools 9; accepted-source repair minimum 8 |
| schoolId | 1 | Required; immutable after identity save | Valid ID; remains the same across repair |
| displayName | 1 | Required; editable | Nonblank school name; source/Current name stays unchanged until its appropriate transition |
| basedOnRevision | 1 | Optional; system-managed | Absent for initial planning; successor binds accepted inputRevision; sha256: followed by 64 lowercase hexadecimal digits |
| subjects | 1 | Required collection | Exactly the saved subject rows |
| teachers | 1 | Required collection | Exactly the saved teacher rows |
| cohorts | 1 | Required collection | Exactly the saved class rows |
| rooms | 1 | Required collection | Exactly the saved room rows |
| periods | 1 | Required collection | Exactly the declared period rows |
| reservedPeriodIds | 1 | Optional selection | Unique declared period IDs; omission means no reservations |
| lessons | 1 | Required collection | Exactly the saved occurrences, including an empty array |
| roomAssignments | 9 | Optional collection | Native policy rows; absent below 9, even an empty collection is unavailable there |
| softConstraintOverrides | 1 | Optional collection | At most one override per available ordinary preference ID |

### Subject fields

Exactly these seven rows—no more, no fewer:

| Field | Since | Required / range | Omission and meaning |
|---|---:|---|---|
| id | 1 | Required ID | Immutable after first save |
| displayName | 1 | Required nonblank name | Editable |
| reservedPeriodsAllowed | 6 | Optional boolean | False; permission to use school-reserved periods |
| maxWeeklyReservedLessonsPerCohort | 6 | Optional integer 1–2147483647 | Unlimited; if present, reservedPeriodsAllowed must explicitly be true |
| dayEdgeOnly | 6 | Optional boolean | False; restricts subject to supported cohort-day edges |
| maxDailyLessonsPerCohort | 6 | Optional integer 1–2147483647 | Unlimited; hard daily subject count per cohort |
| curatorLesson | 8 | Optional boolean | False; teacher must be the cohort's curator, with its supported home-room obligations |

### Teacher fields

Exactly these five rows—no more, no fewer:

| Field | Since | Required / range | Omission and meaning |
|---|---:|---|---|
| id | 1 | Required ID | Immutable after first save |
| displayName | 1 | Required nonblank name | Editable |
| qualifiedSubjectIds | 1 | Required unique subject-ID array | May be empty; ordinary lesson qualification references this set |
| availablePeriodIds | 1 | Optional unique period-ID array | All declared periods; explicit empty means no availability |
| undesirablePeriodIds | 1 | Optional unique period-ID array | None; ordinary preference, not unavailability |

### Cohort fields

Exactly these 12 rows—no more, no fewer:

| Field | Since | Required / range | Omission and meaning |
|---|---:|---|---|
| id | 1 | Required ID | Immutable after first save |
| displayName | 1 | Required nonblank name | Administrator-facing class name |
| size | 1 | Required integer 1–2147483647 | Student count for room capacity |
| availablePeriodIds | 1 | Optional unique period-ID array | All declared periods; explicit empty means no availability |
| undesirablePeriodIds | 1 | Optional unique period-ID array | None; ordinary preference |
| maxDailyLessonSpread | 4 | Optional integer 0–2147483647 | 1; target for the weekly-balance preference, not a hard limit |
| maxDailyGaps | 5 | Optional integer 0–2147483647 | 0 in catalogs 5–9; prior catalogs have unrestricted gaps |
| latestStartSlot | 6 | Optional integer 1–2147483647 | Unrestricted hard latest start in regular-slot terms |
| preferredLatestStartSlot | 6 | Optional integer 1–2147483647 | 3; preferred latest start in regular-slot terms |
| dailyLessonSpreadLimit | 7 | Optional integer 0–2147483647 | Unrestricted hard daily lesson-load spread |
| curatorTeacherId | 8 | Optional teacher ID | No curator; required for lessons of a curator subject |
| homeRoomId | 8 | Optional room ID | No home room; when present, hard for curator lessons and fallback preference for eligible other lessons |

No home room is invented or required merely by opening a cohort. A matching Use home room policy requires one.
Curator qualification is automatic only for that curator's own cohort's curator-subject lessons.

### Room fields

Exactly these five rows—no more, no fewer:

| Field | Since | Required / range | Omission and meaning |
|---|---:|---|---|
| id | 1 | Required ID | Immutable after first save |
| displayName | 1 | Required nonblank name | Editable |
| capacity | 1 | Required integer 0–2147483647 | Zero is allowed as definition data |
| capabilityIds | 1 | Required unique valid-ID token array | May be empty; no separate capability catalog |
| availablePeriodIds | 1 | Optional unique period-ID array | All declared periods; explicit empty means no availability |

### Period fields

Exactly these six rows—no more, no fewer:

| Field | Since | Required / range | Omission and meaning |
|---|---:|---|---|
| id | 1 | Required ID | Immutable after first save; timing fields remain editable |
| displayName | 1 | Required nonblank name | Editable; neither name nor ID implies reservation |
| weekday | 1 | Required weekday | Exactly MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY |
| order | 1 | Required integer 1–2147483647 | Unique within weekday; declared position |
| startTime | 1 | Optional local clock string | HH:mm or HH:mm:ss; requires endTime |
| endTime | 1 | Optional local clock string | HH:mm or HH:mm:ss; requires startTime, positive duration |

Supplied intervals must not overlap on a weekday. Unchanged optional clock values retain their supplied representation.
Clock times describe displayed teaching time; they do not add a new duration/overlap-based solver model.

### Lesson fields

Exactly these 11 rows—no more, no fewer:

| Field | Since | Required / range | Omission and meaning |
|---|---:|---|---|
| id | 1 | Required ID | Immutable occurrence identity |
| displayName | 1 | Required nonblank name | Generated initially for new occurrences; editable and not regenerated on ordinary edits |
| subjectId | 1 | Required subject ID | One subject |
| cohortId | 1 | Required cohort ID | One cohort |
| teacherId | 1 | Required teacher ID | One preassigned teacher; supported qualification/curator checks apply |
| seriesId | 1 | Optional valid ID | No series; members must share cohort and subject |
| requiredRoomCapabilityIds | 1 | Optional unique valid-ID token array | No capability requirements |
| preferredRoomIds | 1 | Optional unique room-ID array | Omitted or empty falls back to cohort home-room preference when applicable |
| undesirablePeriodIds | 1 | Optional unique period-ID array | None; ordinary preference |
| periodLock | 1 | Optional period ID | No persistent period lock |
| roomLock | 1 | Optional room ID | No persistent room lock |

Each occurrence occupies one period. Multiple teachers, cohorts, rooms, duration spans, or adjacency requirements are
not alternative values of these fields. Changing series membership explains its effect on the same-day preference.

Lock values and their accepted provenance must be read together. A source periodLock or roomLock marked
ATTEMPT_SCOPED represents a prior repair pin, not persistent school policy. UC-1 expires it in the working successor
while preserving the exact source; review and source reverts explain that distinction. An explicit policy save
establishes persistent provenance, even for the same value. New repair pins remain separately identified and cannot
overwrite or erase authored persistent locks implicitly.

### Room-assignment fields

Exactly these five rows—no more, no fewer:

| Field | Since | Required / range | Omission and meaning |
|---|---:|---|---|
| id | 9 | Required ID | Immutable rule identity; there is no contract displayName field |
| subjectId | 9 | Required subject ID | Matches this subject |
| teacherId | 9 | Optional teacher ID | Omission matches any teacher for the subject |
| allowedRoomIds | 9 | Nonempty unique room-ID array in this target mode | Allowed-room set; mutually exclusive with useHomeRoom |
| useHomeRoom | 9 | Exactly true in this target mode | Use each matched cohort's home room; mutually exclusive with allowedRoomIds |

Exactly one target mode is required. All matching rules intersect, and their IDs/selectors/targets are preserved.

### Soft-override fields

Exactly these two rows—no more, no fewer:

| Field | Since | Required / range | Meaning |
|---|---:|---|---|
| constraintId | 1 | Required available ID from Preference weights | One override per ID |
| weight | 1 | Required integer 0–1000000 | Explicit zero disables this ordinary preference |

### Preference weights

Exactly these seven rows—no more, no fewer; only the subset introduced by the draft catalog is available.
Every catalog default below is 1.

| Constraint ID | Since | Meaning |
|---|---:|---|
| soft.teacher-gap | 1 | Teacher timetable gaps |
| soft.series-same-day | 1 | Additional occurrences of a series on the same weekday |
| soft.undesirable-period | 1 | Declared undesirable periods |
| soft.non-preferred-room | 1 | Use outside effective preferred rooms |
| soft.cohort-gap | 2 | Cohort timetable gaps |
| soft.cohort-week-balance | 2 | Weekly load balance, using the supported daily-spread target |
| soft.cohort-late-start | 3 | Starting after the preferred regular slot |

### Catalog upgrade adjustments

Exactly these three rows—no more, no fewer—cover behavior-changing defaults when increasing supported catalogs.
Other newly available omitted fields keep their neutral or already equivalent effective values.

| Crossing | Adjustment | Preserved behavior |
|---|---|---|
| Source below 2, target at least 2 | Add explicit zero overrides for soft.cohort-gap and soft.cohort-week-balance | Newly introduced preferences remain inactive |
| Source below 3, target at least 3 | Add explicit zero override for soft.cohort-late-start | Newly introduced start preference remains inactive |
| Source below 5, target at least 5 | Set each source cohort's omitted maxDailyGaps explicitly to 2147483647 | Earlier unrestricted gaps remain unrestricted |

An accepted source below 8 must preview/confirm an upgrade to 8 when authoring opens; an explicit later upgrade targets
9. A source already at 8/9 needs no opening upgrade. Initial-source catalogs are retained until explicitly upgraded.
Existing explicit weights/attributes win. Reverts use the source's effective values under the retained newer catalog;
they do not remove compensating values in a way that adopts new defaults accidentally.

### Identifier rules

New suggestions use Unicode compatibility decomposition, remove combining diacritics, and lowercase invariantly.
Characters outside ASCII letters/digits/dot/underscore/hyphen become hyphens; consecutive hyphens collapse, leading
non-alphanumeric characters and trailing hyphens are removed. A result with no alphanumeric content uses its item-kind
name. Suggestions are limited to 128 characters and made unique by the first available -2, -3, and subsequent suffix,
shortening the base when needed. Õ/ä/š become o/a/s. Names themselves are preserved.

Manual overrides may use any valid contract ID, including uppercase. IDs are unique within each declared entity
collection; identical strings across different collections are allowed. Source/current draft and removed identities
in the lineage reserve their IDs. Source restoration reuses the original identity explicitly.

New requirement series suggestions combine cohort and subject display names. New generated occurrences append .01,
.02, and subsequent sequence numbers to the series suggestion, shortening only the generated base if needed. The
sequence skips occupied/reserved IDs and remains monotonic for that requirement's retained editing context. Names
initially combine cohort, subject, and occurrence number. Existing series IDs, occurrence IDs/names, and exceptions
are never regenerated by opening, count reduction, renaming, or teacher changes.

### Dependency references

Exactly these seven rows—no more, no fewer—define the removal flow's reference population:

| Target | Incoming definition references / handling |
|---|---|
| Period | Root reservedPeriodIds; teacher/cohort availablePeriodIds and undesirablePeriodIds; room availablePeriodIds; lesson periodLock and undesirablePeriodIds |
| Subject | Teacher qualifiedSubjectIds; lesson subjectId; room-rule subjectId |
| Teacher | Cohort curatorTeacherId; lesson teacherId; optional room-rule teacherId |
| Cohort | Lesson cohortId |
| Room | Cohort homeRoomId; lesson preferredRoomIds and roomLock; room-rule allowedRoomIds |
| Series / weekly requirement | Lesson seriesId plus editor membership; explicit regrouping or selection of all cancelled occurrences |
| Lesson / room rule | No incoming kernel entity references; removing an occurrence also updates its requirement count explicitly |

Historical source assignments are displayed for impact and never modified by removal. Omitted availability has no
literal incoming references. Removing the final allowed room requires an explicit replacement, target-mode change, or
rule removal; an empty allowed-room target is invalid. Capability tokens are owned field values, not declared targets.

### Validation meanings

UC-15 consumes exactly these two severity classes—no more, no fewer:

| Class | Meaning | Finish effect |
|---|---|---|
| Blocking | Required/type/range/identity/reference/version violations; nonblank names; duplicate orders/overlapping or invalid paired times; qualifications and series consistency; curator obligations; contradictory locks/reservations/availability/locked collisions; invalid matched room policies; duplicate/unavailable overrides or score-range overflow; incomplete editor work; authoritative kernel errors | Finish refused |
| Advisory | Proven necessary-condition scheduling impossibility: teacher/cohort load exceeds an eligible-period upper bound, no room can hold a taught cohort, or no room supplies a lesson's required capability combination | Visible, does not refuse finish |

Advisories account for subject reservation permissions and declared availability, apply only to configured teaching,
and never claim a complete feasibility analysis. A contradiction that the contract itself rejects remains blocking
even when a similar general shortage can be advisory. Source-vs-draft timing impact is change review, not an invented
kernel validation error. Verification unavailability is an incomplete operation, not proof of invalidity or feasibility.

### Small-school acceptance data

UC-16 G5 uses this exact synthetic school. These initial tables contain exactly the stated rows—no more, no fewer.
Other school values are allowed in the product; additional validation cases use explicitly labelled isolated variants.

School identity: authoring-demo-school, display name Authoring Demo School, schema 1, catalog 9, no basedOnRevision.
Optional fields are omitted except the paired bell times and the lesson series/capability values stated below.
Required arrays are present.
There are two subjects, two teachers, one cohort, two rooms, ten periods, and nine lessons. No reservations, room rules,
or preference overrides are configured.

| Subject ID | Name |
|---|---|
| mathematics | Mathematics |
| estonian | Estonian |

| Teacher ID | Name | Qualified subjects |
|---|---|---|
| mari | Mari | mathematics |
| jaan | Jaan | mathematics, estonian |

| Cohort ID | Name | Size |
|---|---|---:|
| 5a | 5A | 20 |

| Room ID | Name | Capacity | Capabilities |
|---|---|---:|---|
| b1 | B1 | 25 | general-classroom |
| b2 | B2 | 25 | general-classroom |

| Period ID | Name | Weekday | Order | Start | End |
|---|---|---|---:|---|---|
| monday-1 | Monday 1 | MONDAY | 1 | 09:00 | 09:45 |
| monday-2 | Monday 2 | MONDAY | 2 | 10:00 | 10:45 |
| tuesday-1 | Tuesday 1 | TUESDAY | 1 | 09:00 | 09:45 |
| tuesday-2 | Tuesday 2 | TUESDAY | 2 | 10:00 | 10:45 |
| wednesday-1 | Wednesday 1 | WEDNESDAY | 1 | 09:00 | 09:45 |
| wednesday-2 | Wednesday 2 | WEDNESDAY | 2 | 10:00 | 10:45 |
| thursday-1 | Thursday 1 | THURSDAY | 1 | 09:00 | 09:45 |
| thursday-2 | Thursday 2 | THURSDAY | 2 | 10:00 | 10:45 |
| friday-1 | Friday 1 | FRIDAY | 1 | 09:00 | 09:45 |
| friday-2 | Friday 2 | FRIDAY | 2 | 10:00 | 10:45 |

The administrator generates these five weekdays with two periods each, initially using 09:00–09:45 and 10:00–10:40,
then adjusts each second period's end to 10:45 in the individual-row preview before explicitly saving the final grid.

| Requirement / series | Cohort | Subject | Shared teacher | Count | Required capabilities |
|---|---|---|---|---:|---|
| 5a-mathematics | 5a | mathematics | mari | 5 | general-classroom |
| 5a-estonian | 5a | estonian | jaan | 4 | general-classroom |

The five Mathematics occurrences have IDs 5a-mathematics.01 through .05 and names 5A Mathematics 1 through 5.
The four Estonian occurrences have IDs 5a-estonian.01 through .04 and names 5A Estonian 1 through 4. Each has the
corresponding shared cohort/subject/teacher/series and requiredRoomCapabilityIds [general-classroom]. Other optional
lesson fields are absent. This defines exactly nine occurrence rows, not approximate sample data.

Isolated variants exercise the associated UC guarantees: an explicit Mathematics teacher exception to jaan; changing
a default until an exception equals it and back; count reduction with selected removal and fresh-ID addition; mixed
imported values and ungrouped lessons; selected availability versus omission after grid addition; invalid references,
constraints and save failures; source restores; and catalog 1–4 upgrades using Catalog upgrade adjustments. Variants
must identify their differences and must not silently change this primary onboarding school.

### MVK acceptance data

UC-17 G6 starts with the exact repository reference definition at examples/mvk.json, SHA-256
c2a61ac92fd92a53af0b163a6fa51cff97ff1daacae2c4b6ee00dd59f5e6cf25. It is used in an isolated workspace, with a matching
real verified baseline generated from that definition. Its school ID is merivalja-kool, name Merivälja Kool, schema 1,
catalog 9, with 26 subjects, 51 teachers, 23 cohorts, 42 rooms, 50 periods, 520 lessons, and seven room policies.
It is a fixed reference definition, not the user's mutable runtime school. If unavailable or its digest differs,
this journey cannot claim to have exercised the specified reference data.

The seven source policies are exactly these rows—no more, no fewer—and survive the journey unchanged:

| Policy ID | Subject ID | Teacher scope | Target |
|---|---|---|---|
| mvk-history-a231 | ajalugu | any | allowedRoomIds [a231] |
| mvk-piret-maths-b212 | matemaatika | piret-noor | allowedRoomIds [b212] |
| mvk-heiki-literature-a209 | kirjandus | heiki-raudla | allowedRoomIds [a209] |
| mvk-chemistry-a233 | keemia | any | allowedRoomIds [a233] |
| mvk-physics-a223 | fuusika | any | allowedRoomIds [a223-lab] |
| mvk-physical-education-halls | kehaline-kasvatus | any | allowedRoomIds [kk1, kk2, kk3] |
| mvk-study-support-home | opiabi | any | useHomeRoom true |

Exactly these three authored changes—no more, no fewer—define the core existing-school journey:

| Target | Source | Authored value / selection |
|---|---|---|
| Cohort 6c homeRoomId | Absent; 6C has size 24 and curator olga-simonovits | a210, capacity 30, general-classroom capability |
| Series 6c.vene-keel weekly count | Two occurrences, .01 and .02, both assigned to olga-simonovits | One occurrence: explicitly cancel lesson 6c.vene-keel.02, preserve .01 and its settings |
| Period mon-1 start/end | MONDAY, order 2, 08:45:00–09:30:00 | Same ID/weekday/order, 08:50:00–09:35:00 |

The successor has 519 lessons before any separately labelled staging variant. No source lesson ID/name, policy,
qualification, reservation, preference override, or other attribute is silently regenerated. Review identifies
every baseline lesson at mon-1 as experiencing the supplied clock-time change, including any whose destination
retains that period ID. The accepted source remains exact during draft, finish, seeded repair, abandonment/restoration,
re-finish, solve, and proposal review. Acceptance verifies and publishes the complete successor/result.

Additional availability and pin behavior is exercised in separately labelled seeded-repair variants so its effects
are distinguishable from the three core authored changes. No extra user-authored values are smuggled into the
reference source to force a green result. An unsuccessful real solve is recorded as unsuccessful; it is not acceptance
evidence.

## Out of scope

- New accounts, roles, approval chains, multi-user concurrent authoring, or outbound data services.
- Mobile authoring/repair mutations; narrow inspection remains read-only.
- Parking/rebasing authoring alongside another live workflow.
- Arbitrary cross-resource copying or bulk editing beyond grid generation, requirement operations, selected teacher
  reassignment, guided removal, and catalog adjustments.
- Persistent operation undo/redo; source-based reverts are in scope.
- Spreadsheet/student-information-system imports or recreating an MVK-sized school as the onboarding acceptance gate.
- Identifier renaming or silent reuse within an active authoring lineage.
- Manual teacher publication or a handoff of pending manual edits into authoring.
- No-solve publication after baseline acceptance.
- New kernel models for variable-duration/multi-period adjacency, overlapping/split groups, merged cohorts,
  team teaching, multi-room lessons, or solver-selected teachers.
- Performance thresholds, custom keyboard-only navigation, or a new participant study; this feature requires the
  concrete browser/durable/kernel evidence and agent-run administrator journeys named in its UC guarantees.

## External dependencies

No product decision is deferred to an unavailable stakeholder. Existing local workspace and kernel execution must be
available to exercise the declared boundaries. Runtime verification or walkthrough evidence that cannot be obtained
is a delivery/convergence limitation, not permission to weaken a use-case guarantee or claim approval. Technical
rules and implementation are subsequent stages and must not choose new product behavior.
