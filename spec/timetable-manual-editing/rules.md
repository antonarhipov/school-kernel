# Technical Rules: Timetable Manual Editing

## Design overview

The manual editing workflow integrates into `timetable-workspace` alongside the accepted baseline. An administrator enters `MANUAL_DRAFT` state via `POST /api/manual-draft`. The server persists active manual modifications and current conflict reports inside the JSONB `document.manualDraft` field of `workspace_aggregate`, incrementing version and verifying optimistic concurrency (`ETag`/`If-Match`).

Mutations (`PATCH /api/manual-draft`) update lesson assignments (period, room, teacher) or revert individual lessons. Each mutation triggers authoritative server-side conflict evaluation across all six normative constraints: teacher availability/clash, room availability/clash, cohort clash, and room policy compliance. A lightweight client-side validator mirrors these rules to provide immediate UI feedback, highlighting conflicting cells and rendering explanatory overlays.

Discarding (`DELETE /api/manual-draft`) purges `manualDraft` and restores `ACCEPTED_BASELINE`. Publishing (`POST /api/manual-draft/publish`) verifies that the active conflicts count is zero, invokes the School Kernel verifier (`BaselineVerifier`) against the candidate timetable, atomically increments the baseline revision, and advances `ACCEPTED_BASELINE`. All operations use existing loopback HTTP and CSRF protections without external runtime dependencies.

## Codebase alignment

- Implementation resides within the `timetable-workspace` Maven module, consuming `kernel-cli` and `kernel-contract` domain models and verifiers.
- Stack conventions inherited: Java 25, Spring Boot 4.1.1, PostgreSQL 18.6, Flyway, `JdbcClient`, Jackson 3, Testcontainers 2.0.5, native HTML5/CSS3 and vanilla ES modules. Node, npm, webpack, and frontend frameworks remain strictly excluded.
- A new Flyway migration `V2__add_manual_draft_state.sql` updates the `workspace_aggregate.lifecycle_state` check constraint to include `'MANUAL_DRAFT'`.
- All user-facing labels and conflict messages are localized in `messages.js`.
- Security boundaries remain unchanged: loopback-only binding, Host/Origin validation, session-bound CSRF tokens, and `Cache-Control: no-store`.

## Security surface

| Route and method | Access and protection | Feature relevance |
|---|---|---|
| `GET /` | Local loopback only; redirects to `/workspace/` | Entry point |
| `GET /workspace/**` | Local loopback only; static same-origin assets | Serves UI components, CSS, and ES modules |
| `GET /api/csrf` | Local loopback only; no school data | Supplies session CSRF token for mutations |
| `GET /api/workspace` | Local loopback only; `ETag`, `Cache-Control: no-store` | Supplies snapshot including `manualDraft` and conflicts |
| `POST /api/manual-draft` | Local same-origin; CSRF and matching `If-Match` | Starts manual editing draft from `ACCEPTED_BASELINE` |
| `PATCH /api/manual-draft` | Local same-origin; CSRF and matching `If-Match` | Reassigns lesson period/room/teacher or reverts lesson |
| `DELETE /api/manual-draft` | Local same-origin; CSRF and matching `If-Match` | Discards manual draft and restores baseline |
| `POST /api/manual-draft/publish` | Local same-origin; CSRF and matching `If-Match` | Publishes conflict-free draft as new baseline |
| Existing `/api/repair-draft/**`, `/api/runs/**`, `/api/proposal/**` | Local same-origin; CSRF and matching `If-Match` | Refused with HTTP 409 while in `MANUAL_DRAFT` |

## Verification strategy

Automated verification executes against isolated PostgreSQL 18.6 Testcontainers and an ephemeral Spring Boot server using headless Chrome automation. Tests do not use production or developer databases.

- **UC-1**: Start manual draft from accepted baseline; assert transition to `MANUAL_DRAFT`, version increment, clean conflict count, and refusal if not in `ACCEPTED_BASELINE`.
- **UC-2**: Reassign lesson period, room, and teacher; verify persistence across reload, conflict detection for each normative constraint type, cell highlighting, and badge counter.
- **UC-3**: Activate conflicting cell; verify explanatory overlay popover with exact conflict code, competing lesson IDs, and multi-conflict enumeration.
- **UC-4**: Revert modified lesson; verify exact restoration of accepted baseline values, automatic conflict resolution, and persistence.
- **UC-5**: Discard draft with confirmation; assert deletion of draft node, restoration of `ACCEPTED_BASELINE`, and cancellation handling.
- **UC-6**: Attempt publication with $>0$ conflicts (assert 422 refusal and unchanged baseline); publish with 0 conflicts (assert kernel verification pass, updated baseline revision, version increment, and transition to `ACCEPTED_BASELINE`).

## Rules

### RULE-1 - Lifecycle State and Storage Migration

- Applies to: UC-1, UC-5, UC-6
- Constraint: `WorkspaceState` MUST include `MANUAL_DRAFT`. A Flyway migration MUST update the `workspace_aggregate.lifecycle_state` check constraint to permit `'MANUAL_DRAFT'`. Any state transition not explicitly declared in `spec.md` MUST be refused with HTTP 409 `INVALID_WORKSPACE_TRANSITION` without mutating state.
- Reason: Enforces database-level data integrity and prevents invalid lifecycle transitions.
- Verification: Flyway migration execution test and unit/integration tests asserting HTTP 409 on invalid lifecycle transitions.

