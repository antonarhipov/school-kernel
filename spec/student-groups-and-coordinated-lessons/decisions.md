# Student Groups and Coordinated Lessons: Design Decisions

Status: D001–D035 and consolidated visual review accepted; specification created on 2026-10-18.

Started: 2026-10-18

Source: Design grilling of [proposal.md](proposal.md).

This file records decisions explicitly confirmed by the user. Proposal statements and interview recommendations
are candidates until confirmed. The initial interview authorized recording decisions. After accepting the
consolidated review, the user requested creation of the specification. Application implementation remains
outside the current task.

## Accepted decisions

### D001 — First-release scope: multiple arrangements within a class

Accepted on 2026-10-18. Q1 answer: **B**.

- Support different group memberships and group counts for different subjects within the same class.
- Support two, three, four or more configured groups, parallel group lessons, and cross-subject swaps across
  periods or days.
- Detect student overlap between arrangements and between groups and their whole class.
- Apply room capacity, capability and availability constraints to the attending audience.
- Defer cross-class electives and simultaneous multi-teacher sessions from the first release.

This selects the first-release boundary. Membership representation and coordination are separate branches
resolved below. Student assignment is explicitly outside School Kernel under D010.

### D002 — Anonymous membership blocks

Accepted on 2026-10-18. Q2 answer: **B**.

- Represent class membership using disjoint anonymous membership blocks, each with a size.
- A named group is a union of those blocks; a whole-class lesson includes all blocks belonging to the class.
- Students represented by a block always attend together across the modeled arrangements. Different arrangements
  may combine blocks differently; more complicated overlaps require finer blocks.
- Two lesson audiences overlap when they share at least one membership block. Group size is the sum of the
  sizes of its distinct member blocks.
- Individual student identifiers, student records and individual student timetables are outside this release.

For example, four blocks of seven students can form English E1 = blocks 1+2 and E2 = blocks 3+4, while handicraft
H1-H4 each use one block. This example does not restrict arrangements to nested splits.

Membership blocks describe anonymous scheduling audiences and are distinct from consecutive lesson blocks.
D010 clarifies that administrators declare splits and group labels, while the school assigns actual students
outside School Kernel. D011 establishes declarations of group-level relationships between different splits.

### D003 — Automatic coordination with optional administrator constraints

Accepted on 2026-10-18. Q3 answer: **User-defined hybrid**, replacing the original A/B framing.

- Let the solver determine compatible group-subject combinations and their timetable placement by default.
- Administrators need not manually enumerate every parallel round or swap pattern.
- Allow administrators to constrain coordination when the school needs a particular relationship or pattern.
- Administrator constraints narrow the solver's freedom; unspecified relationships remain flexible.
- The normal workflow should remain simple, with additional coordination controls available when needed.

User rationale: mandatory manual combinations would complicate management, while automation must retain
administrator control when needed.

This decision concerns scheduling coordination. Student assignment is outside School Kernel under D010.
Coverage defaults are resolved by D004, optional hard controls by D019, and repeated-occurrence selection
by D033. Duration and consecutive-lesson examples must be made explicit in the specification.

### D004 — Prefer simultaneous coverage, with an optional strict rule

Accepted on 2026-10-18. Q4 answer: **B**.

- Prefer combinations that keep groups occupied together and avoid student gaps.
- Under this coverage default, an idle group is permitted when needed for feasibility; an idle group alone does
  not violate the coverage policy.
- Allow the administrator to require complete class coverage for selected coordinated activities.
- Keep the ordinary automatic workflow flexible while exposing stricter coordination when needed.

D019 scopes optional strict coverage to selected coordinated activities. D034 resolves the coverage
preference's optimization priority. D005 separately resolves the policy for internal student gaps.

### D005 — Internal student gaps: soft by default with a very high penalty

Accepted on 2026-10-18. Q5 answer: **B**, with the explicit addition that the default internal-gap penalty must
be very high to minimize gap occurrences.

