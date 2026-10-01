# Technical Rules: School Definition Authoring

## Design overview

Definition authoring adds a `DEFINITION_DRAFT` lifecycle state to the singleton `workspace_aggregate`. Opening a draft
(`POST /api/definition-draft`) deep-copies the source definition, either `document.initialDefinition` or
`document.acceptedBaseline.definition`, into `document.definitionDraft`, together with the source state, the source
definition revision, the effective catalog version, and, for an accepted source, `basedOnRevision`. The source nodes
are never written while the draft exists.

Edits are typed commands sent to `PATCH /api/definition-draft`: add, update, revert, and remove for cohorts,
teachers, and rooms, plus teacher reassignment for lessons and room-assignment rules. The server applies each command
to the draft definition, normalizes omission, re-runs a deterministic workspace check pass that produces the normative
issue list, and persists everything in one optimistic transaction. The browser renders the snapshot's issues and never
computes them.

Finishing (`POST /api/definition-draft/finish`) always invokes School Kernel verification of the exact complete draft.
An initial source uses the existing definition-only `verify`. An accepted source uses a new kernel verification mode
for successor definitions, which checks lineage against the accepted result without solving. On `VERIFIED`, one
transaction either replaces the initial definition (as `POST /api/initial-draft/replace` does) or creates a repair
draft whose base definition is the verified successor and which keeps the definition draft as `authoredDraft`. The
existing repair services then stage, pin, compile, solve, and accept from that base instead of the accepted
definition. Discarding such a repair draft restores `authoredDraft` as `definitionDraft`.

## Codebase alignment

- All workspace changes live in `timetable-workspace`. The one kernel change (RULE-6) lives in `kernel-cli` and
  `kernel-contract`, under the existing verify command and verification-result schema.
- Stack conventions are inherited unchanged: Java 25, Spring Boot 4.1.1, PostgreSQL 18.6, Flyway, `JdbcClient`,
  Jackson 3 (`tools.jackson`), Testcontainers 2.0.5, and native HTML, CSS, and ES modules with no Node, npm, framework,
  or bundler.
- Follow the existing service shape: a `DefinitionDraftService` beside `ManualDraftService` and `RepairDraftService`,
  loading through `WorkspaceRepository`, checking versions with `ImportService.requireMatchingVersion`, mutating through
  new `WorkspaceMutation` methods, and signalling failures with `WorkspaceProblem`.
- Issue evaluation is a pure, Spring-free class (`DefinitionDraftChecks`) so it can be table-tested without a database.
- A new Flyway migration `V3__add_definition_draft_state.sql` extends the `lifecycle_state` check constraint.
- New browser code lives in dedicated ES modules (`definition-editor.js`, `period-grid.js`) imported by `app.js`. All
  text lives in `messages.js`. Styling goes through `styles.css` classes and the `hidden` attribute, never inline styles,
  to satisfy the existing CSP.
- Existing ID, canonical-JSON, and revision helpers (`CanonicalJson`, the `sha256:` revision form) are reused. They are
  not reimplemented.

## Security surface

Existing protections are unchanged: loopback binding, Host and Origin validation, session-bound CSRF,
`Cache-Control: no-store`, the CSP, and the stable problem shape.

| Route and method | Access and protection | Feature relevance |
|---|---|---|
| `GET /api/workspace` | Local, no CSRF; emits `ETag` | Snapshot includes `definitionDraft`, its issues, and the latest verification outcome |
| `POST /api/definition-draft` | Local, CSRF and `If-Match` required | UC-1: open a draft from `INITIAL_DRAFT` or `ACCEPTED_BASELINE` |
| `PATCH /api/definition-draft` | Local, CSRF and `If-Match` required | UC-2, UC-3, UC-4, UC-8: add, update, revert, remove, and reassign commands |
| `DELETE /api/definition-draft` | Local, CSRF and `If-Match` required; body `{"confirmed": true}` | UC-6: discard and return to the source state |
| `POST /api/definition-draft/finish` | Local, CSRF and `If-Match` required | UC-7: verify and hand off |
| `DELETE /api/repair-draft` | Unchanged protection | Returns to `DEFINITION_DRAFT` when `repairDraft.authoredDraft` is present (RULE-9) |
| `POST /api/import`, `POST /api/initial-draft/replace`, `POST /api/workspace/upload-definition`, `POST /api/workspace/clear`, `DELETE /api/workspace`, `/api/runs/**`, `/api/repair-draft/**`, `/api/manual-draft/**`, `/api/proposal/**` mutations | Unchanged protection | Refused with `409 INVALID_WORKSPACE_TRANSITION` while in `DEFINITION_DRAFT` |

