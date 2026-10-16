# Demo container preparation

This is a locally testable deployment artifact, not a qualified public release. Read
[execution status](../../spec/demo-deployment/status.md) before deploying. UC-2's public private-session, restart and
expiry checks pass under the audited isolated-ingress boundary described below. See UC-6 status for proposal execution, queueing, reservations and reuse evidence. Acceptance, the remaining visitor journeys, capacity and operational qualification belong to their later use cases. Do not publish this intermediate build to visitors.

Build from the repository root:

```sh
docker build --file deploy/demo/Dockerfile --tag school-kernel-demo:local .
```

The build context explicitly includes only production inputs. It excludes real-school examples, tests, operator
configuration, Git metadata and prior build output. The runtime image contains the workspace JAR, packaged kernel
and two launchers. It runs as UID/GID 10001, with a private umask and no database/volume. The demo profile binds to
`0.0.0.0:$PORT` (8080 by default). All eight packaged timetable pairs must pass code/document fingerprint checks and real
kernel verification before the HTTP server becomes ready. A scoring-evidence mismatch intentionally fails startup;
never regenerate that evidence merely to bypass the failure. Regenerate/verify the sample results through the
explicit maintenance workflow when kernel scoring/build inputs change.

Starting JVM heap ceilings are 512 MiB for the web process, 1,024 MiB for a solve and 256 MiB for each verification.
`DEMO_WEB_HEAP_MB`, `DEMO_SOLVER_HEAP_MB` and `DEMO_VERIFY_HEAP_MB` can override them with positive integers.
These are tuning inputs, not an aggregate memory qualification; native memory, caches, workspaces and concurrent
verification must be measured under UC-12. The image's Java process is PID 1 and receives SIGTERM directly.

For a health-only local smoke check, use an isolated container (never a local school database):

```sh
docker run --rm --name school-kernel-demo-smoke --read-only \
  --tmpfs /tmp:rw,noexec,nosuid,size=128m --cpus 2 --memory 3000000000 \
  --publish 127.0.0.1:18080:8080 --env DEMO_PUBLIC_HOSTS=demo.example \
  school-kernel-demo:local
```

In another terminal, `curl -H 'Host: healthcheck.railway.app' http://127.0.0.1:18080/healthz` must return `UP`
without a session cookie. Normal visitor requests require HTTPS and a configured host, so this HTTP smoke check
does not exercise the browser or prove an edge trust boundary. Stop the disposable container after the check.

