# Faster demo startup

Provide an optional locally built demo executable that reaches HTTP readiness faster than the existing packaged JVM
application, with repeatable measurements and the same visitor behavior.

## Scope and resolved decisions

- Optimize the demo web process. Scheduling and verification continue through the existing separate kernel process.
- Keep the existing JVM build and launch available. The optional executable is built for its host OS and architecture.
- Measure elapsed process launch to a successful readiness response, and report the framework's startup log separately.
  Compare the median of at least three sequential launches of each artifact on the same machine. These are repeated
  launches with normal filesystem caches, not an OS cold-boot or deployed capacity qualification.
- Exercise a sample, session ownership, export, and a real on-demand kernel operation through HTTP. This operator
  startup goal does not change presentation; no new browser interaction or visual acceptance is introduced.
- The authorized deployment follow-up replaces the existing Railway demo web runtime with its Linux native build.
  Preserve the existing service, Free plan, region, single replica, sleeping policy, resource limits and ingress.
  Upload only pinned production build inputs; verify public behavior and identify native startup from deployed logs.

## Actors and domain terms

- Operator: developer building and running the local demo.
- Visitor: owner of an isolated temporary demo session.
- Readiness: a successful response from the existing demo health endpoint.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
| --- | --- | --- | --- |
| UC-1 | Start a usable demo faster | Operator | None |
| UC-2 | Run the native demo on the existing public service | Operator | Requires UC-1 |

## UC-1 - Start a usable demo faster (primary)

- Goal: Start the local demo with lower measured startup latency and preserve its existing functionality.
- Primary actor: Operator.
- Supporting actors: Visitor, existing scheduling kernel.
- Trigger: The operator builds and launches the optional executable.
- Preconditions: The supported toolchain and local native compiler are installed; the packaged kernel is available.
- Relations: Requires: none. Includes: none. Extends: none.

### Main success scenario

1. The operator builds the optional executable from the same application sources as the JVM artifact.
2. The operator launches the executable with the local demo configuration.
3. The system serves readiness, its static workspace, and the packaged sample catalog.
4. A visitor opens a packaged sample, inspects its accepted timetable, and exports the accepted definition/result pair.
5. A visitor requests an operation requiring kernel verification; the system invokes the existing separate kernel and
   returns its checked outcome.
6. The operator compares at least three launches of each artifact and observes a lower native median time to readiness.
7. The operator stops the demo and can restart it as a fresh temporary workspace.

### Extensions

- 1a. If the toolchain or native compilation is unavailable, the build reports its failure; the existing JVM build
  and launch remain available; end.
- 2a. If configuration is invalid or the port cannot be bound, launch fails without serving readiness; end.
- 4a. If a different session requests the first visitor's state, it receives only its own workspace; continue at step 5.
- 5a. If input is invalid, the system returns the existing located validation refusal without changing the accepted
  timetable; continue at step 6.
- 6a. If the measured median does not improve, record the result as an unsuccessful optimization; end.
- 7a. If a visitor presents a cookie from before restart, the system reports that the session ended; after the visitor
  explicitly starts again, it provides a fresh empty workspace; end.

### Guarantees

- G1. Existing host/origin checks, session isolation, revisions, accepted data, and kernel process limits are preserved.
- G2. Packaged school definitions, timetable variants and exported documents retain their exact JSON values.
- G3. A successful readiness response is verified against the running executable; build success alone is insufficient.
- G4. Measurements identify runtime, machine, artifact, timing method, trial values, and medians without promising
  those timings on other machines or under deployment conditions.
- G5. Building and measuring uses disposable local processes and sessions; no deployment, operator data, billing or
  provider configuration is changed.

### Postconditions

- Success: An operator can build and launch the documented executable, observe lower startup latency, and use the
  preserved demo operations.
- Minimal guarantee: The JVM launch remains available and unsuccessful native builds or checks are reported honestly.

## Out of scope

Native compilation of the scheduling kernel, provider capacity qualification, JVM checkpoint
or training caches, solver throughput optimization, and UI changes.

## UC-2 - Run the native demo on the existing public service

- Goal: Make the native demo available at the existing public demo address.
- Primary actor: Operator.
- Supporting actors: Visitor, hosting provider, existing scheduling kernel.
- Trigger: The operator requests deployment of the native version.
- Preconditions: UC-1 succeeds and the operator has access to the existing service.
- Relations: Requires: UC-1, whose native-compatible application is deployed. Includes: none. Extends: none.

### Main success scenario

1. The operator prepares a host-compatible native runtime with the unchanged separate kernel.
2. The operator verifies readiness and a kernel-backed visitor operation in an isolated local container.
3. The operator verifies the existing destination, its ingress boundary and current provider settings.
4. The operator deploys the pinned production inputs to that service.
5. The service becomes healthy, its logs identify native execution and the demo profile, and a visitor opens a
   packaged timetable and generates a checked candidate through the existing application flow.
6. The operator verifies exact public static/sample data, preserved isolation/origin refusals and unchanged provider
   settings, then clears only disposable verification sessions through a same-image restart.
7. The restarted native service remains healthy and serves the unchanged public address.

### Extensions

- 1a. If the Linux build fails, report retained build evidence and preserve the current public deployment; end.
- 2a. If the local container checks fail, correct the implementation and repeat step 2 before upload; resume at step 2.
- 3a. If the destination or ingress is inconsistent with the existing boundary, report the conflict and retain the
  current service until it is resolved; end.
- 4a. If deployment is denied or fails, report the provider/review result and retain the last healthy release; end.
- 5a. If the new runtime fails readiness or visitor checks, retain diagnostics and restore the previous healthy release
  when needed; end.

### Guarantees

- G1. Deploy only explicitly staged production inputs to the existing demo service; never upload credentials,
  operator files, real-school examples, Git metadata, tests or prior output.
- G2. Preserve the existing plan, billing controls, region, CPU/memory limits, replica count, sleeping policy, public
  address and host/origin/session boundaries.
- G3. Packaged sample documents and static assets match the pinned inputs. Generated candidates retain their
  checked revision/build provenance and existing explicit save semantics.
- G4. Report deployed startup separately from local measurements; successful deployment does not qualify provider
  capacity, costs or solver throughput.

### Postconditions

- Success: The existing public demo runs the native web executable with a verified visitor journey and healthy restart.
- Minimal guarantee: Failed checks or deployment attempts are recorded and the previous healthy release is retained
  or restored; no unrelated provider settings or operator data are changed.
