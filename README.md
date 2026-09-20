# School Kernel

School Kernel is a stateless Java 25 command-line runtime for generating and replanning a recurring school timetable.
The current implementation exposes initial planning through canonical, versioned JSON.

## Build

```bash
./mvnw clean verify
```

The build creates `target/school-kernel.jar`, a dependency-inclusive executable JAR. The repository launcher uses that
artifact:

```bash
./school-kernel plan \
  --definition examples/initial-school.json \
  --output /tmp/timetable.json \
  --step-limit 100

./school-kernel replan \
  --definition examples/updated-school.json \
  --current /tmp/timetable.json \
  --output /tmp/revised-timetable.json \
  --step-limit 100
```

The output file contains the only machine-readable result. Human diagnostics use stderr. Existing output is preserved
unless `--force` is supplied; replacement is atomic. `--time-limit` and `--step-limit` are mutually exclusive. The
default is a 30-second time limit and seed `0`.

Initial planning returns exit code `0` for `FEASIBLE`, `2` for `INVALID_INPUT`, `3` when no feasible timetable was
found, `4` for a safely handled internal error, `64` for command misuse, `74` for filesystem or publication failure,
and `130` for interruption before publication.

## Public contract

- [`school-definition-v1.schema.json`](src/main/resources/schema/school-definition-v1.schema.json) defines inputs.
- [`result-v1.schema.json`](src/main/resources/schema/result-v1.schema.json) defines structured outcomes.
- Schema and catalog version `1` are the only supported versions.
- Definitions and timetable assignment state use RFC 8785 canonical JSON hashed with SHA-256. Revision IDs have the
  form `sha256:<lowercase-hex>`.
- A feasible result assigns every lesson once and satisfies the exact hard catalog in the specification.
- Product scoring is lexicographic: feasibility, period stability, room stability, then ordinary preference penalty.
  Initial planning has zero stability penalties.
- A failed search returns diagnostics only. It never publishes a partial or hard-violating timetable and never claims
  infeasibility or optimality.

The complete behavior, constraint catalog, failure semantics, and score definitions are in
[`spec/spec.md`](spec/spec.md).

## Timetable viewer

The dependency-free web viewer renders a result JSON by class, teacher, or room. Start a static server from the
repository root, then open <http://localhost:8080/ui/>:

```bash
python3 -m http.server 8080
```

[`examples/timetable.json`](examples/timetable.json) loads automatically. Use **Open JSON** or drag another result
file onto the page to inspect it.

The target-scale performance measurement is deliberately separate from correctness gates:

```bash
./mvnw -Pbenchmark test
```

It reports elapsed time for 1,000 lessons, 100 teachers, 60 cohorts, 100 rooms, and 60 periods without imposing a
shared-CI timing threshold.
