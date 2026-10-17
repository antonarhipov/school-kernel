# Feature specifications

Each product effort owns a directory under `spec/`. Artifacts from different proposals must not share a status ledger,
rules file, checkpoints, or convergence reports.

```text
spec/<feature>/
  proposal.md
  decisions.md        # when proposal decisions need a separate record
  spec.md             # use-case specification
  rules.md            # optional feature-specific technical constraints
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
- [`kernel-day-edges`](kernel-day-edges/) specifies catalog 6 per-cohort start bounds and per-subject placement
  rules, including the first exception to a period reservation (MVK's Õpiabi in slot 0).
- [`kernel-cohort-daily-spread`](kernel-cohort-daily-spread/) specifies the catalog 7 hard per-cohort daily lesson
  spread limit and the one-minute workspace run it requires for MVK.
- [`kernel-cohort-curator`](kernel-cohort-curator/) specifies catalog 8 cohort curators, home rooms, and curator
  lessons such as Klassitund, which the curator teaches in the home room.
- [`kernel-room-assignment`](kernel-room-assignment/) specifies catalog 9 reusable hard subject and teacher-subject
  room assignments, allowed-room sets, and cohort-home-room targets.
- [`kernel-reserved-periods`](kernel-reserved-periods/) specifies school-wide period reservations for ordinary
  planning and repair, with MVK's five `*-0` periods reserved.
- [`timetable-manual-editing`](timetable-manual-editing/) records the original manual assignment-editing specification.
  Current manual editing supports period and room changes, durable drafts, conflict explanations and publication.
  [School-definition authoring UC-20](school-definition-authoring/spec.md#uc-20---find-the-supported-route-for-a-teacher-change)
  supersedes its teacher-change path: teaching responsibility is authored in the definition; saved legacy manual
  teacher differences block publication until explicitly reverted or discarded.
- [`school-definition-authoring`](school-definition-authoring/spec.md) specifies the
  [proposal](school-definition-authoring/proposal.md) for full supported definition authoring from an empty workspace,
  an initial definition, or an accepted baseline. Durable drafts cover weekly requirements, all supported settings,
  and verified finish into initial planning or seeded repair with explicit acceptance.
  **IMPLEMENTED:** all 27 use cases approved through recorded actor walkthroughs, including school-scale editor refinements;
  [status and final regression evidence](school-definition-authoring/status.md).
- [`timetable-matrix-lenses`](timetable-matrix-lenses/) proposes turning the Teacher and Room filters into lenses that
  pivot the whole-school Week/Day matrix to one teacher or room, superseding the separate focused schedule surface.
- [`demo-deployment`](demo-deployment/) proposes a public, no-login demo profile of the workspace on Railway, with
  in-memory per-visitor workspaces and an asynchronous, bounded solve queue offering Quick and Thorough budgets.
  Instead of structured authoring, visitors pick a pseudonymized sample school from a dropdown and can edit its JSON;
  edited versions are saved under an identifier in the browser's local storage and join the dropdown. Persistence,
  accounts, and a hosted Timefold solver remain later, demand-driven steps.
- [`school-setup-editor`](school-setup-editor/spec.md) specifies a fresh structured editor for the current demo
  ([proposal](school-setup-editor/proposal.md)):
  grid-first setup, class-owned weekly requirements, explicit teacher eligibility, contextual scheduling rules,
  autosaved drafts and named school copies. It includes new kernel support for explicit consecutive lesson blocks
  of two or more lessons, with consecutive lessons forbidden by default under the new rules;
  real-product definition versioning and activation are future requirements. The earlier authoring feature remains
  a separate historical implementation.

The approved `kernel-v1` evidence preserves path names recorded at the time of its original submissions, even though
the artifacts now live below `spec/kernel-v1/`.

Agent skills resolve one feature directory before reading or writing artifacts. When a request does not identify the
feature and more than one directory could apply, the agent must ask which feature is in scope.
