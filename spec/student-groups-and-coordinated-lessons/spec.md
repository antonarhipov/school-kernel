# Student Groups and Coordinated Lessons

## Feature summary

An administrator declares reusable, labeled splits of a class and the weekly teaching each audience receives.
The solver chooses eligible teachers, periods, rooms and compatible parallel combinations. English and
handicraft groups can exchange activities across periods or days without requiring a manually authored
rotation. Optional hard coordination narrows this freedom when a school needs a particular arrangement.
Subjects expose readable teaching arrangements by class, with group creation/reuse available from that
class-subject plan. Classes retains the reusable-group overview.

The timetable accounts for actual anonymous attendance, including crossing splits, whole-class teaching,
room capacity, consecutive lessons and student gaps. Compact cells show parallel lessons side by side;
inspection, repair, manual correction and printable details preserve the identity of each lesson.

This is the behavioral contract for [proposal.md](proposal.md), as resolved by D001–D035 in
[decisions.md](decisions.md). The original proposal's four-row schedule is superseded by the local
**Cross-day example**, which does not double-book audiences. The approved
[visual preview](design/group-cell-layouts.html) illustrates the cell treatment; its sample content and
forty-four-pixel height are not additional product requirements.

## Scope and resolved decisions

- D001–D003, D010–D012 and D025–D027: class-local reusable splits, positive group sizes, anonymous
  relationships and automatic compatible placement. Support two, three, four and larger configured group
  counts within the application's disclosed size limits. There is no student directory or assignment journey.
- D004–D009 and D034: internal student gaps are permitted by default, with a separate optimization priority
  above all ordinary preferences. Optional hard gap-free and selected strict coverage rules remain available.
- D013–D018 and D029–D032: whole-class and grouped weekly demand, inherited requirements with overrides,
  automatic eligible teachers with weekly continuity, optional fixed teachers or deliberate weekly portions,
  actual-audience room and scheduling rules, and workload indicators rather than workload constraints.
- D019 and D033: optional counted coordination; the solver chooses distinct occurrence pairings. Start
  together, same/different days, allowed days/periods and fixed slots are hard when enabled.
- D020–D024: existing compact cell dimensions, vertical divisions, stable constituent positions where
  possible, neutral untaught sections, a summary when sections cannot be used, and complete printable details.
- D007, D028 and D031: repair preserves weekly teacher allocations before times and rooms; manual moves
  affect the selected lesson, with an explicit review of any necessary combined change.
- D035: subject-facing class teaching arrangements in previews and editable copies; group creation/reuse
  preserves subject/class context, while available reusable groups do not imply grouped delivery.

The current [school setup editor](../school-setup-editor/spec.md),
[demo timetable experience](../demo-timetable-experience/spec.md) and
[timetable workspace](../timetable-workspace/spec.md) remain the surrounding journeys. This feature expands
their indivisible-class and preassigned-teacher boundaries only as specified below. It does not introduce
accounts, change landing behavior, or replace their storage, cancellation, lock or acceptance lifecycles.

For newly authored plans under this feature, the default hard maximum of zero internal gaps from the older
setup-editor contract is superseded by the soft policy in **Optimization priorities**. Existing explicit
hard gap settings and older supported rule versions retain their meaning. Existing preassigned teachers
remain hard fixed assignments until an explicit change to automatic selection. Adoption is observable,
never inferred from existing placements.

The small defaults needed to make the accepted decisions executable are defined in **Normative data** and
the consuming use cases: unavailable/lunch gap boundaries, counted coordination, repair identity, compact
fallback and faithful version handling. There are no unresolved product choices.

## Actors and domain terms

- **Administrator:** the existing school-setup and timetable actor. The public demo remains anonymous and
  browser-local; the normal workspace keeps its existing access and accepted-baseline boundary.
- **Kernel caller:** an external consumer of the published definition, generation, repair or review boundary.
  It can exercise these goals without the authoring UI.
- **Timetable service:** the supporting actor that obtains a kernel result and keeps its exact definition pair.
- **Class:** the parent audience with a declared positive student count.
- **Split / arrangement:** one complete partition of a class into labeled, nonempty groups. A split can be
  reused by multiple subjects. Matching names or sizes do not establish the same audience.
- **Anonymous membership block:** a disjoint positive-count intersection of the class's used arrangements.
  It represents students who attend together across those arrangements, without identifying any student.
  Blocks partition the class; groups are unions of blocks. This is distinct from a consecutive lesson block.
- **Audience:** the whole class or a declared group within it, resolved to anonymous membership blocks.
  Audiences overlap exactly when their block sets intersect.
- **Teaching plan:** weekly subject demand for an audience, its teacher policy and applicable rules.
  Whole-class and group portions together provide each attending block's required teaching.
- **Session:** one actual subject lesson occupying one teaching period, with one audience, one teacher and
  one room. A required consecutive block consists of multiple neighboring sessions, consuming existing demand.
- **Weekly teacher allocation:** the selected teacher for a stable audience/subject portion across the week.
  Deliberate sharing declares separately identified fixed-teacher portions.
- **Coordination round:** one matched occurrence from each selected activity, subject to the declared rule.
  An activity is a teaching plan's single session or an explicitly selected required consecutive block.
- **Internal gap:** an unoccupied available teaching period between a block's first and last attendance in
  an uninterrupted available part of a day. Counts use actual attendance across all subjects and arrangements.
- **Definition/result pair:** the exact definition snapshot and assignments reviewed against it, with rule
  version, identities, chosen teachers, audiences, periods, rooms and quality data kept together.
- **Fixed teacher / lock / attempt pin:** respectively a hard teaching policy, a retained assignment
  restriction, and an attempt-scoped restriction. Their existing distinct lifetimes continue to apply.

## Use-case map

An outcome required below can come from this UI, faithful import or restored supported data. A relationship
does not require the administrator to repeat an already available outcome. Includes also establishes an
execution dependency; no ordering beyond these relationships is prescribed.

Exactly these rows — no more, no fewer:

| ID | Actor goal | Primary actor | Relations |
| --- | --- | --- | --- |
| UC-1 | Declare reusable class splits | Administrator | None |
| UC-2 | Describe relationships between different splits | Administrator | Requires UC-1 |
| UC-3 | Declare whole-class and group teaching requirements | Administrator | Requires UC-1 |
| UC-4 | Set eligible teachers and weekly teaching policies | Administrator | Requires UC-3 |
| UC-5 | Constrain audience scheduling and coordination | Administrator | Requires UC-3 |
| UC-6 | Review grouping and scheduling readiness | Administrator | None |
| UC-7 | Save a named school snapshot | Administrator | Includes UC-6 at step 2 |
| UC-8 | Review exact audience and resource assignments | Kernel caller | None |
| UC-9 | Obtain a complete coordinated timetable | Kernel caller | Includes UC-8 at step 4 |
| UC-10 | Generate and keep a grouped school timetable (primary) | Administrator | Requires UC-7; Includes UC-9 at step 3 |
| UC-11 | Inspect parallel lessons and untaught audiences | Administrator | Requires UC-8 |
| UC-12 | Monitor student gaps and teacher workload | Administrator | Requires UC-8 |
| UC-13 | Review and accept a coordinated repair | Administrator | Requires UC-8; Includes UC-9 at step 4; Includes UC-8 at step 6 |
| UC-14 | Correct selected lessons manually | Administrator | Requires UC-8; Includes UC-8 at step 5 |
| UC-15 | Exchange and adopt definitions without losing meaning | Administrator or kernel caller | None |
| UC-16 | Print or export a readable grouped timetable | Administrator | Requires UC-8 |

## State models

### Authoring and generation

The draft has independent durability and readiness states. It can be retained while incomplete, invalid or
not yet checked. Editing a checked revision makes readiness unchecked; a successful check marks that exact
revision ready, while a failed check marks it blocked with issues. UC-1–UC-6 and UC-15 cover these transitions.
Draft retention failure leaves an explicit unsaved/recovery state; it never becomes a successful save claim.

UC-7 creates a new immutable named school snapshot from a ready revision. Later edits create a new working
revision, not a mutation of that snapshot. UC-10 moves an explicit request through running to checked
candidate, failed or cancelled. A checked candidate can be kept as a new named timetable or explicitly
discarded. Keeping requires successful validation and storage; a failed keep leaves the candidate retryable.
Late or stale completion cannot attach to a different revision, school or generation request.

The existing explicit draft recovery/discard and local artifact deletion operations also apply to these
extended artifacts. Startup continues to open the bundled default timetable, with retained work reached
explicitly through existing navigation. No new arrival or automatic-resume transition is introduced.

### Review, repair and manual correction

Review yields valid, invalid or validity-unestablished for an exact pair. Only valid pairs can become accepted
or be kept as named valid timetables. An invalid manual working copy remains editable and recoverable.

UC-13 starts from a valid accepted baseline, prepares a checked repair proposal, and either explicitly
accepts or discards it. Acceptance advances the baseline only after the exact proposal is checked and
durably accepted. Failure preserves the baseline and a retryable proposal. Workspace restart restores its
durable draft, not eligibility to accept an old proposal without a new attempt and review.

UC-14 creates a manual working copy from a valid pair. Edits can make it unchecked or invalid; review marks
the exact revision valid or invalid. Explicit keep/accept publishes only a valid revision under the host
profile's existing lifecycle. Discard abandons only the working copy.

All other transitions are refused with an explanation, preserving the current pair and durable draft.
UC-7, UC-10, UC-13 and UC-14 specify the observable refusal and recovery paths.

## UC-1 - Declare reusable class splits

- Goal: Describe the school's existing group arrangements without entering individual students.
- Primary actor: Administrator.
- Supporting actors: None.
- Trigger: The administrator adds or revises a split in a class's setup.
- Preconditions: A working school setup and parent class exist; incomplete surrounding setup is permitted.
- Relations:
  - Requires: None.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The administrator opens a class-subject teaching arrangement from Subjects, or the reusable-group overview
   in Classes, and chooses to add an arrangement, supplying its name and group count.
