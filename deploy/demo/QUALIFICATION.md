# Deferred deployment qualification

The implementation session on 2026-10-16 performs no deployment or provider mutation. Local browser evidence is in
`spec/demo-deployment/checkpoints/UC-7.md` through `UC-11.md`; it is not public launch qualification. This runbook
makes the remaining operator work concrete for the later deployment request. Preserve the intended school target
and retain failed attempts. Do not enable public launch from this checklist alone.

Use one isolated Hobby service/replica in the EU, sleeping off, zero deployment overlap and a generated HTTPS domain.
`railway-qualification.ts` is prepared modern IaC, not a successfully applied configuration. Review its plan with the
installed SDK and bind actual service settings, domain, image digest, build/configuration hashes and date to evidence.
Do not repurpose the existing Free smoke service or a different workload without the requested deployment scope.
Configure the workspace-wide $5 spending alert and $15 hard limit from the spec and record their effective values;
these affect every workload in that workspace. Hobby included usage is a target, not a hard cost ceiling. Neither
application allowance nor telemetry replaces the provider hard stop.

## Cold computations

Prepare fictional inputs without network access:

```sh
python3 tools/demo/qualify_capacity.py --output /tmp/demo-capacity-inputs
```

This creates Small, Two-class and Full inputs plus both representative 60/72-cohort targets (2,160 lessons,
100 teachers, 80 rooms, 50 periods; W=8,640,000). Both representative definitions were independently checked by the
real packaged kernel locally. Generation does not solve them or prove feasibility within the search budget.

On an isolated validation deployment explicitly configured with `demo.reuse.enabled=false`, run:

```sh
python3 tools/demo/qualify_capacity.py --base https://THE-QUALIFICATION-HOST --output /tmp/demo-cold-attempt-1
```

The runner refuses a service that does not explicitly report reuse disabled. It uses five consecutive physical
computations for each sample/target, checks verified proposal state, unique computation IDs, requested effort,
original process durations and complete raw snapshots. It never accepts a generated proposal, deploys, changes
settings, silently retries a failure or declares combined capacity. Each invocation creates a fresh directory.
Initial Thorough is 60 seconds; if the target fails, explicitly configure up to 120 seconds and repeat. Failure at
120 seconds with the intended dimensions withholds launch; do not lower the target or substitute cached results.
Record wall, construction/search, verification, termination and cleanup separately using the work/process events.

## Combined memory and bounds

Bind all of the following to the exact build and effective configuration before recording PASS:

1. Capture both inclusive complexity boundaries and one-over refusals (including a small-byte, large-work input),
   compiled repair and accepted import. Local deterministic boundary tests remain useful evidence; deployed refusal
   must retain exact prior Current and launch no process.
2. Fill visitor artifacts to the configured byte ceiling, retain the largest permitted workspaces at the configured
   ceiling, then overlap a large solve with two **standalone** verifications. Cache entries/bytes, active workspaces,
   distinct queue, running solve/verification counts and reserved time must corroborate the overlap in summaries.
   A solve's own checking process is separately bounded and can also overlap two standalone verifiers. Include
   non-heap/runtime/process overhead; heap settings alone are not aggregate memory proof.
3. Record CPU throttling and cgroup current/peak/limit/OOM events continuously through startup, construction/search,
   verification, cancellation, timeout, process failure and cleanup. The read-only helper can observe an existing
   disposable Linux container while separate real traffic runs:

   ```sh
   python3 tools/demo/observe_resources.py --container THE-LOCAL-CONTAINER --seconds 300 --output /tmp/demo-memory-1
   ```

   On the deployed service use the same script in its Linux cgroup or retain provider/container measurements with
   equivalent fields. This helper does not create workload, upload anything, or turn an idle sample into qualification.
   Cgroup lifetime peak and sampled peak are distinct. Confirm provider aggregate graphs and container headroom;
   no per-process attribution is inferred from service CPU/memory graphs alone.
