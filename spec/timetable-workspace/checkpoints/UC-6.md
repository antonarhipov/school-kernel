# Use-Case Checkpoint: UC-6 - Decide whether a proposal becomes current

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `129727a` (revision for convergence finding C-1; original implementation based on `a48259f`)
- Submission commit: HEAD at convergence
- Relations verified: Requires approved UC-5; the production review and decision paths consume its complete verified repair proposal and exact accepted, intent, successor-definition, result, run, and timetable identities.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-7 | `ProposalReviewServiceTest.buildsExactOverlappingImpactReview` verifies the six authoritative categories, unique/direct/ripple counts, changed dimensions, and old/proposed group context. `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` removes the derived review from the persisted UC-5 proposal, reloads it in real Chrome, reviews it, and performs keyboard-confirmed acceptance. `WorkspaceRepairPlanningIT.acceptsExactRepairProposalAndStartsNextRepairWithoutPriorAttemptPins` verifies the complete derived snapshot and exact acceptance from the same legacy persistence shape, then starts a later repair from the new parent without the prior attempt pin. | PASS |
| Extension 1a | `ProposalReviewServiceTest.buildsExactOverlappingImpactReview` places `l1` in both teacher and period categories while the unique total remains two across three category entries. Direct-effect membership is reported independently. | PASS |
| Extension 2a | The same test asserts all six categories in normative order, including empty additions, cancellations, and forced moves; real Chrome renders `Additions 0` and `Cancellations 0`. | PASS |
| Extension 3a | `WorkspaceBrowserIT.measuresTargetScaleProposalReviewOpeningInRealBrowser` opens an accepted lesson outside the 100 changed lessons and observes `Accepted and unchanged · not included in change totals` plus accepted-assignment detail. | PASS |
| Extension 5a | `WorkspaceRepairPlanningIT.discardsOnlyProposalAndRetainsRepairDraft` issues the production DELETE command and compares the complete accepted baseline and repair draft while proving only the proposal was removed. | PASS |
| Extension 5b | The review's separate `Revise intent` action uses the same proposal-only invalidation command and returns to retained `REPAIR_DRAFT`; the production integration test verifies that transition and exact retained content. | PASS |
| Extension 6a | `WorkspaceRepairPlanningIT.invalidatesEveryStaleRepairProposalIdentity` independently corrupts source version, accepted revision, successor revision, intent revision, proposed revision, run, limit, termination, elapsed time, counts, result, and review; every request returns `STALE_PROPOSAL`, preserves the exact accepted baseline, removes the proposal, and returns to the draft. | PASS |
| Extension 6b | `WorkspaceRepairPlanningIT.rollsBackFailedAcceptanceAndKeepsProposalReviewable` injects a PostgreSQL trigger failure, receives safe `STORAGE_UNAVAILABLE`, and compares the exact document and version in `REPAIR_PROPOSAL`; retry then succeeds. | PASS |
| G1 | The review service forms a set of changed lesson IDs across the six unchanged kernel category arrays; unit assertions prove overlap does not inflate the headline and grouping totals remain separate arrays rather than an additive total. | PASS |
| G2 | Old and proposed review sides carry stable lesson, subject, class, teacher, period, and room IDs directly from complete accepted/proposed documents. Shared display lookup falls back to the stable ID with an unavailable-name cue; exact-ID and missing-name presentation remain covered by the accepted-view browser suite. | PASS |
| G3 | Production preserves the six kernel arrays and their membership unchanged, derives only a separate unique union and explanatory direct/ripple membership, and makes no definition-only enumeration claim. | PASS |
| G4 | Real Chrome renders native buttons, select, checkbox, and distinct text labels; keyboard Space confirms then accepts, context controls return to review, and narrow rendering is read-only without acceptance controls. | PASS |
| G5 | `WorkspaceBrowserIT.measuresTargetScaleProposalReviewOpeningInRealBrowser` loads approximately 1,000 lessons and measures proposal-review opening at 337.700 ms in the clean full run, below one second with solver time explicitly excluded. | PASS |
| G6 | Acceptance remains disabled until the explicit checkbox is selected; server acceptance revalidates the complete identity and verified revisions, while the conditional single-row transaction stores the full bundle, advances state, and clears transient state together. | PASS |
| Success postcondition | HTTP/PostgreSQL and real-Chrome journeys compare the accepted definition/result with the proposal, prove no proposal/draft remains, and compile the next repair with the new result input revision as parent and no prior attempt-scoped pin. | PASS |
| Minimal guarantee | Discard, revise, all stale-identity cases, and injected storage failure compare the prior accepted bundle exactly; none labels or stores the proposal as current. | PASS |
| Requires UC-5 | UC-5 is approved. UC-6 tests create the proposal through the production packaged-CLI UC-5 route, remove the UC-6-derived review to reproduce the exact pre-UC-6 persisted shape, and prove server-authored review, browser reload, and acceptance before exercising the remaining decision paths. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Clean reactor verification compiles and tests the unchanged kernel/workspace process boundary; UC-6 adds only workspace review and decision services. | PASS |
| RULE-2 | `./mvnw -q clean verify` runs on the pinned Java/Spring/PostgreSQL baseline and retains the existing dependency architecture. | PASS |
| RULE-3 | Review, proposal, draft, manifest, and accepted bundle remain in the one relationally guarded JSONB aggregate accessed through `JdbcClient`; no secondary store/model was added. | PASS |
| RULE-5 | Real PostgreSQL journeys exercise `REPAIR_PROPOSAL -> ACCEPTED_BASELINE` and `REPAIR_PROPOSAL -> REPAIR_DRAFT`; existing lifecycle regressions reject every unrelated transition. | PASS |
| RULE-6 | Accept and discard require current `If-Match`; production resolves the strong ETag to a version used by conditional SQL, and the stale identity matrix proves no accepted mutation on refusal. | PASS |
| RULE-7 | The accepted definition/result/manifest/state change and proposal/draft cleanup use one conditional update. Injected PostgreSQL failure proves document/version rollback and retryability. | PASS |
| RULE-8 | Accepted and proposed kernel documents are deep-copied losslessly; proposal-review identity uses RFC-8785 canonical bytes so PostgreSQL JSONB property ordering cannot create false mismatches. | PASS |
| RULE-17 | Acceptance checks source version, accepted/result/definition/intent/timetable/run/limit/termination/elapsed/category identities and independently verifies the candidate. A present review must match its canonical regeneration; an absent legacy derived review is regenerated from the authoritative proposal inputs. The corruption matrix covers every persisted guard. | PASS |
| RULE-18 | One server-authored review preserves exactly additions, cancellations, teacher changes, forced moves, period moves, and room-only moves; the unique union, old/proposed details, overlap, empty categories, and group contexts are unit/browser tested. | PASS |
| RULE-19 | The server completes a missing legacy derived review from authoritative stored inputs before returning the snapshot; the browser renders review/detail/context without reconstructing scheduling truth. Chrome journeys cover the persisted legacy proposal plus accepted and proposed contexts. | PASS |
| RULE-20 | Visible strings are centralized in the English message catalog; native semantic controls, text labels beyond color, keyboard confirmation/acceptance, and read-only narrow presentation are verified. | PASS |
| RULE-21 | New mutations remain under the existing loopback Host/Origin/session-CSRF protections; integration commands use the same protected HTTP boundary and the full security matrix remains green. | PASS |
| RULE-22 | Stale proposal and injected storage failures return stable safe problem codes/messages without candidate data, paths, SQL text, or exception details. | PASS |
| RULE-23 | Acceptance retains safe last-run execution evidence and does not introduce school-data logging; the clean captured-log regression remains green. | PASS |
| RULE-24 | Disposable PostgreSQL 18.6, ephemeral HTTP, packaged School Kernel, and real Chrome execute the actor success path and consequential negative paths in the standard Maven lifecycle with no H2 fallback. | PASS |
| RULE-25 | The repeatable target-scale Chrome fixture records a 333.800 ms review opening for 1,000 lessons; solver time is excluded and prior search/filter/day/selection/pin measurements remain separate and green. | PASS |