2. The system presents labeled groups with balanced default sizes from **Split defaults**, beside the class total.
3. The administrator keeps or edits labels and counts and chooses subjects that reuse this split.
4. The system shows totals and any inconsistencies, retains the draft and distinguishes its durability from readiness.
5. The administrator leaves and reopens setup; the system restores the split's exact values and stable identities.

### Extensions

- 2a. The class size or group count is missing, nonintegral, nonpositive or outside disclosed supported limits.
  Preserve the unfinished values, identify the field and explain why balanced groups cannot yet be supplied;
  resume at step 3.
- 3a. Counts are nonpositive or do not total the class size, or labels are blank. Identify the affected groups,
  retain them as incomplete draft data and block readiness rather than silently changing values; resume at step 3.
- 3b. Renaming, resizing or removing a referenced split/group would affect relationships, teaching or rules.
  Show the affected references; require explicit correction/removal or cancel the change, never silently
  retarget them. Preserve recoverable incomplete work if the administrator proceeds; resume at step 3.
- 4a. Draft retention fails or a stale tab would overwrite newer work. Show the failure/conflict, retain the
  working values for recovery and preserve the last durable revision; end.
- 5a. The administrator discards the working draft. Apply the existing explicit discard behavior, preserving
  named school/timetable copies and other drafts; end.

### Guarantees

- G1. The routine concepts are teaching arrangement, group label and count. No student name, identifier, roster or assignment
  input is requested. Anonymous blocks are derived scheduling information, not a student-management screen.
- G2. One reused split has the same groups, identities, sizes and relationships across subjects; it is not
  copied into independent subject splits. Two different splits with identical labels remain different.
- G3. Custom labels retain original characters and full text. Group identity does not depend on label,
  display order or abbreviation. The solver cannot alter declared counts to fit rooms.
- G4. Existing setup navigation, free section switching, recovery and source tracking continue. New controls
  use English labels, labeled keyboard-operable inputs, visible focus and linked field issues; opening advanced
  relationships is optional. Input is displayed as text. Long names wrap or shorten visually without page overflow.
- G5. Draft retention preserves parent class identity/count, split and group identities/names/order/counts,
  reuse references and unfinished values. Unmappable retained settings are shown and protected under UC-15,
  rather than dropped by editing an unrelated field.
- G6. The UC's implemented outcome must be demonstrated through an isolated real-browser class-setup journey,
  including recovery, two/three/four groups, custom labels and invalid totals; existing saved user data is not
  modified to supply walkthrough evidence. This is an agent-run evidence requirement, not a human approval gate.

### Postconditions

- Success: A reusable split is retained and available for teaching plans and relationship declarations.
- Minimal guarantee: Incomplete work remains recoverable; existing saved schools and timetables are unchanged.

## UC-2 - Describe relationships between different splits

- Goal: Establish enough anonymous attendance information to schedule different arrangements together safely.
- Primary actor: Administrator.
- Supporting actors: None.
- Trigger: The administrator relates two or more different splits of a class.
- Preconditions: The split declarations from UC-1 or an equivalent supported source exist.
- Relations:
  - Requires: UC-1, for the declared groups.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The administrator opens relationships for the class and selects its relevant splits.
2. For a nested arrangement, the administrator declares which smaller groups compose each larger group.
3. The system derives sizes and disjoint anonymous membership blocks, shows the resulting overlaps and checks totals.
4. The administrator reviews the relationship and retains it with the setup draft.

### Extensions

- 2a. The arrangements cross. The administrator opens advanced overlap counts and supplies a count for each
  combined intersection; the system derives blocks from positive counts and shows margins/totals; resume at step 3.
- 2b. Three or more arrangements require joint information. Show the combined intersection dimensions and
  existing known totals; require the missing joint counts rather than selecting a joint distribution from
  pairwise counts. Retain partial input and identify unresolved combinations; resume at step 2.
- 3a. A count is negative/nonintegral, totals disagree, a union repeats an audience, or declarations contradict
  each other. Identify the affected class, groups and totals; preserve input and mark the relationship incomplete;
  resume at step 2.
- 3b. A relationship is missing. Show the unresolved splits and direct options to reuse a split, declare a
  nested combination or enter overlap counts; retain the incomplete draft; end.
- 4a. A changed group invalidates a prior relationship, retention fails or newer retained work conflicts.
  Show the actual condition and affected references, preserve the working and prior durable values, and
  require correction/retry without claiming readiness; end.

### Guarantees

- G1. Groups within one complete split partition the class. Positive joint intersections partition it once;
  zero intersections denote no overlap and do not create empty membership blocks. All margins reconcile
  with declared group sizes and the class total. The **Crossing example** is supported.
- G2. All positive blocks sharing a group's coordinate belong to it. A whole-class audience contains all
  class blocks. A composite audience uses the union, without duplicate counts. No student identity is retained.
- G3. Group numbers, labels and equal sizes never imply identity, nesting or disjointness across splits.
  Missing relationships required by a requested timetable block generation/repair; there is no conservative
  mode that omits student-gap analysis. Unused arrangements do not require unnecessary joint declarations.
- G4. The simple reuse/nested paths remain available without displaying joint-count controls. Advanced input
  has explicit group headings, totals, keyboard access and field-level issues. UC-1 G4–G6 apply.
- G5. Retention preserves the related split/group identities, union declarations, every supplied joint count,
  incomplete dimensions and their stable correspondence. Equivalent refinement of anonymous blocks cannot
  change audience overlap, demand, capacity or aggregate gap cost.

### Postconditions

- Success: The relevant arrangements have a consistent anonymous attendance model.
- Minimal guarantee: Unresolved relationships are visible and retained; no audience membership is guessed.

## UC-3 - Declare whole-class and group teaching requirements

- Goal: State how much weekly subject teaching each audience receives, including mixed delivery.
- Primary actor: Administrator.
- Supporting actors: None.
- Trigger: The administrator creates or changes a class-subject teaching plan.
- Preconditions: A parent class and subject exist; grouped delivery has a declared split from UC-1 or equivalent data.
- Relations:
  - Requires: UC-1, for any group audiences used by the plan.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The administrator opens a subject and sees its teaching arrangements by class, then selects a class
   to enter or review its weekly requirement and whole-class, grouped or mixed delivery. An existing
   reusable arrangement can be selected for grouped teaching.
2. The system shows each group's inherited requirement and the contribution from whole-class sessions.
3. The administrator optionally overrides a group's total or changes the whole-class portion.
4. The system shows required actual sessions, teaching received per group and outstanding group portions,
   and retains the exact plan with its inherited/overridden values distinguished.

### Extensions

- 1a. The administrator needs a different audience arrangement. Preserve the plan and subject/class context;
  allow creation or revision of reusable groups there, with an explicit return to the same teaching plan;
  continue with UC-1.
- 1b. A subject has no teaching requirement for a class. Show that absence and offer creation for the selected
  class without duplicating an existing requirement; resume at step 1.
- 1c. A bundled or retained school is being previewed. Show the same readable arrangements without conversion
  or JSON editing. Editing follows the existing explicit copy-conversion boundary; end.
- 3a. An override is cleared. Restore inheritance from the current class-subject default, show the recomputed
  demand and retain that inheritance rather than a copied number; resume at step 4.
- 4a. Counts are missing, nonintegral or negative, whole-class teaching exceeds an attending group's total, or
  positive group teaching lacks an audience. Identify each inconsistency, retain the draft and block readiness;
  resume at step 3.
- 4b. A requirement reduction/removal affects teacher portions, block rules, coordination or session references.
  Show those consequences, require explicit resolution and retain unresolved draft data without silently
  dropping constraints; resume at step 3.
- 4c. Retention fails or conflicts with a newer revision. Explain the condition, preserve working data and the
  prior durable revision, and offer the existing recovery path; end.

### Guarantees

- G1. Weekly demand measures teaching received by attending anonymous blocks. A whole-class session consumes
  one teacher and one room once and contributes one period to every attending group's subject total.
  Separate parallel group sessions consume their own resources. The **Mixed-demand example** is normative.
- G2. For the mixed plan, each group's remaining sessions equal its total minus the whole-class contribution.
  Zero outstanding teaching creates no group sessions. Every used block receives exactly its declared subject
  requirement; neither generation nor repair adds or drops demand to improve coordination.
- G3. Ordinary equal requirements use inherited defaults; overrides remain optional. A later default change
  updates inherited values and exposes resulting issues but does not overwrite explicit overrides.
- G4. Existing class/subject rules continue, interpreted per actual attendance under UC-5 and UC-8.
  Counts, identities, subject/audience references, whole-class portion, inherited state, overrides and stable
  session references survive retention. UC-1 G4–G6 and UC-15's protection against lossy mapping apply.
- G5. Subjects show readable class-specific teaching arrangements: delivery, arrangement name, group labels
  and sizes, weekly totals including whole-class contribution, teacher policies, fixed rooms and declared
  parallel-teaching rules. Missing values remain explicit. Plain whole-class teaching is distinguished from
  grouped teaching; unused reusable arrangements never imply that a subject is grouped. Raw record JSON is
  available only as optional advanced detail.
- G6. Subject navigation and arrangement editing preserve the selected subject and class, independent search
  context, stable group/plan/session identities and unrelated plans. Classes retain a reusable-group overview
  explaining that each subject chooses its own delivery. Merely inspecting or navigating never changes teaching.
- G7. A real-browser journey demonstrates bundled English with two groups, whole-class and mixed examples,
  subject-context creation/reuse/edit/return, draft reopen, scoped refusal of referenced-group deletion, and
  supported narrow-screen access with readable wrapping and no horizontal page overflow. The surrounding
  editor's existing phone visibility boundary remains in force.