4. Prove the configured solve overhead/verification maxima and termination grace conservatively cover real lifetime.
   Reserved time remains held until child reap. Check failure/cancel settlement and UTC rollover with active
   reservations; actual kernel-process events and allowance summaries must agree. Reduce the workspace ceiling
   and repeat combined load if necessary, preserving the intended individual-school target.

## Journeys, platform and final values

Run isolated public Chrome owners through sample use, custom definition/initial solve/acceptance, protected repair,
manual publication, accepted export/re-import, stale/foreign refusal, expiry and same-image restart. Reuse the real
actor tests with explicit remote support only where they support it; local-only process-injection tests must remain
local. Confirm accepted files and private ownership by complete value, not screenshots alone. Repeat edge host,
scheme, spoof-header and health-host tests under the isolated-ingress preflight in README.md. Check production-only
packaging and sample/scoring readiness. Retain default-profile persistence/authoring/planning/repair/restart regression
results from a separate disposable PostgreSQL instance.

Record effective W/lesson limits, supported combinations (not the Cartesian product of independent maxima),
Thorough duration, all process maxima/reservations, replica/CPU/container/JVM limits, workspace/cache ceilings,
raw durations and memory/OOM/throttling evidence, public journey ledgers, and actual idle/load/egress usage/spend.
Only after every required dimension passes enable default reuse and bind a fresh startup/configuration event.
Shutdown/redeploy stops admissions/child work and ends all in-memory state; no continuity is promised.

## Usage and retention review

`DemoEvents` writes raw single-line JSON to standard output. Exactly seven families are emitted: `startup`,
`session`, `definition`, `work`, `reuse`, `decision`, `aggregate`. `demo.observability.summary-interval` defaults to
`1m`; use `1s` only on an isolated test service. Summary groups are `context`, `workspaces`, `queue`, `processes`,
`cache`, `allowance`, `productUse`. Activity keys absent from an interval mean zero. Gauges and interval counters
are distinct; snapshots are taken under each component's lock and may straddle a concurrent state transition.

Count compute cost from `process_finished.processMs`, separated by SOLVE/VERIFICATION (verification success is VERIFIED, not a search-feasibility claim). `logical_finished` counts
visitor outcomes and queue wait; cached `originalSolveProcessMs`/`originalVerificationProcessMs` are provenance,
never added physical consumption. An initial cache miss followed by a verification coalescing hit records both
lookup miss and avoided process. Wait percentiles are upper bounds in 10-ms bins (ten-minute cap), distinct from
complete request duration. Startup sample checks are reported in startup and excluded from visitor allowance.
UTC day/reset and reservation settlement/released-time fields reconcile allowance; restart UUID identifies a new
in-memory counter epoch, never a visitor. No session identifier or anonymous tracking identifier is emitted.

Custom-definition origin is a bounded client-declared category, not independently proved file/sample lineage.
Product use distinguishes SAMPLE_OPEN, DEFINITION_SAMPLE/FILE/CUSTOM, ACCEPT_INITIAL/REPAIR, MANUAL_PUBLISH,
EXPORT and IMPORT. Acceptance follows the successful atomic mutation, so merely opening a sample or proposal
never counts. Conversion keys count each milestone at most once per in-memory workspace, following definition/sample
use; they carry no identity. Import means file continuation/exchange, not proof of the same person returning or
account/persistence demand. Polls emit no activity event. No school IDs/names, raw documents, IP/cookies, browser
labels or session IDs are included in application events; provider request logs are a separate retention scope.

Review Hobby CPU/memory/disk/network graphs (up to 30 days) and current/projected workspace usage/spend. Do not
configure a Pro-only threshold monitor or add an analytics backend. Hobby's seven-day viewing window differs from
staff-reported underlying 90-day US West storage and undeletable request metadata; absence in the UI is not deletion.
The visitor disclosure in the application states the same four retention scopes as the specification.
At one summary/minute there are 1,440/day and 10,080/week; at 1 KiB/line approximately 1.4 MiB/day and 9.8 MiB/week,
plus other events. The reviewed platform cap is 500 lines/second/replica; no total storage guarantee is inferred.
