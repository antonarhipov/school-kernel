# Convergence: UC-2 - Trace subject teaching and teacher load

## Summary

- Submission: `checkpoints/UC-2.md` committed with the revision at `5df107e` (original submission `90fe265`; prior rejection on `5d426b5`).
- Verdict: APPROVE WITH NOTES
- Findings: 0 critical, 0 gap, 0 protocol; one non-blocking diagnostic note for an intermittent unrelated Chrome DevTools timeout.
- Suite: `./mvnw -q -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify`: initial independent run 17/0 failures/1 error/0 skipped (repair-draft CDP timeout); targeted repair retry 1/0/0/0; full independent retry 17/0/0/0, plus 26 unit tests.
- Working tree impact from verification: verifier report and status update only; test-run `.output.txt` removed. Pre-existing `.idea/encodings.xml` and untracked `spec/timetable-ux-polish` work preserved.

## Protocol Gate

1. UC-2 alone is `READY_FOR_CONVERGENCE` at `status.md:5-15`: pass.
2. The checkpoint, production fix, real-browser test, and status were committed together at `5df107e`; the submitted `HEAD at convergence` is identifiable and immutable: **P-1 closed**.
3. UC-1 is `APPROVED` at `status.md:13`; UC-2 extends UC-1 at 4a (`spec.md:193-196`): pass.
4. UC-3 is `APPROVED`, not in progress or awaiting convergence (`status.md:15`): pass.
5. Checkpoint `checkpoints/UC-2.md:12-41` covers all scenario branches, guarantees, postconditions, rules, relationships, commands, actual changed files, and approved-UC regression; status preserves prior evidence: pass.
6. `git show 5df107e` changes only `app.js`, `WorkspaceBrowserIT.java`, UC-2 checkpoint, and status. The previous verifier-owned convergence report, `.idea`, and untracked UX-polish work remain outside the implementation commit: pass.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | UC-1 accepted Week postcondition, Math/Alex highlight and ribbon | Packaged Chrome/PostgreSQL journey | `WorkspaceBrowserIT.java:324-361` loads the accepted snapshot and asserts one Math, two Alex, one dual match and assigned/unavailable/available states; independent full retry passed. |
| Administrator | Subject-only, intersected filters, Day, clear one | One represented lesson and one subject/teacher/dual match | `WorkspaceBrowserIT.java:364-385` asserts teacher count is one, then teacher count returns to two only after the subject is cleared; `app.js:744-753` counts unique IDs in `filteredAssignments()`: **C-1 closed**. |
| Administrator | UC-2 scale Week/Day, explicit/omitted availability, zero paths | Kernel-verified normative data with independent ID and ribbon expectations | `WorkspaceBrowserIT.java:418-587,1291-1326` verifies definition/result revisions through `KernelVerifier`, compares all displayed unique IDs and totals to accepted assignments, checks every teacher-16 explicit ribbon slot and every teacher-17 omitted-availability Day/Week slot; both independent full and focused runs passed: **G-2 closed**. |
| Administrator | Empty Tuesday intersection, clear identities, reset | No matches or mutation | `WorkspaceBrowserIT.java:387-413,513-528` checks zeroes, active filters, and exact before/after stored document. |
| Administrator | Approved UC-1/UC-3 and older workspace browser journeys | Full regression | Initial run encountered a CDP timeout at repair-draft discard (`WorkspaceBrowserIT.java:895`); targeted retry passed 1/0/0/0 and complete retry passed 17/0/0/0. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-2 step 1 | Exact subject selected | Browser chooses `math` at `WorkspaceBrowserIT.java:339-342` | STRONG | yes |
| UC-2 step 2 | Subject cue/count, nonmatches remain | `WorkspaceBrowserIT.java:343-348`; `app.js:761-770` | STRONG | yes |
| UC-2 step 3 | Exact teacher selected | Browser chooses `teacher-alex` at `WorkspaceBrowserIT.java:350-353` | STRONG | yes |
| UC-2 step 4 | Distinct dual cues and teacher/dual counts | `WorkspaceBrowserIT.java:354-362`; `app.js:744-770` | STRONG | yes |
| UC-2 step 5 | All ribbon states, Day retains selection | `WorkspaceBrowserIT.java:357-360,376-379`; `app.js:778-789` | STRONG | yes |
| UC-2 step 6 | Explicit single/dual match modes | `WorkspaceBrowserIT.java:364,372`; `inspection-state.js:37-45` | STRONG | yes |
| UC-2 step 7 | Intersect, narrowed label and totals for represented range | `app.js:744-753` counts after `filteredAssignments()`; `WorkspaceBrowserIT.java:372-379,488-509,531-559` checks one teacher match in the small intersection and independently checks 1,000-assignment Week/Day represented ID sets and exact visible totals | STRONG | yes |
| UC-2 step 8 | Clear one identity/mode | `WorkspaceBrowserIT.java:381-385,402-408`; `inspection-state.js:27-49` | STRONG | yes |
| UC-2 step 9 | Other investigation survives | Teacher selection checked at `WorkspaceBrowserIT.java:381-385` | STRONG | yes |
| UC-2 extension 2a | Zero subject, surrounding timetable | Tuesday zero subject at `WorkspaceBrowserIT.java:387-400`; explicit-only filtering in `app.js:710-715` | STRONG | yes |
| UC-2 extension 4a | Zero teacher, authoritative ribbon | `WorkspaceBrowserIT.java:402-405,513-528`; `app.js:778-789` | STRONG | yes |
| UC-2 extension 6a | Empty intersection, active modes, reset | `WorkspaceBrowserIT.java:392-410`; `app.js:405-409`; `messages.js:221-227` | STRONG | yes |
| UC-2 extension 7a | Filtered selection clears with announcement | `WorkspaceBrowserIT.java:364-370`; `app.js:602-609` | STRONG | yes |
| UC-2 G1 | Stable subject/teacher IDs | `accepted-model.js:7-28`; `app.js:711-715,761-765` | STRONG | yes |
| UC-2 G2 | Positive distinct cues, no default dimming | `app.js:761-770`; `styles.css:99-105`; browser nonmatches at `WorkspaceBrowserIT.java:343-348` | STRONG | yes |
| UC-2 G3 | Unique counts after explicit filters, not viewport | `app.js:744-753` counts unique represented lesson IDs; `WorkspaceBrowserIT.java:531-559` compares calculated ID sets to rendered tiles and totals over subject-only, intersected, teacher-only, empty, and reset states at 1,000 assignments | STRONG | yes |
| UC-2 G4 | Availability from assignments and definition | `app.js:778-789`; `WorkspaceBrowserIT.java:562-587` independently derives every ribbon state from accepted assignment and explicit/omitted availability; `WorkspaceBrowserIT.java:488-528` checks explicit teacher-16 Week/Day and omitted teacher-17 Week/Day with zero assignments | STRONG | yes |
| UC-2 G5 | Identities retained on range switch, absent from storage | `inspection-state.js:6-16,112-117`; `WorkspaceBrowserIT.java:372-385` | STRONG | yes |
| UC-2 G6 | Distinct accessible/visible lesson cues | `app.js:761-775`; `styles.css:99-105` | STRONG | yes |
| UC-2 G7 | Visible pointer controls and filter reset | `app.js:385-410,557-578`; Chrome clicks at `WorkspaceBrowserIT.java:339-410,480-528` | STRONG | yes |
| UC-2 G8 | Full real-browser paths on validation-scale snapshot without mutation | `WorkspaceBrowserIT.java:418-528,1291-1326` checks cardinalities, long names, by-value identities, populated/empty entities, kernel-verified complete accepted pair, every required UC-2 path, unchanged PostgreSQL document; focused and complete browser retry pass | STRONG | yes |
| UC-2 success postcondition | Account for represented subject/teacher lessons and availability | `WorkspaceBrowserIT.java:488-528,531-587` independently checks rendered IDs/counts and all explicit/omitted teacher ribbon period classifications | STRONG | yes |
| UC-2 minimal guarantee | Empty result leaves source state unchanged, visible zeroes | `WorkspaceBrowserIT.java:392-413,513-528`; original model remains `accepted-model.js:3-28` | STRONG | yes |
| UC-2 Requires UC-1 | Consume accepted whole-school snapshot | `WorkspaceBrowserIT.java:324-337,480-486` begins at real UC-1 success screen; `app.js:376-410` uses same model | STRONG | yes |
| UC-2 Extends UC-1 at 4a | Launch from whole-school inspection, retain base | `WorkspaceBrowserIT.java:339-362,480-509`; full UC-1 browser regression passed | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Native modules, shared immutable model and renderers | `accepted-model.js:3-28`, `inspection-state.js:4-100`, `app.js:376-410`; no dependency delta in UC-2 diff | PASS |
| RULE-2 | Complete single snapshot/index, no read-model endpoint | `accepted-model.js:3-28`, `app.js:376-410,778-789`; submitted diff has no backend code | PASS |
| RULE-4 | One state authority and valid range/filter transitions | `inspection-state.js:27-98`, `app.js:594-609` | PASS |
| RULE-6 | Stable lesson ID through range/selection | `accepted-model.js:13`, `app.js:629-640`; browser `WorkspaceBrowserIT.java:355-374` | PASS |
| RULE-7 | Counts MUST derive from unique IDs in represented result after intersections | `app.js:744-753` filters before counting, with a unique-ID set per criterion; `WorkspaceBrowserIT.java:531-559` calculates and compares expected accepted IDs and totals independently in the real browser | PASS |
| RULE-8 | Assignment/authoritative availability, omitted means all periods | `app.js:778-789`; `WorkspaceBrowserIT.java:562-587` checks all 60 explicit Week slots and all 60 omitted Week slots, Day subsets, assigned/available/unavailable precedence, and zero-accepted-assignment ribbon | PASS |
| RULE-9 | Catalog text, escaped names | `messages.js:193-227`, `app.js:385-410,778-789`; no introduced raw copy in current revision | PASS |
| RULE-10 | Native pointer controls and non-color cues | `app.js:394-410`, `styles.css:99-105`, Chrome interactions at `WorkspaceBrowserIT.java:330-398` | PASS |
| RULE-11 | No backend or durable mutation | UC-2 submitted diff contains no Java production/config/migration change; exact before/after at `WorkspaceBrowserIT.java:315-316,402` | PASS |
| RULE-12 | Existing local security surface | No route/security diff; full 16-browser regression plus module unit tests; inspection uses existing assets/read path | PASS |
| RULE-14 | Real UC-1 journey, independent filtered ID totals, default availability | Two packaged Chrome/PostgreSQL UC-2 journeys start at accepted UC-1 Week and check independently computed IDs and authoritative ribbon values; `WorkspaceBrowserIT.java:363-405,480-528,531-587` | PASS |
| RULE-16 | Isolated normative/browser fixture and user walkthrough gate | `WorkspaceBrowserIT.java:418-468,1291-1326` generates 60 classes, 100 teachers, 100 rooms, 1,000 lessons, long names and occupied/empty periods; kernel verifies definition and complete accepted pair. Previous report records user `LGTM` on 2026-09-23 for the UC-2 walkthrough; feature-wide five-participant gate remains governed by `spec.md:321-350` | PASS for UC-2 technical convergence; no new feature-wide participant claim |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Shared accepted model/range and whole-school renderer | Complete `WorkspaceBrowserIT` retry 17/0/0/0 includes UC-1 paths | PASS |
| UC-3 | Shared filters, empty-view message and renderer | Complete retry includes UC-3 browser journeys; revised assertion matches `messages.js:227` | PASS |
| Approved workspace/repair UCs | Packaged Chrome routes and fixture | Initial CDP timeout at `WorkspaceBrowserIT.java:895` did not recur in isolated retry or full retry | PASS on retry; intermittent timeout noted |

