# Faster local demo startup

The demo currently logs about 3.303 seconds to start on the operator's machine. Provide an optional GraalVM native
launch using the installed Java 25 toolchain, with a measured comparison against the existing JVM launch. Preserve
the current demo journeys and keep the scheduling kernel on the JVM.

2026-10-18 follow-up: deploy the native web application to the existing Railway demo service. Preserve its Free plan,
resource/ingress settings and separate JVM kernel; verify the deployed runtime and visitor flow.
