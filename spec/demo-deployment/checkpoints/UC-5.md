# UC-5 checkpoint

Status: **APPROVED**. Implementation, local checks, public Chrome journey and same-image cleanup complete, 2026-10-16.
Requires approved UC-4; real saved versions, panel, use identity hook and private owner boundary were consumed.
Base `d55ab636558224f2d33a5e71313e840dd9486428`, preserved dirty `demo` checkout. No commit requested or made.

[Execution ledger](../../../output/execution/demo-deployment/UC-5/20261016-1/ledger.md),
[starting manifest](../../../output/execution/demo-deployment/UC-5/20261016-1/start-source-manifest.json),
[exact production patch](../../../output/execution/demo-deployment/UC-5/20261016-1/uc5-production.patch),
[154-file production manifest](../../../output/execution/demo-deployment/UC-5/20261016-1/upload-context-manifest.json),
[production archive hash](../../../output/execution/demo-deployment/UC-5/20261016-1/production-submission.sha256),
and [source delta](../../../output/execution/demo-deployment/UC-5/20261016-1/source-delta.json) bind the submission.
Only eleven production files differ from approved UC-4. Four new demo classes implement limits, initial-definition
installation, bounded verification/reuse and daily reservations. Existing changes cover the owner/route gate,
legacy definition-only bypass refusal, process cleanup, demo settings and panel/application controls. Test changes
add six focused cases, two complete UC-5 Chrome journeys, and migrate UC-2's definition-only setup through the saved-use
UI. Documentation records the observed local and public approval evidence. All unrelated starting files retain their hashes.

## Contract and local evidence

The [complete final local journey](../../../output/playwright/demo-deployment/UC-5/local-final-20261016-1-journey/ledger.md)
and [real process/configuration fault journey](../../../output/playwright/demo-deployment/UC-5/local-final-20261016-1-faults/ledger.md)
PASS against the final packaged submission. Real installed Chrome uses disposable TLS, private fixed runtime JARs and
isolated owners. Screenshots were opened and inspected for the initial draft, busy refusal and 390px read-only panel;
final temporary-storage wording replaces the earlier observed normal-profile durable-storage sentence.

| Contract | Observed coverage |
|---|---|
| Main 1; 1a | Full edited with real keystrokes, exact CRLF/Unicode file opened/saved/reloaded; unsaved use disabled. Active-run refusal and cancel/wait guidance pass in the complete affected UC-2 journey. Stale/corrupt browser entries retain UC-4 protections. |
| Main 2; 2a | Named work to discard and accepted-export guidance; declined replacement preserves complete Current, editor and saved version. Direct unconfirmed replacement is refused. |
| Main 3; G1/G3 | Raw stored text submitted unchanged, label/filename never sent; strict UTF-8/object/duplicate/trailing/schema checks, separate 2 MiB limit, independent inclusive W/lesson gates, overflow-safe arithmetic, malformed collections rejected before process work. Array order, supported fields and explicit-empty versus omitted optional data survive storage by value. |
| 3a/3b; G2 | Real kernel rejects located reference errors; unsupported/null/schema fields, initial repair ancestry, oversized transport and excessive lesson count leave complete accepted state and saved text intact. No seeded repair or accepted timetable can be created through definition-only use. |
| 3c/3d; G4 | Two deliberately stalled real verifier processes saturate the two slots; third UI use returns busy within configured wait. Real watchdog and process exit fail safely. Real insufficient-reservation configuration returns daily refusal while sample use remains available. Reservation tests prove actual elapsed process consumption, pre-start/failure release, overlapping held capacity, UTC rollover and a trusted hit at exhausted allowance. |
| Main 4; 4a; G5 | Actual packaged successful verification plus CAS installs the exact complete definition only as INITIAL_DRAFT. Real same-owner stale tab and mutation during validation preserve the newer complete baseline; ended-session technical path cannot install. |
| Main 5; success | No Current/result/proposal/run; real summary, lesson-based Quick/Thorough default metadata and initial-planning entry. Separately saved sample-derived definition has only recognized safe sample origin; file/custom origins exclude labels. |
| G4 | Complete canonical documents/mode/kernel-build identity, array/optional/result differences, disabled reuse, fixed TTL/byte ceiling, proactive expiry and success-only caching. Actual local main journey launches six verifier commands total: three startup, one invalid reference and two distinct successful custom definitions; repeated identical use launches none. No solve starts. |
| G6/minimal | Two private real browsers plus same-owner tab, full invalid/oversized/busy/daily/stale preservation, page reload and saved In use identity, narrow read-only presentation and no overflow. The public HTTPS journey also passes with exact snapshots and owner isolation; see the public evidence below. |

## Passing checks

