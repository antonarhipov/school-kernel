# UC-3 execution ledger

Date: 2026-10-16. Base `d55ab636558224f2d33a5e71313e840dd9486428`, dirty `demo` checkout preserved.
Verdict: **PASS / APPROVED UC-3**, automated execute approval after technical and public-browser verification.

The [checkpoint](../../../../../spec/demo-deployment/checkpoints/UC-3.md) contains the complete contract/evidence map,
findings, exact submission references, commands and outstanding destination handoffs. The seven-file production patch
and before/after hashes bind the UC-2 baseline; upload-context-manifest.json identifies all 149 uploaded inputs.
final-source-manifest.json retains the current checkout, including the preserved concurrent storage-control copy
change. submission.zip uses the approved staged production bytes plus this task's test/maintenance/documentation
boundary. approved-production-source-manifest.json identifies that exact production snapshot; the concurrent copy
patch is excluded from approval and deployment.

[Final verification](final-verification.json) checks all 149 approved production hashes, the six unchanged sample
documents, the separately preserved copy edit, relative evidence links and every entry in the
[submission ZIP](submission.zip). Its [SHA-256](submission.sha256) identifies the completed archive.

| Check | Result and retained evidence |
|---|---|
| Unit/build | 107 kernel + 52 workspace; zero failures/errors/skips. build-fix.log; unit-summary-final.json. |
| Scoring | VerifySampleScores evaluates each stored assignment with current packaged code. Complete scores, weights and revisions match; evidence contains exact recomputed scores. |
| Anonymization/package | Exact inverse/order reproduction, 51 distinct demo names, zero source-reference overlap. artifact-scan-final.json and image-artifact-scan-final.json inspect 23,656 entries each. No real examples in image. |
| Readiness faults | readiness-2/checks.json: old build, stale score and invalid pair exit 1 before readiness; stale-score maintenance refuses. Earlier extraction-helper failure retained under readiness/. |
| Container | Image build succeeds; container-smoke/ledger.md passes real sample verification, read-only/non-root operation, host/header refusal, SIGTERM and cleanup under smoke limits. No capacity qualification claimed. |
| Local sample UI | [Full PASS](../../../../playwright/demo-deployment/UC-3/local-20261016-6/ledger.md); rejected launcher after deployment verification proves zero visitor commands. |
| Related UC-2 | [Session regression PASS](../../../../playwright/demo-deployment/UC-2/uc3-regression-20261016-1/ledger.md), including real active-run sample refusal without mutation. |
| Public sample UI | [Full PASS](../../../../playwright/demo-deployment/UC-3/public-20261016-1/ledger.md), deployment ef120a58-f97c-4ed1-b03d-f71a4d662eef. Normal public TLS, two owners and a stale tab. |
| Process/events | deployed-startup.jsonl / deployed-after-browser.jsonl: three startup kernel events, six sample opens, zero visitor kernel processes. No solve/proposal-accept request in browser network evidence. |
| Edge | edge-probes.json: 73 stateless probes PASS; no new session. |
| Post-cleanup | cleanup-checks.json: same deployment restarted to remove disposable sessions; three fresh startup verifications, 40 consecutive public health/static 200s, no session creation. Final topology preflight PASS. |
| Layout | Public/local Full Week/details, Day/lenses, stale reload, manual entry and 390px screenshots visually inspected. Page overflow check passes. |
| Workspace | Starting and final status/manifest retained. No unrelated file changed by this task; the concurrent Delete all copy edit is preserved. No operator school/database, financial mutation or commit. |

The browser 503 is explicitly simulated before a server mutation, not a claimed real storage outage. Missing-confirmation
500 was an observed product finding, fixed and reverified as safe 428. The earlier failed/interrupted attempts remain.

All sample definitions/results and generation evidence retain their starting hashes. Only scoring evidence gained
recomputed scores/current catalog binding. Package scanning treats exact JSON/JVM string literals and raw full names as
identities; a generic one-word substring in unrelated third-party author metadata is not a source-school identity.

Scope ends at UC-3. Cache/allowance states belong to UC-6; their unimplemented lifecycle is not represented as live
coverage. The process-free sample path is independent of those services. Later full repair/manual/export/import UCs
and UC-12 launch qualification retain their own gates.
