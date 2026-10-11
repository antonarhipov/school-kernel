# Use-Case Status: school-definition-authoring

## Current

- Use case: UC-10
- Status: PENDING_WALKTHROUGH
- Next eligible: UC-11 after UC-10 walkthrough

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `d4a66f6` / [checkpoint](checkpoints/UC-1.md) | [convergence](convergence/UC-1.md) |
| UC-2 | APPROVED | UC-1 | `9b59583` / [checkpoint](checkpoints/UC-2.md) | [convergence](convergence/UC-2.md) |
| UC-3 | APPROVED | UC-1 | `a30de35` / [checkpoint](checkpoints/UC-3.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-3/20261011-2/README.md) |
| UC-4 | APPROVED | UC-1 | `dd395c4` / [checkpoint](checkpoints/UC-4.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-4/20261011-2/README.md) |
| UC-5 | APPROVED | UC-1 | `1084c81` / [checkpoint](checkpoints/UC-5.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-5/20261011-1/README.md) |
| UC-6 | APPROVED | UC-1 | `6ca5e84` / [checkpoint](checkpoints/UC-6.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-6/20261011-1/README.md) |
| UC-7 | APPROVED | UC-1 | `6622f19` / [checkpoint](checkpoints/UC-7.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-7/20261011-1/README.md) |
| UC-8 | APPROVED | UC-1 | `42f3b53` / [checkpoint](checkpoints/UC-8.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-8/20261011-1/README.md) |
| UC-9 | APPROVED | UC-1 | `8109b2b` / [checkpoint](checkpoints/UC-9.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-9/20261011-2/README.md) |
| UC-10 | PENDING_WALKTHROUGH | UC-1 | [checkpoint](checkpoints/UC-10.md) | pending |
| UC-11 | NOT_STARTED | UC-1 | - | - |
| UC-12 | NOT_STARTED | UC-1 | - | - |
| UC-13 | NOT_STARTED | UC-1 | - | - |
| UC-14 | NOT_STARTED | UC-1 | - | - |
| UC-15 | NOT_STARTED | UC-1 | - | - |
| UC-16 | NOT_STARTED | UC-1, UC-15 | - | - |
| UC-17 | NOT_STARTED | UC-16 | - | - |
| UC-18 | NOT_STARTED | UC-16 | - | - |
| UC-19 | NOT_STARTED | UC-1 | - | - |
| UC-20 | NOT_STARTED | none | - | - |

## Technical decision pass

- Sources: spec.md (all 20 UCs, lifecycle, normative data and exclusions), existing local security, versioned PostgreSQL aggregate, bundled definition schema, execute/converge skills. No feature rules.md exists.
- Relationship targets exist; the dependency graph is acyclic. UC-1 is the first eligible root; UC-20 is an independent later root.
- Preserve JSON trees and optional presence; atomically open with the existing workspace version/state compare-and-swap. Source and accepted provenance are copied exactly. Reuse bundled schema validation before copying existing sources. Accepted-source upgrade follows the exact preservation table; no kernel model or outbound service is introduced.
- Retain current local Host/Origin/CSRF protections for new routes; refuse competing mutations at the server boundary and protect reset/clear atomically.
- UC-1 supplies freely navigable saved-content sections. Editing and finish actions belong to dependent UCs and are not exposed yet.
- Verification respects the user's instruction: cheap checks during construction; targeted HTTP/browser and shared-surface regression checks at the completed slice. No repeated slow full-suite runs for incremental checks.

## UC-1 Evidence

- Started: 2026-10-11
- Started from: 0fd30998e2b20e79ed61392a2125c73067d2c76e
- Pre-existing dirty files: none
- Prior notes described a removed partial implementation; live checkout has only persisted-draft read compatibility, not a prior approved submission.

- Implementation submission: `06c0201f24b127f75efeec89f145966aa60b10ea`
- Completed focused checks: 16 unit/architecture + 10 browser tests, zero failures/errors/skips; accepted Current regression 11/11 passed separately. See checkpoint for exact commands and contract coverage.
- Changed files and technical evidence: [UC-1 checkpoint](checkpoints/UC-1.md).
- Test impact: no tracked runtime data or unrelated files changed.

## Blockers

