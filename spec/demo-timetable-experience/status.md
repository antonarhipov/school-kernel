# Timetable-first demo execution

Base: `d55ab636558224f2d33a5e71313e840dd9486428` (`demo`). User authorized all agreed behavior on 2026-10-16.
The existing demo-deployment implementation is pre-existing dirty work and is preserved. No commit was requested.

| UC | Status | Requires | Evidence |
|---|---|---|---|
| UC-1 | APPROVED | None | [Checkpoint](checkpoints/UC-1.md); [passing Chrome ledger](../../output/playwright/demo-timetable-experience/UC-1/local-1792180297118/ledger.md) |
| UC-2 | APPROVED | UC-1 | [Checkpoint](checkpoints/UC-2.md); [passing Chrome ledger](../../output/playwright/demo-timetable-experience/UC-2/local-1792181856760/ledger.md) |
| UC-3 | APPROVED | UC-1, UC-2 | [Checkpoint](checkpoints/UC-3.md); [passing Chrome ledger](../../output/playwright/demo-timetable-experience/UC-3/local-1792181645428/ledger.md) |
| UC-4 | APPROVED | UC-1 | Scoped browser-local cleanup; isolated and public Chrome journeys pass |

## Material constraints

- Demo-only client entry and endpoints; normal-profile behavior retains its existing contract.
- Browser-owned school/version/draft catalog; packaged samples are immutable. Failed storage writes preserve old data.
- Full mandatory validation and scoring must use the authoritative kernel evaluator rather than a partial duplicate.
- In-flight validation is exact-candidate scoped; switching must not apply stale responses to another draft.
- Physical kernel calls retain bounded concurrency, allowance accounting, private temporary files and cleanup.
- The pre-existing demo deployment's capacity/billing qualification is separate from functional acceptance here.

## Evidence

- 2026-10-16: Recorded the complete confirmed proposal, declarative three-UC specification and scope overrides.
  Existing repository has no applicable AGENTS.md. Existing demo-deployment status, rules, current production source
  and test infrastructure inspected. Specification relationship graph is UC-1 → UC-2 → UC-3 and acyclic.

- 2026-10-16: UC-1 approved from six sample unit tests, four storage checks and the complete recorded Chrome journey, including corrupt-entry/storage recovery; no browser errors. UC-2 is next eligible and started.

- 2026-10-16: UC-2 approved after complete packaged Chrome journey, including real delayed stale response, failed-check retry, 390px non-overlap, exact saved values, storage refusal and actual restart. All focused CLI/storage/transport/lifecycle checks pass. UC-3 is next eligible and started.

- 2026-10-16: Full integrated DemoTimetableBrowserIT run passed all three actor journeys: UC-1 `local-1792181471256`, UC-2 `local-1792181545936`, UC-3 `local-1792181645428`. UC-3 approved; editor/generation handoffs closed. All browser contexts and private services stopped; screenshots inspected. Full reactor unit tests: 185 passed. Eight Node persistence/feedback checks pass. Packaged source-identity scan and all four corrupt-readiness/maintenance checks pass.

- 2026-10-16: Final UC-2 full journey passed again at `local-1792181856760`, including exactly one eligibility-error
  highlight, named gap times, room/time suitability hints, delayed stale response, failed-check retry, 390px layout,
  quota refusal, exact named save and real restart. Three packaged ReviewCliIT checks pass; ten Node checks pass.
  Final production/maintenance source and both JAR hashes are pinned in
  `output/execution/demo-timetable-experience/final/`; moved legacy fixtures were checked byte-for-byte against
  the pre-existing baseline. The final JAR scan passes. `git diff --check` passes. All three UCs are APPROVED,
  no destination handoff remains, and all disposable browsers/private services are stopped.
  Work is complete locally; public redeployment and the separate capacity/billing qualification were not performed.