## Verification strategy

- **Unit:** `DefinitionDraftChecks` is table-tested per issue code, with positive, negative, and attribution cases.
  Command application is tested for every attribute in the normative table, for omission normalization, and for catalog
  gating. Dependent discovery is tested for every row of *Removal dependents*.
- **Kernel:** `VerifyCliIT` and `DefinitionLoader` tests cover successor mode: success, wrong `basedOnRevision`, school
  mismatch, non-`FEASIBLE` current, option misuse, and zero solver construction.
- **Integration:** `WorkspaceDefinitionDraftIT` drives the real HTTP stack against Testcontainers PostgreSQL 18.6. For
  every command it asserts the database document before and after, source byte-identity, version increments, and the
  412, 428, 409, and 422 paths. Finish runs against the packaged kernel process.
- **Repair seam:** `WorkspaceRepairDraftIT` and `WorkspaceRepairPlanningIT` gain cases for a repair draft seeded from
  an authored successor: staging, pins, direct effects, compilation, and acceptance.
- **Browser:** `DefinitionAuthoringBrowserIT` uses headless Chrome to cover each UC main path plus keyboard-only
  period-grid editing, issue navigation, confirmation dialogs, and the absence of inline styles and inline strings.
- **Acceptance:** `DefinitionAuthoringAcceptanceIT` runs the spec's three acceptance scenarios end to end against the
  packaged kernel. The MVK home-room scenario starts from an accepted baseline produced by planning `examples/mvk.json`
  with the packaged kernel (or a recorded result for it) and asserts that only cohort `6c` differs in the accepted
  successor definition.
- Tests use isolated containers, temporary directories, and ephemeral ports, and never modify `examples/`.

## Rules

### RULE-1 - Lifecycle state, source tracking, and migration

- Applies to: UC-1, UC-6, UC-7
- Constraint: `WorkspaceState` MUST include `DEFINITION_DRAFT`, and `V3__add_definition_draft_state.sql` MUST add
  `'DEFINITION_DRAFT'` to the `lifecycle_state` check constraint. Opening is allowed only from `INITIAL_DRAFT` or
  `ACCEPTED_BASELINE`. From `EMPTY` it MUST fail with `409 DEFINITION_SOURCE_REQUIRED`, and from any other state with
  `409 INVALID_WORKSPACE_TRANSITION`. The draft MUST record `sourceState` and `sourceDefinitionRevision`. Discard MUST
  return exactly to `sourceState`. Every route in the security table marked as refused MUST return `409` without
  mutation while the state is `DEFINITION_DRAFT`, and its problem detail MUST name the open definition draft. Restart
  recovery MUST leave `DEFINITION_DRAFT` untouched. The one additional entry into `DEFINITION_DRAFT` is
  `REPAIR_DRAFT` -> `DEFINITION_DRAFT` through repair discard (RULE-9). The restored draft keeps its original
  `sourceState` of `ACCEPTED_BASELINE`.
- Reason: The spec's state model is closed, and the draft may only return to where it came from.
- Verification: Migration applied to fresh and V2 databases. A transition matrix test for every state against every
  definition-draft route and every refused route asserts status and zero mutation.

### RULE-2 - Draft storage and revision

