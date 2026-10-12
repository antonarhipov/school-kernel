# Use-Case Status: school-definition-authoring

## Current

- Use case: UC-27
- Status: APPROVED
- Feature: IMPLEMENTED — all27 use cases approved
- Next eligible: none

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
| UC-10 | APPROVED | UC-1 | `c997bd6` / [checkpoint](checkpoints/UC-10.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-10/20261011-1/README.md) |
| UC-11 | APPROVED | UC-1 | `1fc3328` / [checkpoint](checkpoints/UC-11.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-11/20261011-2/README.md) |
| UC-12 | APPROVED | UC-1 | `dfabfd8` / [checkpoint](checkpoints/UC-12.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-12/20261011-3/README.md) |
| UC-13 | APPROVED | UC-1 | `caddae3` / [checkpoint](checkpoints/UC-13.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-13/20261011-1/README.md) |
| UC-14 | APPROVED | UC-1 | `7eaef7a` / [checkpoint](checkpoints/UC-14.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-14/20261012-2/README.md) |
| UC-15 | APPROVED | UC-1 | `ac6faa0` / [checkpoint](checkpoints/UC-15.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-15/20261012-5/README.md) |
| UC-16 | APPROVED | UC-1, UC-15 | `56f8cdd` / [checkpoint](checkpoints/UC-16.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-16/20261012-5/README.md) |
| UC-17 | APPROVED | UC-16 | `88756ab` / [checkpoint](checkpoints/UC-17.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-17/20261012-3/README.md) |
| UC-18 | APPROVED | UC-16 | `99a8fac` / [checkpoint](checkpoints/UC-18.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-18/20261012-1/README.md) |
| UC-19 | APPROVED | UC-1 | `e90d7a5` + correction `726774d` / [checkpoint](checkpoints/UC-19.md) | [original PASS](../../output/playwright/school-definition-authoring/UC-19/20261012-3/README.md), [legacy correction PASS](../../output/playwright/school-definition-authoring/UC-19/discard-legacy-20261012-4/README.md) |
| UC-20 | APPROVED | none | `1090151` / [checkpoint](checkpoints/UC-20.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-20/20261012-7/README.md) |
| UC-21 | APPROVED | UC-1 | [checkpoint](checkpoints/UC-21.md) / exact patch | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-21/ux-20261012-1/README.md) |
| UC-22 | APPROVED | UC-1 | [checkpoint](checkpoints/UC-22.md) / exact patch | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-22/ux-20261012-1/README.md) |
| UC-23 | APPROVED | UC-1 | [checkpoint](checkpoints/UC-23.md) / exact patch | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-23/ux-20261012-5/README.md) |

| UC-24 | APPROVED | UC-1 | [checkpoint](checkpoints/UC-24.md) / exact patch | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-24/remaining-20261012-2/README.md) |
| UC-25 | APPROVED | UC-1 | [checkpoint](checkpoints/UC-25.md) / exact patch | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-25/remaining-20261012-3/README.md) |
| UC-26 | APPROVED | UC-1 | [checkpoint](checkpoints/UC-26.md) / exact patch | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-26/remaining-20261012-4/README.md) |
| UC-27 | APPROVED | UC-1 | [checkpoint](checkpoints/UC-27.md) / exact patch | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-27/remaining-20261012-2/README.md) |

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

## UC-10 approval

- Automatically APPROVED on c997bd6 after [recorded real Playwright PASS](../../output/playwright/school-definition-authoring/UC-10/20261011-1/README.md). Exact isolated v22 document/native/source, fourteen inspected screenshots and scope/target/invalid/intersection/lock/unmatched/responsive/stale/conflict/transport/navigation/reload outcomes retained. All 34 affected technical journeys plus 26 unit/architecture pass across final evidence. Failed parse/diagnostic expectation logs preserved; no open finding or operator mutation. UC-12/13/14 remain destination handoffs. Proceeding UC-11.

## UC-11 evidence

- Base 35787da, only agent-owned preference edits in progress. All seven ordinary weights, default omission, explicit zero, raw invalid supported values, selected-only CAS and exact kernel-equivalent overflow calculation implemented. Grid/entity/requirement/occurrence changes refresh dependent bounds. UI saved/effective table, fixed preference identity, stale selected conflicts, Save/Discard/Stay and narrow saved-only inspection. UC-12/14 destination handoffs remain. Technical/browser checks in progress; approval pending.

