# UC-3 local container smoke check

Image: `sha256:921f565d163001626a3a85011192886367e210d844604a37e9fdc74c174c68e9`

PASS: read-only non-root image becomes ready after three real packaged-sample verifications; health creates no session

PASS: HTTP and untrusted scheme/host/forwarded requests disclose no workspace and create no session

PASS: all three startup verifications ran under the container verification heap ceiling

PASS: direct SIGTERM stops the disposable web process cleanly

Local packaging PASS only. No public Railway, capacity, spend, queue or reuse qualification.
Container removed; no volume or database was used. Local image retained for review.
