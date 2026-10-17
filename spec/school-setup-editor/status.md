# School setup editor execution

Base: `be5b76d0ffc8a9851c4e8308540b1de12c5b4425`. Started 2026-10-17. Pre-existing dirty files: none.
Scope: all 18 specified use cases, local implementation and isolated runtime evidence; public deployment is out of scope.
No rules.md exists. Dependencies checked against the use-case map: all targets exist; graph is acyclic.

| UC | Status | Dependencies |
| --- | --- | --- |
| UC-1 | APPROVED | None |
| UC-2 | APPROVED | UC-1 |
| UC-3 | APPROVED | UC-1 |
| UC-4 | APPROVED | UC-1 |
| UC-5 | APPROVED | UC-1 |
| UC-6 | APPROVED | UC-3, UC-4 |
| UC-7 | APPROVED | UC-6 |
| UC-8 | APPROVED | UC-2, UC-5 |
| UC-9 | APPROVED | UC-2, UC-4 |
| UC-10 | APPROVED | UC-1 |
| UC-11 | APPROVED | UC-6 |
| UC-12 | APPROVED | UC-13 |
| UC-13 | APPROVED | None |
| UC-14 | APPROVED | UC-1 |
| UC-15 | APPROVED | UC-1 |
| UC-16 | APPROVED | UC-14 |
| UC-17 | APPROVED | UC-16, UC-12 |
| UC-18 | APPROVED | None |

## Evidence

- UC-1: source drafts retain the complete exact kernel document and cannot claim conversion before UC-15. Setup data shares the existing browser catalog's atomic write and stale-tab guard. Normal-profile authoring remains separate.
- Browser tooling: no enabled Playwright skill is listed. Use the repository's Java Playwright packaged-service harness with real Chrome, isolated storage, screenshots and a durable UC-specific ledger.
- UC-1 checks: packaged Maven build passes; existing Node demo checks 18/18 pass. First local browser attempt could not bind a socket under the sandbox; the authorized local runtime was rerun with escalation. First Chrome journey found the setup entry clickable before initialization on reload; fixed by disabling it until binding completes. Failed attempt retained at `../../output/playwright/school-setup-editor/UC-1/1792267791567/ledger.md`.

- UC-1 APPROVED 2026-10-17: final full journey PASS, no browser errors; [walkthrough](../../output/playwright/school-setup-editor/UC-1/1792267998863/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-1/1792267998863/submission.json), [checkpoint](checkpoints/UC-1.md). Earlier failed attempts preserved. No additional human gate.
- UC-2 IN_PROGRESS: next eligible UC; same base, current exact diff includes approved UC-1.

- UC-2 APPROVED 2026-10-17: [passing full walkthrough](../../output/playwright/school-setup-editor/UC-2/1792268382000/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-2/1792268382000/submission.json), [checkpoint](checkpoints/UC-2.md). UC-1 regression also passes.
- UC-3 IN_PROGRESS: subject catalog and contextual hard rules; same base plus approved UC-1/2.

- UC-3 APPROVED 2026-10-17: [passing walkthrough](../../output/playwright/school-setup-editor/UC-3/1792268646854/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-3/1792268646854/submission.json), [checkpoint](checkpoints/UC-3.md). UC-1/2 full regressions pass.
- UC-4 IN_PROGRESS: class details, curators and five contextual scheduling controls; exact base plus approved UC-1/2/3.

- UC-4 APPROVED 2026-10-17: [passing walkthrough](../../output/playwright/school-setup-editor/UC-4/1792269019967/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-4/1792269019967/submission.json), [checkpoint](checkpoints/UC-4.md). UC-6 plan handoff remains outstanding.
- UC-5 IN_PROGRESS: subject-specific teacher eligibility with independent qualification and derived allocations.

- UC-5 APPROVED 2026-10-17: [passing walkthrough](../../output/playwright/school-setup-editor/UC-5/1792269259966/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-5/1792269259966/submission.json), [checkpoint](checkpoints/UC-5.md). All affected UC-1–4 journeys pass.
- UC-6 IN_PROGRESS: real class-owned demand/allocations with stable ordinary occurrences and explicit eligibility grants.

