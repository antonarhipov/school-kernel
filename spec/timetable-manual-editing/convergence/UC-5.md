# Convergence: UC-5 - Discard manual editing draft

## Summary

- Submission: `spec/timetable-manual-editing/checkpoints/UC-5.md` at `b728b14`
- Verdict: APPROVE
- Findings: 0 critical, 0 gap, 0 protocol, 0 drift, 0 cosmetic
- Suite: 9 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

1. Target UC: UC-5, status `READY_FOR_CONVERGENCE`: PASS
2. `checkpoints/UC-5.md` exists and is committed in `b728b14`: PASS
3. Dependency `UC-1` is `APPROVED`: PASS
4. No other UC is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`: PASS
5. Checkpoint includes complete evidence rows across scenario, extensions, guarantees, rules: PASS
6. Diff contains only UC-5 implementation (`DELETE /api/manual-draft`, confirmation handling, purge of draft aggregate, UI discard button): PASS

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Requests to discard manual editing draft (Step 1) | Clicks "Discard draft" button | Observed: `#discard-manual-draft` rendered in manual draft mode |
| System | Displays confirmation dialog warning of permanent loss (Step 2) | Browser confirmation prompt | Observed: `window.confirm(M.confirmDiscardDraft)` executed |
| Administrator | Cancels confirmation dialog (Extension 3a) | Draft remains intact | Observed: cancellation aborts request, draft remains in `MANUAL_DRAFT` |
| Administrator | Confirms discard (Step 3) | Sends DELETE with `{ confirmed: true }` | Observed: `DELETE /api/manual-draft` invoked with CSRF and `If-Match` |
| System | Deletes draft & transitions to ACCEPTED_BASELINE (Steps 4-6) | State returns to `ACCEPTED_BASELINE`, draft node removed | Observed: 200 OK, `state: "ACCEPTED_BASELINE"`, `manualDraft` missing, clean baseline rendered |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-5 main step 1 | Request to discard draft | `app.js:1573` (click handler on `#discard-manual-draft`) | STRONG | yes |
| UC-5 main step 2 | Display confirmation dialog | `app.js:1577` (`window.confirm(M.confirmDiscardDraft)`) | STRONG | yes |
| UC-5 main step 3 | Administrator confirms discard | `app.js:1580` (`mutateJson('/api/manual-draft', 'DELETE', { confirmed: true })`) | STRONG | yes |
| UC-5 main step 4 | Deletes manual draft from durable storage | `ManualDraftService.java:64`, `WorkspaceManualDraftIT.java:395` | STRONG | yes |
| UC-5 main step 5 | Transitions workspace state to ACCEPTED_BASELINE | `ManualDraftService.java:68`, `WorkspaceManualDraftIT.java:394` | STRONG | yes |
| UC-5 main step 6 | Renders accepted baseline timetable cleanly | `app.js:1838` (`render(result)`), `WorkspaceManualDraftIT.java:400` | STRONG | yes |
| UC-5 extension 3a | Cancel confirmation dialog preserves draft | `app.js:1578` (`if (!confirmed) return;`), `WorkspaceManualDraftIT.java:378` | STRONG | yes |
| UC-5 extension 4a | Storage failure reports error and preserves draft | `app.js:1815` (failure reporting alert, state kept) | STRONG | yes |
| UC-5 G1 | Clean discard (completely purges draft records) | `WorkspaceManualDraftIT.java:395` (`manualDraft` node is missing) | STRONG | yes |
| UC-5 G2 | Confirmation barrier (explicit positive confirmation required) | `ManualDraftService.java:59`, `WorkspaceManualDraftIT.java:378` (unconfirmed rejected with 422 `CONFIRMATION_REQUIRED`) | STRONG | yes |
| UC-5 success postcondition | Workspace is in `ACCEPTED_BASELINE`; all unaccepted adjustments are purged | `WorkspaceManualDraftIT.java:394` | STRONG | yes |
| UC-5 minimal guarantee | Workspace remains in `MANUAL_DRAFT` if discard cancelled or fails | `WorkspaceManualDraftIT.java:383` | STRONG | yes |
| Requires UC-1 | Requires manual draft to be open | `ManualDraftService.java:56`, `WorkspaceManualDraftIT.java:406` | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | State transition `MANUAL_DRAFT` -> `ACCEPTED_BASELINE`; invalid transition rejected with HTTP 409 `INVALID_WORKSPACE_TRANSITION` | `WorkspaceManualDraftIT.java:406` | PASS |
| RULE-2 | Optimistic concurrency enforced via `If-Match` | `ManualDraftService.java:55`, `WorkspaceMutation.java` | PASS |
| RULE-6 | Discard requires explicit `{ "confirmed": true }` and purges `manualDraft` | `ManualDraftService.java:59`, `WorkspaceManualDraftIT.java:378` | PASS |
| RULE-7 | Automated integration verification on PostgreSQL Testcontainers | `WorkspaceManualDraftIT.java` (9/9 tests pass) | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Draft opening and initial model | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline`, `refusesOpenDraftFromNonAcceptedState` | PASS |
| UC-2 | Draft mutation & conflict detection | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications`, `detectsClashesAndPersistsConflicts` | PASS |
| UC-3 | Inspect conflict details and causal explanations | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations` | PASS |
| UC-4 | Revert individual lesson assignment | `WorkspaceManualDraftIT.revertsIndividualLessonCleanly` | PASS |

## Findings

None.

## Walkthrough confirmed by user on 2026-09-28 ("yes. all good").

## Status Update

- Status: `PENDING_WALKTHROUGH` -> `APPROVED`
- Next eligible UCs: UC-6

## Response to execute

APPROVED: UC-5 is verified and approved. Execution may proceed to UC-6.
