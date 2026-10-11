# UC-15 walkthrough attempt 2 — FAIL

2026-10-12, submission `528d082`, real Chromium localhost:18093, stable production JAR /tmp/uc15-workspace.jar, disposable authoring_loop_uc15_r2. Correctly imported the verified accepted definition/result and opened authoring with exact source. Main successfully saved same-ID mon1 weekday/order/clock edits, explicit Alex availability[], and Authored Blair name. It then used a nonexistent class-field selector `#cohort-displayName` instead of the observed Class name control (`#class-displayName`) and timed out before changing the class.

[Entry](entry.txt), [main error](main.txt), [exact state/snapshot](failure.txt), [screenshot](failure.png) retained. No production failure is inferred. Fix the harness to use the observed Class name role and repeat the full journey on fresh data. Remaining issue/filter/link/current/responsive scenarios not reached; no approval. Operator data untouched. Database/server retained.
