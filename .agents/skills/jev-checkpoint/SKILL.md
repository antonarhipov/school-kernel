---
name: jev-checkpoint
description: Semantically preflight one use-case checkpoint with Jev by grading each contract-evidence row before independent convergence. Use after drafting a feature checkpoint or when asked whether checkpoint evidence corresponds to its specification. Never use it as approval or as a substitute for tests and runtime reproduction.
---

# Jev Checkpoint Preflight

Use Jev to find overstated, incomplete, or boundary-misaligned evidence before `converge` audits one use case. Jev is an
advisory semantic reviewer. The repository's deterministic checks and independent convergence remain authoritative.

Pipeline position: execute -> checkpoint draft -> **Jev preflight** -> converge

## Boundary

Jev may classify the strength of supplied evidence. It cannot run code, inspect an unprovided file, establish that a
command really ran, prove absence of side effects, or approve a use case. Typed output guarantees the response shape,
not the truth of the judgment.

Keep facts that code can decide out of Jev: checkpoint completeness, Git status and commit identity, test exit status,
schema validity, exact values, counts, hashes, and file changes. Verify those deterministically.

## Inputs

Resolve the same `<feature-dir>` used by the executor. Never combine a specification, rules, or checkpoint from sibling
feature directories.

- The one target use case and related contracts from `<feature-dir>/spec.md`
- Applicable constraints from `<feature-dir>/rules.md`
- The draft `<feature-dir>/checkpoints/UC-n.md`
- Raw evidence behind each claim: assertion excerpts, runtime observations, boundary type, and negative obligations
- The immutable submission or exact diff when commits are prohibited

Do not grade several use cases together. Do not send the entire repository or a broad diff as state. Create one narrow
state object per contract element so unrelated context cannot distract the model.

## Credential and service rules

- Read the API key only from `TYPESAFE_API_KEY`. Never print, persist, or place it in a command argument.
- Use the pinned model `jev-1.13.0` unless the user explicitly authorizes a model change after recalibration.
- A Jev call sends its state to TypeSafe's API. Include only the minimum excerpts authorized for this verification.
- Treat API failure, missing credentials, rate limiting, or low confidence as `REVIEW`, never as a pass.
- Retry only transient `429` and `529` responses, at most twice after the initial request.

Before changing the integration, re-read the live [API](https://docs.typesafe.ai/api.md),
[model](https://docs.typesafe.ai/models.md), and
[jaggedness](https://docs.typesafe.ai/model-jaggedness/jev-1.13.md) documentation.

## Workflow

1. Run the ordinary deterministic checkpoint and repository checks first. A deterministic failure is already a finding;
   do not ask Jev to reinterpret it.
2. Create a JSON bundle following [references/bundle-format.md](references/bundle-format.md). Include every scenario,
   extension, guarantee, postcondition, relationship, and applicable rule as a separate item. Executor prose is a claim,
   not evidence.
3. Install the pinned helper dependencies with `npm --prefix .agents/skills/jev-checkpoint ci` when `node_modules` is
   absent or its lockfile changed. These dependencies belong to the skill, not the Java application.
4. Validate the bundle locally before any network call:

   ```bash
   npm --prefix .agents/skills/jev-checkpoint run jev-checkpoint -- \
     --bundle <bundle.json> --validate-only
   ```

5. Run the preflight and save its machine-readable report beside the checkpoint:

   ```bash
   npm --prefix .agents/skills/jev-checkpoint run jev-checkpoint -- \
     --bundle <bundle.json> \
     --output <feature-dir>/checkpoints/UC-n.jev.json \
     --model jev-1.13.0
   ```

6. Independently inspect every flagged item:
   - Confirmed `WEAK`, `MISPLACED`, `IMPOSSIBLE`, `ABSENT`, missing-negative-proof, or contradiction: revise the
     implementation/evidence before submission or record a convergence finding.
   - Low confidence or insufficient context: route to the human/reasoning verifier.
   - Demonstrable false positive: keep the underlying evidence and record why the Jev flag was not adopted.
7. Commit the Jev report with the checkpoint when it was part of execute. Invoke `converge` regardless of whether Jev
   flagged anything. Converge must obtain its own evidence and must not copy Jev's judgment as a finding.

## Decision rule

Jev can only escalate review. It cannot produce `APPROVED`, `APPROVED WITH NOTES`, or `READY_FOR_CONVERGENCE`.
A result with no flags means "no issue detected in the supplied excerpts," not "the implementation matches the spec."

Until thresholds are calibrated on this repository's historical submissions, treat the report as pilot telemetry. Use
the rejected submissions and later convergence findings listed in the bundle reference as the initial labeled set.