None. Prior UC-3 procedural blocker resolved by the user-authorized workflow update below.

## Deviations

- Test cadence: user requested slow tests only when necessary; consolidate runtime checks at the slice boundary.

## UC-1 Convergence Evidence

- Independent committed-submission audit: all 26 workspace unit tests and 17 selected integration/browser tests passed, zero failures/errors/skips. Shared accepted Current regression previously passed 11/11. Exact command and coverage: [convergence report](convergence/UC-1.md).
- Repository clean before/after verification; no runtime school data changed.
- Initial verdict: PENDING_WALKTHROUGH; no implementation findings. Superseded by the administrator-confirmed approval below.
- Initial walkthrough result: not yet received at the original convergence; confirmation has since been received (below). Isolated workspace: http://localhost:18081/workspace/; exact script and environment lifecycle recorded in convergence report.
- Full unrelated reactor/browser/solver suites were not rerun or claimed green; user requested slow checks only when necessary.

## UC-1 Navigation Revision

- User decision: walkthrough accepted ("All good."); replace Resources with dedicated Teachers and Rooms tabs and approve UC-1 after the split.
- Updated UC-1 main step 5 to reflect the approved navigation decision. Classes and Room policies also have dedicated tabs so all existing sections remain directly reachable without the Resources grouping.
- Scope: navigation labels/grouping only; source data, persistence, lifecycle, and editing scope are unchanged.
- Verification: focused existing real-browser navigation/viewport/reload journey; prior complete UC-1 evidence remains applicable to unchanged behavior. No redundant broad slow suite run.
- Focused revision validation: 4 architecture checks and 1 real-browser navigation/responsive/reload journey passed, zero failures/errors/skips; JavaScript syntax and diff hygiene passed.
- Revised submission: HEAD at convergence. UC-1 is ready to record the already-received administrator confirmation and approve.

## UC-1 Approval

- Administrator confirmation: "All good." Requested dedicated Teachers/Rooms tabs instead of Resources and explicitly instructed approval after the split.
- Revision submission: `d4a66f68fb8dbff44a3e34998ebe8ae850b06d51`.
- Convergence verdict: APPROVED; zero blocking findings. Spec and implementation now reflect the requested navigation; focused checks passed. Prior source/lifecycle/security/regression evidence remains applicable to unchanged behavior.
- Isolated walkthrough refreshed at http://localhost:18081/workspace/ with the new tabs; complete saved workspace checksum unchanged across restart.
- No additional approval requested; no dependent UC started in this turn.

## UC-2 Evidence

- Started: 2026-10-11; base commit `80146e7f00d5f51500ca0e48a71de9ade477651d`; pre-existing dirty files: none.
- Dependency: UC-1 is APPROVED at its committed convergence report.
- Technical decision pass: use the existing version/state CAS and aggregate transaction; update only schoolId/displayName, draft revision and school identity diagnostics. Existing or first-saved identity is immutable. Preserve source, Current, optional presence and all editor context. New route uses existing local Host/Origin/CSRF protections. Explicit form save; working inputs live only in browser memory and survive responsive rendering. No new feature rules or architecture deviations are needed. Sources: spec UC-2, School fields, Identifier rules, form/lifecycle models; UC-1 approved boundary; existing repository and security configuration.

- Implementation submission: HEAD at convergence; [UC-2 checkpoint](checkpoints/UC-2.md) records actual changed files, contract evidence and exact commands.
- Completed slice: 26 unit/architecture + 18 integration/browser tests passed, zero failures/errors/skips. All seven UC-1 journeys and three saved-draft compatibility journeys passed; two local security/route regressions passed.
- JavaScript syntax and diff hygiene passed. No tracked runtime data or unrelated changes from verification.
- Runnable isolated walkthrough: http://localhost:18082/workspace/, disposable UC-2 database on 24482; server session 85398. Operator school untouched. Administrator confirmation still required for this new UI goal.

## UC-2 Initial Convergence

- Submission `60ff34c`; all 44 checks rerun successfully, but standalone Chrome audit found C-1: long saved name gives 2402px page width at 1280px viewport.
- Verdict NEEDS_REVISION. Fix heading wrapping and add the saved-long-name browser regression; no later UC selected.

