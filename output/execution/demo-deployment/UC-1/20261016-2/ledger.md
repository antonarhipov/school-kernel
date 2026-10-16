# UC-1 execution ledger — PASS

Date: 2026-10-16. Base `d55ab636558224f2d33a5e71313e840dd9486428`; exact changed source hashes:
[source-sha256.json](source-sha256.json). Actor: packaged Java 25 CLI; no UI or browser applicable.
All mutable test inputs, results and process logs use disposable JUnit temporary directories.

| Contract | Action | Expected and observed | Verdict |
|---|---|---|---|
| Main 1–3, G1 | Packaged initial plan with 1 ms search | Exit 3, NO_FEASIBLE_SOLUTION_FOUND, TIME_LIMIT, explanation of incomplete construction; no timetable/revision/score/change report; schema-valid | PASS |
| Main 1–3, G1/G3 | Produce a real feasible baseline; add an unassigned lesson; packaged repair with 1 ms search | Same outcome; baseline definition and result bytes unchanged | PASS |
| Main 4, G2 | Read safeMessage | Explains budget exhaustion before all lessons were placed and possibility of later success | PASS |
| Extension 2a | Packaged deterministic complete initial solve | FEASIBLE; its actual result supplies the repair baseline | PASS |
| Extension 2b | Existing PlanCliIT/ReplanCliIT hard-infeasible cases | Existing no-feasible results still pass | PASS |
| Extension 2c | Malformed JSON and missing file through packaged CLI | INVALID_INPUT/exit 2 versus transport/exit 74; missing input publishes no result | PASS |
| Extension 2c, G3 | Existing service fault-injection and incomplete STEP_LIMIT test | True internal errors remain INTERNAL_ERROR; no candidate published | PASS |
| G1/G2 | Independently evaluate candidate despite a solver-provided feasible flag | Incomplete time-limited candidate is unsuccessful; forged solver metadata cannot publish it | PASS |

Final command: `./mvnw -q -pl kernel-cli -am -Dtest=PlanServiceTest,ReplanServiceTest
-Dsurefire.failIfNoSpecifiedTests=false -Dit.test=IncompleteConstructionCliIT
-Dfailsafe.failIfNoSpecifiedTests=false verify` — exit 0. Twelve service checks and one packaged integration
journey pass. The earlier completed PlanCliIT (11) and ReplanCliIT (8) runs pass against the same production change.
Raw summaries and all failed-attempt Maven logs are retained beside this ledger.

Earlier harness findings: sub-millisecond durations are unsupported by Timefold; the new repair lesson needed its
required displayName; the established transport exit is 74. The harness was corrected to use a real 1 ms budget,
a valid complete successor, and the existing exit contract. These were not changes to production behavior.

No screenshot is useful for this CLI-only goal. No server, browser, database, or operator school was used.
JUnit processes are stopped and disposable directories cleaned by the runner. Separate synthetic CLI artifacts
from investigation remain under `../20261016-1/`. No destination handoff is needed for UC-1.