- 2026-10-16: User authorized deployment to the existing Free service. Production-only 171-file upload is live as
  `0a3da7be-c9ac-4d08-9d84-005c8d6ff087` (SUCCESS) at
  [the public demo](https://demo-smoke-production.up.railway.app/workspace/). Provider plan remains FREE; complete
  service settings and runtime variables are unchanged. `review` now uses the existing 96 MiB verification heap;
  full-school review and launcher selection checks pass. The full public Chrome journey passes all six matrices,
  switching/filter rules, actionable teacher/room/period conflicts, exact immutable named save/reload, real Quick
  generation and 390px layout. A separately recorded actual same-image restart preserves exact browser-saved data.
  Before/after stateless edge checks, 40 static hashes, six exact sample pairs and isolated ingress pass. Final
  aggregate records zero active demo workspaces. The first remote attempt's harness notification mismatch is
  retained; no production UI fix was needed. [Deployment ledger](../../output/execution/demo-timetable-experience/free-20261016-1/ledger.md),
  [public walkthrough](../../output/playwright/demo-timetable-experience/public/free-20261016-2/ledger.md),
  [restart proof](../../output/playwright/demo-timetable-experience/public/free-restart-20261016-1/ledger.md).
  All three UCs remain APPROVED. Free sleeping/limited capacity remains distinct from paid launch qualification.

- 2026-10-17: User rejected the new matrix styling and requested the previous compact retro appearance. Restoring
  the existing shared `current-mode`, `week-lesson` and 24-colour subject rules; paper background, compact shell,
  fixed density and always-visible tiny room labels. Only the demo CSS and presentation markup change; validation,
  saved browser data, school/version navigation and backend code remain exact. Local and public browser evidence
  for this correction is being recorded under `output/execution/demo-timetable-experience/retro-20261017-1/`.

- 2026-10-17: Compact retro correction is verified and live as `7af6ff33-8d8a-41ae-9022-4daf4d112d5c` (SUCCESS).
  Only two presentation files differ from the preceding deployment. Existing shared matrix rules and palette are
  restored, with measured 20px Week tiles/27px rows and visible tiny room labels. Two local Chrome journeys,
  complete public Chrome/edit/save/Quick journey, exact saved-version proof after actual restart, desktop/390px
  screenshot inspection, all 40 static hashes/six exact pairs and edge checks pass. FREE plan and resource manifest
  are unchanged. Cleanup records zero active workspaces; no commit. All three UCs remain APPROVED.
  [Restoration/deployment ledger](../../output/execution/demo-timetable-experience/retro-20261017-1/ledger.md),
  [public matrix screenshot](../../output/playwright/demo-timetable-experience/public/retro-20261017-1/00-default-retro-matrix.png).

- 2026-10-17: User requested a fourth bundled school in English at Kasevälja's scale, with distinct class/staff/room
  counts and its own timetable. UC-1 catalog extension is IN_PROGRESS; previous approvals retain their recorded
  scope. Meadowbrook is constructed independently: 22 classes in Years 1–9, 614 weekly lessons, 41 teachers,
  35 rooms and 30 periods. Consistent course teachers and daily subject limits keep schedules plausible. Its two
  variants are evaluated by the real kernel; source examples, the existing nine definition/result files, and all
  UI/style files are unchanged. Evidence: `output/execution/demo-timetable-experience/english-20261017-1/`.

- 2026-10-17: UC-1's English catalog extension is APPROVED and live as
  `dc3b9ac9-909b-4d18-a60d-b87a5f4cf7e0` (SUCCESS). Meadowbrook has 22 classes, 614 lessons, 41 teachers,
  35 rooms and two feasible variants differing in 32 weekday placements. Seven sample tests, four events tests,
  five corrupt-readiness/maintenance checks, full packaged UC-1 Chrome journey and full public Chrome journey pass.
  English 614-lesson validation works on the unchanged Free service and explains/blocks a real collision.
  All original samples and all 40 static files are exact. Actual restart preserves exact browser-saved data;
  final aggregate shows zero active workspaces. The first restart harness's relative-path error was corrected;
  its failed evidence is retained. No product fix was required. All three UCs remain APPROVED; no handoff remains.
  [English addition/deployment ledger](../../output/execution/demo-timetable-experience/english-20261017-1/ledger.md),
  [final local journey](../../output/playwright/demo-timetable-experience/UC-1/local-1792185961440/ledger.md),
  [public journey](../../output/playwright/demo-timetable-experience/public/english-20261017-1/ledger.md),
  [restart proof](../../output/playwright/demo-timetable-experience/public/english-restart-20261017-2/ledger.md).

- 2026-10-17: User requested renaming and logically rebalancing the Star Wars school. UC-1's Imperial curriculum
  revision is IN_PROGRESS; previous approvals retain their recorded scope. The independent Coruscant Imperial
  Academy definition has junior/intermediate/senior stages with 25/30/35 weekly lessons, 18 cohorts, 540 lessons,
  29 qualified specialist teachers and 37 rooms. Both variants pass complete kernel evaluation, with 32 lessons
  moved. Other school artifacts and all static/style files remain unchanged. Evidence is being recorded under
  `output/execution/demo-timetable-experience/imperial-20261017-1/`.

- 2026-10-17: UC-1's Imperial curriculum revision is APPROVED and live as
  `fe66a8a4-9004-4d01-b229-2e7216d83c5f` (SUCCESS). Coruscant Imperial Academy now has 18 cohorts,
  540 lessons, 29 specialist teachers and 37 rooms. Junior/intermediate/senior classes have balanced 25/30/35
  lesson weeks, no class gaps, uniform five/six/seven-lesson days and daily subject limits. Both alternatives
  are complete kernel-verified schedules. Eight sample tests, all three packaged browser journeys, six refusal
  checks, full public Chrome and actual same-image restart pass. A full-school Imperial collision is explained
  and blocks saving on Free. Other schools and all 40 UI/style files are exact. The initial public test assumed
  the first cohort must differ between alternatives; it now selects a genuinely changed cohort without weakening
  filter/network/difference assertions, and its failed evidence is retained. Final active workspaces/processes
  are zero, temporary browser state removed, no commit. All three UCs remain APPROVED; no handoff remains.
  [Revision/deployment ledger](../../output/execution/demo-timetable-experience/imperial-20261017-1/ledger.md),
  [public journey](../../output/playwright/demo-timetable-experience/public/imperial-20261017-2/ledger.md),
  [restart proof](../../output/playwright/demo-timetable-experience/public/imperial-restart-20261017-1/ledger.md).

- 2026-10-17: UC-4 browser-local cleanup is APPROVED and live as
  `8891ef46-d92d-4e51-8efc-66690e81e6de` (SUCCESS). Utilities explains the local browser/device scope, with
  explicit confirmation. Thirteen Node storage checks, eight sample checks, exact isolated production and complete
  public Chrome journeys pass. Cancel/Escape preserve exact data; removal refusal restores raw values; confirmed
  cleanup removes current/legacy/corrupt artifacts and drafts without server mutation, and stale tabs cannot restore
  them. Cookies, server state, other browser owners and unrelated preferences are preserved; new work saves afterward.
  Same-image restart and final health/edge/static checks pass; final workspaces/processes/queue/cache are zero.
  FREE settings and the compact retro matrix are unchanged. The three-asset release excludes concurrent backend/editor
  work, which remains preserved in the shared checkout. All four UCs are APPROVED; no commit or handoff remains.
  [Cleanup/deployment ledger](../../output/execution/demo-timetable-experience/local-data-20261017-1/ledger.md),
  [public cleanup journey](../../output/playwright/demo-timetable-experience/public/local-data-20261017-1/ledger.md),
  [verification summary](../../output/execution/demo-timetable-experience/local-data-20261017-1/verification-summary.json).
