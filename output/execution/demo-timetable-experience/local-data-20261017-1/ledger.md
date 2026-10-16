# Local school/timetable artifact cleanup

2026-10-17 (Europe/Tallinn), branch `demo`, base `d55ab636558224f2d33a5e71313e840dd9486428`.
User requested a Utilities cleanup button explicitly scoped to locally stored schools and timetables.
Pre-existing dirty work recorded in `/private/tmp/school-demo-clear-preexisting-status-20261017-1.txt` is preserved.
No commit requested. Existing Free deployment authorization continues; no plan/resource/variable/billing change.

UC-4 is APPROVED. Utilities explains that deletion affects only this browser/device's saved school definitions,
timetables and unfinished edits; bundled examples stay available and deletion cannot be undone. A modal repeats
the scope, initially focuses Cancel, supports Escape and requires explicit destructive confirmation.

The removal targets only `school-kernel.timetable-demo.v1`, historical `school-kernel.definition.v1.*` artifacts and
their `school-kernel.definition-use.v1` metadata. Raw values need not parse. Refused multi-key removal attempts
restore exact raw prior values; restoration failure is reported as possibly partial. Unrelated preferences/storage,
cookies, bundled sample files and other browser owners are untouched. Successful deletion blocks persistence from
pending callbacks, clears local review timers, detaches generation results and reloads the clean default. Stale-tab
writes retain the existing compare-and-swap guard. There is no server mutation request for cleanup.

Production changes are confined to three browser assets: `demo-workbench.js`, `demo-workbench.css` and
`timetable-storage.js`. Matrix rules/markup, all sample artifacts, kernel and server source remain exact.
Technical checks and real packaged/public Chrome evidence are linked below.

The upload preflight found unrelated concurrent backend edits relative to the last deployment. No current backend
file is copied into this release. The production-only snapshot is reconstructed from the prior Imperial release's
hashed archive and overlaid with exactly the three cleanup assets. [Excluded concurrent files](excluded-concurrent-changes.json),
[three-file delta](production-delta.json), [manifest](upload-manifest.json), [preflight](payload-preflight.json).
The shared-checkout browser journeys passed, but release verification additionally uses an isolated tree with these
exact production bytes. The first isolated build compiled an unrelated newly added hardening test against the old
production APIs and failed; [log retained](isolated-browser.log). Verification now includes only the complete test
dependency closure (DemoLocalDataBrowserIT, DemoTimetableBrowserIT, DemoPlanningBrowserIT); no production source,
user test or workspace artifact was changed to resolve this harness boundary.

A second isolated attempt had not packaged the separate kernel executable; its [log](isolated-browser-final.log)
is retained. Once the complete reactor was packaged, isolated attempts 3/4 exposed a genuine payload-isolation
error before deployment: a concurrent autosave change touched the same workbench file and imported a new file
outside the prior allowlist. Nothing was deployed. The release workbench is now reconstructed from the prior
immutable archive plus only the cleanup edits: [exact patch](cleanup-only-workbench.patch). The shared autosave
implementation is preserved, including its cancellation hook, but excluded from this cleanup-only release.
[All static import references](static-imports.json) resolve and a Node import of the staged workbench passes.
The corrected complete isolated journey passed as attempt 5; earlier logs/ledgers remain intact.

## Verified release

Thirteen Node storage checks pass ([log](storage-tests.log)), including current/legacy/corrupt removal, exact
rollback, explicit partial-restoration failure, unrelated-key preservation and stale-tab refusal. Eight unchanged
sample checks pass ([XML](sample-tests.xml)). Shared-checkout navigation and cleanup journeys pass
([navigation XML](navigation-tests.xml), [initial cleanup XML](initial-cleanup-tests.xml)); their concurrent
backend bytes are not represented as the release artifact.

The exact reconstructed production payload passes the complete
[isolated UC-4 Chrome journey](../../../playwright/demo-timetable-experience/UC-4/isolated-20261017-5/ledger.md)
([XML](isolated-cleanup-tests.xml), [log](isolated-browser-5.log)). All 174 production source hashes, stage hashes,
isolated source hashes and packaged resources agree. [Final package/privacy scan](packaged-provenance-final.json)
passes; [source/test/JAR hashes](verification-source-sha256.json) pin verification inputs.
The [default matrix screenshot](matrix-presentation-comparison.json) is byte-for-byte identical to the previous
public release. No matrix markup or shared matrix CSS changed.

Deployment `8891ef46-d92d-4e51-8efc-66690e81e6de` is SUCCESS at
[the existing demo](https://demo-smoke-production.up.railway.app/workspace/), image
`sha256:4bb31c1080d399eea731cff79468ae80d02184d34c3dffa5360900dacbc3102c`.
[Upload](deployment-upload.log), [build](build-final.jsonl), [provider ready](deployment-ready.json).
The source archive SHA-256 is `e20f7b4d1bba14bfbe09ccfc9ad8280f34ae3d96216249908c3e1fcee4f58fde`.
[Provider comparison](provider-comparison.json) proves the Free plan, complete service manifests, effective runtime
limits, process bounds and kernel remain unchanged. [Startup](startup-ready.json) verifies all bundled schools.
The complete [public Chrome journey](../../../playwright/demo-timetable-experience/public/local-data-20261017-1/ledger.md)
passes ([XML](public-cleanup-tests.xml)). It creates real saved school/timetable/draft/editor work; proves Cancel,
Escape and exact rollback after an injected removal refusal; confirms scoped deletion without a server mutation;
checks stale-tab refusal, all bundled alternatives, corrupt-storage recovery and saving new work afterward.
Desktop, 390px confirmation, failure and clean-default screenshots were inspected.

An actual same-image restart clears the disposable test server sessions. [Fresh startup](restart-startup-ready.json)
has a new counter epoch with the identical service artifact, kernel, effective settings and four VERIFIED schools.
The final [aggregate/cleanup record](cleanup-final.json) shows zero active workspaces, running solve/verification
processes, waiting owners and cache entries. Disposable browser contexts are closed; operator storage is untouched.
[Final public checks](public-after-cleanup/cleanup-public-checks.json) pass 40 stateless health/static reads,
nine edge probes, all 40 exact static hashes and eight exact timetable pairs without creating cookies or sessions.
[Final ingress check](boundary-final.json) passes. [Final integrity](final-source-integrity.json) confirms all 174
production files agree across archive/stage/isolated source, and 59 packaged resources and nine pinned verification
inputs still agree. [Verification summary](verification-summary.json) records PASS. Concurrent shared-checkout
changes remain preserved and excluded from this cleanup-only deployment. No commit or handoff remains.
