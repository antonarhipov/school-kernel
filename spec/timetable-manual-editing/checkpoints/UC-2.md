# Use-Case Checkpoint: UC-2 - Reassign lesson slot, room, or teacher

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: 6f84e4f61f7d54407b71345d36e2f170f3f2252a
- Submission commit: HEAD at convergence
- Relations verified: UC-1 (requires manual draft to be open)

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-2 main steps 1-8 | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:128`) | PASS |
| UC-2 extension 3a, 4a | `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:237`) | PASS |
| UC-2 extension 6a | `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:163`) | PASS |
| UC-2 G1 | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:159`) | PASS |
| UC-2 G2 | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:151`) | PASS |
| UC-2 G3 | `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:199`) | PASS |
| UC-2 success postcondition | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:146`) | PASS |
| UC-2 minimal guarantee | `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java:251`) | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-2 | `WorkspaceController.java` (`@PatchMapping("/api/manual-draft")`), `SecurityConfiguration.java`, `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` | PASS |
| RULE-3 | `ManualDraftService.evaluateConflicts` authoritatively detects all 6 constraint types; verified in `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` | PASS |
| RULE-5 | Conflicting assignments are persisted in database `manualDraft` document; verified in `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` | PASS |
| RULE-7 | `WorkspaceManualDraftIT.java` executed against Testcontainers PostgreSQL container | PASS |

## Validation

- Focused commands: `mvn test -pl timetable-workspace -Dtest=WorkspaceManualDraftIT` (7 run, 0 failures, 0 errors, 0 skipped)
- Full relevant suite: `mvn test -pl timetable-workspace` (26 run, 0 failures, 0 errors, 0 skipped)
- Working tree impact from tests: none
- Runtime evidence: Actor selects lesson in DRAFT mode, Inspector sidebar opens Assignment Editor with period, room, and teacher dropdowns. On save, AJAX `PATCH /api/manual-draft` is dispatched with ETag; canvas updates position, conflict badge updates, conflicting lessons highlight with red borders, conflict details display in inspector, and modified lessons display modified status.
- Changed files:
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/ManualDraftService.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceController.java`
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/main/resources/static/workspace/messages.js`
  - `timetable-workspace/src/main/resources/static/workspace/styles.css`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
- Approved UCs regression-tested: UC-1 tests passed (3/3 in `WorkspaceManualDraftIT`).

## Notes

- In addition to slot/room/teacher reassignment, `ManualDraftService` and `WorkspaceManualDraftIT` also verified `REVERT_LESSON` reverting individual assignments to baseline and clearing modifications cleanly.
