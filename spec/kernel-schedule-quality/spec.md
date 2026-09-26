# Feature: Class timetable quality

## Summary and resolved decisions

The administrator can request an initial timetable or a repair using catalog version 2. Both apply two default class-quality preferences. A class gap is one available, unassigned period between that class's first and last lesson in a continuous availability block on one day. Weekly imbalance is the sum, for every pair of weekdays available to a class, of the excess difference in lesson counts beyond one. A balanced distribution therefore has no weekly-balance matches when all daily counts differ by at most one. Days unavailable to a class are excluded. The default weight of each new preference is one; the existing override range includes zero to disable a preference.

Hard feasibility and the existing lexicographic period-move, room-only-move, ordinary-preference order remain in force. The preferences cannot justify an additional period move during repair. Catalog version 1 keeps its original four preference rows and remains valid as an accepted predecessor. A repair from a version 1 workspace baseline produces a version 2 successor definition and result.

## Actors and terms

- School administrator: requests initial planning or a repair and reviews a candidate before acceptance.
- Class: the cohort attached to a lesson.
- Available weekday: a weekday containing at least one period in the class's declared availability.
- Quality penalty: a reported, weighted preference match; it does not make a timetable infeasible.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Obtain a quality-aware timetable proposal | School administrator | none |

## UC-1 - Obtain a quality-aware timetable proposal (primary)

- Goal: Receive a feasible initial or repaired timetable that prefers compact class days and a balanced class week.
- Primary actor: School administrator
- Supporting actors: scheduling kernel
- Trigger: The administrator requests initial planning or generates a repair proposal.
- Preconditions: A valid definition exists; repair additionally has an accepted definition/result pair and a conflict-free repair intent.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests planning with a catalog version 2 school definition.
2. The system validates the definition and searches for a complete, hard-feasible timetable using the [quality catalog](#quality-catalog).
3. The system publishes a feasible candidate with both class-quality match counts, effective weights, and aggregate penalties in its score breakdown.
4. The administrator can inspect the candidate without the system changing an accepted timetable.

### Extensions

- 1a. If the administrator instead requests a repair, the system retains the accepted predecessor, applies the repair intent to a complete successor definition, and searches using the same class-quality preferences; continue at step 3.
- 1b. If the accepted predecessor uses catalog version 1, the system verifies it under its original contract and uses catalog version 2 for the repair successor; continue at step 3.
- 2a. If a definition uses catalog version 1 for initial planning or direct kernel replanning, the system applies the original four preference rows and emits a version 1 result; end.
- 2b. If an override sets either new preference weight to zero, the system reports its matches with zero aggregate penalty; continue at step 3.
- 2c. If validation fails or search finds no feasible timetable within its limit, the system reports that outcome without publishing a candidate or changing the accepted timetable; end.

### Guarantees

- G1. Class-gap and weekly-balance matches are non-negative, calculated from all lessons of a class and the class's declared availability, and use exactly the semantics in the [quality catalog](#quality-catalog).
- G2. Hard constraints, period stability, room stability, and the remaining preference rows retain their existing priority and meaning. A time-limited feasible result is not described as optimal.
- G3. The MV5 repair case prefers moving `5a.int-o.01` to `fri-1` in `b216` over moving it to `fri-6` when the other assignments are equal, leaving no Friday 5A gap. The candidate does not need an extra period or room move for this placement.
- G4. Initial planning of a feasible MV5-like week prefers a distribution with no weekly-balance matches over the `7,7,5,2,1` distribution when hard validity and higher-priority scores are equal.
- G5. Existing catalog version 1 definition/result pairs remain verifiable and usable as repair predecessors; their revisions and assignments do not change merely because catalog version 2 exists.
- G6. A published catalog version 2 result identifies the new catalog, lists exactly its six preference rows in catalog order, and preserves the existing result fields and revision semantics.

### Postconditions

- Success: A complete, feasible, revision-verifiable candidate exists with the applicable quality score; acceptance remains a separate action.
- Minimal guarantee: Invalid, interrupted, or unsuccessful planning preserves the accepted predecessor and publishes no replacement timetable.

## Quality catalog

Catalog version 1 contains exactly the existing ten hard rows, two stability rows, and four preference rows. Catalog version 2 retains those rows and adds exactly these two ordinary-preference rows, each with default weight `1`:

| ID | Match semantics |
|---|---|
| `soft.cohort-gap` | One match per available unassigned period between the first and last assigned period of one class in a continuous availability block on one weekday. An unavailable period breaks the block. |
| `soft.cohort-week-balance` | For each class and each pair of its available weekdays, `max(0, abs(lessonCountA - lessonCountB) - 1)` matches. Days with zero lessons are included when available. |

## Out of scope

Daily minimum or maximum lesson counts, hard bans on gaps, school calendar policy screens, and automatic acceptance are outside this feature.
