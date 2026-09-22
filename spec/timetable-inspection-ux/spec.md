# Timetable Inspection UX Specification

## Feature summary

This feature is the presentation successor to accepted timetable-workspace UC-3 for inspection of a verified accepted
timetable. It gives the school timetable administrator a complete Week overview, a precise Day matrix, independent
subject and teacher investigation, honest teacher availability, preserved narrowing and focused schedules, and exact
lesson inspection.

The feature changes presentation behavior only. The accepted definition, accepted result, workspace lifecycle, repair
intent, and proposal state remain authoritative and unchanged by every behavior in this specification. Integration of
these inspection surfaces with Draft and Proposal is owned by the separate persistent repair-canvas feature.

## Scope and resolved decisions

- On an operational desktop, Week is the default when no valid local preference exists for the current school. Without
  an explicit filter, Week represents every declared class and every definition-ordered weekday and period in the
  accepted timetable.
- Day remains the precise operational range: classes are rows and the selected weekday's periods are columns.
- Week is an overview of the same accepted assignments, not a second timetable and not a claim that every detail fits
  without scrolling.
- Compact lesson tiles are the default. A density or zoom accommodation remains available during the session and is
  not persisted until representative validation justifies removing it.
- Existing search, narrowing, reset, and focused class, teacher, and room schedules remain available. A narrowed view
  is always labelled as narrowed and is never called the complete school.
- Subject and teacher selections highlight without removing lessons. `Show only matches` is a separate, explicit
  filtering action using the same selected identity. Concurrent filters intersect.
- Highlighting uses positive cues. Nonmatching lessons are not dimmed by default.
- Teacher availability is derived only from authoritative accepted-definition availability and accepted assignments.
  Absence of an assignment is never sufficient to call a teacher available.
- Counts cover the complete represented population and time range after explicit filters, regardless of scrolling or
  virtualization. They count unique lesson identities, not rendered elements.
- Only the time range and last weekday are stored locally per school and device. Search, highlights, filters, density,
  focused schedule, and lesson selection reset on reload.
- Narrow screens provide read-only Day or focused agendas, withhold editing and acceptance actions, preserve the
  desktop range preference, and do not claim to provide the complete operational workbench.
- The administrator-facing term is `Class`; technical details may identify its kernel term `cohort`. Display names,
  weekday order, period order, optional times, and identities come from the accepted definition. Opaque IDs are never
  parsed into display meaning.
- Accepted definitions without authoritative group metadata render ordinary lesson assignments and expose no disabled,
  empty, or inferred group controls.
- No new role, authentication behavior, route access, scheduling semantic, or durable workspace state is introduced.

## Actors and domain terms

- **School timetable administrator:** inspects the accepted whole-school timetable and investigates lessons, subjects,
  teachers, classes, and rooms.
- **Local presentation preference store:** retains only the last valid desktop time range and weekday for one school on
  one device; it is not workspace authority.

Terms:

- **Current · accepted:** the complete accepted definition/result pair that remains authoritative during inspection.
- **Time range:** either the complete recurring `Week` or one selected `Day`.
- **Represented population:** every class and assignment included after applying explicit filters, including content
  outside the current viewport.
- **Viewport:** the currently rendered or visible portion of the represented population.
- **Compact lesson tile:** one selectable representation of one accepted lesson assignment.
- **Highlight:** a positive, non-destructive cue for an exact subject, teacher, or search match.
- **Filter:** an explicit restriction of the represented population, disclosed by an active-filter summary.
- **Teacher ribbon:** a period sequence for one selected teacher that distinguishes assigned, available-but-unassigned,
  and unavailable periods.
- **Focused schedule:** a secondary read-only recurring schedule for one class, teacher, or room.
- **Selection:** one stable lesson identity whose complete accepted details are open in the inspector.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Inspect the accepted school in Week or Day | School timetable administrator | Primary; none |
| UC-2 | Trace subject teaching and teacher load | School timetable administrator | Requires UC-1; extends UC-1 at 4a |
| UC-3 | Narrow the timetable or open a focused schedule | School timetable administrator | Requires UC-1; extends UC-1 at 4b |

## Presentation state model

Presentation state is local and never changes a workspace lifecycle state.