## UC-2 C-1 Revision

- Started from `8153851`; prior finding C-1; pre-existing dirty files none.
- Added wrapping for unrestricted school headings and a saved-140-character-name real-browser regression at 1600x900, 1280x800, 390x844 and 700x844. Stored name/ID and complete document remain exact.

- Revision validation: 26 unit/architecture + 19 integration/browser tests passed, zero failures/errors/skips; `/tmp/uc2-revision-verify.log`. Standalone real Chrome repeats C-1 with no overflow (1280/1280, 390/390) and narrow read-only inspection.
- Revised submission: HEAD at convergence; only heading CSS, the new browser regression, status and checkpoint changed after 8153851. No tracked runtime data changed.
- Isolated walkthrough refreshed to corrected packaged server session 73332, `/tmp/uc2-walkthrough-final.log`, same localhost:18082 and disposable PostgreSQL 24482. Conditional cleanup removed only the agent-owned long-name variant; starts EMPTY, version 0.

## UC-2 Reconvergence

- Revised immutable submission `9b595839745ef96578aadd9a70bcebbd4ee8a550` audited. Fresh rerun: 26 unit/architecture + 19 integration/browser tests, zero failures/errors/skips, BUILD SUCCESS; `/tmp/uc2-reconvergence.log`. Repository clean before/after.
- C-1 resolved by wrapping and the saved-long-name real-browser regression. Zero open implementation/protocol findings.
- Verdict: PENDING_WALKTHROUGH. Administrator confirmation of UC-2 has not been received; UC-1 remains approved and later UCs remain untouched.
- [Walkthrough and complete evidence](convergence/UC-2.md) at http://localhost:18082/workspace/; corrected isolated server session 73332; fresh EMPTY workspace.

## UC-2 Automated Walkthrough

- User requested the agent perform the walkthrough using Playwright. Completed all six declared steps in real headed Chrome on 2026-10-11: PASS.
- Verified creation/invalid ID/advanced override, rename/reload, Stay/Discard/Save and continue, blank-name blocker/correction, responsive read-only inspection and retained edits, and real two-tab stale-save review/retry. Final reload restores version 8, `Working resize name`, stable ID `Demo.ID_9`, empty source/issues and no Current.
- [Durable walkthrough evidence and screenshots](../../output/playwright/uc2-walkthrough/README.md). Only the disposable UC-2 database changed. Browser closed; application remains available at localhost:18082.
- Requested automated execution is complete. No human approval is inferred, so PENDING_WALKTHROUGH remains under spec UC-1 G7 and converge; no repeat walkthrough requested and no later UC started. Existing 45 passing checks remain applicable; no code changed.

## UC-2 Approval and Walkthrough Policy

- Administrator confirmation (2026-10-11): “ok. it seems that the automated walkthrough worked fine. let's approve the UC”. UC-2 is APPROVED, with the non-blocking inherited stale-save wording note K-1 recorded in convergence.
- Evidence: immutable implementation `9b59583`, complete passing convergence (26 unit/architecture + 19 integration/browser checks), and six-step real Playwright walkthrough committed at `9c14c5c`. No changed code or unresolved behavioral evidence requires another test run.
- User authorized automatic Playwright walkthroughs after checkpoint and technical convergence to support the autonomous implementation loop. Updated spec UC-1 G7 and all dependent walkthrough references: passing agent-run real-browser and durable-state evidence now permits automatic approval; isolation, failure checks and exact fidelity requirements remain intact. This supersedes the prior confirmation policy, without changing actor-visible behavior or invalidating existing UC-1/UC-2 evidence.
- Updated spec/rules/execute/converge skills to retain one-UC immutable checkpoints, commit technical convergence before the walkthrough, immediately run Playwright, record and commit final approval, and continue within the authorized scope. Behavioral failures return to revision; inaccessible tooling/runtime blocks approval. Explicit human gates remain supported when required.
- UC-3 is next eligible; this approval/workflow request does not start its implementation. No runtime/operator data changed by this update.

## UC-3 Evidence