- Permit internal student gaps by default, while giving them a very high default penalty so the solver strongly
  seeks to minimize them.
- Offer an optional strict rule requiring a gap-free student day.
- An internal gap is a free teaching period between an audience's first and last lesson. Lessons in periods
  1, 2 and 4 have a gap in period 3; lessons in 2, 3 and 4 start later but have no internal gap.
- Evaluate actual attendance for each membership block across all subjects and arrangements. Another group's
  lesson cannot conceal a gap for students who do not attend it.
- Later starts and earlier finishes are not internal gaps.

D006 resolves the high penalty as a priority above ordinary preferences, and D007 resolves its relationship
to repair stability. D008 resolves gap counting and size weighting. Further gap-policy detail is bounded by
D009; lunch/break treatment and the optional strict rule still need concrete specification defaults.

### D006 — Student-gap cost outranks ordinary preferences

Accepted on 2026-10-18. Q6 answer: **B**.

- Give internal student-gap cost a distinct optimization priority above ordinary preferences.
- A candidate's lower student-gap cost always outranks improvements to teacher gaps, preferred rooms and other
  ordinary preferences; those improvements cannot collectively outweigh a worse student-gap cost.
- Student gaps remain permitted unless the optional strict rule is enabled. Hard constraints continue to apply.
- This selects score precedence, not a guarantee that a time-limited search proves the best possible timetable.

The priority relative to repair stability is resolved by D007, and the gap cost measure by D008. Further
configuration detail is bounded by D009.

### D007 — Preserve timetable stability before reducing gaps during repair

Accepted on 2026-10-18. Q7 answer: **A**.

- For period/room stability during repair, minimize period changes before room-only changes, then
  student-gap cost and ordinary preference costs. D028 adds teacher-allocation stability ahead of this
  sequence. All hard rules and explicit locks continue to apply.
- Prefer a repair moving one lesson and leaving a student gap over a repair moving five lessons to eliminate
  that gap, assuming both satisfy the hard rules and use the same applicable stability accounting.
- During initial generation, there is no accepted timetable to preserve; student-gap cost therefore takes
  precedence over ordinary preferences directly.
- An explicitly enabled gap-free hard rule still requires a gap-free result, even if meeting it requires more
  changes than a result that would be permitted under the default soft policy.

This preserves the existing priority between period and room stability. Stable audience/session identities,
membership changes and their impact on repair change accounting must be made explicit in the specification.

### D008 — Measure internal gaps in student-periods

Accepted on 2026-10-18. Q8 answer: **B**.

- Calculate student-gap cost by multiplying each membership block's empty internal teaching periods by its size
  and summing across disjoint blocks.
- A one-period gap for fourteen students and a two-period gap for seven students each cost fourteen
  student-periods.
- Equivalent subdivision of a membership block leaves the aggregate cost unchanged. Overlapping named groups
  cannot count the same students' waiting time twice.

### D009 — Keep residual gap handling proportionate

Accepted on 2026-10-18 from the user's qualification of Q8.

- Keep the accepted strong defaults and student-period metric, without elaborating an extensive gap-policy
  or fairness system for hypothetical edge cases.
- The user expects occasional remaining gap cases to be straightforward for an administrator to resolve
  manually. Prioritize a clear timetable and practical review/correction within the normal administrator
  workflow.
- Return the interview to the core grouping and coordination workflow. Additional gap-specific mechanisms
  require a concrete need rather than further speculative policy branches.

This does not weaken hard conflict, capacity or explicitly selected strict rules. D031 establishes the
individual-lesson correction default and D028 the review of proposed teacher changes.

### D010 — Declare splits and labels; student assignment stays at school

Accepted on 2026-10-18 from the user's clarification of Q9. The original blocks-first versus split-first
question is replaced by this explicit product boundary.

