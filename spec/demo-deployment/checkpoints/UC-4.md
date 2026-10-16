# UC-4 checkpoint

Status: **APPROVED**, 2026-10-16. Scope: UC-4 only. Requires approved UC-2; its real panel/private-session boundary
was consumed by two disposable owners and a same-owner tab. UC-5 is now eligible and remains NOT_STARTED.
Base: `d55ab636558224f2d33a5e71313e840dd9486428`, dirty `demo` checkout preserved. No commit requested or made.

## Submission

[Execution ledger](../../../output/execution/demo-deployment/UC-4/20261016-1/ledger.md),
[starting source manifest](../../../output/execution/demo-deployment/UC-4/20261016-1/start-source-manifest.json),
[final source manifest](../../../output/execution/demo-deployment/UC-4/20261016-1/final-source-manifest.json),
[exact production patch](../../../output/execution/demo-deployment/UC-4/20261016-1/uc4-production.patch),
[three-file delta from UC-3](../../../output/execution/demo-deployment/UC-4/20261016-1/production-delta-from-uc3.json),
and [150-file production-only upload manifest](../../../output/execution/demo-deployment/UC-4/20261016-1/upload-context-manifest.json)
bind the submission. Only `demo-panel.js`, new `demo-definitions.js`, and the successful demo sample-use callback's
returned snapshot in `app.js` change production. All other production inputs retain approved UC-3 hashes.

The pre-existing Delete all browser-storage label and workspace/sample preservation intent are retained. Confirmed
deletion now also retains editor text, as its tooltip/confirmation explain. Test changes add the complete UC-4 journey
and copy UC-2's running JAR once into its temporary directory so an unrelated rebuild cannot invalidate runtime proof.
A focused Node test covers storage validation, byte limits, removal rollback and complete in-use identity.
The [full source submission](../../../output/execution/demo-deployment/UC-4/20261016-1/submission.sha256) and
[final verification](../../../output/execution/demo-deployment/UC-4/20261016-1/final-verification.json) retain source and
boundary checks. No unrelated starting file changed.

Public URL: https://demo-smoke-production.up.railway.app.
Final deployment: `f4be45da-754d-4e6c-a80d-7c004b47c1d0`, SUCCESS.
Image: `sha256:d430e303a5660f133a637b4247ba244e135d7081cea67538c2550974a9738aa0`.
The first deployment `9c261364-3505-4214-8025-b39f51691c61` is superseded only by the explicit imported-bundle origin
label; its source manifest/archive remain retained with `-1` suffixes. The final full public journey uses final bytes.
This remains the authorized isolated Free smoke service; no provider settings or financial controls were changed.

## Contract coverage

The complete [public Chrome ledger](../../../output/playwright/demo-deployment/UC-4/public-20261016-1/ledger.md)
records 14 steps with action, expectation, observed values, screenshots/snapshots and PASS. The supporting
[local full journey](../../../output/playwright/demo-deployment/UC-4/local-20261016-5/ledger.md) also passes.

| Contract | Implemented and observed |
|---|---|
| Main 1-3 | Sample/saved/file text in monospace editor; whitespace-sensitive changed status; first syntax-error line/column without kernel work. Raw opened/stored text is separate from textarea newline normalization. |
| Main 4-5 | Named Save as, strict/reserved IDs, explicit named overwrite, exactly five envelope fields, precise file/sample/saved provenance, explicit saved time, newest-first list and real cross-tab refresh. |
| Main 6; G3/G4 | Exact CRLF/Unicode reload/download/revert; malformed text can download; confirmed single/all deletion keeps samples, Current, unrelated preferences and open text. |
| 1a; G2 | Corrupt/unsupported envelopes, extra/invalid fields, bad provenance, size/object/syntax faults and reserved keys remain unreadable. Disabled native options cannot execute; named deletion remains available. |
| 1b/4b/6a | Declined selection, file opening, revert, replacement, single/all deletion preserve exact stored and working values. |
| 1c/4a | File/editor UTF-8 2 MiB guard is independent; invalid syntax, non-object roots, read failure and invalid labels refuse without replacement. Inclusive byte boundary has focused technical proof. |
| 5a/minimal | Atomic Storage.setItem refusal retains the old complete entry and exact editor text, with download/delete guidance. Public browser quota/access/delete faults preserve data; multi-key deletion rollback has focused proof. |
| G1 | Syntax-valid kernel-invalid unfinished object saves. No unused definition or browser label is submitted. The local-storage phase starts no visitor kernel work; later real repair/import tracking is explicitly separate. |
| G5 | Persistent disclosure covers origin/device/browser retention, shared-browser exposure, site-data clearing, eviction/private closure, download durability and no sync. Existing UC-2 real restart regression preserves exact browser text. |
| G6 | Actual loaded sample survives selection/reload; saving an identical copy never marks it used. Real repair acceptance changes definition and clears every mark. Exact re-import shows imported-bundle origin with no dropdown mark. Saved-use identity binds the complete definition and exact envelope; replacement/deletion invalidation is tested at the production-module boundary. |
| G7 | Real public Chrome covers exact-text reload, named replacement, reserved IDs, unfinished work, storage/file faults, corrupt entries, declined actions, deletion and actual cross-tab replacement/removal without overwriting open text. |
| Success/minimal | Complete exact named envelope persists; declined/failed actions retain previous storage/editor/Current and the second owner. Narrow 390px inspection is read-only with no document overflow. |