- Started: 2026-10-11; base commit `dc06893`; pre-existing dirty files: none. UC-1 is APPROVED.
- Scoped technical decision: retain JSON trees and optional clock representations; grid preview is read-only and bound to workspace version; one transactional CAS saves all selected rows/reservations and grid diagnostics. Existing IDs cannot be removed or renamed through this route. Availability arrays remain exact; omission automatically includes added slots. Timing review derives from exact source definition/accepted assignments and explicit proposed lesson locks. No feature rules exist; sources are UC-3, Period fields, Identifier rules, form/lifecycle models and approved UC-1 security/presentation boundaries.

- Grid construction checks: BUILD SUCCESS; 26 unit/architecture and 25 integration/browser checks, zero failures/errors/skips (/tmp/uc3-final-verify.log). Additional stronger conflict/addition assertions: two browser checks plus 26 unit/architecture checks passed (/tmp/uc3-assertions-verify.log). Syntax/diff hygiene passed. Existing UC-1 and UC-2 journeys remain green; operator data untouched.
- [Current construction evidence](checkpoints/UC-3.md). Not yet ready for convergence: extension 3b routes deletion through unimplemented UC-13. Direct deletion is safely refused without mutation, but guidance alone is not the complete continuation. User ordering decision requested; no later UC started and no contract/approval claim changed.

- Cleanup: unused agent-owned packaged server PID 73192/session 97656 stopped; disposable UC-3 walkthrough container removed. No browser session was opened and no operator/earlier walkthrough data was changed. Cached Playwright CLI is available; the wrapper attempted a registry lookup that failed under restricted network, so future walkthroughs can use the installed cached CLI.

## Automated execution policy update — 2026-10-11

- User explicitly requested loosening the execute skill, durable Playwright walkthrough ledgers with applicable screenshots, automatic approval on PASS, and production fixes/repeated walkthroughs on FAIL followed by the next UC. Updated repository execute and installed Playwright skills; both pass quick_validate.py. Separate converge reviews are optional; no two-attempt limit or procedural approval pause remains.
- Cross-UC “continue with” references are destination handoffs, not implicit Requires dependencies. UC-3 checks safe period-removal guidance/refusal now; UC-13 owns its complete guided-removal journey. This pending handoff must be implemented and verified in UC-13 before feature completion. Explicit relationship dependencies, product behavior, isolation, exact fidelity and negative-path evidence remain required. No specification has been weakened and no UC-13 completion is claimed.
- Resumed UC-3 from the existing preserved implementation and 51 passing relevant checks plus final strengthened assertions. No changed production code requires another technical run. Next is its standalone real-browser walkthrough and recorded automatic approval, then UC-4.

## UC-3 walkthrough attempt 1

- Submission add37ea; [recorded ledger](../../output/playwright/school-definition-authoring/UC-3/20261011-1/README.md): FAIL F-1. Real two-tab stale retry overwrites an unrelated newer period name. Main generation/save, cancellation, invalid-grid correction, responsive retention and unsaved navigation passed; no approval inferred. Execute is fixing three-way refresh/explicit conflict review and will repeat the full journey. All changes remain confined to the disposable loop workspace.

- F-1 revision: refresh now performs a three-way field merge against the working base. Newer untouched rows/fields are retained; conflicting edited fields show both values and block preview/save until an explicit keep/use-newer choice. Added real two-tab browser regression for both cases.

- F-1 technical validation: 26 unit/architecture checks and all 25 existing integration/browser journeys passed on the corrected production code. New two-tab regression initially asserted before async refresh completed; added a wait for its actual resulting input, then it passed (1/1, plus 26 unit/architecture). Logs: /tmp/uc3-f1-verify.log (synchronization failure retained), /tmp/uc3-f1-focused.log (BUILD SUCCESS). All expected value assertions retained. No production change after the full relevant run; no operator or tracked runtime data changed.

## UC-3 automatic approval

- Submission a30de35; [attempt 2 ledger and screenshots](../../output/playwright/school-definition-authoring/UC-3/20261011-2/README.md): PASS. Complete main/negative/responsive/stale/conflict/save/reload/addition journey repeated in fresh isolated Chrome workspace; F-1 resolved with unrelated newer rows retained and explicit same-field conflict choice. Technical evidence and final v12 API document linked in ledger. UC-3 is APPROVED automatically under the user-updated workflow; next UC-4. Destination UC-13 removal handoff remains tracked and is not approved by this slice.