- Initial UC-11 run retained at /tmp/uc11-verify.log. All grid checks pass; School identity viewport regression exposes added navigation wrapping that pushes its Save below 800px. Compact section buttons restore the existing visible-controls contract. Explicit default-only test fixture also corrected to omit imported overrides; invalid numeric assertions compare exact JSON types. Repeat affected checks before approval.

- Final UC-11 technical BUILD SUCCESS /tmp/uc11-revision-verify.log: all three preference/seven School journeys plus 27 unit/architecture checks pass. Initial log supplies unchanged seven grid/four room-policy passes. Exact raw types/default omission/selected repair, source/Current/restart, state/CSRF/stale/storage/transport and responsive contracts pass. Navigation regression fixed. Module syntax/diff hygiene pass. PENDING_WALKTHROUGH; failures retained.

- UC-11 standalone attempt 1 FAIL F-1: entry harness clicked the already-present EMPTY Create-school control before import completed. Source inspection caught blank instead of intended nonempty fixture. [Retained ledger](../../output/playwright/school-definition-authoring/UC-11/20261011-1/README.md). Fix harness wait/assertions and repeat the entire journey in a fresh database; no production defect inferred.

## UC-11 approval

- Automatically APPROVED on 1fc3328 after [complete repeat Playwright PASS](../../output/playwright/school-definition-authoring/UC-11/20261011-2/README.md). Exact v35/source/native and twelve inspected screenshots retained. All seven IDs/defaults/zero/maximum, invalid/correction, responsive/transport/stale/conflict/navigation/discard/reload pass. F-1 corrected by explicit import/source synchronization; failed attempt preserved. Relevant 21 journeys and 27 unit/architecture pass across retained logs. No operator mutation or unresolved finding; UC-12/14 remain handoffs. Proceeding UC-12.

## UC-12 evidence

- Started from 9a9a3e6, clean worktree. Preview/token-bound atomic catalog 9 transition reuses the exact accepted-opening preservation table, keeps explicit/native/source/Current values, and records changed defaults/new settings/exact compensations. Full entity/teaching/preference issues refresh. School preview/confirm/cancel and unavailable-setting navigation handoffs implemented; pending/version/failure guards require fresh review. Technical checks pending.

- UC-12 primary technical BUILD SUCCESS /tmp/uc12-verify.log: all 24 catalog/preference/authoring/subject/policy journeys and 27 unit/architecture checks pass. Six class journeys are running separately (/tmp/uc12-cohort-verify.log); the initial command mistakenly named DefinitionClassesBrowserIT rather than DefinitionCohortsBrowserIT, so no omitted checks are counted. All catalog crossings, exact compensation/native/source values, stale/token/storage/transport/CSRF/state/restart and guarded UI pass. Module syntax/diff hygiene pass.

- Class regression BUILD SUCCESS /tmp/uc12-cohort-verify.log, all six journeys. Final total 30 relevant journeys + 27 unit/architecture pass. Ready for standalone recorded Playwright walkthrough; no technical finding.

- UC-12 standalone attempt 1 FAIL F-1: recovery dialog disappeared during stale-refresh/transport/screenshot sequence, preventing its refresh control. [Retained evidence](../../output/playwright/school-definition-authoring/UC-12/20261011-1/README.md). Non-mutation during failed sequence observed; subsequent reproduction after CLI route lifetime ended legitimately saved v4 and is distinguished. Bind closure to owned modal, bound modal layout, assert visible failure controls after capture, rerun affected checks and complete fresh walkthrough.

- F-1 revision BUILD SUCCESS /tmp/uc12-revision-verify.log, all four catalog journeys and 27 unit/architecture checks including visible transport-dialog controls after screenshot. Prior 26 unchanged affected journeys remain green. Repeat full standalone journey on revised build/fresh source; not yet approved.

- Attempt 2 FAIL F-2 [ledger](../../output/playwright/school-definition-authoring/UC-12/20261011-2/README.md): F-1 recovery/capture assertions pass through exact v4 upgrade; host script structuredClone unavailable while comparing cohort. Replace host clone with JSON roundtrip and repeat entire walkthrough on fresh data; no production defect inferred.

## UC-12 approval

