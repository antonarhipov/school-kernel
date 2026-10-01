# Use-Case Status: Timetable Matrix Lenses

## Current

- Use case: UC-2
- Status: READY_FOR_CONVERGENCE (revision 1)
- Next eligible: none until UC-2 is APPROVED (then UC-3, UC-4)

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | cba775d (rev. 1; first submission e840460) | `convergence/UC-1.md` (APPROVE WITH NOTES: K-2, K-3; walkthrough confirmed 2026-10-01) |
| UC-2 | READY_FOR_CONVERGENCE | UC-1 | HEAD at convergence (rev. 1; first submission 013c12a) | `convergence/UC-2.md` (REJECT at 013c12a: G-1, G-2) |
| UC-3 | NOT_STARTED | UC-1 (extends UC-1 at 5a) | - | - |
| UC-4 | NOT_STARTED | UC-1 (extends UC-1 at 1a) | - | - |

## UC-2 Evidence

- Revision 1 started from f54ec03 with no dirty files. It resolves G-1 and G-2 with three new tests
  (`MatrixLensBrowserIT.java:492`, `:525`, `:548`). It implements the user's D-1 decision: scroll restore only in
  the recorded range (`app.js:979`, `:984`). `MatrixLensBrowserIT`: 14 run, 0 failures, 0 errors. A precedence
  mutant fails as intended. Full `./mvnw -q -pl timetable-workspace -am verify`: 120 integration tests, 2 failures,
  9 errors. The failing set is identical to the 064bf64 baseline. `AcceptedInspectionBrowserIT` 11/11,
  `ScaleTimingBrowserIT` 3/3, `WorkspaceManualDraftIT` 11/11.

- Convergence: REJECT at 013c12a. Findings to resolve: G-1 (no committed evidence that Reset and `#clear-filters`
  return a "Show only matches" teacher lens and subject filter mode to highlight mode, per inspection-ux rule 7),
  G-2 (the state-rule-2 precedence assertion at `MatrixLensBrowserIT.java:407` survives a mutation where the recorded
  position wins over a represented selection). For the user: D-1 (after a range change inside a lens, the Week scroll
  offset is reused on Day class rows). See `convergence/UC-2.md`.

- Started from: 002cc2f
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence
- Changed files: `inspection-state.js`, `app.js`, and `messages.js` (workspace static); `MatrixLensBrowserIT.java`;
  `checkpoints/UC-2.md`
- Commands and results:
  - `MatrixLensBrowserIT`: 11 run, 0 failures, 0 errors. A mutation check (with `settleMatrixScroll` made a no-op)
    fails all three scroll tests.
  - Full `./mvnw -q -pl timetable-workspace -am verify`: 117 integration tests, 2 failures, 9 errors. The failing set
    is identical to the 064bf64 baseline. `AcceptedInspectionBrowserIT` passes 11/11, `ScaleTimingBrowserIT` 3/3, and
    `WorkspaceManualDraftIT` 11/11.

| Contract element | Evidence |
|---|---|
| UC-2 main steps 1-5 | `MatrixLensBrowserIT.java:345`, `:384`; `inspection-state.js:30`; `app.js:965`, `:983` |
| UC-2 extensions 1a, 1b, 3a | `MatrixLensBrowserIT.java:419`, `:357-361` |
| UC-2 extension 1c | `MatrixLensBrowserIT.java:455`; `app.js:1896`, `:1903` |
| UC-2 G1, G2 | `MatrixLensBrowserIT.java:403`, `:377` |
| RULE-1, 2, 3, 4, 6, 7, 10 | See `checkpoints/UC-2.md` |

## UC-1 Evidence

- Approved: the user confirmed the walkthrough on 2026-10-01 and accepted D-1 as intended. Cosmetic notes K-2 and K-3
  remain.
- Convergence: PENDING WALKTHROUGH at cba775d. C-1, G-1, and K-1 resolved. Open for the user: walkthrough in
  `convergence/UC-1.md`, including confirmation of D-1. Non-blocking: K-2, K-3.