- Schools assign actual students to groups internally, outside School Kernel.
- School Kernel declares class splits and maintains the resulting groups and their labels.
- Default group labels are "Group 1", "Group 2", and so on. Administrators can supply custom group names.
- Do not collect student names or student identifiers, or introduce a student-assignment workflow.
- Anonymous membership blocks from D002 are a scheduling abstraction; the administrator's primary concepts
  are splits and labeled groups.

This clarification does not itself establish overlaps between differently sized subject splits; D011 addresses
the group-level relationship information. D012 resolves group-size entry/defaults.

### D011 — Declare group-level relationships between subject splits

Accepted on 2026-10-18. Q10 answer: **"sounds good"**.

- Support administrator declarations of relationships between differently sized subject splits.
- For example, English E1 comprises handicraft H1 + H2, and English E2 comprises H3 + H4.
- These group-level declarations establish which audiences overlap and which are disjoint. In the example,
  E1 overlaps H1 and H2, while it is disjoint from H3 and H4 and may attend parallel sessions with them.
- Represent these relationships without student names, identifiers or student-assignment actions.

Group names and counts alone do not establish overlaps. D026 resolves entry of crossing arrangements,
and D027 resolves incomplete audience relationships.

### D012 — Balanced, editable group sizes

Accepted on 2026-10-18. Q11 answer: **A**.

- When creating a split, distribute the class size as evenly as possible across its groups by default.
- A class of twenty-nine split into two groups defaults to sizes fifteen and fourteen.
- Allow the administrator to edit the counts to match the school's actual split.
- Use positive whole-number group sizes summing to the class size for a complete split.
- Derive composite group sizes from their constituent groups and require consistency with declared
  relationships between splits.
- Use these declared counts for room-capacity checks. The solver schedules the declared audiences without
  silently changing their sizes to fit rooms.

These counts carry no student identifiers and do not assign students to groups.

### D013 — Mix whole-class and group teaching in one weekly subject plan

Accepted on 2026-10-18. Q12 answer: **A**.

- Allow a class-subject weekly teaching plan to contain both whole-class and grouped portions.
- Interpret the weekly teaching requirement as lessons received by the attending students, not a sum of all
  separate group sessions.
- For example, three English lessons can comprise two whole-class sessions plus one session for each of two
  English groups: four scheduled sessions, with each student receiving three lessons.
- Keep student teaching time, scheduled session counts and teacher workload distinct. A shared whole-class
  session is one actual session, rather than one duplicate for each constituent group.

Teacher allocation is resolved by D014. Different requirements for particular groups are resolved by D030.

### D014 — Solver chooses teachers

Accepted on 2026-10-18. Q13 answer: **"The solver"** (option B).

- The administrator supplies eligible teachers; the solver selects teacher assignments as well as periods,
  rooms and compatible parallel combinations.
- Apply this allocation choice to the whole-class and group portions of the feature's teaching plans.
- Each session still uses one teacher; simultaneous team teaching remains deferred under D001.
- This deliberately expands the solver's responsibility beyond the existing preassigned-teacher boundary.

Teacher continuity is resolved by D015, workload policy by D016, and eligibility/group restrictions by D017.
Compatibility with existing fixed assignments is resolved by D029.

### D015 — Keep a consistent weekly teacher per audience and subject

Accepted on 2026-10-18. Q14 answer: **A**.

- The solver chooses one consistent teacher for each group's subject teaching across the week by default.
- For example, all three of a group's weekly English sessions use the same chosen teacher.
- Whole-class and group portions may have different teachers; the whole-class portion also has a consistent
  chosen teacher across its weekly sessions by default.
- Permit deliberate sharing of weekly teaching through an explicit exception, rather than changing teachers
  from lesson to lesson automatically.

Workload policy is resolved by D016, existing fixed assignments by D029, and the shared-teaching exception
mechanism by D032. Any additional links between whole-class and group teachers remain unselected.

### D016 — Weekly teacher workload is an indicator, not a scheduling constraint

Accepted on 2026-10-18 from the user's correction of Q15's proposed hard maximum.

- Teacher availability constrains scheduling capacity; a weekly teaching maximum is an indicator for the
  administrator to monitor, not a solver constraint.
