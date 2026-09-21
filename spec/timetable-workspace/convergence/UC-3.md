# Convergence: UC-3 - Understand the accepted whole-school timetable

## Summary

- Submission: `spec/timetable-workspace/checkpoints/UC-3.md` at `1a796d273e0105dc74a9eda3411b7a04ef4dad3c`
- Walkthrough enablement: Compose-managed local database support at `2523205`
- Verdict: APPROVE
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: final clean run 141 tests, 0 failures, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

- Exactly UC-3 was `READY_FOR_CONVERGENCE`; no other UC was in progress or ready.
- The checkpoint, implementation, tests, status, and locally validated 28-item Jev bundle are committed together at
  `1a796d2`; the external Jev review was appropriately recorded as `REVIEW` because disclosure was not authorized.
- Required UC-1 is `APPROVED`; the production complete-snapshot test consumes its accepted definition/result state.
- The checkpoint has distinct rows for all five main steps, four extensions, six guarantees, both postconditions,
  `Requires UC-1`, and RULE-1, RULE-2, RULE-4, and RULE-19 through RULE-25.
- Inspection of all ten changed files found only UC-3 implementation, tests, localization enabling work needed by
  RULE-20, status, and checkpoint evidence. No UC-4 repair behavior or unrelated user work is present.
- Pre-verification Git status was clean at submitted commit `1a796d2`.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator in real Chrome | Main 1-2 | Accepted complete day matrix with named occupied and empty cells | Accepted label, Mathematics, Alex, Room 102, Empty, and `Showing 1 of 1 classes` rendered through ephemeral HTTP/PostgreSQL |
| Administrator in real Chrome | Main 3-4, ext 3a/3b | Local filtering, active criteria, match/reset, assignment details | Real text input produced filtered label and match; no-match showed exact criterion/reset; trusted Space opened exact accepted assignment and technical ID |
| Administrator in real Chrome | Main 5 | Keyboard entry to focused schedule and retained return | Trusted Space opened `Teacher schedule · Alex`; Monday and return action rendered, then matrix context returned |
| Administrator in real Chrome | Extension 2a | Declared empty structure without invented lessons | Empty accepted school rendered Year 7A, Monday 1, and Empty; exact PostgreSQL document was unchanged |
| Administrator at 390x844 | Extension 5a | Explicit read-only focused schedule without desktop workflow | Read-only class schedule rendered; acceptance action and desktop matrix were absent; browser console remained empty |
| Administrator at target scale | G5/RULE-25 | All post-load p95 values below 250 ms | Focused run: search 69.7, filter 43.8, day 216.2, selection 169.8 ms. Full run: search 48.8, filter 38.3, day 176.1, selection 131.6 ms. Solver excluded |
| HTTP client | RULE-19/G6 | Complete snapshot and no mutation | `GET /api/workspace` returned exact accepted JSON and ETag; every UC-3 journey preserved exact version/document |
| Administrator starting the packaged app | RULE-30 | Database starts and stops with the application | With no database variables set, the executable JAR started Compose PostgreSQL 18.6 on loopback, waited for healthy, applied Flyway, returned `EMPTY`/ETag `ws-0` and the workspace page over HTTP, then stopped the container while retaining the named volume |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Weekday navigation is metadata-backed | Real browser accepted Monday; `app.js:106-116,182,275` uses weekday metadata and ordered periods | STRONG | yes |
| Main step 2 | Complete class-by-period matrix | Browser asserts occupied/empty/names/count; `app.js:129-142` crosses every class with every selected-day period | STRONG | yes |
| Main step 3 | Inspection controls retain whole-school/accepted context | Browser text input, selects/buttons, density and selection execute in the accepted view; no mutation request exists in handlers | STRONG | yes |
| Main step 4 | Match/criteria/reset/details stay in matrix | Browser observes filtered/match/no-match/reset and exact accepted detail; DOM retains matrix | STRONG | yes |
| Main step 5 | Focused schedules and return context | Trusted keyboard opens teacher schedule; return action restores matrix; production code provides all three focus types | STRONG | yes |
| Extension 2a | Empty accepted school retains declared structure | Real Chrome asserts class, period, empty message/cells and exact unchanged storage | STRONG | yes |
| Extension 3a | Narrowed matrix is labeled filtered | Browser and aria label both change to `Filtered whole-school matrix` under active criterion | STRONG | yes |
| Extension 3b | No match shows criteria/reset without mutation | Browser asserts no-match text, exact criterion/reset; final exact storage comparison passes | STRONG | yes |
| Extension 5a | Narrow screen is read-only and withholds desktop workflow | Real 390x844 Chrome asserts read-only notice and class schedule, with no acceptance action or matrix | STRONG | yes |
| G1 | Metadata is authoritative; IDs stay opaque/technical | Map joins use exact IDs; display uses metadata; browser separately exposes opaque lesson ID in details | STRONG | yes |
| G2 | Class maps explicitly to kernel cohort | Central catalog exactness test and rendered technical mapping prove both terms | STRONG | yes |
| G3 | Shown states are not color-only | Accepted, Match, Selected, and Accepted assignment have visible text/icons; no later-UC states are shown | STRONG | yes |
| G4 | Complete inspection is keyboard-operable | Native inputs/selects/buttons cover all controls; real text and trusted Space prove consequential selection/focus paths | STRONG | yes |
| G5 | Validation-scale p95 is below 250 ms | Independent focused and full Chrome runs validate exact cardinalities, 20 samples, nearest-rank p95, and solver exclusion | STRONG | yes |
| G6 | Inspection never mutates workspace state | Main, empty, snapshot, and scale journeys compare exact PostgreSQL version/document before and after | STRONG | yes |
| Success postcondition | Accepted assignment and school context are identifiable | Browser identifies exact lesson/class/teacher/room within complete and focused accepted context | STRONG | yes |
| Minimal guarantee | Empty/no-match/presentation invent nothing and mutate nothing | Definition-backed empty structure plus exact storage equality across all negative/presentation paths | STRONG | yes |
| Requires UC-1 | Approved accepted state is consumed | UC-1 is approved; production snapshot returns the exact accepted definition/result pair and accepted state | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Separate reactor/process boundary; no workspace kernel implementation dependency | Clean reactor and unchanged ArchUnit boundary test pass; no dependency changes | PASS |
| RULE-2 | Pinned Java 25/Spring/PostgreSQL stack without excluded alternatives | Clean Java 25 reactor passes on PostgreSQL 18.6/Testcontainers; UC-3 adds dependency-free ES/CSS only | PASS |
| RULE-4 | Flyway is the only schema authority | No schema change; fresh migration and Flyway failure tests pass in the clean suite | PASS |
| RULE-19 | Complete snapshot; native local presentation; opaque IDs | Exact HTTP snapshot test plus in-browser search/filter/day/selection/rendering and map joins; no filter API | PASS |
| RULE-20 | Central English, Class/cohort mapping, non-color semantics, keyboard, narrow read-only | Catalog architecture test and independent Chrome keyboard/narrow journeys pass | PASS |
| RULE-21 | Same-origin loopback security surface remains narrow | Shell/catalog CSP route tests plus complete approved security regression suite pass | PASS |
| RULE-22 | Stable safe API failures | UC-3 adds no failure mapping; full HTTP problem/security/database/kernel regression suite passes | PASS |
| RULE-23 | Safe bounded observability | UC-3 emits no new logs/persistence; captured safe-log and run-evidence regressions pass | PASS |
| RULE-24 | Disposable PostgreSQL, real HTTP/browser, exact negative assertions | Both verifier runs use PostgreSQL 18.6 Testcontainers and real Chrome; no fallback/skips; state comparisons exact | PASS |
| RULE-25 | Complete scale fixture and post-load p95 separate from solver | Exact 1000/100/60/100/60 fixture, raw 20-sample series, p95 assertion, and solver exclusion reproduced twice | PASS |
| RULE-30 | Packaged local startup owns a loopback, health-checked, persistent walkthrough database | `compose.yaml`, packaged `spring-boot-docker-compose`, disabled test profile, architecture assertions, and the live packaged-JAR start/HTTP/stop/volume journey all pass | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Timetable workspace UC-1 | Required accepted baseline, shared shell/security/snapshot | Full clean reactor passes import, verification, PostgreSQL, HTTP, browser, and security suites | PASS |
| Timetable workspace UC-2 | Shared localized shell, accepted view, browser driver | Focused `WorkspaceBrowserIT` reruns both UC-2 browser journeys; full planning/acceptance suite passes | PASS |
| Kernel UC-1/UC-2 | Shared reactor/public contract and packaged executable | Complete kernel unit, architecture, packaged plan/replan/verify suites pass in 140-test clean reactor | PASS |