## UC-4 Evidence

- Started 2026-10-11; base 63c2f24; pre-existing dirty files none. Explicit Requires UC-1 is APPROVED. Sources: UC-4, Subject fields, identifier/form/lifecycle models, existing local security and atomic aggregate. No feature rules.
- Technical choices: save only the selected subject, immutable valid identity, supported incomplete numeric/text work retained with issues; optional fields retain omission. Catalog-unavailable fields are refused at HTTP and explained in the UI. Recompute subject and affected-lesson issues, without editing qualifications/lessons/policies/source/Current. Reuse form state/version recovery and preserve newer unselected values. UC-12/13/14 remain destination handoffs, not implicit ordering blockers.

- UC-4 construction: six new subject HTTP/browser journeys and 26 unit/architecture checks passed after fixing Add-subject selection; BUILD SUCCESS `/tmp/uc4-revision-verify.log`. All 24 existing authoring/grid/school/compatibility journeys passed in the initial run, whose three UC-4 failures remain recorded (`/tmp/uc4-verify.log`). One was an inadequate catalog-1 fixture for a catalog-8 curator control; corrected the labelled draft variant to catalog 9 while preserving exact source. No test assertion was weakened. No operator data changed. Syntax/diff hygiene passed.
- UC-4 is PENDING_WALKTHROUGH. Outstanding destination handoffs: UC-12 catalog, UC-13 removal, UC-14 restoration. See checkpoint for boundaries and exact commands. No missing security test class is included in the pass count.

## UC-4 walkthrough finding F-1

- Attempt 1 on aba0f60: FAIL. Numeric value inputs have no accessible name in Chrome's accessible snapshot. Ledger/screenshots: [attempt 1](../../output/playwright/school-definition-authoring/UC-4/20261011-1/README.md). No durable mutation occurred. Added explicit labels and browser regression; a complete new attempt is required before approval.

- F-1 technical revision passes: 26 unit/architecture checks and the strengthened main save/reload/restart browser journey, BUILD SUCCESS `/tmp/uc4-label-verify.log`. Repeat the complete standalone walkthrough on the corrected build.

## UC-4 approval

- Automatically APPROVED after complete repeat Playwright PASS on dd395c4. [Attempt 2 ledger and screenshots](../../output/playwright/school-definition-authoring/UC-4/20261011-2/README.md). F-1 closed; Add-subject regression closed by technical/browser evidence. Final exact disposable workspace v25 preserved in the ledger, source {} and no Current; source/accepted fidelity also passes labelled technical variants.
- Six subject and 24 affected existing browser journeys pass, with 26 unit/architecture checks. Focused F-1 regression additionally passes; failures remain recorded, not reclassified green. No unresolved implementation finding. Catalog/removal/restoration handoffs remain for UC-12/13/14.
- Browser/server/data retained for immediately continuing UC-5 under existing authorization; no additional approval required.

## UC-5 evidence

- Started 2026-10-11 from 9208002; pre-existing dirty files none. UC-1 is APPROVED. Sources: UC-5, Teacher fields, Identifier rules, form model, preserved source/Current and local security contracts. No rules.md exists.
- Selected-teacher JSON-tree save uses the same transactional version/state CAS, exact optional presence, identity reservation and local access checks. Qualification/availability/undesirability remain distinct; supported invalid references persist with complete issues, never inferred qualifications or reassigned lessons. Real-browser form/reload/stale/responsive/failure evidence will control automatic approval.
- Destination handoffs: teaching reassignment UC-9, guided removal UC-13, source restoration UC-14; pending until those UCs run.

- UC-5 initial technical run retained at `/tmp/uc5-verify.log` (test compilation error corrected). `/tmp/uc5-final-verify.log` found one teacher assertion observing old diagnostics because pristine submits showed Saved during the write. Production fix gives both entity forms an explicit Saving status; stronger helpers assert each successful save advances the durable version exactly once. All 20 approved subject/grid/school journeys passed in that run. Repeating all affected teacher/subject journeys on the corrected code before walkthrough.

