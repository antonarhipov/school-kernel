# UC-7 — Named exact school snapshot

Base `3cb66f30ef996a8cf3bb93f968cc53db15fc1191`; automated approval 2026-10-18. No commit/deployment requested.

The existing named-save lifecycle consumes UC-6's exact catalog11 checked revision and atomically stores a new immutable school identity, definition/revision/build, complete cloned authoring/check and source identity while consuming only its matching setup draft. The extended schema/compiler supplies grouped policies and optional values without changing the storage boundary. Incomplete unused authoring remains outside active definition but within the school authoring snapshot. Existing name checks, stale guards, quota retry, startup default, source-copy and explicit selection behavior apply. No additional production save mechanism was necessary.

[Passing full packaged Chrome journey](../../../output/playwright/student-groups-and-coordinated-lessons/UC-7/1792351295827/ledger.md): 1/1 PASS. Blank name refuses; named insertion quota injection leaves exact authoring/old catalog; retry saves complete checked group/teacher/gap-free data plus unfinished unused split. Exact catalog11/revision comparisons pass. No plan process or timetable is created. Reload opens bundled default, explicit school selection restores copy; duplicate name refuses; an independent source-linked edit cannot overwrite the saved source; real second-tab update prevents stale save and retains newer durable work. Saved source compares by value throughout. Inspected refusal screenshot with recovery explanation and unchanged source. Context/processes closed; isolated data only.

The implementation inherited 72 passing JS checks, CLI 4/4 and UC-6/5 browser regressions. Exact working source/test submission is captured alongside ledger in `submission-files.txt`, `submission.patch`, `source/`; unrelated Python cache excluded.

Remaining handoffs: generation selection/search UC-9/10 and independent assignments UC-8. No feasibility or generated timetable is claimed from named save.
