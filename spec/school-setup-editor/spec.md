# School setup editor

## Feature summary

An administrator creates a school from empty through a full-screen structured editor, saves a named school
definition independently of a timetable, and explicitly generates and keeps a timetable from that saved definition.
Class plans own weekly demand and teacher allocations; teacher profiles own eligibility and availability.

This specification implements the [proposal](proposal.md), including the 2026-10-17 clarification: under the new
scheduling rules, consecutive lessons in the same subject for the same class are forbidden unless explicitly
required. A requirement declares a block length of two or more lessons and a separate weekly frequency.

## Scope and resolved decisions

- Delivery targets the anonymous public demo. Existing normal-workspace authoring remains separate.
- The structured editor and advanced JSON editor have independent working data and explicit import/export.
- Setup starts with the grid. Suggested sections guide the administrator without locking navigation.
- Incomplete and invalid work is retained as a draft. A named school copy requires a checked, complete definition;
  it does not require a generated timetable. This keeps incomplete work recoverable without presenting it as a
  generation-ready school.
- Every named save creates a new browser-local school identity. Existing schools and timetables are immutable.
- Consecutive requirements belong to a class's subject plan. Each length has its own positive weekly count and
  “at least” or “exactly” operator. Different lengths need separate rules. Counting maximal runs avoids ambiguous
  overlapping doubles: three adjacent lessons are a triple, and four are a block of four.
- The new default prohibition is versioned scheduling behavior. Older definitions and results keep their original
  rules. Conversion into the new setup model explicitly previews and adopts the new rules, without manufacturing
  block requirements from an old timetable.
- Imported settings outside the simplified controls are preserved and shown as retained settings. A change that
  cannot preserve them is refused, with the affected values identified. Full-contract form editing is not a goal.
- Existing demo access, isolation, input limits, bounded generation, cancellation and local cleanup continue.
  This feature supersedes the JSON-only setup entry and generation handoff in the
  [timetable-first demo specification](../demo-timetable-experience/spec.md), and extends its local cleanup and
  timetable validation to cover setup data and new scheduling rules. Other exploration and manual-edit behavior
  remains governed by that specification.
- English interface labels follow the current demo. Entered names and imported labels retain their original
  language. No new account, login, permission role or localization selector is introduced.
- Agent-run isolated browser walkthroughs and published kernel-boundary evidence determine acceptance. There is
  no additional human approval gate.

## Actors and domain terms

- **Administrator:** an anonymous demo visitor authoring a school in one browser.
- **Kernel caller:** an application or command-line user requesting generation or assignment review.
- **Setup draft:** recoverable authoring work with its own identity, optional source school and current edit revision.
- **School copy:** an immutable, named, checked definition with its authoring snapshot, saved in this browser.
- **Class:** one indivisible student group sharing a teaching plan; called a cohort in kernel JSON.
- **Eligibility:** permission for one teacher to teach one subject in one class. Qualification alone does not grant
  eligibility for every class.
- **Weekly demand:** a class-subject lesson count. Allocations distribute that count among teachers.
- **Occurrence:** one of those weekly lessons; a consecutive block uses existing occurrences.
- **Teaching period:** an ordinary or reserved opportunity to teach. Breaks and school-wide lunch are not periods
  available for lesson placement.
- **Consecutive block:** a maximal run of one class's subject across neighboring teaching periods on one day,
  uninterrupted by lunch. Every occurrence in a permitted block shares one teacher and one room.
- **Blocking issue:** missing or invalid definition data or a demonstrated contradiction in hard requirements.
  A soft preference is advisory. Passing readiness checks is not proof that a timetable exists.
- **Generated candidate:** complete, verified assignments awaiting an explicit timetable name and save.

## Use-case map

“Requires” refers to an available successful outcome, including an equivalent outcome restored from durable data.
It does not require re-entering imported entities. Includes also implies an execution dependency. Free navigation
does not bypass the data prerequisites of an action.

| ID | Actor goal | Primary actor | Relations |
| --- | --- | --- | --- |
| UC-1 | Start, resume and recover school setup | Administrator | None |
| UC-2 | Arrange the school week | Administrator | Requires UC-1 |
| UC-3 | Define subjects and subject rules | Administrator | Requires UC-1 |
| UC-4 | Define classes and their curators | Administrator | Requires UC-1 |
| UC-5 | Record teacher eligibility | Administrator | Requires UC-1 |
| UC-6 | Allocate a class's weekly teaching | Administrator | Requires UC-3, UC-4 |
| UC-7 | Copy a class curriculum | Administrator | Requires UC-6 |
| UC-8 | Mark teacher availability and preferences | Administrator | Requires UC-2, UC-5 |
| UC-9 | Arrange class-specific lunch | Administrator | Requires UC-2, UC-4 |
| UC-10 | Define rooms and placement requirements | Administrator | Requires UC-1 |
| UC-11 | Require consecutive lesson blocks | Administrator | Requires UC-6 |
| UC-12 | Obtain a timetable under declared scheduling rules | Kernel caller | Includes UC-13 at step 4 |
| UC-13 | Review complete assignments against a definition | Kernel caller | None |
| UC-14 | Review setup readiness | Administrator | Requires UC-1 |
| UC-15 | Exchange definitions with the advanced JSON tool | Administrator | Requires UC-1 |
| UC-16 | Save a named school definition copy | Administrator | Includes UC-14 at step 2 |
| UC-17 | Generate and keep a school timetable (primary) | Administrator | Requires UC-16; includes UC-12 at step 4 |
| UC-18 | Delete local school artifacts | Administrator | None |

## State models

### Draft validation

A draft is **Incomplete**, **Unchecked**, **Checking**, **Blocked**, **Ready**, or **Check failed**.

- Starting empty creates Incomplete. Loading/importing creates Incomplete or Unchecked according to its data.
- An edit from any state produces Incomplete or Unchecked and invalidates earlier readiness evidence.
- Explicit review from any state enters Checking. Its matching result produces Blocked or Ready; an unavailable
  check produces Check failed. Incomplete data is reported as Blocked without submitting fabricated kernel lessons.
- Editing during Checking supersedes that check. Its eventual response cannot change the newer revision's state.
- Restore retains Ready only when its complete data, rule version and checking build still match; otherwise it
  becomes Incomplete or Unchecked. Failed or refused saves do not turn a draft into Ready.
- Saving Ready creates a separate immutable school copy and consumes that exact source draft revision. Later
  editing the copy starts a successor draft. Explicit draft discard removes only that draft and restores its source
  or the new-school entry; cancellation preserves it.

UC-1 covers creation, restore, edits and discard; UC-14 covers checks; UC-16 covers named save.

### Draft durability

Durability is independent of validity: **Pending**, **Saving**, **Saved in this browser**, **Only on this page**,
or **Storage conflict**.

Creating or recovering a draft, or editing it, enters Pending; autosave enters Saving. A successful write of the current revision enters Saved in this
browser; a newer revision stays Pending. A failed write enters Only on this page, retaining the working edit and
last durable value. Retry follows Pending → Saving. Detection of a newer stored value enters Storage conflict and
refuses overwriting it. Explicit recovery first preserves the current work, reloads the latest stored state and
then, if requested, creates a separate recovered draft; it never silently merges two edits. Closing the overlay
does not discard a draft or imply a successful write. These transitions belong to UC-1.

### Saved school and generation

A saved school exists with zero or more independently named timetables. Saving it does not start generation.
A generation attempt is **Preparing**, **Queued**, **Running**, **Checking result**, **Candidate**, **Unsuccessful**,
**Cancelled**, or **Interrupted**.

UC-17 starts Preparing. It may progress through Queued, Running and Checking result, skipping phases only when
the service reports an already checked reused result. Matching successful review produces Candidate. A terminal
failure or no complete solution produces Unsuccessful; loss of recoverable run provenance produces Interrupted.
An acknowledged cancellation of an active run produces Cancelled. A cancellation failure leaves the actual run
state visible. Once a terminal outcome is accepted, late competing callbacks cannot replace it.

Candidate becomes a named timetable only on explicit successful save. Discard removes only the candidate.
Unsuccessful, Cancelled, Interrupted and saved/discarded Candidate permit an explicit new attempt; no automatic
retry starts another solve. An unresolved candidate must be saved or discarded before another attempt for that
school. Reload reconnects to an active attempt only with matching provenance; otherwise it becomes Interrupted.

