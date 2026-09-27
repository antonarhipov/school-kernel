# Use-Case Checkpoint: UC-1 - Open manual editing draft

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: 41109b60659633d99c0ced27fee36df2950da905
- Submission commit: HEAD at convergence
- Relations verified: none (root use case)

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main steps 1-4 | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:64`) | PASS |
| UC-1 extension 1a | `WorkspaceManualDraftIT.refusesOpenDraftFromNonAcceptedState` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:91`) | PASS |
| UC-1 extension 3a | `WorkspaceManualDraftIT.enforcesOptimisticConcurrency` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:103`) | PASS |
| UC-1 G1 | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:87`) | PASS |
| UC-1 G2 | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:82`) | PASS |
| UC-1 success postcondition | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:88`) | PASS |
| UC-1 minimal guarantee | `WorkspaceManualDraftIT.refusesOpenDraftFromNonAcceptedState` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:98`) | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `V2__add_manual_draft_state.sql` schema migration applied by Flyway on startup in PostgreSQL container | PASS |
| RULE-2 | `WorkspaceController.java` (`POST /api/manual-draft`), `SecurityConfiguration.java`, `WorkspaceManualDraftIT.enforcesOptimisticConcurrency` | PASS |
| RULE-7 | `WorkspaceManualDraftIT.java` executed against Testcontainers PostgreSQL container | PASS |

## Validation

- Focused commands: `mvn test -Dtest=WorkspaceManualDraftIT` (3 run, 0 failures, 0 errors, 0 skipped)
- Full relevant suite: `mvn test -Dtest=WorkspaceRepairDraftIT` (11 run, 0 failures, 0 errors, 0 skipped)
- Working tree impact from tests: none
- Runtime evidence: primary actor clicks "Manual editing" button (`#start-manual-draft`), dispatches `POST /api/manual-draft`, server transitions workspace to `MANUAL_DRAFT`, initializes `manualDraft` bundle, and client renders draft workbench with conflict badge ("0 conflicts")
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
- Approved UCs regression-tested: none (first UC of feature; regression checked against accepted repair draft suite)

## Notes

none

READY FOR CONVERGENCE: UC-1
