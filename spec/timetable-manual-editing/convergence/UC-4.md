# Convergence: UC-4 - Revert individual lesson assignment

## Summary

- Submission: `spec/timetable-manual-editing/checkpoints/UC-4.md` at `fd8a23e`
- Verdict: PENDING WALKTHROUGH
- Findings: 0 critical, 0 gap, 0 protocol, 0 drift, 0 cosmetic
- Suite: 8 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

1. Target UC: UC-4, status `READY_FOR_CONVERGENCE`: PASS
2. `checkpoints/UC-4.md` exists and is committed in `fd8a23e`: PASS
3. Dependency `UC-1` is `APPROVED`: PASS
4. No other UC is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`: PASS
5. Checkpoint includes complete evidence rows across scenario, extensions, guarantees, rules: PASS
6. Diff contains only UC-4 implementation (revert button enablement/disablement, revert handler, baseline fidelity test): PASS

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Selects unmodified lesson in `MANUAL_DRAFT` (Ext 1a) | Revert button is disabled | Observed: `<button id="revert-lesson-btn" disabled>` rendered |
| Administrator | Reassigns a lesson | Lesson marked modified; revert button is enabled | Observed: `manual.modified` true, `#revert-lesson-btn` enabled |
| Administrator | Clicks "Revert to baseline" | Lesson restored to baseline values | Observed: `PATCH /api/manual-draft` with `REVERT_LESSON` returns 200; `periodId`, `roomId`, `teacherId` restored; modifications cleared |
| Administrator | Conflict caused by modified lesson | Conflict cleared on revert | Observed: `conflicts` array cleared when cause reverted |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-4 main step 1 | Select modified lesson and activate revert | `app.js:1538` (`#revert-lesson-btn` click handler), `WorkspaceManualDraftIT.java:225` | STRONG | yes |
| UC-4 main step 2 | Restore period slot, room, teacher to accepted baseline | `ManualDraftService.java:152`, `WorkspaceManualDraftIT.java:240` | STRONG | yes |
| UC-4 main step 3 | Automatic conflict recalculation across all lessons | `ManualDraftService.java:165`, `WorkspaceManualDraftIT.java:234` | STRONG | yes |
| UC-4 main step 4 | Durably persist updated draft | `ManualDraftService.java:168`, `WorkspaceManualDraftIT.java:229` | STRONG | yes |
| UC-4 main step 5 | Update canvas, clear modifications and conflict highlights | `app.js` (`renderStateCard`, `renderLessonCell`, `manualLessonState`) | STRONG | yes |
| UC-4 extension 1a | Revert disabled if lesson has no modifications | `app.js:1102` (`disabled` attribute), `app.js:1541` (guard check) | STRONG | yes |
| UC-4 extension 4a | Storage error reported, uncommitted state retained | `app.js:1552` (status error displayed, no corruption) | STRONG | yes |
| UC-4 G1 | Baseline fidelity (exact period, room, teacher restored) | `WorkspaceManualDraftIT.java:240` (period, room, teacher explicitly matched to baseline) | STRONG | yes |
| UC-4 G2 | Automatic conflict recalculation (resolved conflicts removed) | `WorkspaceManualDraftIT.java:234` (`conflicts` and `modifications` empty) | STRONG | yes |
| UC-4 success postcondition | Draft matches baseline for reverted lesson, persisted, conflict indicators update | `WorkspaceManualDraftIT.java:234` | STRONG | yes |
| UC-4 minimal guarantee | Prior draft state preserved on failure | `WorkspaceManualDraftIT.java:248` (version concurrency checks) | STRONG | yes |
| Requires UC-1 | Requires manual draft to be open | `ManualDraftService.java:49`, `WorkspaceManualDraftIT.java:212` | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Working draft lifecycle maintained during modifications and reverts | `WorkspaceManualDraftIT.java:210` | PASS |
| RULE-2 | Optimistic locking enforced on mutate | `WorkspaceManualDraftIT.java:252` | PASS |
| RULE-3 | Normative constraints evaluated on revert and resolved conflicts removed | `WorkspaceManualDraftIT.java:234` | PASS |
| RULE-5 | UI visual cues reflect modified and conflict status immediately | `app.js:1102`, `app.js:1200` | PASS |
| RULE-7 | Automated integration verification on real containerized PostgreSQL | `WorkspaceManualDraftIT.java` (8/8 passing via Testcontainers) | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Draft lifecycle, base model | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline`, `refusesOpenDraftFromNonAcceptedState` | PASS |
| UC-2 | Draft mutation & conflict detection | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications`, `detectsClashesAndPersistsConflicts` | PASS |
| UC-3 | Inspect conflict details and causal explanations | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations` | PASS |

## Findings

None.

## Walkthrough

1. In the Timetable Workspace UI in the browser (`http://localhost:8080/workspace/`), select an unmodified lesson:
   - Notice in the Inspector panel that the **"Revert to baseline"** button is visible but **disabled**.
2. Change its period, room, or teacher, and click **"Save assignment"**:
   - The cell displays the `(modified)` state, and the **"Revert to baseline"** button is now **enabled**.
3. Click **"Revert to baseline"**:
   - The lesson moves back to its original slot/room/teacher in the accepted baseline.
   - The `(modified)` badge and any conflict highlights caused by that lesson disappear.
   - The **"Revert to baseline"** button becomes **disabled** again.

## Status Update

- Status: `READY_FOR_CONVERGENCE` -> `PENDING_WALKTHROUGH`
- Next eligible UCs: none (pending UC-4 walkthrough confirmation)

## Response to execute

PENDING WALKTHROUGH: Awaiting human confirmation of UC-4 revert to baseline walkthrough.
