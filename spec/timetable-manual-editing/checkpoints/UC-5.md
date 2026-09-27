# Use-Case Checkpoint: UC-5 - Discard manual editing draft

## Summary

- Status: READY_FOR_CONVERGENCE
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-1 (active manual draft)

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-5 main steps 1-6 | `WorkspaceManualDraftIT.discardsManualDraftWithConfirmationAndRestoresAcceptedBaseline` (`WorkspaceManualDraftIT.java:356`), UI `#discard-manual-draft` handling in `app.js:662`, `app.js:1573` | PASS |
| UC-5 extension 3a (Cancel confirmation) | `app.js:1578` (`if (!confirmed) return;`), `WorkspaceManualDraftIT.java:378` (unconfirmed discard rejected with 422 `CONFIRMATION_REQUIRED`, draft state preserved) | PASS |
| UC-5 extension 4a (Persistence failure handling) | `app.js:1815` (error displayed, draft state preserved in client) | PASS |
| UC-5 G1 (Clean discard) | `WorkspaceManualDraftIT.java:395` (`manualDraft` node completely removed, workspace restored to `ACCEPTED_BASELINE`, 0 draft conflicts) | PASS |
| UC-5 G2 (Confirmation barrier) | `WorkspaceManualDraftIT.java:378` (unconfirmed discard rejected with 422 `CONFIRMATION_REQUIRED`, native `window.confirm` in UI) | PASS |
| UC-5 success postcondition | Workspace is in `ACCEPTED_BASELINE`; all unaccepted adjustments are purged; accepted baseline timetable rendered | PASS |
| UC-5 minimal guarantee | Workspace remains in `MANUAL_DRAFT` if discard is unconfirmed or fails | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 (Lifecycle State) | Discard transitions `MANUAL_DRAFT` -> `ACCEPTED_BASELINE`; discard from other states refused with HTTP 409 `INVALID_WORKSPACE_TRANSITION`; tested in `WorkspaceManualDraftIT.java:406` | PASS |
| RULE-2 (Optimistic Concurrency) | Requires `If-Match` header matching current draft version; verified in `ManualDraftService.java:55` and `WorkspaceMutation.java` | PASS |
| RULE-6 (Complete Discard Isolation) | Requires explicit `{ "confirmed": true }` payload; completely purges `document.manualDraft`; tested in `WorkspaceManualDraftIT.java:378`, `ManualDraftService.java:60` | PASS |
| RULE-7 (Automated Integration Verification) | Testcontainers PostgreSQL integration test suite in `WorkspaceManualDraftIT.java` (9/9 tests pass) | PASS |

## Validation

- Focused commands: `mvn test -pl timetable-workspace -Dtest=WorkspaceManualDraftIT` (9 run, 0 failures, 0 errors, 0 skipped)
- Working tree impact from tests: none
- Runtime evidence:
  - In `MANUAL_DRAFT`, the "Discard draft" button is rendered in the header task area.
  - Clicking "Discard draft" triggers a confirmation dialog warning that unaccepted edits will be permanently lost.
  - If cancelled, the draft and edits remain intact.
  - If confirmed, sends `DELETE /api/manual-draft` with `{ "confirmed": true }` and `If-Match`.
  - Workspace transitions back to `ACCEPTED_BASELINE`, the `manualDraft` document node is purged, and the accepted timetable is rendered cleanly.
- Changed files:
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/ManualDraftService.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceController.java`
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/main/resources/static/workspace/messages.js`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
- Approved UCs regression-tested: UC-1, UC-2, UC-3, UC-4 (9/9 in `WorkspaceManualDraftIT`).