### Postconditions

- Success: The retained weekly plan explains both required sessions and teaching received by each audience.
- Minimal guarantee: No required teaching is silently resized; invalid or unfinished demand is recoverable.

## UC-4 - Set eligible teachers and weekly teaching policies

- Goal: Allow automatic teacher selection while retaining explicit school restrictions and intentional sharing.
- Primary actor: Administrator.
- Supporting actors: None.
- Trigger: The administrator reviews teacher allocation for a teaching plan.
- Preconditions: A teaching plan from UC-3 or equivalent data exists; eligible teacher records may still be incomplete.
- Relations:
  - Requires: UC-3, for audience/subject demand.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The administrator opens a plan's teacher settings and sees automatic selection as the new-plan default.
2. The system shows candidates qualified for the subject and eligible for the parent class, with inherited
   candidates for each whole-class/group portion.
3. The administrator keeps automatic selection or narrows a group's candidates and reviews weekly continuity.
4. The system retains the policy and shows any missing candidates or restrictions requiring correction.

### Extensions

- 1a. An existing definition has preassigned teachers. Show them as fixed, preserving all preassigned choices;
  offer explicit adoption of automatic selection and show its affected allocations before applying it;
  resume at step 2.
- 3a. A particular teacher is required. The administrator fixes an eligible teacher for that portion; show the
  hard assignment and retain it; resume at step 4.
- 3b. Intentional weekly sharing is required. The administrator declares named eligible teachers and positive
  weekly portions, such as two sessions with A and one with B. Show the relevant total and retain the portions;
  resume at step 4.
- 4a. No candidate exists, a restricted/fixed teacher is ineligible, or portions fail to sum to the portion's
  demand. Identify the audience, subject, teacher/total and correction route; retain the incomplete policy;
  resume at step 3.
- 4b. A teacher/requirement edit invalidates the policy, or retention fails/conflicts. Explain the affected
  policy and durability status, preserve working/prior durable values and block readiness as applicable; end.

### Guarantees

- G1. Default automatic selection chooses one weekly teacher for each audience/subject portion. Whole-class
  and group portions can have different teachers. Candidates must remain qualified, class-eligible, within
  group restrictions and available for every assigned session. Continuity is not optional solver discretion.
- G2. Intentional sharing is advanced input, not mandatory per-occurrence allocation. Its teacher-fixed
  portions sum to the relevant whole-class or outstanding group portion, never count shared class teaching
  twice, and leave periods/rooms to the solver. No simultaneous team teaching is implied.
- G3. Fixed assignments and intentional fixed portions are hard. Automatic accepted allocations supply a
  repair stability preference, not a hidden lock. A change of fixed/automatic policy requires explicit input.
- G4. A teacher's weekly reference maximum is a monitoring value only in this contract: exceeding it neither
  rejects a timetable nor changes its score. Availability, qualifications, conflicts and explicit assignment
  restrictions remain hard; no new balancing, minimum-load or daily-load policy is introduced.
- G5. The published kernel input carries eligibility, restrictions, fixed/automatic policy, continuity and
  portions; an editor-only candidate list does not satisfy this UC. Retention preserves these fields and
  teacher identities exactly, including an empty candidate list as an error rather than unrestricted choice.
- G6. UC-1 G4–G6 apply, including a browser demonstration of inherited candidates, fixed compatibility and
  deliberate weekly sharing. Kernel allocation and review evidence is additionally required by UC-8/UC-9.

### Postconditions

- Success: The plan has an explicit, retained teacher policy with eligible weekly allocations to be solved.
- Minimal guarantee: Existing fixed teachers remain fixed; incomplete candidate policies are recoverable.

## UC-5 - Constrain audience scheduling and coordination

- Goal: Express necessary school rules while leaving unconstrained combinations and placements to the solver.
- Primary actor: Administrator.
- Supporting actors: None.
- Trigger: The administrator changes scheduling rules or opens optional coordination for teaching activities.
- Preconditions: The selected teaching plans from UC-3 or equivalent data exist.
- Relations:
  - Requires: UC-3, for the selected activities and demand.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The administrator reviews inherited class/subject rules and the default soft internal-gap policy.
2. When a specific relationship is needed, the administrator selects the activities, a coordination type
   from **Coordination controls**, and the required weekly count.
3. The system shows the selected audiences, single-session or required-block activity units, hard rule scope
   and the occurrences left flexible, without requiring manually enumerated pairings.
4. The administrator confirms the settings; the system retains them and identifies local contradictions.

### Extensions

- 1a. Consecutive teaching is required. The administrator supplies the existing exact-length, frequency and
  at-least/exactly rule for the relevant audience/subject; show its demand consumption and inherited scope;
  resume at step 2.
- 2a. The administrator requires strict class coverage for selected rounds or a hard gap-free day for a class.
  Show the precise coverage/duration or all-block gap-free scope before retaining the hard rule; resume at step 3.
- 2b. Only day/period availability or one fixed occurrence is needed. The administrator restricts the selected
  activity/count to the chosen days/periods or fixed slot; show the affected occurrences and leave the rest
  flexible; resume at step 3.
- 3a. A count exceeds available distinct activity occurrences, referenced activities are missing, a fixed slot
  cannot hold the duration, or rules directly contradict each other. Explain the specific conflict, retain
  input and block readiness rather than reducing the count or relaxing another rule; resume at step 2.
- 4a. A referenced requirement changes or retention fails/conflicts. Identify affected rules and the actual
  durability condition, preserve all explicit settings and existing durable copies; end.

### Guarantees

- G1. All controls in **Coordination controls** are supported. Absence of a control leaves automatic compatible
  placement available. Co-location by the solver creates no implicit bundle, pairing or rotation requirement.
- G2. A count K creates K distinct rounds: each participant contributes one occurrence per round, without
  reusing it within that rule. The solver chooses the matching; different rules can refer to the same
  occurrence if all constraints hold. It never silently lowers K. Extra occurrences remain flexible.
- G3. Start-together means equal start period, not equal duration or ending. A required double can start beside
  one single; another compatible single can cover the other group's second period. If strict coverage also
  applies, sessions from the selected teaching plans must cover the whole class exactly once in every period
  occupied by the round. Additional occurrences from those plans may fill a longer activity's remaining
  periods without becoming extra matched starts. A single ending early does not provide second-period
  coverage. Lessons from unselected plans cannot silently substitute for selected coverage activities.
- G4. Inherited unavailability, lunch, daily subject limits, reserved-period limits, latest-start/day-shape and
  consecutive rules are checked for each actual attending block across whole-class and group teaching.
  Disjoint simultaneous subject sessions do not count twice for one student's daily limit. Each applicable
  group receives its own required blocks, on different days if no hard coordination links them.
- G5. Consecutive semantics from the current setup editor remain: maximal same-subject runs, exact permitted
  lengths, positive weekly frequencies, one teacher/room throughout, existing demand consumption and no
  undeclared run of length two or more under that rule version. Ordinary breaks preserve adjacency; lunch,
  skipped teaching periods and day boundaries interrupt it. Changing teacher/room does not split a forbidden
  run into singles. Whole-class/group sessions can form one audience's run only if all these conditions hold.
- G6. Default inherited block rules apply to every attending audience, with whole-class sessions contributing
  to each one's attendance. Intentional teacher portions must permit whole required blocks; a block is not
  split between teachers. Existing mandatory-block exemptions from ordinary same-day clustering continue,
  assessed per actual attendance without duplicating shared sessions' resource costs.
- G7. Applicable hard room capability, availability, allowed-room and fixed-room restrictions remain in force.
  A group may explicitly narrow inherited allowed rooms or choose a fixed room within them; an empty
  intersection is a blocking issue. The solver never widens a restriction to make parallel teaching fit.
- G8. Controls preserve activity identities, rule kind/scope, count, day/period sets, fixed slots, coverage/gap
  mode and consecutive settings. UC-1 G4–G6 and UC-15's unmappable-value protection apply.

### Postconditions

- Success: The retained plan expresses only the administrator's selected hard controls and inherited rules.
- Minimal guarantee: Contradictions remain visible; no constraint or requested occurrence count is discarded.

## UC-6 - Review grouping and scheduling readiness

- Goal: Understand whether a draft can be saved for generation, and correct any blocking setup issues.
- Primary actor: Administrator.
- Supporting actors: Timetable service.
- Trigger: The administrator opens Review or requests a readiness check.
- Preconditions: A working draft exists; it can be empty, incomplete or imported.
- Relations:
  - Requires: None.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The administrator requests review of the current setup revision.
2. The system checks structural completeness, used audience relationships, per-audience demand, eligible
   teacher policies, room compatibility, hard coordination and existing grid/rule consistency.
3. The system shows group/session/student-teaching totals, hard/soft policy summary, nonblocking monitoring
   values and any blocking issues with direct correction routes.
4. With no blocking issues, the system marks that exact revision ready for named save, explaining that
   readiness does not prove timetable feasibility.

### Extensions

- 2a. Used splits lack complete relationships or have inconsistent totals. Name the class and splits and
  offer direct reuse, nested-combination or overlap-count correction; preserve the draft; end.
- 2b. A required audience has no eligible teacher, no compatible room, excessive demand for its available
  periods, or another necessary check fails. Identify the affected audience/resource/rule and mark the
  revision blocked without claiming an exhaustive infeasibility proof; preserve the draft; end.
- 2c. An unrelated unused split lacks relationships. List its draft issue without blocking generation of plans
  that do not use it; resume at step 3.
- 2d. A rule/version/value cannot be understood or the check cannot complete. Show validity as unestablished
  and the reason; preserve the draft and prevent a ready/save claim; end.
- 4a. The draft changes during checking. Mark the response as belonging to its older revision; preserve all
  newer input and require review of that revision before save; end.

### Guarantees

