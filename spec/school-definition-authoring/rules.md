# Technical Rules: School Definition Authoring

## Design overview

Definition authoring adds a `DEFINITION_DRAFT` lifecycle state to the singleton `workspace_aggregate`. Opening a draft
(`POST /api/definition-draft`) deep-copies the source definition, either `document.initialDefinition` or
`document.acceptedBaseline.definition`, into `document.definitionDraft`, together with the source state, the source
definition revision, the effective catalog version, and, for an accepted source, `basedOnRevision`. The source nodes
are never written while the draft exists.

Edits are typed resource commands sent to `PATCH /api/definition-draft`: add, update, revert, and remove for cohorts,
teachers, and rooms. The server applies each command to the draft definition, normalizes omission, re-runs a
deterministic workspace check pass that produces the normative issue list, and persists everything in one optimistic
transaction. The browser renders the snapshot's issues and never computes them.

Finishing (`POST /api/definition-draft/finish`) always invokes School Kernel verification of the exact complete draft.
An initial source uses the existing definition-only `verify`. An accepted source uses a new kernel verification mode
for successor definitions, which checks lineage against the accepted result without solving. On `VERIFIED`, one
transaction either replaces the initial definition (as `POST /api/initial-draft/replace` does) or creates a repair
draft whose base definition is the verified successor. The existing repair services then stage, pin, compile, solve,
and accept from that base instead of the accepted definition.

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
| `PATCH /api/definition-draft` | Local, CSRF and `If-Match` required | UC-2, UC-3, UC-4: add, update, revert, and remove resource commands |
| `DELETE /api/definition-draft` | Local, CSRF and `If-Match` required; body `{"confirmed": true}` | UC-6: discard and return to the source state |
| `POST /api/definition-draft/finish` | Local, CSRF and `If-Match` required | UC-7: verify and hand off |
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
- Tests use isolated containers, temporary directories, and ephemeral ports, and never modify `examples/`.

## Rules

### RULE-1 - Lifecycle state, source tracking, and migration

- Applies to: UC-1, UC-6, UC-7
- Constraint: `WorkspaceState` MUST include `DEFINITION_DRAFT`, and `V3__add_definition_draft_state.sql` MUST add
  `'DEFINITION_DRAFT'` to the `lifecycle_state` check constraint. Opening is allowed only from `INITIAL_DRAFT` or
  `ACCEPTED_BASELINE`. From `EMPTY` it MUST fail with `409 DEFINITION_SOURCE_REQUIRED`, and from any other state with
  `409 INVALID_WORKSPACE_TRANSITION`. The draft MUST record `sourceState` and `sourceDefinitionRevision`. Discard MUST
  return exactly to `sourceState`. Every route in the security table marked as refused MUST return `409` without
  mutation while the state is `DEFINITION_DRAFT`. Restart recovery MUST leave `DEFINITION_DRAFT` untouched.
- Reason: The spec's state model is closed, and the draft may only return to where it came from.
- Verification: Migration applied to fresh and V2 databases. A transition matrix test for every state against every
  definition-draft route and every refused route asserts status and zero mutation.

### RULE-2 - Draft storage and revision

- Applies to: all use cases
- Constraint: The draft MUST be stored as `document.definitionDraft` with exactly `sourceState`,
  `sourceDefinitionRevision`, `definition` (a complete school-definition document), `draftRevision`, `issues`, and an
  optional `lastVerification`. `draftRevision` MUST be `sha256:<lowercase-hex>` over the canonical JSON of `definition`,
  not a timestamp. `document.initialDefinition` and `document.acceptedBaseline` MUST remain byte-identical while the draft
  exists. For an accepted source, the draft definition MUST carry `basedOnRevision` equal to
  `acceptedBaseline.result.inputRevision` and `catalogVersion` equal to `max(8, accepted catalogVersion)`. For an initial
  source, it MUST carry neither `basedOnRevision` nor a changed catalog version.
- Reason: A content-derived revision makes staleness checks (RULE-7) exact, and an untouched source makes discard free.
- Verification: IT asserts the stored shape, recomputes `draftRevision`, and byte-compares the source nodes after every
  command, discard, and failed finish.

