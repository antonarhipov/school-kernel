# Convergence: UC-4 - Prepare a protected repair draft

## Summary

- Submission: `spec/timetable-workspace/checkpoints/UC-4.md` and implementation at `5f545ee716a66ad6080acb5a382e5caa51aff7d7`
- Verdict: PENDING WALKTHROUGH
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic
- Builder suite: 152 tests, 0 failures, 0 errors, 0 skipped
- Verifier reproduction: 9 focused PostgreSQL/HTTP tests pass; the focused real-Chrome UC-4 journey passes
- Working tree impact from verification: none before this report and status update

## Protocol Gate

- Exactly UC-4 was `READY_FOR_CONVERGENCE`; UC-1 is approved and no later use case has started.
- The checkpoint, implementation, tests, status, and locally valid 35-item Jev bundle are committed together at
  `5f545ee`; external Jev review is correctly recorded as `REVIEW` because repository disclosure was not authorized.
- The checkpoint separately covers main steps 1-7, every extension, G1-G6, both postconditions, `Requires UC-1`, and
  every applicable rule: RULE-1 through RULE-6, RULE-8, RULE-15, RULE-16, and RULE-19 through RULE-25.
- Diff inspection from base `f3b9253b67f9be528cd6b6ff90343ba9b4992f28` found only UC-4 implementation,
  verification, checkpoint, status, and advisory evidence. UC-5 and UC-8 behavior has not begun.
- Pre-verification Git status was clean at the submitted commit.

## Runtime Reproduction

| Actor | Step or extension | Converge observed |
|---|---|---|
| Administrator over real HTTP/PostgreSQL | Main 1-2; extension 2a | Teacher and room weekly unavailability is stored separately, omitted availability is materialized, and direct effects are exact, including the empty-effect case |
| Administrator over real HTTP/PostgreSQL | Main 3-4; extensions 4a/7a | Accepted assignments, policy locks, and attempt pins remain distinct; contradictory pins identify the lesson, block readiness, and clear only when the conflicting attempt pin is removed |
| Administrator over real HTTP/PostgreSQL | Main 5-6; extensions 5a/6a | Preview does not mutate; confirmation recomputes the complete immutable snapshot; undo removes exactly its source while retaining an independent pin |
| Administrator over real HTTP/PostgreSQL | Main 7; extensions 6b/7b/7c | Durable summary survives reload; storage failure preserves the last durable draft and prevents solve; confirmed discard restores the accepted baseline exactly |
| Administrator in real Chrome | Complete UC-4 actor journey | Desktop keyboard pinning, named conflict, preview/confirm/undo, narrow read-only presentation, and explicit discard pass with no browser errors |
| Administrator at target scale | G6/RULE-25 | Submitted clean run p95 was 166.3 ms; verifier full-run samples yielded 152.4 ms p95 for persisted pin feedback, below 250 ms with solver time excluded |

## Evidence Ledger

