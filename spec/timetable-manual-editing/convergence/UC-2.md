# Convergence: UC-2 - Reassign lesson slot, room, or teacher

## Summary

- Submission: spec/timetable-manual-editing/checkpoints/UC-2.md at 5273fab
- Verdict: APPROVED
- Findings: 0 critical, 0 gap, 0 protocol, 0 drift, 0 cosmetic
- Suite: 7 run, 0 failed, 0 errors, 0 skipped (`WorkspaceManualDraftIT`), full workspace suite 26 run, 0 failed
- Working tree impact from verification: none

## Protocol Gate

1. Target UC: `UC-2` with status `READY_FOR_CONVERGENCE` (in checkpoint): verified.
2. Checkpoint `spec/timetable-manual-editing/checkpoints/UC-2.md` committed at `5273fab`: verified.
3. Dependencies: `UC-1` is `APPROVED`: verified.
4. No other UC is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`: verified (`UC-3` to `UC-6` are `NOT_STARTED`).
5. Checkpoint contains all contract, rule, and validation evidence rows: verified.
6. Diff contains only UC-2 slice and necessary enabling infrastructure: verified.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Timetable administrator | Step 1-3: Views draft and selects lesson | Lesson cell clicked in matrix, inspector opens with lesson details | Verified in `app.js` (`selectLesson`, `lessonDetails`) |
| Timetable administrator | Step 4-5: Modifies period, room, or teacher and saves | Assignment editor dropdowns populated; form submit dispatches `PATCH /api/manual-draft` | Verified in `app.js` (`bindManualEditor`, `mutateJson`) |
| Timetable administrator | Step 6: Server evaluates conflicts and persists mutation | Server executes authoritative `evaluateConflicts` and replaces draft in PostgreSQL | Verified in `ManualDraftService.mutateDraft` and `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` |
| Timetable administrator | Step 7-8: Canvas reflects moved lesson and displays modified cue | Lesson cell renders in new slot/room, marked with `.modified` and `Modified from accepted` badge | Verified in `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` & `app.js` |
| Timetable administrator | Ext 6a: Conflict detected | Double-booking or room/teacher incompatibility generates conflict items, cell gets `.conflicting` border, conflict badge updates | Verified in `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` |
| Timetable administrator | Ext 3a, 4a: Stale version / invalid entity | HTTP 412 `STALE_WORKSPACE_VERSION` or HTTP 422 `INVALID_*` returned | Verified in `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-2 main steps 1-8 | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L128-L161`: asserts HTTP 200, state `MANUAL_DRAFT`, modified lesson tracked in `modifications`, zero conflicts, baseline untouched | STRONG | yes |
| UC-2 extension 3a, 4a | `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L237-L268`: asserts HTTP 428 on missing `If-Match`, HTTP 412 on mismatched ETag, HTTP 422 on invalid lesson or period | STRONG | yes |
| UC-2 extension 6a | `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L163-L203`: asserts detection of `TEACHER_CLASH`, `ROOM_CLASH`, `COHORT_CLASH`, and `ROOM_INCOMPATIBLE`, with competing lesson IDs and descriptions | STRONG | yes |
| UC-2 G1 | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L159`: asserts `acceptedBaseline` in DB document remains unchanged | STRONG | yes |
| UC-2 G2 | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L151-L157`: asserts `modifications[lessonId]` records `periodChanged=true`, `roomChanged=false`, `teacherChanged=false`, `originalPeriodId` | STRONG | yes |
| UC-2 G3 | `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L199-L202`: asserts conflicting state is persisted in database and workspace remains in `MANUAL_DRAFT` | STRONG | yes |
| UC-2 success postcondition | `WorkspaceManualDraftIT.reassignsLessonCleanlyAndTracksModifications` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L146-L150`: asserts aggregate updated with new assignment, new ETag, and `MANUAL_DRAFT` lifecycle | STRONG | yes |
| UC-2 minimal guarantee | `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` | `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java#L251-L268`: asserts invalid mutations make no changes to workspace state | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-2 | REST API endpoints adhere to CSRF + optimistic concurrency ETag precondition headers | `WorkspaceController.java` (`PATCH /api/manual-draft`), `SecurityConfiguration.java` (`permitAll`), `WorkspaceManualDraftIT.enforcesOptimisticLockingAndInputValidation` | PASS |
| RULE-3 | Authoritative server-side conflict evaluation engine covering all 6 normative constraints | `ManualDraftService.evaluateConflicts` authoritatively detects `TEACHER_UNAVAILABLE`, `ROOM_UNAVAILABLE`, `TEACHER_CLASH`, `ROOM_CLASH`, `COHORT_CLASH`, `ROOM_INCOMPATIBLE`; verified in `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` | PASS |
| RULE-5 | Working Draft persistence: edits and conflicts persist in database while keeping baseline intact | `WorkspaceManualDraftIT.detectsClashesAndPersistsConflicts` verifies DB row persistence and baseline isolation | PASS |
| RULE-7 | Continuous containerized verification against PostgreSQL | `WorkspaceManualDraftIT.java` executes against Testcontainers PostgreSQL 18.6 | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|\n| UC-1 | Manual draft lifecycle initialization and concurrency | `WorkspaceManualDraftIT.opensManualDraftFromAcceptedBaseline`, `refusesOpenDraftFromNonAcceptedState`, `enforcesOptimisticConcurrency` (all pass) | PASS |
| Accepted / Repair Workspace Suite | Full workspace aggregate domain operations | `mvn test -pl timetable-workspace` (26 tests pass) | PASS |

## Findings

None. All automated contract assertions are `STRONG` and verified against Testcontainers PostgreSQL.

## Walkthrough

- **Persona:** Timetable Administrator
- **Starting State:** Workspace loaded in browser in `MANUAL_DRAFT` mode with "Manual draft open" and "0 conflicts".
- **Steps:**
  1. Click on any lesson cell in the timetable matrix (e.g. Science 1).
  2. Notice that the Inspector sidebar opens, displaying the **Assignment Editor** section with dropdowns for **Period**, **Room**, and **Teacher**.
  3. Select a new period (e.g., Monday 3) and click **Save assignment**.
  4. Notice that:
     - The lesson moves to Monday 3 on the timetable grid.
     - The cell displays dashed border styling and a **Modified from accepted** badge.
     - The header badge maintains **0 conflicts**.
     - A **Revert to accepted** button appears in the Inspector sidebar.
  5. Now select a conflicting period or room (e.g., assign it to Monday 1 in Room 102 where Mathematics 1 is already assigned, or assign it to an incompatible room lacking the "lab" capability):
     - Click **Save assignment**.
     - Notice that the header badge turns red and updates to show the conflict count (e.g., **2 conflicts**).
     - The conflicting cells are highlighted in the timetable with red borders (`.conflicting`).
     - In the Inspector sidebar, warning callouts (`⚠️`) detail the conflict reasons (e.g., *Room Room 102 is double-booked in period Monday 1 with Mathematics 1* and *Teacher Alex is double-booked in period Monday 1 with Mathematics 1*).
  6. Refresh the browser page: confirm that all manual edits, modified statuses, and conflicts remain intact across page reloads.
  7. Click **Revert to accepted**: verify the lesson reverts to its original baseline slot and room, modifications clear, and conflicts return to zero.
- **User Confirmation:** Confirmed by user ("confirmed").

## Status Update

- Status change: `PENDING_WALKTHROUGH` -> `APPROVED`
- Next eligible: `UC-3`

## Response to execute

APPROVED
