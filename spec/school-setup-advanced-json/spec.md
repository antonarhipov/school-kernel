# Complete school document editing

## Summary and resolved decisions

Forms and collapsed Advanced JSON edit the same complete school definition inside School setup. Explicit Apply is required; pending JSON locks form editing until apply or explicit discard. Invalid syntax/structure is refused; semantic incompleteness remains draft work. No lossy round trip is permitted. Existing kernel and recovery imports remain supported with explicit conversion adoption. Utilities is removed, with its actions relocated to Setup. Existing immutable bundled schools, checked local-copy saving, clean timetable landing, animated overlay, generation and local-cleanup protections remain in force.

## Actors and domain terms

- Administrator: demo visitor authoring and generating for browser-local schools.
- School document: versioned, complete authoring data, distinct from solver input and editor/session metadata.
- JSON buffer: unfinished text belonging to one setup draft; never a second school-save workflow.
- Apply: structurally checked replacement of that draft's school data, invalidating old readiness evidence.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
| --- | --- | --- | --- |
| UC-1 | Edit the same school using forms or JSON | Administrator | None; primary |
| UC-2 | Exchange complete and legacy school documents | Administrator | Requires UC-1 |
| UC-3 | Manage generation and local data from Setup | Administrator | Requires UC-1 |

## State model

A draft has synchronized JSON or unapplied JSON. Form edits refresh synchronized JSON. Text edits enter unapplied state. Successful Apply or explicit discard returns to synchronized state. Invalid Apply preserves unapplied state. Closing/reload preserves either state. Form mutations, school saving and checking are refused while unapplied text exists; the administrator is directed to Apply, Discard or keep editing JSON. No other transition changes the document or silently drops text. Draft persistence retains existing conflict/quota guards.

## Document format

Version 1 uses `documentType: "school-setup"`, `formatVersion: 1`, `name`, `grid`, `subjects`, `teachers`, `classes`, `rooms` and `plans`. The [downloadable JSON Schema](../../timetable-workspace/src/main/resources/static/workspace/school-document-schema.js) defines supported nested fields and rejects unknown properties. Unfinished numeric form input may be retained as text; semantic validation requires valid numbers before saving. Optional absence and explicit empty collections remain distinct. Generated lesson subject/class references come from their teaching plan; generated solver fields and mutable check/session metadata have no competing editable representation.

## UC-1 - Edit the same school using forms or JSON (primary)

- Goal: Edit the same school using forms or JSON.
- Primary actor: Administrator
- Supporting actors: None
- Trigger: The administrator opens Advanced JSON.
- Preconditions: School setup has an active independent draft.
- Relations:
  - Requires: None
  - Includes: None
  - Extends: None

### Main success scenario

1. The system shows the complete school document and explains explicit Apply.
2. The administrator edits JSON; the system retains the exact unfinished text in that draft.
3. The administrator requests Apply; the system validates syntax and structure, replaces only school data, clears stale readiness evidence and reports any located semantic issues.
4. The administrator returns to forms and sees the exact applied settings.
5. The administrator saves a school copy after resolving issues; the system checks the exact current definition and saves complete authoring data through the existing immutable-copy lifecycle.

### Extensions

- 3a. Invalid syntax, duplicate keys, unsupported properties or structurally invalid fields: show located actionable errors; leave school data and text unchanged; resume at step 2.
- 3b. Structurally valid incomplete or inconsistent data: retain it as draft work, show field paths and refuse school save until corrected; resume at step 2.
- 4a. Unapplied text exists: require Apply, explicit Discard or continued JSON editing before form mutations, check or save; resume at step 2.
- 2a. Close or reload: retain exact pending text and draft; resume at step 1 on explicit reopen.
- 2b. Quota or stale-tab refusal: show persistence failure, retain in-page work and offer recovery/retry; resume at step 2.

### Guarantees

- G1. Preserve by value names and identifiers, named grid intervals/times/kinds/adjacency, qualification and class eligibility, availability/preferences/lunch, unassigned demand, allocations, individual occurrences/locks/series, room policies, block rules, inherited optional absence and explicit empty sets. Reject unsupported fields rather than silently stripping them.
- G2. Editor identity, navigation, provenance and check evidence cannot be forged by editing the school document. Applied data invalidates readiness. Bundled and other saved schools/timetables are unchanged.
- G3. Errors expose precise document paths; structural validation and semantic validation have distinct outcomes. Full validation includes authoritative solver-input validation before school saving; it is not proof of a feasible timetable.
- G4. Advanced is collapsed on initial activation; controls are keyboard reachable in the desktop layout (above 700 CSS pixels). School setup and its entry are unavailable in the mobile layout. Resizing from desktop to mobile closes Setup and its confirmations while preserving unfinished work; returning to desktop restores the entry and requires explicit reopening.

