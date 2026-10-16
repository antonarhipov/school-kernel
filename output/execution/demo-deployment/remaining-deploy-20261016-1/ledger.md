# Remaining demo deployment attempt

2026-10-16. User requested deployment and authorized it in general. No deployment/upload ran.

- PASS: all 162 source/stage/archive files still match the reviewed production manifest and archive SHA-256 `d292a016f6edb85874eb98597643e555159829e2ec3f10a73cc082d46f054dd7`. [Payload preflight](payload-preflight.json).
- PASS: read-only current provider state confirms existing `school-kernel-free-smoke / demo-smoke`, one EU replica, unchanged one-CPU/500,000,000-byte limits and sleeping enabled; latest deployment remains `a3ebd477-0aa8-46ee-a8b5-38d33d35b78a`. [State](provider-before.json).
- PASS: fresh isolated ingress preflight confirms exactly one project/environment service and generated domain, no custom domains or TCP proxy. [Boundary](boundary-before.json).
- BLOCKED: automatic approval review rejected the exact upload/deployment before execution. General authorization was insufficient for private source egress; it requires the user to explicitly authorize this payload and destination. [Exact review and concrete request](deployment-approval-block.json). No indirect retry or workaround attempted.

After explicit payload/destination authorization, upload only the pinned stage to this service, wait for readiness, verify public visitor flows/current static hashes/safe events, clean disposable sessions by same-image restart, and recheck topology. No plan/billing upgrade is proposed. Hobby capacity/spend qualification remains separate and unperformed.
