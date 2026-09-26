# Feature: Cohort day-start quality

## Summary and resolved decisions

A school administrator can obtain an initial or repaired timetable that prefers each cohort's first lesson of a taught day to begin within the first three declared school-day slots. The quality result reports the number of cohort-days that start later. This is a preference, so hard feasibility and repair stability retain their existing priority; a time-limited feasible result does not promise that every preference is satisfied. A day with no lesson has no start to score. The threshold is the third slot by weekday order in the school definition, even when order values are nonconsecutive. A first lesson after that slot counts once for its cohort-day, regardless of how late it starts.

Catalog version 3 adds one preference to the version 2 catalog. Catalog versions 1 and 2 remain readable with their original rows and meanings. The new preference defaults to weight `1` and uses the existing override range of `0` through `1,000,000`. Existing internal cohort gaps, late starts, and nonpreferred rooms remain separately visible in the result; a school's weights express its current tradeoffs. For the MVK school, one internal cohort gap has more weight than one late start, and one late start has more weight than one nonpreferred room. The MVK definition carries the overrides and is checked in a normal 30-second run.

## Actors and terms

- School administrator: requests planning or repair and reviews the result.
- Taught cohort-day: one cohort and weekday with at least one assigned lesson.
- School-day slot: one declared period's position after sorting that weekday's periods by their unique order values.
- Late start: a taught cohort-day whose earliest assigned lesson is in slot four or later.
- Quality match: one reported late-start cohort-day; its weighted penalty does not make the timetable infeasible.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Obtain a start-aware timetable proposal | School administrator | none |

## UC-1 - Obtain a start-aware timetable proposal (primary)

- Goal: Receive a feasible timetable that favors starts within the first three slots and reports remaining late starts.
- Primary actor: School administrator
- Supporting actors: scheduling kernel
- Trigger: The administrator requests initial planning or generates a repair proposal.
- Preconditions: A valid definition exists; repair additionally has a verified current definition/result pair and a valid successor definition.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator requests initial planning with a catalog version 3 school definition.
2. The system validates the definition and searches for a complete, hard-feasible timetable using the [start-quality catalog](#start-quality-catalog).
3. The system publishes a feasible candidate that reports the late-start match count, effective weight, and aggregate penalty alongside the existing preference rows.
4. The administrator can inspect the candidate and identify cohort-days whose first lesson starts after the third declared slot.

### Extensions

- 1a. If the administrator requests repair, the system retains the verified current timetable, applies the successor definition, and searches with the same start-quality preference; continue at step 3.
- 1b. If the workspace's accepted predecessor uses catalog version 1 or 2, the system retains that predecessor and creates a version 3 repair successor; continue at step 3.
- 2a. If a definition uses catalog version 1 or 2 for direct planning or replanning, the system applies that version's original rows and does not report a late-start row; end.
- 2b. If the late-start weight is zero, the system still reports its match count with zero aggregate penalty; continue at step 3.
- 2c. If a cohort has no lessons on a weekday, that day produces no late-start match; continue at step 3.
- 2d. If the definition is invalid or search finds no feasible timetable within its limit, the system reports the applicable failure without publishing a candidate or changing a verified current timetable; end.

### Guarantees

- G1. Exactly one late-start match is counted for each taught cohort-day whose first assigned lesson is after the third declared weekday slot. Days with no lesson do not match. Availability, locks, and other constraints do not hide a late start in the reported count.
- G2. The new match count uses the definition's weekday and order values, not period IDs, display labels, clock times, or an assumption that order values are consecutive. A weekday with fewer than four declared slots cannot yield a late-start match.
- G3. A catalog version 3 result identifies that version, lists exactly seven preference rows in catalog order, and reports the same late-start count used to score its candidate. The existing result fields and revision semantics remain intact.
- G4. Existing catalog version 1 and 2 definition/result pairs remain verifiable and usable as repair predecessors without changing their revisions, assignments, weights, or preference rows.
- G5. Hard feasibility, period stability, room stability, and the existing ordinary preferences retain their current meanings and priority. A late-start preference does not authorize an extra period or room move during repair solely to improve start time. A time-limited feasible result is not claimed to be optimal or universally on time.

### Postconditions

- Success: A complete, feasible, revision-verifiable candidate exists with the applicable start-quality score; acceptance remains a separate action.
- Minimal guarantee: Invalid, interrupted, or unsuccessful planning preserves any verified current timetable and publishes no replacement timetable.

## Start-quality catalog

Catalog version 1 has exactly its original four ordinary preferences. Catalog version 2 has exactly those four plus `soft.cohort-gap` and `soft.cohort-week-balance`. Catalog version 3 has exactly the version 2 rows plus this one row, with default weight `1`:

| ID | Match semantics |
|---|---|
| `soft.cohort-late-start` | One match for each cohort and weekday with at least one lesson when its first lesson is in the fourth or a later declared school-day slot. |

## Out of scope

Hard start-time bans, a required lesson on every available weekday, individualized cohort start thresholds, new screens, and automatic acceptance are outside this feature.
