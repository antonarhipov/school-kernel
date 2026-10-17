# UC-6 checkpoint

Base `be5b76d0ffc8a9851c4e8308540b1de12c5b4425`; reviewable uncommitted diff on approved UC-1–5.
Class-owned unique subject rows, required/allocated/unassigned accounting; explicit teacher allocations and split
teaching; explicit named qualification/eligibility grants with cancel non-mutation; derived teacher workloads and
real class-plan links. Stable ordinary occurrences retain identity, names, series and settings; additions create
only assigned occurrences, reductions keep oldest surviving identities; imported ungrouped homogeneous templates
stay ungrouped. Protected rows/occurrence locks refuse count/allocation/removal with advanced-route guidance.
Ordinary row removal previews demand, allocations, blocks/room settings and requires confirmation.

Production: `setup-plans.js`, integrations in setup model/editor/catalog/teachers, `demo-workbench.js`,
`DemoController` exposing existing configured lesson/work limits. Counts over the effective lesson limit stay as
invalid draft input without allocating unbounded occurrence objects. No new product limit was introduced.
Tests: `tools/demo/setup-plans.test.mjs`, `SchoolSetupCatalogBrowserIT` plus updated UC-4/5 destination checks.
Node checks 27/27 pass. Full packaged Chrome UC-1–6 journeys: 6/6 pass. Failed empty-Classes navigation attempt
retained; missing-selection guard fixed and full affected journeys repeated. `git diff --check` passes.

Browser evidence covers demand without placeholders, cancelled/confirmed explicit grants, three/two split,
exact surviving values, one spreading series, workload/navigation/reload, over-allocation/unfinished count,
protected refusal durable equality, scoped deletion and 320/390/1440px. The owning-plan handoffs from UC-4/5 are
closed. Row block/room settings and kernel enforcement remain UC-10/11/12/13 destinations.
