# Use-Case Checkpoint: UC-4 - Prepare a protected repair draft

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `f3b9253b67f9be528cd6b6ff90343ba9b4992f28`
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-1; the production repair service consumes the approved exact accepted definition/result/manifest bundle and never replaces it.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-7 | `WorkspaceRepairDraftIT.preparesDurableRepairDraftWithoutChangingAcceptedBundle` and `stagesRoomUnavailabilityAndMaterializesRemainingPeriods` exercise teacher and room intent, direct effects, individual pins, durable summary, conflicts, and compilation over real HTTP/PostgreSQL; `WorkspaceBrowserIT.preparesProtectedRepairDraftInRealBrowser` performs the complete desktop actor journey in real Chrome. | PASS |
| Extension 1a | `handlesExtensionsAndRestoresDurableDraft` submits an unsupported teacher-reassignment action, receives `422 INVALID_REPAIR_DRAFT`, and compares the exact accepted bundle and lifecycle. | PASS |
| Extension 2a | The same test stages teacher unavailability in `mon-3`, asserts an empty direct-effect set, retains the staged change, and restores the exact durable draft through the repository boundary. | PASS |
| Extensions 4a and 7a | `blocksConflictingPinsWithoutChoosingAWinner` proves both an attempt period pin and persistent policy period lock produce the named lesson conflict and `readyToSolve=false`; removing only the attempt pin resolves that case. | PASS |
| Extension 5a | `previewsConfirmsAndUndoesExactBulkSnapshot` proves preview itself leaves pins empty; the browser exposes and can cancel the preview without a mutation command. | PASS |
| Extension 6a | The same test confirms a bulk snapshot, independently pins its room, undoes the bulk action, and proves only the independent room source remains. | PASS |
| Extension 6b | `autoSaveFailureRetainsLastDurableDraft` injects a PostgreSQL update failure, receives `503 STORAGE_UNAVAILABLE`, compares the exact prior draft/baseline, and proves run submission is refused. | PASS |
| Extension 7b | `handlesExtensionsAndRestoresDurableDraft` requires explicit discard confirmation, refuses an unconfirmed request, then clears intent/pins and returns to the byte-equivalent accepted baseline. | PASS |
| Extension 7c | The durable-draft test compares the HTTP snapshot to a fresh repository load; `REPAIR_DRAFT` is a stable lifecycle state untouched by startup recovery. | PASS |
| G1 | Field-by-field compilation tests prove deep-copy overlay, accepted `inputRevision` lineage, omitted-availability materialization, and accepted-value period/room locks while the stored accepted definition/result remain equal. | PASS |
| G2 | Bulk tests preview `UNAFFECTED` as exactly `lesson-science-1`, cover deterministic DAY/CLASS/filter-independent selection, and prove confirmation recomputes the complete server snapshot while altered or stale previews cannot mutate the draft. | PASS |
| G3 | Individual and bulk sources coexist in deterministic pin provenance; exact undo retains the independent source, discard clears all attempt state, and persistent manifest locks remain conflicts/policy state. | PASS |
| G4 | Real Chrome observes direct, pinned, conflict, and accepted text states, uses trusted keyboard activation for lesson selection and pinning, and proves narrow-screen editing is absent; policy/unpinned states are catalog-backed visible labels. | PASS |
| G5 | Every HTTP, failure, bulk, conflict, discard, and browser journey compares the exact accepted baseline; no repair mutation updates accepted JSON. | PASS |
| G6 | `measuresTargetScalePinFeedbackInRealBrowser` uses 1,000 lessons, 100 teachers, 60 classes, 100 rooms, and 60 periods; final clean-run persisted pin-feedback p95 was 166.3 ms, below 250 ms with solver time excluded. | PASS |
| Success postcondition | Real HTTP/PostgreSQL assertions leave one `REPAIR_DRAFT` with exact changes, direct-effect revision/set, deterministic non-conflicting pins, intent revision, and `persisted=true`; compilation consumes that state without accepted mutation. | PASS |
| Minimal guarantee | Conflict, stale/missing version, malformed request, unsupported action, autosave failure, unconfirmed discard, and refused solve tests prove the accepted bundle is byte-equivalent and no invalid/unpersisted draft is submitted. | PASS |
| Requires UC-1 | UC-1 is approved; all UC-4 journeys begin with its persisted accepted definition/result/manifest shape through the production singleton aggregate and conditional mutation path. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Clean reactor dependency/archive/ArchUnit checks pass; UC-4 adds only workspace classes/assets and no kernel implementation dependency. | PASS |
| RULE-2 | Java 25 clean verification resolves the pinned Spring Boot 4.1.1, PostgreSQL 18.6, JdbcClient, and Testcontainers stack; no dependency or frontend framework was added. | PASS |
| RULE-3 | `RepairDraftService` stores intent, revisions, effects, conflicts, bulk snapshots, and pins inside the existing singleton JSONB document; repository tests compare full documents. | PASS |
| RULE-4 | No schema change was required; the complete fresh/checksum/failure Flyway suite remains green. | PASS |
| RULE-5 | Start, edit, discard, and refused-run tests cover the allowed accepted-baseline/repair-draft edges and `INVALID_WORKSPACE_TRANSITION` refusal without mutation. | PASS |
| RULE-6 | Missing/stale/racing HTTP tests prove `428`/`412`, matching ETags, conditional SQL versioning, one winner, and exact non-mutation. | PASS |
| RULE-8 | Compilation deep-copies lossless accepted JSON, treats IDs opaquely, applies deterministic ordering, uses RFC-8785 canonical intent hashing, and preserves omitted versus materialized availability at the accepted boundary. | PASS |
| RULE-15 | Teacher/room table journeys compare overlay, materialized availability, lineage, copied accepted pins, persistent locks, and unchanged accepted documents by value. | PASS |
| RULE-16 | Preview records source version, direct-effect revision, ordered lesson IDs, dimensions, count, scope, and conflicts from one loaded aggregate; confirmation canonically recomputes the complete snapshot, rejects alteration/duplicate/staleness, and undo removes only its source. DAY, CLASS, UNAFFECTED, filters, later edits, overlaps, individual pins, conflicts, and empty snapshots are covered. | PASS |
| RULE-19 | `GET /api/workspace` carries the full accepted bundle/draft snapshot; native ES modules render and mutate only intent commands with the current ETag; real Chrome covers desktop and narrow boundaries. | PASS |
| RULE-20 | All new visible English comes from `messages.js`; visible state text accompanies color, native controls are keyboard-operable, and the existing message-catalog architecture test passes. | PASS |
| RULE-21 | Security configuration allowlists exactly the specified repair routes; real HTTP uses session CSRF, accepted Origin/Host, and current ETag; the unchanged security matrix passes. | PASS |
| RULE-22 | Repair validation, transition, stale, malformed, conflict, and storage failures use the stable safe problem shape; negative tests inspect codes/status and absence of mutation. | PASS |
| RULE-23 | UC-4 adds no draft/pin logging and persists no solver evidence; the existing prohibited-value logging suite remains green. | PASS |
| RULE-24 | Success and negative journeys run through ephemeral HTTP and disposable PostgreSQL 18.6; real Chrome covers the actor path and scale path, with exact state assertions after consequential steps. | PASS |
| RULE-25 | The real-browser scale test records 20 post-load persisted pin-feedback samples, calculates nearest-rank p95, asserts below 250 ms, and excludes solver time. | PASS |

