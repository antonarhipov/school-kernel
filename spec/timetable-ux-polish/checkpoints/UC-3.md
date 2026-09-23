# Use-Case Checkpoint: UC-3 - Follow repair generation without losing timetable context

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `d44501e20d68a44399a494198ee1c624c08229d2` (first convergence verdict; original implementation base `71c7c0a8f40254dd650279d6d2486ffab4b85022`)
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-2's actual persisted conflict-free Draft, accepted identity, pin, selection, and range.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1–2: request and frozen Solving | `WorkspaceBrowserIT.inspectsFrozenRepairRunAndRecoversWithoutPublishingInRealBrowser` (`WorkspaceBrowserIT.java:1439–1472`): actual browser Draft POST/run ID, inspector cancellation and limit, frozen controls, exact stored accepted bundle and draft | PASS |
| Main 3–4: responsive modes and retained canvas | Same browser test (`:1454–1487`): exact identity, selection, Day and filters across Current/Draft/Solving/focused, unchanged DOM after polls, unchanged durable run during mode changes and narrow read-only | PASS |
| Main 5–6 and success postcondition | `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` (`:1309–1374`): packaged solver, independently verified proposal, Proposal mode on accepted canvas, retained selected accepted lesson, exact accepted bundle and Draft, feasible run with no active run/Solving mode | PASS |
| Extension 2a and minimal guarantee | Frozen browser test (`:1488–1493`): cancellation of blocked run, exact original accepted/Draft, terminal cancellation, no proposal, restored mutable Draft | PASS |
| Extension 2b | Frozen browser test (`:1495–1512`): production startup recovery listener followed by browser reload, durable INTERRUPTED lastRun and original draft/baseline, no proposal; released late process does not publish | PASS |
| Extension 3a | Modes share the accepted identity set; existing browser tests for Day selection invalidation and accepted preference fallback (`WorkspaceBrowserIT.java:740–912`), `app.js:245–283,449–460` | PASS |
| Extension 5a | `WorkspaceBrowserIT.showsFailedRepairEvidenceAndGatedRetryInRealBrowser` (`:1516–1568`): no feasible, invalid input, transport, interruption, rejected output, timeout; safe inspector and Utilities diagnostics, no raw process text, exact original accepted/Draft, no proposal | PASS |
| Extension 5b | Same browser test (`:1533–1545`): only eligible unchanged-intent retry starts a new run with `PT2M`; cancellation disallows further retry | PASS |
| Extension 6a | Same browser test (`:1546–1568`): mismatched kernel result rejected at packaged process boundary and cannot render/persist a candidate; HTTP integration tests additionally exercise stale/rejected outcomes | PASS |
| G1, G2 | Frozen browser test (`:1439–1487`): run has only contextual cancel; accepted IDs, filters, modes, inspector, selection, and unchanged run ID/document | PASS |
| G3, G4 | Feasible and failed browser tests (`:1309–1374,1439–1568`): byte-equivalent accepted bundle/Draft, only verified feasible result creates proposal | PASS |
| G5, G6 | Browser tests (`:1450–1492,1514–1568`): limit/status/native cancellation, diagnostic text, narrow read-only accepted focus and no cancellation | PASS |
| G7, G8 and minimal guarantee | All three browser journeys plus complete HTTP/kernel integration suite; guarded requests, safe recovery and late-result suppression, exact stored accepted/Draft and no proposal on negative paths | PASS |
| Revision G-1 / ext 2b, G7, RULE-3, RULE-11 | `WorkspaceBrowserIT.restoresInterruptedRepairAfterActualApplicationRestartInRealBrowser` (`:1573–1627`): browser starts run on separately started Spring application and isolated PostgreSQL; first application actually closes; fresh application recovers; browser connects to its new port; exact accepted bundle/Draft, original run ID, durable interrupted outcome, one version increment, no proposal, no stale write after releasing old process | PASS |
| Revision G-2 / G8, RULE-9, RULE-11 | `WorkspaceBrowserIT.followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshotInRealBrowser` (`:1629–1706`): independent packaged verifier confirms full accepted pair/revisions; 1,000 named lessons, 60 cohorts, 100 teachers/rooms, 60 periods; real teacher-16 period-0 Draft, selected lesson-960, run and modes with full identity and exact Monday IDs, cancellation and rejected output without candidate or mutation | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `app.js:36–44,106–130,470–535` uses one accepted model and complete workspace snapshot; no new dependency, endpoint, or alternate timetable source | PASS |
| RULE-2 | `inspection-state.js:26–40,101–114`, `app.js:36–44,129–142`; browser verifies retained view and exact unchanged persisted document across mode switches, reload defaults | PASS |
| RULE-3 | `RepairPlanningService.java:181–269`, `WorkspaceRepository.java:170–185`; browser and HTTP tests assert guarded solve/retry/cancel/recovery and no stale result | PASS |
| RULE-4 | `app.js:369–399,470–585`; browser asserts exact same accepted assignments, protected pin, frozen controls and unchanged accepted/Draft | PASS |
| RULE-6 | `app.js:287–348,462–467,569–590`, `messages.js:119–129`; accepted identity, escaped diagnostics and no raw process data | PASS |
| RULE-7 | `app.js:287–399,470–535`, `messages.js:119–129`; fixed existing inspector, applicable Utilities, native buttons, narrow read-only and descriptive text | PASS |
| RULE-8 | Existing security layer untouched; browser negative and full HTTP security suites green, no new route, no browser storage for run data | PASS |
| RULE-9 | `WorkspaceBrowserIT.java:1309–1706` uses independently verified normative-scale accepted fixture and isolated Postgres, exact accepted/Draft/ID assertions; full scale/inspection suite green | PASS |
| RULE-11 | `WorkspaceBrowserIT.java:1309–1706`, `WorkspaceRepairPlanningIT.java`: real actor-boundary main journey; cancellation/actual app restart/rejected/failure/retry/narrow/modes and complete regression | PASS |

