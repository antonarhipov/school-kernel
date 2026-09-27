# Feature: Gap-free cohort days

## Summary and resolved decisions

A catalog version 5 school definition forbids cohort gaps. Each cohort may declare `maxDailyGaps`, a non-negative
integer allowance of gaps per weekday; omission means zero. A timetable is feasible only if no cohort-day exceeds its
allowance. The existing `soft.cohort-gap` preference remains, so gaps within an allowance are still discouraged.

The gap definition is unchanged from catalog 2: an available period with no cohort lesson between that cohort's
first and last assigned lessons in one continuous availability block on a weekday. Empty periods before or after the
block, and periods the cohort or school has made unavailable or reserved, are not gaps.

Catalogs 1 through 4 retain their exact input and score meanings. They reject `maxDailyGaps` and impose no hard gap
limit.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Obtain a timetable without unplanned cohort gaps | School administrator | none |

## UC-1 - Obtain a timetable without unplanned cohort gaps (primary)

### Main success scenario

1. The administrator supplies a catalog version 5 definition and requests initial planning or repair.
2. The system validates the definition and searches for a complete timetable that satisfies every hard constraint,
   including each cohort's daily gap allowance.
3. The system publishes a feasible candidate in which no cohort-day has more gaps than its allowance.

### Extensions

- 1a. If a cohort declares `maxDailyGaps: n`, up to `n` gaps per weekday are feasible for that cohort; continue at 2.
- 1b. If the definition uses catalog 1 through 4, the system applies that catalog's meaning without a hard gap limit.
- 1c. If a catalog 1 through 4 definition declares `maxDailyGaps`, or any value is negative or fractional, the
  system reports invalid input without searching; end.
- 2a. If search ends without a candidate within the allowances, the system reports `NO_FEASIBLE_SOLUTION_FOUND`
  with `hard.cohort-gap` diagnostics naming the cohort and gap periods, publishes no timetable, and preserves any
  accepted predecessor; end.

### Guarantees

- G1. The solver, published evaluation, and baseline verifier count the same excess gaps: per cohort-day,
  `max(0, gaps - maxDailyGaps)`.
- G2. Cohort gaps occupy a second hard score level below physical conflicts. Both levels must be zero for feasibility.
- G3. A catalog 4 accepted predecessor is repaired as a catalog 5 successor without changing the predecessor.

## Normative MVK configuration

`examples/mvk.json` uses catalog 5, and no cohort declares `maxDailyGaps`. Its other fields match the catalog 4
configuration in `kernel-cohort-daily-balance`.

## Out of scope

Per-period gap permissions, per-weekday allowances, and workspace screens for editing the allowance.