- Display actual assigned weekly teaching workload. Where a school supplies a reference maximum, use it for
  monitoring and comparison rather than rejecting a timetable or penalizing its solver score.
- Count actual taught sessions once. Separate group sessions contribute separately; a shared whole-class
  session is not duplicated for its constituent groups.
- Exceeding a reference weekly maximum does not by itself make a timetable invalid or prevent acceptance.
- Existing teacher conflict and qualification rules continue to apply; this decision concerns workload limits.

Detailed indicator presentation belongs to the main review workflow. No minimum-load, balancing or daily-cap
policy is introduced by this decision.

### D017 — Inherit class-subject teacher eligibility with optional group restrictions

Accepted on 2026-10-18. Q16 answer: **A**.

- Derive a group's candidate teachers from those eligible for the parent class and qualified for the subject.
- Let the solver choose among those candidates, subject to availability, weekly continuity and other
  applicable rules.
- Allow the administrator to narrow a group's candidate list or fix a particular eligible teacher when needed.
- Keep automatic allocation as the normal workflow; group-specific restrictions are optional.

The feature must represent candidate eligibility at the solver boundary; editor-only eligibility checks are
insufficient once teachers become solver-selected. D029 resolves existing fixed assignment compatibility.

### D018 — Inherit class and subject rules, evaluated by actual attendance

Accepted on 2026-10-18. Q17 answer: **"follow the recommendation"**.

- Groups inherit existing class and subject rules by default. Class unavailability and lunch apply to all
  constituent groups.
- Daily subject limits, reserved-period allowances and day-shape rules are evaluated for actual anonymous
  attendance, including both whole-class and group teaching. Two parallel English sessions for disjoint
  groups count as one English lesson per attending student, not two.
- Required consecutive lessons apply to each relevant group; its own required block must be satisfied.
  Different groups can receive their blocks on different days unless additional coordination says otherwise.
- Applicable room restrictions remain in force; capacity is checked against the session's actual audience.
  A declared fixed room is not silently relaxed to make parallel sessions fit.

This is an inheritance/counting principle. The precise representation of mixed teaching in consecutive rules,
group-specific room settings and the final policy-adoption/compatibility mechanics remain specification work.

### D019 — Initial optional coordination controls

Accepted on 2026-10-18. Q18 answer: **"I think it's good"**.

- Require selected group lessons to start together.
- Require selected coordinated sessions to occur on the same day or on different days.
- Restrict selected lessons to chosen days or periods, including a fixed slot when needed.
- Include the already accepted optional strict class-coverage and gap-free rules.

Enabling a requirement makes it hard; unspecified coordination stays under solver control. A swap can span
different days automatically, without the administrator having to construct a rotation or fix its slots.

- Defer general ordered rotations and alternating-week patterns. Existing required consecutive-lesson rules
  remain supported under D018.

D033 resolves repeated-session selection and pairing. Duration and consecutive-lesson examples must be
made explicit in the specification; a start-together control must be distinguished from complete coverage.

### D020 — Divide compact timetable cells vertically, with abbreviated text

Accepted on 2026-10-18. Q19 answer: the user's correction favors vertical divisions within the existing
compact cells, especially for the usual two or three groups. This supersedes the earlier stacked-band
recommendation.

- Interpret vertical divisions as side-by-side rectangular sections within one class/period cell.
- Keep the existing cell dimensions; adding group lessons must not increase row height or cell width.
- Shorten visible subject and group labels to fit. Preserve the full custom group names and lesson details
  in the inspector and in hover descriptions; abbreviation changes presentation, not stored names.
- Keep each displayed group lesson separately selectable. Selection or tapping must expose full details,
  so inspection does not depend on hover.
- Whole-class lessons occupy the full cell. Different subjects may occupy adjacent sections in a group cell.
- Support for four or more groups remains part of D001. D021 resolves the dense-cell fallback.