- Applies to: all use cases
- Constraint: The draft MUST be stored as `document.definitionDraft` with exactly `sourceState`,
  `sourceDefinitionRevision`, `sourceCatalogVersion`, `openedRevision`, `definition` (a complete school-definition
  document), `draftRevision`, `issues`, and an optional `lastVerification`. `draftRevision` MUST be
  `sha256:<lowercase-hex>` over the canonical JSON of `definition`, not a timestamp. `openedRevision` MUST be the
  `draftRevision` computed at open, after `basedOnRevision` and `catalogVersion` are set, and MUST NOT change
  afterwards. "Unchanged" in the spec means `draftRevision` equals `openedRevision`. `sourceDefinitionRevision` MUST
  NOT be used for that comparison, because for an accepted source the opened draft already differs from the source.
  `sourceCatalogVersion` MUST be the source definition's `catalogVersion`. `document.initialDefinition` and
  `document.acceptedBaseline` MUST remain byte-identical while the draft exists. For an accepted source, the draft
  definition MUST carry `basedOnRevision` equal to `acceptedBaseline.result.inputRevision` and `catalogVersion` equal
  to `max(8, accepted catalogVersion)`. For an initial source, it MUST carry neither `basedOnRevision` nor a changed
  catalog version.
- Reason: A content-derived revision makes staleness checks (RULE-7) exact, and an untouched source makes discard free.
- Verification: IT asserts the stored shape, recomputes `draftRevision`, and byte-compares the source nodes after every
  command, discard, and failed finish. A dedicated case opens from an accepted catalog-3 baseline and asserts that
  `draftRevision` differs from `sourceDefinitionRevision` but equals `openedRevision`.

### RULE-3 - Typed resource commands with optimistic concurrency

- Applies to: UC-2, UC-3, UC-4
- Constraint: `PATCH /api/definition-draft` MUST accept exactly one command per request:
  `{"command":"ADD_RESOURCE","resourceType":…,"resource":{…}}`,
  `{"command":"UPDATE_RESOURCE","resourceType":…,"resourceId":…,"set":{…},"clear":[…]}`,
  `{"command":"REVERT_RESOURCE","resourceType":…,"resourceId":…,"attributes":[…]}` (with `attributes` omitted for a
  whole-resource revert), `{"command":"REMOVE_RESOURCE",…,"confirmed":true}`, or
  `{"command":"REASSIGN_TEACHER","fromTeacherId":…,"toTeacherId":…,"lessonIds":[…],"roomAssignmentIds":[…]}`, where
  `resourceType` is `COHORT`, `TEACHER`, or `ROOM`. Every mutation MUST require a matching `If-Match` (`428` if absent, `412
  STALE_WORKSPACE_VERSION` if stale) and CSRF. It MUST increment the version atomically, and a failed request MUST NOT
  change the stored draft. A malformed command MUST return `400`. An ID that breaks the contract pattern or duplicates
  one in the draft MUST return `422 INVALID_RESOURCE_ID`. Removing a referenced resource MUST return
  `409 RESOURCE_HAS_DEPENDENTS`, listing every dependent as `{kind, id, attribute, resolvedBy}`, where `resolvedBy` is
  `REASSIGN_TEACHER`, `UPDATE_RESOURCE`, or `NONE` per the spec's *Removal dependents* table. `REVERT_RESOURCE` on a
  resource absent from the source MUST return `409`. A revert `attributes` entry outside the *Editable attributes*
  table, or equal to `id`, MUST return `422 UNSUPPORTED_ATTRIBUTE`. `REASSIGN_TEACHER` MUST return
  `422 INVALID_REASSIGNMENT` when either teacher is absent from the draft, the two teachers are equal, both lists are
  empty, a list contains duplicates, or a listed lesson or rule does not currently name `fromTeacherId`.
  Content-level invalidity (missing values, unknown references, unqualified target teachers, and so on) MUST NOT reject
  the command. It becomes issues (RULE-5).
