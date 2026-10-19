# UC-5 — Hard audience scheduling and counted coordination

Base: `3cb66f30ef996a8cf3bb93f968cc53db15fc1191`. Automated approval: 2026-10-18. No commit/deployment requested.

Implemented consecutive exact-length/weekly-count/At least or Exactly controls with inherited and explicit audience scope; room capabilities, inherited/narrowed allowed rooms and fixed rooms; optional counted start-together, same-day, different-days, allowed day/period, fixed slot, strict selected-plan coverage and all-block gap-free controls. Activity/portion/block/rule IDs remain stable; occurrence matching is left to the solver. Inputs retain unfinished counts and explicit empty sets. Missing references, excessive counts, block demand, fixed duration/lunch boundary, room intersections and direct singleton-fixed contradictions produce issues without rewriting values. Referenced block removal/replacement is refused; hard-control deletion has preview/cancel.

Authoring schema, readability, exact document exchange and schema2/catalog11 compilation carry these values. Static CLI validation recognizes the controls; runtime assignment evaluation/search remains UC-8/9. This checkpoint does not approve those destinations.

Checks: `node --test tools/demo/*.test.mjs` 69/69 PASS. `./mvnw -pl kernel-cli -am -Dtest=NoUnitTests -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=AudienceTeacherCliIT -Dfailsafe.failIfNoSpecifiedTests=false verify` 3/3 PASS, including supported controls and refusal of excessive distinct counts, empty rooms and insufficient fixed duration, preserving input bytes. `git diff --check` PASS. Packaged Chrome UC-5 walkthrough 1/1 PASS; affected UC-3 and UC-4 browser regressions also PASS in the earlier combined run (whose overall failure was UC-5's incorrect fixture room selector). Failed attempt retained.

[Passing walkthrough and exact submission](../../../output/playwright/student-groups-and-coordinated-lessons/UC-5/1792350421591/ledger.md). Its `submission-files.txt`, `submission.patch` and `source/` pin the implementation; unrelated Python cache excluded. Inspected 768px screenshot: controls fit with readable adjacent checkbox labels and no overflow. Browser context and local servers closed; fixture is now durable test resource.

Remaining destination handoffs: readiness/named save UC-6/7; independent audience assignment semantics and bounded search UC-8/9; consuming timetable UI UCs. No partial generation or class-only approximation is published.