- UC-5 revision BUILD SUCCESS: 26 unit/architecture and all 11 teacher/subject HTTP/browser journeys pass in `/tmp/uc5-revision-verify.log`; the prior run supplies all 14 unchanged grid/school regression passes. Exact version/source/Current assertions pass. Syntax/diff hygiene pass. Ready for recorded standalone Playwright walkthrough, not yet approved.

## UC-5 approval

- Automatically APPROVED after recorded real Playwright PASS on 1084c81: [ledger/screenshots](../../output/playwright/school-definition-authoring/UC-5/20261011-1/README.md). Exact disposable v40 document retained; all five fields, refusal/invalid/correction, omission/empty, responsive, two-tab conflicts, injected transport retry, selection/save/reload journeys pass. Screenshots inspected, no open implementation finding.
- Technical evidence: 26 unit/architecture and 25 teacher/subject/grid/school journeys pass across the final/revision logs; initial failures preserved and fixed. Source/Current remain exact in fresh initial/accepted variants. No operator data changed. UC-9/13/14 handoffs remain outstanding.
- Main browser/server/database retained for immediately proceeding to UC-6 within authorization.

## UC-6 evidence

- Started 2026-10-11 from ea2356c; pre-existing dirty files none. Sources: UC-6, all 12 Cohort fields, Identifier rules, form/lifecycle/validation meaning tables and kernel curator/home-room/policy semantics. UC-1 APPROVED; no feature rules.md.
- Preserve exact JSON optional presence and source/Current via selected-class version/state CAS. Explicit default/value/no-limit choices; supported incomplete values and references save with blockers. Curator/home-room changes never rewrite lessons or policies. Necessary-condition advisories remain distinct from definition blockers. UC-12/13/14 are outstanding destination handoffs.

- UC-6 initial `/tmp/uc6-verify.log` passes 26 unit/architecture, all five teacher/seven grid/seven school journeys and five of six new class journeys. Subject/class room-policy variants exposed a strict Jackson boolean read of omitted useHomeRoom. Fixed optional boolean reads for policies and reserved permission; the affected full entity suite is rerunning in `/tmp/uc6-revision-verify.log`. Added explicit typed-invalid linked/array value fidelity assertions for the class HTTP boundary; those will run in the final focused check.

- UC-6 revision BUILD SUCCESS `/tmp/uc6-revision-verify.log`: 26 unit/architecture and all 17 class/subject/teacher journeys pass. Prior unchanged grid/school checks pass 14/14. Optional boolean failure resolved. Final strengthened class types/catalog check running `/tmp/uc6-types-verify.log`; no production change after full revision pass.

- Final typed-invalid assertion (`/tmp/uc6-types-verify.log`) found another strict Jackson coercion: an array-valued homeRoomId threw on asString(), returning 500 instead of saving supported invalid work. JSON tree diagnostics now use the explicit fallback string reader; type validators still report blockers and the original JSON value stays exact. Full class/subject/teacher suite repeated on this production fix (`/tmp/uc6-final-verify.log`); the stronger typed assertions remain intact.

- UC-6 final BUILD SUCCESS `/tmp/uc6-final-verify.log`: 26 unit/architecture and all 17 class/subject/teacher journeys pass with strengthened invalid-type fidelity checks. Fourteen unchanged grid/school journeys pass in retained initial evidence. Strict optional boolean/string diagnostic failures are fixed; failed logs preserved. Ready for standalone recorded Playwright walkthrough on a fresh disposable fixture.

## UC-6 approval

- Automatically APPROVED on submission 6ca5e84 after [recorded Playwright PASS](../../output/playwright/school-definition-authoring/UC-6/20261011-1/README.md). Exact isolated v18 document and inspected screenshots retained; all twelve fields, linked curator/policy issues, advisories/defaults/no-limit, responsive/stale/conflict/transport/identity/add/selection/reload pass. Relevant technical checks pass; failed diagnostic logs preserved and fixed. Source/Current untouched. UC-12/13/14 destination handoffs remain. Proceeding directly to UC-7.

## UC-7 evidence

- Started 2026-10-11 from bce5872; pre-existing dirty files none. Five Room fields, exact open tokens, zero capacity and omitted/empty availability implemented with selected-room CAS and local protections. Shared dependent room/class/lesson diagnostics refresh without changing source/Current or dependents. UC-13/14 are outstanding destination handoffs. [Checkpoint](checkpoints/UC-7.md); technical suite /tmp/uc7-verify.log running.