- UC-6 browser finding: opening an empty Classes section could dereference a missing selection when both selected ID and plan context were absent. Fixed the explicit selected-record guard; all failed runs retained. Full current/affected journeys will repeat.

- UC-6 APPROVED 2026-10-17: [passing full walkthrough](../../output/playwright/school-setup-editor/UC-6/1792269906405/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-6/1792269906405/submission.json), [checkpoint](checkpoints/UC-6.md). All six current editor journeys pass. UC-4/5 class-plan handoffs closed; room/block settings remain UC-10/11.
- UC-7 IN_PROGRESS: copy only curriculum subjects/counts, keep teacher allocations independent.

- UC-7 APPROVED 2026-10-17: [passing walkthrough](../../output/playwright/school-setup-editor/UC-7/1792270052488/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-7/1792270052488/submission.json), [checkpoint](checkpoints/UC-7.md). UC-6 full regression also passes.
- UC-8 IN_PROGRESS: hard/soft teacher markings and explicit range operations.

- UC-8 APPROVED 2026-10-17: [passing walkthrough](../../output/playwright/school-setup-editor/UC-8/1792270315461/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-8/1792270315461/submission.json), [checkpoint](checkpoints/UC-8.md). UC-5 availability handoff closed. Block-window issue handoff remains UC-11.
- UC-9 IN_PROGRESS: class lunch reasons independent of other unavailability and common bell times.

- UC-9 APPROVED 2026-10-17: [passing walkthrough](../../output/playwright/school-setup-editor/UC-9/1792270506118/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-9/1792270506118/submission.json), [checkpoint](checkpoints/UC-9.md). Kernel lunch behavior and timetable rendering remain UC-12/13/17.
- UC-10 IN_PROGRESS: rooms, availability and intersections of hard placement policies.

- UC-10 APPROVED 2026-10-18: [passing walkthrough](../../output/playwright/school-setup-editor/UC-10/1792270779495/ledger.md), [source hashes](../../output/playwright/school-setup-editor/UC-10/1792270779495/submission.json), [checkpoint](checkpoints/UC-10.md). UC-6 row-room handoff closed.
- UC-11 IN_PROGRESS: exact block length/operator/weekly frequency and necessary common-teacher/common-room windows. Kernel enforcement remains separate UC-12/13.

- UC-11 APPROVED 2026-10-18: [full walkthrough](../../output/playwright/school-setup-editor/UC-11/1792271223810/ledger.md), [checkpoint](checkpoints/UC-11.md). Native input event target bug fixed; failed attempt retained. Block-window editor handoffs from UC-2/8/9/10 closed; published enforcement remains UC-12/13.
- UC-13 IN_PROGRESS before UC-12: new catalog 10 with explicit adjacency and maximal-run review; legacy catalogs remain supported.

- UC-12 APPROVED 2026-10-18: [published packaged CLI ledger](../../output/kernel/school-setup-editor/1792271608245/ledger.md), [checkpoint](checkpoints/UC-12.md). All 17 examples, legacy feasibility/scoring, valid generation+verification, deliberately forged invalid results, located metadata/assignment failures and bounded forced-undeclared failure pass. Kernel tests 107/107 pass.

- UC-13 APPROVED 2026-10-18: [published packaged CLI ledger](../../output/kernel/school-setup-editor/1792271608245/ledger.md), [checkpoint](checkpoints/UC-13.md). All 17 examples, legacy feasibility/scoring, valid generation+verification, deliberately forged invalid results, located metadata/assignment failures and bounded forced-undeclared failure pass. Kernel tests 107/107 pass.

- UC-14 APPROVED 2026-10-18: [real Chrome ledger](../../output/playwright/school-setup-editor/UC-14/1792271611799/ledger.md), [checkpoint](checkpoints/UC-14.md). Empty blockers, exact compiled revision/build, counts, stale response rejection and unavailable check pass. Node 34/34 pass.
- UC-15 IN_PROGRESS: explicit independent import/conversion/export and full authoring recovery.

