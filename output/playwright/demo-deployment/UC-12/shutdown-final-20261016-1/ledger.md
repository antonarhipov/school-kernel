# UC-12 local shutdown actor evidence

2026-10-16T17:38:01.845448Z; packaged local TLS, actual Chrome owner and startup sample checks. The visitor verifier is explicitly replaced by a real blocking sleep process; no fabricated success or capacity evidence. SIGTERM is a real infrastructure action, not claimed as a browser click.

- PASS **UC-12 6a/G6: real visitor definition verification is deliberately held in an actual child process; exact prior Current remains unchanged before SIGTERM**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-in-flight.png), [rendered snapshot](01-in-flight.txt).

- PASS **Actual SIGTERM / one-second web grace:** the application exits without forced kill, verifier child is reaped, private directory is removed, and one CANCELLED verification completion is logged. Allowance/queued-caller settlement is independently asserted with two actual processes in DemoVerificationLifecycleTest.

Overall: PASS

No deployment/provider change; service, browser and test processes cleaned up. The ended in-memory workspace is not restored or accepted.
