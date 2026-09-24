# Timetable Workbench Layout

## Status

Draft proposal for product review. This is a presentation successor to
[`timetable-ux-polish`](../timetable-ux-polish/proposal.md), not a change to its recorded use-case verdicts.
At the time of this proposal, its UC-5 is `PENDING_WALKTHROUGH`; the required administrator feature gate has not
been confirmed. This proposal does not claim that gate has passed.

The visual reference is the
[`timetable administrator workbench prototype`](../../ui/timetable-administrator-workbench-prototype.html).
Its composition and density are useful targets. Its example lessons, short labels, split groups, and styling code
are illustrative rather than authoritative production data or implementation assets.

## Purpose

Make the existing timetable workbench comfortable for sustained desktop use. The complete whole-school timetable
should command the available screen space, and an administrator should be able to prepare and review a repair without
working through a long, narrow sidebar. The current lifecycle and safe repair workflow already exist; this proposal
changes their layout and interaction hierarchy.

## Evidence and problem

The prototype places school and state navigation in a short top bar, Day/Week and highlights in a compact toolbar,
and a dense five-day timetable beside a small contextual summary. At a 1600 x 900 browser viewport, its timetable
occupies most of the visible width and begins near the top of the page.

The production [styles](../../timetable-workspace/src/main/resources/static/workspace/styles.css) currently limit
`header` and `main` to 1440 px and give the page header 52 px of top padding. The page repeats school and state
context inside a padded card. A large inspection toolbar, a separate investigation
panel, status rows, and introductory text precede the matrix. Its compact Day cell is at least 78 px tall and each
Week period slot is at least 50 px tall, compared with the prototype's illustrative 49 px Day cell and 27 px Week
period slot. These dimensions are evidence of the gap, not values to copy without testing real school names.

The production inspector is fixed at 300 px. The current
[workbench rendering](../../timetable-workspace/src/main/resources/static/workspace/app.js) puts the selected lesson,
staged change, period choices, conflicts, individual and bulk protection, history, discard, and generation controls
in that column during Draft. In Proposal,
it also contains category lists, groupings, exact comparisons, and acceptance decisions. Its width is appropriate
for a selected lesson and short summary, but its contents are a complete workflow. Making its text smaller would
leave the underlying layout problem in place.

## Product hypothesis

Administrators will scan and repair a whole-school timetable more easily when:

1. the desktop shell uses the available width and puts the timetable directly below a compact header and toolbar;
2. lesson cells convey subject, room, and state at a density close to the prototype without hiding authoritative
   detail or reducing controls to illegible targets;
3. the side inspector answers “what is selected and why does it matter?”;
4. a separate, wide task area answers “what am I changing, protecting, reviewing, or deciding?”;
5. the timetable remains visible and responsive while that task area is in use; and
6. Current remains unmistakably accepted until explicit, durable proposal acceptance.

## Recommended desktop composition

```text
School and accepted state  |  Current / Draft / Solving / Proposal  |  Utilities
Day / Week  |  weekday  |  search  |  subject highlight  |  teacher highlight  |  Filters

Whole-school timetable canvas                         |  Selected lesson / concise context

Wide task area, open when needed:
  Draft: intent and periods  |  effects and conflicts  |  protection and preview  |  actions
  Proposal: change navigation  |  exact before/after review  |  decision actions
```

This is a relationship sketch, not a pixel specification. The preferred task area is a dock beneath the canvas,
within the same desktop workbench. It uses the available width, has its own scroll region when necessary, and never
covers timetable cells. When open, the canvas retains a usable minimum height and its own scroll position. The dock
can be collapsed when the administrator returns to scanning. Its precise height and breakpoints must be settled
against real content at 1280 x 800 and 1600 x 900, including long school-controlled names.

### Compact shell

- Give the accepted-workbench shell near-full viewport width with modest outer gutters. Keep the empty/import and
  initial-planning journeys separate; they need not inherit this operational layout.
