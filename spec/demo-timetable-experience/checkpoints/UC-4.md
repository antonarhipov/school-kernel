# UC-4 checkpoint

Base `d55ab636558224f2d33a5e71313e840dd9486428`, branch `demo`; existing uncommitted work preserved, no commit requested.
User requested browser-local school/timetable cleanup on 2026-10-17.

Utilities includes a local-data explanation/button and a confirmation with Cancel initially focused. Explicit
confirmation deletes the current timetable artifact key, historical definition keys and their use metadata,
including malformed raw entries. It clears drafts/editor work, prevents pending callbacks from writing artifacts
back, preserves the stale-tab guard and reloads the default bundled matrix. Unrelated preferences/storage,
cookies, bundled samples, other owners and server state are unchanged. Refused deletion restores exact raw values
where possible; partial restoration failure is clearly reported.

Production delta: only demo-workbench.js, demo-workbench.css and timetable-storage.js. Existing shared matrix
rules and rendering markup are unchanged. Initial post-cleanup default screenshot is byte-for-byte identical to
the previous public release. Concurrent backend work is excluded by reconstructing the prior release's production
archive and overlaying exactly these three assets. [Manifest, preservation and verification ledger](../../../output/execution/demo-timetable-experience/local-data-20261017-1/ledger.md).

UC-4 is APPROVED. Thirteen Node storage checks and eight sample checks pass. Shared-checkout packaged navigation,
the exact isolated production journey and the complete public Chrome cleanup journey pass. Public checks cover
Cancel/Escape, 390px confirmation, removal refusal/rollback, scoped deletion, stale-tab refusal, corrupt-storage
recovery, bundled alternatives and saving new work. Exact server state, cookies, unrelated preferences and another
browser owner are preserved; cleanup sends no server mutation. The public screenshots were inspected.

The actual same-image restart retains the identical service artifact, kernel/settings and four VERIFIED schools.
Final health/edge/static checks pass; all 40 public static assets/eight pairs match the pinned release. Final counters
show zero workspaces, running processes, waiting owners and cache entries. All 174 production files agree across
archive/stage/isolated source; 59 packaged resources and nine verification inputs agree. FREE settings are unchanged.
Failed isolation attempts remain recorded in the linked ledger; concurrent source, including editor autosave, is
preserved in the shared checkout and excluded from the cleanup-only release. No commit or destination handoff remains.