- Reason: Persist-invalid is a confirmed decision. Only requests that are structurally unsafe or ambiguous are rejected.
- Verification: IT for each command with success, stale, missing `If-Match`, CSRF absent, malformed, duplicate ID,
  dependents-present, and every `INVALID_REASSIGNMENT` path, asserting the response and the database.

### RULE-4 - Resource-scoped edits, catalog gating, and omission normalization

- Applies to: UC-2, UC-3
- Constraint: Commands MUST touch only the addressed element of `cohorts[]`, `teachers[]`, or `rooms[]`, and only the
  attributes listed in the spec's *Editable attributes* table. `id` MUST be settable only by `ADD_RESOURCE`. An attribute
  whose minimum catalog is above the draft's `catalogVersion` MUST return `422 UNSUPPORTED_ATTRIBUTE`, and so must any
  attribute not in the table. Commands MUST NOT modify `subjects`, `periods`, `reservedPeriodIds`,
  `softConstraintOverrides`, or top-level fields. `lessons` and `roomAssignments` MAY be modified only by
  `REASSIGN_TEACHER`, and only in the `teacherId` of the listed elements, keeping their array positions.
  Attribute revert MUST restore each listed attribute to its source value, removing it when the source omits it, and
  MUST leave every other attribute untouched. A cleared attribute MUST be removed from the
  element. A period set equal to all declared periods MUST be stored as an omitted `availablePeriodIds`. A new or
  changed period set MUST be written in declared period order (weekday, then `order`), and arrays that were not edited
  MUST keep their original order. An added resource MUST be appended at the end of its collection. A reverted resource
  MUST equal its source element and keep its original array position.
- Reason: This keeps edits resource-scoped (spec UC-3 G4), omissions intact, and diffs minimal, so canonical revisions
  change only when meaning changes.
- Verification: Table-driven tests for every attribute row, covering set, clear, gated, unknown, and attribute revert.
  A byte-diff test proves untouched collections and elements are identical, including lessons and rules not listed in
  a reassignment. Reassign-and-back round-trip test. Omission round-trip tests.

### RULE-5 - Server-authoritative workspace checks

- Applies to: UC-2, UC-3, UC-4, UC-5, UC-7
- Constraint: After every successful command and on open, `DefinitionDraftChecks` MUST recompute the complete
  `issues` array. Each issue MUST be `{code, severity, resourceType, resourceId, attribute, dependents[], messageKey}`
  using exactly the codes and severities of the spec's *Issue codes* table, except `KERNEL_REJECTED`, which only RULE-7
  produces. `resourceType` is `COHORT`, `TEACHER`, `ROOM`, or, for `CAPABILITY_NOT_PROVIDED` only, `CAPABILITY` with
  the capability ID as `resourceId`. An issue in a lesson that was caused by a resource attribute (a qualification,
  availability, capacity, capabilities, curator, or home room) MUST be attributed to that resource and attribute, with
  the lessons in `dependents`. Lesson checks (`TEACHER_NOT_QUALIFIED`, `CURATOR_REQUIRED`,
  `LOCK_CONTRADICTS_AVAILABILITY`) MUST use `lessons[].teacherId` from the draft. The following codes MUST be computed
  as specified:
  - `LOAD_EXCEEDS_AVAILABILITY`: per teacher and per cohort, the count of lessons naming it exceeds the count of
    declared periods in its effective available set (all declared periods when omitted, reserved periods included).
    Attributed to `availablePeriodIds`, with the lessons as dependents.
  - `NO_ROOM_FITS_COHORT`: a cohort with at least one lesson has `size` greater than the maximum `capacity` of all
    declared rooms. Attributed to the cohort's `size`, with its lessons as dependents.
  - `CAPABILITY_NOT_PROVIDED`: one issue per capability that at least one lesson's `requiredRoomCapabilityIds`
    contains and no room's `capabilityIds` contains, compared exactly, with the lessons as dependents.
  - `CATALOG_RAISED`: only for an accepted source with `sourceCatalogVersion` below the draft's `catalogVersion`. One
    issue per cohort and per omitted attribute among `maxDailyLessonSpread` (min 4), `maxDailyGaps` (min 5), and
    `preferredLatestStartSlot` (min 6) whose min catalog is above `sourceCatalogVersion` and at or below the draft's
    `catalogVersion`. It disappears once the attribute is set.
  - `ASSIGNMENT_TEACHER_DIVERGES`: only for an accepted source. One issue per definition teacher, listing every lesson
    whose accepted assignment `teacherId` differs from the draft's `lessons[].teacherId`, attributed to that definition
    teacher with `attribute` null.
  - `ACCEPTED_ASSIGNMENT_AFFECTED`: only for an accepted source, by evaluating each accepted assignment's period and
    room with the lesson's draft `teacherId` (the teacher repair planning will assign) against the draft's teacher,
    cohort, and room availability, room capacity and capabilities, and home rooms. A lesson is also affected when its
    draft teacher differs from the assignment's and the draft teacher already teaches another accepted lesson in that
    period, or when its room is absent from the draft.
  Issue order MUST be deterministic: severity, then resource type (`COHORT`, `TEACHER`, `ROOM`, `CAPABILITY`), then
  resource ID, then code. Checks MUST NOT call the kernel process.
