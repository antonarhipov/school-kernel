# UC-4 browser-version walkthrough

Date: 2026-10-16T13:44:32.581192Z
Application: https://localhost:60755
Browser: real Chrome, Playwright; two isolated owners and one same-owner tab. Disposable data only.
Public TLS validates normally; local TLS is disposable. Build/source identity is in the execution manifest.

PASS | main 1-5/G1/G2: exact CRLF/Unicode file saved in a five-field file-origin envelope; unfinished object makes no server change; second tab list refreshes | [screenshot](01-exact-save.png) / [snapshot](01-exact-save.txt)

PASS | main 6/G3/G4: reload and selection preserve exact CRLF bytes; browser-local download matches the original file | [screenshot](02-reload-download.png) / [snapshot](02-reload-download.txt)

PASS | 1b/4b/6a: declined selection, file opening, revert and named replacement preserve exact open and saved text | [screenshot](03-declines.png) / [snapshot](03-declines.txt)

PASS | main 4-6/G2: confirmed named replacement stores exact edited text, saved-source derivation and explicit save time | [screenshot](04-replace.png) / [snapshot](04-replace.txt)

PASS | 4a/G1/G4: first syntax line/column, reserved/invalid labels and non-object roots refused; malformed text still downloads exactly | [screenshot](05-validation.png) / [snapshot](05-validation.txt)

PASS | FAIL: com.microsoft.playwright.PlaywrightException: Error { | [screenshot](failure.png) / [snapshot](failure.txt)

Overall: FAIL: com.microsoft.playwright.PlaywrightException: Error {
  message='TypeError: Illegal invocation
    at UtilityScript.evaluate (<anonymous>:318:18)
    at UtilityScript.<anonymous> (<anonymous>:1:44)
  name='Error
  stack='Error: TypeError: Illegal invocation
    at UtilityScript.evaluate (<anonymous>:318:18)
    at UtilityScript.<anonymous> (<anonymous>:1:44)
    at CRExecutionContext.evaluateWithArguments (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1488993611445715425/package/lib/coreBundle.js:35610:17)
    at async LongStandingScope._race (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1488993611445715425/package/lib/coreBundle.js:3487:18)
    at async evaluateExpression (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1488993611445715425/package/lib/coreBundle.js:19519:12)
    at async _Frame._evaluateExpression (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1488993611445715425/package/lib/coreBundle.js:23705:24)
    at async _Frame.evaluateExpression (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1488993611445715425/package/lib/coreBundle.js:23701:16)
    at async _FrameDispatcher.evaluateExpression (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1488993611445715425/package/lib/coreBundle.js:54732:41)
    at async _ProgressController.run (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1488993611445715425/package/lib/coreBundle.js:12374:27)
    at async DispatcherConnection.dispatch (/private/var/folders/c_/5_n98lpd65j__pbwgzctxx_80000gn/T/playwright-java-1488993611445715425/package/lib/coreBundle.js:19135:27)
}

Quota/SecurityError/file-read faults are explicitly injected in browser APIs. No server failure is claimed.
Contexts closed; local server stopped. Public sessions retained for expiry or explicit cleanup. UC-5 consumes saved-use tracking; model checks do not claim that future actor journey.