- Automatically APPROVED on dfabfd8 after [complete recorded Playwright PASS](../../output/playwright/school-definition-authoring/UC-12/20261011-3/README.md). Exact v5/source/native and eleven inspected screenshots retained. Preview/cancel/compensation/stale/transport/fresh review/atomic upgrade/deliberate newer default/responsive/reload pass. F-1/F-2 fixed, failed attempts preserved. All 30 technical journeys and 27 unit/architecture pass; strengthened four recovery journeys and final handoff/restart check pass (/tmp/uc12-handoffs-verify.log BUILD SUCCESS). Catalog destination handoffs from UC-3/4/6/10/11 are now implemented and verified. No operator mutation/unresolved finding. Proceeding UC-13.

## UC-13 evidence

- Started from cfa24c3, clean worktree. Full seven-target native dependency table plus saved requirement defaults/context, separate historical Current placements, explicit cancellation/policy removal/ref replacement and target-mode choices. Inspect/preview are non-mutating; exact document/version/request token governs one atomic complete operation. No unknown/unresolved replacement or final-empty policy target accepted. Counts, reservations, authored locks, issues and selected lineage identity reservations updated explicitly; source/Current exact. UI guided resolution/preview/cancel/stale/read-only/navigation flow and entity handoffs in construction; technical checks/approval pending.

- Initial /tmp/uc13-verify.log retained: 27 unit/architecture, all seven grid/five occurrence/four requirement journeys and four of six new removal journeys pass. Two new journeys used source room-102 as a no-dependency target despite existing preferred-room references; explicitly create unused-room and retain no-dependency/new-identity assertions. Strengthen Current placement expansion to selected owned groups/policy matches and explicit differing default/occurrence replacement exception intent. Full six removal journeys repeat on updated submission.

- /tmp/uc13-revision-verify.log retains five backend removal passes + 27 unit/architecture; UI fixture added unused-room through HTTP but did not reload before selecting it. Add explicit reload after fixture mutation, keeping the rendered actor path required. New regroup regression preserves equal-valued exception intent and all native constraints; selected membership/default differences become explicit exceptions, no future implicit overwrite. Repeat complete seven removal journeys on the updated code/tests.

- /tmp/uc13-final-verify.log passes six backend removal journeys and 27 unit/architecture. Newly strengthened all-editor handoff walk finds Occurrence details was passed base actions instead of the wrapper carrying guided-removal navigation. Fix the production action wiring; complete seven-journey suite repeats. No backend finding; failed evidence retained.

### UC-13 technical submission

All seven removal journeys and 27 unit/architecture checks pass in `/tmp/uc13-modal-verify.log`; 16 unchanged grid/occurrence/requirement regressions passed in `/tmp/uc13-verify.log`. Retained failed checks exposed the missing occurrence handoff and disabled controls after preview cancellation; both production fixes verified.

### UC-13 automatic approval

Recorded complete first standalone attempt PASS, exact v13, 17 inspected screenshots, source/Current fidelity and recovery evidence. [Walkthrough ledger](../../output/playwright/school-definition-authoring/UC-13/20261011-1/README.md). Approved under the authorized autonomous loop; proceeding with UC-14 source restoration.

### UC-14 technical submission

Seven restoration journeys plus 27 unit/architecture checks pass in `/tmp/uc14-context-verify.log`. Seven approved removal regressions pass in retained `/tmp/uc14-revision-verify.log`. Exact grouped/occurrence regression class names corrected and running separately. Individual preference and weekly source context restoration retain unrelated native data and exception intent; no member resurrection. Earlier compile/assertion/build-overlap failures retained.

UC-14 first walkthrough FAIL retained: harness assumed a common teacher despite an explicitly mixed source. Production saved exact no-default/mixed/exception context. Correct the source-derived assertion and repeat complete walkthrough on fresh data.

### UC-14 automatic approval

Complete repeat PASS, exact v18, 14 inspected screenshots. Seven restoration, seven removal and nine grouped/occurrence journeys PASS. [Ledger](../../output/playwright/school-definition-authoring/UC-14/20261012-2/README.md). Source-restoration handoffs closed; proceeding to UC-15.

### UC-15 technical submission

Five complete review journeys and 27 unit/architecture checks PASS (`/tmp/uc15-revision-verify.log`); seven restoration regressions PASS in retained `/tmp/uc15-verify.log`. Initial review fixture errors (hidden collapsed evidence and new-school entry in School) corrected; source/Current, issue sorting across restart, literal text, timing/addition/cancellation sides, exact/stale/removed links and unsaved boundary verified.

UC-15 attempt1 FAIL retained: import wait raced existing Create school, opening EMPTY rather than accepted source; CAS refused concurrent import. Await named rendered accepted-source Author definition and assert durable accepted state before proceeding. Full repeat required.