The revised [design/group-cell-layouts.html](design/group-cell-layouts.html) demonstrates whole-class,
two-group and three-group lessons at the same compact dimensions, composite sections, untaught groups and
narrow mobile summaries. A design control changes the three-group example to four groups. It is a discussion
preview, not an implemented product screen or a PDF acceptance example.

### D021 — Use a compact summary when individual sections become unusable

Accepted on 2026-10-18. Q20 answer: **A**.

- Retain side-by-side group sections whenever abbreviated labels remain readable and the individual sections
  remain usable selection targets.
- If the available width or number of groups makes the individual sections unusable, show a compact summary
  such as "4 groups" within the same cell dimensions.
- Selecting the summary opens an inspector listing every group lesson, with each lesson individually
  inspectable and its full details accessible.
- The summary must clearly identify grouped teaching; it must not resemble one whole-class lesson.
- Apply the fallback according to available space, including narrow mobile cells. Four groups do not
  automatically trigger it if their individual sections remain usable.

The preview's narrow mobile week demonstrates this fallback, including individual group inspection and
an explicit untaught-group count for partial coverage.

### D022 — Align composite-group sections to their constituent audiences

Accepted on 2026-10-18. Q21 answer: **A**.

- Where declared group relationships permit, align the cell's sections to the constituent audiences.
- A composite group's lesson spans the sections of its constituent groups instead of receiving the same
  width as each smaller simultaneous session.
- For E1 = H1 + H2 and E2 = H3 + H4, Tuesday can show English E1 across the left half and handicraft H3/H4
  in the two right-hand quarters. Thursday shows handicraft H1/H2 in the left-hand quarters and English E2
  across the right half.
- Keep those audience positions consistent across the coordinated swap, making the subject changes easy
  to follow without changing cell dimensions.
- Apply D021's compact-summary fallback if individual sections cannot remain usable.

This decision concerns visual treatment of declared composite groups, not solver audience membership or an
exact encoding of student counts by area. It does not establish a universal lane order for arbitrary
non-nested arrangements. The preview's 7C row demonstrates the composite sections and cross-day swap.

### D023 — Preserve an untaught group's visible section

Accepted on 2026-10-18. Q22 answer: **A**.

- Where the audience sections are known, keep an untaught group's section in its usual position instead of
  expanding another group's lesson across the cell.
- Use a neutral appearance and a short label such as "G2 —". Hover or inspection explains "Group 2: no lesson".
- Make partial-class teaching visibly distinct from a whole-class lesson.
- Do not treat every untaught section as an internal gap or an error. Later starts and earlier finishes
  remain permitted under D005; actual internal-gap assessment depends on the audience's complete day.
- Keep D021's summary fallback when the individual sections cannot remain usable. Its inspector must also
  identify untaught audiences so that a partial-class period cannot appear to cover the whole class.

The preview's 7D row demonstrates the untaught-group state. The mobile example also exposes an untaught
audience through a compact summary and its inspector.

### D024 — Preserve compact group cells in PDF and print, with a printable key

Accepted on 2026-10-18. Q23 answer: **A**, explicitly confirmed after the initial keyboard-layout input "Ф".

- Retain compact split cells, constituent-audience spans and neutral untaught sections in PDF and print.
- Provide a printable key for abbreviated names and references to full lesson details wherever those details
  do not fit in the cell.
- Dense summaries must reference their complete group-lesson listing in the export, including untaught
  audiences where applicable.
- The export must be understandable without hover or an interactive inspector. Preserve the compact grid
  rather than expanding its cells to accommodate all full descriptions.

### D025 — Reuse a declared class split across subjects

Accepted on 2026-10-18. Q24 answer: **A**.

- Declare a class split once and allow multiple subjects to use it.
- Reusing a split reuses the same group audiences, names and sizes; it does not duplicate the split for each
  subject or require relationships between identical copies.
- Subjects with different group arrangements use separate splits, with their audience relationships
  declared under D011.
- Matching group numbers, labels or sizes alone does not establish identical audiences.

