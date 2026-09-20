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
- [`timetable-workspace`](timetable-workspace/) contains the proposal for the administrator-facing product slice.

The approved `kernel-v1` evidence preserves path names recorded at the time of its original submissions, even though
the artifacts now live below `spec/kernel-v1/`.

Agent skills resolve one feature directory before reading or writing artifacts. When a request does not identify the
feature and more than one directory could apply, the agent must ask which feature is in scope.