### RULE-2 - Optimistic Concurrency and ETag Guard

- Applies to: all use cases
- Constraint: All mutating endpoints (`POST /api/manual-draft`, `PATCH /api/manual-draft`, `DELETE /api/manual-draft`, `POST /api/manual-draft/publish`) MUST require an `If-Match` header matching the current workspace ETag and a valid session CSRF token. Mutations MUST atomically increment the workspace version on success and return HTTP 412 `PRECONDITION_FAILED` if `If-Match` does not match.
- Reason: Protects against concurrent administrative sessions or stale browser state overwriting drafts or baselines.
- Verification: Integration tests submitting requests with stale ETags or missing CSRF tokens asserting HTTP 412 or 403.

### RULE-3 - Authoritative Server-Side Conflict Validation

- Applies to: UC-2, UC-3, UC-6
- Constraint: The backend service MUST authoritatively evaluate assignments against all six normative conflict types (`TEACHER_UNAVAILABLE`, `TEACHER_CLASH`, `ROOM_UNAVAILABLE`, `ROOM_CLASH`, `COHORT_CLASH`, `ROOM_INCOMPATIBLE`) on every `PATCH /api/manual-draft`. The evaluated conflict metadata MUST be stored in `document.manualDraft.conflicts` and returned in `GET /api/workspace`. The server MUST NOT delegate conflict authority solely to the client.
- Reason: Server authority guarantees persistence fidelity and ensures that invalid timetables cannot bypass validation.
- Verification: Backend service unit tests asserting correct conflict objects for each normative conflict code, verified against the definition and assignments.

### RULE-4 - Conflict-Gated Baseline Publication with Kernel Verification

- Applies to: UC-6
- Constraint: `POST /api/manual-draft/publish` MUST refuse baseline publication with HTTP 422 if `manualDraft.conflicts` is non-empty. If conflicts are zero, the service MUST execute `BaselineVerifier.verify(definition, timetable)` from `kernel-cli`. If verification succeeds, it MUST atomically advance the accepted baseline, generate a new SHA-256 timetable revision, increment version, and purge `manualDraft`. If verification fails, it MUST refuse publication with HTTP 422 and retain the draft intact.
- Reason: Preserves the fundamental invariant that an `ACCEPTED_BASELINE` is always feasible and valid according to School Kernel.
- Verification: Integration tests attempting publication with 1 conflict (asserting 422 and baseline unchanged) and with 0 conflicts (asserting 200, updated baseline revision, and cleared draft).

### RULE-5 - Zero Dependency Native UI with Synchronized Conflict Highlighting

- Applies to: UC-2, UC-3
- Constraint: The manual editing UI MUST be implemented in native HTML, CSS, and ES modules with no npm, framework, or bundler dependencies. Conflicting cells MUST receive a visual highlight class and accessible ARIA attributes. Activating a conflicting cell MUST display an overlay popover containing the human-readable explanation and competing lesson IDs from the message catalog. All user-facing strings MUST reside in `messages.js`.
- Reason: Maintains architectural consistency across the workspace module and ensures accessible, localized feedback without frontend bloat.
- Verification: Headless Chrome browser assertions verifying CSS highlighting classes, ARIA alerts, and overlay content matching the active conflict.

### RULE-6 - Complete Discard and Per-Lesson Revert Isolation

- Applies to: UC-4, UC-5
- Constraint: Discarding the draft (`DELETE /api/manual-draft`) MUST require an explicit `{ "confirmed": true }` payload and MUST completely remove `document.manualDraft`, restoring the exact accepted baseline version + 1 without residual adjustments. Reverting an individual lesson (`PATCH /api/manual-draft` with `{ "action": "REVERT_LESSON", "lessonId": "..." }`) MUST restore the exact period, room, and teacher from `acceptedBaseline.result.timetable.assignments` and trigger full conflict re-evaluation.
- Reason: Prevents unconfirmed destructive loss of work and ensures clean restoration to accepted values without state contamination.
- Verification: Database aggregate inspection before and after revert and discard verifying exact assignment restoration and absence of orphaned draft nodes.

### RULE-7 - End-to-End Browser and Containerized Test Boundary

- Applies to: all use cases
- Constraint: Automated verification MUST execute within isolated PostgreSQL 18.6 Testcontainers and an ephemeral Spring Boot server using headless Chrome. Verification MUST assert visible DOM state, overlay contents, conflict badges, and durable database JSONB state after every consequential step. Tests MUST NOT touch production or developer databases.
- Reason: Guarantees end-to-end reliability from user interaction down to PostgreSQL JSONB storage without side effects on external environments.
- Verification: Maven build running the complete integration test suite passing against Testcontainers.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-7 |
| UC-2 | RULE-2, RULE-3, RULE-5, RULE-7 |
| UC-3 | RULE-3, RULE-5, RULE-7 |
| UC-4 | RULE-2, RULE-3, RULE-6, RULE-7 |
| UC-5 | RULE-1, RULE-2, RULE-6, RULE-7 |
| UC-6 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-7 |

## Design exclusions

- Automatic re-solving of non-selected lessons via OptaPlanner / Timefold during manual editing.
- Multi-user collaboration, locking protocols, or conflict merging across different client sessions.
- Editing master school definition entities (classes, subjects, teacher contracts) within the manual editing draft.
- Split-group partition mutation or cohort group reassignments.

## External dependencies

None.
