# UC-1 checkpoint - faster local demo startup

- Date: 2026-10-18. Status: APPROVED from the operator's actual native-process/HTTP boundary.
- Base: `b9458eaa7c2957d8766a33b481949975e255ab90`; no commit or deployment requested or performed.
- Exact production/tool submission hashes: [submission](../../../output/execution/demo-startup/technical-20261018-1/submission.json).
- Pre-existing dirty `tools/demo/__pycache__/check_packaged_samples.cpython-314.pyc` was not edited by this work.

## Implementation

`native-demo` is an opt-in Maven profile for the web application, using Spring AOT with the demo profile and GraalVM
Native Build Tools 0.11.4. The ordinary reactor also builds the unchanged separate JVM kernel. The native classpath
excludes Docker Compose. `DemoRuntimeHints` includes dynamically opened sample JSON, contract schemas and demo logging
configuration. Root/demo READMEs document build, launch, matching toolchain architecture and repeatable comparison.
`tools/demo/measure_startup.py` runs alternating JVM/native launches, captures hashes/logs/timings, performs real HTTP
checks and reaps every process.

## Runtime evidence

- [Execution ledger](../../../output/execution/demo-startup/ledger.md).
- [Same-toolchain ARM64 comparison](../../../output/execution/demo-startup/arm64-20261018-2/results.json): three launches
  each; median framework time **1.270 s JVM / 0.081 s native**; median launch-to-HTTP **1.5559 s / 0.1000 s**.
- [Initial comparison](../../../output/execution/demo-startup/arm64-20261018-1/results.json) retains the first native
  invocation's **1.4821 s HTTP / 0.122 s framework** discrepancy. Subsequent native HTTP trials were 0.1018/0.0959 s.
  Do not present repeated-launch medians as OS cold-start guarantees.
- [Existing SDKMAN JVM comparison](../../../output/execution/demo-startup/sdkman-20261018-1/results.json) also passes,
  but is an Intel JVM versus ARM64 executable comparison: median framework 7.196/0.082 s, HTTP 8.5563/0.1033 s.
  This does not reproduce the user's IDE launch or isolate compilation from CPU architecture; the ARM64 pair above
  is the primary comparison.
- Both runtimes serve source-identical HTML, demo configuration and every source definition/result in the four-school/eight-variant
  bundle. Sample use installs exact definition/result; exported archive entries match accepted JSON values exactly.
  A second session stays EMPTY. Real separate-kernel definition verification and timetable review succeed; malformed
  input and a foreign Origin are refused while the accepted snapshot remains identical.
- [Lifecycle probes](../../../output/execution/demo-startup/native-lifecycle-20261018-2/results.json): invalid session
  configuration and occupied port exit nonzero; SIGTERM reaps the service; the old visitor cookie gets 410
  `SESSION_ENDED`; refreshed CSRF plus explicit Start again yields EMPTY. The first probe's incorrect expectation of
  automatic EMPTY is [retained](../../../output/execution/demo-startup/native-lifecycle-20261018-1/results.json), along
  with all logs. Existing behavior was preserved; no application fix was needed.

## Technical checks and limits

- `JAVA_HOME=/tmp/school-demo-graalvm25-arm64/Contents/Home GRAALVM_HOME=/tmp/school-demo-graalvm25-arm64/Contents/Home
  ./mvnw -Pnative-demo -DskipTests package`: PASS. Native compilation 51.5 s; reactor 58.480 s; binary about 91 MiB,
  Mach-O ARM64; compiler peak RSS 4.21 GB. [Build log](../../../output/execution/demo-startup/technical-20261018-1/school-demo-native-build-4.log).
- `./mvnw -o test` with JVM attach access: PASS, **197 tests** (107 kernel/contract, 90 workspace), no failures/errors
  or skips. Report totals use the final Maven command, not stale XML reports from earlier work.
  [Log](../../../output/execution/demo-startup/technical-20261018-1/school-demo-startup-unit-tests-2.log).
- `git diff --check`: PASS. No feature input, packaged sample or kernel implementation changes.
- Failed attempts are retained: sandbox binding/Mockito attach restrictions, unavailable offline compatibility
  metadata, Intel GraalVM versus ARM64 compiler, and newer native build plugin requiring newer GraalVM schema files.
  Pinning plugin 0.11.4 and using official ARM64 CE 25.0.1 resolves the build issues.
- Native Logback emits a nonfatal unknown-version metadata warning; actual configured application/structured event
  logging is present. No logging failure or missing application resources were observed.
- UI presentation is unchanged; the operator goal is verified through real process/HTTP boundaries, including
  byte-identical static HTML. No browser/visual approval, full PostgreSQL integration-suite result, Linux executable,
  deployed cold-start/capacity result, or solver performance improvement is claimed. No destination handoffs remain.
