# Local demo HTTP and direct artifact loading

Date: 2026-10-18. User explicitly requested direct local testing with the demo profile and removal of bundled-artifact startup verification. This supersedes earlier startup-verification requirements. Base remains `be5b76d0ffc8a9851c4e8308540b1de12c5b4425` plus the shared uncommitted feature and prior UI corrections. No deployment or commit performed.

## Implemented behavior

- `demo` with its default loopback-only host configuration works at `http://localhost:8080/workspace/` and `http://127.0.0.1:8080/workspace/`. No second profile, certificate or proxy-header workaround is needed.
- The local configuration binds only to loopback, uses HTTP-compatible session cookies, requires same-origin mutations, and rejects remote peers/destinations, foreign hosts/origins and forwarded headers. A public hostname, trusted proxy or Railway configuration retains the existing HTTPS gate and secure-cookie configuration. Mixed public/local host lists do not select local mode.
- Startup loads the four definitions/eight results directly and generates their small export manifests in memory. It performs no sample fingerprint/evidence comparison, scoring or kernel verification. The kernel code fingerprint remains available for on-demand review/generation provenance. Unreadable required JSON still prevents a partial catalog from being published.
- Explicit edits/review/generation retain on-demand kernel checks. Artifact verification now belongs to maintenance; `check_sample_readiness.py` performs its positive/corrupt/stale cases offline and never starts the web app. README and specifications record the user's changed boundary.

## Automated PASS

[Execution/source ledger](../../../output/school-setup-editor/2026-10-18-local-http/ledger.md).

- 19 unit checks: local HTTP/binding/cookies and refusal cases, existing public/proxy/Railway boundary cases, sample copying/lifecycle/direct loading/unreadable-resource cases.
- Real packaged Chrome HTTP walkthrough: localhost and IPv4 alias; clean 520-lesson baseline; non-Secure local cookies; **zero startup kernel processes**; a visitor's explicit edit invokes real review; setup draft persists/resumes; reload opens the clean matrix. No TLS flags, host override or ignored certificate errors in this journey. Only its ephemeral port and disposable kernel wrapper differ from a normal local launch. Screenshots of the clean matrix and settled overlay inspected directly.
- Existing eight-variant timetable browser journey passes with zero startup processes. It retains selectors, filtering and responsive inspection coverage.
- Five offline artifact cases pass: complete bundle accepted; stale score and invalid full/English/Imperial result rejected by maintenance. No web server is started by those checks.
- `git diff --check` passes. Disposable browser contexts and servers close normally. Operator data and the public service were not changed.

Retained attempts: sandbox Mockito self-attachment failure (authorized test rerun passes); first HTTP browser attempt incorrectly tried to restart an already-active session (test corrected to exercise actual editing); a test-only Spring Boot accessor compile error (corrected); first successful screenshot caught the rollout in progress (final journey waits for animation and captures the settled overlay). No failed attempt is presented as passing evidence.
