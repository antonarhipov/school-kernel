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
- Schema version `1` supports catalog versions `1` through `4`. Earlier catalogs remain readable for existing accepted
  results; [`MV5`](examples/mv5.json) uses catalog `2`, and [`MVK`](examples/mvk.json) uses catalog `4`.
- Definitions and timetable assignment state use RFC 8785 canonical JSON hashed with SHA-256. Revision IDs have the
  form `sha256:<lowercase-hex>`.
- A feasible result assigns every lesson once and satisfies the exact hard catalog in the specification.
- Optional `reservedPeriodIds` names declared school periods excluded from ordinary planning and repair. MVK reserves
  `mon-0`, `tue-0`, `wed-0`, `thu-0`, and `fri-0`; period names or ID suffixes alone have no special meaning. A feasible
  result never assigns a lesson there. Omitting the field preserves existing definitions and results. Exceptional
  permission to use a reserved period is not yet supported.
- Product scoring is lexicographic: feasibility, period stability, room stability, then ordinary preference penalty.
  Initial planning has zero stability penalties.
- Catalog `2` adds a penalty for each class gap inside an available school-day block and a penalty for weekly class
  loads that differ by more than one lesson. Catalog `3` adds one penalty for each taught class-day whose first lesson
  begins after the third regular slot (or third declared slot when none are reserved). Catalog `4` allows each cohort
  to declare `maxDailyLessonSpread`, the preferred maximum difference between its busiest and quietest available
  weekdays. Omitting it preserves the one-lesson default; exceeding it adds weekly-balance matches but does not make
  the timetable infeasible. MVK declares `1` on every cohort and weights weekly balance and late starts at `30`,
  cohort gaps at `20`, and teacher gaps and non-preferred rooms at `5`. The late-start preference discourages first
  lessons after the third regular period; it does not compare start times within the first three periods.
  The preferences default to weight `1`; `softConstraintOverrides` can tune
  them from `0` through `1,000,000`. Workspace repair creates a catalog `4` successor while preserving its accepted
  predecessor. In repair, preferences choose among equally stable proposals; they do not authorize extra period or
  room moves solely to improve class quality.
- A failed search returns diagnostics only. It never publishes a partial or hard-violating timetable and never claims
  infeasibility or optimality.

The complete behavior, constraint catalog, failure semantics, and score definitions are in
[`spec/kernel-v1/spec.md`](spec/kernel-v1/spec.md), with the catalog `2` quality extension in
[`spec/kernel-schedule-quality/spec.md`](spec/kernel-schedule-quality/spec.md) and the catalog `3` start-quality extension
in [`spec/kernel-cohort-start-quality/spec.md`](spec/kernel-cohort-start-quality/spec.md). The cohort-level spread
setting is specified in [`spec/kernel-cohort-daily-balance/spec.md`](spec/kernel-cohort-daily-balance/spec.md).
School-wide reservation behavior is in [`spec/kernel-reserved-periods/spec.md`](spec/kernel-reserved-periods/spec.md).

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