- UC-7 technical BUILD SUCCESS `/tmp/uc7-revision-verify.log`: 26 unit/architecture and all four room journeys pass. Initial `/tmp/uc7-verify.log` passes all 17 affected class/subject/teacher journeys and three room journeys; one fixture lacked the capability it asserted, corrected explicitly without weakening assertions. Initial failure retained. Syntax/diff checks pass, operator data untouched. PENDING_WALKTHROUGH.

## UC-7 approval

- Automatically APPROVED on 6622f19 after [recorded Playwright PASS](../../output/playwright/school-definition-authoring/UC-7/20261011-1/README.md). Exact v34 document, all five fields, capacity zero/open/empty tokens, omitted/empty availability, linked policy blockers/advisories, invalid/cancel/correction, responsive/stale/conflict/transport/add/selection/reload pass; screenshots inspected. Technical four room and 17 affected entity journeys plus 26 unit/architecture pass. Automation-only wrong fault route is retained and corrected, not classified as a product bug. Source/Current intact, no open finding. UC-13/14 remain handoffs; proceeding UC-8.

## UC-8 evidence

- Started 2026-10-11 from 2438513, pre-existing dirty files none. Durable requirement context and conservative imported inference, complete atomic preview/save, exact cancellation choices, stable generated IDs/names, scopes and explicit equal exceptions, supported incomplete count work, zero-count context implemented. Native fields remain native, source/Current unchanged. [Checkpoint](checkpoints/UC-8.md). UC-9/14 are destination handoffs. Technical suite /tmp/uc8-verify.log running; initial compile cast error retained in /tmp/uc8-compile.log, corrected package passes.

- UC-8 initial `/tmp/uc8-verify.log`: 26 unit/architecture and all 21 approved room/class/subject/teacher journeys pass; all four new requirement journeys fail because the GET inspection route lacked local allowlist access. Added GET allowlist and explicit read-failure Retry UI. Revision `/tmp/uc8-revision-verify.log`: backend mixed/default/exception and incomplete/zero-count journeys pass, two form journeys expose showModal on a detached parent. Attached-before-open scheduling fixes it; pending-request navigation and stale render guards added. Full requirement suite repeated in /tmp/uc8-modal-verify.log. Failed logs retained, no operator data changed.

- `/tmp/uc8-modal-verify.log` BUILD SUCCESS: all four requirement journeys and 26 unit/architecture pass after dialog attachment, complete teaching/context diagnostic refresh and pending/stale render guards. `/tmp/uc8-final-verify.log` BUILD SUCCESS: all 39 requirement/entity/grid/school journeys plus 26 unit/architecture. During final review, distinguished new versus existing series identity explicitly; added collision/immutability refusals and tests. Final request-contract suite running /tmp/uc8-identity-verify.log. No walkthrough approval inferred yet.

- UC-8 final identity contract BUILD SUCCESS `/tmp/uc8-identity-verify.log`: four requirement journeys plus 26 unit/architecture pass, including new-series collision and immutable saved ID guards. All 39 affected journeys pass in /tmp/uc8-final-verify.log; only the UC-8 request shape/identity guards and incomplete-count explanation changed afterward and have been verified. Syntax/diff checks pass. PENDING_WALKTHROUGH against a fresh verified mixed-source fixture/database, no operator data touched.

## UC-8 approval

- Automatically APPROVED on 42f3b53 after [recorded standalone Playwright PASS](../../output/playwright/school-definition-authoring/UC-8/20261011-1/README.md). Exact isolated v19 document/context and inspected screenshots retained. Mixed/equal exceptions, selected/Apply-all scopes, canceled/refused count operations, stable IDs/names, incomplete/zero context, all native shared settings/lock blockers, responsive/stale/conflict/transport/navigation/reload/source fidelity pass. Zero blockers and two necessary-load advisories at final state; no timetable-feasibility claim. Relevant 39 journeys plus final four identity-contract journeys and 26 unit/architecture pass; failed evidence preserved and fixed. No operator change or open finding. UC-9/14 destination handoffs remain; starting UC-9.

## UC-9 evidence