| Dimension | Values | Initial or reload behavior | Allowed transition |
|---|---|---|---|
| Desktop time range | `Week`, `Day` | Restore the valid per-school local value; otherwise `Week` | Administrator switches range |
| Last weekday | Any definition-declared weekday | Restore the valid per-school local value; otherwise the first definition-ordered weekday | Administrator chooses a Day, or Week-to-Day follows the selected lesson |
| Density accommodation | Compact default plus available density or zoom accommodation | Compact | Administrator changes presentation density or zoom |
| Subject investigation | No selection, exact subject identity; highlight or filter mode | No selection | Administrator selects, filters, clears, or resets |
| Teacher investigation | No selection, exact teacher identity; highlight or filter mode | No selection | Administrator selects, filters, clears, or resets |
| Search and other filters | Empty or explicit criteria | Empty | Administrator applies, clears, or resets criteria |
| Lesson selection | No selection or one stable lesson identity | No selection | Administrator selects, closes, changes to an excluding Day, or opens a focused schedule |
| Surface | Whole-school timetable or one focused class/teacher/room schedule | Whole-school timetable on desktop; read-only Day/focused agenda on narrow screens | Administrator opens or returns from a focused schedule |

Rules for all presentation-state transitions:

1. Day-to-Week retains the selected lesson and active investigation state.
2. Week-to-Day with a selected lesson opens that lesson's weekday and retains the selection.
3. Week-to-Day without a selected lesson opens the last valid weekday.
4. Manually selecting a Day that excludes the selected lesson clears selection and announces why; highlights and
   filters remain active.
5. Subject and teacher filters intersect each other and all other active filters. Their highlight cues remain distinct.
6. Clearing one subject or teacher selection leaves the other selection unchanged.
7. Resetting filters removes narrowing criteria and their active summary but retains the chosen time range. Subject and
   teacher selections return to highlight mode rather than being silently discarded.
8. Reload retains only the valid time range and weekday. All other presentation dimensions return to their initial
   values.
9. A missing, invalid, or unavailable local preference store falls back to Week and the first definition-ordered
   weekday without blocking inspection or changing workspace data.
10. All other transitions are refused or ignored without changing accepted, draft, proposal, or policy state.

## UC-1 - Inspect the accepted school in Week or Day (primary)

- Goal: Understand the complete school's current accepted timetable at the time range needed for an operational task.
- Primary actor: School timetable administrator
- Supporting actors: Local presentation preference store
- Trigger: The administrator opens a workspace containing a verified accepted timetable or returns from a focused
  schedule.
- Preconditions: A verified accepted definition/result pair and authoritative school display metadata exist.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator opens the accepted timetable on an operational desktop.
2. The workspace identifies the school and `Current · accepted` baseline, restores a valid local range preference or
   opens Week, and renders the complete represented population.
3. In Week, the workspace shows classes as rows and weekdays in definition order, with each class-day containing the
   definition-ordered period sequence; in Day, it shows classes as rows and the selected weekday's periods as columns.
4. The administrator switches Week or Day, navigates the selected weekday, adjusts the available density or zoom, or
   chooses one of the independent investigations at extension point 4a or 4b.
5. The workspace applies the presentation-state transition rules, derives the exact assignments for the selected range
   from the same accepted baseline, and labels the represented range and whether it is complete or narrowed.
6. The administrator selects an occupied lesson tile.
7. The workspace opens the exact accepted subject, class, teacher, weekday, period, room, and lesson identity without
   leaving the timetable context.

### Extensions

- 2a. If the accepted timetable has no assignments, the workspace still renders every declared class, weekday, period,
  and empty slot, identifies the baseline as Current · accepted, and invents no lesson; continue at step 4.
- 2b. If a stored range or weekday is absent or invalid for this school, the workspace opens Week and uses the first
  definition-ordered weekday as the Day fallback; continue at step 3.
- 2c. If the viewport is narrow, the workspace opens a read-only Day or focused agenda, states that the desktop
  operational workbench is unavailable, withholds editing and acceptance actions, and preserves the desktop preference;
  end.
- 4a. If the administrator wants to trace a subject or teacher, continue with UC-2; end.
- 4b. If the administrator wants to search, narrow, or open a focused schedule, continue with UC-3; end.
- 5a. If a manually selected Day does not contain the selected lesson, the workspace clears that selection, announces
  that it is outside the represented Day, and preserves the other presentation state; resume at step 5.
- 5b. If the local preference cannot be stored, the workspace completes the range change, makes no promise to restore
  it later, and changes no workspace state; resume at step 5.
