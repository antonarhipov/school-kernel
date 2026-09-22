# Timetable UX Polish

## Status

Draft for product review. This proposal does not change the approved timetable-workspace lifecycle, the accepted
timetable, or the status of `timetable-inspection-ux` use cases.

The visual and interaction reference is
[`ui/timetable-administrator-workbench-prototype.html`](../../ui/timetable-administrator-workbench-prototype.html).
The reference establishes the intended composition and hierarchy; its illustrative data does not authorize unsupported
kernel behavior.

## Purpose

Turn the existing timetable screens into one coherent administrator workbench. The whole-school timetable remains the
dominant surface while the administrator inspects the current timetable, stages repair intent, waits for a repair,
reviews proposal impact, and decides whether a proposal becomes current.

This proposal implements the persistent-canvas direction described as UX-5 in
[`ux-evolution`](../ux-evolution/proposal.md). It preserves the Week/Day inspection behavior already implemented by
[`timetable-inspection-ux`](../timetable-inspection-ux/proposal.md), but carries that behavior consistently through the
Draft and Proposal workflow.

## Why another UX increment is needed

The accepted-timetable inspection increment successfully introduced a complete Week overview, precise Day view,
compact lesson tiles, subject and teacher investigation, honest teacher availability, filters, and focused schedules.
It did not implement the complete `ux-evolution` workbench.

The current production presentation still changes shape at lifecycle boundaries:

- Current shows the new Week/Day inspection surface, but export and repair-entry controls precede the timetable;
- Draft becomes a separate Day-only repair form and matrix;
- Solving exposes the accepted timetable beside run status, but does not establish one continuous workbench contract;
- Proposal becomes a report of categories and before/after cards, with links that temporarily replace the report to
  show timetable context;
- lesson details appear below the timetable instead of in a persistent contextual inspector.

Each screen can represent its state correctly, but the journey feels like moving between forms and reports rather than
operating one timetable. This is the missing structural quality identified when comparing the implementation with the
prototype and the original UX hypothesis.

## Product hypothesis

An administrator can understand and trust a timetable repair more easily when:

1. Current, Draft, Solving, and Proposal are explicit modes of one workbench;
2. the same whole-school Day/Week canvas remains visible and spatially stable across those modes;
3. inspection, repair intent, conflicts, protection, and proposed changes appear as layers on that canvas;
4. one side inspector explains the selected lesson, active repair rule, pin, conflict, or proposal change;
5. import, export, diagnostics, and destructive actions remain available without displacing the operational timetable;
6. the accepted timetable is unmistakably current until a proposal is explicitly and durably accepted.

## Design target

### Persistent workbench shell

The desktop workspace follows the prototype's composition:

- a compact header identifies the school, accepted revision, and authoritative lifecycle state;
- a state switcher presents available Current, Draft, Solving, and Proposal modes without inventing unavailable state;
- a compact toolbar holds Day/Week, weekday, subject, teacher, search, and applicable narrowing controls;
- the whole-school timetable occupies the primary content area;
- a contextual side inspector contains details, intent, explanations, and mode-specific actions;
- secondary operations such as import, export, diagnostics, and destructive reset are placed outside the primary scan
  path, using disclosure or a secondary utility area.

The production implementation should adopt the prototype's hierarchy and visual rhythm rather than reproduce every
prototype pixel. Existing safe native controls, message catalog, dependency-free assets, and authoritative display
metadata remain required.

### Current mode

Current mode retains the accepted `timetable-inspection-ux` contract:

- Week is the complete whole-school overview and Day is the precise operational matrix;
- subject and teacher investigation, teacher availability, search, explicit filters, focused schedules, and exact
  lesson selection remain available;
- selecting a lesson opens its accepted details in the side inspector without moving the administrator below the
  canvas;
- starting a repair is an intent action associated with the timetable context, not a large form that pushes the
  timetable down the page;
- export remains available as a secondary operation.

### Draft mode

Draft mode keeps the same Day/Week canvas and inspection controls. Staged teacher or room unavailability appears as an
overlay on accepted lessons:

- directly affected lessons and blocking conflicts use distinct text and non-color cues;
- selecting an affected lesson explains the accepted assignment and the staged rule in the inspector;
- period and room pins can be applied or removed from the selected lesson in context;
- bulk pinning and draft summaries remain available through the inspector or secondary panels;
- the header always states that the draft has not changed the accepted timetable;
- solve, discard, and conflict-resolution actions do not replace the canvas.

### Solving mode

While a repair is running, the accepted timetable stays available for inspection. Draft mutation and proposal actions
are frozen, run state and cancellation are presented in the inspector, and the workbench continues to identify the
accepted timetable as current.

### Proposal mode

Proposal review uses the same timetable canvas rather than making the impact report the primary page:

- accepted and proposed placement remain distinguishable;
- direct effects, ripple effects, protected assignments, additions, and cancellations use explicit non-color cues;
- selecting a changed lesson opens exact before/after teacher, period, room, class, and subject details;
- category totals and grouping summaries support the canvas from the inspector or a secondary review panel;
- unchanged lessons remain visible but visually quiet;
- accept, revise, and discard actions remain adjacent to the proposal explanation;
- only a successful explicit acceptance advances Current.

### Focused schedules and narrow screens