UC-15 attempts2/3 FAIL retained: observed Classes selector corrected; residual page/context fault route aborted cohort saves. Exact request-failure evidence and explicit UI retry after clearing routes prove v6 legitimate recovery. Fresh Chrome context and full repeat required. Failed UC-15 servers (18092–18094) stopped after evidence capture; disposable databases/logs retained.

UC-15 attempt4 action/state scripts PASS but screenshot inspection found obsolete Current draft-editing guidance. NEEDS_REVISION retained; corrected message now accurately says Current is accepted and draft changes unpublished. Relevant prior technical behavior checks unchanged; full new-build walkthrough repeat required.

### UC-15 automatic approval

Complete final-build repeat PASS, exact v9 and 18 inspected images, accurate Current/unpublished-draft distinction. [Ledger](../../output/playwright/school-definition-authoring/UC-15/20261012-5/README.md). All earlier attempts retained. Five review/seven restoration journeys PASS, technical behavior unchanged by final message fix. Proceeding to UC-16 with exact nine-lesson onboarding requirement.

### 2026-10-12 — UC-16 implementation and technical revision

Finish now binds confirmation to the whole saved document/version, recomputes Review blockers, verifies complete initial/successor definitions, and atomically creates initial or seeded repair work. Kernel successor-only verification needed an explicit non-solving mode; source verification remains separate and exact. Initial acceptance retains authoring context. First checks retained in `/tmp/uc16-verify.log`: five Review and eight existing initial-planning journeys passed; Finish exposed the successor-verifier gap and persisted school-identity recomputation gap, now fixed. The malformed test requirement context was also corrected without weakening product assertions. Packaged CLI verification plus ten semantic checks passed in `/tmp/uc16-kernel-verify.log`. Revised Finish/Review check is running; no approval yet.

### 2026-10-12 — UC-16 technical PASS

Revised checks PASS: 6 Finish + 5 Review browser journeys and 8 verifier/architecture checks; packaged kernel 9 CLI journeys + 10 semantic checks. Evidence paths above and [checkpoint](checkpoints/UC-16.md). Beginning exact normative nine-lesson real-browser onboarding against isolated `authoring_loop_uc16`; no approval until ledger PASS.

### 2026-10-12 — UC-16 walkthrough revisions and baseline comparison

Attempts1–3 retained under `output/playwright/school-definition-authoring/UC-16/`. Exact nine-lesson onboarding/first acceptance passed twice. Attempt1 selector typo fixed; attempt2 led to explicit verified seed identity and viewport-safe confirmation; attempt3 found long SHA notice narrow overflow, now wrapped. Final6 Finish test repeat is running. Resize-wait check passes (`/tmp/uc16-viewport-verify.log`). Ordinary repair regression failures are unchanged: same four IDs at `0c360a4` in isolated `/tmp/uc16-baseline`, `/tmp/uc16-repair-baseline.log`; pointer interception and geometry failures occur without authored seed. Preserve and resolve relevant operational defects during UC-17. Older agent-owned servers stopped after disposable PostgreSQL reached connection limit; their databases/evidence remain. No operator runtime changes.

### 2026-10-12 — UC-16 APPROVED; UC-17 started

Final-build complete normative repeat5 PASS v20 with20 inspected images; same-build separate fault journey from attempt4 also PASS and linked. Exact9-lesson form onboarding, realPT1M plan/result verification/first acceptance, saved-context retention; initial/accepted/zero/invalid/unavailable/stale/transport/storage paths, exact complete source/Current preservation and narrow seeded identity. [Passing ledger](../../output/playwright/school-definition-authoring/UC-16/20261012-5/README.md). No human confirmation gate. Beginning UC-17, including UC-18 enabling restoration for mandatory MVK journey; neither destination approved yet.

### 2026-10-12 — UC-17 implementation and walkthrough correction

Base `e86b5d1`, four carried UC-17 source edits only. Complete authored seed compilation, source/seed verification, intent/context binding, native diffs and actual same-ID timing review, exact context/metadata acceptance, persistent-versus-attempt lock provenance. UC-18 return-to-authoring is necessary enabling work for G6; not approved separately yet.5 focused service tests and6 Finish browser journeys PASS. First seeded browser checks found missing security route (403), fixed; run creation test expectation corrected to202. Existing four ordinary RepairDraftBrowserIT failures recur; repair inspector now uses a sidebar at1280+ and stacks below the canvas at smaller widths to prevent pointer interception and satisfy reachable task controls. Corrected regression run underway `/tmp/uc17-checks-r2.log`.

