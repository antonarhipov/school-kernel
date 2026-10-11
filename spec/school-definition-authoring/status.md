# Use-Case Status: school-definition-authoring

## Current

- Use case: UC-1
- Status: PENDING_WALKTHROUGH
- Next eligible: none until UC-1 convergence and walkthrough

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | PENDING_WALKTHROUGH | none | `06c0201` / [checkpoint](checkpoints/UC-1.md) | [convergence](convergence/UC-1.md) |
| UC-2 | NOT_STARTED | UC-1 | - | - |
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
- Verdict: PENDING_WALKTHROUGH; no implementation findings.
- User walkthrough result: not yet received. Isolated workspace: http://localhost:18081/workspace/; exact script and environment lifecycle recorded in convergence report.
- Full unrelated reactor/browser/solver suites were not rerun or claimed green; user requested slow checks only when necessary.
