# UC-3 checkpoint

Base: `d55ab636558224f2d33a5e71313e840dd9486428`; uncommitted authorized implementation on `demo`.
UC-1 and UC-2 approved before this slice. Existing unrelated work is preserved by the baseline capture.

Implemented: Utilities school-definition editor, located JSON feedback, complete bounded stateless definition check,
named immutable browser school copies, exact original document retention, explicit Quick/Thorough generation,
owner-scoped queue/status/cancellation, failed/interrupted recovery, originating-school result association, generated
candidate retention and UC-2 naming/save. Generation never mutates another school, current visible timetable or draft.
The complete generated result retains its original solver limit, score/revisions and provenance when saved without edits.
A generated candidate is distinct from a saved version; Discard removes it and Save requires a new name.

Copied definitions receive independent browser identities while preserving every authored field/optional value.
Existing browser definition envelopes remain untouched and are exposed as recoverable schools without timetables.
Run identities/status and generated complete candidates are retained in browser storage; unavailable session runs
produce an interruption explanation and retain the school. Navigation does not wait for a run or change viewed data.
Bundled fixtures and their source requirements remain immutable.

Initial complete Chrome journey passed at
`output/playwright/demo-timetable-experience/UC-3/local-1792181263356/ledger.md`:
invalid JSON/unknown teacher errors, exact editor reload, duplicate name refusal, complete school copy with no solve,
real Thorough cancellation, injected actual process failure, real Quick checked result, school-switch isolation,
candidate reload, exact named result/definition/provenance, browser-owner isolation and 390px editor layout.
Screenshots inspected; no page overflow or browser errors. Final integrated journey is linked from status.
Technical checks: six custom-definition, nine planning and six sample tests; full 185-test reactor unit run passes.

Changed source: DemoTimetableController definition-check endpoint; demo workbench editor/generation lifecycle;
legacy fixture paths moved to test resources with dependent fixture readers updated; DemoEvents/definition origin
sample allowlists; current six-pair maintenance/scoring/provenance/readiness tooling and deployment documentation.
The old Small-school fixtures remain only in tests, not production resources. Maintenance placement seeds remain
outside shipped resources. All six scores independently reevaluated before evidence hashing.

Historical demo-deployment browser suites target removed editor/import/export/repair controls; they are retained as
historical source and are superseded for this UI by DemoTimetableBrowserIT. Their backend unit boundaries remain run.
No external deployment, billing or provider capacity qualification is asserted. No destination handoff remains.

Final integrated UC-3 journey: `output/playwright/demo-timetable-experience/UC-3/local-1792181645428/ledger.md` (PASS). All handoffs complete.