- 7a. If the selected lesson is outside the currently rendered viewport, the workspace brings its represented position
  into view or opens its details without changing the represented population; end.

### Guarantees

- G1. Inspection actions never mutate or replace the accepted definition, accepted result, repair draft, proposal,
  policy intent, or workspace lifecycle state.
- G2. `Current · accepted`, school identity, represented time range, and complete-versus-narrowed population remain
  visible or available to assistive technology throughout the journey.
- G3. An unfiltered Week represents every declared class and every definition-ordered weekday and period. Scrolling or
  virtualization may reduce the rendered viewport but never changes the represented population or its totals.
- G4. Empty period positions remain structurally identifiable and are never described as teacher availability.
- G5. Each occupied tile leads with the authoritative subject display name and keeps the room visibly present in its
  lower-right area. Day tiles also keep the teacher visible. Week tiles may move the teacher to accessible text and the
  inspector. Every complete accessible name and inspector exposes subject, teacher, room, class, period, and stable
  lesson identity.
- G6. A full display name may be clamped visually but remains complete in accessible text and the inspector. No
  abbreviation or display meaning is derived from an opaque ID.
- G7. Whole-school controls, range and weekday navigation, density or zoom accommodation, lesson selection, closing the
  inspector, and returning from focused schedules are keyboard-operable and have the same outcome as pointer use.
- G8. Color is not the only indication of range, selection, focus, or accepted state.
- G9. School/state controls, weekday and period headers, and the class-name context remain available while the
  administrator scrolls. Logical keyboard movement follows class, weekday, and period order even when content is
  virtualized.
- G10. The compact tile is the default on every reload. The available density or zoom accommodation does not change
  represented data, counts, selection identity, or durable workspace state.
- G11. An accepted definition without authoritative group metadata produces ordinary lesson tiles and no disabled,
  empty, or inferred group UI.
- G12. Real-browser verification exercises desktop Week and Day plus the narrow read-only extension against the
  validation-scale accepted snapshot and compares the exact durable workspace state before and after inspection.

### Postconditions

- Success: The administrator can identify an accepted lesson and its complete whole-school Week or precise Day context,
  with an explicit Current · accepted baseline.
- Minimal guarantee: When inspection cannot present a requested local preference or viewport position, no lesson is
  invented, omitted from the represented population, or persisted differently, and every authoritative workspace state
  remains unchanged.

## UC-2 - Trace subject teaching and teacher load

- Goal: Find every lesson for one subject and explain one teacher's assigned and available periods without losing
  surrounding school context.
- Primary actor: School timetable administrator
- Supporting actors: none
- Trigger: From the whole-school timetable, the administrator selects a subject, a teacher, or both.
- Preconditions: UC-1 can render the accepted timetable and the selected identities belong to its accepted definition.
- Relations:
  - Requires: UC-1, because exact highlights and teacher availability require its represented accepted timetable
  - Includes: none
  - Extends: UC-1 at extension point 4a

### Main success scenario

1. The administrator selects one exact subject identity.
2. The workspace gives every matching lesson in the represented range a subject cue, reports the unique matching-lesson
   count, and leaves nonmatching lessons present and readable.
3. The administrator also selects one exact teacher identity.
4. The workspace gives every assignment for that teacher a distinct teacher cue, preserves both cues on dual matches,
   reports teacher and dual-match counts, and shows the teacher ribbon for the represented range.
5. The administrator reads the ribbon's assigned, available-but-unassigned, and unavailable periods and switches Week
   or Day while retaining both selected identities.
6. The administrator optionally activates `Show only matches` for either or both identities.
7. The workspace intersects active filters, labels the timetable as narrowed, retains separate subject, teacher, and
   dual-match totals for the represented range, and leaves Current · accepted visible.
8. The administrator clears either identity or its filter mode.
9. The workspace clears only that identity or mode and preserves the remaining investigation.

### Extensions

- 2a. If the represented range contains no lesson for the subject, the workspace reports zero exact matches, preserves
  the complete surrounding timetable, and offers clearing or range change; resume at step 3.
- 4a. If the represented range contains no assignment for the teacher, the workspace reports zero teacher matches and
  still derives the ribbon from authoritative availability; continue at step 5.
- 6a. If intersecting filters produce no represented lessons, the workspace shows the active identities and filter
  modes, reports zero represented lessons, offers one-action filter reset, and invents no availability or assignment;
  resume at step 8.