### D026 — Describe crossing splits using anonymous overlap counts

Accepted on 2026-10-18. Q25 answer: **A**.

- Offer an advanced anonymous overlap-count editor for crossing arrangements.
- For English E1/E2 and German G1/G2, each of size fourteen, a table can record four intersections of seven
  students each. Neither split needs to be expressed as unions of whole groups from the other split.
- Counts are whole numbers, permit zero intersections and must reconcile with declared group sizes and the
  class total. Derive the internal membership blocks from the positive intersections.
- Do not require separately named base groups or any student names, identifiers or assignment workflow.
- For more than two crossing splits, represent the combined intersections; do not assume pairwise counts
  uniquely determine joint membership.
- Keep shared splits and nested combinations on their simpler declaration paths. Expose the advanced
  count editor only when the school needs crossing arrangements.

The treatment of a relationship that has not yet been supplied is resolved by D027.

### D027 — Require complete audience relationships before generation or repair

Accepted on 2026-10-18. Q26 answer: **A**.

- Allow an incomplete grouping setup to be saved as a draft.
- Require the missing audience relationships before generating or repairing a timetable that uses the
  affected splits. Preserve the attendance-based conflict and student-period gap calculations.
- Identify the affected class and splits, with a direct route to reuse, combination or overlap-count entry.
- Do not infer audience identity from matching group numbers, names or sizes.
- Do not introduce conservative generation with incomplete student-gap analysis as an alternative mode.

This requirement applies to relationships needed by the requested timetable, rather than unrelated unused
splits elsewhere in the setup.

### D028 — Preserve accepted weekly teacher allocations before lesson times during repair

Accepted on 2026-10-18. Q27 answer: **A**.

- After hard constraints, minimize changes to accepted weekly audience/subject teacher allocations before
  period changes, room-only changes, student-gap cost and ordinary preferences, in that order.
- A different eligible teacher may be proposed during repair, while respecting D015's weekly continuity and
  all other applicable rules. Teacher stability is a preference, not an implicit hard teacher lock.
- Prefer preserving a group's accepted weekly teacher over keeping lesson times unchanged when those
  preferences conflict and both candidates satisfy the hard rules.
- Explicitly fixed teachers remain hard constraints under D017.
- Show proposed teacher changes in the repair review before acceptance.

This extends D007's repair stability order. It does not create a teacher-stability preference for initial
generation without an accepted teacher allocation.

### D029 — Preserve existing preassigned teachers as fixed assignments

Accepted on 2026-10-18. Q28 answer: **A**.

- Preserve existing preassigned teachers as hard fixed assignments when existing definitions are read or
  upgraded; do not silently reinterpret them as solver preferences.
- Let the administrator explicitly switch a teaching plan to automatic selection from eligible teachers.
- Newly created plans default to automatic selection under D014, with optional fixed teachers under D017.
- Automatic plans use the accepted weekly allocation as the repair stability baseline under D028; fixed
  assignments remain hard constraints.
- Compatibility must preserve existing explicit assignment behavior rather than rewriting teacher choices
  as a side effect of introducing the new feature.

### D030 — Inherit weekly requirements with optional per-group overrides

Accepted on 2026-10-18. Q29 answer: **A**.

- Groups inherit the class-subject weekly requirement by default.
- Allow an explicit per-group override, such as three weekly English lessons for one group and four for
  another. Keep the ordinary equal-requirement setup simple.
- In a mixed whole-class and group plan, shared whole-class sessions count toward each attending group's
  total; remaining group teaching supplies that group's outstanding requirement.
- The solver schedules the declared requirements; it must not silently add, remove or change teaching
  requirements to improve coordination or timetable quality.

### D031 — Move only the selected group lesson by default

Accepted on 2026-10-18. Q30 answer: **A**.

- A manual move acts on the selected group lesson by default, rather than every parallel lesson in its cell.
- Lessons placed together automatically do not become an implicit hard bundle.
- If an explicit coordination rule requires linked lessons to move together, offer the combined change
  explicitly for review. Do not silently move other sessions or discard the coordination rule.