## Validation

- Focused commands: `./mvnw -q -pl timetable-workspace -am -Dtest=WorkspaceRepairDraftIT -Dsurefire.failIfNoSpecifiedTests=false test` - PASS; real-Chrome UC-4 journey - PASS; target-scale pin feedback - PASS after replacing a full 1,000-lesson rerender whose first run measured 369.8 ms p95 with a confirmed incremental update path.
- Full relevant suite: `./mvnw -q clean verify` - 152 tests, 0 failures, 0 errors, 0 skipped.
- Working tree impact from tests: none; `git diff --check` passes.
- Runtime evidence: real Chrome over ephemeral HTTP/PostgreSQL stages teacher unavailability, exposes direct/conflict/pin state, resolves a conflict, previews/confirms/undoes a bulk snapshot, verifies narrow read-only behavior, and explicitly discards the draft with zero browser errors.
- Changed files: workspace repair service/controller/mutation/security/problem mapping; native `app.js`, `messages.js`, and `styles.css`; PostgreSQL/HTTP and browser tests; status, checkpoint, and Jev bundle.
- Approved UCs regression-tested: timetable-workspace UC-1 through UC-3 and kernel UC-1/UC-2 all pass in the 152-test clean reactor.
- Jev preflight: `UC-4.jev-bundle.json` validates locally with 35 atomic items for pinned `jev-1.13.0`; external review is `REVIEW` because this turn did not authorize disclosure of repository paths, excerpts, test observations, and project claims to TypeSafe. No report was produced and no semantic approval is claimed.

## Notes

- The first target-scale implementation rebuilt the complete matrix after every persisted pin and missed G6 at 369.8 ms p95. The submitted incremental response path updates only the confirmed lesson/panel/count state; the final clean run measured 166.3 ms p95.
- The required administrator walkthrough remains the independent convergence gate; this checkpoint claims automated technical readiness only.
- Jev `REVIEW` is an authorization limitation rather than counterevidence; every deterministic claim remains backed by the clean suite and cited real HTTP/PostgreSQL/browser evidence.

READY FOR CONVERGENCE: UC-4