- Show school identity, accepted revision, lifecycle status, and available presentation modes once, in a compact top
  bar. Keep the distinction between the accepted Current and an unaccepted Draft or Proposal explicit.
- Keep Day/Week, selected weekday, search, subject highlight, and teacher highlight directly visible. Put class,
  teacher, room, and period narrowing in a labelled Filters disclosure with an always-visible active-filter summary
  and clear action. Highlighting remains non-destructive; explicit filters still narrow.
- Keep export and applicable diagnostics in Utilities. Keep lifecycle actions in their relevant task area.
- Preserve the existing single snapshot, presentation-state owner, message catalog, native controls, and safe text
  rendering. Rearranging controls must not turn mode or inspection navigation into a workspace mutation.

### Timetable canvas

- Use one fixed compact tile design for Day and Week. Reduce excessive cell height, padding, and class-column width
  through real-browser comparison with the prototype at operational desktop widths.
- Keep the room visibly in every occupied tile and the subject as its leading label. Full subject, teacher, class,
  room, period, and stable lesson identity remain available in the accessible name and on selection. Do not invent
  abbreviations from opaque IDs.
- Keep the complete school population, definition-ordered weekdays and periods, honest empty structure, sticky
  class/time context, overlays, selection, and class/teacher/room drill-downs. No visual compaction may erase a
  lesson, misstate a total, or make a filtered subset appear complete.
- Preserve distinct non-color cues for subject and teacher investigation, conflict, pin, accepted origin, proposed
  destination, direct effect, ripple effect, and selection. Real long labels and combined cues determine the minimum
  readable size; the prototype's exact measurements are not a mandate.
- Preserve split-group behavior only when an approved versioned kernel contract supplies group identities. The
  prototype's example split tiles are not a reason to infer groups from names or coincident assignments.

### Context inspector and repair task area

The inspector stays a stable, collapsible desktop region for the selected lesson, its accepted details, relevant
draft/proposal cues, and a short state summary. Collapsing it expands the canvas and retains selection. Explicit
selection reopens it. It does not contain the full period picker, bulk workflow, proposal category lists, or all
decision controls.

Starting a repair opens the task area with teacher/room, resource, and recurring period choices laid out across its
width. In Draft, it shows the staged intent, direct effects and blocking conflicts, individual and bulk protection,
preview and undo, readiness, and solve/discard controls in adjacent sections. Selecting a conflict or protected
lesson links to the same lesson on the canvas and in the inspector. A blocked or unsaved draft cannot generate a
repair; opening or closing the task area does not alter durable intent.

In Solving, the editing sections become read-only. A compact task-area status shows the frozen intent, run limit,
safe diagnostics, and cancellation. The accepted timetable remains responsive. Failure or cancellation returns to
the existing Draft controls without presenting a candidate as Current.

In Proposal, the task area supports the combined canvas overlay with navigable change categories and groupings,
unique and overlapping counts labelled honestly, protected lessons, exact before/after values, and accept/revise/
discard decisions. Selecting an origin or destination keeps one stable lesson identity and exposes both sides.
The decision controls remain visible in the review area without requiring a scroll through a 300 px report column.
Failed acceptance leaves the old Current and reviewable Proposal; only explicit, durable success advances Current.

Current opens with the task area collapsed. Entering or reloading Draft or Proposal opens the relevant task area so
the next task is discoverable; an administrator may collapse it to inspect more of the timetable. Mode changes retain
representable Day/Week, filters, highlights, selection, focused context, and canvas scroll under the existing
presentation-state contract. Task-area open state is presentation-only and never stored in the workspace document.

### Smaller widths

Supported desktop widths must retain a usable whole-school canvas and task area. As width falls, task-area sections
may stack within the wide dock, rather than becoming a single narrow form. The existing narrow-screen boundary
remains a read-only Day or focused agenda: it identifies the lifecycle honestly and offers no repair mutation,
run cancellation, or proposal decision.