## Validation

- Focused suites: proposal review unit test, expanded repair HTTP/PostgreSQL integration suite, real-Chrome acceptance journey, and target-scale proposal review all pass.
- Full relevant suite: `./mvnw -q clean verify` - 165 tests, 0 failures, 0 errors, 0 skipped; proposal-review opening 337.700 ms with solver time excluded.
- Working tree impact from tests: none; `git diff --check` passes.
- Runtime evidence: real Chrome over ephemeral HTTP/PostgreSQL creates a repair through packaged School Kernel, renders all six categories with before/after impact, accepts using keyboard-native explicit confirmation, and opens the exact new baseline.
- Changed files in this revision: `WorkspaceController`, `RepairProposalService`, `WorkspaceRepairPlanningIT`, `WorkspaceBrowserIT`, status, checkpoint, and Jev advisory bundle.
- Approved UCs regression-tested: timetable-workspace UC-1 through UC-5 and kernel UC-1/UC-2 all pass in the 165-test clean reactor.
- Jev preflight: revised `UC-6.jev-bundle.json` validates locally with 33 atomic items using the already-built pinned `jev-1.13.0` helper. External review requires disclosure of narrow repository excerpts to TypeSafe and is `REVIEW` because this turn did not authorize that disclosure; no report was produced and no semantic approval is claimed.

## Notes

- Clean verification emitted expected Hikari warnings from stopped Testcontainers contexts used by negative tests; the reactor exited 0 and all 165 reports record zero failures/errors/skips.
- PostgreSQL JSONB may reorder object properties, so acceptance compares the regenerated review using canonical JSON bytes instead of order-sensitive tree equality.
- The required administrator walkthrough and independent evidence audit remain convergence responsibilities; this checkpoint claims automated technical readiness only.

READY FOR CONVERGENCE: UC-6
