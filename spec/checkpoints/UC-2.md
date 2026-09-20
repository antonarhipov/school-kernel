# Use-Case Checkpoint: UC-2 - Replan a current timetable

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `498fb2e358cf03b46dc5f4bae84072691b5cad28`
- Revision base commit: `801deb59066831d9e213245305cf1ee460613136`
- Submission commit: `HEAD at convergence`
- Relations verified: Requires UC-1; every packaged UC-2 success journey generates and consumes its baseline through the approved `plan` command

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-3 and success postcondition | `ReplanCliIT.mainSuccessAndDirectSuccessor`; fresh plan/replan launcher journey; revised output is accepted as the next direct baseline | PASS |
| Extension 1a | Replan with both limits exits 64 and creates no output | PASS |
| Extension 1b | Output equal to current exits 64 and current bytes remain identical | PASS |
| Extension 1c | Missing input and overwrite refusal are packaged; injected resource safeguard and publication failure preserve prior bytes | PASS |
| Extension 2a | Malformed input and a schema-valid, revision-verifiable duplicate lesson assignment return `INVALID_INPUT` before solving | PASS |
| Extension 2b | Tampered revision, wrong lineage, and wrong school are rejected before solving | PASS |
| Extension 2c | Contradictory period lock is semantic invalid input before solving | PASS |
| Extension 2d | Main fixture accepts the current timetable becoming stale under a complete updated definition | PASS |
| Extension 2e | `emptyUpdateReportsCancellations` bypasses search and lists both cancellations | PASS |
| Extension 2f | No-room updated definition returns deterministic preflight failure without search | PASS |
| Extension 2g | Step-bounded main/change journeys publish the best complete feasible result with actual termination | PASS |
| Extension 2h | Four lessons competing for three periods exhaust search; result has diagnostics, no timetable/change report | PASS |
| Extensions 2i-2k | Injected UC-2 internal/interrupt/publication failures plus packaged SIGINT prove safe result, exit 130/no publication, and exit 74/prior-byte preservation | PASS |
| G1-G3 | UC-1 rules regress green; `CurrentTimetableReader` validates strict result schema, status, school, revision, and direct lineage before solving | PASS |
| G4-G8 | Common-ID mapping, exact 1-hard/3-soft levels, solver trade-off fixtures, and independent period/room lock journeys prove stability ordering and exclusions | PASS |
| G9 | Packaged tests compare complete sorted arrays, all old/new fields, independent forced dimensions, allowed teacher overlap, and forbidden move overlap | PASS |
| G10 | Revised assignments preserve all six snapshot IDs and produce a new verifiable timetable revision | PASS |
| G11 | Limit-bounded score is reported without global-minimality claim | PASS |
| Minimal guarantee | Refusal, safeguard, validation, search, interruption, and injected publication paths omit replacement timetable/change report and preserve current/destination bytes | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 through RULE-5 | Existing suites plus deterministic duplicate-current rejection prove schema, semantic, revision, lineage, and pre-solver validation | PASS |
| RULE-6 | Configuration asserts one hard/three soft levels; deterministic solver fixtures prove period > room > preference choices | PASS |
| RULE-7 and RULE-8 | Catalog rows unchanged; stability rows are separate product-score components; failed search exposes diagnostics only | PASS |
| RULE-9 through RULE-12 | UC-2-specific interrupt/publication/safeguard tests cover the shared atomic boundary; packaged CLI, stateless runtime, shaded JAR, launcher, and wrapper remain green | PASS |
| RULE-14 | Production UC-1 baselines are strict-schema/revision/semantic validated, including duplicate-ID rejection, and byte-preserved | PASS |
| RULE-15 | Trade-off solver fixtures and full-value packaged reports prove ordering, independent locks, exact categories, ordering, and overlap semantics | PASS |
| RULE-16 | 61-test clean lifecycle: 42 unit and 19 packaged tests; no tracked writes | PASS |
| RULE-17 | Packaged replan stdout is empty; solver lifecycle stays on stderr; no documents/timetables are logged | PASS |

## Validation

- Focused commands: service failure injection, solver dominance trade-offs, packaged plan -> replan -> replan direct-successor, exact report, duplicate-current, and SIGINT paths.
- Full relevant suite: `./mvnw -q clean verify` - 61 run, 0 failed, 0 errors, 0 skipped.
- Working tree impact from tests: none; only ignored `target/` artifacts.
- Runtime evidence: real launcher returned `FEASIBLE`, preserved both baseline assignments, reported zero moves and six empty change categories, and left current bytes unchanged.
- Changed files: exact groups are listed in `spec/status.md`.
- Approved UCs regression-tested: UC-1, all 45 prior checks remain green within the 61-test run.

## Notes

C-1 and G-1 through G-3 from the prior convergence report are addressed. The four concurrent Merivälja fixture files remain excluded. No deviations.

READY FOR CONVERGENCE: UC-2