[Walkthrough attempt1 FAIL](../../output/playwright/school-definition-authoring/UC-17/20261012-1/README.md): exact MVK definition SHA checked, real baseline planned/verified; exactly three native edits,519 lessons, complete source/seed fidelity and narrow inspection passed. Abandonment route403 prevented restoration. Preserve failure/screenshot; full journey must repeat on corrected build. No UC-17 approval.

### 2026-10-12 — UC-17 corrected technical PASS and second walkthrough findings

`/tmp/uc17-checks-r2.log`:5 focused service tests,3 seeded lifecycle/refusal/storage-failure browser tests,6 ordinary repair browser journeys PASS; all four baseline failure IDs repaired without weakening tests. [Attempt2](../../output/playwright/school-definition-authoring/UC-17/20261012-2/README.md) exact MVK full authoring/restoration/re-finish/real repair PASS; generated feasible TIME_LIMIT proposal has1 cancellation,0 period-ID moves,11 actual timing impacts. Review harness failed opening disclosure through DOM mutation; final journey uses rendered summary clicks. Corrected product wording from same-time to same-period/class and proposal seed notice to verified proposal awaiting acceptance. Final acceptance/storage/rejected-output and Finish regressions running `/tmp/uc17-checks-r3.log`;16 verification/planning/service unit tests PASS. Screenshots attempt1/2 contact sheets inspected; review full-page images excessively tall, final captures use meaningful viewport states.

### 2026-10-12 — UC-17 automatic approval

Production `88756ab`, [full PASS ledger](../../output/playwright/school-definition-authoring/UC-17/20261012-3/README.md). Mandatory exact MVK authoring/restoration/re-Finish/packaged repair/independent verification/review/explicit acceptance complete.519 lessons,1 cancellation,0 period moves and11 timing impacts. Actual saved-proposal restart and acceptance-only storage failure preserved entire v10 snapshot; explicit retry atomically accepted exact definition/result/context/metadata v11; final packaged verification PASS. Extra staged-availability/pin variant on copied real proposal passed and survives reload; source/seed exact. Five seeded browser tests now PASS r5 after correcting API-start polling harness. Main images/contact sheet and readable timing supplemental inspected. UC-17 APPROVED; continue UC-18 separately using retained v13 seeded variant at18108. Operator data untouched.

### 2026-10-12 — UC-18 automatic approval

`99a8fac`, [PASS ledger](../../output/playwright/school-definition-authoring/UC-18/20261012-1/README.md). Real saved MVK seed with extra teacher unavailability and both placement pin dimensions: cancel/narrow/stale two-tab refusal and actual return-storage refusal preserved whole v14; retry restored exact retained authoring/context/issues/omissions/source v15. Active solve refused return; actual cancel and explicit restoration exact v19. Copied real proposal showed proposal-only-discard-first guidance and refused direct return without mutation. Eight images/contact sheet inspected, compile/diff and immediately preceding unchanged backend technical checks PASS. UC-18 APPROVED; proceed UC-19.

### 2026-10-12 — UC-19 automatic approval

Submission e90d7a5; full standalone repeat3 PASS. All three source states, exact pre-upgrade catalog1 restoration after real solve/accept/upgrade, unsaved cancellation/resize, stale412 and actual storage503/retry/source/Current fidelity/reload.14 original screenshots and contact sheet inspected.3 discard+7 authoring browser checks and1 unit PASS r3; earlier harness label/desktop-filter assumptions corrected without weakening durability assertions. Failed/superseded artifacts retained. [Ledger](../../output/playwright/school-definition-authoring/UC-19/20261012-3/README.md). UC-19 APPROVED; continue UC-20.

### 2026-10-12 — UC-20 implementation and walkthrough revision

Direct teacher fields (including same/null) refused without auto-draft; manual teacher selector removed; lifecycle/shared/occurrence route and explicit legacy full-assignment revert/publication block.11 existing manual API checks and5 unit checks PASS r2;3 of4 new browser paths PASS. Accepted inspection and route-entry checks exposed inspector overlap; compact responsibility text plus explicit close-and-show-route handoff added. First two standalone attempts retained, no approval; full changed-build repeat required.

### 2026-10-12 — UC-20 regression repair loop