- G1. Blocking feedback identifies class/split/group/subject/teacher/room/rule as applicable and links to the
  correction location. Warnings such as a weekly reference workload exceedance are visibly nonblocking.
- G2. Readiness never resizes groups, infers cross-split overlap, changes teacher policy, creates additional
  sessions, drops coordination or treats unknown data as a supported default.
- G3. Ready means a complete supported schedulable definition can be represented, including required
  relationships; it does not mean a solution has been found. An incomplete unrelated draft setting is
  retained separately and cannot be emitted as if it were a complete active scheduling value.
- G4. Counts and summaries use the same anonymous attendance and actual-session rules as UC-8. Review records
  the checked revision and rules, preserving every value in **Fidelity field groups**. UC-1 G4–G6 apply.

### Postconditions

- Success: The exact revision is ready with an understandable demand/rule summary.
- Minimal guarantee: A blocked or unestablished review preserves input and gives actionable feedback.

## UC-7 - Save a named school snapshot

- Goal: Keep a complete grouped school setup as an explicit, reusable school copy.
- Primary actor: Administrator.
- Supporting actors: Timetable service.
- Trigger: The administrator names and requests saving the current school setup.
- Preconditions: A working setup revision exists.
- Relations:
  - Requires: None.
  - Includes: UC-6 at step 2.
  - Extends: None.

### Main success scenario

1. The administrator supplies a school-copy name and requests save.
2. The exact draft revision is checked through UC-6.
3. The system creates a new immutable school identity holding the checked definition and complete retained
   authoring snapshot, and confirms its name and local persistence scope.
4. The administrator can explicitly select the saved school for generation or begin a new working revision.

### Extensions

- 1a. The name is blank or conflicts under existing name rules. Explain the issue, preserve all setup values
  and the entered name, and request correction; resume at step 1.
- 2a. Review is blocked/unestablished or the working revision has changed. Keep the draft and existing schools,
  show the issue and refuse creation of a ready named school; end.
- 3a. Retention fails or another tab has changed the target storage state. Report the actual failure/conflict,
  preserve the draft and prior copies, and keep recovery/retry available without claiming success; end.

### Guarantees

- G1. Named save does not run generation or claim feasibility. It does not modify the source school, its
  timetables, bundled definitions or other saved copies. Browser-local copies remain local to browser/origin.
- G2. The saved snapshot preserves **Fidelity field groups**, source identity/revision and unsupported retained
  authoring content. Incomplete unused arrangement data remains identified as draft content, outside the
  checked active definition. No blocked active setting is hidden by making a named copy.
- G3. Existing structured/advanced working-data separation, restart behavior and explicit deletion journeys
  continue. UC-1 G4–G6 apply, including reload and comparison with the unchanged source copy.

### Postconditions

- Success: A new saved school snapshot is available for explicit selection and generation.
- Minimal guarantee: Existing copies are preserved; an unsuccessful save leaves recoverable working input.

## UC-8 - Review exact audience and resource assignments

- Goal: Determine whether complete supplied assignments satisfy their exact definition and quality accounting.
- Primary actor: Kernel caller.
- Supporting actors: None.
- Trigger: The caller submits a definition/result pair for review at the published kernel boundary.
- Preconditions: The caller supplies a supported definition and candidate, or receives a diagnostic explaining why not.
- Relations:
  - Requires: None.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The caller supplies the exact definition, candidate session identities, audiences, chosen teachers, periods,
   rooms, applicable baseline and rules/version.
2. The system resolves every used audience to complete anonymous blocks and checks session completeness,
   actual attendance demand, teacher policies, conflicts, room capacity/capability and availability.
3. The system checks all audience-aware rules, required consecutive blocks, counted coordination, explicit
   locks/pins and any hard gap/coverage settings.
4. The system returns a valid result with recomputed scores and explanatory counts from **Optimization
   priorities**, **Gap accounting** and **Repair comparison**, tied to this exact pair.

### Extensions

- 1a. The version is unsupported, identities cannot be resolved, relationships are incomplete, assignments
  are missing/duplicated, or fields cannot be mapped. Return specific diagnostics and no valid-result claim;
  preserve supplied data and any accepted pair; end.
- 2a. An audience, teacher or room is double-booked, a teacher is ineligible, a room is too small/unsuitable,
  demand differs or availability is violated. Identify the sessions, affected audience/resource and rule;
  return invalid without altering assignments or accepted state; end.
- 3a. A hard rule, teacher continuity/portion, required block or coordination count fails. Return invalid with
  the observed and required values, without relaxing the rule or changing the requested count; end.
- 4a. Review cannot finish. Report validity as unestablished, preserving the supplied candidate and accepted
  pair; end.
- 4b. The asserted quality disagrees with recomputation. Disclose the mismatch and use the independently
  computed quality, preserving the original input for diagnostics; never use the asserted score as evidence
  of validity; resume at step 4.

### Guarantees

- G1. Anonymous attendance is authoritative. Whole-class sessions overlap every group of the class; groups
  from different arrangements clash if any positive block is shared. Disjoint groups may run in parallel.
  Overlapping labels and visually separate sections never authorize concurrent attendance.
- G2. Every actual session has exactly one teacher and one room; every session's capacity uses its actual
  distinct audience size. Two simultaneous group sessions cannot share one teacher or room. A shared
  whole-class/composite-group session is reviewed once for resources and once per attending block for demand.
- G3. Qualification, parent-class eligibility, narrowed candidates, fixed teachers, weekly continuity,
  intentional portions and teacher availability are independently checked at this public boundary.
  Reference workload exceedance is a nonblocking indicator and contributes no hard or soft score.
- G4. UC-5 G2–G7 apply to review. Required sessions cannot be dropped to satisfy resources, gaps or coordination.
  Each actual attendance sequence includes whole-class and group teaching for all daily and consecutive rules.
- G5. Gap totals use disjoint counts without double counting across arrangements. Hard gap-free means zero
  internal gaps for every affected block/day. Strict coverage is assessed for each occupied period of its
  selected rounds; default coverage is only an ordinary preference.
- G6. Review preserves the fields in **Fidelity field groups**, names every unresolvable value and does not
  mutate definitions, assignments, baseline, locks or policy. A valid result is usable only with its reviewed
  definition and rules, not another revision with the same display name.
- G7. Evidence must exercise the published kernel boundary with nested and crossing groups, three/four groups,
  mixed demand, two separate teachers/rooms for simultaneous practicals, deliberate sharing, every supported
  hard coordination control and invalid candidates. Include the local normative examples and legacy fixed
  assignments. Generator output alone is not independent review evidence.

### Postconditions

- Success: The exact pair is independently valid, with correct attendance/resource accounting and quality.
- Minimal guarantee: Invalid/unestablished review never changes the accepted pair or falsely reports validity.

## UC-9 - Obtain a complete coordinated timetable

- Goal: Obtain a valid timetable under declared audience, resource and coordination rules.
- Primary actor: Kernel caller.
- Supporting actors: None.
- Trigger: The caller requests initial generation or a repair attempt through the published kernel boundary.
- Preconditions: A supported complete definition is supplied; repair also supplies its exact accepted baseline
  and explicit attempt changes. Missing prerequisites produce the diagnostic path below.
- Relations:
  - Requires: None.
  - Includes: UC-8 at step 4.
  - Extends: None.

### Main success scenario

1. The caller supplies the definition, initial/repair mode, search budget and any baseline, locks or attempt pins.
2. The system confirms the request's active requirements and policies, rejecting incomplete used relationships.
3. The solver chooses eligible weekly teachers, compatible combinations, periods and rooms for all sessions,
   ranking candidates under **Optimization priorities** and the applicable **Repair comparison**.
4. The exact complete candidate is independently reviewed through UC-8.
5. The caller receives the checked definition/result pair, selected weekly teacher allocations, coordination
   occurrence matches, quality breakdown and search outcome/budget information.

### Extensions

- 2a. Input is incomplete, unsupported or inconsistent, eligible teachers are absent, or a required audience
  relationship is missing. Identify the affected class/split/plan/rule and correction; do not start a partial
  conservative solve or change the supplied definition; end.
- 3a. The caller cancels. Report cancellation and preserve the accepted baseline and inputs; do not publish
  a late candidate as the accepted result; end.
- 3b. The budget ends without a valid complete candidate. Report that no valid candidate was found within the
  budget, with available diagnostics; do not claim proven infeasibility or publish a partial timetable; end.
- 3c. The system establishes that the hard requirements cannot be satisfied. Report the supported infeasibility
  conclusion and affected requirements without inventing a minimal conflicting set or reducing demand/counts;
  preserve all inputs and accepted state; end.
- 3d. A valid candidate exists when the budget ends. Return it with the actual termination/quality information,
  without claiming global optimality; resume at step 4.
- 4a. Independent review fails or cannot complete. Return the diagnostic outcome, publish no valid timetable
  and preserve the baseline; end.

### Guarantees

- G1. Automatic coordination is the default; the caller need not enumerate parallel rounds, ordered swaps or
  teacher choices. Unspecified compatible combinations can occur across different days. Explicit hard
  coordination narrows search and is never weakened to produce a result.
- G2. Hard validity precedes every preference. Internal gap cost has its own priority above all ordinary
  preferences; an arbitrary accumulation of ordinary benefits cannot outweigh one extra student-period gap.
  Repair stability precedes gap reduction exactly as declared. A finite search need not prove the best score.
- G3. Teacher selection respects weekly continuity and all eligibility/availability conditions from UC-4/UC-8.
  Fixed assignments remain hard. A weekly reference workload is neither a limit nor an optimization target.
- G4. Actual audience counts and teaching demand are immutable during search. The solver cannot merge two
  declared sessions, resize groups, omit a group, silently reuse occurrences in one counted rule or infer
  unknown intersections to improve feasibility.
