# UC-2 deployed expiry walkthrough

2026-10-16T12:34:42.837940Z
Application: https://demo-smoke-production.up.railway.app; real packaged kernel, Chrome with normal TLS validation, two isolated disposable contexts.
Test-only effective clock configuration: 15-second idle / 75-second absolute; defaults restored separately by operator.

PASS | UC-2 G5: real private kernel run with browser polling; exact browser version saved | [screenshot](01-active-run.png) / [snapshot](01-active-run.txt)

PASS | UC-2 idle owner holds a private unfinished repair; active owner's input remains private | [screenshot](02-idle-draft.png) / [snapshot](02-idle-draft.txt)

PASS | UC-2 2b/G5: 15-second idle expiry loses unfinished repair while the polling owner remains active | [screenshot](03-idle-ended.png) / [snapshot](03-idle-ended.txt)

PASS | UC-2 G5: private run/result is lost at absolute expiry despite polling; no proposal or Current recovered; browser version remains exact | [screenshot](04-absolute-ended.png) / [snapshot](04-absolute-ended.txt)

PASS | UC-2 2b: explicit Start again creates EMPTY without replay, recovery or acceptance; expired run is indistinguishable from a random ID | [screenshot](05-fresh-empty.png) / [snapshot](05-fresh-empty.txt)

PASS. Browser contexts close, temporary input removed by JUnit. Disposable fresh EMPTY workspace remains until expiry/operator reset. No accepted work was recovered or silently accepted.