Real actor attempts3–6 passed behavioral assertions but remained unapproved while final regressions exposed inspector reachability defects. Current popovers avoid toolbar/filter controls; Draft/Proposal/Solving inspectors use side/stack layout. Old manual teacher lens test replaced by supported room departure with exact teacher preservation; old proposal expectations now include independently specified Actual timing rows.2 corrected lens paths and4 teacher paths PASS r6; solving completion and old timing expectation corrected for r7. All failed attempts/logs/images retained; no human gate introduced.

### 2026-10-12 — UC-20 final technical PASS

Final r8:33 unit/architecture,11 manual API,4 teacher-route browser and exact normative proposal review PASS. The remaining assertion expected excluded selection to survive explicit narrow class/day changes, contrary to the inspection contract; now verifies clearing and explicitly reselects before collapse/reopen scroll preservation. Full changed-build standalone repeat7 PASS;17 screenshots/contact sheet inspected, direct409 lifecycle refusals and exact Current, legacy full revert, native cancellation and real shared/occurrence controls. Approval ledger being finalized; reactor/source/grouped/occurrence/discard/persistence/repair final checks running. Earlier failed evidence retained.

### 2026-10-12 — UC-20 automatic approval

Submission `1090151`, full changed-build repeat7 PASS.17 inspected screenshots, native manual cancel/discard exact v55–56; source/Current and legacy422/full revert/publication/reload v59–61; actual shared/selected teacher controls and restoration v62–63. Real retained MVK proposal/draft/solve direct409 with state-specific guidance and exact snapshots, real cancellation v22–25.33 workspace unit/architecture +16 final UC/regression paths PASS r8; related accepted/lens/proposal/Finish/seeded/repair-completion coverage retained. [Passing ledger](../../output/playwright/school-definition-authoring/UC-20/20261012-7/README.md). All20 approved; no destination handoffs remain. Final reactor and representative feature regressions running.

### 2026-10-12 — final regression harness correction

Reactor unit checks and source/discard/grouped/occurrence journeys pass so far. SavedDefinitionDraftBrowserIT expected a disabled `#edit-teacher`, superseded by UC-20's required absence. Correct only that assertion; disabled period/room and exact saved draft/source/Current refusal/reload assertions retained. [Failed regression evidence](../../output/playwright/school-definition-authoring/final-regression/20261012-1/README.md). Production/walkthrough unchanged. Repeat all three saved-draft compatibility cases after final ordinary repair checks.

### 2026-10-12 — feature IMPLEMENTED

All20 use cases APPROVED, all explicit dependencies and destination handoffs complete. Final136 unit checks and28 representative source/authoring/grouped/occurrence/discard/persistence/ordinary-repair API/browser paths PASS across complete reactor attempt and focused corrected compatibility repeat. Initial stale teacher-selector assertion failure retained, single test expectation updated, all3 saved-draft cases and33 workspace unit/architecture repeat PASS. Production and complete passing UC-20 walkthrough unchanged. [Final completion/regression/runtime ledger](../../output/playwright/school-definition-authoring/final-regression/20261012-2/README.md).17 superseded browsers and3 agent-owned servers closed; disposable evidence/data retained. Final accepted demo v63 available at18110, no active solve/fault trigger, operator runtime/data untouched. No human confirmation or separate convergence gate claimed.

### 2026-10-12 — UC-19 persisted legacy discard finding

User-reported `definition-discard.js:12` uncaught school access reproduced on an exact isolated copy of operator v233: legacy draft has sourceState/sourceDefinitionRevision/sourceCatalogVersion, no sourceDocument. Accepted MVK source remains in the outer aggregate. [FAIL ledger](../../output/playwright/school-definition-authoring/UC-19/discard-legacy-20261012-1/README.md). Operator workspace/API process not mutated. Correct UI source lookup and add guarded explicit legacy restoration: require source state, preserved definition canonical revision, top-level revision, accepted result input revision, source catalog and school identity agreement; remove only the authoring subtree. Null/malformed/unknown/mismatched source still refuses409 without mutation. Normal recorded-source discard remains exact. Technical and changed-build walkthrough pending; historical approval evidence preserved.

### 2026-10-12 — UC-19 correction technical and walkthrough PASS

