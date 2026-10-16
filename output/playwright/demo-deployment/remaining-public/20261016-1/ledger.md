# Remaining use cases: public Chrome walkthrough

2026-10-16T18:24:52.468390Z; https://demo-smoke-production.up.railway.app; normal TLS validation; two disposable private owners. Exact authorized production archive d292a016f6edb85874eb98597643e555159829e2ec3f10a73cc082d46f054dd7, deployment a96c3c89-1c5e-4a13-9093-fcfb40f29368.

- PASS **UC-8 initial: real Quick/custom definition, explicit acknowledgement, exact atomic acceptance, stale refusal and independent reused acceptance**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-initial-accepted.png), [rendered snapshot](01-initial-accepted.txt).

- PASS **UC-7: conflict blocking, individual/bulk pin cancel/undo, exact reload, availability undo and confirmed discard preserve complete Current and foreign owner**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-repair-undo.png), [rendered snapshot](02-repair-undo.txt).

- PASS **UC-8/10: real protected repair explicitly accepted with exact pair/ancestry/attempt provenance; draft and unaccepted proposal exports contain only prior Current**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-repair-accepted.png), [rendered snapshot](03-repair-accepted.txt).

- PASS **UC-9/10: real verified manual room publication preserves exact definition/assignments/lock origins; teacher refusal, reload and draft-excluding export preserve Current**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](04-manual-published.png), [rendered snapshot](04-manual-published.txt).

- FAIL **FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 30000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 30000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-5011661622742901574/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-5011661622742901574/package/lib/coreBundle.js:19135:44)
}**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertion failed as recorded above. [Screenshot](failure.png), [rendered snapshot](failure.txt).

Overall: FAIL: com.microsoft.playwright.TimeoutError: Error {
  message='Timeout 30000ms exceeded.
  name='TimeoutError
  stack='TimeoutError: Timeout 30000ms exceeded.
    at _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-5011661622742901574/package/lib/coreBundle.js:12360:32)
    at DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-5011661622742901574/package/lib/coreBundle.js:19135:44)
}
Public actor checks only; local process fault/allowance boundary evidence remains separately recorded. Browser contexts close; same-image provider restart must remove disposable sessions. No account/resource/billing setting mutation. This is a Free-demo verification, not Hobby capacity/spend qualification.