## Scope and boundaries

This feature changes the packaged `/workspace/` presentation and its interaction layout. It reuses the existing
accepted model, repair draft, run, proposal comparison, APIs, and guarded mutations. It must preserve accepted-state
immutability, proposal identity checks, CSRF/`If-Match`, kernel verification, cancellation and failure handling,
atomic acceptance, and the local access boundary.

It does not add scheduling rules, resource types, persistent room policies, group metadata, a second timetable
model, a new API or database field, accounts, mobile editing, drag-and-drop assignment mutation, a frontend
framework, remote assets, or a density toggle. It does not redesign initial import or initial planning.

This proposal deliberately supersedes the *placement* choices in `timetable-ux-polish` that put full Draft and
Proposal workflows into the fixed inspector. A later specification must update those use-case presentation steps
and RULE-7's inspector checks for this feature, while retaining the accepted behavioral and safety guarantees.
It must not silently rewrite or retroactively mark the earlier feature approved.

## Recommended delivery sequence

1. **Shell and canvas:** compact the top-level layout and tiles, keep the complete Week/Day inspection semantics,
   and compare real screenshots at the two desktop widths and the narrow read-only boundary.
2. **Draft and Solving task area:** move repair entry, period selection, conflicts, pinning, bulk preview, run state,
   and cancellation out of the inspector. Keep one live canvas and the same durable actions.
3. **Proposal task area:** move change navigation, exact comparison, and decisions out of the inspector while
   preserving the existing combined overlay and atomic acceptance path.
4. **Complete journey and administrator validation:** exercise Current -> Draft -> Solving -> Proposal -> Current,
   failures and retries, a second repair, and visual comfort at realistic scale. Converge each specified use case
   before progressing under the repository's use-case workflow.

## Validation target

Use the existing generated or properly anonymized validation-scale school: at least 60 classes, 100 teachers,
100 rooms, and 900-1,100 accepted lessons, with long display names, empty positions, a blocking conflict, individual
and bulk protection, and a feasible proposal with direct and ripple changes. Use the same fixture for before/after
screenshots at 1280 x 800 and 1600 x 900. At 1600 px, all five weekdays in the validation fixture should fit the
canvas without internal horizontal scrolling; at both desktop sizes, no control or label needed to complete a repair may
be clipped, and the canvas must stay visible while the task area is open. Record visible class rows, scroll burden,
and label legibility rather than claiming success from a CSS dimension alone.

Real-browser checks must demonstrate that administrators can stage and revise the same supported disruption, find
and resolve a blocking conflict, preview and apply protection, follow or cancel a run, compare direct and ripple
changes, and deliberately accept a proposal without losing the accepted timetable context. Verify exact stored state
before and after each consequential action, unchanged state on presentation changes, the complete shared browser
regression, native keyboard access, and the narrow read-only boundary.

For administrator validation, use the existing six-task UC-5 journey as a baseline and record task outcomes,
corrections, Current-state answers, and concrete layout friction from five timetable administrators across at least
three schools. The layout should be revised if the task area or canvas prevents those users from completing the
journey comfortably; a general approval comment or automated browser pass is not that evidence.

## Decisions to settle in the specification

1. The dock's minimum usable canvas height, maximum open height, and exact behavior at the supported desktop
   breakpoint, based on both target viewports and long-name fixtures.
2. Whether the selection inspector remains visible alongside the canvas while the task area is open at 1280 px, or
   becomes a compact selection summary that can be reopened without losing the draft/review task.
3. Which filters remain in the toolbar when active and how the Filters disclosure communicates narrowed versus
   complete school population without adding a third permanent toolbar row.
4. How a selected proposal change in the task area navigates to its accepted origin and proposed destination while
   preserving one lesson identity, both-side explanation, and representable Day/Week context.