- Reason: Fast, exact, attributable feedback on every save. The kernel stays the final authority.
- Verification: Unit table per code with positive, negative, and attribution cases, including a case-only capability
  mismatch (`Lab` against `lab`) for `CAPABILITY_NOT_PROVIDED`, catalog 3 to 8 and catalog 9 to 9 cases for
  `CATALOG_RAISED`, and a manually swapped assignment for `ASSIGNMENT_TEACHER_DIVERGES`. An MVK-scale timing test
  records check duration as evidence, not as a gate.

### RULE-6 - Kernel successor-definition verification

- Applies to: UC-7
- Constraint: `school-kernel verify` MUST accept `--definition PATH --current PATH`, mutually exclusive with `--result`
  (misuse exits `64`). In this mode it MUST validate the definition with replan semantics
  (`DefinitionValidator.validateForReplan`) and read `--current` as a complete `FEASIBLE` result. It MUST require
  matching `schoolId` and `basedOnRevision` equal to the current `inputRevision`, reporting violations through the
  existing validation-report shape at `/basedOnRevision` and `/schoolId`. It MUST NOT construct or run the solver or
  verify the current assignments against the new definition. `verification-result-v1` MUST add the mode value
  `SUCCESSOR_DEFINITION`. `VERIFIED` in that mode requires `schoolId`, `catalogVersion`, and `definitionRevision` and
  forbids `timetableRevision`. Existing modes, statuses, exit codes, and outputs MUST be unchanged.
- Reason: Definition-only verification rejects `basedOnRevision`, and baseline verification requires the result to
  match the new definition. Neither can vet a successor before repair, and stripping lineage in the workspace would
  verify a different document than the one repair submits.
- Verification: `VerifyCliIT` success and each failure, option-misuse exit `64`, schema conditional validation, a
  zero-solver assertion, and regression runs of every existing verify test.

### RULE-7 - Structured verification outcome and staleness

- Applies to: UC-5, UC-7
- Constraint: `KernelVerifier` MUST expose the complete validation-report errors (`location`, `entityIds`, `message`),
  bounded to the first 200 entries with a truncation flag, in addition to today's single safe message. Finish MUST
  always invoke verification of the exact draft definition, and MUST NOT reuse an earlier outcome. The outcome MUST be
  stored as `lastVerification {draftRevision, status, errors[], truncated}`. Each error whose JSON pointer is inside a
  cohort, teacher, or room element, or inside a lesson whose failure maps to a resource attribute as in RULE-5, MUST be
  attributed to that resource and attribute. Every other error becomes a `KERNEL_REJECTED` issue carrying the
  kernel-authored message. The snapshot MUST mark `lastVerification` as outdated when its `draftRevision` differs from
  the current one. `INTERNAL_ERROR` or transport failure MUST return `503` and store no verified status.
