# UC-15 walkthrough attempt 3 — FAIL

2026-10-12, submission `528d082`, Chromium localhost:18094, stable production JAR and disposable authoring_loop_uc15_r3. Correct accepted source imported/opened; mon1 timing, Alex availability[] and Blair name saved v5. Class save failed with `Failed to fetch` and main timed out waiting for Saved. [Failure state/snapshot](failure.json) shows class work unsaved and exact durable v5.

[Request failure reproduction](class-reproduction.txt) proves `/api/definition-draft/cohorts` was aborted with net::ERR_FAILED. Clearing all stale agent-owned page/context request routes and explicitly retrying [through UI](class-route-recovery.txt) committed the class save v6 normally. This was residual fault interception in the long-lived browser session, not an application defect. v6 is a legitimate follow-up save, not failed-transport non-mutation evidence. Full main/recovery journey not complete; no approval.

Use a fresh Chrome session and disposable database for a full repeat. Scripts/output/failure screenshot retained. Source and Current exact; operator data untouched. Server/database retained until next run preparation.
