# UC-10 recorded export journey

2026-10-16T17:00:03.485260Z; real Chrome/local TLS and packaged kernel; disposable owner.

- PASS **main 1-4/1a/2a/G1-G3: no-Current export refused; sample and repair-draft exports contain exactly three accepted documents, exclude intent and start zero processes**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](01-sample-export.png), [rendered snapshot](01-sample-export.txt).

- PASS **2a/G1/G4: generated proposal excluded; after own explicit acceptance, exported complete repair pair preserves parent lineage and provenance exactly without kernel work**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](02-repair-export.png), [rendered snapshot](02-repair-export.txt).

- PASS **2a/G1/G2: manual edits remain private draft; export contains only the exact accepted repaired baseline and performs no verification or solve**. Expected: specified outcome with exact value/privacy/non-mutation assertions above. Observed: assertions passed. [Screenshot](03-manual-excluded.png), [rendered snapshot](03-manual-excluded.txt).

Overall: PASS

UC-11 owns fresh-owner verified re-import; archives retained here for round-trip verification. Deployment deferred; contexts/service cleaned up.