33 unit/architecture +8 discard/persistence browser cases PASS, reordered legacy fixtures and8 refused malformed/mismatched sources. Whole actual MVK copy restored exactly via independently verified kernel source revision; authoring raw JSON hash differs because kernel normalizes entity/set order. Repeat4 actor journey PASS: legacy v233→234, modern source re-entry/discard, initial/EMPTY, null-source409, no uncaught errors,8 inspected images. Actual operator8080 dialog/cancel PASS and complete v233 unchanged. Fixed build `/tmp/discard-authoring-fixed-r2.jar` restored stopped8080 using existing local database; no migrations/data changes, original browser working values preserved. Final approval ledger being recorded; earlier failure/interrupted evidence retained.

### 2026-10-12 — UC-19 legacy correction automatic approval

Submission `726774d`, [complete repeat4 PASS ledger](../../output/playwright/school-definition-authoring/UC-19/discard-legacy-20261012-4/README.md).33 unit/architecture +8 discard/persistence cases PASS. Exact actual MVK legacy source restored/reloaded in isolated copy v233→234; modern accepted, initial,EMPTY and explicit null-source refusal PASS;8 inspected screenshots, zero uncaught errors. Actual8080 confirmation/cancel preserves entire operator v233; corrected local runtime retained, user's original browser/working values untouched. Source revision is independently kernel-verified, not authoring JSON hash; initial/canonical-order and refusal regressions strengthened. Failed/interrupted attempts immutable. All20 approved, no outstanding handoff/blocker; original final regression coverage remains applicable and new affected checks PASS.


### 2026-10-12 — authorized UI refinements

User accepted the review recommendations and requested implementation. Added UC-21–UC-23 to the existing specification; UC-1–UC-20 approvals/evidence remain intact. Base `b1fd27daad4b7866a6e50ba9f18273b7ea1d5bc2`; pre-existing untracked `.agents/skills/antfu-web-design-guidelines/` is unrelated and preserved. UC-21 IN_PROGRESS; UC-22/23 NOT_STARTED. Requires UC-1 is satisfied. Narrow inspection remains read-only; no lifecycle/data-model change or arbitrary bulk editing.

UC-21 PENDING_WALKTHROUGH: production-boundary administrator journey and filter supplement PASS in isolated18112; immutable source235 and exact saved236 recorded. Technical four-entity regression still running; approval waits for final result.

### 2026-10-12 — UC-21 APPROVED; UC-22 started

33 workspace unit/architecture +21 entity browser cases PASS. [Real copied-school journey/filter supplement and exact diff](../../output/playwright/school-definition-authoring/UC-21/ux-20261012-1/README.md): duplicate names/diacritics, bounded pages, qualification/capability/issues filters, natural class sorting, keyboard, Save/Stay, hidden open record, section round trips,390 read-only and working-value retention, full saved/source/Current comparison PASS. [Checkpoint](checkpoints/UC-21.md). Operator untouched. UC-22 IN_PROGRESS; UC-23 NOT_STARTED.

### 2026-10-12 — UC-22 APPROVED; UC-23 started

[182-requirement real-browser PASS and exact diff](../../output/playwright/school-definition-authoring/UC-22/ux-20261012-1/README.md);4 requirement browser cases +1 unit PASS. Scoped filtering, explicit preview/count save, canceled/aborted writes, original occurrence/source/Current fidelity, reload and1600/1280/700/390 no-overflow/read-only outcomes recorded. [Checkpoint](checkpoints/UC-22.md). UC-23 IN_PROGRESS.


### 2026-10-12 — UC-23 final verification

33 unit/architecture and22 affected browser cases PASS in final-repeat; corrected-focus repeat33+2 PASS. Preserved earlier case-sensitive reservation/test-hidden-ID failures and native-dialog setup interruption. Real attempt3 exposed queued-details expansion/focus race; synchronous expansion capture and detached-event guard added. Attempt4 behavioral PASS (237→245, source/Current exact), but visual inspection prompted concise class-reference help and bounded issue summary. Final changed-build complete walkthrough and focused regression running; approval awaits both.


### 2026-10-12 — UI refinements complete, UC-23 APPROVED

Final full repeat5 PASS: stable responsive/screenshots across all five tabs, keyboard picker and mode focus, exact invalid/unresolved field navigation/correction,50-slot grids, explicit saves245→253, complete definition restoration and source/Current/reload exact.33 unit/architecture +42 distinct browser regressions PASS across unaffected/corrected focused runs; final production-polish33+2 PASS. [Complete evidence/patch/runtime ledger](../../output/playwright/school-definition-authoring/UC-23/ux-20261012-5/README.md). Local8080 started with exact verified final package; complete operator saved235 unchanged, all five tabs checked in owned read-only browser, original user's browser untouched. No backend/schema changes. All23 approved, no handoff/blocker. Failed/superseded evidence retained; automatic approval only.