### Transition boundary

All transitions not listed above are refused without changing stored schools, timetables, another draft, the
selected active run, or unrelated browser data. Confirmed local cleanup in UC-18 removes the local artifacts and
invalidates their pending callbacks; it does not mutate a running server session.

## UC-1 - Start, resume and recover school setup

- Goal: Enter and leave setup with unfinished work recoverable and its source unambiguous.
- Primary actor: Administrator
- Supporting actors: browser storage
- Trigger: The administrator requests School setup, a new school, or recovery of unfinished setup.
- Preconditions: The demo is open; storage may be unavailable or contain unreadable data.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator opens the vertical School setup entry near the timetable's upper-left side for the selected
   school, or starts a new school from that entry.
2. The full-screen editor covers the application, moving from left to right, and identifies the draft and source.
   An existing draft resumes; a new school has no subjects, people, classes, rooms, teaching demand or intervals.
3. The editor offers the sections in Setup sections, starting a new school at Timetable grid, with the school name
   in its header, section issue counts, a close action and a plainly labelled durability status.
4. The administrator changes a value, navigates freely between sections and closes the editor.
5. The current revision is autosaved, including incomplete input. Closing restores the prior timetable context.
6. The administrator reopens setup or reloads the demo and resumes the last durable work and editing location.

### Extensions

- 2a. A source has kernel JSON but no setup snapshot. Open a retained source draft and offer the explicit
  conversion preview in UC-15, with the source unchanged and unsupported edits unavailable; continue at step 3.
- 2b. Stored data cannot be read or its authoring version is unsupported. Retain the raw data, explain the affected
  artifact and offer readable schools, empty setup and local cleanup without claiming successful recovery; end.
- 2c. Several drafts belong to the selected source. Show their source and working names and require an explicit
  choice, distinguishing recovered drafts even when names match; resume at step 2.
- 4a. The administrator requests discard of this draft. Show its scope and request confirmation; confirmation removes
  only this draft and returns to its source or the new-school entry; cancellation preserves every value; end.
- 5a. Writing fails, including quota failure. Retain the edit in memory, preserve the last complete durable value
  and show “Only on this page” with retry and loss-on-page-close guidance; continue at step 6.
- 5b. Another tab changed storage. Refuse the overwrite and retain this page's work. Explicit recovery offers a
  downloadable recovery document containing the full authoring snapshot, then reloads current data and offers a
  separate recovered draft. If recovery cannot be read, retain it and explain why; end.
- 6a. Browser storage remains unavailable. Restore only what is readable and retain current in-memory work during
  overlay navigation; do not promise recovery after page close; end.

### Guarantees

- G1. All editor use cases preserve the fields in Authoring snapshot fidelity. Autosave runs after edits settle and
  is flushed on section/source changes, overlay close and explicit save/export. Only a matching successful write
  earns Saved in this browser. No autosave creates a named school copy or starts a solve.
- G2. Drafts are isolated by draft and source identity, including multiple unfinished new schools. Labels are not
  identities. Selection or closing never silently retargets, discards or replaces another draft.
- G3. Opening/closing preserves the underlying school, timetable, filters, lesson selection and manual draft,
  except for explicit navigation or a successful named school/timetable save.
  Setup uses the selected school's saved definition, not unsaved timetable edits or teacher overrides in another
  timetable alternative. The source description makes this distinction visible.
- G4. Section navigation, record search and focused editing work without completing earlier sections. Search
  matches displayed names and identifiers without changing stored records; dozens of teachers and parallel classes
  do not require opening every record or editing one enormous form.
- G5. At widths from 320 CSS pixels, controls wrap inside the viewport; grids can switch days without losing
  markings or hiding actions. Desktop uses the full application area. The underlying mobile whole-week timetable
  still fits its viewport. Existing compact palette, borders and monospace time labels remain recognizable.
- G6. All actions, matrices and disclosures are keyboard-operable, with visible focus and text labels for rule
  strength and markings. Opening moves focus inside the editor; closing restores the entry focus. The covered
  timetable cannot receive edits. Reduced-motion preference suppresses the slide without delaying access.
- G7. Draft storage is browser-local to this origin/device. No account recovery, cross-device retention or editing
  of another visitor's temporary workspace is implied. Supplied names are rendered as text.
- G8. Acceptance of editor use cases requires agent-run real-browser journeys with isolated disposable storage,
  desktop and narrow views, reload/reopen, failures and browsing at least 40 teachers and 20 classes. Passing checks must include
  observable outcomes and absence of browser runtime errors; source-only assertions do not establish completion.

### Postconditions

- Success: The administrator can resume the correct setup draft and return to the preserved timetable context.
- Minimal guarantee: Failure is explicit; readable saved data and current in-memory edits are not silently replaced.

## UC-2 - Arrange the school week

- Goal: Describe the school's teaching opportunities, breaks and lunch before allocating lessons.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator arranges a day or copies its pattern to selected weekdays.
- Preconditions: A setup draft exists.
- Relations:
  - Requires: UC-1, for a draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator chooses active weekdays and adds named, timed intervals to a day using Grid interval kinds.
2. The editor shows the ordered day, teaching-period numbers and interval times, distinguishing ordinary and
   reserved teaching from non-teaching intervals.
3. The administrator explicitly copies that day to selected weekdays, reviewing replacement of existing intervals.
4. Independent destination intervals are created; the administrator adjusts one day without changing another.
5. The resulting week and any issues are retained in the draft.

### Extensions

- 1a. An interval has missing times, an end not after its start, overlap, or an invalid weekday/order. Preserve the
  unfinished value, mark the affected intervals and block readiness until corrected; continue at step 1.
- 3a. Replacement/removal affects availability, lunch markings, locks or adjacency. Show every affected reference;
  require an explicit valid remap or clear decision for each before committing removal. Cancellation or unresolved
  references preserve the previous grid and settings; resume at step 3.
- 4a. An edit changes which periods neighbor one another. Recompute affected block issues and invalidate checks,
  without rewriting block requirements or silently relocating lessons; continue at step 5.

### Guarantees

- G1. Surviving periods and intervals retain identity when renamed, reordered or retimed. Copied intervals get
  independent identities. Copying the grid does not copy teacher, room or class availability markings.
- G2. New periods are available to newly authored teachers, classes and rooms unless explicitly blocked. Imported
  “all periods” availability includes new periods; an imported explicit whitelist, including an empty list, does
  not acquire new periods without an explicit availability change.
- G3. Active days are chosen from Monday through Sunday. Breaks/lunch never become placeable lessons. Reserved teaching remains subject to subject permission.
  Ordered teaching opportunities and lunch boundaries reach the kernel without relying on labels or clock gaps.
- G4. UC-1's durability, navigation, keyboard and narrow-screen guarantees apply to this day editor.

### Postconditions

- Success: A restorable, independently editable week pattern describes teaching and non-teaching time.
- Minimal guarantee: An invalid draft is retained; a refused destructive change drops no references.

## UC-3 - Define subjects and subject rules

- Goal: Maintain recognizable subjects and the supported restrictions that apply to their lessons.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator adds, edits or removes a subject.
- Preconditions: A setup draft exists.
- Relations:
  - Requires: UC-1, for a draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator finds or adds a subject and supplies its display name.
2. The editor exposes contextual scheduling settings: reserved-slot permission, optional positive weekly reserved
   maximum per class, optional positive daily maximum per class, day-edge-only and curator-lesson designation.
3. The administrator changes a setting and sees its scope and hard-rule status.
4. The subject and affected class-plan issues are retained.

### Extensions

- 2a. A reserved maximum is supplied without reserved permission, or a limit is not a positive integer. Retain the
  input and show a blocking issue without silently granting permission or changing the limit; resume at step 2.
- 2b. A rule conflicts with class plans, such as a daily maximum below a required block length. Show the affected
  plans as blocked; do not relax either rule; continue at step 4.
- 3a. Removal would leave a class plan, qualification, eligibility or retained setting referencing the subject.
  Refuse removal and identify the references to resolve first; end.

### Guarantees

- G1. Reserved permission grants access to all reserved periods and forces none to be used. The reserved maximum
  counts occurrences per class/week; the daily maximum counts all that class's subject occurrences across teachers.
