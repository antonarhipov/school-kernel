# UC-1 checkpoint

Base: `d55ab636558224f2d33a5e71313e840dd9486428`, branch `demo`, uncommitted authorized implementation.
Pre-existing dirty work is recorded separately in `output/execution/demo-timetable-experience/baseline-20261016/`.

Implemented: demo-only timetable entry; browser-held three-school/six-version catalog; full Estonian Variant A default;
instant switching with no network/process launch; same-school filters retained, different-school filters reset;
readable exact saved versions restored without overwriting malformed entries; responsive matrix/lesson details;
Utilities editor-only shell. Browser persistence explicitly distinguishes browser data from temporary session data.

Samples: fictional synthetic identities, Estonian curriculum and primary class-teacher blocks expanded to real subjects;
56-lesson 5A/5B and two 520-lesson/23-class schools. Each variant differs in 32 lesson placements across weekdays.
Every pair is evaluated by ScheduleEvaluator and verified at packaged application startup with matching build/hash/score.

Changed source: DemoSamples, DemoSampleController, SecurityConfiguration, DemoSessionFilter; demo branch in app.js;
index.html; demo-workbench.js/css; timetable-storage.js; demo/timetables resources; build/variant/evidence maintenance tools.
Checks: six DemoSamplesTest tests, four Node storage tests and actual packaged Chrome journey.
The first corrected journey passed in `output/playwright/demo-timetable-experience/UC-1/local-1792180191863/ledger.md`.
Desktop and 390px screenshots inspected: controls wrap, no page overflow, matrix scroll contained, lesson popup fits.
Two earlier readiness failures were test waits (hidden option visibility/default 5-second wait); retained with evidence.
Final recovery journey passed: `output/playwright/demo-timetable-experience/UC-1/local-1792180297118/ledger.md`. Both corruption recovery cases preserved stored values.

Destination handoffs: editing/save toolbar belongs to UC-2; Utilities school-copy/generation controls belong to UC-3.
No public deployment or capacity qualification is asserted.
# Styling correction, 2026-10-17

User explicitly requested restoring the preceding compact retro matrix. Shared original matrix/tile/subject palette
rules now govern the demo, including fixed compact density and visible room text. Only presentation CSS/markup
changed. Local and published desktop/narrow Chrome checks pass; published Week tiles measure 20px and rows 27px.
Current Free deployment `7af6ff33-8d8a-41ae-9022-4daf4d112d5c` preserves all navigation/editing/save behavior.
[Correction evidence](../../../output/execution/demo-timetable-experience/retro-20261017-1/ledger.md). UC-1 remains APPROVED.

# English school addition, 2026-10-17

User requested a fourth school comparable in scale to Kasevälja, with different resources and its own timetable.
Meadowbrook Community School is independently constructed in English: 22 classes, 614 weekly lessons, 41 teachers,
35 rooms, 30 periods. Two evaluated/verified feasible variants move 32 lessons across weekdays. Daily subject
limits, fixed course teachers, age-specific workloads and specialist rooms keep the definition plausible.
Existing samples and every static/style file are unchanged. The normative catalog now has four schools/eight pairs.

UC-1 extension is APPROVED from seven sample tests, four events tests, five refusal checks, the complete final
packaged Chrome journey and the complete public Chrome/edit/save/generation journey. Exact saved data survives
actual same-image restart; final workspaces/processes are zero. Existing UC-2/UC-3 handoffs remain closed.
[Full verification/deployment ledger](../../../output/execution/demo-timetable-experience/english-20261017-1/ledger.md).

# Imperial curriculum revision, 2026-10-17

Coruscant Imperial Academy replaces the former renamed-copy example with an independently constructed definition:
18 cohorts, 540 lessons, 29 specialist teachers, 37 rooms and 27 subjects. Junior/intermediate/senior stages have
25/30/35 weekly lessons with uniform five/six/seven-lesson days, no class gaps and once-daily subjects. Fixed course
teachers, balanced staff loads and specialized room capabilities/capacities apply to both complete verified
alternatives, which differ in 32 weekday placements. Other school artifacts and every UI/style file remain exact.

UC-1 revision is APPROVED from eight sample tests, all three packaged Chrome journeys, six refusal checks,
full public Chrome validation/edit/save/generation and exact browser-saved data after actual same-image restart.
The initial public harness's first-cohort-difference assumption was corrected to derive a genuinely changed class;
failed evidence retained, no production change needed. The unchanged Free service is live as
`fe66a8a4-9004-4d01-b229-2e7216d83c5f`. Final workspaces/processes are zero; existing handoffs remain closed.
[Exact production manifest, evidence and deployment ledger](../../../output/execution/demo-timetable-experience/imperial-20261017-1/ledger.md).

# Compact mobile correction, 2026-10-17

Base `4911b04`, initially clean. User authorized phone-friendly compact presentation. UC-1 extension 3a and G7
now require collapsed filters and a whole week fitting phone width. Phone rendering places population labels
above schedules, uses short weekday labels and a 22px period column, and preserves full details in lesson inspection.
Week/Day controls remain visible; filters retain values and show an active count. Desktop keeps its original matrix.

Changed production files: `demo-workbench.js`, `demo-workbench.css`, new `mobile-matrix.js`. The new
`MobileTimetableBrowserIT` records the complete phone journey; the existing exploration test opens the disclosure
before selecting a mobile filter. Proposal/spec/status record the user request and verification. No definition,
result, storage contract, backend or operator data changes; no commit or deployment.

APPROVED: final package and exact resource hashes, full exploration and dedicated mobile journeys pass;
320/360/390/430/700px, all eight variants, exact source lesson identities, zero horizontal overflow, visible rooms,
filters/lenses/highlights, inspector timings, phone/desktop resize and reload are verified. No mutating requests or
additional kernel processes during exploration; no browser runtime errors. Screenshot inspection confirms layout.
Earlier edit/save and generation PASS evidence is reused for unchanged behavior. Failed attempts are retained.
No outstanding destination handoff.

[Execution ledger and exact manifest](../../../output/execution/demo-timetable-experience/mobile-20261017-1/ledger.md),
[final phone journey](../../../output/playwright/demo-timetable-experience/UC-1/mobile-1792192912633/ledger.md),
[final exploration journey](../../../output/playwright/demo-timetable-experience/UC-1/local-1792192823136/ledger.md).

## Mobile deployment, 2026-10-17

After local verification, the user explicitly requested deployment. The exact three mobile assets are live in
`e8b68aa9-97a3-4e68-80a1-a65658fac2fb` (SUCCESS), with all other bytes from the preceding hardening release intact.
Public Chrome repeats the complete phone journey successfully; all 42 static assets/eight sample pairs match;
health, edge, restart and final zero-workspace checks pass. Free plan/full service manifest/kernel are unchanged.
UC-1 remains APPROVED. [Deployment evidence](../../../output/execution/demo-timetable-experience/mobile-deploy-20261017-1/ledger.md).
