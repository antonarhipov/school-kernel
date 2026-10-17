# Timetable landing and Day pattern correction

Date: 2026-10-18. Base: `be5b76d0ffc8a9851c4e8308540b1de12c5b4425` plus the existing uncommitted feature. The user rejected the earlier UI alignment; this checkpoint supersedes its presentation verdict. No commit or deployment is authorized or performed.

## Corrected behavior

- The timetable remains the initial demo surface. Setup has an explicit closed-dialog style and opens only through the vertical left entry. No setup draft is created by arrival alone. Closing retains the exact selected school/timetable/filter matrix. Stored drafts resume only after opening setup.
- First setup entry offers **Edit a copy** of the selected school and **Create new school**. New/copy actions also remain visible when resuming a draft. Empty schools have no fabricated catalog, demand or intervals.
- Bundled definitions without authoring snapshots show their actual grid in a read-only preview. Explicit conversion still discloses and requires acceptance of rule adoption before editing; it creates a separate browser-local draft. It opens at the section from which editing was requested. Copies with authoring snapshots start at Timetable grid; unfinished drafts retain their editing location.
- The linked `output/design/school-setup-revival/grid-editor-preview.html` remains the design reference: 148px sidebar, 22px content padding/title, compact active-day tabs, type/start/end/remove rows, day copying, week overview, simple close footer, and the existing 350ms horizontal rollout with reduced-motion support. Extra step headings and next/previous controls are removed. Interval names/order, weekday configuration and JSON/recovery tools use disclosures. Text time inputs preserve malformed/unfinished values required by UC-2.
- Immutable named school saves and explicit generation retain their existing contracts. No kernel, server or public-deployment behavior is changed by this UI correction.

## Evidence boundary

The unchanged packaged-demo baseline already landed on the bundled timetable in fresh Chrome and after reload: [baseline ledger](../../../output/playwright/school-setup-editor/UI-landing/1792276530288/ledger.md). The user's unexpected setup landing was not reproduced there; this checkpoint does not claim a diagnosed cause. New browser coverage exercises the exact arrival/copy/new/reload sequence.

The first correction run retained three failing journeys. Two used obsolete row/navigation selectors; the third attempted a now-disclosed weekday checkbox without opening its disclosure. Tests now navigate the actual controls and retain the original value, identity, nonmutation and recovery assertions. Failed attempts remain available in their original output directories.

Final evidence and verdict are recorded in the linked correction ledger when verification completes.

## Final verdict: automated PASS

[Correction ledger and exact source submission](../../../output/school-setup-editor/2026-10-18-timetable-landing/ledger.md).

- Complete setup regression: 19/19 Chrome journeys passed. After two small grid refinements (generated names follow the chosen interval kind until customized; deactivating the selected day falls back to a surviving active day), the affected UC-1, UC-2 and prototype journeys passed again, followed by the expanded landing journey. Its initial `uncheck()` assertion was unsuitable for a control whose removal is intentionally deferred until confirmation; the final journey clicks, confirms, and asserts the resulting state. This failed attempt is retained.
- [Final arrival/copy/new/reload journey](../../../output/playwright/school-setup-editor/UI-landing/1792277403078/ledger.md): fresh matrix, explicit choices, read-only source, explicit adoption, independent locally stored edits, unchanged bundled payload, exact matrix return, empty school and reload while setup was open all pass.
- [Final prototype journey](../../../output/playwright/school-setup-editor/UI-prototype/1792277324140/ledger.md): actual linked reference, eight interval rows, copied week, rollout, six sections, optional tools, 320/390/768/1440px overflow and footer reachability, Escape/focus and reduced motion pass. Desktop, narrow, source/editable, chooser and return-to-matrix screenshots inspected directly.
- JavaScript checks: 41/41 pass. `git diff --check` passes. Browser contexts and isolated local servers are closed. No unrelated work is staged, committed or deployed.
