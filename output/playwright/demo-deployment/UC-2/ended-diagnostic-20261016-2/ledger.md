# UC-2 expired-cookie recovery diagnostic

PASS: explicit Start again, 20 actual-browser reads and reload remain usable. Full retention/privacy scopes, definition groups, no-store, CSP, HSTS and nosniff are checked. Stale clear returns 412 with the complete accepted snapshot unchanged.
The separate Playwright APIRequestContext retains an old cookie after rotation; it is not the browser actor. See network.txt. Browser closes.

Correction after inspecting network.txt: all 20 API checks also returned 200. The earlier stale-cookie attribution was premature; the failed attempts sampled an in-flight transition. Explicit awaited response polling fixes the harness timing. No application session fix was needed.