- G2. Day-edge-only means first or last occupied lesson of the class's day, not a fixed clock slot. It does not
  promise the requested early/late pattern. No unsupported general subject availability matrix is introduced.
- G3. A curator lesson must be taught by that class's curator; nomination alone creates no lessons. The curator's
  own-class implicit qualification and mandatory configured-home-room rule retain their existing kernel meanings.
- G4. Identity, optional presence and unrelated retained settings survive edits. Omitted limits remain unlimited,
  not zero. Consecutive permission is never inferred from a daily maximum. UC-1's editor guarantees apply.

### Postconditions

- Success: Subjects and their explicit settings are visible and available to class plans and teacher profiles.
- Minimal guarantee: Unfinished settings remain draft issues; refusal leaves existing references intact.

## UC-4 - Define classes and their curators

- Goal: Describe student classes, nominate curators and set class-level scheduling settings.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator adds or edits a class.
- Preconditions: A setup draft exists.
- Relations:
  - Requires: UC-1, for a draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator finds or adds a class, supplying a name and positive student count.
2. The administrator optionally nominates an existing teacher as curator and an existing room as preferred home room.
3. The administrator optionally edits the Class scheduling controls. Contextual settings distinguish inherited
   defaults from explicit values and show retained availability/preferences that the normal controls do not edit.
4. The class is retained and its teaching plan is available.

### Extensions

- 1a. Required details are unfinished or a referenced teacher/room does not exist. Retain the input and link a
  blocking issue to the missing detail; continue at step 2.
- 2a. The chosen teacher already curates another class. Accept the nomination independently; continue at step 3.
- 3a. Removing a class would remove its plan or retained lesson settings. Show the complete deletion scope and
  require confirmation; protected imported occurrence details must be resolved through the advanced route first.
  Cancel/refusal preserves everything; confirmed supported removal also clears that class's eligibility/lunch
  references and invalidates readiness; end.

### Guarantees

- G1. Curator nomination creates no demand, allocation or subject qualification for other classes.
- G2. A home room is a soft preference for ordinary lessons. Applicable curator/home-room and explicit room policies
  can make it mandatory; the editor labels that difference rather than treating preference as an override.
- G3. New classes inherit the current supported kernel defaults, including zero permitted internal daily gaps,
  visible in scheduling settings. Imported explicit values and the effective defaults of an older catalog are
  preserved during conversion. Unchanged optional settings are not normalized away.
- G4. Class identity, size, curator, home room and settings follow Authoring snapshot fidelity and UC-1's guarantees.

### Postconditions

- Success: A restorable class can receive weekly demand without requiring a curator or home room.
- Minimal guarantee: Missing details are visible; no unrelated class, teacher or room is changed.

## UC-5 - Record teacher eligibility

- Goal: Describe what each teacher may teach and for which classes, without duplicating lesson allocations.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator adds or edits a teacher profile.
- Preconditions: A setup draft exists; referenced subjects/classes exist when an eligibility relationship is added.
- Relations:
  - Requires: UC-1, for a draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator finds or adds a named teacher.
2. The administrator records qualified subjects and eligible classes separately under each subject.
3. The profile shows allocations and weekly workload derived from class plans, with links to those plans.
4. The teacher profile and eligibility relationships are retained.

### Extensions

- 2a. An eligibility/qualification removal conflicts with allocations. Retain those allocations, show affected
  class-subject rows as blocking issues and require explicit correction; continue at step 3.
- 2b. A reference is missing or the profile is unfinished. Retain the draft input and identify the issue; end.
- 3a. The administrator requests removal of a teacher still allocated, nominated as curator or referenced by a
  retained rule. Refuse removal and list the references; end.

### Guarantees

- G1. Eligibility is a teacher-subject-class relationship, never the cross product of independent lists. A newly
  created teacher starts available in every teaching period but has no silently granted class eligibility.
- G2. Workload counts actual allocated occurrences once, including occurrences consumed by blocks. Curator
  nominations and unassigned demand add no teacher workload. Weekly allocation is edited only in class plans.
- G3. Profiles preserve qualification, eligibility and availability separately, including curator-specific implicit
  qualification. UC-1's browsing, fidelity and durability guarantees apply.

### Postconditions

- Success: A teacher's eligibility and derived workload are consistent with the visible class plans.
- Minimal guarantee: No eligibility edit silently reallocates or deletes teaching.

## UC-6 - Allocate a class's weekly teaching

- Goal: State required lessons and assign the teachers responsible for them.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator adds or changes a class-subject teaching row.
- Preconditions: The class and subject exist; a teacher exists when allocating to that teacher.
- Relations:
  - Requires: UC-3, for the subject
  - Requires: UC-4, for the class
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator opens a class plan and adds a subject with a positive integer weekly count.
2. The row shows required, allocated and unassigned totals and offers eligible teachers for that class and subject.
3. The administrator allocates all occurrences to one teacher or explicitly divides the count among teachers.
4. The plan and each affected teacher's derived workload update together and are retained.

### Extensions

- 2a. No suitable eligible teacher exists, or allocation is postponed. Retain the unassigned demand, explain that it
  blocks generation and allow navigation; end.
- 3a. The intended teacher lacks qualification or class eligibility. Offer an explicit action naming the change,
  such as “Allow Mari to teach Mathematics in 5A”, including qualification when needed. Only explicit acceptance
  changes eligibility; then resume at step 3. Declining leaves eligibility and allocations unchanged; end.
- 3b. Allocation exceeds demand, uses an invalid count or leaves a remainder. Show the required/allocated difference
  as a blocking issue, retain the entered draft and do not fabricate lessons to compensate; continue at step 4.
- 3c. A count/allocation change or row removal concerns a protected imported row, occurrence, lock,
  exception or series relationship. Identify it and refuse the change, retaining the previous row;
  offer the advanced JSON route; end.
- 3d. The administrator removes an ordinary row. Preview its demand, allocations and block/room rules; confirmation
  removes those together, while cancellation preserves them; end.

### Guarantees

- G1. There is one ordinary demand row per class and subject. Allocations are positive counts per teacher; an
  unassigned remainder is not a teacher and produces no kernel lesson. Ready rows allocate exactly their demand.
- G2. Each allocated occurrence produces one explicitly assigned lesson. Stable surviving occurrence identities,
  names and series are retained. New ordinary rows use one spreading series per class-subject, including split
  teaching. Count increases create only the additional occurrences; safe reductions remove the most recently
  created unprotected occurrences. Imported per-occurrence differences remain visible and protected.
- G3. A new row has no consecutive rule: undeclared consecutive runs are hard-forbidden under the new catalog,
  while separated occurrences may share a day. A subject daily cap is independent.
- G4. No selection alone extends eligibility. Imported curator-specific implicit qualification does not become
  general qualification or eligibility. Retained hard settings are not overwritten by the normal row controls.
- G5. Authoring snapshot fidelity and UC-1's navigation, keyboard and autosave guarantees apply.

### Postconditions

- Success: The class has an independently owned weekly plan with accurate totals and derived teacher workloads.
- Minimal guarantee: Unassigned/invalid work remains recoverable; refused edits change no protected occurrence.

## UC-7 - Copy a class curriculum

- Goal: Reuse subjects and weekly demand while assigning destination teachers independently.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator requests a curriculum copy to another class.
- Preconditions: A source plan and a distinct destination class exist.
- Relations:
  - Requires: UC-6, for a source curriculum
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator selects a source plan and destination class.
2. A preview lists the subjects and required weekly counts to copy and explains that teachers and contextual
   class/row settings are not copied.
3. The administrator confirms the copy into an empty destination plan.
4. Independent destination rows appear with all demand unassigned; source allocations and teacher workload remain
   unchanged. The destination plan is retained for separate allocation.

### Extensions

- 2a. The destination already has teaching rows. Refuse replacement, preserve both plans and explain that this
  action requires an empty destination; end.
- 2b. Source demand is missing or not a positive integer. Identify the source rows to correct without partially
  copying the plan; end.
- 3a. The administrator cancels. Preserve both plans exactly; end.

### Guarantees

