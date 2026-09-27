# Convergence: UC-3 - Inspect conflict details and explanation

## Summary

- Submission: `spec/timetable-manual-editing/checkpoints/UC-3.md` at `ae4ba8e`
- Verdict: APPROVE
- Findings: 0 critical, 0 gap, 0 protocol, 0 drift, 0 cosmetic
- Suite: 8 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

1. Target UC: UC-3, status `READY_FOR_CONVERGENCE`: PASS
2. `checkpoints/UC-3.md` exists and is committed: PASS
3. Dependencies `UC-1` and `UC-2` are `APPROVED`: PASS
4. No other UC is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`: PASS
5. Checkpoint includes complete evidence rows across scenario, extensions, guarantees, rules: PASS
6. Diff contains only UC-3 implementation (conflict cues, indicator markup, adjacent popup overlay, and test): PASS

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Reassigns lesson into slot occupied by another class | Reassigned with conflicts detected | Observed: Red conflicting border + ⚠️ conflict indicator rendered on cell |
| Administrator | Clicks or hovers ⚠️ conflict indicator | Explanatory overlay pops up | Observed: Adjacent `.conflict-overlay` appears with conflict count, code, description, and competing assignments |
| Administrator | Inspects multiple concurrent conflicts (Ext 2a) | Multi-conflict itemization | Observed: All clashes (`TEACHER_CLASH`, `ROOM_CLASH`, `COHORT_CLASH`) itemized in overlay and inspector |
| Administrator | Selects conflicting cell | Inspector details panel opens | Observed: Inspector displays itemized conflict warnings and competing lessons |
| Administrator | Dismisses overlay (Escape / click outside) | Overlay hides | Observed: `.conflict-overlay` dismissed without altering timetable state |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-3 main step 1 | Indicator activation / cell selection | `app.js` (`.conflict-indicator`, `selectLesson`), `WorkspaceManualDraftIT.java:272` | STRONG | yes |
| UC-3 main step 2 | Adjacent explanatory overlay | `app.js` (`conflictOverlayMarkup`, `bindConflictOverlays`), `styles.css` (`.conflict-overlay`) | STRONG | yes |
| UC-3 main step 3 | Conflict description, code, resource, competing lessons | `WorkspaceManualDraftIT.java:309`, `app.js` (`competing-meta`) | STRONG | yes |
| UC-3 main step 4 | Dismissal on Escape / click outside | `app.js` (`bindConflictOverlays`), non-mutating | STRONG | yes |
| UC-3 extension 2a | Multi-conflict enumeration | `WorkspaceManualDraftIT.java:302` (3 simultaneous conflicts on single lesson) | STRONG | yes |
| UC-3 G1 | Complete causal explanation | `WorkspaceManualDraftIT.java:309` (`resourceType`, `resourceId`, `periodId`, `description`, `competingLessonIds`) | STRONG | yes |
| UC-3 G2 | Non-mutating inspection | `WorkspaceManualDraftIT.java:328` (ETag and DB state byte-identical after GET `/api/workspace`) | STRONG | yes |
| UC-3 success postcondition | Explanatory overlay displayed with clear actionable rationale | `conflictOverlayMarkup`, `styles.css`, `WorkspaceManualDraftIT.java:309` | STRONG | yes |
| UC-3 minimal guarantee | Canvas and draft state unchanged during inspection | `WorkspaceManualDraftIT.java:328` | STRONG | yes |
| Extends UC-2 | Extends UC-2 at extension point 6a | `ManualDraftService.java:128`, `app.js` | STRONG | yes |
| Requires UC-1 | Requires manual draft to be open | `ManualDraftService.java`, `WorkspaceManualDraftIT.java:275` | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-3 | Normative constraints authoritatively detected with exact codes and causal reasons | `ManualDraftService.evaluateConflicts`, `WorkspaceManualDraftIT.java:302` | PASS |
| RULE-5 | Immediate cell highlighting and adjacent explanatory overlay explaining conflict causes | `styles.css`, `app.js`, `WorkspaceManualDraftIT.java:309` | PASS |
| RULE-7 | Automated integration verification on real containerized PostgreSQL | `WorkspaceManualDraftIT.java` (8/8 passing via Testcontainers) | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Draft lifecycle, base model | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline`, `refusesOpenDraftFromNonAcceptedState`, `enforcesOptimisticConcurrency` | PASS |
| UC-2 | Draft mutation & conflict detection | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications`, `detectsClashesAndPersistsConflicts`, `revertsIndividualLessonCleanly` | PASS |

## Findings

None.

## Walkthrough

1. Open the Timetable Workspace UI in the browser (`http://localhost:8080`).
2. Click **Start Manual Editing** to enter `MANUAL_DRAFT` mode.
3. Select any lesson in the matrix and use the Assignment Editor in the Inspector panel to reassign it to a slot already occupied by another class.
4. Click **Save Assignment**.
5. Observe:
   - The reassigned cell is highlighted with a red conflict border and displays a `⚠️` conflict indicator.
   - Click or hover the `⚠️` indicator: an explanatory popup overlay appears directly adjacent to the cell, itemizing each detected conflict (e.g. `TEACHER_CLASH`, `COHORT_CLASH`, `ROOM_CLASH`), explaining the double-booking, and identifying competing assignments.
   - The Inspector sidebar also itemizes the conflict causes.
   - Press `Escape` or click outside the overlay: the overlay closes cleanly.

Walkthrough confirmed and validated by user on 2026-09-28.

## Status Update

- Status: `PENDING_WALKTHROUGH` -> `APPROVED`
- Next eligible UCs: UC-4, UC-5, UC-6

## Response to execute

APPROVED: UC-3 is verified and approved. Execution may proceed to UC-4.