- Convergence: REJECT at e840460. Findings to resolve: C-1 (no "Show week" in the Proposal-mode comparison
  inspector), G-1 (no executable committed evidence for the SOLVING lens, the Day room-lens tile and header, or
  Proposal review-target navigation in and out of a lens). Non-blocking: D-1 and K-1. See `convergence/UC-1.md`.

- Revision 1 (resolving C-1, G-1, K-1; D-1 recorded for the walkthrough). It started from 69ac6e4, with no dirty
  files beforehand. Changed: `app.js` (Show week in the Proposal comparison inspector, and the manual editor bound
  once per form), `MatrixLensBrowserIT.java` (the Solving lens, the Day room lens, and Proposal Show week plus
  review-target tests), and `checkpoints/UC-1.md`.
  - `MatrixLensBrowserIT`: 7 run, 0 failures, 0 errors.
  - Full `./mvnw -q -pl timetable-workspace -am verify`: 113 integration tests, 2 failures, 9 errors. The failing set
    is identical to baseline 064bf64.

- Started from: 064bf64
- Pre-existing dirty files: none (the spec artifacts were committed in 064bf64 before implementation began)
- Implementation submission: HEAD at convergence
- Changed files: `spec/timetable-matrix-lenses/{rules,status}.md`, `checkpoints/UC-1.md`; workspace static
  `app.js`, `inspection-state.js`, `week-renderer.js`, `day-renderer.js`, `focused-renderer.js`, `messages.js`,
  `styles.css`; tests `MatrixLensBrowserIT.java` (new), `AcceptedInspectionBrowserIT.java`,
  `ProposalReviewBrowserIT.java`, `RepairCompletionBrowserIT.java`, `RepairDraftBrowserIT.java`,
  `RepairRunBrowserIT.java`, `ScaleTimingBrowserIT.java`, `Workbench.java`
- Commands and results:
  - `./mvnw -q -pl timetable-workspace -am '-Dit.test=MatrixLensBrowserIT' -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false verify`:
    5 run, 0 failures, 0 errors
  - `./mvnw -q -pl timetable-workspace -am verify`: 111 integration tests, 2 failures, 9 errors, 0 skipped. This is
    the same failing set as the 064bf64 baseline (106 run, 2 failures, 9 errors), all pre-existing repair pin and
    inspector-geometry failures. The list is in `checkpoints/UC-1.md`.

| Contract element | Evidence |
|---|---|
| UC-1 main steps 1-6 | `MatrixLensBrowserIT.java:30`, `:122`; `app.js:911`, `:954`, `:964` |
| UC-1 extension 1a | Unchanged narrow path (UC-4 scope); `AcceptedInspectionBrowserIT.java:68` |
| UC-1 extension 4a | `MatrixLensBrowserIT.java:162` |
| UC-1 extension 4b | `MatrixLensBrowserIT.java:192` |
| UC-1 extension 4c | `MatrixLensBrowserIT.java:101` |
| UC-1 extension 5a | `MatrixLensBrowserIT.java:128` (entry only; UC-3 scope) |
| UC-1 G1-G4 | `MatrixLensBrowserIT.java:45`, `:59`, `:73`, `:106` |
| UC-1 minimal guarantee | `MatrixLensBrowserIT.java:220` |
| RULE-1..RULE-10 | See `checkpoints/UC-1.md` rule evidence |

## Blockers

none

## Deviations

- UC-2, D-1 (user decision, 2026-10-01): the scroll position recorded when the lens was entered is restored only if
  the range (Week/Day) is still the one it was recorded in. Otherwise class rows start at the top, or at a represented
  selection. Spec reference: the presentation state model, rule 2.

none approved. Two fixes outside the lens code are recorded in `checkpoints/UC-1.md` Notes: the draft model is now
the filter source, and the manual editor is bound once per form. The interpretation D-1 (the lens follows a changed
teacher investigation) was confirmed by the user on 2026-10-01.