Class, teacher, and room schedules remain drill-downs from the whole-school context. Returning restores the prior
range, selection, investigation, filters, mode, and representable scroll context.

Narrow screens remain read-only Day or focused agendas. They identify Current, Draft, Solving, or Proposal honestly
but do not expose repair mutation or proposal acceptance and do not claim to provide the complete desktop workbench.

## Scope

- Recompose the existing workspace into the persistent shell represented by the prototype.
- Reuse one accepted-snapshot model and one presentation-state authority across Current, Draft, Solving, and Proposal.
- Preserve the approved Week/Day, highlighting, filtering, availability, focused-schedule, and selection semantics.
- Move lesson, intent, pin, conflict, run, and proposal explanations into one contextual inspector.
- Render supported draft conflicts and proposal changes as overlays on the whole-school canvas.
- Keep existing lifecycle, optimistic concurrency, kernel invocation, feasibility verification, atomic acceptance,
  recovery, and export guarantees unchanged.
- Resolve presentation regressions and run the complete shared browser suite before convergence.

## Explicit non-goals

- cohort partitions, group targets, or split-slot rendering until an approved kernel contract supplies authoritative
  group metadata;
- primary-room policies, policy exceptions, or policy provenance editing;
- new repair instruction types or changes to solver semantics;
- direct drag-and-drop mutation of accepted assignments;
- a new backend projection, timetable query API, database read model, lifecycle state, or workspace document field;
- a frontend framework, bundler, client state library, remote asset, or runtime dependency;
- mobile repair editing or proposal acceptance;
- copying illustrative prototype abbreviations, groups, or data into production when they are not authoritative.

## Relationship to existing features

- `timetable-workspace` remains authoritative for lifecycle, persistence, repair intent, proposal generation, review,
  acceptance, and export behavior.
- `timetable-inspection-ux` remains authoritative for accepted Week/Day inspection and presentation-state semantics.
- This feature is a presentation and interaction successor where those capabilities currently render as separate
  screens. It must consume their production paths rather than duplicate them.
- Split groups and primary-room policies remain separate later features from the `ux-evolution` roadmap.

## Validation

Use the complete validation-scale accepted snapshot and a repair journey containing direct effects, blocking conflict,
protected assignments, a feasible proposal, and solver ripple effects.

Automated real-browser verification must prove that the administrator can:

- move between Current, Draft, Solving, and Proposal without losing the represented Day/Week context where valid;
- inspect the same accepted lesson identity in every mode;
- distinguish accepted state, staged intent, direct conflict, pin protection, proposed placement, and ripple effect
  without relying on color;
- follow a changed lesson between old and proposed placement without leaving the workbench;
- use class, teacher, and room drill-downs and return to the retained whole-school context;
- confirm that inspection and mode switching cause no durable mutation;
- confirm that failed solve, cancellation, discard, and failed acceptance preserve the accepted baseline exactly;
- confirm that successful acceptance alone advances Current and refreshes the workbench to the new accepted revision.

The complete shared browser regression suite must pass. A focused test or visually plausible screen is not sufficient.

Administrator validation must use the task and participant evidence required by the eventual specification. A single
unstructured `PASS`, `LGTM`, or `looks good` response cannot stand in for a multi-participant gate when the specification
requires participant count, school distribution, per-task outcomes, errors, and accepted-state comprehension.

## Entry conditions and known evidence debt

Before this feature is converged:

1. `timetable-inspection-ux` UC-2 must be reconverged after the complete `WorkspaceBrowserIT` suite passes. The current
   integrated suite contains a stale UC-2 empty-filter text assertion after the UC-3 wording change.
2. The inspection feature's status and blocker sections must agree about any remaining `NEEDS_REVISION` use case.
3. Existing walkthrough evidence must be described at the strength actually recorded; undocumented participant
   distribution or task results must not be inferred from a general approval response.

These items are not reasons to redesign the approved inspection semantics. They are baseline proof obligations that
must not be hidden by the polish work.

## Recommended delivery sequence

### P-1 — Persistent shell and Current mode

Adopt the prototype hierarchy, make the timetable the dominant surface, move details into the side inspector, and
retain all accepted inspection behavior.

### P-2 — Draft and Solving continuity

Carry the same model, range, investigation state, and canvas into Draft and Solving. Move supported intent, conflicts,
pins, run status, and cancellation into contextual overlays and the inspector.

### P-3 — Proposal canvas review

Render accepted/proposed context, direct/ripple cues, protected assignments, exact before/after details, and decision
actions without replacing the timetable with a report.

### P-4 — Cross-mode convergence

Run the complete browser, security, lifecycle, persistence, failure, and scale regressions, then perform the derived
administrator walkthrough against the production implementation.

Each increment should be specified, implemented, and converged independently. No increment may weaken accepted-state
immutability or claim unsupported group or policy behavior merely to resemble the prototype.

## Decisions to confirm during specification

1. Whether the state switcher permits navigation back to Current while a Draft or Proposal exists, or only reports the
   lifecycle state while the canvas supplies accepted/proposed layers.
2. Whether the contextual inspector is fixed, resizable, or collapsible at supported desktop widths.
3. Which secondary utility placement best keeps import/export and destructive actions discoverable without competing
   with the timetable.
4. Whether proposal comparison defaults to overlay, old/proposed toggle, or a combined presentation with per-change
   before/after detail.
