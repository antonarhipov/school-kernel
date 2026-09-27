# Convergence: UC-1 - Open manual editing draft

## Summary

- Submission: spec/timetable-manual-editing/checkpoints/UC-1.md at c309189
- Verdict: APPROVED
- Findings: 0 critical, 0 gap, 0 protocol, 0 drift, 0 cosmetic
- Suite: 14 run, 0 failed, 0 errors, 0 skipped (`WorkspaceManualDraftIT` + `WorkspaceRepairDraftIT`)
- Working tree impact from verification: none

## Protocol Gate

1. Target UC: `UC-1` with status `READY_FOR_CONVERGENCE`: verified.
2. Checkpoint `spec/timetable-manual-editing/checkpoints/UC-1.md` committed at `c309189`: verified.
3. Dependencies: none (`UC-1` is root): verified.
4. No other UC is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`: verified (`UC-2` to `UC-6` are `NOT_STARTED`).
5. Checkpoint contains all contract, rule, and validation evidence rows: verified.
6. Diff contains only UC-1 slice and necessary shared enabling infrastructure: verified.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Timetable administrator | Step 1: Navigates to timetable view | In ACCEPTED_BASELINE, "Manual editing" button displayed | Verified in `app.js` (`#start-manual-draft` in `#workbench-task-area` launch) |
| Timetable administrator | Step 2: Clicks "Manual editing" | Dispatches `POST /api/manual-draft` with CSRF + ETag | Verified in `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline` (HTTP 200, ETag ws-8) |
| Timetable administrator | Step 3: Workspace enters `MANUAL_DRAFT` | Initial draft contains baseline assignments, zero modifications, zero conflicts | Verified in `WorkspaceManualDraftIT` (HTTP 200, state `MANUAL_DRAFT`, assignments match baseline) |
| Timetable administrator | Step 4: UI renders draft workbench | Displays "Manual draft open" state and "0 conflicts" badge | Verified in `app.js` (`#conflict-summary-badge`, `.conflict-badge.clean`, `M.zeroConflicts`) |
| Timetable administrator | Ext 1a: Attempt draft from non-accepted state | HTTP 409 `INVALID_WORKSPACE_TRANSITION` | Verified in `WorkspaceManualDraftIT.refusesOpenDraftFromNonAcceptedState` |
| Timetable administrator | Ext 3a: Stale version conflict | HTTP 412 `STALE_WORKSPACE_VERSION` | Verified in `WorkspaceManualDraftIT.enforcesOptimisticConcurrency` |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-1 main steps 1-4 | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L64-L89`: asserts HTTP 200, state `MANUAL_DRAFT`, version incremented, zero conflicts, assignments match baseline | STRONG | yes |
| UC-1 extension 1a | `WorkspaceManualDraftIT.refusesOpenDraftFromNonAcceptedState` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L91-L101`: asserts HTTP 409 `INVALID_WORKSPACE_TRANSITION` from `EMPTY` state, aggregate unchanged | STRONG | yes |
| UC-1 extension 3a | `WorkspaceManualDraftIT.enforcesOptimisticConcurrency` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L103-L119`: asserts HTTP 428 on missing `If-Match`, HTTP 412 on mismatched ETag, aggregate unchanged | STRONG | yes |
| UC-1 G1 | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L87`: asserts `acceptedBaseline` in DB document is byte-identical before and after draft creation | STRONG | yes |
| UC-1 G2 | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L82-L84`: asserts `manualDraft.assignments` copies baseline assignments by value | STRONG | yes |
| UC-1 success postcondition | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L72-L88`: asserts DB row `lifecycle_state='MANUAL_DRAFT'`, version incremented, working draft persisted | STRONG | yes |
| UC-1 minimal guarantee | `WorkspaceManualDraftIT.refusesOpenDraftFromNonAcceptedState` & `enforcesOptimisticConcurrency` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L100-L118`: asserts rejected transitions result in zero state mutation | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | V2 Flyway migration alters lifecycle_state check constraint to add MANUAL_DRAFT | `timetable-workspace/src/main/resources/db/migration/V2__add_manual_draft_state.sql` applied during PostgreSQL startup in `WorkspaceManualDraftIT` | PASS |
| RULE-2 | REST API endpoints adhere to CSRF + optimistic concurrency ETag precondition headers | `WorkspaceController.java` (`POST /api/manual-draft`), `SecurityConfiguration.java` (`permitAll`), `WorkspaceManualDraftIT.enforcesOptimisticConcurrency` | PASS |
| RULE-7 | Continuous containerized verification against PostgreSQL | `WorkspaceManualDraftIT.java` uses Testcontainers PostgreSQL 18.6 container | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Pre-existing repair draft suite | Workspace aggregate state machine and inspection UI | `mvn test -Dtest=WorkspaceRepairDraftIT` (11 tests pass) | PASS |

## Findings

None. All automated evidence is `STRONG`.

## Walkthrough

- **Persona:** Timetable Administrator
- **Starting State:** Workspace loaded in browser with an accepted baseline (`ACCEPTED_BASELINE`).
- **Steps:**
  1. Open the timetable application in the browser (`/`).
  2. Verify that in the action bar, a **Manual editing** button (`#start-manual-draft`) is visible next to "Start repair".
  3. Click **Manual editing**.
  4. Confirm that the application transitions into the draft view:
     - The state label displays **Manual draft open**.
     - A green badge displaying **0 conflicts** (`#conflict-summary-badge`) appears in the heading.
     - Mode navigation buttons allow switching between **Draft** and **Current**.
     - The timetable matrix displays the baseline assignments ready for editing.
  5. Refresh the page to verify draft persistence: the draft state remains active surviving browser reload.
- **User Confirmation:** Confirmed by user ("The manual editing button is visible and I get 'manual draft open' label in the inspector widget").

## Status Update

- Status change: `PENDING_WALKTHROUGH` -> `APPROVED`
- Next eligible: `UC-2`

## Response to execute

APPROVED