- UC-15 APPROVED 2026-10-18: [full Chrome walkthrough](../../output/playwright/school-setup-editor/UC-15/1792271940691/ledger.md), [checkpoint](checkpoints/UC-15.md). Explicit adoption, independent buffers/drafts, by-value fidelity, recovery, duplicate refusal and protected ungrouped block edits pass. Node 38/38 pass. Initial invalid legacy fixture was correctly refused; failed attempt retained and fixture corrected.
- UC-16 IN_PROGRESS: atomic checked named school save with exact authoring/check snapshot.

- UC-16 APPROVED 2026-10-18: [real Chrome ledger](../../output/playwright/school-setup-editor/UC-16/1792272057983/ledger.md), [checkpoint](checkpoints/UC-16.md). Duplicate/long-name refusal, quota atomic failure, exact snapshot/check revision, no solve, consumed draft and reload successor pass.
- UC-17 IN_PROGRESS: pinned saved-school generation, independent candidate and retained timed grid/lunch.

- UC-17 APPROVED 2026-10-18: [empty school to named timetable and actual process restart](../../output/playwright/school-setup-editor/UC-17/1792273569484/ledger.md), [cancellation, origin selection, quota recovery, name refusal and discard](../../output/playwright/school-setup-editor/UC-17-faults/1792273382359/ledger.md), [checkpoint](checkpoints/UC-17.md). Real kernel generation and independent review preserve the exact saved definition, build, authoring grid and complete assignments; editing a declared double into class lunch is rejected and restoring it permits saving. Real service stop/restart marks the second Thorough run Interrupted without retry or data loss. Screenshots inspected for separate lesson cells, real times and named ordinary break/school/class lunch.
- UC-18 APPROVED 2026-10-18: [complete cleanup journey](../../output/playwright/school-setup-editor/UC-18/1792273429540/ledger.md), [checkpoint](checkpoints/UC-18.md). Cancel preserves exact raw values; injected removal failure rolls them back; confirmed cleanup removes malformed artifacts, refuses stale-tab resurrection and permits fresh authoring without a server mutation. Cleanup epoch also protects the initially absent catalog case (Node assertion).
- Runtime findings resolved: catalog-10 verified results have 20 hard counters (legacy catalogs retain 19); the web verifier now checks the correct versioned count. A fully explored tiny solve can legitimately finish SEARCH_EXHAUSTED before its time limit; the runner accepts this and TIME_LIMIT while retaining independent verification and rejecting unqualified terminations. Both fixes have focused unit coverage and passing real generation/manual-review evidence. Failed attempts remain in their original output directories.
- Final readiness refinement: authoritative 400/413/422 refusals are Blocked with located entity/interval links; transport/service errors remain Check failed. [Final UC-14 journey](../../output/playwright/school-setup-editor/UC-14/1792273603892/ledger.md) proves located refusal, exact subject navigation and stale/non-ready outcomes. Final affected UC-15/16/17 journeys also pass.
- Final regression 2026-10-18: all 17 browser journeys PASS; after the readiness refinement, all four affected journeys PASS again together with workspace unit tests 86/86. Kernel unit tests 107/107, Node checks 41/41, and [published CLI block-contract examples and negative boundaries](../../output/kernel/school-setup-editor/1792273351588/ledger.md) PASS. Responsive assertions cover 320/390/1440px; final UC-14 and UC-17 screenshots inspected. [Final regression ledger and exact source manifest](../../output/school-setup-editor/2026-10-18-final/ledger.md) retain commands, logs, source hashes, tracked patch and untracked source bytes.
- All destination handoffs are closed: class plans (UC-6), room/availability/block editors (UC-8/10/11), versioned kernel enforcement/review (UC-12/13), explicit conversion (UC-15), checked save (UC-16), generation and retained-grid rendering (UC-17). No blocker or open product decision remains. All 18 UCs are APPROVED. Changes remain uncommitted; no deployment performed. Validation uses isolated local demo data; unrelated PostgreSQL/normal-profile integration suites were not part of this feature's checks.