## Validation

- Focused commands: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#inspectsFrozenRepairRunAndRecoversWithoutPublishingInRealBrowser+showsFailedRepairEvidenceAndGatedRetryInRealBrowser+generatesRepairProposalInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS; final success-evidence addition: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#generatesRepairProposalInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS.
- Full relevant suite: `./mvnw -pl timetable-workspace -am verify` PASS (26 unit; 75 integration including 27 browser, 0 failures/errors/skips); also previous initial submission PASS (26 unit, 73 integration).
- Revision focused command: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#restoresInterruptedRepairAfterActualApplicationRestartInRealBrowser+followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshotInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 2 browser).
- Working tree impact from tests: untracked `.output.txt` generated by command capture was removed; no tracked runtime files changed. Pre-existing `.idea/encodings.xml` untouched.
- Runtime evidence: local administrator, `/workspace/` packaged assets against isolated PostgreSQL; real solver feasible handoff, controlled process-boundary failure, production recovery listener and late-result guard, exact persisted state checked after each outcome.
- Changed files in initial implementation: `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceRepository.java`, `timetable-workspace/src/main/resources/static/workspace/{app.js,messages.js}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/{WorkspaceBrowserIT.java,WorkspaceRepairPlanningIT.java}`, `spec/timetable-ux-polish/{status.md,checkpoints/UC-3.md}`. Revision changes only `WorkspaceBrowserIT.java` and the two UC-3 evidence reports.
- Approved UCs regression-tested: UC-1, UC-2 and their accepted-inspection prerequisite through all 75 integration tests including 27 browser tests; 0 failures/errors/skips.

## Notes

No product deviation. UC-4 combined comparison is deliberately not implemented here; UC-3 retains the accepted canvas and context for its handoff. Human walkthrough remains for independent convergence.

READY FOR CONVERGENCE: UC-3