## Passing checks

- Build/unit: **107 kernel + 52 workspace tests**, zero failures/errors/skips; `build-1.log` and `unit-summary.json`.
  Final packaging/test compilation passes. No backend/kernel production code changed in UC-4.
- `node --test tools/demo/definition-storage.test.mjs`: **5 passing tests**; `storage-tests-final.log`.
- `python3 tools/demo/check_packaged_samples.py --output ... kernel-cli/target/school-kernel.jar timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar`:
  PASS against final artifacts, **23,657 entries**, exact fictional sample/inverse/order checks and source exclusions;
  `artifact-scan-final.json` / `artifact-scan-final.log`.
- Packaged and deployed static resources match all **33 pinned source hashes**;
  `local-static-hashes.json` / `deployed-static-hashes.json`.
- Full local UC-4 and full final public UC-4 journeys: PASS. Public browser errors: zero. The public ledger retains
  complete original/repaired/imported workspace and saved-envelope values, not only counts/revisions.
- Affected [UC-3 sample regression](../../../output/playwright/demo-deployment/UC-3/uc4-regression-20261016-1/ledger.md)
  and [UC-2 fixed-runtime session regression](../../../output/playwright/demo-deployment/UC-2/uc4-regression-20261016-2/ledger.md):
  PASS, including real run/cancel, owner isolation, stale/failed actions, two process starts and retained browser text.
- [Safe public event evidence](../../../output/execution/demo-deployment/UC-4/20261016-1/public-events.json): only three
  startup verifications precede the sample/local-storage phase; the later real repair/accept/import commands are
  separately visible. No browser label, filename or unused unfinished document appears in application logs.
- Nine deployed stateless edge/security probes pass. Cleanup restarts the same image, re-verifies all three pairs,
  removes disposable sessions, and passes 40 consecutive session-free health/static HTTPS reads.
  [Final topology](../../../output/execution/demo-deployment/UC-4/20261016-1/boundary-final.json): one service/replica,
  generated domain, no TCP proxy, same final deployment/image, SUCCESS.
- `git diff --check`, exact intended-change boundary, source/archive and relative evidence-link checks: PASS.

## Findings, limits and cleanup

Failed attempts are retained. Attempt 1 hit sandbox socket denial; attempt 2 had asynchronous file-dialog timing;
attempt 3 returned a native function from a Playwright fault-reset expression; attempt 4 used Playwright's disabled
predicate on an option rather than asserting its native disabled property. These harness corrections preserve the
production guarantees. Exact CRLF retention, syntax feedback and strict storage handling were implemented before
the passing complete journeys. The first UC-2 regression was invalidated by a simultaneous package replacing the
running JAR, producing a lazy class-load failure. The harness now copies its runtime artifact once; its complete rerun
passes. No failed attempt was erased or called a product PASS.

Quota, Storage access/delete and file-read failures are explicitly injected browser API faults, not a claimed server
storage outage. Public screenshots were opened and visually inspected for readable saved/failure/origin states and
390px overflow. There is no standalone installed Playwright skill; Java Playwright drives actual rendered controls
and records the administrator journey. Execute supplies automated approval; no human/independent review is claimed.

UC-5 owns Use this definition, server validation/replacement, and the complete saved-version-to-initial-planning
journey. It must call `rememberDefinitionUse` only after successful custom installation, passing the actual returned
snapshot. Its saved-use branch already validates complete loaded definition and exact saved-envelope identity;
module tests cover reload identity, replacement/deletion, array/optional-value differences, initial acceptance and
import/repair invalidation. This enabling work does not approve UC-5. The real tracking supplement uses existing
one-minute repair/import behavior solely to prove UC-4 G6; it does not approve UC-6 effort/admission/accounting or
full UC-7/8/11 contracts. UC-12 launch/capacity/spending blockers remain unchanged.

All browser contexts close, temporary inputs/downloads and local runtime copies are removed, and local processes
stop. Public disposable workspaces are removed by the verified cleanup restart. The isolated deployment remains
at its original Free smoke settings; no operator database/data, financial setting, memory or Git commit was changed.
