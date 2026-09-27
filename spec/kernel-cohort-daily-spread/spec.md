# Feature: Hard cohort daily lesson spread

## Summary and resolved decisions

A catalog version 7 school definition may declare `dailyLessonSpreadLimit` on a cohort: a non-negative integer. A
timetable is feasible only if, for every pair of the cohort's available weekdays, the lesson counts differ by at most
the limit. Omitting it imposes no hard limit.

The count is the catalog 4 weekly-balance count with the limit in place of `maxDailyLessonSpread`. Over every pair of
weekdays with at least one available regular period for the cohort, `hard.cohort-daily-spread` adds
`max(0, |count(a) - count(b)| - dailyLessonSpreadLimit)`. A weekday without lessons counts as zero. A lesson in a
permitted reserved period counts toward its weekday.

`maxDailyLessonSpread` keeps its catalog 4 meaning as the preferred target of `soft.cohort-week-balance`. A preferred
spread wider than the hard limit is valid; it has no additional effect.

`hard.cohort-daily-spread` joins the day-shape rules on hard level 1. Catalogs 1 through 6 reject the field and keep
their exact meanings.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Obtain a timetable whose cohort weeks stay within their daily spread limits | School administrator | none |

## UC-1 - Obtain a timetable whose cohort weeks stay within their daily spread limits (primary)

- Goal: Receive a timetable in which no cohort's busiest and quietest available weekdays differ by more than its
  limit.
- Primary actor: School administrator
- Supporting actors: scheduling kernel, timetable workspace
- Trigger: The administrator requests initial planning or repair with a catalog 7 definition.
- Preconditions: A definition exists; repair additionally has a verifiable accepted predecessor.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator supplies a catalog 7 definition in which some cohorts declare `dailyLessonSpreadLimit` and
   requests planning.
2. The system validates the definition and searches for a complete timetable in which every cohort's weekday lesson
   counts stay within its limit.
3. The system publishes a feasible candidate whose `soft.cohort-week-balance` row counts the remaining excess over
   each cohort's preferred spread.

### Extensions

- 1a. If the administrator requests repair, the system applies the same limits to the successor; continue at 3.
- 1b. If a catalog 1 through 6 definition declares the field, or the value is negative or fractional, the system
  reports invalid input without searching; end.
- 2a. If no timetable within the limits is found, the system reports `NO_FEASIBLE_SOLUTION_FOUND` with
  `hard.cohort-daily-spread` diagnostics naming the cohort, its busiest weekday, and its quietest weekday, publishes
  no timetable, and preserves any accepted predecessor; end.

### Guarantees

- G1. The solver, published evaluation, and baseline verifier count identical `hard.cohort-daily-spread` matches.
- G2. For a cohort without the field, the kernel imposes no hard spread limit, and the weekly-balance preference
  counts exactly what catalog 6 counted.
- G3. A catalog 6 accepted predecessor is repaired as a catalog 7 successor without changing the predecessor.

### Postconditions

- Success: A feasible candidate exists whose every cohort week stays within its limit.
- Minimal guarantee: An unsuccessful search publishes no timetable and leaves any accepted predecessor unchanged.

## Workspace run limit

The workspace's normal initial and repair run passes a one-minute kernel limit (`PT1M`) instead of 30 seconds. This
supersedes the 30-second value in `timetable-workspace` RULE-12 and D026. The unchanged two-minute retry preset and
the rule that a watchdog ends a process no later than ten seconds after its kernel limit are kept. The kernel's own
30-second default (kernel-v1 D022, G7) is unchanged.

## Normative MVK configuration

`examples/mvk.json` uses catalog 7. Cohorts `1a` through `4b` declare `dailyLessonSpreadLimit: 1`, and cohorts `5a`
through `9c` declare `dailyLessonSpreadLimit: 2`. Every other field matches catalog 6.

## Out of scope

Per-weekday minimum or maximum lesson counts, an earliest end slot, school-wide spread limits, and workspace screens
for editing the limit.