- Prototype alignment APPROVED 2026-10-18: revisited the actual planning Day pattern prototype after the user's visual correction. Restored the vertical left-edge entry at all widths, deliberate opening/closing slide, compact header, numbered quiet navigation, next-step footer, compact interval rows and week overview. Draft/JSON/recovery utilities and technical identifiers no longer dominate everyday editing; optional settings remain disclosed. Native modal isolation, reduced motion, storage failure/recovery and exact source identities are retained. [Review checkpoint](checkpoints/UI-prototype-alignment.md), [final visual walkthrough](../../output/playwright/school-setup-editor/UI-prototype/1792275494844/ledger.md), [exact UI diff and regression evidence](../../output/school-setup-editor/2026-10-18-ui-alignment/ledger.md).
- Prototype alignment verification: full 18-journey Chrome regression initially 17/18; exchange test synchronization corrected by waiting for the exact preceding explicit JSON transfer's autosave before taking its nonmutation baseline. All eight affected journeys then passed. Final selected-section hover and concise curator-help changes passed both affected journeys again. All 18 distinct journeys have passing applicable evidence; Node 41/41 and diff checks pass. Failed attempts are retained, including an initial module initialization error fixed before the first passing visual review. No deployment or commit.

- UI landing/prototype correction IN_PROGRESS 2026-10-18: the user rejected the previous prototype alignment; its automated presentation verdict is superseded. Fresh packaged-demo Chrome arrival and reload both showed the bundled timetable with setup closed before any click ([baseline ledger](../../output/playwright/school-setup-editor/UI-landing/1792276530288/ledger.md)); the reported unexpected setup landing is not reproduced in that baseline. Added an explicit closed-dialog style, an entry choice between selected-school copy and empty school, populated read-only source-grid preview with explicit conversion consent, and a closer Day pattern presentation. Existing source definitions, browser-local draft/save lifecycle and explicit generation remain authoritative. Browser validation is in progress; prior failed attempts are retained.
- UI landing/prototype correction APPROVED by automated walkthrough 2026-10-18: [checkpoint](checkpoints/UI-timetable-landing.md), [final arrival/copy/new/reload ledger](../../output/playwright/school-setup-editor/UI-landing/1792277403078/ledger.md), [final prototype/rollout/responsive ledger](../../output/playwright/school-setup-editor/UI-prototype/1792277324140/ledger.md), [exact correction submission and regression evidence](../../output/school-setup-editor/2026-10-18-timetable-landing/ledger.md). Full 19-journey Chrome regression passes; after final interval-name/active-day refinements, all four affected journeys pass again in the recorded runs. JavaScript checks 41/41 and diff checks pass. Fresh arrival, reload with stored work, and reload while setup was open all show the timetable matrix. Setup's explicit copy/new choices and closer Day pattern layout supersede the previous rejected presentation. Baseline root cause remains unestablished; no deployment or commit.

## 2026-10-18 — clean startup and retired examples: APPROVED (automated)

User correction: every arrival/reload starts on the bundled precomputed timetable, without applying retained draft selections. Explicit navigation/Resume retains saved work; left setup entry opens the existing prototype overlay. Removed old public examples, their conversion scripts and automatic legacy-definition promotion; preserved regression rules with fictional test-only identities. The timetable specification now records this startup contract.

[Correction checkpoint](checkpoints/UI-clean-start.md) · [Exact execution/source evidence](../../output/school-setup-editor/2026-10-18-clean-start/ledger.md). Eight affected Chrome journeys, 41 JS, 40 kernel checks and 39 CLI checks pass; packaged sample values/bytes match authoritative resources. This supersedes the previous landing checkpoint's startup-restoration boundary. Existing unrelated work remains uncommitted. The public browser tabs still show an older deployment; no deployment performed.

## 2026-10-18 — demo local HTTP and startup artifact loading: APPROVED (automated)

The demo profile now supports direct localhost/127.0.0.1 HTTP with default loopback-only configuration and HTTP-compatible cookies. Public-host/proxy deployments retain HTTPS checks. Startup reads prebundled artifacts directly with zero kernel verification processes; checks move to explicit maintenance. User correction supersedes older startup-verification rules. Explicit edits/generation still validate on demand.

[Checkpoint](checkpoints/UI-local-http.md) · [Execution/source ledger](../../output/school-setup-editor/2026-10-18-local-http/ledger.md): 19 unit checks, local HTTP Chrome journey, existing variant/navigation Chrome journey and five offline artifact checks pass. No commit, deployment or operator-data cleanup performed.