- G1. Copy includes only subject references and weekly counts. It does not copy teachers, eligibility, curator,
  home room, availability, lunch, block rules, fixed rooms, occurrence identities, locks or exceptions.
  School-wide subject rules still apply because the subjects are shared.
- G2. Later edits never propagate between source and destination. Copy is one complete draft change under UC-1's
  durability and fidelity guarantees.

### Postconditions

- Success: The destination has the copied curriculum and explicit unassigned demand.
- Minimal guarantee: Refused or cancelled copy has no partial destination rows and no source mutation.

## UC-8 - Mark teacher availability and preferences

- Goal: Express hard working limits and softer time preferences on the school grid.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator marks a teacher's weekly availability.
- Preconditions: A teacher and teaching periods exist.
- Relations:
  - Requires: UC-2, for the grid
  - Requires: UC-5, for the teacher
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator opens a teacher's week, with times and reserved slots visible.
2. The administrator chooses a labelled state from Availability markings and applies it to individual periods,
   selected periods, a whole day or a defined afternoon range.
3. The grid shows the resulting marking and its hard/soft meaning; the administrator can clear or replace it.
4. The markings are retained and linked load/placement issues are updated.

### Extensions

- 2a. No teaching periods exist in the chosen range. Explain that no marking changed and link back to the grid; end.
- 2b. The change leaves assigned demand without enough available periods or eliminates a required block option.
  Retain the marking and show the affected rows as blocking issues; continue at step 4.
- 3a. An imported unavailable period also contains a retained avoidance preference. Show both facts with hard
  unavailability taking precedence; preserve the preference until the administrator explicitly replaces/clears the
  marking, explaining that replacement changes both facts for those periods; continue at step 4.

### Guarantees

- G1. New teachers default to available. Unavailable is a hard exclusion; Prefer to avoid permits placement with
  a soft penalty; Available clears both author-authored restrictions for the selected periods. Color alone does
  not communicate the state.
- G2. Reserved permission is independent of teacher availability. Break/lunch intervals cannot be marked as
  placeable periods. Grid edits preserve whitelist versus all-period intent as in UC-2.
- G3. Markings preserve exact period references and imported optional presence unless explicitly edited.
  UC-1's keyboard, day navigation and durability guarantees apply.

### Postconditions

- Success: The teacher's hard availability and advisory avoidance are distinguishable and retained.
- Minimal guarantee: No marking silently reallocates teaching or relaxes another hard requirement.

## UC-9 - Arrange class-specific lunch

- Goal: Block different classes for lunch at different teaching periods.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator marks or clears lunch periods for a class.
- Preconditions: A class and teaching grid exist.
- Relations:
  - Requires: UC-2, for the grid
  - Requires: UC-4, for the class
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator opens the class week and selects teaching periods as that class's lunch.
2. The editor distinguishes these lunch blocks from school-wide lunch and other imported class restrictions.
3. The class becomes unavailable in those periods; other classes retain their own availability.
4. Lunch markings and resulting readiness issues are retained.

### Extensions

- 1a. The administrator clears a lunch mark where another class restriction also applies. Remove only the lunch
  reason and keep the period unavailable, explaining the remaining restriction; continue at step 3.
- 3a. Lunch removes a required block option or leaves insufficient capacity. Keep the intended lunch and show the
  affected demand/rule as a blocking issue without moving lunch or lessons automatically; continue at step 4.

### Guarantees

- G1. Lunch blocks only existing teaching periods for that class and does not change the common bell times.
  School-wide lunch remains a separate non-teaching interval.
- G2. A class lunch period cannot be skipped to create block adjacency. It is excluded from that class's gap
  accounting, so lunch itself does not create a forbidden internal teaching gap.
- G3. Saved/reopened authoring and class timetable inspection identify lunch by name and time. Other imported
  unavailable periods are not relabelled as lunch. UC-1's fidelity, keyboard and durability guarantees apply.

### Postconditions

- Success: The class has a visible, restorable lunch pattern independent of other classes.
- Minimal guarantee: No lunch operation silently clears another restriction or changes another class.

## UC-10 - Define rooms and placement requirements

- Goal: Let generation choose suitable rooms while making mandatory placement requirements explicit.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator maintains a room or a contextual room requirement.
- Preconditions: A setup draft exists; the relevant class/subject/plan exists when its policy is edited.
- Relations:
  - Requires: UC-1, for a draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator finds or adds rooms with names, nonnegative capacities and capabilities, and optionally marks
   hard room availability when teaching periods exist. New rooms start available.
2. The administrator optionally chooses a class home-room preference, a subject's nonempty allowed-room pool,
   or one mandatory fixed room for all weekly occurrences of a class-subject row.
3. Contextual settings show the distinction between preferences, allowed-room restrictions, required capabilities
   and fixed room requirements, including applicable retained teacher-subject policies.
4. The editor retains these settings and shows affected placement issues.

### Extensions

- 1a. Values or room references are missing/invalid. Retain the unfinished input and identify the issue; end.
- 2a. No room satisfies the intersection of hard policies, capacity, capabilities and availability. Show the empty
  intersection and contributing rules; retain the draft and do not choose an overriding rule; continue at step 4.
- 2b. A row-wide change would overwrite differing imported occurrence locks or exceptions. Refuse the change,
  identify protected occurrences and offer the advanced route; end.
- 3a. Removal of a room would leave a home-room, policy, preference or occurrence reference. Refuse removal and
  list the references to resolve; end.

### Guarantees

- G1. Without a fixed room, suitable-room selection is automatic. No soft home-room preference overrides capacity,
  capability, availability, subject/teacher-subject policy, curator rules or a fixed room.
- G2. A row's fixed room applies to every occurrence, across split teachers and blocks. A required block shares one
  room throughout. Per-occurrence room subsets are retained on import but not authored by this row control.
- G3. Room values, policies and optional presence follow Authoring snapshot fidelity. UC-1's keyboard, browsing
  and autosave guarantees apply.

### Postconditions

- Success: Rooms and visibly hard or soft placement requirements are available to readiness and generation.
- Minimal guarantee: No conflict is resolved by silently discarding a hard requirement.

## UC-11 - Require consecutive lesson blocks

- Goal: Explicitly require consecutive runs of specified lengths within a class's weekly subject demand.
- Primary actor: Administrator
- Supporting actors: none
- Trigger: The administrator adds, changes or removes a block rule in a class-subject plan.
- Preconditions: The class-subject plan exists; its grid and allocations may still be incomplete.
- Relations:
  - Requires: UC-6, for weekly demand
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator opens the plan's scheduling settings and sees that undeclared consecutive lessons are forbidden.
2. The administrator specifies an integer block length of at least two, a positive weekly block count and an
   operator from Block frequency operators.
3. The editor shows the consumed weekly occurrences, remaining demand, common-teacher/common-room condition and
   whether the rule is a minimum or an exact count. Additional lengths can be declared with separate rules.
4. The editor checks necessary conditions and retains the explicit rules for kernel enforcement.

### Extensions

- 2a. Length/count is invalid, a length is duplicated, or minimum required consumption exceeds weekly demand.
  Retain the input and identify the blocking issue; resume at step 2.
- 3a. The grid is incomplete, no allocation can supply a whole block, the daily cap is too small, or there is no compatible consecutive
  run of periods and common room. Show the affected rule and reason; retain it without reducing its length or
  weakening it to a preference; continue at step 4.
- 3b. A protected imported series or occurrence structure cannot accept the rule without changing unrelated
  semantics. Refuse the rule change and identify the advanced values to resolve; end.
- 4a. The administrator removes a rule. Explain that runs of that length become forbidden; on confirmation remove
  the rule and invalidate checks, without changing weekly counts or teacher allocations; end.

### Guarantees

- G1. Rules are hard and have exactly the semantics in Consecutive block semantics and Block examples.
  Length and weekly frequency are never conflated. No length is pre-authorized by another length.
- G2. A block consumes existing occurrences once; it adds neither lessons nor workload. One teacher's allocation
  supplies the entire block, and every occurrence uses the same room. Hard requirements apply to every occurrence.
- G3. Each lesson remains a separate timed cell, with ordinary breaks visible. Lunch interrupts adjacency.
  No grouping preference or hand-picked period lock substitutes for a required block.
- G4. Saving/reopening retains class, subject, length, operator and frequency exactly. UC-1's presentation and
  durability guarantees apply. UI acceptance alone cannot establish kernel enforcement.