- 7a. If a filter excludes the selected lesson, the workspace clears lesson selection with an announcement while
  preserving the selected subject and teacher identities; resume at step 7.

### Guarantees

- G1. Subject and teacher matching uses stable identities from the accepted definition, never partial display-name or
  opaque-ID inference.
- G2. Highlighting is non-destructive and uses positive text, icon, outline, or pattern cues; nonmatches are not dimmed
  by default, and color is not the only distinction.
- G3. Subject, teacher, and dual-match counts cover unique lesson identities in the complete represented range after
  explicit filters, not merely the rendered viewport.
- G4. The teacher ribbon classifies a period as assigned from accepted assignments, unavailable from authoritative
  definition availability, and available-but-unassigned only when it is authoritative availability with no accepted
  assignment. It never infers availability from an empty class cell.
- G5. The selected teacher and subject identities persist across Day/Week changes but reset on application reload. They
  are never written to the durable workspace aggregate.
- G6. Subject cue, teacher cue, dual match, selection, and accepted state remain mutually distinguishable and are all
  enumerated by the tile's accessible name and inspector where applicable.
- G7. Selector operation, `Show only matches`, clearing either identity, range switching, and filter reset are
  keyboard-operable and produce the same represented population as pointer operation.
- G8. Real-browser verification covers subject-only, teacher-only, combined, zero-match, clear-one, clear-all,
  single-filter, and intersected-filter paths against the validation-scale snapshot without durable workspace mutation.

### Postconditions

- Success: The administrator can account for every represented lesson of the selected subject, every accepted assignment
  of the selected teacher, and the teacher's authoritative assigned, available-but-unassigned, and unavailable periods.
- Minimal guarantee: A missing match or empty intersection changes no accepted or draft data, removes no lesson from the
  underlying accepted timetable, and remains explainable through visible active criteria and exact zero counts.

## UC-3 - Narrow the timetable or open a focused schedule

- Goal: Investigate a smaller population or one class, teacher, or room while retaining an honest path back to the
  whole-school accepted context.
- Primary actor: School timetable administrator
- Supporting actors: none
- Trigger: From the whole-school timetable, the administrator searches, applies an applicable filter, or opens a focused
  schedule.
- Preconditions: UC-1 can render the accepted whole-school timetable.
- Relations:
  - Requires: UC-1, because narrowing and focused schedules derive from its accepted population and context
  - Includes: none
  - Extends: UC-1 at extension point 4b

### Main success scenario

1. The administrator searches accepted lesson metadata or applies an explicit class, room, or period criterion.
2. Search gives matching lessons a positive cue without removing surrounding lessons; explicit filters restrict the
   represented population by intersection.
3. The workspace labels the result as complete or narrowed, reports represented classes and unique lessons against the
   complete accepted population, lists every active criterion, and offers one-action filter reset.
4. The administrator selects a result or opens the focused schedule for one class, teacher, or room.
5. The workspace shows the selected accepted lesson details or the chosen entity's complete recurring accepted
   schedule, identifies empty weekdays or periods honestly, and retains a return path to the prior whole-school range.
6. The administrator returns to the whole-school timetable or resets the filters.
7. The workspace restores the retained Week/Day context; reset removes narrowing criteria while preserving range and
   any independently selected subject or teacher in highlight mode.

### Extensions

- 2a. If search has no match, the workspace reports zero search matches, keeps the complete timetable represented, and
  offers clearing the query; resume at step 1.
- 3a. If active filters yield no represented lesson, the workspace retains the declared active criteria, labels the
  result as narrowed and empty, offers one-action reset, and never presents it as the complete school; resume at step 6.
- 4a. If the chosen class, teacher, or room has no accepted lesson, the focused schedule identifies the entity and its
  empty recurring schedule without inventing an assignment; resume at step 6.
- 4b. On a narrow screen, the workspace opens the requested read-only class, teacher, or room agenda, withholds editing
  and acceptance actions, and does not claim to show the desktop whole-school workbench; end.
- 5a. If the selected result is outside the rendered viewport, the workspace brings its represented position into view
  or opens the same accepted details without changing filters or totals; resume at step 5.

### Guarantees

- G1. Search compares authoritative display metadata and available stable technical identities but never derives display
  meaning from identifier spelling.
- G2. Search is a non-destructive positive highlight. Only explicitly activated filters narrow represented classes or
  lessons.
