# Feature specifications

Each product effort owns a directory under `spec/`. Artifacts from different proposals must not share a status ledger,
rules file, checkpoints, or convergence reports.

```text
spec/<feature>/
  proposal.md
  decisions.md        # when proposal decisions need a separate record
  spec.md             # use-case specification
  rules.md            # technical rules
  status.md            # execution ledger
  checkpoints/        # executor evidence
  convergence/        # independent verdicts
```

Current feature directories:

- [`kernel-v1`](kernel-v1/) contains the approved headless scheduling and replanning runtime.
- [`timetable-workspace`](timetable-workspace/) contains the approved first administrator-facing product slice.
- [`ux-evolution`](ux-evolution/) contains the non-normative, forward-looking timetable-workspace UX
  [`roadmap proposal`](ux-evolution/proposal.md) and its cross-feature [`design draft`](ux-evolution/spec.md); executable
  behavior is extracted into independently gated feature directories.
- [`timetable-inspection-ux`](timetable-inspection-ux/) contains the first independently gated feature extracted from
  that roadmap: the focused [`proposal`](timetable-inspection-ux/proposal.md) and declarative
  [`specification`](timetable-inspection-ux/spec.md), plus its confirmed [`technical rules`](timetable-inspection-ux/rules.md),
  for accepted-timetable Week/Day inspection.
- [`kernel-schedule-quality`](kernel-schedule-quality/) specifies class-day gaps and weekly load balance for the next
  scheduling catalog while retaining version 1 baselines.
- [`kernel-cohort-start-quality`](kernel-cohort-start-quality/) specifies the catalog 3 late-start measure while
  preserving catalog 1 and 2 definitions and results.
- [`kernel-cohort-daily-balance`](kernel-cohort-daily-balance/) specifies the catalog 4 cohort-level daily-spread target
  while preserving the earlier weekly-balance meanings.
- [`kernel-cohort-no-gaps`](kernel-cohort-no-gaps/) specifies the catalog 5 hard cohort-gap constraint and per-cohort
  `maxDailyGaps` allowance, superseding the soft-only gap handling in catalog 4.
- [`kernel-reserved-periods`](kernel-reserved-periods/) specifies school-wide period reservations for ordinary
  planning and repair, with MVK's five `*-0` periods reserved.

The approved `kernel-v1` evidence preserves path names recorded at the time of its original submissions, even though
the artifacts now live below `spec/kernel-v1/`.

Agent skills resolve one feature directory before reading or writing artifacts. When a request does not identify the
feature and more than one directory could apply, the agent must ask which feature is in scope.
