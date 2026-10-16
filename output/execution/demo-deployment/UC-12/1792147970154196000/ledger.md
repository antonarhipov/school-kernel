# UC-12 local container smoke check

Image: `sha256:6e5dc68824ac3a6f8f58b65cde70e86fedd144724a43e94636b8116fe13e5816`

PASS: read-only non-root image becomes ready after three real packaged-sample verifications; health creates no session

PASS: HTTP and untrusted scheme/host/forwarded requests disclose no workspace and create no session

PASS: all three startup verifications ran under the container verification heap ceiling

PASS: direct SIGTERM stops the disposable web process cleanly

Local packaging PASS only. No public Railway, capacity, spend, queue or reuse qualification.
Container removed; no volume or database was used. Local image retained for review.

Resource settings and post-startup idle observation: [resources.json](resources.json). Idle observation is not startup peak or workload qualification.

A production-only upload context was prepared separately; [upload-context-manifest.json](upload-context-manifest.json) records all 150 files, bytes and SHA-256 values. This directory has not been uploaded. Railway CLI browserless authentication is pending. No provider deployment, plan upgrade or billing change has occurred.