- G5. Returned fields preserve **Fidelity field groups** and include all chosen teacher, audience, period and
  room identities, matched coordination and recomputed quality. Unsupported or unmappable values fail
  explicitly rather than falling back to a class-only or fixed-teacher approximation.
- G6. Published-boundary evidence covers initial and repair requests, bounded search, cancellation, failure
  preservation and the corrected **Cross-day example** under suitable resources. A hard-invalid counterexample
  must be rejected independently through UC-8. No runtime result is accepted on source inspection alone.

### Postconditions

- Success: The caller has a complete checked pair with search limits and chosen allocations made explicit.
- Minimal guarantee: An unsuccessful attempt leaves prior accepted data and every hard requirement intact.

## UC-10 - Generate and keep a grouped school timetable (primary)

- Goal: Turn a saved grouped school setup into a named, inspectable valid timetable.
- Primary actor: Administrator.
- Supporting actors: Timetable service, kernel caller.
- Trigger: The administrator selects a saved school and requests generation.
- Preconditions: A complete immutable school snapshot from UC-7 or equivalent supported saved data exists.
- Relations:
  - Requires: UC-7, for the selected saved school snapshot.
  - Includes: UC-9 at step 3.
  - Extends: None.

### Main success scenario

1. The administrator selects the saved school and existing Quick/Thorough search choice, reviewing its grouped
   demand and allocation policy summary.
2. The system pins that exact school snapshot and starts an identified run, showing progress and cancellation.
3. The timetable service obtains a complete checked timetable through UC-9 in initial-generation mode.
4. The system presents the candidate in the compact timetable with selected teachers, student gaps, monitoring
   indicators and any nonblocking quality limitations available for inspection.
5. The administrator names and keeps the candidate; the system durably creates a new timetable identity paired
   with the exact generated definition and confirms it is available through explicit saved-timetable selection.

### Extensions

- 1a. There are newer unsaved setup edits. Explain that generation uses the selected saved school and offer
  the existing path to save/select another copy; preserve both revisions; resume at step 1.
- 2a. The selected snapshot cannot be loaded or is incompatible. Explain the affected data/version and refuse
  to substitute another school or discard fields; preserve all saved artifacts; end.
- 3a. Generation fails, is cancelled or finds no valid result in its budget. Show the actual UC-9 outcome,
  preserve the school and existing timetables, and allow an explicit retry/change of setup; end.
- 3b. The result belongs to an older run or the administrator has switched school. Associate it only with its
  originating run, never overwrite the active school, filters or newer candidate; end.
- 4a. The administrator discards the candidate. Discard only that candidate and preserve saved school/timetables; end.
- 5a. The name is invalid, retention fails or a stale storage conflict occurs. Explain the condition, keep the
  candidate and entered name retryable, and preserve existing durable artifacts; end.

### Guarantees

- G1. Viewing or generating never overwrites a saved timetable or accepts a workspace repair. Saving a new
  timetable uses the host profile's existing publication rules; public-demo copies remain browser-local.
- G2. Kept copies preserve **Fidelity field groups**, the exact definition/result pair, allocation/coordination
  choices and quality. Reopening cannot regenerate teachers or rebind audiences from the current draft.
- G3. Existing whole-week/day, filters, inspector, source navigation, local deletion, startup, progress, budget
  and cancellation behavior continue. Changing school clears selection/filters; changing timetable within
  one school retains applicable filters. No new login or role gate is introduced.
- G4. A reference workload exceedance or permitted residual gap does not by itself prevent keep. A hard
  gap-free, capacity, attendance, teacher, block or coordination violation does prevent it.
- G5. Verification requires an isolated real-browser journey from split declaration through relationships,
  mixed demand, automatic teachers, optional coordination, named school save, generation, inspection,
  timetable keep and reload. It must verify the exact public kernel result/pair and failure preservation,
  including a narrow viewport. Passing technical and browser evidence supports automatic UC approval;
  no additional human confirmation gate is required.

### Postconditions

- Success: A new named timetable can be reopened with exact grouped assignments and quality.
- Minimal guarantee: No unsuccessful or stale run changes an existing school, saved timetable or accepted baseline.

## UC-11 - Inspect parallel lessons and untaught audiences

- Goal: Understand who is taught in each compact cell and inspect any individual parallel lesson.
- Primary actor: Administrator.
- Supporting actors: None.
- Trigger: The administrator opens a valid timetable, changes its view/filter or selects a lesson/cell summary.
- Preconditions: A valid definition/result pair reviewed through UC-8 or equivalent independent review is available.
- Relations:
  - Requires: UC-8, for the exact checked pair.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The administrator opens an existing Week/Day or class/teacher/room view.
2. The system shows whole-class sessions at full width and compatible parallel lessons in side-by-side
   sections with vertical boundaries, keeping the existing cell width and row height.
3. The administrator selects, taps, focuses or hovers a lesson; the system exposes its full description and
   makes each separate lesson available for selection.
4. The administrator changes period/view or closes inspection; the system preserves the existing navigation
   and restores usable focus/selection context.

### Extensions

- 2a. Composite audiences have a compatible constituent ordering. Span their constituent sections and keep
  those positions across the swap as in **Cross-day example**; resume at step 3.
- 2b. Some audiences have no lesson. Preserve their neutral labeled sections in the established audience
  positions; inspection states the full audience name and no-lesson status; resume at step 3.
- 2c. Sections cannot be represented as contiguous nonoverlapping spans, or become narrower than the
  **Compact presentation** threshold. Show the compact summary; selection opens all simultaneous lessons
  and untaught audiences in a complete inspector list; resume at step 3.
- 2d. Filters hide some parallel lessons. Indicate that the cell is filtered and retain the full underlying
  audience/no-lesson distinction in inspection; do not label a hidden lesson's audience untaught; resume at step 3.
- 3a. No hover is available. Selection/tapping or keyboard activation supplies the same full details; resume at step 4.
- 3b. The pair is missing/incompatible or a lesson reference is unresolved. Show the load/reference issue,
  refuse invented details and preserve the previous valid view; end.

### Guarantees

- G1. **Compact presentation** is normative. No diagonal or stacked-cell rendering is required; group count
  never increases the established cell dimensions. Whole-class and partial-class teaching remain distinguishable.
- G2. Visible subject/group text may abbreviate, but stored labels do not. Full details include school/class,
  split/group or whole-class audience, size, subject, teacher, room, weekday/period/time, consecutive-block
  context and applicable explicit coordination. Untaught rows identify their audience/count without fake sessions.
- G3. Known nested audiences keep stable constituent positions, including unequal composite spans. Width is
  not promised as an exact encoding of headcount. Crossing arrangements need not have a universal lane order;
  the summary fallback supplies faithful complete attendance without misleading alignment.
- G4. Every displayed lesson has an accessible name and keyboard/touch selection. Inspector lists have visible
  focus and full names, preserve a clear selected item and return focus on close. Neither color nor hover
  alone communicates subject, untaught status, selection or errors.
- G5. At viewport widths 320–700 pixels, filters start collapsed and the whole five-day week fits the viewport
  width with short headings and a slim period column. Inspection may wrap and scroll vertically. Summary
  fallback is available at any width; lesson cells do not expand to accommodate descriptions.
- G6. Existing subject and tiny room metadata in Week, and teacher detail in Day/inspector, continue where
  sections permit. A compact summary cannot masquerade as one whole-class session. A neutral untaught
  section is not itself an internal-gap warning; UC-12 uses the complete day.
- G7. Browser evidence covers whole-class, two/three/four groups, composites across days, crossing fallback,
  partial coverage, long custom labels, keyboard, touch, filtering and 320/700-pixel views. It compares
  selected full details against the exact pair, not only screenshots of a standalone design preview.

### Postconditions

- Success: Every lesson and untaught audience can be understood without expanding the timetable geometry.
- Minimal guarantee: Unavailable details are identified; no audience or teacher assignment is fabricated.

## UC-12 - Monitor student gaps and teacher workload

- Goal: Identify residual waiting time and compare actual teacher workloads with reference values.
- Primary actor: Administrator.
- Supporting actors: None.
- Trigger: The administrator opens School setup → Student gaps and teacher workload for the selected timetable.
- Preconditions: A checked pair from UC-8 or equivalent review is available.
- Relations:
  - Requires: UC-8, for authoritative attendance and allocation.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The administrator opens the Student gaps and teacher workload tab in School setup. The tab identifies
   the selected school and timetable; it is not displayed within the timetable view. Opening monitoring does
   not require creating or converting a setup draft. Unsaved setup edits do not change the displayed totals.
2. The system shows internal student-gap cost in student-periods, affected class/audiences and days/periods,
   distinguishing it from teacher gaps and the ordinary simultaneous-coverage preference.
3. The administrator selects a teacher; the system shows actual weekly taught periods and any reference
   maximum, with the amount above it explicitly described as a monitoring indicator.
4. The administrator follows an affected period or allocation into the timetable/inspector for practical review.

### Extensions

- 2a. There are no internal gaps. Show zero student-periods, without counting later starts or earlier finishes
  as gaps; resume at step 3.
- 2b. An affected block crosses named groups. Show the anonymous audience combination/count and relevant
  group names, without duplicating the same waiting students in aggregate totals; resume at step 3.
- 3a. No reference maximum is supplied. Show actual workload and that no reference is set; do not invent a
  threshold or warning; resume at step 4.
- 3b. The reference is exceeded. Show actual/reference/difference as nonblocking, with no invalidity or score
  penalty attributed to it; resume at step 4.
- 4a. The pair changed or explanatory data cannot be reconciled. Identify the stale/unavailable summary,
  preserve the pair and require recomputation before claiming current totals; end.

### Guarantees

- G1. **Gap accounting** controls gap locations and weighting. Aggregates remain invariant under equivalent
  block subdivision. Per-group views may show the same incident for context but explicitly deduplicate the
  total; a whole-class schedule cannot conceal one group's gap.
