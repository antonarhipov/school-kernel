# UC-2 checkpoint

Base: `d55ab636558224f2d33a5e71313e840dd9486428`; uncommitted authorized changes on `demo`.
UC-1 is approved. Pre-existing dirty source is preserved separately in the feature baseline capture.

Implemented: teacher/room/period inspector edits; whole-candidate kernel review; draft source snapshots and exact
per-lesson patches; actionable global and local conflicts, lesson highlighting, preference counts and source deltas;
name/Save/Discard bottom overlay; exact-candidate/build checks; stale response refusal; atomic browser version saves.
Original versions and all unedited assignment fields remain unchanged. Validation is never restored from browser flags.

The new CLI `review` operation uses the existing definition loader, PlanningMapper, all 19 ScheduleEvaluator hard
constraints and seven preference counters, HardConstraintDiagnostics and ResultFactory. No solver runs or moves occur.
A valid reviewed result has actual revisions/score and explicit MANUAL_REVIEW / NONE provenance. The result schema
accepts this new provenance; existing plan/replan values retain their behavior.
The demo review endpoint shares the existing two verification slots, allowance, exact build/input cache, private
process transport, watchdog, shutdown interruption and cleanup. Existing host/origin/CSRF/session/size/complexity gates apply.

Storage saves atomically add one version and remove exactly its source draft. Quota failure retains the previous
complete durable document and the in-memory edit. Another tab cannot overwrite unseen changes. Source snapshots
keep old drafts meaningful when future deployments change the bundled examples.

Evidence: three packaged ReviewCliIT tests; five Node storage tests; six sample, four kernel transport and one
verification lifecycle tests. Initial real Chrome journey passed at
`output/playwright/demo-timetable-experience/UC-2/local-1792180754064/ledger.md`, including eligibility, collisions,
room capability, hidden-lesson navigation, reload, duplicate/blank names, injected storage failure, exact teacher-only
saved assignments, independent original and actual service restart. Refined collective-day highlighting and added
stale/failure/mobile checks passed the final full journey at
`output/playwright/demo-timetable-experience/UC-2/local-1792180964253/ledger.md`. Desktop conflict and mobile screenshots inspected; no overlap or browser errors.

Changes: ReviewService and CLI command; result schema; KernelVerifier/DemoVerificationService; DemoTimetableController
and security route; demo transport/feedback/workbench/storage/CSS; focused CLI/storage/browser tests.
Destination handoff: Utilities school-copy/generation remains owned by UC-3. No deployment is asserted.

Final complete edit journey: `output/playwright/demo-timetable-experience/UC-2/local-1792181856760/ledger.md` (PASS). Conflicts name actual empty periods/capacity; eligibility errors identify only the named lesson; ten Node checks pass. Final source/artifact identity is pinned in the feature execution final directory.
