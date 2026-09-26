# Feature: Cohort daily lesson balance

## Summary and resolved decisions

A school administrator can set `maxDailyLessonSpread` on each cohort in a catalog version 4 school definition. The
value is the desired maximum difference between the busiest and quietest available weekdays, counting days with no
lessons. It is a soft target, so exceeding it adds weekly-balance matches but does not make a timetable infeasible.
For each pair of available weekdays, the match count is the difference in lesson counts beyond that cohort's target.
Zero matches are equivalent to the busiest-to-quietest spread being within the target. Omission means one lesson,
which preserves the existing weekly-balance meaning. The value is a non-negative integer.

Catalog versions 1 through 3 retain their exact input and score meanings. Version 4 retains the seven ordinary
preference rows from version 3 and adds the cohort setting without adding a score row. MVK declares a target of two
for every cohort and sets the weekly-balance weight to four. One cohort gap still weighs ten and one late start weighs
five, so both remain costlier than one balance match. A feasible time-limited result is not
claimed to satisfy every target or to be optimal.

## Actors and terms

- School administrator: supplies a definition and requests initial planning or repair.
- Available weekday: a weekday with at least one period available to the cohort after school-wide reservations.
- Daily lesson count: the cohort's assigned lessons on one available weekday, including zero.
- Daily spread: maximum daily count minus minimum daily count across available weekdays.
- Weekly-balance match: one unit of pairwise lesson-count difference beyond the cohort's configured target.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Obtain a cohort-balanced timetable proposal | School administrator | none |

## UC-1 - Obtain a cohort-balanced timetable proposal (primary)

- Goal: Receive a feasible initial or repaired timetable that favors each cohort's declared daily spread target.
- Primary actor: School administrator
- Supporting actors: scheduling kernel
- Trigger: The administrator requests initial planning or a repair proposal.
- Preconditions: A valid school definition exists; repair also has a verified accepted definition/result pair and a complete successor definition.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator supplies a catalog version 4 definition with a daily spread target on at least one cohort and requests planning.
2. The system validates the definition and searches for a complete hard-feasible timetable, preferring lower weighted weekly-balance excess along with the other catalog preferences.
3. The system publishes a feasible candidate with its effective weights, weekly-balance match count and aggregate penalty.
4. The administrator inspects each cohort's daily counts and can distinguish a met target from a remaining excess.

### Extensions

- 1a. If the administrator requests repair from a verified predecessor, the system retains that predecessor and applies the complete successor definition, including each cohort target; continue at step 2.
- 1b. If a catalog version 4 cohort omits the target, the system uses one lesson for that cohort; continue at step 2.
- 1c. If the predecessor uses catalog version 1, 2, or 3, it remains verifiable under its own catalog; a workspace repair successor uses catalog version 4 without changing the predecessor; continue at step 2.
- 1d. If the administrator plans or verifies a catalog version 1, 2, or 3 definition directly, the system applies that catalog's original input and score meanings; end.
- 2a. If a cohort target is negative, fractional, or supplied in an older catalog, the system reports invalid input without starting search or publishing a timetable; end.
- 2b. If the balance weight is zero, the system still reports the match count and reports zero aggregate balance penalty; continue at step 3.
- 2c. If search ends without a feasible candidate, the system reports the unsuccessful outcome without a timetable and preserves any accepted predecessor; end.
- 3a. If a bounded feasible candidate exceeds a cohort target, the system reports its weighted excess and labels the candidate feasible without claiming that the target or global optimum was reached; continue at step 4.

### Guarantees

- G1. For each cohort, weekly-balance matches equal the sum over unordered pairs of available weekdays of `max(0, abs(countA - countB) - maxDailyLessonSpread)`. School-wide reserved periods do not make a day available. Zero-lesson available days count.
- G2. The solver and published score calculate the same weekly-balance match count for a given complete assignment. A value of zero is allowed and means equal daily counts are preferred.
- G3. Catalog version 4 results identify that version and retain exactly seven ordinary-preference rows in catalog order. Versions 1 through 3 and their existing definition/result pairs retain their earlier rows, meanings, and revisions.
- G4. Hard feasibility, period stability, and room stability retain priority over ordinary preferences. A repair does not add a period or room move solely to satisfy a daily spread target.
- G5. MVK's definition declares target two for every cohort and weight four for weekly balance. A normal 30-second run is assessed against the previous `2,5,5,3,4` 6B distribution, but the target is a preference rather than a universal guarantee.

### Postconditions

- Success: A complete, feasible, revision-verifiable candidate exists and its score reports the applicable weekly-balance penalty; acceptance remains a separate action.
- Minimal guarantee: Invalid, interrupted, or unsuccessful planning publishes no candidate and preserves any accepted predecessor.

## Out of scope

Hard daily load limits, per-day minimums or maximums, new workspace policy-editing screens, and automatic acceptance.