- G2. Workload counts actual teaching periods once per teacher/session. A double counts two; two separate
  group sessions count separately; one shared session is not repeated per member group. Availability and
  teacher clashes remain hard even when an indicator is below its reference.
- G3. Monitoring does not resize demand, change teacher policies, move lessons or advance an accepted baseline.
  Gap correction uses UC-13 or UC-14; no new fairness or individual-student workflow is introduced.
- G4. Summaries remain tied to the exact pair's definition and scoring version. Full text, keyboard access,
  narrow-screen readability and real-browser evidence from UC-11 G4–G7 apply to these indicators.

### Postconditions

- Success: The administrator can locate actual student waiting time and understand each teacher's workload.
- Minimal guarantee: Missing/stale information is labeled; monitoring alone never changes the timetable.

## UC-13 - Review and accept a coordinated repair

- Goal: Resolve changed availability or explicit scheduling restrictions while preserving the accepted timetable
  in the agreed stability order.
- Primary actor: Administrator.
- Supporting actors: Timetable service, kernel caller.
- Trigger: The administrator requests repair from a valid accepted baseline in the existing repair workflow.
- Preconditions: The exact reviewed baseline from UC-8 or equivalent exists and the host profile supports repair.
- Relations:
  - Requires: UC-8, for the accepted definition/result pair.
  - Includes: UC-9 at step 4; UC-8 at step 6.
  - Extends: None.

### Main success scenario

1. The administrator prepares the existing repair draft, changing supported availability/restrictions and
   reviewing retained locks separately from attempt-scoped pins.
2. The system identifies the exact baseline, draft revision and all affected audiences, rules and allocations.
3. The administrator requests a repair attempt with the intended restrictions and search budget.
4. The service obtains a checked repair candidate through UC-9, using **Repair comparison** and the baseline.
5. The system presents a proposal showing every teacher replacement and each period/room change, unchanged
   allocations, unmatched added/removed/audience-changed sessions, and resulting gap/workload/quality differences.
6. The administrator explicitly accepts; the exact proposed definition/assignments are rechecked through UC-8
   against the same baseline, locks and attempt restrictions.
7. Successful durable acceptance advances the baseline and records the accepted pair; the administrator sees
   the accepted timetable and outcome.

### Extensions

- 2a. Used relationships or teacher policies are incomplete, a fixed teacher is unavailable, or a lock
  conflicts. Identify the affected audience/teacher/rule and refuse an invalid attempt without quietly
  changing policy or unlocking assignments; preserve the baseline and repair draft; end.
- 2b. Routine setup editing has created a new school copy. Direct its generation through UC-10; do not
  implicitly seed repair from an unrelated school or infer identity from names; preserve both copies; end.
- 4a. Repair is cancelled, fails review or produces no valid candidate within budget. Show the actual outcome,
  preserve the accepted baseline and durable draft, and allow an explicit new attempt; end.
- 5a. The administrator discards the proposal. Remove its acceptance eligibility, preserve baseline and
  retained draft under existing behavior, and do not apply teacher/period/room changes; end.
- 6a. The draft/baseline changed, the proposal belongs to an older attempt, or the workspace restarted.
  Explain that the proposal is stale/ineligible; preserve the current baseline and draft and require a new
  attempt rather than applying the old proposal; end.
- 6b. Recheck fails or acceptance cannot complete. Show the precise review/storage failure, keep the prior
  baseline and a retryable proposal when it is still current; do not claim partial acceptance; end.

### Guarantees

- G1. Repair ranks hard validity, automatic weekly teacher-allocation stability, period stability, room-only
  stability, student-period gaps, then ordinary preferences. It may retain gaps to avoid greater earlier-tier
  disruption. An explicitly enabled gap-free rule remains hard and can require additional changes.
- G2. Fixed teachers and explicit locks remain hard; accepted automatic teachers remain preferences. Every
  proposed automatic replacement is shown with audience, subject, old/new teacher and affected sessions
  before acceptance. Reference workload alone never forces or rewards a replacement.
- G3. **Repair comparison** governs stable identities. No added, removed or changed-audience occurrence is
  disguised as a room-only change or silently matched by label, sort position or coincident period.
- G4. Preparation, solve and review do not advance accepted state. Acceptance preserves the definition/result
  pair and **Fidelity field groups** with the existing atomicity/retry boundary. Attempt pins do not silently
  become persistent locks; persistent locks are not discarded by beginning a new attempt.
- G5. This UC extends the supported repair workflow. It does not add a repair screen to a profile that offers
  only initial generation and manual alternatives. Profile-specific retained-draft and publication rules
  continue; there is no silent cross-profile migration or baseline replacement.
- G6. Evidence includes a real repair browser journey, kernel score comparisons proving the declared tier order,
  a visible teacher replacement, fixed-teacher refusal, explicit accept/discard, stale attempt and failed
  acceptance preservation. Passing evidence permits agent approval without an additional human gate.

### Postconditions

- Success: The administrator explicitly accepted a checked coordinated repair as the new baseline.
- Minimal guarantee: A failed, stale, cancelled or discarded proposal never changes the accepted pair.

## UC-14 - Correct selected lessons manually

- Goal: Make practical adjustments to individual group lessons and keep only a fully valid resulting timetable.
- Primary actor: Administrator.
- Supporting actors: Timetable service, kernel caller.
- Trigger: The administrator starts a manual alternative or correction from a selected lesson.
- Preconditions: A reviewed pair from UC-8 or equivalent is available.
- Relations:
  - Requires: UC-8, for the source pair.
  - Includes: UC-8 at step 5.
  - Extends: None.

### Main success scenario

1. The administrator selects one specific lesson, including through a split-cell or summary inspector.
2. The system opens a manual working copy and identifies the selected lesson's audience, subject, teacher,
   period and room, leaving coincident lessons independent.
3. The administrator changes the selected period/room or another supported assignment under the declared policy.
4. The system shows all changed assignments and any provisional conflicts without silently moving another lesson.
5. The administrator requests keep/accept; the exact resulting definition and assignments are reviewed through UC-8.
6. With valid review, the system applies the existing explicit named-alternative or workspace acceptance
   lifecycle and confirms the saved result.

### Extensions

- 3a. A hard coordination or consecutive rule requires other sessions to change. Explain the rule and offer
  an explicit combined proposal listing every affected lesson; the administrator chooses that proposal or
  keeps/cancels the provisional individual change without secondary moves; resume at step 4.
- 3b. A teacher change would affect weekly continuity. Require an explicit change to the whole affected
  automatic weekly allocation, show all affected sessions and check eligibility; do not turn it into an
  undeclared shared-teaching exception. Fixed-policy changes require explicit definition editing; resume at step 4.
- 4a. The intermediate draft is invalid. Show audience/resource/rule conflicts and preserve editable working
  changes, clearly ineligible for keep/accept; resume at step 3.
- 4b. The administrator discards manual work. Remove only the working copy under existing discard behavior,
  restoring the source view without changing any saved pair; end.
- 5a. Review fails or is unestablished. Show the specific issue, preserve working changes and the accepted
  source, and refuse publication; resume at step 3.
- 6a. The name is invalid, the source/revision is stale, or storage/acceptance fails. Explain the condition,
  keep the working copy recoverable and preserve the previous durable pair; end.

### Guarantees

- G1. One selected lesson is the default edit unit; accidental co-location does not bundle sessions.
  No secondary teacher, time or room change is hidden. A required consecutive block or hard coordinated
  combination is offered explicitly rather than silently split or moved.
- G2. Hard rules are never removed by moving a lesson. Occurrence pairings may be re-evaluated only as allowed
  by their counted rule, with the resulting matching visible; the required count remains intact.
- G3. A valid alternative may have a worse soft score or permitted gaps than its source. The administrator
  controls the correction; optimality is not required for manual save. Hard gap-free and fixed teachers still apply.
- G4. Source and resulting definitions remain paired with their own assignments. UC-8 and **Fidelity field
  groups** govern exact teachers, audiences, periods, rooms, locks and quality. A demo alternative creates a
  new timetable identity; workspace acceptance follows its explicit review lifecycle.
- G5. Real-browser evidence includes selecting one of several lessons, moving it without unrelated moves,
  invalid intermediate work, explicit combined change, teacher-continuity handling, valid keep/reload and
  refusal/preservation. UC-11's keyboard, touch and compact presentation guarantees apply.

### Postconditions

- Success: An explicitly reviewed valid correction is kept under the existing publication lifecycle.
- Minimal guarantee: Invalid, discarded or unsuccessful corrections do not change the accepted source.

## UC-15 - Exchange and adopt definitions without losing meaning

- Goal: Import, export or adopt supported school/timetable data with exact attendance and teacher semantics.
- Primary actor: Administrator or kernel caller.
- Supporting actors: Timetable service when importing through the UI.
- Trigger: The actor requests advanced-data transfer, recovery, public-boundary load or explicit rule adoption.
- Preconditions: Source data or a working definition/result pair exists; it need not already be valid.
- Relations:
  - Requires: None.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The actor selects source data and the explicit import/export/adoption operation.
2. The system identifies its supported contract/rule version, preserved fields and any adoption changes.
3. The actor confirms any explicit transfer between advanced and structured working data or adoption preview.
4. The system produces or retains the faithful result with **Fidelity field groups**, preserving imported
   identities and the distinction between draft completeness, supported values and reviewed validity.
5. Reopening or re-importing the produced data restores the same supported scheduling meaning and display names.

### Extensions

- 2a. The source is legacy whole-class/fixed-teacher data. Keep its fixed assignments and original supported
  rule behavior, including explicit gap limits. Offer new automatic/gap/consecutive behavior only through
  an explicit previewed adoption; resume at step 3.
