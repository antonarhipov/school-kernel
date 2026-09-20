# Use-Case Checkpoint: UC-2 - Replan a current timetable

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `498fb2e358cf03b46dc5f4bae84072691b5cad28`
- Submission commit: `HEAD at convergence`
- Relations verified: Requires UC-1; every packaged UC-2 success journey generates and consumes its baseline through the approved `plan` command

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-3 and success postcondition | `ReplanCliIT.mainSuccessAndDirectSuccessor`; fresh plan/replan launcher journey; revised output is accepted as the next direct baseline | PASS |
| Extension 1a | Replan with both limits exits 64 and creates no output | PASS |
| Extension 1b | Output equal to current exits 64 and current bytes remain identical | PASS |
| Extension 1c | Missing definition exits 74, no output, current preserved | PASS |
| Extension 2a | Malformed current returns `INVALID_INPUT`, no solver/timetable | PASS |
| Extension 2b | Tampered revision, wrong lineage, and wrong school are rejected before solving | PASS |
| Extension 2c | Contradictory period lock is semantic invalid input before solving | PASS |
| Extension 2d | Main fixture accepts the current timetable becoming stale under a complete updated definition | PASS |
| Extension 2e | `emptyUpdateReportsCancellations` bypasses search and lists both cancellations | PASS |
| Extension 2f | No-room updated definition returns deterministic preflight failure without search | PASS |
| Extension 2g | Step-bounded main/change journeys publish the best complete feasible result with actual termination | PASS |
| Extension 2h | Four lessons competing for three periods exhaust search; result has diagnostics, no timetable/change report | PASS |
| Extensions 2i-2k | Replan uses the approved safe internal envelope, interrupt checks, and `FileBoundary` atomic publication path regression-tested by UC-1 | PASS |
| G1-G3 | UC-1 rules regress green; `CurrentTimetableReader` validates strict result schema, status, school, revision, and direct lineage before solving | PASS |
| G4-G8 | Common-ID baseline mapping plus stability constraints/evaluator; `stabilityMoves` verifies period, room-only, and forced exclusions | PASS |
| G9 | Packaged tests assert additions, cancellations, teacher changes, forced moves, period moves, and room-only moves by exact IDs/fields | PASS |
| G10 | Revised assignments preserve all six snapshot IDs and produce a new verifiable timetable revision | PASS |
| G11 | Limit-bounded score is reported without global-minimality claim | PASS |
| Minimal guarantee | All refused/failed paths omit replacement timetable/change report and preserve current/destination bytes | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 through RULE-5 | Existing boundary/schema/validation/revision suites plus `CurrentTimetableReader` and packaged tamper tests | PASS |
| RULE-6 | Bendable levels 0/1/2 implement period, room-only, preference ordering; one thread/seed/limit suite remains green | PASS |
| RULE-7 and RULE-8 | Catalog rows unchanged; stability rows are separate product-score components; failed search exposes diagnostics only | PASS |
| RULE-9 through RULE-12 | Replan shares atomic file boundary, centralized CLI, stateless runtime, shaded JAR, launcher, and wrapper | PASS |
| RULE-14 | Every baseline is generated through production UC-1, parsed with the strict result schema, revision-verified, and byte-preserved | PASS |
| RULE-15 | Common-ID stability facts and deterministic exact change classification in packaged tests | PASS |
| RULE-16 | 53-test clean lifecycle: schema, validation, constraint, solver, 18 packaged processes; no tracked writes | PASS |
| RULE-17 | Packaged replan stdout is empty; solver lifecycle stays on stderr; no documents/timetables are logged | PASS |

## Validation

- Focused commands: packaged plan -> replan -> replan direct-successor flows and exact negative fixtures.
- Full relevant suite: `./mvnw -q clean verify` - 53 run, 0 failed, 0 errors, 0 skipped.
- Working tree impact from tests: none; only ignored `target/` artifacts.
- Runtime evidence: real launcher returned `FEASIBLE`, preserved both baseline assignments, reported zero moves and six empty change categories, and left current bytes unchanged.
- Changed files: exact groups are listed in `spec/status.md`.
- Approved UCs regression-tested: UC-1, all 45 prior checks remain green within the 53-test run.

## Notes

The four concurrent Merivälja fixture files remain excluded. No deviations.

READY FOR CONVERGENCE: UC-2
