# School Kernel

School Kernel is a Java 25 Maven reactor containing the versioned public contract, the stateless planning CLI, and a
local school timetable operations workspace.

## Build

```bash
./mvnw clean verify
```

The build creates `kernel-cli/target/school-kernel.jar` and
`timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar`. The repository launcher uses the kernel artifact:

```bash
./school-kernel plan \
  --definition examples/initial-school.json \
  --output /tmp/timetable.json \
  --step-limit 100

./school-kernel replan \
  --current-definition examples/initial-school.json \
  --definition examples/updated-school.json \
  --current /tmp/timetable.json \
  --output /tmp/revised-timetable.json \
  --step-limit 100

./school-kernel verify \
  --definition examples/initial-school.json \
  --output /tmp/verification.json
```

The output file contains the only machine-readable result. Human diagnostics use stderr. Existing output is preserved
unless `--force` is supplied; replacement is atomic. `--time-limit` and `--step-limit` are mutually exclusive. The
default is a 30-second time limit and seed `0`.

Initial planning returns exit code `0` for `FEASIBLE`, `2` for `INVALID_INPUT`, `3` when no feasible timetable was
found, `4` for a safely handled internal error, `64` for command misuse, `74` for filesystem or publication failure,
and `130` for interruption before publication.

## Public contract

- [`school-definition-v1.schema.json`](kernel-contract/src/main/resources/schema/school-definition-v1.schema.json) defines inputs.
- [`result-v1.schema.json`](kernel-contract/src/main/resources/schema/result-v1.schema.json) defines planning outcomes.
- [`verification-result-v1.schema.json`](kernel-contract/src/main/resources/schema/verification-result-v1.schema.json)
  defines the non-solving import-verification outcome.
- Schema version `1` supports catalog versions `1` through `9`. Earlier catalogs remain readable for existing accepted
  results; [`MV5`](examples/mv5.json) uses catalog `2`, and [`MVK`](examples/mvk.json) uses catalog `9`.
- Definitions and timetable assignment state use RFC 8785 canonical JSON hashed with SHA-256. Revision IDs have the
  form `sha256:<lowercase-hex>`.
- A feasible result assigns every lesson once and satisfies the exact hard catalog in the specification.
- Optional `reservedPeriodIds` names declared school periods excluded from ordinary planning and repair. MVK reserves
  `mon-0`, `tue-0`, `wed-0`, `thu-0`, and `fri-0`; period names or ID suffixes alone have no special meaning. A feasible
  result never assigns a lesson there unless its subject declares `reservedPeriodsAllowed` (catalog `6`). Omitting the
  field preserves existing definitions and results.
- Product scoring is lexicographic: feasibility, period stability, room stability, then ordinary preference penalty.
  Feasibility has two hard levels, physical conflicts first and day-shape rules (cohort gaps, start bounds, subject
  placement) second; both must be zero. Initial planning has zero stability penalties.
- Catalog `2` adds a penalty for each class gap inside an available school-day block and a penalty for weekly class
  loads that differ by more than one lesson. Catalog `3` adds one penalty for each taught class-day whose first lesson
  begins after the third regular slot (or third declared slot when none are reserved). Catalog `4` allows each cohort
  to declare `maxDailyLessonSpread`, the preferred maximum difference between its busiest and quietest available
  weekdays. Omitting it preserves the one-lesson default; exceeding it adds weekly-balance matches but does not make
  the timetable infeasible. Catalog `5` makes cohort gaps a hard constraint, `hard.cohort-gap`: each cohort-day may
  contain at most the cohort's `maxDailyGaps` gaps, which defaults to `0`. A gap-free timetable that cannot be found
  is reported as unsuccessful rather than published with gaps. Catalogs `1` through `4` place no hard limit on gaps,
  so their accepted results remain verifiable. MVK declares `maxDailyLessonSpread: 1` on every cohort and weights cohort gaps at the supported maximum of
  `1,000,000`, late starts at `10,000`, weekly balance at `1,000`, and teacher gaps and non-preferred rooms at `5`. A cohort gap
  is an empty available period between that cohort's first and last lesson of the day, not time before or after its
  lesson block. The late-start preference discourages first lessons after the third regular period; it does not
  compare start times within the first three periods.
  The preferences default to weight `1`; `softConstraintOverrides` can tune
  them from `0` through `1,000,000`. Workspace repair creates at least a catalog `8` successor while preserving its accepted
  predecessor, so a repair proposal also closes any gaps an older accepted timetable had. In repair, preferences choose among equally stable proposals; they do not authorize extra period or
  room moves solely to improve class quality.
- Catalog `6` adds per-cohort start bounds and per-subject placement rules. Slots count regular periods from `1` in
  declared order, so in MVK slot `2` is `mon-2` and the reserved `mon-0` comes before slot `1`. A cohort's hard
  `latestStartSlot` makes a taught day that starts later infeasible (`hard.cohort-late-start`), and
  `preferredLatestStartSlot` replaces the late-start preference's third-slot threshold for that cohort. A subject may
  declare `reservedPeriodsAllowed`, `maxWeeklyReservedLessonsPerCohort`, `dayEdgeOnly` (each lesson is the cohort's
  first or last of the day), and `maxDailyLessonsPerCohort`; each is a hard rule. An occupied reserved period counts
  as part of the cohort's day for gaps. MVK bounds grades 1 through 3 at slot `2` and lets Õpiabi use slot `0` once a
  week per cohort, at most once a day, and only at the start or end of the day.