- 2b. A value, version, lock, heterogeneous session policy, retained setting or relationship cannot be mapped.
  Name its location and reason; preserve the raw source and current working data and refuse a lossy
  conversion, structured edit or export rather than dropping it; end.
- 2c. Source is malformed or contains missing/duplicate/unresolved identities. Show the exact diagnostic,
  preserve entered/raw content for correction and do not replace the current valid pair; end.
- 3a. The actor cancels transfer/adoption. Preserve both working models and prior saved artifacts; end.
- 4a. Source is incomplete but representable as a draft. Retain unfinished values, report blocking issues and
  exclude any ready/valid claim; resume at step 5.
- 4b. Retention/export fails or conflicts with a newer working revision. Explain the condition, retain the
  source and recoverable working content, and do not report a successful transfer/save; end.

### Guarantees

- G1. The public contract explicitly distinguishes supported versions without this specification prescribing
  a version number. Older clients cannot silently reinterpret grouped audiences as whole classes or omit
  solver-selected teachers. Unsupported feature-bearing input fails observably.
- G2. Existing preassigned teachers remain fixed, including differing fixed teachers on legacy sessions. A
  legacy plan that cannot express new weekly continuity retains its supported legacy meaning and is
  protected from lossy editing until explicitly restructured. No teacher-policy adoption is inferred.
- G3. New gap defaults never erase an existing explicit hard setting. Older consecutive rules/series remain
  effective until explicit adoption. Adoption discloses every affected policy/default/weight and materializes
  settings needed to preserve unrelated behavior. It never invents block permission from existing placements.
- G4. Structured and advanced working models remain separate until explicit transfer. Export and inspection
  do not implicitly adopt rules, solve, accept a repair, regenerate teachers or mutate a source artifact.
- G5. Fidelity includes optional absence versus explicit empty lists, entered language, identifiers, order
  where meaningful and unknown retained values. Semantic interchange preserves equivalent anonymous
  audiences without student records; raw recovery preserves unmappable content without claiming it is valid.
- G6. Verification includes kernel round trips for feature-bearing and supported legacy pairs, deliberately
  unmappable values, and an isolated browser advanced/structured transfer with cancellation, failure and
  reload. Compare preserved values and behavior, not merely successful parsing.

### Postconditions

- Success: Produced/restored data preserves supported meaning, or an explicitly accepted adoption is fully disclosed.
- Minimal guarantee: Unmappable or unsupported content is identified and retained; no silent lossy conversion occurs.

## UC-16 - Print or export a readable grouped timetable

- Goal: Share the compact timetable on paper or in PDF without requiring interactive inspection.
- Primary actor: Administrator.
- Supporting actors: Existing print/PDF facility.
- Trigger: The administrator prints or exports the selected timetable/view.
- Preconditions: A valid reviewed pair from UC-8 or equivalent is selected.
- Relations:
  - Requires: UC-8, for the authoritative pair.
  - Includes: None.
  - Extends: None.

### Main success scenario

1. The administrator selects the existing export scope and print/PDF operation.
2. The system renders the timetable with whole-class widths, side-by-side groups and composite spans.
   Empty timetable slots stay blank. Split sections occupy the complete row without a spare header strip.
3. Lesson subjects, groups, teachers and rooms appear directly in the timetable. Time intervals appear in
   the left slot column, with weekday labels when daily bell times differ.
4. The administrator obtains only timetable pages, identified by school, timetable and class or teacher.
   No technical appendix, reference codes, abbreviation key, allocation identifiers, revision data,
   validation rules or coordination diagnostics are included.

### Extensions

- 2a. Names or parallel lessons do not fit. Wrap lesson information within the grid, grow rows as needed,
  and continue the timetable onto additional pages with repeated headings and slot context. Crowded or
  crossing groups use stacked lesson entries instead of a summary requiring a separate key; resume at step 3.
- 2b. A teacher view is exported. Include that teacher's lessons and their classes/groups; other teaching
  is omitted and empty cells stay blank. The class or teacher heading identifies the scope; resume at step 3.
- 3a. A lesson cannot be resolved or the pair is incompatible. Identify the issue and refuse an apparently
  complete export with missing lessons; preserve the pair and previous outputs; end.
- 4a. Export/printing fails or is cancelled. Report the actual outcome where observable and preserve the
  timetable, selection and saved artifacts; end.

### Guarantees

- G1. Every PDF page contains a timetable grid. Additional pages are timetable continuations, never
  technical detail pages. Ordinary class and teacher weeks fit on one page when their lesson labels fit.
- G2. Subjects, audience/group names, teachers and rooms are readable in the grid itself, with full labels
  wrapped rather than replaced by technical references. Underlying timetable data remains unchanged.
- G3. Constituent positions, partial coverage and lesson boundaries remain understandable in monochrome.
  Lesson colors are retained. No exported meaning relies on hover or hidden inspector state.
- G4. Evidence includes rendered PDF pages for the corrected cross-day example, two/three/four groups,
  crowded groups, long custom names and partial coverage. Check that all pages are timetable pages,
  split sections fill their rows, and empty slots remain blank.

### Postconditions

- Success: A timetable-only output can be read and shared without interactive controls or technical notes.
- Minimal guarantee: A failed export does not modify timetable data or claim complete output with unresolved lessons.

## Normative data

The tables below define exact rows for their stated rules/examples, not an exhaustive catalog of school
names, subjects, teachers, rooms, days or group counts. Each consuming UC incorporates its referenced data
into its behavioral contract.

### Split defaults

Normative for UC-1–UC-3. For class size N and group count K, N and K are positive integers, K ≥ 2 and K ≤ N.
Within disclosed application limits, do not impose a special maximum of two, three or four groups.
Let q = floor(N / K) and r = N mod K. The first r groups have size q + 1 and the remaining groups size q.
Default labels are Group 1 through Group K in displayed order. Administrators can change labels and counts;
every complete split still has positive integer sizes totaling N. Labels need not establish global uniqueness;
split context and stable identity disambiguate them.

Exactly these rows — no more, no fewer:

| Class size | Group count | Default sizes |
| --- | --- | --- |
| 29 | 2 | 15, 14 |
| 28 | 3 | 10, 9, 9 |
| 28 | 4 | 7, 7, 7, 7 |

### Coordination controls

Normative for UC-5, UC-8, UC-9 and UC-14. Counts are positive integers. A participant occurrence is one
session, or one complete required consecutive block if that block is explicitly selected as the activity.
Block rounds require distinct blocks; selecting a block does not count its interior sessions as extra rounds.
At least K distinct matched rounds must satisfy the rule. Additional accidental matches are allowed;
the administrator is not declaring exactly K and forbidding further compatible co-location.

Exactly these rows — no more, no fewer:

| Control | Hard meaning when enabled |
| --- | --- |
| Start together | Every participant in each required round has the same weekday and start teaching period |
| Same day | Every participant in each required round is on the same weekday; start periods may differ |
| Different days | Participants in each required round occupy pairwise different weekdays; enough distinct days must exist |
| Allowed days/periods | Each of the selected counted occurrences starts within the chosen allowed sets and its complete duration fits all hard availability; unselected occurrences remain flexible |
| Fixed slot | One selected occurrence starts at the declared weekday/period; repeating requirements use separately declared fixed occurrences, without reusing one occurrence |
| Strict class coverage | In every teaching period occupied by a selected round, simultaneous sessions from its selected teaching plans cover every parent-class block exactly once; additional occurrences from those plans may fill remaining duration, without adding matched starts or shortening a session/block |
| Gap-free student day | Every block of the selected class has zero internal gaps on each applicable day, across all subjects and arrangements |

Rules can be combined; contradictory combinations block readiness or fail generation without weakening them.
Pairwise nonoverlap follows attendance/resource validity and needs no separate manual control.
An English plan with three singles and a handicraft plan with two singles, requiring two coordinated starts,
uses two distinct English occurrences and both handicraft occurrences; the third English occurrence is flexible.

For unequal durations, group A can attend a required two-period science block while disjoint group B attends
one English single followed by one handicraft single. A start-together rule can pair the science block's
start with English; a separate strict-coverage rule selecting all three activities requires complete coverage
through both periods. It does not force handicraft to start with English. Two neighboring same-subject
singles would instead form a maximal run and require the corresponding consecutive permission; calling
them separate singles does not bypass that rule.

### Optimization priorities

Normative for UC-5, UC-8–UC-10 and UC-12–UC-14. Compare tiers lexicographically: improvement in a lower tier
cannot compensate for deterioration in an earlier one. Only hard-valid candidates are publishable.

Exactly these rows — no more, no fewer:

| Priority | Initial generation | Repair |
| --- | --- | --- |
| 1 | All hard constraints | All hard constraints, including fixed teachers, retained locks and attempt pins |
| 2 | Not applicable | Minimize changed comparable automatic weekly teacher allocations |
| 3 | Not applicable | Minimize comparable sessions with changed period |
| 4 | Not applicable | Minimize comparable sessions with unchanged period and changed room |
| 5 | Minimize internal student-gap student-periods | Minimize internal student-gap student-periods |
| 6 | Ordinary preferences, including simultaneous coverage | Ordinary preferences, including simultaneous coverage |

Initial generation has no accepted baseline and thus no invented stability costs. Other applicable hard and
ordinary rules retain their supported strengths/weights. There is no workload-reference penalty or separate
gap fairness tier. In the new contract, ordinary simultaneous-coverage cost is one penalty unit for each
uncovered student-period at a teaching period where at least one session of that class occurs. Count from
disjoint blocks; exclude periods in which the class has no sessions. This ordinary cost can include a later
start or earlier finish, but cannot outweigh the distinct internal-gap tier. An explicit strict-coverage rule
is hard instead of being satisfied by paying this ordinary penalty.

### Gap accounting