## Findings

None. Automated evidence is strong and the required human UI walkthrough is confirmed.

The first two clean runs after adding Compose support exposed the existing day-change timing path at 368.5 ms and
281.3 ms p95 under loaded-suite conditions. The accepted model now indexes assignments by class/period once and avoids
two full 1,000-assignment rescans on unfiltered rerender. The focused rerun passed at 186.5 ms and the final clean-suite
run passed at 199.4 ms; search, filter, and selection also remained below 250 ms. This was treated as a measured product
correction, not hidden as a retry.

## Walkthrough

User result: PASS, confirmed by the user on 2026-09-21.

1. Open an accepted workspace on a desktop-width screen. Select a weekday and confirm every declared class remains a
   row, definition-ordered periods are columns, empty cells are visible, and occupied cells lead with subject and show
   teacher and room. Confirm the accepted-current label and whole-school context remain visible.
2. Use only the keyboard to search, apply a class/teacher/room filter, focus a period, switch day, change density, and
   select a lesson. Confirm the matrix says `Filtered whole-school matrix`, every active criterion is summarized,
   matches are text-labeled, Reset is one action, and accepted assignment details open without leaving the matrix.
3. Search for a value with no match. Confirm the active criterion and reset action remain visible, reset restores the
   complete matrix, and no invented assignment appears.
4. Open each class, teacher, and room schedule, confirm definition-backed names/order, then return to the retained
   whole-school matrix context.
5. At a narrow width (390 px is sufficient), confirm the view explicitly says `Read-only focused schedule`, offers a
   class/teacher/room schedule, and exposes no desktop matrix, editing, or acceptance action.
6. If an accepted empty-school fixture is available, confirm declared classes, weekdays, periods, and empty cells are
   still visible with `No accepted lessons are scheduled` and no invented assignment.

## Status Update

`PENDING_WALKTHROUGH` -> `APPROVED`; next eligible UCs: UC-4 and UC-8.

## Response to execute

APPROVED