### RULE-3 - Typed resource commands with optimistic concurrency

- Applies to: UC-2, UC-3, UC-4
- Constraint: `PATCH /api/definition-draft` MUST accept exactly one command per request:
  `{"command":"ADD_RESOURCE","resourceType":…,"resource":{…}}`,
  `{"command":"UPDATE_RESOURCE","resourceType":…,"resourceId":…,"set":{…},"clear":[…]}`,
  `{"command":"REVERT_RESOURCE",…}`, or `{"command":"REMOVE_RESOURCE",…,"confirmed":true}`, where `resourceType` is
  `COHORT`, `TEACHER`, or `ROOM`. Every mutation MUST require a matching `If-Match` (`428` if absent, `412
  STALE_WORKSPACE_VERSION` if stale) and CSRF. It MUST increment the version atomically, and a failed request MUST NOT
  change the stored draft. A malformed command MUST return `400`. An ID that breaks the contract pattern or duplicates
  one in the draft MUST return `422 INVALID_RESOURCE_ID`. Removing a referenced resource MUST return
  `409 RESOURCE_HAS_DEPENDENTS`, listing every dependent as `{kind, id, attribute}`. `REVERT_RESOURCE` on a resource
  absent from the source MUST return `409`. Content-level invalidity (missing values, unknown references, and so on)
  MUST NOT reject the command. It becomes issues (RULE-5).
- Reason: Persist-invalid is a confirmed decision. Only requests that are structurally unsafe or ambiguous are rejected.
- Verification: IT for each command with success, stale, missing `If-Match`, CSRF absent, malformed, duplicate ID, and
  dependents-present paths, asserting the response and the database.

### RULE-4 - Resource-scoped edits, catalog gating, and omission normalization

- Applies to: UC-2, UC-3
- Constraint: Commands MUST touch only the addressed element of `cohorts[]`, `teachers[]`, or `rooms[]`, and only the
  attributes listed in the spec's *Editable attributes* table. `id` MUST be settable only by `ADD_RESOURCE`. An attribute
  whose minimum catalog is above the draft's `catalogVersion` MUST return `422 UNSUPPORTED_ATTRIBUTE`, and so must any
  attribute not in the table. Commands MUST NOT modify `lessons`, `subjects`, `periods`, `reservedPeriodIds`,
  `roomAssignments`, `softConstraintOverrides`, or top-level fields. A cleared attribute MUST be removed from the
  element. A period set equal to all declared periods MUST be stored as an omitted `availablePeriodIds`. A new or
  changed period set MUST be written in declared period order (weekday, then `order`), and arrays that were not edited
  MUST keep their original order. An added resource MUST be appended at the end of its collection. A reverted resource
  MUST equal its source element and keep its original array position.
- Reason: This keeps edits resource-scoped (spec UC-3 G4), omissions intact, and diffs minimal, so canonical revisions
  change only when meaning changes.
- Verification: Table-driven tests for every attribute row, covering set, clear, gated, and unknown. A byte-diff test
  proves untouched collections and elements are identical. Omission round-trip tests.

### RULE-5 - Server-authoritative workspace checks

- Applies to: UC-2, UC-3, UC-4, UC-5, UC-7
- Constraint: After every successful command and on open, `DefinitionDraftChecks` MUST recompute the complete
  `issues` array. Each issue MUST be `{code, severity, resourceType, resourceId, attribute, dependents[], messageKey}`
  using exactly the codes and severities of the spec's *Issue codes* table, except `KERNEL_REJECTED`, which only RULE-7
  produces. An issue in a lesson that was caused by a resource attribute (a qualification, availability, capacity,
  capabilities, curator, or home room) MUST be attributed to that resource and attribute, with the lessons in
  `dependents`. `ACCEPTED_ASSIGNMENT_AFFECTED` MUST be computed only for an accepted source, by evaluating accepted
  assignments against the draft's teacher, cohort, and room availability, room capacity and capabilities, and home
  rooms. Issue order MUST be deterministic: severity, then resource type, then resource ID, then code. Checks MUST NOT
  call the kernel process.
