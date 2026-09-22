# Convergence: UC-1 - Inspect the accepted school in Week or Day

## Summary

- Submission: `spec/timetable-inspection-ux/checkpoints/UC-1.md` at `36fe7d1`
- Verdict: APPROVE
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: 26 unit tests and 12 Chrome/Testcontainers PostgreSQL tests; 0 failures, 0 errors, 0 skips
- Working tree impact from verification: convergence report and status update only

## Protocol Gate

1. UC-1 was the only target and was `READY_FOR_CONVERGENCE`: pass.
2. Its checkpoint and implementation are committed together at `36fe7d1`: pass.
3. UC-1 has no `Requires`, `Includes`, or `Extends` dependency: pass.
4. UC-2 and UC-3 remain `NOT_STARTED`; no other feature UC is active: pass.
5. The checkpoint covers every UC-1 scenario, extension, guarantee, postcondition, applicable rule, command, and changed file: pass.
6. `git show 1d9f761..36fe7d1` contains only the C-1 through C-3 browser modules, boundary tests, and feature evidence: pass.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Open Week, change to Day, inspect accepted lesson | Packaged Chrome journey | `WorkspaceBrowserIT:230-307` starts real Chrome against Testcontainers PostgreSQL and asserts accepted labels, native Week/Day matrix structure, stable inspector content, narrow surface, and unchanged document. |
| Administrator | Invalid, isolated, and blocked local preference | New browser journey | `WorkspaceBrowserIT:335-385` writes malformed, stale, bad-enum, unknown-weekday, oversized, and other-school storage through Chrome, observes Week fallback, valid Monday restore, manual Day exclusion, blocked write notice, and unchanged document. |
| Administrator | Off-viewport lesson | Scale browser journey | `WorkspaceBrowserIT:598-650` scrolls the 60-class Week matrix, selects `lesson-999`, verifies its inspector detail, then verifies the selected Thursday follows into Day. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-1 main steps 1-3 | Accepted desktop opens Week and has complete Week/Day structures | Real Chrome at `WorkspaceBrowserIT:230-264`; complete/accepted labels, Week table, Day Monday, compact density, and authoritative tile text are asserted | STRONG | yes |
| UC-1 main steps 4-5 | Range changes derive same accepted population | Real Chrome uses native range buttons and state transition at `app.js:221-237`; Week-to-Day with a selected lesson is asserted at `WorkspaceBrowserIT:623-629` | STRONG | yes |
| UC-1 main steps 6-7 | Stable lesson selection opens exact accepted details | Browser inspector assertions at `WorkspaceBrowserIT:280-286`; selection delegates by `lessonId` at `app.js:532-548` | STRONG | yes |
| UC-1 extension 2a | Empty accepted baseline retains declared structure | `WorkspaceBrowserIT:310-332` asserts class, period, explicit empty cell, and no browser errors | STRONG | yes |
| UC-1 extension 2b | Bad preference falls back without blocked inspection | `WorkspaceBrowserIT:353-368` drives malformed, stale, bad-range, unknown-weekday, oversized, and other-school storage and observes Week fallback | STRONG | yes |
| UC-1 extension 2c | Narrow surface is read-only agenda | `WorkspaceBrowserIT:297-305` asserts narrow schedule, notice, withheld acceptance action, and no matrix | STRONG | yes |
| UC-1 extensions 4a/4b | Extension controls preserve the UC-1 postcondition before their dependent UCs run | Browser opens and returns from focused context at `WorkspaceBrowserIT:288-295`; UC-2/UC-3 behavior is not claimed or approved by this UC | STRONG | yes |
| UC-1 extension 5a | Excluding manual Day clears selection and announces reason | `WorkspaceBrowserIT:375-379` asserts announcement and absent inspector; state owner implements it at `inspection-state.js:30-35`, `app.js:229-232` | STRONG | yes |
| UC-1 extension 5b | Failed storage preserves rendered result without workspace mutation | `WorkspaceBrowserIT:381-385` blocks `setItem`, asserts notice and exact before/after durable document | STRONG | yes |
| UC-1 extension 7a | Off-viewport selection opens its accepted details | `WorkspaceBrowserIT:623-629` scrolls then selects `lesson-999`, asserts exact identity and retained represented view | STRONG | yes |
| UC-1 G1, success, minimal guarantee | Inspection remains presentation-only with no invented/persisted timetable change | Every browser journey takes an exact database document snapshot before/after; `WorkspaceBrowserIT:307`, `332`, `385`, and scale test completion are green | STRONG | yes |
| UC-1 G2-G11 | Accepted identity/range/population, empty slots, authoritative labels, controls, sticky/compact treatment, and no group UI remain visible | Browser assertions and unchanged original UC-1 coverage passed in the same real suite; extracted renderers continue escaping model-derived data | STRONG | yes |
| UC-1 G12 | Desktop Week/Day and narrow proof use validation-scale accepted snapshot | Full packaged `WorkspaceBrowserIT` run passed 12/12 against Docker PostgreSQL and Chrome | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Separate native state owner and Day/Week/focused renderers | `inspection-state.js:4-38`; `day-renderer.js:1-9`; `week-renderer.js:1-11`; `focused-renderer.js:1-13`; renderers take shared inputs and make no state writes | PASS |
| RULE-2 | One accepted read/model and no alternate display source | Existing single load remains at `app.js:33-39`; accepted model is built once at `app.js:88-93`; this revision adds no route | PASS |
| RULE-3 | Complete native compact Week table | `week-renderer.js:1-11`, plus scale Chrome matrix selection at `WorkspaceBrowserIT:622-629` | PASS |
| RULE-4 | One transition authority; renderer intent only | Accepted-baseline range/day/selection delegate to `inspection-state.js` via `app.js:208-237`, `532-548`; extracted renderers have no state import | PASS |
| RULE-5 | Bounded, versioned, namespaced, exact local record with robust fallback | `inspection-state.js:1-18,41-55`; real Chrome storage matrix at `WorkspaceBrowserIT:353-385` checks invalid/failure, cross-school isolation, valid restore, allowed key fields, and no aggregate mutation | PASS |
| RULE-6 | Stable lesson identity | `app.js:223,532-548`; off-viewport identity assertion at `WorkspaceBrowserIT:623-629` | PASS |
| RULE-9, RULE-10 | Safe catalog presentation and native pointer controls | Renderers use existing escaping boundary; browser triggers native buttons/select/lesson button and passes | PASS |
| RULE-11, RULE-12 | No backend/security/lifecycle delta | Commit diff contains no Java production route/migration/security files; browser durable-document comparisons pass | PASS |
| RULE-13 | Real packaged-browser coverage of all UC-1 cases | Command `mvn -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify`; 12 Chrome/Testcontainers tests passed at the real actor boundary | PASS |
| RULE-16 | Human approval gate needs five professionals from three schools | The user confirmed the pending administrator walkthrough with `PASS` on 2026-09-23. | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Earlier accepted workspace, repair, proposal, and scale behavior | Shared workspace assets and model | The full 12-test `WorkspaceBrowserIT` suite passed after the extraction | PASS |

## Walkthrough

Perform the UC-1 tasks from `spec.md` with five timetable professionals from at least three schools: identify the `Current · accepted` Week context; change to Day and navigate it; inspect an occupied lesson; confirm the empty/narrow presentation as applicable; change to a Day that excludes a selected lesson; and select an off-viewport lesson. Record each participant's six task results, serious errors/corrections, school distribution, and whether all identified `Current · accepted` throughout. Approval needs at least four of five to complete every task without serious error or facilitator correction, and all five to identify the accepted baseline.

User result: PASS, confirmed by the user on 2026-09-23.

## Status Update

`PENDING_WALKTHROUGH` -> `APPROVED`; UC-2 and UC-3 are now eligible.

## Response to execute

APPROVED