- Reason: The administrator needs every kernel objection placed where it can be fixed, not just the first one, and a
  stale "verified" badge would be dishonest.
- Verification: Verifier unit tests with multi-error fixtures and truncation. IT asserting the mapping, the
  outdated flag after a later edit, and the 503 path storing nothing verified.

### RULE-8 - Atomic, source-specific handoff

- Applies to: UC-7
- Constraint: Finish MUST return `409 NOTHING_TO_FINISH` when `draftRevision` equals `openedRevision`, and
  `422 BLOCKING_ISSUES` (with the count) when any blocking issue exists, both before the kernel is invoked. On
  `VERIFIED`, one database transaction MUST remove `definitionDraft` and do one of the following:
  (a) for an initial source, write `initialDefinition`, `definitionRevision`, and `school` exactly as
  `ImportService.replaceInitial` does, and enter `INITIAL_DRAFT`;
  (b) for an accepted source, create `repairDraft` with empty `changes`, `pins`, and `bulkActions`, a
  `baseDefinition` set to the verified successor, `baseDefinitionRevision`, `authoredChangeCount`, the lesson IDs
  flagged `ACCEPTED_ASSIGNMENT_AFFECTED` as seeded direct effects, and `authoredDraft` set to the complete removed
  `definitionDraft` node (including `lastVerification`), and enter `REPAIR_DRAFT`.
  `acceptedBaseline` MUST remain byte-identical. A failed transaction MUST leave the draft and source untouched.
- Reason: All-or-nothing progression (spec UC-7 G3). This reuses the one existing initial-replacement semantics
  instead of forking it.
- Verification: IT for both sources, asserting the resulting document shape, source immutability, and version.
  Fault-injection IT for a storage failure mid-handoff.

### RULE-9 - Repair drafts honor an authored base definition

- Applies to: UC-7 (downstream in `timetable-workspace` UC-4 to UC-6)
- Constraint: When `repairDraft.baseDefinition` is present, `RepairDraftService` staging, direct-effect, conflict, and
  bulk-pin logic, and `compiledDefinition`, MUST use it in place of `acceptedBaseline.definition`. Compilation MUST start
  from a deep copy of `baseDefinition`. It MUST NOT re-derive `basedOnRevision` or re-raise `catalogVersion`, and it MUST
  still clear prior attempt-scoped locks and apply staged intent. Accepted assignments remain the stability reference.
  `intentRevision` MUST cover `baseDefinitionRevision`. Seeded direct effects MUST merge with staged ones.
  `authoredDraft` MUST be carried unchanged through `SOLVING_REPAIR`, `REPAIR_PROPOSAL`, proposal rejection, and
  restart recovery. When `authoredDraft` is present, `DELETE /api/repair-draft` MUST, in one transaction, remove
  `repairDraft`, write `authoredDraft` back as `document.definitionDraft` byte-identical to how it was stored, and
  enter `DEFINITION_DRAFT`. Its confirmation MUST state that staged changes and pins will be discarded and that the
  `authoredChangeCount` authored changes will be kept in the definition draft. Accepting the repair proposal MUST
  remove `authoredDraft` together with `repairDraft`. Without `baseDefinition`, behavior MUST be byte-identical to
  today's.
- Reason: The repair workflow is the only path from successor definition to accepted baseline. It must plan the
  authored successor rather than silently plan the old definition. Discarding a repair attempt must not cost the
  administrator their authoring work.
- Verification: Existing repair ITs pass unchanged. New cases prove a new teacher or room reaches the compiled
  successor and the accepted proposal, that a reassigned lesson is taught by its new teacher in the proposal, and
  that seeded direct effects appear. Discard cases cover return from `REPAIR_DRAFT` (byte-identical restored draft,
  accepted baseline untouched, discard message shown), survival across a restart and a rejected proposal, and
  removal of `authoredDraft` on acceptance.

### RULE-10 - Native, accessible authoring UI