- Validate the complete resulting change against all applicable hard constraints before acceptance.
- Keep the administrator's individual-lesson control for practical corrections, including residual gaps
  under D009.

### D032 — Enter deliberate shared teaching as weekly portions

Accepted on 2026-10-18. Q31 answer: **A**.

- Represent an explicit shared-teaching exception as weekly portions, for example "2 English lessons with
  Teacher A; 1 with Teacher B" for the same group.
- Let the solver choose periods and rooms subject to teacher availability and all other applicable rules.
- Require the portions to add up to the relevant declared teaching requirement, keeping shared whole-class
  sessions and group sessions distinct under D013/D030.
- Keep one consistent weekly teacher as the normal plan under D015. Shared portions are an optional
  advanced exception, rather than mandatory teacher entry for every lesson occurrence.

### D033 — Specify a coordination count and let the solver choose occurrence pairings

Accepted on 2026-10-18. Q32 answer: **A**.

- For optional coordination of repeating lessons, select the teaching plans and required number of
  coordinated occurrences. Let the solver choose the occurrence pairings and timetable slots.
- For three English lessons and two handicraft lessons, requiring two coordinated starts pairs two
  English occurrences with the handicraft occurrences while leaving the extra English occurrence flexible.
- Each selected pairing must satisfy the enabled hard rule. Do not silently reduce the requested count
  when it cannot be met.
- Occurrences outside that requested coordination remain unconstrained by that rule, subject to all other
  applicable requirements.
- Automatic compatible parallel placement under D003 remains the ordinary workflow when no coordination
  constraint is supplied. General ordered rotations and alternating-week patterns remain deferred under D019.

### D034 — Rank simultaneous coverage below internal student gaps

Accepted on 2026-10-18. Q33 answer: **A**.

- Put the preference for simultaneous class coverage with ordinary preferences, below internal student-gap
  cost. Prefer avoiding actual internal waiting time over synchronizing groups' starts or finishes.
- An explicitly enabled strict coverage rule remains hard for the selected coordinated activities.
- The repair order is hard constraints, teacher-allocation stability, period stability, room-only stability,
  internal student-gap cost, then ordinary preferences including simultaneous coverage.
- Without an accepted baseline, initial generation has no repair stability costs; internal student gaps
  outrank ordinary preferences directly.

## Current decision frontier

All individual product-choice questions Q1-Q33 have been answered. The user accepted the consolidated decision
set and visual examples, then explicitly requested the specification: "looks good. I think we are ready to
create the spec". The resulting behavioral contract is [spec.md](spec.md). Application implementation has
not been requested.

## Consolidated review

### First-release boundary

The accepted feature covers declaration of reusable class splits and anonymous audience relationships,
whole-class/group teaching plans, solver-selected eligible teachers, periods and rooms, optional hard
coordination, attendance-aware validation and scoring, repair and manual correction, compact timetable
inspection, and self-contained PDF/print output. The ordinary workflow starts with labeled splits and
balanced editable counts; crossing overlaps and exceptional teaching/coordination controls stay advanced.

Actual student assignment and student records remain outside School Kernel. Cross-class electives,
simultaneous team teaching, general ordered rotations and alternating-week patterns remain deferred under
D001/D019. These decisions are planning input, not a completed normative specification or implementation.

### Corrected cross-day acceptance example

Use a class of twenty-eight: English E1/E2 each contain fourteen; handicraft H1-H4 each contain seven.
E1 comprises H1 + H2, and E2 comprises H3 + H4.

| Slot | English session | Handicraft sessions | Audience coverage |
|---|---|---|---|
| Tuesday, period 2 | E1 | H3 and H4 | H1 + H2 + H3 + H4, each once |
| Thursday, period 4 | E2 | H1 and H2 | H1 + H2 + H3 + H4, each once |