## Findings

No blocking findings. C-1 is closed by counts over the represented, uniquely identified lessons and independently calculated Chrome assertions (`app.js:744-753`, `WorkspaceBrowserIT.java:531-559`). G-2 is closed by the complete kernel-verified scale pair and explicit/omitted authoritative ribbon comparisons (`WorkspaceBrowserIT.java:418-587,1291-1326`). P-1 is closed by the immutable `5df107e` submission. Prior G-1 remains closed by independent full-browser regression.

### Diagnostic note - Intermittent unrelated DevTools timeout

The first independent full run had one error at `WorkspaceBrowserIT.java:895` while discarding a protected repair draft (CDP command timeout), the same class of intermittent failure observed in the previous convergence. The isolated retry and complete 17-test retry passed without edits; this is not attributed to the UC-2 submission. No test was skipped or weakened.

## Walkthrough

Previously recorded user confirmation of the UC-2 walkthrough: `LGTM` on 2026-09-23. Its tasks are: find a subject's Week lessons, explain the selected teacher's assigned/available/unavailable ribbon, switch Week/Day retaining identities, apply single/intersected filters and account for the represented counts, observe empty and zero-match paths, clear one identity and recover the complete accepted context. This carries forward the prior confirmation; it does not claim new five-participant, three-school feature-wide validation.

## Status Update

`READY_FOR_CONVERGENCE` -> `APPROVED`; all use cases in this feature's status are technically approved. The feature-wide normative five-participant administrator gate remains a separate requirement and is not asserted by this UC-2 technical verdict. UC-1 and UC-3 retain their approved statuses; executor evidence remains in `status.md`.

## Response to execute

APPROVED WITH NOTES: UC-2 is technically converged; the first independent full run had an intermittent unrelated DevTools timeout, followed by an isolated pass and a full 17-test pass.
