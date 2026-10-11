# Use-Case Status: school-definition-authoring

## Current

- Use case: UC-2
- Status: APPROVED
- Next eligible: UC-3 in document order

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `d4a66f6` / [checkpoint](checkpoints/UC-1.md) | [convergence](convergence/UC-1.md) |
| UC-2 | APPROVED | UC-1 | `9b59583` / [checkpoint](checkpoints/UC-2.md) | [convergence](convergence/UC-2.md) |
| UC-3 | NOT_STARTED | UC-1 | - | - |
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

None.

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
