# Use-Case Status: Timetable Manual Editing

## Current

- Use case: UC-6
- Status: IN_PROGRESS
- Next eligible: none (UC-6 in progress)

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | c309189 | convergence/UC-1.md |
| UC-2 | APPROVED | UC-1 | 5273fab | convergence/UC-2.md |
| UC-3 | APPROVED | UC-1, UC-2 | ae4ba8e | convergence/UC-3.md |
| UC-4 | APPROVED | UC-1 | 335ee49 | convergence/UC-4.md |
| UC-5 | APPROVED | UC-1 | b728b14 | convergence/UC-5.md |
| UC-6 | IN_PROGRESS | UC-1 | - | - |

## UC-5 Evidence

- Started from: 66a4d3f
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files:
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/ManualDraftService.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceController.java`
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/main/resources/static/workspace/messages.js`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
  - `spec/timetable-manual-editing/checkpoints/UC-5.md`
- Commands and results:
  - `mvn test -pl timetable-workspace -Dtest=WorkspaceManualDraftIT`: 9 run, 0 failures, 0 errors, 0 skipped

| Contract element | Evidence |
|---|---|
| UC-5 main steps 1-6 | `WorkspaceManualDraftIT.java:356`, `ManualDraftService.java:55`, `WorkspaceController.java:128`, `app.js:662` |
| UC-5 extension 3a | `app.js:1578`, `WorkspaceManualDraftIT.java:378` |
| UC-5 extension 4a | `app.js:1815` |
| UC-5 G1 | `WorkspaceManualDraftIT.java:395` |
| UC-5 G2 | `WorkspaceManualDraftIT.java:378` |
| UC-5 success postcondition | `WorkspaceManualDraftIT.java:395`, `app.js` |
| UC-5 minimal guarantee | `WorkspaceManualDraftIT.java:378` |
| RULE-1, RULE-2, RULE-6, RULE-7 | `ManualDraftService.java`, `WorkspaceManualDraftIT.java`, `app.js` |

## UC-4 Evidence

- Started from: 15f479ff737b83072212fbe937b4e94a8c985ec9
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files:
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
  - `spec/timetable-manual-editing/checkpoints/UC-4.md`
- Commands and results:
  - `mvn test -pl timetable-workspace -Dtest=WorkspaceManualDraftIT`: 8 run, 0 failures, 0 errors, 0 skipped

| Contract element | Evidence |
|---|---|
| UC-4 main steps 1-5 | `WorkspaceManualDraftIT.java:210`, `app.js:1538` |
| UC-4 extension 1a | `app.js:1102` (`disabled` attribute when unmodified), `app.js:1541` |
| UC-4 extension 4a | `app.js:1552` (error displayed, client state preserved) |
| UC-4 G1 | `WorkspaceManualDraftIT.java:240` (period, room, teacher restored to baseline values) |
| UC-4 G2 | `WorkspaceManualDraftIT.java:234` (modifications and conflicts cleared) |
| UC-4 success postcondition | `WorkspaceManualDraftIT.java:234`, `app.js` |
| UC-4 minimal guarantee | `WorkspaceManualDraftIT.java:248` (optimistic concurrency / ETag) |
| RULE-1, RULE-2, RULE-3, RULE-5, RULE-7 | `ManualDraftService.java`, `WorkspaceManualDraftIT.java`, `app.js` |

## UC-3 Evidence

- Started from: 9048e41a37c355887be177b94998eeea4457e5e1
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files:
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/main/resources/static/workspace/messages.js`
  - `timetable-workspace/src/main/resources/static/workspace/styles.css`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
  - `spec/timetable-manual-editing/checkpoints/UC-3.md`
- Commands and results:
  - `mvn test -pl timetable-workspace -Dtest=WorkspaceManualDraftIT`: 8 run, 0 failures, 0 errors, 0 skipped

| Contract element | Evidence |
|---|---|
| UC-3 main steps 1-4 | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations`, UI overlay in `app.js` & `styles.css` |
| UC-3 extension 2a | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations` (`WorkspaceManualDraftIT.java:302`) |
| UC-3 G1 | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations` (`WorkspaceManualDraftIT.java:309`) |
| UC-3 G2 | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations` (`WorkspaceManualDraftIT.java:328`) |
| UC-3 success postcondition | `conflictOverlayMarkup`, `manualCues`, `WorkspaceManualDraftIT.java:309` |
| UC-3 minimal guarantee | `WorkspaceManualDraftIT.java:328` (ETag, version, state unchanged) |
| RULE-3 | `ManualDraftService.evaluateConflicts`, `WorkspaceManualDraftIT.java:302` |
| RULE-5 | UI overlay adjacent to cell + inspector itemization + DB persistence |
| RULE-7 | `WorkspaceManualDraftIT.java` against Testcontainers PostgreSQL |

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