These are two coordinated slots, not four parallel rows. Each handicraft group is a separate session,
requiring its own available teacher and suitable room when simultaneous; for rooms with capacity seven,
H3/H4 cannot share one room and H1/H2 cannot share one room. Teachers and rooms can be reused across days
when available. This supplies one English session and one handicraft session per attending student across
the two slots, without pupil, teacher or room clashes under suitable resource assignments.

The proposal's original illustrative table double-books audiences. Preserve it as the original draft;
the specification must use this corrected example rather than reproduce its four-row placement.

### Visual review

The consolidated discussion preview covers whole-class, two-/three-group, composite, untaught-group and
narrow-screen summary states. All timetable cells use the same forty-four-pixel preview height. Audience
sections preserve their positions across a composite swap; full descriptions remain available through
selection and hover. The inspector lists all audiences represented by a compact summary.

JavaScript syntax and standalone preview rendering were checked. A Chrome visual inspection confirmed the
compact geometry, composite spans, untaught sections, mobile summaries and initial inspector content.
This is design evidence, not application or PDF acceptance testing. The user accepted this consolidated
view on 2026-10-18; no further individual product-choice branch is currently open.

## Specification defaults and integration resolutions

The user authorized [spec.md](spec.md) after the consolidated review. It contains sixteen use cases, with
UC-10, generate and keep a grouped school timetable, as the primary actor journey. Earlier forward references
in this interview record to remaining specification work are resolved there.

The following are specification-level resolutions of smaller details and existing-contract conflicts,
not additional interview answers or claims of application implementation:

- The new soft-gap default supersedes the older editor's implicit hard-zero default for newly authored
  feature plans. Explicit hard gap limits and supported older rule versions keep their meaning. This
  implements D005 without silently changing existing schools.
- Lunch and class-unavailable periods split gap-accounting parts; ordinary breaks are not teaching
  opportunities and empty reserved periods are excluded. This follows current gap accounting rather than
  introducing a new lunch or exception policy.
- A required coordination count is a minimum number of distinct matched rounds; additional accidental
  matches are permitted. Extra occurrences remain flexible under D033. Start-together does not require
  equal duration. Strict coverage checks every occupied period and may use additional occurrences from
  the selected teaching plans to cover a longer activity's remaining duration.
- Ordinary simultaneous-coverage cost counts uncovered student-periods during periods when the class has
  teaching, at one ordinary penalty unit each. It stays below the separate internal-gap tier under D034;
  weekly teacher reference maxima contribute no penalty.
- Repair matches stable audience/subject/portion and session identities, preserving identity through
  renames, reordering and equivalent anonymous-block refinement. One changed automatic weekly allocation
  counts once; added, removed or audience-changed sessions are reported separately. This makes D028's
  priority reviewable without inventing continuity from labels or positions.
- Sections narrower than twenty-four CSS pixels, or unable to form faithful contiguous audience spans,
  use the summary fallback. Inspector lesson controls are at least forty-four pixels high. These concrete
  usability defaults make D021 deterministic while preserving the existing compact grid.
- Incomplete unused arrangements can remain retained authoring data outside the checked active definition;
  relationships needed by the requested timetable remain mandatory. Named save still requires a complete
  checked active definition, and routine edits create a new school copy rather than implicit seeded repair.

The specification's structure, relationship graph, inclusion/extension step references, links and exact-row
tables were checked. Browser/kernel/print evidence described in its use cases is required during later
implementation and convergence; it has not been performed by creating this specification.


### D035 — Subject-facing teaching arrangements

Accepted on 2026-10-19 from the user request to correct the English/groups journey.

- Subjects show teaching arrangements by class, including readable bundled previews.
- Weekly demand, groups, teachers, rooms and parallel teaching are visible together.
- Create or reuse groups from the class-subject teaching plan and return to that context.
- Classes retain reusable group definitions; their existence does not split every lesson.
- Preserve the existing domain identities, copy-conversion boundary and all unrelated work.

This corrects UC-1/UC-3 navigation and presentation without changing attendance or solver semantics.
