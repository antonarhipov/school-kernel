# Use-Case Checkpoint: UC-3 - Narrow the timetable or open a focused schedule

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `a76b802`
- Submission commit: HEAD at convergence
- Relations verified: UC-1 `Requires` and extension at UC-1 4b through the shared accepted-timetable model and renderer.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-3 main steps 1-3, extensions 2a and 3a | Scale Chrome journey proves search-only highlighting retains every tile and complete label; explicit class, teacher, room, and period intersections narrow; zero search and zero narrowed results are distinct. | PASS |
| UC-3 main steps 4-7, extensions 4a and 5a | Chrome journeys select lesson details, cycle Class/Teacher/Room schedules, show an empty Room 99 schedule, and restore the Day/filter context. | PASS |
| UC-3 extension 4b, guarantees G1-G8, postconditions | Packaged browser checks use the 60-class/1,000-assignment snapshot, native controls, narrow read-only shared renderer, and exact before/after durable workspace comparisons. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1, RULE-2, RULE-4, RULE-6, RULE-7 | `inspection-state.js` owns filters/focus; `app.js` uses the immutable accepted model and stable lesson IDs. | PASS |
| RULE-9, RULE-10 | `messages.js`, text-safe markup, visible native controls, and non-color search cue. | PASS |
| RULE-11, RULE-12 | No backend, migration, route, lifecycle, or security change; Chrome tests compare durable state. | PASS |
| RULE-15, RULE-16 | Isolated real Chrome/Testcontainers scale journeys cover search, filters, focus, empty cases, return, and no mutation. | PASS |

## Validation

- Unit suite: `mvn -q -pl timetable-workspace -am test` — passed.
- Browser journeys: targeted Chrome/Testcontainers tests passed independently for narrowing, focused schedules, and empty focused schedule.
- Hygiene: `git diff --check` — passed.
- Walkthrough: user confirmed “It looks good” on 2026-09-23.

READY FOR CONVERGENCE: UC-3