- G3. Concurrent filters intersect. Every narrowed result shows its complete active-criteria summary and one-action
  reset, and no narrowed view is described as the complete school.
- G4. Focused class, teacher, and room schedules are secondary inspection views and never replace or satisfy the
  whole-school Week/Day representation.
- G5. Returning from a focused schedule restores the retained time range, last weekday, highlights, filters, and scroll
  context where still representable. Application reload follows the presentation state model instead.
- G6. Search, filters, active criteria, counts, reset, focused-view entry and return, empty results, and selected details
  are keyboard-operable and perceivable without color alone.
- G7. Narrowing, search, focused schedules, and reset never mutate the accepted definition/result pair or any draft,
  proposal, or policy state.
- G8. Real-browser verification covers search highlight, each applicable filter, intersected filters, empty search,
  empty filter result, reset, all three focused schedule types, return-context retention, and the narrow read-only path
  against the validation-scale snapshot.

### Postconditions

- Success: The administrator can investigate an explicitly narrowed population or focused recurring schedule and return
  to the retained whole-school accepted context without confusing the subset for the complete school.
- Minimal guarantee: Empty, invalid, or reset criteria disclose no invented assignment, persist no presentation state
  beyond the allowed range preference, and leave all authoritative workspace data unchanged.

## Normative validation data

The validation-scale accepted snapshot contains:

- at least 60 definition-declared classes;
- at least 100 definition-declared teachers;
- at least 100 definition-declared rooms;
- between 900 and 1,100 accepted lesson assignments across the complete recurring week;
- long subject, teacher, class, and room display names that exercise clamping without identifier-derived abbreviations;
- teachers with assigned, available-but-unassigned, and unavailable periods;
- classes, teachers, and rooms with both populated and empty recurring periods.

Every validation participant performs exactly these inspection tasks—no more, no fewer for the feature gate:

| Task | Observable completion |
|---|---|
| Find one subject across the Week | Accounts for every matching unique lesson without removing or inventing school context |
| Explain one teacher's recurring load | Distinguishes assigned, available-but-unassigned, and unavailable periods correctly |
| Move between Week and Day | Preserves or clears selection according to the presentation state model and identifies the represented range |
| Identify accepted lesson details | Names subject, class, teacher, weekday, period, room, and the Current · accepted baseline |
| Narrow and recover context | Applies intersecting criteria, recognizes the narrowed population, and returns to the retained whole-school context |
| Use focused schedules | Opens class, teacher, and room schedules and returns without treating any one as the whole school |

The feature gate requires:

1. five participants with real timetable responsibility drawn from at least three schools;
2. at least four of five participants completing every table row without a serious error or facilitator correction;
3. all five participants identifying Current · accepted correctly throughout every task;
4. lower median completion time than the accepted timetable-workspace UI for each materially equivalent task;
5. success and accuracy evidence, without fabricated baseline timing, for behavior the accepted UI cannot perform;
6. real-browser keyboard and pointer evidence at operational desktop widths and the narrow read-only boundary;
7. exact comparison showing no durable workspace mutation across inspection journeys; and
8. recorded raw samples and 95th-percentile observations for initial Week rendering, Day/Week change, highlighting,
   filtering, lesson selection, and focused-schedule opening. These interaction observations are diagnostic unless a
   separately accepted rule establishes a release threshold.

## Out of scope

- Draft and Proposal modes, persistent repair overlays, direct/ripple comparison, proposal acceptance, and draft
  mutation;
- cohort partitions, group target sets, cross-partition disjointness, multi-group tiles, and split-slot overflow;
- primary-room policies, policy previews, policy provenance editing, and soft room preferences;
- adding `shortDisplayName` or any other kernel field;
- changing kernel validation, hard constraints, solver behavior, result identity, or public contract version;
- mobile or narrow-screen editing, repair generation, or proposal acceptance;
- direct manipulation of accepted assignments;
- new authentication, roles, hosted multi-school behavior, publication, notification, substitution, or attendance
  workflows.

## External dependencies

- Approval depends on access to five timetable professionals from at least three schools for the normative feature gate.
  Until that evidence exists, the implementation may be technically converged but cannot be promoted as administrator-
  approved.
- Validation requires a complete synthetic or properly anonymized accepted snapshot satisfying the normative scale and
  content above. Production school data is not required and must not be fabricated or used without appropriate
  provenance.
