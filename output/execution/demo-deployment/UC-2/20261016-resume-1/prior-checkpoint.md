# UC-2 checkpoint

Status: BLOCKED on public Railway evidence; local implementation/walkthrough PASS, 2026-10-16.
Base: `d55ab636558224f2d33a5e71313e840dd9486428`, branch `demo`; no commit requested. An isolated Free smoke is now deployed.
Pre-existing specification/index changes remain separate and preserved.

Private owner authority is held only in the required Secure/HttpOnly/SameSite Strict browser session. The demo uses
in-memory aggregates behind `WorkspaceStore`; normal local JDBC persistence and recovery remain the non-demo profile.
All service/controller reads and version-guarded writes bind to the request owner. Background planning/repair captures
that authority explicitly; expired/stale/duplicate completion cannot publish into another or newer workspace.
Repair autosave-failure bookkeeping is owner-local. Per-entry locking/transaction snapshots preserve atomic updates.

The configurable session cap defaults to 50; idle/absolute limits are 60 minutes/8 hours. Polling refreshes only idle.
Expiry erases the aggregate and cancels its current process. Old cookies after a service restart receive SESSION_ENDED,
with explicit restart/import guidance and retained browser versions. Restart can obtain CSRF without opening a
workspace; it changes the session ID and creates EMPTY only on explicit success. No failed mutation is replayed.

The demo host/origin gate requires approved HTTPS authorities and explicit proxy trust, rejects untrusted forwarded
scheme/host headers, and permits the Railway health host only on GET `/healthz`. Health creates no session. Existing
CSRF, CSP, response-type and no-store protections remain. Structured authoring and return-to-authoring routes return
404; DEFINITION_DRAFT cannot be written to the demo store. The persistent disclosure covers browser storage,
ephemeral workspaces, independent cache retention and Railway metadata retention.

The definition panel, packaged samples and exact browser-text persistence are necessary enabling work. They do not
approve UC-3/4/5. Trusted immutable Current can be exported during repair/manual staging without new demo kernel work;
archive round-trip/content/manifest integrity is still checked. Local non-demo export retains its verification.
That enabling change does not approve UC-10.

Evidence:

- [Technical ledger, changed-file list, source SHA-256 and retained checks](../../../output/execution/demo-deployment/UC-2/20261016-final/ledger.md).
- [Full passing local browser ledger](../../../output/playwright/demo-deployment/UC-2/1792140998412/ledger.md): packaged Boot JAR,
  real packaged kernel, local TLS, two isolated Chrome contexts plus a shared-owner tab; exact sample/export/import
  documents, private repair staging, foreign/random run probes, owner cancellation, active/stale/declined clear,
  confirmed private clear, cookie/security checks, browser text retention and service restart/explicit restart.
- 39 workspace unit tests in 10 classes passed. The full normal-profile WorkspaceImportIT had 25 tests: 23 passed;
  two failures were independently reproduced unchanged on the base commit. Exact IDs and reports are in the ledger.
- Screenshots of the 1280px and 390px flows, private staging/clear and session-ended states were inspected. At 390px
  the page has no horizontal overflow, the matrix scrolls internally and JSON is read-only. No separate Playwright
  skill is present in the supplied skill catalog or expected local path; the recorded Java Playwright journey uses
  the actual rendered controls and owns its isolated browser/server lifecycle.

Approval remains withheld because the complete public lifecycle and reliable provider boundary remain unproven.
The operator explicitly approved the SSH gateway fingerprint; the temporary SSH key was revoked and its local
files deleted. The fixed matching forwarded-host gate passed 8 focused tests. The public
[two-owner Chrome smoke](../../../output/playwright/demo-deployment/free-smoke/20261016-5/ledger.md) passed all
three exact samples, owner isolation, session cookie/HSTS, missing-CSRF/foreign-Origin refusal, two-workspace cap
and narrow inspection. This does not repeat the full local active-run, clear, expiry and restart journey publicly.
The subsequent explicit service restart cleared the disposable workspaces but exposed intermittent health 403s
(13/15 requests 200, 2/15 refused). The empirical exact proxy-peer list is insufficient after restart. See the
[deployed conclusion](../../../output/execution/demo-deployment/UC-12/railway-free-20261016-3/ledger.md).
No provider credential is in source/evidence, and UC-2 is not approved from the passing subset.

Outstanding destination work: sample-maintenance/full sample contract (UC-3), complete local-version behavior (UC-4),
custom use (UC-5), shared computation/expiry detachment and allowance controls (UC-6), and full repair/review/manual/
export/import journeys (UC-7–11). No destination is represented as complete from its entry point or shared storage.
