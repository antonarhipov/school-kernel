# Demo startup status

| Use case | Status | Dependencies | Evidence |
| --- | --- | --- | --- |
| UC-1 | APPROVED | None | [Checkpoint](checkpoints/UC-1.md), [execution ledger](../../output/execution/demo-startup/ledger.md) |
| UC-2 | APPROVED | UC-1 | [Checkpoint](checkpoints/UC-2.md), [deployment ledger](../../output/execution/demo-startup/railway-native-20261018/ledger.md), [public Chrome](../../output/playwright/demo-startup/UC-2/railway-native-20261018-2/ledger.md) |

- 2026-10-18: Started from `b9458eaa7c2957d8766a33b481949975e255ab90`. Pre-existing modification:
  `tools/demo/__pycache__/check_packaged_samples.cpython-314.pyc`; preserve it. GraalVM CE 25.0.1 is installed at
  `~/.sdkman/candidates/java/25.0.1-graalce`. The initial sandboxed JVM probe could not bind a loopback socket;
  it is not a startup performance result. No source changes preceded that probe.

- 2026-10-18: APPROVED after actual ARM64 native build, three-launch comparisons, HTTP fidelity/isolation/export and
  separate-kernel verification/review, refusal non-mutation, startup-failure and restart checks. ARM64 medians:
  1.270 s JVM / 0.081 s native framework time; 1.5559 s / 0.1000 s launch-to-HTTP. All 197 existing unit tests pass
  with JVM attach access. The user's SDKMAN Java/GraalVM installations are Intel binaries; the build uses official
  ARM64 GraalVM CE 25.0.1 extracted only under `/tmp`. No SDKMAN installation, deployment or provider setting was
  changed. First-launch delays, failed harness/environment attempts and the nonfatal native Logback version warning
  are retained in the checkpoint/ledger. All disposable processes are stopped; no outstanding handoffs.

- 2026-10-18: User requested native Railway deployment. UC-2 adds that scope without reopening UC-1. Fresh reads
  locate existing `school-kernel-free-smoke / demo-smoke / production`, Free plan, one EU sleeping replica,
  1 CPU / 500 MB, existing domain and passing isolated-ingress preflight. All earlier worktree changes are preserved.

- 2026-10-18: Linux AMD64 native and JVM fallback containers build. Native container checks pass at 1 CPU / 500 MB,
  including real kernel verification/review, exact samples/export, two-owner isolation and SIGTERM. The first
  Rosetta-specific PID1 executable-path assertion was corrected in the harness; retained failure plus passing report
  are in [UC-2 ledger](../../output/execution/demo-startup/railway-native-20261018/ledger.md).
  First Railway upload `c6f5b281-35cb-4c29-803c-c346704b09af` failed during native analysis without a provider
  diagnostic; the previous JVM release remained in place. Exact-payload retry `15bb7d2d-6472-4f46-8197-d9c47c75ea6a`
  has progressed through analysis, universe, parsing and inlining. Public validation remains pending.

- 2026-10-18: **UC-2 APPROVED**. Identical-payload Railway retry `15bb7d2d-6472-4f46-8197-d9c47c75ea6a`
  succeeds. Public60-asset/eight-timetable fidelity, foreign-Origin refusal, full real Chrome generation/save/reload
  and two-owner isolation pass. Two same-image restarts confirm native PID1, sole demo profile and unchanged artifact;
  logged startup 0.141/0.154s versus earlier deployed JVM 4.717/4.586/4.858s. Final health UP, deployment SUCCESS;
  Free plan, complete service manifest,1 CPU / 500 MB, one sleeping EU replica and ingress/domain unchanged.
  Initial infrastructure build failure, Rosetta assertion, missing-confirmation harness refusal and nonfatal Logback
  package-version warning retained in checkpoint/ledger. Final restart clears disposable test state; no handoff or
  commit remains requested. All unrelated work preserved.