The legacy `deploy/demo/railway.json` describes the original qualification settings. The file
sets one replica, sleeping off, `/healthz` readiness, 2 vCPU/3 GB starting limits and shutdown timing. Its field names
were checked against [Railway's live schema](https://railway.com/railway.schema.json) on 2026-10-16. It intentionally
does not invent an operator account, region, domain, proxy network or financial control. The live API now rejects
new legacy config-file assignments and recommends `.railway/railway.ts`; migrate these qualification settings before
launch. Merely passing the legacy JSON schema does not prove the current deployment API accepts the file.

The operator must supply the Hobby project/service/environment and generated domain, select one EU region, and
set `DEMO_PUBLIC_HOSTS` to the exact lowercase generated domain authority. Railway does not publish a stable
proxy-peer address range. Use `DEMO_RAILWAY_ISOLATED_INGRESS=true` only for an isolated project/environment that
contains exactly this service, one replica and no TCP proxy. First retain a passing read-only preflight:

```sh
python3 tools/demo/check_railway_boundary.py --project <project-id> --environment <environment-id> \
  --service <service-id> --domain <generated-domain> --output <evidence.json>
```

This option also requires Railway's project/environment/service identities and an exact `RAILWAY_PUBLIC_DOMAIN`
match at startup. Leave `DEMO_TRUSTED_PROXY_ADDRESSES` empty. The HTTP edge overwrites scheme/host forwarding,
but a private service or raw TCP caller can forge it; the isolated topology is therefore a required trust boundary.
Recheck before every deployment and after restart; adding a service, domain or TCP proxy requires requalification.
See [RULE-1](../../spec/demo-deployment/rules.md) and its provider sources. There is no per-request cryptographic
edge identity. Only an operator-controlled stable proxy uses the alternative `DEMO_TRUSTED_PROXY_ADDRESSES`
allowlist. Both options default off/empty. Never guess a Railway subnet or trust all private networks.
`Forwarded` is refused. A single `X-Forwarded-Host` may only match the independently allowlisted native `Host`;
it never supplies authority. Duplicate/mismatched forwarding and foreign Origin remain refused.
Only GET `/healthz` accepts `healthcheck.railway.app`.

Configure and record the workspace-wide $5 spending alert and $15 hard limit, including effects on other workloads.
The repository file cannot establish those account settings. No provider token belongs in the repository or browser.
An isolated Free smoke service has been deployed; it is not the qualified public demo. See the execution status.

## Demo proposal execution settings

The demo routes both initial and ordinary compiled-repair requests through one FIFO solver slot, including result
checking. `POST /api/runs` accepts only `{ "effort": "QUICK" }` or `{ "effort": "THOROUGH" }`; omission uses the
school-size default. The local profile retains its existing run API. `GET /api/runs/{id}` is a small private status
resource; DELETE detaches only this visitor. A generated proposal never changes Current by itself.

The application configuration file controls `demo.search.quick-seconds` (5), `thorough-seconds` (60, at most 120),
`lesson-threshold` (200), `demo.queue.max-waiting` (8), `estimated-start-limit` (2m), and `abandonment` (2m).
`demo.kernel.solve-overhead` (10s) bounds process startup/search overhead in addition to the selected search budget;
termination adds a reserved four seconds. Each solve also reserves its own `verification-timeout` (30s) plus four
seconds for termination. Default maximum reservations are therefore **53s Quick / 108s Thorough**. Queue waiting
is free; actual process lifetimes consume the allowance, and unused reservation is released after physical exit.
Observed process/checking overhead refreshes approximate queue estimates. These process bounds still require the
UC-12 deployed capacity measurements; accepting a 120-second configuration does not qualify it for launch.

`demo.reuse.enabled` (true), `max-bytes` (67108864) and `ttl` (1h maximum) govern one shared visitor cache for exact
verification evidence and verified solve artifacts. Disabling it also disables in-flight coalescing. Artifacts retain
complete request identity, result and its verification together; reads never extend completion TTL. Packaged samples
are independent. Configuration changes take effect on restart, which explicitly clears sessions, jobs and counters.

## Isolated Free-plan smoke test

`railway-free-smoke.ts` records the separate test service's effective settings: one EU replica, one CPU and
500,000,000 bytes of memory. Free requires `sleepApplication=true`; it cannot qualify the always-on specification.
Keep the full qualification configuration in `railway.json`. For this initial test, set `DEMO_WEB_HEAP_MB=128`,
`DEMO_VERIFY_HEAP_MB=96` and `DEMO_SOLVER_HEAP_MB=128`; restrict the workspace count to two using
`SPRING_APPLICATION_JSON={"demo":{"max-workspaces":2}}`. The first test covers startup, packaged samples,
HTTPS and session isolation. It does not qualify solving or the full visitor workload. No account upgrade is required
to attempt it. The first deployed test passed startup, packaged sample checks and a real two-owner HTTPS Chrome
journey, but restarting the service exposed intermittent 403s with the empirical exact-peer proxy allowlist.
The [UC-2 resumption](../../spec/demo-deployment/checkpoints/UC-2.md) replaced that list with the explicit isolated-ingress
mode after the provider topology check. The complete public ownership, two-restart and idle/absolute-expiry journeys
pass, followed by restoration of 60-minute idle / 8-hour absolute expiry and the two-workspace smoke cap.
Keep the isolation preflight and configuration constraints above; this passing session boundary does not qualify launch.
The [first test ledger](../../output/execution/demo-deployment/UC-12/railway-free-20261016-3/ledger.md) retains the original failure.
No solve capacity is established. This exported IaC file requires the Railway
TypeScript SDK for plan/apply; it is a snapshot of settings actually applied through the service API, not evidence
of a successful IaC plan/apply.

The matching local preflight is:

```sh
python3 tools/demo/smoke_container.py --memory-bytes 512000000 --cpus 1 --web-heap-mb 128 --verify-heap-mb 96
```

Use an explicitly staged production-only directory for CLI upload. Configure the isolated service before uploading;
the actual smoke upload has no legacy `railway.json`. `.dockerignore` restricts image inputs but does not by itself guarantee the CLI upload excludes
operator files, source examples or prior evidence. Keep a hash manifest of the staged upload outside that directory.
Authenticate the local CLI through `railway login --browserless`; the operator authorizes it in their own signed-in
browser. Credentials, tokens and the transient pairing code do not belong in the upload or evidence.

Before public launch, complete every remaining UC and record the deployed qualification required by
[UC-12](../../spec/demo-deployment/spec.md#uc-12---launch-a-demo-with-measured-capacity-and-spend-protection): cold
reuse-disabled target-school solves, actual process/reservation bounds, aggregate memory at the workspace/cache
ceiling with one solve and two verifiers, public two-browser lifecycle journeys, expiry/redeploy, edge spoof checks,
safe logs, and spend/resource controls. Keep the spec's intended larger-school target if initial settings fail.

## Timetable-first browser experience

The demo opens Kasevälja Põhikool / Variant A directly. School and timetable selectors navigate browser-held
matrices instantly. Changing schools clears inspection filters; changing versions in one school retains them.
The inspector edits teacher, room and time slot. The stateless `/api/demo/timetable/review` boundary invokes the
kernel's `review` operation without solving, checks the entire candidate and returns every hard-rule diagnostic
and preference costs. Errors link to affected lessons and block naming/saving. Save creates an immutable browser
version; the original remains intact. Browser storage failures retain previous complete data and current edits.

School copies, named timetables, complete draft source snapshots and editor text use browser storage. They survive
server expiry/restart/deployment on the same origin/device; clearing site data removes them. Temporary validation
sessions renew on an explicit check/generation action. Utilities contains only the school-definition editor and
Quick/Thorough generation. Generation uses the existing owner/queue/allowance/cancellation/cache boundary; its
complete checked output becomes a browser candidate requiring a name. The normal local workspace is unchanged.

The [timetable experience specification](../../spec/demo-timetable-experience/spec.md) supersedes the old demo
navigation/editor/import/export/repair UI contracts. Historical demo-deployment browser journeys address that
retired UI; the current recorded actor journeys are `DemoTimetableBrowserIT`. Backend isolation, process, queue,
allowance, transport and local-workspace regression tests remain applicable.

## Maintaining the four schools and eight sample timetables

Ordinary builds package committed fictional documents from `demo/timetables`; they never read real examples or
run generation. The catalog contains Ranniku õppehoone (5A/5B), Kasevälja Põhikool (grades 1–9), Coruscant Imperial Academy
and Meadowbrook Community School, each with Variant A and B. Meadowbrook is independently constructed in English:
22 classes in Years 1–9, 614 weekly lessons, 41 teachers, 35 rooms and 30 periods. Small school is absent.
Historical synthetic examples live only in test resources. The Imperial academy is independently constructed with
18 cohorts, 540 weekly lessons, 29 specialist teachers and 37 rooms; junior/intermediate/senior curricula have
25/30/35 lessons a week. The [curriculum specification](../../spec/demo-timetable-experience/spec.md#imperial-academy-progression) records the progression.

For deliberate fixture maintenance, run `python3 tools/demo/build_timetable_samples.py`, then
`java --class-path kernel-cli/target/school-kernel.jar tools/demo/BuildTimetableVariants.java timetable-workspace/src/main/resources/demo/timetables`
and `python3 tools/demo/record_timetable_evidence.py`. To rebuild only Meadowbrook without touching the original
schools, run `python3 tools/demo/build_english_school.py`, then the Java builder above with a final `english`
argument, then record the evidence. For only the academy, use `python3 tools/demo/build_imperial_academy.py`,
then the Java builder with a final `star-wars` argument and record the evidence. Deterministic feasible placement seeds live under
`tools/demo/seeds`, outside the shipped resources. To reseed the two-class placements, explicitly run the packaged
CLI's `plan` command with its definition and `tools/demo/seeds/two-class-result.json` as the output.
The variant builder evaluates every candidate with all kernel rules. Evidence recording independently reevaluates
all eight stored pairs, score breakdowns, weights and revisions before hashing; stale claimed scoring is refused.

`tools/demo/check_packaged_samples.py --output <evidence.json> <kernel.jar> <workspace.jar>` scans actual JARs,
including nested dependencies, for source staff identities, original references, real examples and retired samples.
`tools/demo/check_sample_readiness.py --output <directory>` requires startup refusal for corrupted disposable JARs
and verifies that maintenance cannot bless stale scores. Tools, seeds, tests and real examples are excluded from
production images. Capacity/billing qualification remains separate from functional browser verification.

UC-5 custom saved-definition use is approved from complete local packaged Chrome/fault journeys and the public
Chrome journey on the existing [Free smoke service](https://demo-smoke-production.up.railway.app/workspace/).
Deployment `8bc49107-ee22-40f3-9e13-1ee2d5eb6439` retains the verified image after the cleanup restart; all 33 public
static hashes, sample startup checks, stateless access and edge refusals pass. Provider settings are unchanged.
See the [UC-5 checkpoint](../../spec/demo-deployment/checkpoints/UC-5.md) for pinned inputs and verification evidence.
UC-6 proposal execution is now approved on deployment `a3ebd477-0aa8-46ee-a8b5-38d33d35b78a` after complete local
fault and public Chrome journeys, both actual presets/operations, private sharing/reuse/cancellation and same-image
cleanup. All 34 public static hashes and final isolated-topology checks pass; settings remain unchanged. See the
[UC-6 checkpoint](../../spec/demo-deployment/checkpoints/UC-6.md). UC-7/8 acceptance/remaining journeys and UC-12
public-launch/capacity qualification remain outstanding.


## Remaining implementation, 2026-10-16

UC-7–11 now have local packaged Chrome approval (protected repair, explicit proposal acceptance, manual publication,
accepted export and verified archive/pair import). Their public regressions are deferred by the user's instruction.
Safe usage events/minute summaries and capacity preparation are implemented locally; no deployment, provider setting,
financial control or plan changed during this implementation pass. See [qualification/runbook](QUALIFICATION.md) for
cold target-school measurements, combined-memory/process evidence, public journeys, spend controls and log review
that remain required after the operator requests deployment. Historical Free-smoke results above retain their
original scope and do not qualify the current implementation for public launch.

## Current Free deployment, 2026-10-17

The timetable-first experience is live at [the demo](https://demo-smoke-production.up.railway.app/workspace/),
deployment `2ecc350c-5519-4a0d-ba20-a4f19edd5137` (definition-input hardening; Utilities cleanup, progressive Imperial curriculum and compact retro styling preserved). The provider still reports FREE, with the original one-CPU,
500 MB, one-replica sleeping configuration and unchanged variables. The kernel launcher routes both `verify` and
`review` through `DEMO_VERIFY_HEAP_MB` (96 MiB on this service); real full-school review, including the 614-lesson English school and 540-lesson Imperial academy, passes at that limit.

The complete public Chrome journey passes instant/filter-aware switching, actionable conflicts, exact named saves,
real Quick generation and narrow layout. Exact saved browser data survives an actual same-image restart. All 40
static hashes/eight packaged pairs, health and ingress checks pass before/after cleanup; final active workspaces are
zero. [Cleanup-release deployment ledger](../../output/execution/demo-timetable-experience/local-data-20261017-1/ledger.md). Historical UI
journeys above retain their original scope. Free sleeping and this functional check do not qualify larger-school
solve capacity, combined peak memory or paid launch/spend controls.

Utilities now includes **Delete local schools and timetables…** with explicit browser/device-only scope and a
confirmation. It removes saved definitions, timetables and unfinished edits while preserving bundled examples,
unrelated browser data and server state. The complete isolated/public Chrome cleanup journeys pass, including
Cancel, removal-failure rollback, stale-tab protection and saving again after cleanup. The release changes exactly
three browser assets; concurrent backend/editor work in the shared checkout is preserved and excluded.

The subsequent user-authorized hardening release adds input admission (four global/two per session), constrained
parsing and definition dimensions, fail-fast schema checks, concealed unexpected errors and debounced/size-guarded
editor autosaving with storage recovery. The complete public hardening Chrome journey passes, including actual HTTP
422 without workspace mutation and exact checked-copy saving. All 41 static assets and eight timetable pairs match
the pinned production upload. A same-image restart verifies samples again and leaves zero active workspaces or
processes. Plan, full service manifest, effective runtime settings and kernel build are unchanged.
[Current deployment ledger](../../output/execution/definition-input-hardening/deployment-20261017-1/ledger.md).
