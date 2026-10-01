# Use-Case Status: Timetable Matrix Lenses

## Current

- Use case: UC-1
- Status: READY_FOR_CONVERGENCE (revision 1)
- Next eligible: none until UC-1 is APPROVED

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | READY_FOR_CONVERGENCE | none | HEAD at convergence (rev. 1; first submission e840460) | `convergence/UC-1.md` (REJECT at e840460: C-1, G-1) |
| UC-2 | NOT_STARTED | UC-1 | - | - |
| UC-3 | NOT_STARTED | UC-1 (extends UC-1 at 5a) | - | - |
| UC-4 | NOT_STARTED | UC-1 (extends UC-1 at 1a) | - | - |

## UC-1 Evidence

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

none approved. Two fixes outside the lens code are recorded in `checkpoints/UC-1.md` Notes: the draft model is now
the filter source, and the manual editor is bound once per form. The interpretation D-1 (the lens follows a changed
teacher investigation) is awaiting the user's confirmation in the walkthrough.
