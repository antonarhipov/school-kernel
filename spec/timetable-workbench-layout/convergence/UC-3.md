# Convergence: UC-3 - Follow repair generation while retaining timetable context

## Summary

- Submission: `checkpoints/UC-3.md` at `4bb218ae7a4b50db6e5f93553c1b2c81da1e9689` (base `06e89134600b685304354bf7aa982a4a146819e0`)
- Verdict: APPROVE
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: independent focused run passed 4 browser and 10 repair-planning integration cases; independent full reactor passed 195 tests (kernel CLI 58 unit + 27 integration; workspace 26 unit + 84 integration, including 36 browser), with 0 failures, errors, or skips
- Working tree impact from verification: none tracked; `git status --short` was clean before and after the successful runs. Screenshots remain ignored under `timetable-workspace/target/workbench-layout/`.

## Protocol Gate

1. UC-3 alone was `READY_FOR_CONVERGENCE`; UC-1 and UC-2 were `APPROVED`, and UC-4 and UC-5 were `NOT_STARTED`. No other UC was active.
2. The implementation, tests, ledger, and complete checkpoint were committed together at `4bb218a`; the declared base resolves to `06e8913`. The initial and final audit worktrees were clean.
3. Required UC-2 is approved in `convergence/UC-2.md`; its saved ready Draft and the accepted UC-1 canvas are consumed through the production browser route.
4. The checkpoint has rows for all six main steps, seven extensions, seven guarantees, both postconditions, Requires UC-2, all nine applicable rules, test commands, changed files, and approved-UC regression.
5. All six changed files were inspected against the base. The production diff is confined to `app.js`, `messages.js`, and `styles.css`; it adds no server route, schema, dependency, durable field, or UC-4 decision behavior. The browser-test changes and this feature's status/checkpoint are attributable to UC-3.
6. `git diff --check 06e8913..4bb218a` and independent `node --check` of both changed modules passed. The full architecture tests passed without a new exemption. The first focused attempt was blocked before integration tests by sandbox-denied Docker socket access; the same command reran with Docker access and passed. That environmental attempt is not counted as a product failure or a skipped test.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator in packaged Chrome with isolated PostgreSQL | Saved UC-2 Draft into Solving | Ready teacher intent, room pin, accepted Current, frozen PT30S run | Fresh focused and full CDP runs passed `WorkspaceBrowserIT.java:2090–2152`: independently verified 1,000-lesson accepted school, exact saved teacher-16/period-0 and lesson-500 room pin, one run ID, PT30S, identical accepted/Draft JSON and no Proposal. |
| Administrator | Inspect while run continues | Context and accepted canvas retained; status/cancel remain visible | Browser `:2153–2219,1925–1942` passed range, mode, inspector, detail, focus/return and breakpoint actions. It compared exact running JSON, zero mutating presentation requests and one scheduler invocation. Fresh 1600, 1280 and 390 px screenshots were inspected; status/cancel are below the visible canvas on desktop and absent in the read-only narrow agenda. |
| Administrator | Cancel active run | Editable Draft, cancelled terminal outcome, no candidate | Browser `:2219–2225` and service `WorkspaceRepairPlanningIT.java:454–535` passed exact accepted/Draft preservation, `CANCELLED`, no Proposal, and late-result suppression. |
| Administrator after application stop | Recover an interrupted run | One safe terminal outcome; no late Proposal | `WorkspaceBrowserIT.java:2033–2085` passed after closing the actual application and restarting against the same durable database: prior run ID became interrupted, version advanced once, accepted/Draft remained exact, Draft reopened, and no old output published. |
| Administrator | Unsuccessful, rejected, timed-out and retry branches | Safe diagnostics, unchanged-intent PT2M only, no candidate | Browser `:1976–2030,2224–2228` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:376–535` passed no-feasible, invalid, transport, interruption, mismatch, watchdog, stale/late and retry-gating paths with exact state and candidate absence. |
| Administrator | Complete feasible result | Verified Proposal opens, old Current remains accepted | Browser `:1731–1794` passed a genuine packaged feasible replan. The normative browser `:2229–2272` passed a complete feasible fixture through the production run boundary and the packaged independent verifier, then asserted one Proposal, no active Solving, old accepted/Draft JSON, retained lesson context, and the open wide review task. |

The normative fixture in `WorkspaceBrowserIT.java:3122–3290` contains 60 classes, 100 teachers, 100 rooms, 60 ordered periods across five weekdays, and exactly 1,000 distinct accepted assignments. The accepted pair and the complete normative successor are checked by the packaged verifier before storage or proposed display. The browser tests assert exact represented IDs and versioned durable documents rather than only counts or HTTP status. The complete shared browser regression includes initial journeys, accepted inspection, Draft protection, repair, and Proposal behavior.

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1 | Request from saved conflict-free Draft | Browser `WorkspaceBrowserIT.java:2090–2143` checks ready saved teacher intent and exact pin, then clicks production solve; focused/full PASS | STRONG | yes |
| Main 2 | Enter frozen Solving with compact status task | Browser `:2144–2152,2163–2200` compares accepted/Draft/run/limit, frozen named intent and pin, visible cancel, absent edit/decision controls, and measured task geometry; PASS | STRONG | yes |
| Main 3 | Inspect, change range/focus or mode during live run | Browser `:2153–2219,1925–1942` exercises selection, Week/Day, inspector, task detail, Current/Draft/Solving and focused return against one blocked process; PASS | STRONG | yes |
| Main 4 | Preserve context and accepted identity; no running candidate | Browser `:2144–2219` compares exact document and run invocation, selected lesson and represented IDs, accepted labels, and absent Proposal; PASS | STRONG | yes |
| Main 5 | Complete feasible result for frozen Draft | Genuine packaged replan in browser `:1731–1794`; independently verified normative fixture at the production process boundary `:2229–2272,3238–3290`; PASS | STRONG | yes |
| Main 6 | Verified Proposal handoff | Browser `:2229–2272` checks Proposal lifecycle/default mode/open task, absent Solving and active run, exact old accepted/Draft and proposed result, retained selection and reachable review decisions; PASS | STRONG | yes |
| Extension 1a | Stale, conflicting or unsaved Draft refuses start | HTTP/JDBC `WorkspaceRepairDraftIT.java:129–160,329–354` and `WorkspaceRepairPlanningIT.java:502–535` assert refusal, exact document/version and no process; PASS | STRONG | yes |
| Extension 2a | Cancellation safely returns to Draft | Browser `:2219–2225`, HTTP `WorkspaceRepairPlanningIT.java:454–535` assert cancelled terminal state, editable Draft, unchanged Current and no candidate or late publication; PASS | STRONG | yes |
| Extension 2b | Actual restart recovers safely | Browser `:2033–2085` stops/restarts the application on one database, checks exact old Current/Draft, run ID/version, interrupted outcome, editable task and absent Proposal; PASS | STRONG | yes |
| Extension 3a | Unrepresentable context clears with explanation only | Browser `:2208–2213` changes to Tuesday from a Monday selection, checks explicit notice, cleared lesson details, intact cancel/status and exact running document; PASS | STRONG | yes |
| Extension 5a | No-feasible, failure, timeout and diagnostics return safely | Browser `:1976–2030`, HTTP/JDBC `WorkspaceRepairPlanningIT.java:420–535` assert safe task/Utilities text, exact Current/Draft, terminal codes and no candidate; PASS | STRONG | yes |
| Extension 5b | Only unchanged no-feasible intent allows PT2M retry | Browser `:1987–2005` starts PT2M then cancels and checks retry absent; HTTP/JDBC `WorkspaceRepairPlanningIT.java:376–419` refuses changed intent by value; PASS | STRONG | yes |
| Extension 6a | Incomplete, stale, mismatched or rejected output cannot become Proposal | Browser `:2224–2228` rejects mismatch before candidate display; HTTP/JDBC `WorkspaceRepairPlanningIT.java:420–535` covers incomplete/stale/non-feasible and late output with exact state; PASS | STRONG | yes |
| G1 | Only cancellation is offered during frozen Solving | `app.js:437–477,595–647`; browser `:2144–2152,2200–2219` checks absent Draft/pin/discard/accept controls and visible cancel; PASS | STRONG | yes |
| G2 | Task/canvas/inspector geometry and persistent status | `styles.css:337–355,410–412`; browser `:2153–2200,2277–2307` measures ≤35% task, headings plus complete row, no overlay or overflow and visible cancel at 1600/1280, even with detail collapsed; fresh screenshots inspected; PASS | STRONG | yes |
| G3 | Presentation actions retain context without a run or data change | Browser `:2153–2219,1925–1942` compares exact versioned document, zero mutating requests and one scheduler invocation through mode/range/focus/area/resize actions; PASS | STRONG | yes |
| G4 | Only independently verified feasible output creates Proposal | `RepairPlanningService.java:145–259`; browser `:1731–1794,2224–2272` and HTTP `WorkspaceRepairPlanningIT.java:420–535` prove verified handoff and safe failure/rejection; PASS | STRONG | yes |
| G5 | Textual status, limit, cancellation and safe outcome | `app.js:450–477`, `messages.js:122–137`; browser `:1976–2030,2151–2169` checks native controls, visible text/diagnostics and no optimality claim; PASS | STRONG | yes |
| G6 | Narrow Solving is read-only; local access unchanged | Browser `:2183–2200` checks 700/390 px true lifecycle without cancel or mutation, exact run; full `WorkspaceImportIT` 19/19 tests cover local access/security; PASS | STRONG | yes |
| G7 | Normative browser covers run and failure branches | Browser `:1976–2272,3122–3290` exercises live inspection, cancellation, actual restart, failures, retry and verified handoff at target/boundary widths with exact durable state; 36/36 shared browser PASS | STRONG | yes |
| Success postcondition | One verified Proposal, old Current still accepted | Browser `:2229–2272` asserts exact Proposal/result, old accepted/Draft, no run, retained selection and open review task; PASS | STRONG | yes |
| Minimal guarantee | All unsafe outcomes preserve Current/Draft and expose no unverified Proposal | Browser `:1976–2085,2219–2228` and HTTP/JDBC `WorkspaceRepairPlanningIT.java:420–535` compare full documents after cancel/restart/failure/timeout/rejection; PASS | STRONG | yes |
| Requires UC-2 | Run consumes approved saved Draft and accepted canvas | Browser `:2090–2143` obtains the exact ready UC-2 intent/pin on UC-1's 1,000-ID accepted view through production actions; full shared UC-1/UC-2 regression PASS | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | “MUST extend the existing packaged workspace” and “MUST NOT fork accepted assignments” | Six-file diff adds no route, model, storage field or dependency. `app.js:35–175,437–465` uses `/api/workspace`, the accepted model and existing renderers; browser compares stable IDs across modes. | PASS |
| RULE-2 | One owner “MUST derive available/default modes”; presentation actions “MUST NOT issue durable mutation requests” | `inspection-state.js:1–104`, `app.js:117–175`; browser `:2153–2219` checks per-mode detail state, no mutating fetch, exact document/run and reset/retention behavior. | PASS |
| RULE-3 | Services “MUST enforce only” the declared transitions and refuse other starts at the mutation boundary | `RepairPlanningService.java:47–97,145–259`; HTTP `WorkspaceRepairPlanningIT.java:376–535` and Draft HTTP tests assert stale/conflict/unsaved/retry/cancel/late refusal, exact state and no ineligible process/Proposal. | PASS |
| RULE-5 | Inspector “MUST” stay contextual and the task area “MUST” be wide, below canvas and ≤35% height | `app.js:176–257,437–477,595–647`, `styles.css:337–355,410–412`; browser `:2153–2200,2229–2307` and fresh screenshots prove geometry, responsive split, retained selection and reachable controls. | PASS |
| RULE-7 | Views “MUST use authoritative school-controlled display names”; missing candidate metadata “MUST be refused” | `app.js:469–477,595–647,750–766`, `KernelVerifier.java:74–109`; normative browser checks long names/stable IDs and verifier-gated successor; rejected candidate stays absent. Full import/security regressions pass. | PASS |
| RULE-9 | Solving “MUST consume only a saved ready Draft”; only verified feasible output “MAY create Proposal” | `RepairPlanningService.java:47–97,145–259`, `app.js:437–477`; browser `:1900–2272` and planning HTTP `:376–535` prove frozen accepted view, bounded cancel/restart/failure/retry and independent verification. | PASS |
| RULE-12 | Route/access table “MUST remain unchanged”; ≤700 px “MUST” be read-only | No server/security diff; full 19-case `WorkspaceImportIT` hostile Host/Origin/CSRF/`If-Match` regression passed. Browser `:2183–2200` checks 700/390 read-only Solving and exact run. | PASS |
| RULE-13 | New text “MUST use” the catalog; status and effects “MUST” have non-color cues and native controls | Four new labels are in `messages.js:122–137`; `app.js:450–477` uses textual status/limit/frozen intent and native buttons. Browser checks visible cancellation, diagnostics and Utilities; initial journeys remain green. | PASS |
| RULE-14 | Normative isolated actor-boundary and complete shared browser regression “MUST” run; automation “MUST NOT” be called administrator approval | `WorkspaceBrowserIT.java:1976–2307,3122–3290`; independent focused 14 integration and full 36-browser/84-workspace-integration runs passed with exact DB comparisons. Human walkthrough remains pending. | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Workbench-layout UC-1 and UC-2 | Required accepted canvas, ready Draft, modes, focus, inspector, wide task and narrow layout | Independent full browser suite 36/36 and the UC-3 opening 1,000-ID accepted/Draft journey passed; prior approvals unchanged | PASS |
| Existing workspace UC-1–UC-8 and inspection-UX UC-1–UC-3 | Shared import, local access, accepted model, renderer, repair process and proposal lifecycle | Independent full reactor 195/195, including workspace import 19/19, Draft 9/9, repair planning 10/10 and browser 36/36 | PASS |
| `timetable-ux-polish` UC-1–UC-4 and UC-5 automation | Shared operational browser and repair lifecycle | Full shared suite passed; the separate UC-5 five-administrator gate remains `PENDING_WALKTHROUGH` in its own ledger | PASS automation only |

## Findings

None. The full run logged a proposal-review opening of 1063.6 ms against a 1000 ms diagnostic-only reference; no functional or technical-rule timing gate failed. The first sandboxed focused attempt could not access Docker, but the identical authorized run and the full regression both passed.

## Walkthrough

Automated convergence passed. The UC-3 walkthrough script asked the administrator to start from a saved, conflict-free protected Draft and identify accepted Current, frozen weekly intent and pin, PT30S limit, status and Cancel run in the wide task area. During Solving, the script covered lesson selection; Week/Day and Current/Draft/Solving modes; frozen-detail and inspector collapse/reopen; focused schedule return; and confirmation that those actions neither restart the run nor make its output Current. It then covered cancellation back to editable Draft with unchanged Current and no Proposal, a subsequent successful handoff to the open Proposal task area with the prior Current still accepted, and the narrow read-only Solving view. Automated fault journeys covered actual restart, unsuccessful/rejected/timeout outcomes and retry eligibility without asking the administrator to force process or storage failures. On 2026-09-24, the user explicitly replied “WALKTHROUGH accepted.” No additional per-step observations were supplied. This confirms UC-3 only; it does not satisfy the separate five-participant UC-5 feature gate.

## Status Update

UC-3 `READY_FOR_CONVERGENCE` → `PENDING_WALKTHROUGH` after the automated audit, then `PENDING_WALKTHROUGH` → `APPROVED` after explicit administrator confirmation. UC-4 is next eligible. UC-1 and UC-2 remain `APPROVED`; UC-4 and UC-5 remain `NOT_STARTED`.

## Response to execute

APPROVED