### Postconditions

- Success: The plan declares precisely the permitted and required consecutive block lengths and frequencies.
- Minimal guarantee: Invalid rules remain visible draft work; no rule is silently weakened or demand increased.

## UC-12 - Obtain a timetable under declared scheduling rules

- Goal: Obtain complete assignments that satisfy the supplied definition, including its consecutive-lesson policy.
- Primary actor: Kernel caller
- Supporting actors: none
- Trigger: The caller requests generation with a complete supported definition and search controls.
- Preconditions: Inputs are supplied through a published kernel boundary; no setup UI state is required.
- Relations:
  - Requires: none
  - Includes: UC-13 at step 4
  - Extends: none

### Main success scenario

1. The caller supplies the definition and a bounded search request.
2. The kernel validates references, counts, block rules and declared adjacency under the supplied rule version.
3. The kernel searches for complete period and room assignments while respecting all mandatory constraints.
4. The kernel independently reviews the complete candidate through UC-13 against the exact supplied definition.
5. The caller receives a feasible result only after successful review, with complete assignments, definition
   revision, rule version, run/build provenance and quality evidence.

### Extensions

- 2a. Input or block metadata is invalid or unsupported. Return located errors identifying class, subject and rule
  where applicable; publish no timetable and preserve any previously published result; end.
- 3a. Search ends without a complete valid candidate. Return the bounded outcome and known conflicts without
  claiming a feasible timetable or proof of impossibility; end.
- 3b. Cancellation or execution failure interrupts search. Return its actual outcome and publish no unchecked
  candidate; end.
- 4a. Review fails, is incomplete or finds a hard violation. Reject the candidate, report the failed check and
  publish no feasible result; end.

### Guarantees

- G1. New-rule generation enforces Consecutive block semantics, including the hard prohibition when no rule
  exists. It cannot satisfy a block by increasing demand, changing an assigned teacher, ignoring a room restriction,
  skipping periods or crossing lunch.
- G2. New definitions contain sufficient ordered period and lunch-boundary information to reproduce adjacency
  without the editor. Eligibility remains authoring data; kernel lessons still have explicit qualified teachers.
- G3. Ordinary spreading and explicit block requirements interact exactly as defined in Consecutive block
  semantics. Soft quality never offsets a hard violation.
- G4. Supported older rule versions retain their original feasible-set and scoring semantics. The new rule version
  cannot be inferred merely from missing block metadata in an older definition.
- G5. A reused result must match the complete definition, rule semantics, relevant search controls and compatible
  checking build. No previous version's feasible label substitutes for current review.
- G6. Acceptance exercises published generation and verification boundaries with disposable definitions, including
  default prohibition, doubles, triples, longer blocks, mixed lengths, weekly frequencies, ordinary breaks, lunch,
  split allocation, room conflicts and the Block examples. UI controls or internal-only checks are insufficient.

### Postconditions

- Success: A complete, independently checked feasible result matches the exact supplied definition and provenance.
- Minimal guarantee: No invalid, incomplete, stale or mismapped result is published as feasible.

## UC-13 - Review complete assignments against a definition

- Goal: Determine whether a particular arrangement satisfies the supplied definition without solving again.
- Primary actor: Kernel caller
- Supporting actors: none
- Trigger: The caller submits a complete assignment set for review or verification.
- Preconditions: A supported complete definition and purported assignments are supplied.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The caller supplies the definition and assignments.
2. The kernel checks that every declared lesson has exactly one assignment with matching subject, class and
   teacher, and known period and room references.
3. The kernel evaluates all hard constraints and soft preferences under the definition's rule version.
4. The caller receives the validity result, hard conflicts, advisory quality and matching definition provenance.

### Extensions

- 2a. Lessons are missing, duplicated, extra or mismatched, or input is invalid. Return located errors and no
  valid result; do not silently remap identities or fill missing placements; end.
- 3a. A consecutive run is undeclared, has the wrong length, crosses an invalid boundary, changes teacher/room,
  or misses/exceeds a block frequency. Return an invalid review identifying class, subject, expected rule,
  observed runs/counts and affected lesson/period references; continue at step 4.
- 3b. Any other hard requirement fails. Return the affected lessons/rules as hard conflicts; continue at step 4.
- 3c. Review cannot complete or the rule version is unsupported. Explain that validity is unestablished, preserve
  the input and return no valid result; end.

### Guarantees

- G1. Review and result verification use exactly the same block and adjacency semantics as generation. Both the
  explicit requirements and absence-of-permission prohibition participate in validation.
- G2. Review never changes assignments or starts a solver. Hard-invalid arrangements can be retained as manual
  drafts but cannot be saved or displayed as valid named timetable alternatives.
- G3. Soft dissatisfaction alone does not invalidate an arrangement. Failure of this arrangement does not establish
  that the school has no feasible timetable.
- G4. Result fields preserve complete lesson/subject/class/teacher/period/room assignment identities and current
  review provenance. Truncated diagnostic examples are labelled; they never conceal the overall invalid state.
- G5. Acceptance calls the published review and verification boundaries on both valid and deliberately invalid
  arrangements, including each Block example and legacy behavior. A generator's success claim is not review evidence.

### Postconditions

- Success: The caller can distinguish a valid arrangement from hard conflicts and unestablished validity.
- Minimal guarantee: An unavailable or failed check never becomes a successful validation or mutates its input.

## UC-14 - Review setup readiness

- Goal: Understand remaining setup work before saving or generating.
- Primary actor: Administrator
- Supporting actors: definition checking service
- Trigger: The administrator opens Review or requests a fresh readiness check.
- Preconditions: A setup draft exists, including when it is incomplete.
- Relations:
  - Requires: UC-1, for a draft
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests review of the current draft revision.
2. The editor presents class demand, allocated and unassigned totals, teacher loads, regular teaching capacity
   and separately labelled reserved capacity.
3. The editor checks authoring consistency and necessary placement conditions. If complete, it submits the exact
   compiled definition for authoritative definition validation.
4. The administrator sees either Ready or blocking issues linked to the relevant record, field or interval,
   alongside a separate summary of hard requirements and soft preferences.

### Extensions

- 3a. Details, eligibility, allocations, timing, rooms or conversion decisions are incomplete. Show blocking
  issues without inventing defaults or submitting placeholder lessons; continue at step 4.
- 3b. Necessary checks find overload, no suitable room, an incompatible hard policy, or no eligible consecutive
  window/common teacher allocation for a required block. Explain the affected demand and conditions; continue at step 4.
- 3c. Checking fails or the service is unavailable. Show Check failed and retain the draft; do not enable named
  save or label the current revision ready; end.
- 4a. The draft changed during checking or a response has different definition/build provenance. Ignore that
  response for readiness, show that the newer revision needs review and preserve its values; end.

### Guarantees

- G1. Required and allocated counts count individual occurrences, not blocks. Block consumption is explanatory,
  never added again. Over-allocation is named explicitly; spare grid capacity is permitted and not a missing lesson.
- G2. Class regular capacity excludes hard-unavailable/lunch periods. Reserved capacity is shown separately with
  subject permission explained. It is never advertised as universally usable capacity.
- G3. Readiness requires at least one class, subject, teacher, room, teaching period and allocated lesson, positive
  demand counts, complete exact allocations, valid eligibility and references,
  resolved import/grid decisions, satisfied definition-level necessary checks and a matching authoritative check.
  It does not require every optional entity to be used, a curator, a home room or filling every period.
- G4. Necessary-condition success is not a complete timetable feasibility claim. Search failure is not proof of
  impossibility. Hard requirements, definition errors and soft preferences have distinct labels.
- G5. Every check is bound to the exact authoring/compiled-definition revision, rule version and checking build.
  Effective demo limits are enforced and explained at affected values, including the 2 MiB UTF-8 definition limit
  and configured lesson/complexity limits. UC-1's keyboard, issue navigation and retention guarantees apply.

### Postconditions

- Success: The current revision is Ready or has actionable blocking issues, with accurate accounting.
- Minimal guarantee: An unavailable, stale or partial check never authorizes saving an unchecked school copy.

## UC-15 - Exchange definitions with the advanced JSON tool

