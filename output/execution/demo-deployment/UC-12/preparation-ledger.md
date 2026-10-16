# UC-12 local preparation ledger

Date: 2026-10-16. Base: `d55ab636558224f2d33a5e71313e840dd9486428` (`demo`).
Image: `sha256:6e5dc68824ac3a6f8f58b65cde70e86fedd144724a43e94636b8116fe13e5816`.

Changed deployment inputs: `.dockerignore`, `deploy/demo/Dockerfile`, `deploy/demo/start`, `deploy/demo/kernel`,
`deploy/demo/railway.json`, `deploy/demo/README.md`, `tools/demo/smoke_container.py`. Exact input hashes are in
[source-sha256.txt](source-sha256.txt). The UC-2 source manifest binds the application packaged in this image.
No ordinary build reads the real-name `examples` directory; Docker's allowlist also excludes it from the build context.

Checks:

- `docker build --file deploy/demo/Dockerfile --tag school-kernel-demo:local .`: PASS; the Linux build compiles/packages
  production sources without tests. [Initial log](image-build.log), [pinned-base repeat](image-build-pinned.log).
  This build is packaging evidence; the separate UC-1 and UC-2 checks own their test claims.
- `python3 tools/demo/smoke_container.py`: PASS on initial and pinned image. [Final ledger](1792141341654519000/ledger.md),
  [final command output](image-smoke-pinned.log), [retained server log](1792141341654519000/server.log).
  Exactly three startup verifications run; health creates no cookie. Untrusted HTTP and forged forwarding/host
  requests get 403 without a session or workspace ETag. SIGTERM exits 143; owned containers are removed.
- Inspected the final runtime artifacts from a stopped, owned temporary container; copied files were deleted with
  its temporary directory and the container removed. The image contains only the two runtime JARs and launchers
  under `/opt/demo`. [Privacy/artifact report](1792141341654519000/privacy-artifact-scan.json): 53 text resources,
  zero source school/teacher name matches. This is a targeted scan plus production-only context evidence, not an
  assertion about arbitrary visitor data or provider metadata.
- Downloaded `https://railway.com/railway.schema.json` read-only on 2026-10-16. Every configured field's presence,
  type, constant and numeric bounds matched that live schema; [retained schema](railway-schema.json). No package
  installation was needed for this field check. `sh -n deploy/demo/start deploy/demo/kernel`: PASS.
- Final `git diff --check`: PASS.

Verdict: **local preparation PASS; UC-12 BLOCKED**. Actual Railway resource/spend/edge evidence and later visitor
work remain required. No Railway CLI is installed and the available computer-use inventory exposes no connected
browser surface. Operator project/service/domain and authenticated access are still missing. No account credentials
were read or copied. No platform provisioning, public launch, financial change or external communication occurred.
The image remains a local intermediate artifact; the spec's intended target and launch boundary are unchanged.