| Contract element | Evidence obtained | Strength | Verified |
|---|---|---|---|
| Main step 1 | Focused PostgreSQL/HTTP tests start repair and stage named teacher and room weekly periods; Chrome performs the teacher path | STRONG | yes |
| Main step 2 | Exact overlay and materialized-availability assertions prove accepted data is unchanged and directly affected lessons are highlighted; empty direct effects are retained explicitly | STRONG | yes |
| Main step 3 | Chrome and HTTP evidence expose accepted assignment, staged intent, policy-lock, and pin state in whole-school/focused repair context | STRONG | yes |
| Main step 4 | Individual period, room, and both-dimension commands plus DAY, CLASS, and UNAFFECTED bulk scopes are exercised with opaque lesson IDs | STRONG | yes |
| Main step 5 | Preview records exact ordered lesson IDs, dimensions, count, scope, direct-effect revision, source version, and conflicts without mutation | STRONG | yes |
| Main step 6 | Confirmation canonically recomputes the server snapshot, records provenance, shows conflicts, and durably auto-saves | STRONG | yes |
| Main step 7 | Reloaded HTTP snapshot and compiled successor contain the exact weekly changes, effects, pins, conflicts, and persisted readiness state | STRONG | yes |
| Extension 1a | Unsupported teacher reassignment returns safe `INVALID_REPAIR_DRAFT`, stages nothing, and preserves the exact accepted bundle | STRONG | yes |
| Extension 2a | A named unavailable period with no accepted lesson retains the change and reports an empty direct-effect set | STRONG | yes |
| Extension 4a | Attempt and persistent-policy contradictions produce a named blocking conflict without silently selecting a winner | STRONG | yes |
| Extension 5a | Preview is non-mutating and Chrome can cancel it without issuing a repair mutation | STRONG | yes |
| Extension 6a | Source-scoped undo removes exactly the confirmed bulk snapshot while an independently applied room pin remains | STRONG | yes |
| Extension 6b | Injected PostgreSQL update failure returns safe 503, retains the last exact durable draft/baseline, and refuses solve | STRONG | yes |
| Extension 7a | Any named conflict sets `readyToSolve=false`; removing the conflicting attempt source clears the case | STRONG | yes |
| Extension 7b | Unconfirmed discard is refused; confirmed discard removes attempt intent/pins and returns to the exact accepted baseline | STRONG | yes |
| Extension 7c | A fresh repository load restores `REPAIR_DRAFT`, its intent, effects, and attempt pins without acceptance mutation | STRONG | yes |
| G1 | Compilation deep-copies the accepted definition, overlays intent into a complete successor with correct lineage, and copies accepted values into pins | STRONG | yes |
| G2 | UNAFFECTED is the preview-time complement; DAY/CLASS/filter-independent selection, later edits, altered previews, duplicate IDs, staleness, and empty snapshots are covered | STRONG | yes |
| G3 | Deterministic provenance distinguishes policy, individual, and bulk sources; exact undo/discard boundaries preserve only applicable sources | STRONG | yes |
| G4 | Visible text distinguishes direct, pinned, policy, conflict, and unpinned states; trusted keyboard actions cover selection and pinning; narrow editing controls are absent | STRONG | yes |
| G5 | Every success and negative journey compares the exact accepted definition/result/manifest bundle before and after | STRONG | yes |
| G6 | Real Chrome uses the 1,000-lesson/100-teacher/60-class/100-room/60-period fixture and nearest-rank p95 below 250 ms | STRONG | yes |
| Success postcondition | One durable, ready repair draft contains the exact intent, effects, deterministic non-conflicting pins, revision, and compiled successor inputs | STRONG | yes |
| Minimal guarantee | Conflict, version, malformed, unsupported, storage, discard, and submission refusals preserve the exact baseline and prevent invalid/unpersisted solve | STRONG | yes |
| Requires UC-1 | Every journey consumes UC-1's approved exact accepted definition/result/manifest through the singleton aggregate | STRONG | yes |

