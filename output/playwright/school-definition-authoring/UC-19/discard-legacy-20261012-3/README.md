# UC-19 attempt3 — harness interrupted, no approval

Same final packaged source as attempt4. Reusing attempt2 browser's unsaved form triggered the native beforeunload dialog on the first reload; CLI returned modal state without a journey result. Isolated durable workspace remained legacy v233, no discard sent. Raw interrupted script/modal state retained. Repeat against a fresh Chrome context; no production change required.