### Postconditions

- Success: One local school copy is saved with complete authoring settings, or the applied draft is available for continued form editing.
- Minimal guarantee: Existing saved artifacts and pending work remain intact on refused or cancelled actions.

## UC-2 - Exchange complete and legacy school documents

- Goal: Exchange complete and legacy school documents.
- Primary actor: Administrator
- Supporting actors: None
- Trigger: The administrator requests import or export.
- Preconditions: UC-1 is available.
- Relations:
  - Requires: UC-1, shared document editing is available
  - Includes: None
  - Extends: None

### Main success scenario

1. The administrator exports the current school document; the system downloads all authored settings, including incomplete work.
2. The administrator selects a complete school document, legacy kernel document or supported recovery file.
3. The system validates structure and previews adoption, explaining legacy defaults, missing authoring details and retained protected settings.
4. The administrator confirms; the system creates an independent draft preserving the imported school settings.

### Extensions

- 2a. Syntax, duplicate keys, unsupported format/constraint or corrupt structure: show a located refusal and preserve existing draft and source; resume at step 2.
- 4a. Cancel conversion: preserve existing draft, pending text and saved artifacts exactly; end.
- 1a. Unapplied JSON exists: resolve it before export/import adoption, or download the full recovery snapshot to retain it; resume at step 1.

### Guarantees

- G1. Complete document export/import preserves every school setting from UC-1 G1; it excludes mutable check evidence and page/session identity.
- G2. Legacy conversion never silently drops constraints or invents advanced intent. It discloses assumptions and retains supported kernel values, optional absence and explicit empty sets.
- G3. Old standalone JSON text remains recoverable through Advanced; migration never silently replaces current work.

### Postconditions

- Success: An exported complete document is available or a separate imported draft is open.
- Minimal guarantee: Existing saved artifacts and pending work remain intact on refused or cancelled actions.

## UC-3 - Manage generation and local data from Setup

- Goal: Manage generation and local data from Setup.
- Primary actor: Administrator
- Supporting actors: None
- Trigger: The administrator opens Setup to generate or manage local data.
- Preconditions: UC-1 is available; bundled schools are loaded.
- Relations:
  - Requires: UC-1, shared document editing is available
  - Includes: None
  - Extends: None

### Main success scenario

1. The system leaves the timetable as the clean landing screen, with no Utilities section and the left School setup entry.
2. The administrator opens Setup Review for a saved local school and explicitly starts generation with the chosen effort.
3. The system shows progress/cancellation in Review and a compact timetable indicator that reopens the originating school Review.
4. The completed candidate is available in the timetable for independent naming/saving.
5. The administrator opens Advanced and requests local-data cleanup; the system explains scope and requests confirmation.
6. The administrator confirms; the system removes local schools, timetables, candidates, setup/manual/JSON drafts and recovery/selection metadata, then returns to the bundled timetable.

### Extensions

- 2a. Unsaved or bundled school: explain that a checked named local school copy is needed; end.
- 3a. Cancellation, process interruption or failed search: retain school and existing timetables, show the actual outcome and permit a later explicit request; end.
- 5a. Cancel cleanup: preserve exact browser data and active work; end.
- 6a. Removal fails: report failure and preserve/roll back existing data through the existing cleanup boundary; resume at step 5.

### Guarantees

- G1. No generation occurs on landing, import, Apply or school save. Generation uses the exact saved checked definition and existing build/revision guards.
- G2. No Utilities DOM or guidance remains on the timetable. Empty-timetable guidance and generation links lead to Setup Review. Cleanup remains separately confirmed and browser-local; bundled resources are unaffected.
- G3. Cleanup retains epoch/stale-tab protections and prevents pending asynchronous work from recreating deleted artifacts.
- G4. Reload always opens the bundled precomputed timetable with Setup closed, even with saved schools or unfinished JSON.

### Postconditions

- Success: The administrator has a generated candidate or a clean bundled timetable after confirmed cleanup.
- Minimal guarantee: Existing saved artifacts and pending work remain intact on refused or cancelled actions.

## Out of scope

Deployment, server-persisted authoring, new solver constraints, automatic JSON/form merges, and changing bundled data.