- `./mvnw -q -pl timetable-workspace -am verify -Dit.test=DemoCustomDefinitionBrowserIT,DemoSessionBrowserIT,DemoSamplesBrowserIT,DemoDefinitionsBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false ...`: **107 kernel + 58 workspace unit tests; five selected Chrome tests**, zero failures/errors/skips; `final-regression-1.log`, `unit-summary.json`, `browser-summary.json`.
- Full affected [UC-2 session/restart](../../../output/playwright/demo-deployment/UC-2/uc5-regression-20261016-1/ledger.md),
  [UC-3 sample](../../../output/playwright/demo-deployment/UC-3/uc5-regression-20261016-1/ledger.md), and
  [UC-4 storage/repair/import tracking](../../../output/playwright/demo-deployment/UC-4/uc5-regression-20261016-1/ledger.md)
  regressions: PASS. Existing normal-profile unit behavior passes; previously recorded baseline HTTP failures are
  neither rerun nor claimed fixed here. Actual kernel verifier success/timeout/termination is exercised afresh.
- `node --test tools/demo/definition-storage.test.mjs`: five PASS.
- Packaged sample/exclusion scan: PASS, **23,664 entries**. All **33** packaged static resources match pinned source.
- Authorized deployment and final topology: PASS, one service/replica, generated domain, no TCP proxy. The same
  UC-5 deployment/image remains healthy after cleanup; its service manifest matches UC-4 without settings changes.
- Exact intended source boundary, archive/source fingerprints and `git diff --check`: PASS. No unrelated changes
  are folded into a commit. Test contexts/processes close; invalidated run-2 temporary runtime data is removed.

## Public deployment and verification

The user explicitly authorized deployment and verification after the automatic reviewer rejected the first attempt.
The [historical review record](../../../output/execution/demo-deployment/UC-5/20261016-1/deployment-approval-block.json)
is retained; [authorized resumption](../../../output/execution/demo-deployment/UC-5/20261016-1/authorized-resumption.json)
records the resolution and successful source/stage/archive drift checks. No approval blocker remains for UC-5.

Deployed to https://demo-smoke-production.up.railway.app on the existing isolated Free smoke service:
`8bc49107-ee22-40f3-9e13-1ee2d5eb6439`, image
`sha256:77a5d8d450a9d0498234b2f147cffb3dd14a2c82f71480d011147e96ff80cd09`.
Only the pinned 154-file production stage was uploaded; archive SHA-256
`7910c5df9c866cd5c24434bcd90393f3c47572ad4dd51c4164892d54846c0e01`.
The deployment service manifest is unchanged from UC-4. No plan, spending, resource or other provider configuration changed.

The complete [public real-Chrome journey](../../../output/playwright/demo-deployment/UC-5/public-20261016-1-journey/ledger.md)
PASS uses two private disposable owners and a same-owner stale tab, normal public TLS validation and the actual
rendered controls. The selected remote method passes without failures/errors/skips, alongside ten focused unit tests;
[reports](../../../output/execution/demo-deployment/UC-5/20261016-1/public-test-summary.json) are retained separately
from the final local regression reports. Public evidence proves exact CRLF/Unicode Full text, field/order/optional
fidelity, invalid/schema/ancestry/complexity/oversized/stale refusal with exact preservation, initial draft and
saved In-use reload, safe origins, Quick/Thorough defaults, no Current/run/proposal and second-owner isolation.
The initial-draft, located-refusal and 390px screenshots were opened and inspected: controls remain readable,
validation appears beside preserved Current, temporary-storage wording is correct and the narrow panel has no
page overflow. Fault injection is confined to the unchanged passing local journey.

[Application process evidence](../../../output/execution/demo-deployment/UC-5/20261016-1/public-process-evidence.json)
records exactly six verifier launches: three startup samples, one invalid visitor definition and two distinct
successful custom definitions. Repeated Full use launches none; no plan/replan is started. Browser labels,
filenames and definition payloads are absent from application logs. Provider HTTP logs were not fetched.
All 33 public static resources match the pinned source and packaged JAR before and after cleanup.

An authorized restart cleared disposable visitors/caches on the **same deployment and image**. Fresh startup
verified all three packaged samples. Final [cleanup checks](../../../output/execution/demo-deployment/UC-5/20261016-1/cleanup-checks.json),
[40 health/static reads and nine edge probes](../../../output/execution/demo-deployment/UC-5/20261016-1/cleanup-public-checks.json),
and [topology](../../../output/execution/demo-deployment/UC-5/20261016-1/boundary-final.json) PASS without creating a
new session. The first cleanup probe had a case-sensitive Location lookup; the retained diagnostic confirms the
correct 301/HTTPS target. Header names are now normalized, and the complete final check passes without a production change.

UC-5 is approved from observed automated evidence. No additional human walkthrough or separate convergence gate applies.

## Limits and destination handoffs

No installed standalone Playwright skill exists; Java Playwright supplies the real rendered-control walkthrough and
durable ledgers under execute. Failed sandbox/compiler/browser attempts remain retained and are described in the
execution ledger; overlapping run 2 is explicitly invalidated. Final local attempts pass on the pinned build.

UC-6 owns actual selectable effort, solve queue/admission/accounting/coalescing and generated proposals. Its existing
legacy planning entry is only a destination handoff here; showing correct default metadata does not approve UC-6.
Other visitor operations still need integration with the shared verification/resource controls in their own UCs.
UC-12 deployment/capacity/spending launch blockers remain separate. No global feature approval, operator database
change, memory update or Git commit is claimed. The public smoke service now runs the verified UC-5 image identified above; Free smoke remains distinct from UC-12 public-launch qualification.
