# Use-Case Status: school-definition-authoring

## Current

- Use case: UC-3
- Status: APPROVED
- Next eligible: UC-4

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `d4a66f6` / [checkpoint](checkpoints/UC-1.md) | [convergence](convergence/UC-1.md) |
| UC-2 | APPROVED | UC-1 | `9b59583` / [checkpoint](checkpoints/UC-2.md) | [convergence](convergence/UC-2.md) |
| UC-3 | APPROVED | UC-1 | `a30de35` / [checkpoint](checkpoints/UC-3.md) | [PASS walkthrough](../../output/playwright/school-definition-authoring/UC-3/20261011-2/README.md) |
| UC-4 | NOT_STARTED | UC-1 | - | - |
| UC-5 | NOT_STARTED | UC-1 | - | - |
| UC-6 | NOT_STARTED | UC-1 | - | - |
| UC-7 | NOT_STARTED | UC-1 | - | - |
| UC-8 | NOT_STARTED | UC-1 | - | - |
| UC-9 | NOT_STARTED | UC-1 | - | - |
| UC-10 | NOT_STARTED | UC-1 | - | - |
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
