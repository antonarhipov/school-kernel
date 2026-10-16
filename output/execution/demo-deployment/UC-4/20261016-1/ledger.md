# UC-4 execution ledger

Date: 2026-10-16. Verdict: **PASS / APPROVED UC-4**, automated execute approval after technical checks and a
complete public Chrome journey. Base `d55ab636558224f2d33a5e71313e840dd9486428`; pre-existing dirty checkout preserved.
No commit requested or made. Scope ends at UC-4; UC-5 is now eligible and remains NOT_STARTED.

The [checkpoint](../../../../../spec/demo-deployment/checkpoints/UC-4.md) provides the complete contract/limits map.
The [starting manifest](start-source-manifest.json), [final manifest](final-source-manifest.json),
[exact task production patch](uc4-production.patch), [delta from UC-3](production-delta-from-uc3.json),
[150-file production upload](upload-context-manifest.json), [full submission SHA-256](submission.sha256) and
[final verification](final-verification.json) bind the exact submission. Only eight intended source/document/test
paths changed during UC-4; all unrelated starting files retain their starting hashes. Production changes are three
files; other approved UC-3 production hashes remain exact. The existing Delete all storage label is preserved,
with the action now also retaining editor text. Earlier upload/archive bytes are retained with -1 suffixes.

Final public deployment: `f4be45da-754d-4e6c-a80d-7c004b47c1d0`, SUCCESS;
image `sha256:d430e303a5660f133a637b4247ba244e135d7081cea67538c2550974a9738aa0`.
URL: https://demo-smoke-production.up.railway.app. Same authorized isolated Free service and settings; no financial,
resource, account, operator data, memory, credential or external-message change.

| Evidence | Result |
|---|---|
| Java units/build | 107 kernel + 52 workspace; zero failures/errors/skips. build-1.log, unit-summary.json. Final package and test compilation pass. |
| Storage/identity | Five passing focused tests, including strict fields/origin, CRLF text, inclusive UTF-8 bytes, removal rollback, optional presence/array identity, manual publication, initial acceptance, imports, replacement and deletion. storage-tests-final.log. |
| Artifact/source | Final packaged sample/exclusion scan PASS, 23,657 entries. artifact-scan-final.json/log. All 33 packaged/deployed static hashes match pinned source. |
| Full local UC-4 | [14-step PASS](../../../../playwright/demo-deployment/UC-4/local-20261016-5/ledger.md), including exact CRLF/Unicode, storage faults, cross-tab handling and real repair/import tracking. |
| Full public UC-4 | [14-step PASS](../../../../playwright/demo-deployment/UC-4/public-20261016-1/ledger.md) on final production bytes; extra failed-deletion injection and exact observed workspace/envelope artifacts. Browser errors: zero. |
| Affected UC-3 | [Full sample regression PASS](../../../../playwright/demo-deployment/UC-3/uc4-regression-20261016-1/ledger.md). |
| Affected UC-2 | [Full fixed-runtime regression PASS](../../../../playwright/demo-deployment/UC-2/uc4-regression-20261016-2/ledger.md): actual private run/cancel, owner isolation, restart, retained browser text and failed/stale actions. |
| Public events | public-events.json / deployed-after-browser.jsonl: three startup verifications; one sample open; no browser label/filename/unused unfinished document. The later real repair/accept/import commands are separately visible. |
| Visual inspection | Public saved/failure/narrow/import-origin images and local contact sheet opened and inspected. 390px native readOnly and no-overflow assertions pass. |
| Cleanup | Same deployment restarted to remove disposable sessions; fresh startup verifies all three pairs. 40 consecutive session-free health/static HTTPS reads, nine edge/security probes and final one-service/replica/no-TCP preflight pass. cleanup-checks.json, edge-probes.json, boundary-final.json. |
| Source boundary | git diff --check, all intended/unrelated hashes, ZIP integrity and relative evidence links pass in final-verification.json. |

Failed local attempts remain: sandbox socket refusal; asynchronous file-dialog timing; native-function-return fault
reset; option disabled predicate. The first UC-2 regression was invalidated by overwriting a running JAR during a
rebuild. Its runtime now uses a fixed temporary copy; the full rerun passes. These failures are retained and are not
represented as passing product behavior. Production raw-text preservation explicitly handles textarea newline
normalization. The first public deployment is superseded by the final explicit imported-bundle origin label.

Browser quota/access/delete and file-read failures are explicit injected browser API faults, not claimed real
server storage outages. No standalone Playwright skill is installed; repository Java Playwright drives actual UI.
The saved-use tracking helper is implemented and technically tested; UC-5 must call it after successful real custom
installation and owns that complete actor journey. Existing one-minute repair/import behavior is used only as UC-4
tracking evidence; UC-6/7/8/11 and all UC-12 launch/capacity/spending gates retain their own status.

All contexts, local servers, runtime copies, temporary input/download files close or are removed. Public disposable
workspaces are removed by the verified restart; the isolated final deployment is retained. No operator database or
unrelated work was touched.