### 2026-10-12 — remaining-editor refinements authorized

User requested rework of Grid, Room policies, Occurrence details and preference explanations/UI. Added UC24–27 with existing explicit-save and fidelity boundaries; prior23 approvals and all current work preserved. Base b1fd27d plus immutable UC23 submission patch. Unrelated untracked design-guidelines skill preserved. Preference explanations grounded in actual kernel penalty functions. UC24 IN_PROGRESS;25–27 NOT_STARTED.


### 2026-10-12 — UC24 APPROVED; UC25 started

33 unit/architecture+7 grid+5 review cases PASS; full real copied50-slot grid journey2 PASS255→257, exact source/Current/restoration/reload, one inspector, complete atomic preview/generation/negative regression and responsive working retention. [Ledger/patch](../../output/playwright/school-definition-authoring/UC-24/remaining-20261012-2/README.md). Initial helper/VM interruptions retained and corrected without weakening values. UC25 IN_PROGRESS.

### 2026-10-12 — UC25 APPROVED; UC26 started

35-policy full repeat2 PASS261→264; exact restoration/source/Current/reload, filtered-room preservation, invalid target focus/correction and responsive working retention.33 unit/architecture+4 policies+2 shared UX cases PASS. [Ledger/patch](../../output/playwright/school-definition-authoring/UC-25/remaining-20261012-2/README.md). Earlier harness/layout findings retained. UC26 IN_PROGRESS.

### 2026-10-12 — UC26 APPROVED; UC27 started

Final full real521-occurrence repeat3 PASS267→270, explicit2-population across filters, incompatible grouping refusal, exact selected-only edit/full restoration/source/Current/reload, mixed-value display and responsive retention.33 unit/architecture+5 occurrences+2 shared UX cases PASS. [Ledger/patch](../../output/playwright/school-definition-authoring/UC-26/remaining-20261012-3/README.md). Earlier native-checkbox helper/resize race and superseded visual evidence retained. UC27 IN_PROGRESS.

### 2026-10-12 — UC27 technical and walkthrough PASS; final regressions

33 unit/architecture+3 preferences+2 shared UX cases PASS. Full copied-school preference journey1 PASS270→274: all7 grounded explanations/3 goal groups, exact default/explicit0/invalid1.5/explicit1/restoration, live2×5=10 calculation, linked correction focus, Stay/filter/narrow retention/source/Current/reload. Images inspected. Final affected-editor regressions add UI assertions for unavailable/duplicate/unsupported saved choices before approval.

Preference explanation polish explicitly identifies the home-room fallback capacity/capability conditions from PlanningMapper. Behavior unchanged; final walkthrough repeat2 will pin this complete copy.

### 2026-10-12 — complete affected-editor regression PASS

33 unit/architecture+51 browser cases across all11 affected editor classes PASS, zero failures/errors/skips. Unavailable/duplicate/unsupported preference UI assertions also pass. Review focus correction added after this run’s packaged snapshot; targeted6 Review+5 occurrence+2 shared UX repeat and full affected standalone repeats remain before final delivery. [Regression log/results](../../output/playwright/school-definition-authoring/final-remaining-20261012/README.md).

### 2026-10-12 — remaining refinements IMPLEMENTED; all27 APPROVED

Final exact-build policy35/occurrence521/preference7 full repeats PASS274→284. Cold Review link/target focus supplement PASS284→288, full definition restoration and source/Current/reload exact; inspected responsive/contact-sheet/focused-control images.33 unit/architecture+51 affected editor cases PASS; changed-focus suite retained12 passing cases and one new harness strict-selector ambiguity, corrected same owner/field diagnostic selector then all6 Review+33 repeat PASS.52 distinct affected browser cases pass, no remaining failure/skip. Original negative/durable assertions retained. [Final ledger/exact patch/runtime evidence](../../output/playwright/school-definition-authoring/final-remaining-20261012/README.md).

Final package on8080; full operator row/API exactly preserved at238 ACCEPTED_BASELINE,10 served assets exact, owned read-only browser PASS. Existing connection config derived internally; initial disposable-role assumption corrected without DB writes. Original user's browser untouched. Owned18112 server/two verification browsers closed, disposable evidence/data retained. No backend/schema change, no handoff/blocker, automatic approval. All intended changes uncommitted with immutable exact patch; unrelated skill preserved.
