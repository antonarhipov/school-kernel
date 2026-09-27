# Use-Case Checkpoint: UC-6 - Publish manual editing draft as accepted baseline

## Summary

- Status: READY_FOR_CONVERGENCE
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-1 (active manual draft)

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-6 main steps 1-7 | `WorkspaceManualDraftIT.publishesManualDraftAtomicallyAsAcceptedBaseline` (`WorkspaceManualDraftIT.java:452`), UI `#publish-manual-draft` handling in `app.js:662`, `app.js:1587` | PASS |
| UC-6 extension 2a (Hard conflicts present) | `WorkspaceManualDraftIT.refusesPublishWithUnresolvedConflicts` (`WorkspaceManualDraftIT.java:413`), disabled button `#publish-manual-draft` when `conflictsCount > 0` (`app.js:657`) | PASS |
| UC-6 extension 4a (Kernel verification failure) | `ManualDraftService.java:571` catches `KERNEL_VERIFICATION_FAILED` and throws 422 `VERIFICATION_FAILED` | PASS |
| UC-6 extension 5a (Persistence failure handling) | Atomic database transaction via `mutation.replaceManualDraft(...)` preserves draft on failure | PASS |
| UC-6 G1 (Zero conflict publication gate) | Endpoint refuses publish with HTTP 422 `UNRESOLVED_CONFLICTS` if `manualDraft.conflicts` is non-empty; UI disables publish button | PASS |
| UC-6 G2 (Authoritative verification) | `KernelVerifier.verify(...)` runs School Kernel verifier on candidate timetable before accepting; manifest regenerated | PASS |
| UC-6 G3 (Atomic progression) | `acceptedBaseline` updated with new result and manifest, `manualDraft` purged, state transitions to `ACCEPTED_BASELINE`, version advances atomically | PASS |
| UC-6 success postcondition | Workspace state is `ACCEPTED_BASELINE`; new timetable revision is active; manual draft is removed; UI displays accepted view | PASS |
| UC-6 minimal guarantee | If verification or publishing fails, workspace remains in `MANUAL_DRAFT` with no partial baseline updates | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 (Lifecycle State) | Transitions `MANUAL_DRAFT` -> `ACCEPTED_BASELINE`; publishing from other states refused with HTTP 409 `INVALID_WORKSPACE_TRANSITION`; tested in `WorkspaceManualDraftIT.java:516` | PASS |
| RULE-2 (Optimistic Concurrency) | Requires `If-Match` header matching current draft version; tested with stale ETag returning HTTP 412 | PASS |
| RULE-4 (Conflict-Gated Publication) | Refuses publication with HTTP 422 if conflicts exist; executes `KernelVerifier.verify`; tested in `WorkspaceManualDraftIT.java:413`, `ManualDraftService.java:540` | PASS |
| RULE-7 (Automated Integration Verification) | Testcontainers PostgreSQL integration test suite in `WorkspaceManualDraftIT.java` (11/11 tests pass) | PASS |

## Validation

- Focused commands: `mvn test -pl timetable-workspace -Dtest=WorkspaceManualDraftIT` (11 run, 0 failures, 0 errors, 0 skipped)
- Working tree impact from tests: none
- Runtime evidence:
  - In `MANUAL_DRAFT`, the "Publish draft" button (`#publish-manual-draft`) is rendered in the header task area.
  - If hard conflicts exist in the draft, "Publish draft" is disabled (`disabled aria-disabled="true"`) with tooltip indicating conflicts must be resolved first.
  - Attempting to publish with conflicts returns HTTP 422 `UNRESOLVED_CONFLICTS`.
  - When the draft is conflict-free, "Publish draft" is enabled. Clicking it sends `POST /api/manual-draft/publish`.
  - Backend verifies the draft timetable with School Kernel verifier, computes new canonical SHA-256 `timetableRevision`, regenerates manifest, updates `acceptedBaseline`, removes `manualDraft`, increments aggregate version, and transitions workspace state to `ACCEPTED_BASELINE`.
  - UI seamlessly transitions back to accepted baseline mode displaying the updated timetable with the new revision.
- Changed files:
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/ManualDraftService.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceController.java`
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/main/resources/static/workspace/messages.js`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
- Approved UCs regression-tested: UC-1, UC-2, UC-3, UC-4, UC-5 (11/11 in `WorkspaceManualDraftIT`).