- Goal: Explicitly transfer a supported definition while understanding what authoring information is retained.
- Primary actor: Administrator
- Supporting actors: definition checking service
- Trigger: The administrator requests kernel JSON import, conversion of a source school, export, or authoring recovery.
- Preconditions: A setup draft or retained source draft exists; the advanced JSON tool remains independently usable.
- Relations:
  - Requires: UC-1, for a draft/source context
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator chooses a source definition or JSON document for import.
2. The system validates it and previews reconstructed subjects, teachers, classes, rooms, periods, individual
   occurrences and aggregate counts, plus retained settings that the normal controls cannot edit.
3. The preview explains absent authoring data and, for an older catalog, adoption of the new default prohibition.
   The administrator explicitly accepts conversion into a separate draft.
4. The draft retains the source scheduling information, observed teacher-subject-class eligibility and any
   authoring recovery data supplied in a recognized recovery document.
5. On a later explicit export, the administrator reviews the scope and receives the current complete kernel JSON
   or a full authoring recovery document, as selected. The advanced JSON buffer changes only on an explicit transfer.

### Extensions

- 2a. JSON is malformed, duplicated, oversized, unsupported, contains invalid references, or carries repair ancestry
  unsupported for initial generation. Identify the location/reason, preserve the input and existing draft and
  refuse import; end.
- 2b. Mapping cannot preserve scheduling semantics. Identify unmappable values and refuse conversion while
  retaining the source and advanced route; end.
- 3a. The administrator declines adoption or import. Preserve source, working draft and JSON buffer exactly; end.
- 3b. Another working draft would be replaced. Retain it independently and show the new draft's source; no import
  is allowed to overwrite that work; continue at step 4.
- 5a. The draft is incomplete or cannot compile faithfully. Refuse kernel JSON export, explain the blocking values
  and offer a full authoring recovery document instead; end.
- 5b. Download or explicit buffer transfer fails. Report failure and retain both source and working buffers; end.
- 1a. A recognized authoring recovery document is selected. Validate its authoring version and complete field
  mapping; restore into a separate draft, or retain the document and explain unmappable values; end.
- 1b. Export is requested without an import. Use the current draft without converting or replacing it; continue at step 5.

### Guarantees

- G1. Kernel JSON imports preserve identifiers, labels, counts, optional presence, explicit empty lists, series
  membership or absence, locks, exceptions, all applicable policies and soft weights. Aggregate presentation never
  silently re-groups a legacy series or homogenizes individual lessons.
- G2. Conversion to new rules explicitly shows the added default prohibition and materialized settings needed to
  retain other old defaults/weights. Only these previewed changes are authorized. No block permission is inferred
  from an existing schedule, repeated subject, series, daily cap or lock.
- G3. Eligibility reconstructed from JSON contains only observed assigned teacher-subject-class relationships.
  Existing qualifications remain distinct. JSON alone supplies no broader eligibility, unassigned demand or named
  lunch information. Unavailable periods are not automatically called lunch; clock gaps do not imply lunch.
- G4. Missing imported times remain explicitly unspecified until edited; order is retained and no clock times are
  fabricated. Non-teaching interval names and classes' lunch reasons are recovered only from authoring metadata.
- G5. Imported rows with differing occurrence restrictions, mixed series membership or a series shared with another
  row show their retained individual settings and protect count, allocation, block and row-wide policy edits.
  Homogeneous rows retain their common settings when adding occurrences; entirely ungrouped rows stay ungrouped.
  Individually locked/exceptional occurrences also remain protected from removal or reassignment. Refusals identify
  the advanced values to resolve; editing unrelated records remains possible. Unknown authoring versions are not
  silently downgraded.
- G6. Kernel JSON export includes the new consecutive policy, rules and adjacency needed for equivalent scheduling,
  but is labelled as excluding unfinished demand, extended eligibility and other editor-only intent. A recovery
  document preserves all fields in Authoring snapshot fidelity, including incomplete input and source identity.
- G7. Import/export is explicit and does not synchronize editors continuously, save a named school, start a solve,
  accept repaired assignments, or mutate the source. UC-1's input rendering and durability guarantees apply.

### Postconditions

- Success: A separate faithful draft or an explicitly scoped export is available without hidden data loss.
- Minimal guarantee: Unsupported conversion or failed transfer leaves existing work and source data intact.

## UC-16 - Save a named school definition copy

- Goal: Keep a complete school definition without having to generate a timetable.
- Primary actor: Administrator
- Supporting actors: browser storage, definition checking service
- Trigger: The administrator requests Save school copy.
- Preconditions: A setup draft exists and browser-local saving can be attempted.
- Relations:
  - Requires: none
  - Includes: UC-14 at step 2
  - Extends: none

### Main success scenario

1. The administrator supplies the new school's display name and requests save.
2. The exact draft revision and target name are checked through UC-14.
3. With Ready evidence and a nonblank name of at most 120 characters, unique among bundled and local school names
   after trimming and case-insensitive comparison, the system atomically saves a new local school identity,
   complete kernel definition, authoring snapshot and matching check provenance.
4. The new school is selected with No timetable yet, and its saved source draft revision is consumed. The
   administrator can reopen setup, leave the school without a timetable or explicitly generate later.

### Extensions

- 1a. The name is blank, too long or already used. Retain the draft/name and explain the correction; end.
- 2a. Review does not produce Ready for this revision. Save no school copy and show the issues or check failure; end.
- 3a. The name or draft changed while checking. Refuse that save, retain the newer work and require a fresh save
  request; end.
- 3b. Atomic storage fails or another tab has changed it. Publish no school, consume no draft and claim no success.
  Preserve the working draft and prior durable values with UC-1's failure/recovery feedback; end.

### Guarantees

- G1. New school identity is independent of its source and label. Surviving entity/occurrence identifiers and
  scheduling settings remain stable; the explicitly chosen school display name appears in the saved definition.
  Browser identity and complete definition revision, not a reused kernel school identifier or name, distinguish copies.
- G2. Definition, authoring snapshot and validation evidence are saved together with the exact fields in Authoring
  snapshot fidelity. A copy cannot be reopened with metadata from another revision.
- G3. A named incomplete copy is not created. Incomplete work remains an autosaved draft. This action neither
  invokes a solver nor changes existing definitions, timetables or another draft.
- G4. Later edits start a successor draft and another named save creates another school copy. Autosave is not
  product version activation. UC-1's navigation, storage and keyboard guarantees apply.

### Postconditions

- Success: A checked, separately named school is durable in this browser, independently of any timetable.
- Minimal guarantee: Failure retains the draft and source school without a partially saved copy.

## UC-17 - Generate and keep a school timetable (primary)

- Goal: Produce and retain a valid timetable from an explicitly saved school definition.
- Primary actor: Administrator
- Supporting actors: generation service, browser storage
- Trigger: The administrator requests generation for a saved local school.
- Preconditions: A named school copy exists; its saved definition is complete and supported.
- Relations:
  - Requires: UC-16, for a saved school snapshot
  - Includes: UC-12 at step 4
  - Extends: none

### Main success scenario

1. The administrator identifies the saved school and explicitly chooses a search effort.
2. The interface shows the effective Quick/Thorough budgets and explains that generation uses the saved definition,
   identifying any newer unsaved setup draft.
3. Generation pins the school identity, complete definition revision, rule version, authoring/grid snapshot and
   compatible build, then reports preparation, queue, running and result-checking progress.
4. The service obtains a checked timetable through UC-12 using that pinned definition.
5. The verified result appears as a candidate belonging to the originating school, ready for inspection and naming.
6. The administrator inspects the timetable, supplies a nonblank timetable name unique within that school
   (trimmed, case-insensitive, at most 120 characters), and explicitly keeps it.
7. The complete candidate and its matching definition/grid/provenance are atomically saved as a separately named
   timetable; the pending candidate is removed and the saved timetable can be reopened after reload.

### Extensions

- 1a. The source is bundled, the current setup is incomplete/unsaved, or a prior candidate awaits a decision.
  Explain the required school-copy save or candidate save/discard; start no solve; end.
- 2a. Saved evidence needs checking against the current build. Recheck that exact saved definition; an invalid or
  unavailable check blocks generation without modifying the school; otherwise continue at step 3.
