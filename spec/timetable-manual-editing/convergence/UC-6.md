# Convergence: UC-6 - Publish manual editing draft as accepted baseline

## Summary

- Submission: `spec/timetable-manual-editing/checkpoints/UC-6.md` at `2d69f15`
- Verdict: APPROVE
- Findings: 0 critical, 0 gap, 0 protocol, 0 drift, 0 cosmetic
- Suite: 11 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

1. Target UC: UC-6, status `IN_PROGRESS` -> `READY_FOR_CONVERGENCE`: PASS
2. `checkpoints/UC-6.md` exists and is committed in `2d69f15`: PASS
3. Dependency `UC-1` is `APPROVED`: PASS
4. No other UC is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`: PASS
5. Checkpoint includes complete evidence rows across scenario, extensions, guarantees, rules: PASS
6. Diff contains only UC-6 implementation (`POST /api/manual-draft/publish`, conflict gating, School Kernel verification, timetable revision calculation, manifest regeneration, UI publish button): PASS

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Requests to publish draft with hard conflicts (Extension 2a) | Publish button disabled / 422 refused | Observed: `#publish-manual-draft` disabled with tooltip when conflicts > 0; `POST /api/manual-draft/publish` returns 422 `UNRESOLVED_CONFLICTS` |
| Administrator | Reassigns lesson to eliminate conflict, then requests publish (Step 1) | Clicks "Publish draft" | Observed: `#publish-manual-draft` enabled; triggers `POST /api/manual-draft/publish` with CSRF and `If-Match` |
| System | Verifies unresolved conflict count is zero (Step 2) | Checked in service | Observed: `ManualDraftService.java:540` checks `conflicts` array |
| System | Invokes School Kernel verifier against candidate timetable (Step 3) | Kernel verifier executed | Observed: `KernelVerifier.verify(...)` runs `./school-kernel verify` and passes with `exitClass=VERIFIED` |
| System | Computes SHA-256 timetable revision and generates manifest (Step 4) | Revision updated | Observed: canonical SHA-256 computed; `manifests.validatedOrGenerated(...)` regenerates baseline manifest |
| System | Atomically replaces manual draft with accepted baseline (Steps 5-6) | State transitions to `ACCEPTED_BASELINE`, version advances | Observed: 200 OK, `state: "ACCEPTED_BASELINE"`, `manualDraft` missing, aggregate version increments |
| System | Renders new accepted baseline timetable cleanly (Step 7) | UI returns to accepted baseline view | Observed: workbench renders updated assignments and new timetable revision |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-6 main step 1 | Request to publish draft | `app.js:1587` (click handler on `#publish-manual-draft`) | STRONG | yes |
| UC-6 main step 2 | Verifies zero hard conflicts | `ManualDraftService.java:540`, `WorkspaceManualDraftIT.java:413` | STRONG | yes |
| UC-6 main step 3 | Invokes School Kernel verifier | `ManualDraftService.java:567`, `KernelVerifier.java`, test log execution | STRONG | yes |
| UC-6 main step 4 | Generates timetable revision hash & manifest | `ManualDraftService.calculateTimetableRevision`, `ManifestService.java` | STRONG | yes |
| UC-6 main step 5 | Atomically updates accepted baseline in durable storage | `mutation.replaceManualDraft(...)` in `ManualDraftService.java:598` | STRONG | yes |
| UC-6 main step 6 | Transitions state to ACCEPTED_BASELINE and advances version | `ManualDraftService.java:602`, `WorkspaceManualDraftIT.java:470` | STRONG | yes |
| UC-6 main step 7 | Renders new accepted baseline timetable | `app.js:1838` (`render(result)`), `WorkspaceManualDraftIT.java:490` | STRONG | yes |
| UC-6 extension 2a | Refuse publication when hard conflicts present | `WorkspaceManualDraftIT.java:413` (returns 422 `UNRESOLVED_CONFLICTS`), `app.js:657` (disabled button) | STRONG | yes |
| UC-6 extension 4a | Verification failure aborts publication | `ManualDraftService.java:571` (catches `KERNEL_VERIFICATION_FAILED` and throws 422 `VERIFICATION_FAILED`) | STRONG | yes |
| UC-6 extension 5a | Database/persistence failure handling | Transaction rollback preserves `MANUAL_DRAFT` | STRONG | yes |
| UC-6 G1 | Zero conflict publication gate | Refuses publish with 422 `UNRESOLVED_CONFLICTS` when conflicts exist | STRONG | yes |
| UC-6 G2 | Authoritative verification | Mandatory `KernelVerifier.verify` check before acceptance | STRONG | yes |
| UC-6 G3 | Atomic progression | Database transition replaces aggregate atomically and increments version | STRONG | yes |
| UC-6 success postcondition | Workspace state is `ACCEPTED_BASELINE`; new timetable revision active; manual draft purged | `WorkspaceManualDraftIT.java:467-505` | STRONG | yes |
| UC-6 minimal guarantee | Workspace remains in `MANUAL_DRAFT` if verification or publish fails | `WorkspaceManualDraftIT.java:439` | STRONG | yes |
| Requires UC-1 | Requires manual draft to be open | `ManualDraftService.java:536`, `WorkspaceManualDraftIT.java:516` | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 (Lifecycle State) | Transitions `MANUAL_DRAFT` -> `ACCEPTED_BASELINE`; publishing from non-draft states refused with HTTP 409 `INVALID_WORKSPACE_TRANSITION` | `WorkspaceManualDraftIT.java:516`, `ManualDraftService.java:536` | PASS |
| RULE-2 (Optimistic Concurrency) | Enforces `If-Match: "ws-<version>"` header matching current draft version; rejects mismatched versions with HTTP 412 | `ManualDraftService.java:534`, `ImportService.requireMatchingVersion` | PASS |
| RULE-4 (Conflict-Gated Publication) | Refuses publication with HTTP 422 if conflicts exist; executes School Kernel verifier; computes canonical SHA-256 timetable revision; regenerates manifest | `ManualDraftService.java:540-580`, `WorkspaceManualDraftIT.java:413,475` | PASS |
| RULE-7 (Automated Integration Verification) | Testcontainers PostgreSQL integration test suite covers publication, conflict prevention, atomic updates, and state transitions | `WorkspaceManualDraftIT.java` (11/11 tests pass) | PASS |
