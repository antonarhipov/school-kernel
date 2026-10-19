# Audience contract (schema 2, catalog 11)

Definitions retain their exact canonical revision. `audiencePlans` declare class/subject weekly demand and stable
portions/session IDs, independently of selected teachers or placements. A whole-class audience is `class:<classId>`;
a grouped audience uses its stable group ID. Only arrangements with actual group sessions require joint membership
data. Referenced inactive splits can remain in the definition without creating fictitious attendance.

`review --definition school.json --assignments candidate.json --output review.json` accepts either a complete
assignment array or a complete schema-2 result. Each audience session supplies `lessonId`, `subjectId`, `cohortId`,
`audienceId`, `allocationId`, `teacherId`, `periodId` and `roomId`. Automatic/fixed allocations use the portion ID;
intentional sharing uses the declared share ID. Legacy fixed lessons may retain the six-field assignment form.
Complete results additionally carry independently checked weekly allocations and coordination certificates.

Review never invokes generation or changes supplied placements. It checks actual resources once per session and
attendance for every disjoint anonymous block. `period.gapBoundaryAfter` marks an explicit lunch boundary separately
from consecutive adjacency. Class unavailability also splits gap accounting; empty reserved periods are excluded.
Root `sessionLocks` contain stable `sessionId` and an optional `periodId`/`roomId` (at least one required).

The score separates automatic weekly teacher changes, period moves, room-only moves, internal student-gap periods,
and ordinary preferences. Gap cost always counts students, independently of ordinary preference weights. Coverage
cost counts untaught students only at periods where their parent class has a session. Workload references are
monitoring data and contribute no score. `quality.gaps` retains at most 1,000 explanatory rows, with an explicit
total and truncation flag. Aggregate counts remain complete.

A comparison result carries `repairContext`: exact baseline `definition`, `definitionRevision`, `timetableRevision`,
`assignments` and `coordinationMatches`. The baseline is independently reviewed; its historic score is never trusted.
Successor `basedOnRevision` retains the host contract's baseline **definition** revision. Comparison uses stable
session/audience/portion identities and represented anonymous composition; labels and ordering never establish
correspondence. Added, removed or changed demand is reported separately from comparable stability costs.

`verify --definition school.json --result timetable.json --output verification.json` checks the full pair and returns
recomputed score/quality plus `assertedQualityMismatch`. Review also preserves asserted score/quality when they differ.
A quality assertion cannot establish hard validity. Unsupported fields/versions, unresolved identities and incomplete
session sets return located `INVALID_INPUT` diagnostics. Hard-invalid complete assignments return `REVIEWED` with
`valid:false`, conflicts and no publishable result.

Independent coordination matching is bounded to 250,000 search nodes and three seconds, with a recursion safeguard.
Exhaustion reports **validity unestablished**, never proven invalidity. A complete supplied matching certificate is
checked directly without matching search. The JVM property `school.kernel.maxCoordinationReviewNodes` can lower the
node budget for deterministic integration/fault checks. All refusals preserve inputs and accepted state.

`plan --definition school.json --step-limit 10000 --seed 0 --output result.json` searches all actual sessions,
choosing eligible weekly teachers, compatible periods and rooms. `replan` additionally requires `--current-definition`
and `--current` for the exact accepted pair, and successor `basedOnRevision` naming the baseline definition revision.
Fixed teacher policies and session locks remain hard; automatic weekly teachers are the first repair stability tier.
No baseline is inferred for initial generation. Separate tuple components rank weekly teacher, period and room changes,
then student-gap periods and ordinary cost; no weighted sum can trade away an earlier tier.

Audience search uses bounded finite enumeration with resource/attendance pruning and partial stability bounds.
`--step-limit` counts attempted actual-session placements; `--time-limit` bounds search wall time. Independent review
also runs before publication. Search may return its best complete checked candidate at a limit, or
`NO_FEASIBLE_SOLUTION_FOUND` without a partial timetable. `searchOutcome` discloses visited nodes, complete and reviewed
candidates, unestablished matching reviews and whether finite search exhausted. Only complete exhaustion with no
unestablished review supports an infeasibility statement. No global optimality claim is made. Process interruption
does not publish late output or mutate baseline/input files. Full-result review retains the source search metadata
and recomputes validity/quality; it does not perform a new search.

Manual review of a repaired pair can supply `{ "manualReviewVersion": 1, "assignments": [...], "repairContext": {...} }` to `review --assignments`. These are the only three envelope fields. The reviewer validates the exact baseline context and recomputes weekly teachers, coordination matching, comparison, score and quality. This format asserts no old allocation or matching certificate. A full result remains an exact assertion and must preserve its certificates; a plain array remains supported without comparison context.