- Reason: Fast, exact, attributable feedback on every save. The kernel stays the final authority.
- Verification: Unit table per code with positive, negative, and attribution cases. An MVK-scale timing test records
  check duration as evidence, not as a gate.

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
- Constraint: Finish MUST return `409 NOTHING_TO_FINISH` when `draftRevision` equals `sourceDefinitionRevision`, and
  `422 BLOCKING_ISSUES` (with the count) when any blocking issue exists, both before the kernel is invoked. On
  `VERIFIED`, one database transaction MUST remove `definitionDraft` and do one of the following:
  (a) for an initial source, write `initialDefinition`, `definitionRevision`, and `school` exactly as
  `ImportService.replaceInitial` does, and enter `INITIAL_DRAFT`;
  (b) for an accepted source, create `repairDraft` with empty `changes`, `pins`, and `bulkActions`, a
  `baseDefinition` set to the verified successor, `baseDefinitionRevision`, `authoredChangeCount`, and the lesson IDs
  flagged `ACCEPTED_ASSIGNMENT_AFFECTED` as seeded direct effects, and enter `REPAIR_DRAFT`.
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
  `intentRevision` MUST cover `baseDefinitionRevision`. Seeded direct effects MUST merge with staged ones. The repair
  discard confirmation MUST state that `authoredChangeCount` authored resource changes will be discarded. Without
  `baseDefinition`, behavior MUST be byte-identical to today's.
- Reason: The repair workflow is the only path from successor definition to accepted baseline. It must plan the
  authored successor rather than silently plan the old definition.
- Verification: Existing repair ITs pass unchanged. New cases prove a new teacher or room reaches the compiled
  successor and the accepted proposal, that seeded direct effects appear, and that the discard message is shown.

### RULE-10 - Native, accessible authoring UI

- Applies to: UC-1 to UC-7
- Constraint: The editor MUST be native ES modules with no new dependencies. It MUST render only server snapshot data:
  resources, issues, the diff against the source, and `lastVerification`. It MUST send only RULE-3 commands carrying
  the latest `ETag`. It MUST NOT reconstruct definitions or evaluate issues. It MUST show every *Editable attributes* row,
  disable catalog-gated rows with the required catalog, and describe omitted values by their contract meaning from
  `messages.js`. The period grid MUST be keyboard-operable: arrow-key roving focus, Space to toggle, and whole-weekday or
  whole-slot toggles. It MUST expose state with `aria-pressed` or `aria-checked`, and mark reserved periods with text
  as well as styling. Added, modified, removed, blocking, advisory, and outdated states MUST be distinguishable without
  color. Destructive actions MUST use a confirmation dialog that returns focus. All strings MUST come from `messages.js`,
  and there MUST be no inline style attributes.
- Reason: Consistency with the existing workspace UI rules and CSP, and fidelity to spec UC-3 G1 to G3.
- Verification: Browser IT covering keyboard-only journeys, ARIA assertions, a scan for inline styles and strings, and
  gated-attribute rendering at catalog 3 and catalog 9 fixtures.

### RULE-11 - Safe failures and logging

- Applies to: all use cases
- Constraint: All failures MUST use the existing problem shape and status mapping, with the new stable codes
  `DEFINITION_SOURCE_REQUIRED`, `INVALID_RESOURCE_ID`, `RESOURCE_HAS_DEPENDENTS`, `UNSUPPORTED_ATTRIBUTE`,
  `NOTHING_TO_FINISH`, and `BLOCKING_ISSUES`. Logs MAY contain the command kind, resource type, issue counts by
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

## Design exclusions

- No client-side issue evaluation or definition assembly.
- No batch or multi-command PATCH. Undo beyond per-resource revert is not provided.
- No catalog upgrade, ID rename, or edits outside cohorts, teachers, and rooms.
- No reuse of a stored verification outcome at finish.
- No change to kernel solving, replanning, or the school-definition schema.