- 3a. Session expiry, admission/allowance limits, another active session run, service failure or lost provenance
  prevents starting/recovering generation. Explain the actual condition, preserve the school and allow an explicit
  retry; end.
- 3b. The administrator requests cancellation of an active run. On acknowledgement mark it Cancelled and keep
  the school and all timetables; on failure report failure and continue reporting its actual state; end.
- 4a. No complete valid timetable is found within the search budget. Show Unsuccessful and available diagnostics;
  retain school and draft without claiming impossibility; end.
- 4b. A result lacks matching school/run/definition/build provenance, complete assignments or successful review.
  Reject it and show the failure; save no timetable; end.
- 5a. The administrator is editing or viewing another school when generation completes. Keep the candidate attached
  to its origin, show an origin-specific notification and change no current selection or draft; end.
- 5b. Candidate persistence fails. Retain the candidate only in memory with explicit durability feedback and retry;
  do not claim it will survive closing the page; continue at step 6.
- 6a. The administrator explicitly discards the candidate. Remove only that candidate; preserve the saved school,
  existing timetables and setup drafts; end.
- 6b. A manual change is made through the existing timetable-edit journey. Keep it as a separate manual draft and
  review its exact resulting definition/assignments through UC-13. Until valid it cannot be saved; its teacher
  overrides do not rewrite the school's setup baseline; continue at step 6.
- 7a. The name is invalid/duplicate or saving fails, is stale or cannot persist atomically. Retain the candidate,
  its name and working changes, preserve existing versions and explain the correction/retry; end.

### Guarantees

- G1. Only an explicit request starts generation. Saving setup, selecting a school, reopening the editor, reload,
  or failure never starts another run. Only one active browser generation is admitted; existing server queue and
  visitor allowances remain authoritative.
- G2. The current deployed budgets, size guidance, complexity controls, result reuse and cancellation semantics
  remain visible. Budget exhaustion or extra search time gives no feasibility guarantee.
- G3. Neither later setup edits nor navigation changes the pinned generation input. Late responses can update
  only their originating run/candidate, never a newer draft, another school or data removed through UC-18.
- G4. The candidate is never saved under an existing timetable's identity or automatically given a durable name.
  Generation completion does not force-close the setup editor or steal editing focus.
- G5. Timetable rendering retains actual times, named ordinary breaks, school-wide lunch, reserved markings and
  class-specific lunch from the pinned snapshot. Consecutive lessons remain separate cells with breaks visible;
  no merged block spans a break. Class lunch is identified when inspecting that class.
- G6. Complete assignment fields, definition, rule version, run/build/review provenance and origin snapshot are
  preserved as in Authoring snapshot fidelity. A later manual alternative's exact definition governs its own
  assignments; the originating setup snapshot remains clearly identified provenance.
- G7. New-rule manual review enforces both declared blocks and default prohibition through UC-13. Invalid edits
  can be explored as drafts but cannot become valid named alternatives.
- G8. UC-1's keyboard, narrow-screen and failure guarantees apply. Acceptance requires the full empty-school
  browser journey through named school save, real generation, inspection, timetable save and reload, plus an
  interrupted/unsuccessful attempt that leaves saved data intact.

### Postconditions

- Success: A named, durable and verified timetable is associated with the exact originating school definition.
- Minimal guarantee: Failure or cancellation retains saved schools, prior timetables and recoverable working data;
  no unchecked arrangement is reported or saved as valid.

## UC-18 - Delete local school artifacts

- Goal: Remove this demo's saved schools, timetables and unfinished authoring from this browser.
- Primary actor: Administrator
- Supporting actors: browser storage
- Trigger: The administrator requests local-data cleanup in Utilities.
- Preconditions: The demo is open; local data may be empty, unreadable or from an older version.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator reads that cleanup removes this browser's local school definitions, timetables, candidates,
   setup/manual/JSON drafts, authoring metadata and related artifact selections, keeping bundled examples.
2. The administrator requests cleanup and explicitly confirms its irreversible local scope.
3. The system removes those artifacts, invalidates their pending callbacks/writes and closes any deleted setup.
4. The bundled default timetable opens. Deleted local entries remain absent after reload and new work can be saved.

### Extensions

- 2a. Confirmation is cancelled or dismissed. Preserve stored values and current work exactly; end.
- 3a. Removal fails. Restore the prior exact raw values where possible and report failure. If restoration also
  fails, explicitly report possible partial deletion and offer reload/retry rather than claiming preservation; end.
- 3b. Stored entries are unreadable. Delete their raw values without requiring them to parse; continue at step 4.

### Guarantees

- G1. Cleanup includes authoring snapshots and recovery metadata retained by this demo, including earlier formats.
  It does not delete downloaded recovery files, unrelated browser values, cookies, bundled examples or another
  browser's data, and issues no server mutation request.
- G2. Cancel receives initial confirmation focus. Scope and actions remain readable and keyboard-operable at
  320 CSS pixels. No cleanup occurs merely by opening setup or selecting a school.
- G3. Stale tabs, delayed autosaves, checks and generation completions cannot resurrect deleted artifacts or claim
  success for a refused write. Unknown/raw values are preserved exactly during a possible rollback.

### Postconditions

- Success: Only bundled school artifacts remain and deleted setup cannot reappear through stale work.
- Minimal guarantee: Cancellation changes nothing; failure is honest and affects no unrelated storage.

## Normative data

### Setup sections

School setup also includes a **Student gaps and teacher workload** monitoring tab. It displays the selected
school/timetable's checked results, independently of setup edits, and is available before creating a draft.
Without a timetable for that school, it explains that monitoring is unavailable. Class/period and teacher
links return to the corresponding timetable view. This monitoring panel does not appear on the timetable.


The suggested navigation contains exactly these rows — no more, no fewer. The order is guidance, not a prerequisite
chain; contextual scheduling settings and import/export are actions within these sections or the header.

| Order | Label | Contents |
| --- | --- | --- |
| 1 | Timetable grid | Active days, timed day pattern, copying, breaks, lunch and reserved teaching |
| 2 | Subjects | Subject catalog and supported subject rules |
| 3 | Classes | Class details, curator, weekly curriculum, allocations, lunch and contextual rules |
| 4 | Teachers | Qualification, eligibility, availability and derived workload |
| 5 | Rooms | Room details, hard availability and placement settings |
| 6 | Review | Totals, blocking issues, hard/soft summary, named save and generation |

### Grid interval kinds

Exactly these rows — no more, no fewer:

| Kind | Teaching opportunity | Effect on consecutive adjacency |
| --- | --- | --- |
| Lesson | Yes | Participates in the ordered teaching grid |
| Reserved lesson | Yes, for permitted subjects | Participates; subject permission still applies |
| Break | No | Does not interrupt neighboring teaching periods |
| Lunch | No | Interrupts neighboring teaching periods |

### Availability markings

Exactly these rows — no more, no fewer:

| Marking | Meaning |
| --- | --- |
| Available | No authored hard block or avoidance preference for the selected teaching periods |
| Unavailable | Hard exclusion from those teaching periods |
| Prefer to avoid | Placement allowed, with the applicable soft penalty |

Imported simultaneous hard/soft facts remain distinguishable as specified in UC-8; they are not a fourth marking tool.

### Class scheduling controls

Exactly these rows — no more, no fewer. Counts are per class. Defaults are for newly authored definitions;
conversion preserves older effective behavior explicitly. Unfinished or invalid values remain blocking draft input.

| Control | Strength and allowed value | Default and meaning |
| --- | --- | --- |
| Maximum internal gaps per day | Hard; nonnegative integer or Unlimited | Zero; counts unused available teaching periods between occupied periods within an available block, excluding lunch and unavailable periods |
| Latest start | Hard; positive regular-period position or Unlimited | Unlimited; the first lesson must start by this regular teaching opportunity; reserved periods do not increment the position |
| Preferred latest start | Soft; positive regular-period position or No preference | Third regular opportunity; later starts incur a preference penalty |
| Maximum difference in daily lesson counts | Hard; nonnegative integer or Unlimited | Unlimited; applies between every pair of weekdays having an available regular period, including untaught days |
| Preferred difference in daily lesson counts | Soft; nonnegative integer tolerance or No preference | One; each pair of such weekdays incurs cost for the difference exceeding the tolerance |

### Block frequency operators

Exactly these rows — no more, no fewer:

