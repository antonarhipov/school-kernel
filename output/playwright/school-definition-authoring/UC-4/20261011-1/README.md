# UC-4 walkthrough — attempt 1

- Verdict: **FAIL**, F-1. Date 2026-10-11; submission aba0f60; Chrome session authoring-loop, packaged application http://localhost:18083/workspace/.
- Isolation: retained disposable authoring_loop_r2 database on loopback 24483. Starting DEFINITION_DRAFT version 12, empty subjects, exact prior UC-3 period grid, source {}, no Current. No durable write occurred.
- Main 1–2: entered Subjects via real navigation. Expected every field to have an accessible control label. Observed all seven fields, but both numeric value textboxes have no accessible name because their label already labels the mode selector. FAIL.
- [Snapshot and assertion](accessible-controls.txt), [screenshot](missing-number-label.png). The failure was discovered during form inspection before mutation. Remaining main, negative, stale, responsive and reload steps were not run and are not claimed PASS.
- Fix: add explicit accessible names to the numeric value inputs and a real-browser assertion. Repeat the complete walkthrough after technical checks.
- Disposition: browser/application/database retained for the authorized fix/retest loop. Operator data untouched.
- Screenshot inspected: the visible two-column layout is readable; the failure concerns accessible control names, evidenced by the browser snapshot. No unrelated visual failure was found.
