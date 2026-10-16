# UC-1 production HTTP admission evidence

Date: 2026-10-16T22:16:30.306550Z
Application: https://localhost:58443
Isolated packaged service, real TLS sockets and disposable browser sessions.

- PASS: two incomplete bodies occupy one owner's slots; a third is rejected with 429/Retry-After before decoding.
- PASS: two other incomplete bodies fill the global ceiling; a third owner is rejected with INPUT_BUSY.
- PASS: safe workspace reads remain usable; exact initial workspace is unchanged.
- PASS: closing incomplete clients releases leases; subsequent input reaches ordinary validation (422).

No kernel launches beyond startup were requested. All sockets, contexts and the disposable server close.
Overall: PASS