- Started from ac0af58 with only preserved agent-owned UC-9 work. Full Lesson field, explicit inheritance/exception, compatible membership and selected teaching/rule-transfer contracts implemented in JSON-tree atomic preview/save. Source/Current exact; persistent lock choices retain authored provenance. Integrated dedicated Occurrence details navigation and requirement/teacher destination routes. UC-13/14 remain destination handoffs.
- Initial `/tmp/uc9-verify.log` test lambda compilation failure fixed. `/tmp/uc9-revision-verify.log` passes 26 unit/architecture and five affected teacher journeys; new fixtures exposed catalog/accepted-manifest mistakes and check() incorrectly asserting a pending unsaved selection had already committed. Corrected fixtures and use click followed by Stay/non-mutation assertions. Failures preserved; no production expectation weakened. Complete new occurrence/requirement journeys rerunning in `/tmp/uc9-fixture-verify.log`.

- UC-9 final BUILD SUCCESS `/tmp/uc9-final-verify.log`: all 21 occurrence/requirement/authoring/teacher journeys and 26 unit/architecture checks pass. Single-origin manifest bug fixed; exact persistent/shared lock intent passes. Final UI adds replacement qualifications explanation and compatible target suggestions; syntax/diff checks pass. PENDING_WALKTHROUGH with verified fresh disposable fixture.

- UC-9 standalone attempt 1 retained at [ledger](../../output/playwright/school-definition-authoring/UC-9/20261011-1/README.md), FAIL F-1: overly broad harness assertion omitted explicit completion of the retained zero-count Mixed source requirement. Selected occurrence operation is correct; no production defect inferred. Repeat entire journey with explicit remaining-context correction on fresh isolated data. Initial server connection override caught before mutations; relaunch Compose-disabled and verify isolation in log.

## UC-9 approval

- Automatically APPROVED on 8109b2b after [complete repeat Playwright PASS](../../output/playwright/school-definition-authoring/UC-9/20261011-2/README.md). Exact isolated v18 document/intent/source, fourteen inspected screenshots and complete main/negative/responsive/stale/transport/reload journeys retained. F-1 is closed by explicit retained-context completion, no weakened guarantee. All 21 technical journeys plus 26 unit/architecture pass. No unresolved finding; no operator mutation. UC-5/8 occurrence destination routes now verified; UC-13/14 remain outstanding. Proceeding UC-10.

## UC-10 evidence

- Started from 004bdb7, clean worktree. All five native room-policy fields, explicit hard target/scope, version/state CAS and complete policy/lesson diagnostics implemented. No inferred room/home mandate or native placement changes. [Checkpoint](checkpoints/UC-10.md). Catalog/guided-removal/restoration destination handoffs UC-12/13/14 remain.
- `/tmp/uc10-compile.log` BUILD SUCCESS; relevant hard-policy and affected entity/requirement/occurrence suite running in `/tmp/uc10-verify.log`. No walkthrough/approval inferred.

- UC-10 first `/tmp/uc10-verify.log` retained: new UI module had an extra closing brace, preventing all workspace entry. Stopped known-failing test JVM after capturing parse failure; corrected module. Revision `/tmp/uc10-revision-verify.log` passes all four room-policy/four room/five teacher/five occurrence journeys so far. Complete diagnostic refresh exposes unrelated Science teaching intentionally left unqualified in an old curator fixture; retain Science qualification explicitly while clearing Math to test curator exception, preserving every assertion. Final policy rendering also guards supported malformed room capacity/capability/availability values; strengthened rendering and stale waits will be verified in the final focused policy/subject run.

- UC-10 final technical checks pass: all 34 relevant journeys across `/tmp/uc10-revision-verify.log` (unchanged room/teacher/occurrence/requirement passes), `/tmp/uc10-final-verify.log` (six subject/four policy passes, BUILD SUCCESS), and `/tmp/uc10-class-verify.log` (all six class journeys, BUILD SUCCESS), plus 26 unit/architecture. Complete teaching diagnostics require four advisories in the zero-class-availability fixture: retain the original three and explicitly assert the additional teacher-load advisory. New malformed-resource effects guard and strengthened stale wait pass. Syntax via module stdin and diff hygiene pass. Ready for standalone walkthrough; failed logs retained.