## Rule Conformance

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Reactor/ArchUnit checks preserve the process boundary; UC-4 adds no kernel implementation dependency | PASS |
| RULE-2 | Java 25, Spring Boot 4.1.1, PostgreSQL 18.6, JdbcClient, Flyway, and Testcontainers remain pinned; no frontend framework is added | PASS |
| RULE-3 | Intent, effects, conflicts, previews, provenance, and pins live in the existing singleton JSONB aggregate | PASS |
| RULE-4 | No schema change; fresh/checksum/failure Flyway coverage remains green | PASS |
| RULE-5 | Start, edit, reload, discard, and refused-run paths cover the permitted accepted-baseline/repair-draft transitions | PASS |
| RULE-6 | Strong ETag/If-Match plus conditional SQL covers missing, stale, and racing mutations with one durable winner | PASS |
| RULE-8 | Lossless deep copy, opaque IDs, deterministic order, canonical intent hashing, and omitted/materialized availability preserve fidelity | PASS |
| RULE-15 | Overlay compilation creates a complete successor definition with accepted lineage, copied pins, and unchanged accepted data | PASS |
| RULE-16 | Complete immutable previews are server-derived and recomputed; conflicts and exact provenance-scoped undo are covered | PASS |
| RULE-19 | The complete snapshot and native modules provide local repair presentation and ETag-bound intent commands | PASS |
| RULE-20 | Central English catalog, non-color state labels, native keyboard controls, and narrow read-only presentation pass | PASS |
| RULE-21 | Same-origin loopback Host/Origin/CSRF/CORS/route regression plus exact repair allowlist pass | PASS |
| RULE-22 | Validation, transition, stale, malformed, conflict, and storage failures retain stable safe problem shapes | PASS |
| RULE-23 | Draft/pin data is not logged and solver evidence is not invented; prohibited-value logging regression passes | PASS |
| RULE-24 | Focused convergence uses disposable PostgreSQL 18.6, ephemeral HTTP, real Chrome, and exact state assertions | PASS |
| RULE-25 | Complete target-scale fixture, 20 post-load samples, nearest-rank p95, and solver-time exclusion are explicit | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Timetable workspace UC-1 | Required accepted bundle, singleton aggregate, security, concurrency | Submitted 152-test clean reactor plus focused convergence journeys preserve the exact accepted bundle | PASS |
| Timetable workspace UC-2 | Shared shell, lifecycle, mutation and browser driver | Submitted clean reactor and unchanged focused browser infrastructure pass | PASS |
| Timetable workspace UC-3 | Shared renderer and target-scale browser path | Submitted clean reactor passes; verifier UC-3 day p95 was transiently 255.8 ms, then 83.6 ms isolated and 221.6 ms in the next full run | PASS |
| Kernel UC-1/UC-2 | Shared reactor/public JSON contract and successor lineage | Submitted complete clean reactor and verifier unit phase pass without kernel changes | PASS |

## Verification Variance

The builder's authoritative unchanged `./mvnw -q clean verify` passed 152 tests with no failures, errors, or skips.
During independent convergence, the first full run exceeded the already approved UC-3 day-interaction target once at
255.8 ms; the unchanged isolated UC-3 scale rerun passed at 83.6 ms. The next full run measured UC-3 day p95 at
221.6 ms and UC-4 pin-feedback p95 at 152.4 ms, but one Chrome DevTools evaluation in the UC-4 journey timed out after
39.5 seconds amid stopped-container connection warnings. The exact unchanged UC-4 browser journey then passed in
isolation against fresh PostgreSQL 18.6. Because neither symptom reproduced in isolation, no product finding is
recorded; both are classified as loaded-host/browser variance. The failed full rerun is retained here rather than
reported as green.

## Findings

None. Automated contract evidence is sufficient for technical convergence, but approval remains gated on the required
administrator UI walkthrough.

## Walkthrough

1. Open an accepted baseline on a desktop-width screen and confirm it is clearly labeled current.
2. Start repair, select a teacher and a named weekly unavailable period, and confirm directly affected lessons are
   highlighted while the accepted baseline remains current.
3. Select a lesson with the keyboard and pin its period, room, and then both accepted dimensions.
4. Create a contradictory pin and confirm the named blocking conflict appears and solve remains disabled; remove only
   that conflicting pin and confirm the conflict clears.
5. Preview a bulk pin and inspect the exact lesson count, selected dimensions, lesson list, and conflicts. Cancel once
   and confirm no pin was applied.
6. Preview again, confirm, then undo. Confirm only that immutable bulk snapshot is removed and independent pins remain.
7. Resize to a narrow viewport and confirm repair editing controls disappear while the focused read-only schedule stays
   readable.
8. Return to desktop, explicitly confirm discard, discard the draft, and confirm the accepted baseline is unchanged.

## Status Update

`READY_FOR_CONVERGENCE` -> `PENDING_WALKTHROUGH`; no later use case is eligible until the walkthrough is confirmed.

## Response to execute

PENDING WALKTHROUGH: confirm the UC-4 administrator walkthrough before approval or any later use case begins.
