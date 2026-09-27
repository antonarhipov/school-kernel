# Use-Case Checkpoint: UC-4 - Revert individual lesson assignment

## Summary

- Status: READY_FOR_CONVERGENCE
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-1 (active manual draft)

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-4 main steps 1-5 | `WorkspaceManualDraftIT.revertsIndividualLessonCleanly` (`WorkspaceManualDraftIT.java:210`), UI `#revert-lesson-btn` handling in `app.js:1538` | PASS |
| UC-4 extension 1a (Revert disabled when unmodified) | `app.js:1102` rendering `<button id="revert-lesson-btn" type="button" class="secondary"${manual.modified ? '' : ' disabled'}>` and guard in `app.js:1541` | PASS |
| UC-4 extension 4a (Persistence failure handling) | `app.js:1552` displaying error in `#edit-save-status` retaining client-side state without corrupting stored state | PASS |
| UC-4 G1 (Baseline fidelity) | `WorkspaceManualDraftIT.revertsIndividualLessonCleanly` (`WorkspaceManualDraftIT.java:240`) explicitly verifying restored `periodId`, `roomId`, and `teacherId` match baseline values | PASS |
| UC-4 G2 (Automatic conflict recalculation) | `WorkspaceManualDraftIT.revertsIndividualLessonCleanly` (`WorkspaceManualDraftIT.java:234`) asserting that `conflicts` and `modifications` maps are cleared after reverting the lesson | PASS |
| UC-4 success postcondition | Selected lesson matches accepted baseline; draft is persisted with updated version; modifications and conflict highlights are cleared | PASS |
| UC-4 minimal guarantee | Prior draft state remains unchanged if revert cannot be applied (ETag check, optimistic locking) | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 (Working draft lifecycle) | Reverting a lesson updates the draft state and keeps workspace in `MANUAL_DRAFT`; tested in `WorkspaceManualDraftIT.java:210` | PASS |
| RULE-2 (Optimistic concurrency) | Revert request requires `If-Match` matching draft version; verified in `ManualDraftService.java` and `WorkspaceManualDraftIT.java` | PASS |
| RULE-3 (Normative constraints) | Reverting immediately re-runs conflict evaluation and clears resolved conflicts; tested in `WorkspaceManualDraftIT.java:234` | PASS |
| RULE-5 (UI visual cues) | Reverting clears `modified-state` cue, red conflicting border, and `⚠️` indicator from the cell | PASS |
| RULE-7 (Automated integration verification) | `WorkspaceManualDraftIT.revertsIndividualLessonCleanly` run against PostgreSQL Testcontainers | PASS |

## Validation

- Focused commands: `mvn test -pl timetable-workspace -Dtest=WorkspaceManualDraftIT` (8 run, 0 failures, 0 errors, 0 skipped)
- Working tree impact from tests: none
- Runtime evidence:
  - When an unmodified lesson is selected in `MANUAL_DRAFT`, the "Revert to baseline" button is disabled.
  - When a lesson has modifications differing from the accepted baseline, the "Revert to baseline" button is enabled.
  - Clicking "Revert to baseline" triggers `PATCH /api/manual-draft` with `{ action: 'REVERT_LESSON', lessonId }`.
  - The lesson returns to its baseline period, room, and teacher, visual modification indicators are cleared, and any conflict caused by the modified lesson is removed.
- Changed files:
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
- Approved UCs regression-tested: UC-1, UC-2, UC-3 (8/8 in `WorkspaceManualDraftIT`).
