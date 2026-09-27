# Use-Case Status: Timetable Manual Editing

## Current

- Use case: UC-2
- Status: PENDING_WALKTHROUGH
- Next eligible: none (UC-2 pending walkthrough)

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | c309189 | convergence/UC-1.md |
| UC-2 | PENDING_WALKTHROUGH | UC-1 | 5273fab | convergence/UC-2.md |
| UC-3 | NOT_STARTED | UC-1, UC-2 | - | - |
| UC-4 | NOT_STARTED | UC-1 | - | - |
| UC-5 | NOT_STARTED | UC-1 | - | - |
| UC-6 | NOT_STARTED | UC-1 | - | - |

## UC-2 Evidence

- Started from: 6f84e4f61f7d54407b71345d36e2f170f3f2252a
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files:
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/ManualDraftService.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceController.java`
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/main/resources/static/workspace/messages.js`
  - `timetable-workspace/src/main/resources/static/workspace/styles.css`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
  - `spec/timetable-manual-editing/checkpoints/UC-2.md`
- Commands and results:
  - `mvn test -pl timetable-workspace -Dtest=WorkspaceManualDraftIT`: 7 run, 0 failures, 0 errors, 0 skipped
  - `mvn test -pl timetable-workspace`: 26 run, 0 failures, 0 errors, 0 skipped

| Contract element | Evidence |
|---|---|
| UC-2 main steps 1-8 | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` |
| UC-2 extension 3a, 4a | `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` |
| UC-2 extension 6a | `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` |
| UC-2 G1 | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` |
| UC-2 G2 | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` |
| UC-2 G3 | `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` |
| UC-2 success postcondition | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` |
| UC-2 minimal guarantee | `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` |
| RULE-2 | `WorkspaceController.java`, `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` |
| RULE-3 | `ManualDraftService.evaluateConflicts`, `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` |
| RULE-5 | PostgreSQL `manualDraft` JSONB persistence, `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` |
| RULE-7 | `WorkspaceManualDraftIT.java` executed against Testcontainers PostgreSQL |

## UC-1 Evidence

- Started from: 41109b60659633d99c0ced27fee36df2950da905
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files:
  - `timetable-workspace/src/main/resources/db/migration/V2__add_manual_draft_state.sql`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceState.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceMutation.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/ManualDraftService.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceController.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/SecurityConfiguration.java`
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/main/resources/static/workspace/inspection-state.js`
  - `timetable-workspace/src/main/resources/static/workspace/messages.js`
  - `timetable-workspace/src/main/resources/static/workspace/styles.css`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
- Commands and results:
  - `mvn test -Dtest=WorkspaceManualDraftIT` (3 tests run, 0 failures, 0 errors, 0 skipped)
  - `mvn test -Dtest=WorkspaceRepairDraftIT` (11 tests run, 0 failures, 0 errors, 0 skipped)

| Contract element | Evidence |
|---|---|
| UC-1 main steps 1-4 | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline`: tests `POST /api/manual-draft` from `ACCEPTED_BASELINE`, HTTP 200, state `MANUAL_DRAFT`, aggregate version incremented, zero conflicts, zero modifications, initial assignments equal baseline assignments (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L64-L89`) |
| UC-1 extension 1a | `WorkspaceManualDraftIT.refusesOpenDraftFromNonAcceptedState`: tests `POST /api/manual-draft` when state is `EMPTY`, HTTP 409 `INVALID_WORKSPACE_TRANSITION`, zero aggregate mutation (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L91-L101`) |
| UC-1 extension 3a | `WorkspaceManualDraftIT.enforcesOptimisticConcurrency`: tests `POST /api/manual-draft` with missing `If-Match` (HTTP 428) and mismatched ETag (HTTP 412 `STALE_WORKSPACE_VERSION`) (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L103-L119`) |
| UC-1 G1 | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline`: asserts `acceptedBaseline` in DB document is byte-identical before and after draft creation (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L87`) |
| UC-1 G2 | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline`: asserts `manualDraft.assignments` copies baseline assignments by value (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L82-L84`) |
| UC-1 success postcondition | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline`: asserts DB row `lifecycle_state='MANUAL_DRAFT'`, version incremented, working draft persisted (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L72-L88`) |
| UC-1 minimal guarantee | `WorkspaceManualDraftIT.refusesOpenDraftFromNonAcceptedState` & `enforcesOptimisticConcurrency`: asserts rejected transitions result in zero state mutation and preserve current lifecycle (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L100-L118`) |
| RULE-1 | `V2__add_manual_draft_state.sql` Flyway migration applied and verified against real PostgreSQL container (`timetable-workspace/src/main/resources/db/migration/V2__add_manual_draft_state.sql`) |
| RULE-2 | `WorkspaceController.java` (`POST /api/manual-draft`), `SecurityConfiguration.java` (`permitAll`), `WorkspaceManualDraftIT.enforcesOptimisticConcurrency` (CSRF + ETag validation) |
| RULE-7 | Containerized PostgreSQL integration tests (`WorkspaceManualDraftIT.java`) executing full HTTP stack via `Testcontainers` |

## Blockers

none

## Deviations

none