- Applies to: UC-1 to UC-7
- Constraint: The editor MUST be native ES modules with no new dependencies. It MUST render only server snapshot data:
  resources, issues, the diff against the source, and `lastVerification`. It MUST send only RULE-3 commands carrying
  the latest `ETag`. It MUST NOT reconstruct definitions or evaluate issues. It MUST show every *Editable attributes* row,
  disable catalog-gated rows with the required catalog, and describe omitted values by their contract meaning from
  `messages.js`. The period grid MUST be keyboard-operable: arrow-key roving focus, Space to toggle, and whole-weekday or
  whole-slot toggles. It MUST expose state with `aria-pressed` or `aria-checked`, and mark reserved periods with text
  as well as styling. Added, modified, removed, reassigned, blocking, advisory, and outdated states MUST be
  distinguishable without color. Destructive actions MUST use a confirmation dialog that returns focus. While the state
  is `DEFINITION_DRAFT`, every workspace view MUST show a persistent indicator linking to the editor, and controls for
  refused workflows MUST explain the refusal instead of failing silently. The ID suggestion MUST implement the spec's
  *Identifier suggestion* rule exactly, using `String.prototype.normalize("NFKD")`. The reassignment dialog MUST show
  the unqualified-subject and curator-lesson warnings from UC-8 step 4 before confirmation. All strings MUST come from
  `messages.js`, and there MUST be no inline style attributes.
- Reason: Consistency with the existing workspace UI rules and CSP, and fidelity to spec UC-3 G1 to G3.
- Verification: Browser IT covering keyboard-only journeys, ARIA assertions, a scan for inline styles and strings,
  gated-attribute rendering at catalog 3 and catalog 9 fixtures, the open-draft indicator on every view, and an
  ID-suggestion table including `Õpiabi`, `Liivi Kivimäe-Bondarev`, `Šaal`, a blank result, and a collision.

### RULE-11 - Safe failures and logging

- Applies to: all use cases
- Constraint: All failures MUST use the existing problem shape and status mapping, with the new stable codes
  `DEFINITION_SOURCE_REQUIRED`, `INVALID_RESOURCE_ID`, `RESOURCE_HAS_DEPENDENTS`, `UNSUPPORTED_ATTRIBUTE`,
  `INVALID_REASSIGNMENT`, `NOTHING_TO_FINISH`, and `BLOCKING_ISSUES`. Logs MAY contain the command kind, resource type, issue counts by
  severity, verification status, and elapsed time. They MUST NOT contain display names, resource IDs, attribute values,
  or definition content.
- Reason: Preserves the existing no-disclosure logging and deterministic recovery rules.
- Verification: Controller tests for every new code. A log-capture test over a full authoring journey scans for
  display names and IDs from the fixture.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-5, RULE-10, RULE-11 |
| UC-2 | RULE-2, RULE-3, RULE-4, RULE-5, RULE-10, RULE-11 |
| UC-3 | RULE-2, RULE-3, RULE-4, RULE-5, RULE-10, RULE-11 |
| UC-4 | RULE-2, RULE-3, RULE-5, RULE-10, RULE-11 |
| UC-5 | RULE-5, RULE-7, RULE-10 |
| UC-6 | RULE-1, RULE-2, RULE-10, RULE-11 |
| UC-7 | RULE-1, RULE-5, RULE-6, RULE-7, RULE-8, RULE-9, RULE-10, RULE-11 |
| UC-8 | RULE-2, RULE-3, RULE-4, RULE-5, RULE-10, RULE-11 |

## Design exclusions

- No client-side issue evaluation or definition assembly.
- No batch or multi-command PATCH. `REASSIGN_TEACHER` is one command over several items, not a batch of commands.
  Undo beyond resource and attribute revert, and reassigning back, is not provided.
- No catalog upgrade, ID rename, or edits outside cohorts, teachers, and rooms, except `teacherId` through
  `REASSIGN_TEACHER`.
- No change to manual-draft publication.
- No reuse of a stored verification outcome at finish.
- No change to kernel solving, replanning, or the school-definition schema.