- Catalog `7` adds a hard per-cohort `dailyLessonSpreadLimit` (`hard.cohort-daily-spread`). It uses the weekly-balance
  count with the limit in place of `maxDailyLessonSpread`, so a feasible timetable has no pair of available weekdays
  whose lesson counts differ by more than the limit. Omitting it imposes no hard limit, and `maxDailyLessonSpread`
  remains the preferred target. MVK limits grades 1 through 4 to `1` and grades 5 through 9 to `2`. Because an even
  week can push a whole day into the afternoon, MVK also bounds grades 4 through 9 at `latestStartSlot: 4` and lets
  classroom `K2` host music lessons next to `MU`; with a single music room, music filled every morning period.
  The workspace plans and repairs with a one-minute limit because MVK does not
  reliably reach a feasible catalog `7` timetable in 30 seconds.
- Catalog `8` lets a cohort declare its curator (`curatorTeacherId`) and home room (`homeRoomId`), and a subject
  declare `curatorLesson`. Each curator lesson must be taught by its cohort's curator, who needs no separate
  qualification for it, and is held in the cohort's home room when one is declared (`hard.cohort-home-room`). A
  lesson with no room preference of its own prefers the home room if the room can host it. A curator creates no
  lessons; a school adds Klassitund only where it wants one. MVK marks Klassitund as its curator lesson, takes
  21 curators from EduPage class records, infers 5D's curator and room from its Klassitund lessons, and assigns a
  synthetic 6C curator from teachers who teach that cohort. It declares home rooms for grades 1 through 4 and
  derives older-cohort rooms from the curator-taught IntÕ lesson, except 5D's Klassitund room and the unresolved 6C.
  MVK locks those eleven IntÕ lessons to their home rooms and Siiri Aiaste's History lessons to A231, where the
  published timetable consistently assigns them.
  Workspace repair upgrades older catalogs to `8` and preserves newer catalog versions.
- Catalog `9` adds top-level `roomAssignments`: each rule selects a subject and optionally a teacher, then declares
  non-empty `allowedRoomIds` or `useHomeRoom: true`. Every matching rule applies, so their allowed-room sets
  intersect. Empty intersections, missing matched home rooms, and conflicts with individual room locks or room
  requirements are invalid input. `hard.room-assignment` requires every matching lesson to use a permitted room in
  planning, repair, and independent verification. MVK uses this for History in A231, Piret Noor Mathematics in B212,
  Heiki Raudla Literature in A209, Chemistry in a newly declared A233, Physics in A223(LAB), PE in KK1/KK2/KK3,
  and Õpiabi in each cohort's home room. Music still prefers MU but may use another music-capable room. The
  requested A233 is not in the EduPage snapshot; it is synthetic in MVK until its source details are confirmed.
- A failed search returns diagnostics only. It never publishes a partial or hard-violating timetable and never claims
  infeasibility or optimality.

The complete behavior, constraint catalog, failure semantics, and score definitions are in
[`spec/kernel-v1/spec.md`](spec/kernel-v1/spec.md), with the catalog `2` quality extension in
[`spec/kernel-schedule-quality/spec.md`](spec/kernel-schedule-quality/spec.md) and the catalog `3` start-quality extension
in [`spec/kernel-cohort-start-quality/spec.md`](spec/kernel-cohort-start-quality/spec.md). The cohort-level spread
setting is specified in [`spec/kernel-cohort-daily-balance/spec.md`](spec/kernel-cohort-daily-balance/spec.md).
School-wide reservation behavior is in [`spec/kernel-reserved-periods/spec.md`](spec/kernel-reserved-periods/spec.md),
curators and home rooms are in [`spec/kernel-cohort-curator/spec.md`](spec/kernel-cohort-curator/spec.md), and
reusable hard room assignments are in [`spec/kernel-room-assignment/spec.md`](spec/kernel-room-assignment/spec.md).

## Operations workspace

The workspace binds to loopback and stores one versioned aggregate in PostgreSQL. Build once, then launch the packaged
application from the repository root:

```bash
./mvnw -q -pl timetable-workspace -am package
java -jar timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar
```

Spring Boot automatically runs `docker compose up` for the repository's PostgreSQL 18.6 service, waits for its health
check, applies Flyway, and stops the service when the application exits. Its named volume preserves the walkthrough
workspace across ordinary restarts. Open <http://localhost:8080/workspace/>. An empty workspace accepts either an
initial definition, a matching verified definition/result pair, or an accepted-bundle ZIP. Import verification runs
through the packaged kernel; the browser does not require JSON editing or command-line use.

To deliberately erase the local walkthrough database and start over:

```bash
docker compose down --volumes
```

The Compose credential is a well-known local-development value, not a production secret. Set
`SPRING_DOCKER_COMPOSE_ENABLED=false` and provide `WORKSPACE_DATABASE_URL`, `WORKSPACE_DATABASE_USERNAME`, and
`WORKSPACE_DATABASE_PASSWORD` to use an externally managed PostgreSQL instance.

## Timetable viewer

The dependency-free web viewer renders a result JSON by class, teacher, or room. Start a static server from the
repository root, then open <http://localhost:8080/ui/>:

```bash
python3 -m http.server 8080
```

[`examples/timetable.json`](examples/timetable.json) loads automatically. Use **Open JSON** or drag another result
file onto the page to inspect it. On mobile screens, the weekly grid becomes a touch-friendly daily agenda.

The target-scale performance measurement is deliberately separate from correctness gates:

```bash
./mvnw -Pbenchmark test
```

It reports elapsed time for 1,000 lessons, 100 teachers, 60 cohorts, 100 rooms, and 60 periods without imposing a
shared-CI timing threshold.