Normative for UC-5, UC-8–UC-10 and UC-12–UC-14. For each class block, scan actual attendance across all
subjects and arrangements. Class unavailability and lunch split the day into available parts; neither counts
as an empty teaching opportunity or bridges a gap. Ordinary breaks are not teaching opportunities. An empty
reserved period is excluded from gap opportunities; an actually occupied permitted reserved period remains
an attendance endpoint. Within each available part, count unused available teaching periods strictly between
the first and last occupied ones. The cost is the sum of block size × that count. Group-specific teacher or
room unavailability does not remove an otherwise available student teaching period from this calculation.

Exactly these rows — no more, no fewer:

| Actual attendance and context | Internal gap contribution |
| --- | --- |
| 14 students attend P1, P2, P4; P3 is available teaching | 14 student-periods at P3 |
| 7 students attend P1 and P4; P2/P3 are available teaching | 14 student-periods |
| Attendance only P2, P3, P4 | Zero; later start is not an internal gap |
| Attendance only P1, P2, P3 | Zero; earlier finish is not an internal gap |
| Attendance before and after lunch with no internal empty teaching period in either available part | Zero; lunch interrupts the parts |
| Attendance on each side of a class-unavailable teaching period, otherwise no empty teaching period | Zero; the unavailable period splits the parts |
| Attendance P1/P3 with P2 an empty reserved period | Zero; empty reserved opportunity is excluded |
| Two equivalent blocks of 7 both wait through one internal period | 14 student-periods, the same as one block of 14 |

Hard gap-free uses zero by this same definition, not a separate teacher- or group-label gap calculation.

### Repair comparison

Normative for UC-8, UC-9, UC-13 and UC-14. Compare only identities carried faithfully from the exact baseline.
Teacher-allocation keys are stable audience/subject/portion identities; a comparable automatic weekly
allocation contributes one change if its selected teacher differs, regardless of the number of its sessions.
Intentional portions are fixed and cannot be reassigned through a soft teacher change. A comparable session
contributes one period change if its weekday/teaching-period identity differs, and one room-only change if
its period is unchanged and room differs. A teacher change does not conceal period/room changes.

Renaming or reordering alone does not change identity. Equivalent refinement of anonymous blocks does not
change the represented audience. Added/removed demand, changed audience composition/count and genuinely
replaced portions are identified separately as unmatched changes; do not pair them by names, array positions
or convenient times and do not score nonexistent baseline assignments as unchanged. Supported repair input
must preserve the stable identities of surviving sessions/allocations. If it cannot, explain the incompatibility
and require explicit initial generation rather than claiming a stable repair. Routine authoring of a new school
copy follows UC-7/UC-10 and is not automatically a seeded repair.

Exactly these rows — no more, no fewer. Both candidates in each comparison satisfy all hard rules and are
equal in tiers not mentioned before the deciding tier:

| Comparison | Preferred repair |
| --- | --- |
| Preserve a weekly automatic teacher with several period moves vs replace that teacher with no period moves | Preserve the weekly teacher |
| One period move leaving a gap vs five period moves removing that gap, with equal teacher changes | One period move |
| One room-only change vs two room-only changes, with equal teacher/period changes | One room-only change |
| Equal stability, fewer internal student-period gaps but worse ordinary coverage | Fewer internal student-period gaps |
| Fixed teacher unavailable vs proposed substitute without changing the fixed policy | Neither is publishable; explicit policy/availability correction is required |

### Compact presentation

Normative for the interactive views in UC-11 and UC-14. UC-16 uses the timetable-only print layout above.
Keep the host view's existing class/period cell dimensions for all
audience counts. Build side-by-side sections only where each lesson can occupy one contiguous audience
span without misrepresenting overlapping arrangements. Reuse a stable order of declared constituent groups
for nested arrangements throughout the displayed timetable. A composite group spans its constituents.

If any separately selectable section would be less than 24 CSS pixels wide, or contiguous faithful spans
are impossible, replace the cell's sections with one summary control. This is a deterministic fallback,
not a restriction on configured group counts. The summary identifies parallel lesson count and partial
coverage where applicable; its inspector lists every lesson and untaught audience with full details and
touch controls at least 44 CSS pixels high. The summary itself uses the whole existing cell hit area.
Abbreviation/truncation must not overflow the interactive cell. Printed cells wrap lesson information in
the timetable itself rather than use interactive hit areas or technical references. Filtering never converts
a taught audience into a no-lesson audience.

Exactly these rows — no more, no fewer:

| Cell state | Presentation |
| --- | --- |
| Whole-class session | One full-width lesson |
| Two or more representable parallel audiences | Side-by-side selectable sections with shortened text |
| Composite lesson over nested audiences | One selectable span over its constituent sections |
| Known untaught audience | Neutral section in its usual position, short group label and no-lesson mark |
| Too narrow or incompatible crossing spans | Compact summary opening all lessons and untaught audiences |

### Mixed-demand example

Normative for UC-3, UC-8 and UC-9. One class has English groups E1/E2, each with weekly total three.

Exactly these rows — no more, no fewer:

| Delivery portion | Actual sessions | Contribution per attending student |
| --- | --- | --- |
| Whole class | 2 | 2 for both E1 and E2 |
| E1 | 1 | 1 for E1 |
| E2 | 1 | 1 for E2 |

There are four actual sessions and three received periods per student. If E2's explicit total becomes four,
only its outstanding group portion increases to two; the whole-class and E1 portions remain unchanged.

### Crossing example

Normative for UC-2, UC-8 and UC-9. A class of 28 has English E1/E2 and German G1/G2, all size 14.

Exactly these rows — no more, no fewer:

| Anonymous intersection | Count |
| --- | --- |
| E1 ∩ G1 | 7 |
| E1 ∩ G2 | 7 |
| E2 ∩ G1 | 7 |
| E2 ∩ G2 | 7 |

Every English group overlaps every German group; none of these cross-subject pairs can run simultaneously.
E1/E2 may run together, and G1/G2 may run together, given distinct available resources. Adding a third split
requires combined intersections where relevant; these four margins alone do not determine its membership.

### Cross-day example

Normative for UC-2, UC-8–UC-11 and UC-16. Class size is 28. English E1/E2 each have size 14; handicraft
H1–H4 each have size 7. E1 = H1 ∪ H2 and E2 = H3 ∪ H4. Each student requires one English and one handicraft
session in this example. Suitable available qualified teachers and capacity/capability-compatible rooms exist.

Exactly these rows — no more, no fewer:

| Slot | English session | Separate handicraft sessions | Coverage |
| --- | --- | --- | --- |
| Tuesday P2 | E1 | H3 and H4 | Every class block exactly once |
| Thursday P4 | E2 | H1 and H2 | Every class block exactly once |

Each slot has three sessions, requiring three distinct available teachers and three distinct available rooms.
The two handicraft rooms each need capacity at least seven and the English room at least fourteen. H3/H4
cannot share a capacity-seven room or one teacher as two simultaneous sessions; the same applies to H1/H2.
Resources can be reused across days. Each student receives both subjects without overlap. E1 spans the
left two constituent sections on Tuesday; E2 spans the right two on Thursday. The other sections retain
their constituent positions across days. No universal slot/day choice is imposed on other generated plans.

### Fidelity field groups

Normative for UC-1–UC-16 wherever the structure is produced, retained, transferred, reviewed or displayed.
Display/export need expose only the fields relevant to their stated scope; retention and interchange must
preserve the complete supported meaning. Every complete feature snapshot preserves exactly these field
groups — no more, no fewer; existing unrelated supported fields remain required by their governing contracts:

| Field group | Preserved content |
| --- | --- |
| Context and version | Source/school/timetable identity, draft revision, contract/rule versions and applicable host-profile lifecycle |
| Class and splits | Parent identity/count, split/group identities, full labels, order, sizes, reuse and incomplete authoring values |
| Audience relationships | Group unions, combined intersection dimensions/counts, stable audience correspondence and unresolved draft information |
| Teaching demand | Subject/audience identities, weekly totals, inheritance/overrides, whole-class/group portions and stable session identities |
| Teacher policy | Qualified/class-eligible references, narrowed candidates, fixed/automatic mode, continuity and identified weekly sharing portions |
| Scheduling policy | Grid, availability/lunch, inherited and narrowed room rules, daily/reserved/day-shape rules, consecutive policy and counted coordination scope/values |
| Exact assignments | Session audience/teacher/period/room identities, selected weekly allocations and matched coordination occurrences |
| Review and repair context | Exact baseline/pair references, review outcome/version, locks, attempt pins, comparison changes, recomputed gap/workload/quality and search termination |
| Retained source data | Optional presence/empty distinctions, original entered characters, unsupported raw settings and advanced/structured transfer provenance |

An importer or editor that cannot preserve a value must identify it and refuse the lossy action under UC-15.
No field group authorizes retention of student names, identifiers or individual assignments.

## Out of scope

- Individual student records, assignment, attendance tracking and individual student timetables.
- Cross-class electives or audiences assembled from multiple classes.
- Simultaneous team teaching or multiple teachers/rooms within one session.
- Automatic group formation, solver-selected group sizes and automatic alteration of weekly demand.
- General ordered rotations, alternating/fortnightly weeks and arbitrary station sequences.
- New authentication, roles, cloud synchronization, cross-browser storage or profile migration.
- A new gap-fairness system, teacher load optimization, minimum load or reference-maximum constraints.
- Automatic repair acceptance, implicit grouped moves, deployment or application implementation by this document.

## External dependencies

No external stakeholder decision is pending. The published kernel must support the audience, allocation,
coordination, review and version guarantees before the corresponding application outcomes can complete;
an editor-only representation is insufficient. Existing profile storage, repair and print/PDF facilities
remain their host boundaries. If any boundary lacks support, the consuming UC reports the precise unsupported
condition, retains recoverable input and preserves the current valid pair; it must not silently approximate
the feature. This dependency is a required capability, not permission to approve an incomplete use case.