| Operator | Required weekly count of maximal runs of the declared length |
| --- | --- |
| At least | The observed count must be greater than or equal to the declared positive count |
| Exactly | The observed count must equal the declared positive count |

### Consecutive block semantics

These definitions are normative for UC-11, UC-12, UC-13, UC-14 and UC-17:

1. For a class and subject, a run is a maximal sequence of occupied neighboring teaching periods on one weekday
   bearing that subject. Neighboring is determined from the whole declared teaching grid, not just occupied or
   available periods. An intervening other subject, empty teaching period, class lunch period or school-wide lunch
   interrupts a run. Ordinary non-teaching breaks do not. A day boundary always interrupts.
2. A run of one is an ordinary single. A run of length N ≥ 2 is permitted only when that class-subject plan has an
   explicit rule for exactly length N. Permission for a double does not authorize a triple, four-lesson run or
   other length. Changing teacher or room inside a same-subject run does not turn it into separate singles.
3. Every occurrence in a permitted run must have the same assigned teacher and room and satisfy all other hard
   rules. One teacher's allocation must supply the entire block. Eligibility, qualification, availability,
   reserved-slot limits, daily caps, room policies and locks are never relaxed by block permission.
4. One maximal run counts once for its exact length. Runs cannot overlap, share occurrences, split into smaller
   blocks or be concatenated by skipping an unoccupied/unavailable period. Weekly counts are summed over days
   and teachers for that class-subject.
5. A plan may have one rule per length. Each length is an integer ≥ 2; each frequency is a positive integer.
   The sum of length × required frequency over all rules must not exceed weekly demand. An “at least” rule
   permits extra maximal runs of that same length within the existing demand; undeclared lengths remain forbidden.
   Removing all rules restores the default hard prohibition on every run of length two or more.
6. The new-rule default applies even when the rule collection is absent or empty. Older catalog versions retain
   their original behavior. Import adoption is explicit and cannot infer a rule from placements or locks.
7. New ordinary rows share one spreading series per class-subject. Let S be the sum over days of
   max(0, that series' occurrence count on the day − 1). For each declared length N and required weekly count K,
   let C be its observed number of otherwise valid maximal blocks. Subtract
   (N − 1) × min(K, C) from S for that rule, flooring the final result at zero. This exempts mandatory grouping
   from the ordinary same-day spreading penalty. Additional blocks permitted by “at least”, and extra separated
   same-day lessons, retain ordinary clustering cost. No rule means no exemption. Other preferences still apply;
   no preference can make a hard-invalid timetable valid.
8. Imported series identities and absence are preserved. A row whose grouping cannot support the explicit block
   scoring semantics without changing unrelated series behavior is protected from that rule edit until explicitly
   restructured through JSON. Legacy results retain their original scoring; adoption previews any default/weight
   materialization needed to preserve unrelated behavior.

### Block examples

Exactly these rows — no more, no fewer. Each is an independent new-rule example for one class and subject.
Other hard requirements are satisfied, and every example uses existing weekly occurrences. “Single” means an
occurrence separated from all other occurrences of that subject by the grid definition above. Unless stated
otherwise, every run shares one teacher and room. Extra singles, if any, are placed on other days.

| Rules | Observed arrangement | Valid under block rules | Reason |
| --- | --- | --- | --- |
| None | Five separated singles | Yes | No consecutive run |
| None | One run of two | No | Consecutive lessons lack explicit permission |
| Exactly one block of two | One run of two and three singles | Yes | One declared double; five lessons total |
| Exactly one block of two | One run of three and two singles | No | An undeclared triple cannot count as a double |
| Exactly two blocks of two | Two separated runs of two | Yes | Two maximal doubles |
| Exactly two blocks of two | One run of four | No | A four-lesson run is not two doubles |
| At least one block of two | Two separated runs of two | Yes | Additional doubles are expressly permitted |
| Exactly one block of two | Two separated runs of two | No | Exceeds exact weekly frequency |
| Exactly one block of three | One run of three and one single | Yes | Declared triple; four lessons total |
| Exactly one block of two and exactly one of three | Separate runs of two and three | Yes | Each declared length and frequency is satisfied |
| Exactly one block of two | Two neighboring teaching periods separated only by an ordinary break | Yes | Ordinary break preserves adjacency |
| Exactly one block of two | Two lessons on opposite sides of school-wide lunch | No | Two singles; required double is missing |
| Exactly one block of two | Lessons in periods 1 and 3 with period 2 empty or class lunch | No | The intervening teaching period cannot be skipped |
| Exactly one block of two | Adjacent same-subject lessons with different teachers | No | One block cannot span teacher allocations |
| Exactly one block of two | Adjacent same-subject lessons with different rooms | No | A block must use one room |
| Exactly one block of four | One run of four | Yes | Lengths above three are supported |
| Exactly one block of three | Only a run of two | No | Wrong length and missing required triple |

### Authoring snapshot fidelity

Every complete authoring snapshot preserves exactly these field groups — no more, no fewer. The fields within a
group remain optional or incomplete where the draft model allows it; this table does not invent defaults for
missing imported values. Derived totals and check labels do not replace their authoritative source fields.

| Group | Preserved fields and meaning |
| --- | --- |
| Identity and working context | Authoring format version, draft/local school identity, source identities/revision, display name, edit revision, active section, focused record and pending input |
| Week grid | Active weekdays; interval identities, labels, kind, start/end times; teaching-period identities, weekdays and order; reserved status; declared adjacency/lunch boundaries; imported unspecified times |
| Subjects | Identity/name, reserved permission, weekly reserved and daily maxima, day-edge-only, curator-lesson designation, and existing display metadata |
| Teachers | Identity/name, explicit qualifications, subject-specific eligible class references, all-period versus explicit-whitelist availability intent, hard/undesirable period references and retained exceptions |
| Classes | Identity/name/size, curator/home-room references, availability intent and period references, undesirable periods, lunch reasons, explicit/effective hard gap/start/day-load and soft start/balance settings |
| Rooms | Identity/name/capacity, capability identifiers, availability intent and period references |
| Teaching plans and occurrences | Row identity, class/subject references, required weekly count, teacher allocations, unassigned input, stable individual lesson identities/names/teachers, series membership or absence, required capabilities, room preferences, undesirable periods and period/room locks |
| Contextual hard rules | Block class/subject, exact length, operator and weekly count; row-wide fixed room; subject/teacher-subject room policy identities, scope and allowed-room/home-room requirements |
| Retained source settings | Supported definition/rule versions, original optional presence and explicit-empty values, soft weight overrides and effective inherited settings, ancestry/provenance and unmappable source data retained for recovery |
| Saved and generated evidence | Exact compiled definition and revision, authoring revision/snapshot, checking build, source/local identities, saved names/times, run/search provenance, complete assignment fields, validity and quality evidence |

Unmappable data is retained and reported, not silently dropped. Unsupported data cannot be exported or saved as a
faithful valid definition. A previewed, explicitly accepted conversion may materialize settings to preserve old
effective behavior and introduce the new default prohibition; every such difference is disclosed by UC-15.

## Out of scope

- Arbitrary school spreadsheets, automated spreadsheet mapping and a required prefilled import template.
- Merged classes, parallel elective streams, ordered sequences of different subjects, automated swaps or a new
  analytics dashboard.
- Per-occurrence room subsets authored through the ordinary weekly row; existing imported values remain protected.
- Rebuilding timetable direct manipulation, protected repair or normal-profile definition authoring.
- Accounts, server-persisted authoring, sharing, cross-device recovery, public deployment or data migration outside
  this browser's demo artifacts.
- Real-product version history and active-version selection. The future product must keep versions in the same
  school, save a version without generating, retain each timetable's exact definition version and explicitly
  activate a compatible definition/timetable pair while preserving the previous active pair during preparation.
  Mapping an old arrangement to changed lesson/room/period identities is a separate future capability.

## External dependencies

The demo's existing definition checking and bounded generation services must support the new versioned scheduling
contract before UC-17 can complete. UC-12 and UC-13 specify that required kernel behavior; no implementation may
substitute a UI-only promise or silently fall back to legacy rules. If unavailable, setup/draft retention remains
usable and the affected check/generation action reports unavailable support without claiming success.

No product decisions remain open in this specification.
