# Public Demo Deployment Specification


2026-10-18 user correction: bundled artifacts load directly at startup. Kernel verification, fingerprint/evidence
checks and scoring belong to explicit maintenance, not web readiness. This supersedes startup-verification
requirements below. Local demo runs with loopback-only hosts accept direct same-origin HTTP and bind only to
loopback; public hostname/proxy configurations retain the HTTPS boundary.

## Feature summary

The public demo lets a visitor inspect and modify a school timetable without installation or login. Full school is
preselected; explicitly using it opens a precomputed accepted timetable. Visitors can try repairs and manual edits,
or save edited definition JSON in their browser and obtain an initial timetable. Accepted bundles can be downloaded
and exchanged as files.

Each browser session owns an ephemeral workspace. Identical complete requests can reuse verified artifacts or share
one computation, while drafts, proposals, review, and acceptance remain private to each workspace. Global capacity,
queue admission, process-time reservations, and Railway spending controls bound this deliberately temporary service.

This is the behavioral contract derived from the confirmed [proposal](proposal.md). It specifies no implementation
or approval status. Use cases are verified independently before subsequent execution continues.

## Scope and resolved decisions

- One always-on Railway Hobby service, one replica, an EU service region, and a stable Railway-generated public
  domain. No database, login, durable workspace, separate worker service, or Pro subscription is required.
- Exactly three fictionalized current-catalog samples with maintained precomputed timetables. Selection previews;
  explicit use creates the visitor's accepted baseline without visitor-triggered kernel work.
- A definition dropdown and plain monospace JSON editor replace structured authoring. Browser-local versions preserve
  unfinished syntax-valid objects. Using an edited definition resets to initial planning, after confirmation and
  validation. Repair availability/pins and manual period/room changes remain available.
- Quick and Thorough are effort budgets, not quality promises. New computations have one global solver slot, an
  estimated-wait admission gate, and at most eight distinct waiting computations. There are no per-address quotas.
- All visitor-triggered kernel process time, including verification, consumes a reserved daily allowance. Counters,
  sessions, jobs, and visitor caches reset on restart. This is accepted; the platform spending limit is the financial
  backstop rather than a durable application usage ledger.
- Verified feasible results and successful exact-document verification are reused by default, with a configuration-file
  switch to disable reuse. Shared computation never shares workspace state. No public force-fresh action exists.
- Complexity gates use placement work and lesson count independently of upload bytes. The final supported envelope,
  Thorough duration, and memory/session ceiling are established by UC-12 measurements within the agreed target.
- Safe structured logs and configurable minute summaries supplement Hobby's resource/spend views. Provider request
  logs have a separate storage policy; an EU service region does not imply EU log storage.

### Existing contracts retained and overridden

The [workspace](../timetable-workspace/spec.md), [inspection](../timetable-inspection-ux/spec.md),
[matrix lenses](../timetable-matrix-lenses/spec.md), and [manual editing](../timetable-manual-editing/spec.md)
contracts retain their timetable meanings, lineage, hard validity, pin provenance, review totals, display fidelity,
and current supported navigation. The manual teacher-change path is superseded by
[authoring UC-20](../school-definition-authoring/spec.md#uc-20---find-the-supported-route-for-a-teacher-change).
In the demo, its guidance points to JSON definition editing and explains that use starts a new initial timetable.

For this profile only, this specification overrides local-only access, single-school global ownership, persistence
and restart recovery, structured authoring, fixed solve/retry durations, upload ceilings, unbounded execution, and
the requirement to repeat verification when exact trusted evidence is available. The normal local profile keeps
its existing behavior. `DEFINITION_DRAFT`, seeded authoring repair, and return-to-authoring transitions are unavailable
in the demo. This feature changes the kernel's incomplete-construction outcome in UC-1 for all clients.

### Verification boundary

Each applicable use case is exercised through the real public application boundary and packaged kernel with isolated
visitor state, never production school data. UI-facing use cases include an agent-run real-browser walkthrough after
technical convergence; passing evidence permits approval without an additional human confirmation gate. Two isolated
browser sessions demonstrate ownership; tabs in one browser session are deliberately the same owner. Operator use
cases require evidence from the deployed Hobby service, not source-only claims. Cold benchmarks disable reuse.
The detailed guarantees below own the required outcomes; this section introduces no separate acceptance checklist.

## Actors and domain terms

- **Visitor:** An anonymous browser-session owner trying a school timetable. Other visitors have equal capabilities
  but no access to this owner's workspace or run records.
- **Demo operator:** Deploys the service, sets its effective limits, qualifies capacity, and reviews usage/cost.
- **Kernel client:** Consumes the packaged kernel's structured planning or repair outcome, including the demo.
- **School Kernel:** Supporting authority for definition validity, assignments, lineage, feasibility, and verification.
- **Railway:** Supporting hosting, HTTPS edge, resource billing, spend control, and provider logging service.
- **Browser storage:** Device/origin-local storage of explicitly saved definitions and existing view preferences.
- **Accepted baseline / Current:** The complete exact school definition and matching verified feasible result, with
  revisions and provenance. A definition alone, a result alone, or a proposal is never Current.
- **Visitor run:** A session-owned request and its status/proposal identity. At most one is active per workspace.
- **Computation:** One distinct complete kernel solve request; several visitor runs may subscribe to it.
- **Artifact:** An immutable complete verified feasible result with its matching input and original provenance.
- **Verification evidence:** A successful local-kernel verification bound to complete documents, mode, and kernel build.
- **Kernel time:** Sum of elapsed lifetimes of visitor-triggered kernel processes, including concurrent processes
  separately; neither CPU time nor search duration alone.
- **W:** Placement work proxy `lesson_count × planning_period_count × room_count`, evaluated safely from the complete
  definition. It is not a runtime/feasibility prediction.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Understand a search that ends before all lessons are placed | Kernel client | None |
| UC-2 | Open and retain a private temporary demo workspace | Visitor | None |
| UC-3 | Try a bundled timetable immediately | Visitor | Requires UC-2 |
| UC-4 | Keep and edit definition versions in this browser | Visitor | Requires UC-2 |
| UC-5 | Start a new timetable from a custom definition | Visitor | Requires UC-4 |
| UC-6 | Obtain an initial or repair proposal within demo capacity | Visitor | Requires UC-2 |
| UC-7 | Prepare a protected repair of Current | Visitor | Requires UC-2 |
| UC-8 | Decide whether a generated proposal becomes Current | Visitor | Requires UC-6 |
| UC-9 | Adjust periods and rooms manually and publish valid changes | Visitor | Requires UC-2 |
| UC-10 | Keep or share an accepted timetable as a file | Visitor | Requires UC-2 |
| UC-11 | Open an accepted timetable bundle without solving | Visitor | Requires UC-2 |
| UC-12 | Launch a demo with measured capacity and spend protection | Demo operator | None |
| UC-13 | Explain demo usage, resource consumption, and cost | Demo operator | Requires UC-12 |

UC-3 is the primary use case. Baselines used by UC-7, UC-9, or UC-10 may come from a sample, explicit proposal
acceptance, or bundle import; those alternatives do not force every visitor through one onboarding sequence.

## State models

### Session lifecycle

| From | Event | To | Observable result |
|---|---|---|---|
| Absent | First permitted use while capacity exists | Active | New private empty workspace; Full previewed |
| Absent | Workspace capacity full | Absent | Demo-full message; existing visitors unaffected |
| Active | Owner request before expiry | Active | Same workspace; idle clock refreshed, including polling |
| Active | Idle or absolute limit reached | Ended | Workspace unavailable; requests detached from computation |
| Active | Restart, deploy, or shutdown | Ended | Workspace/jobs lost; no recovery or silent acceptance |
| Ended | Visitor returns | Ended | Explanation and start-again/import choices, with browser versions retained |
| Ended | Visitor explicitly starts again and capacity exists | Active | New empty private workspace |

### Workspace lifecycle

The demo has exactly these states: `EMPTY`, `INITIAL_DRAFT`, `SOLVING_INITIAL`, `INITIAL_PROPOSAL`,
`ACCEPTED_BASELINE`, `REPAIR_DRAFT`, `SOLVING_REPAIR`, `REPAIR_PROPOSAL`, and `MANUAL_DRAFT`.

| From | Permitted action/outcome | To |
|---|---|---|
| EMPTY | Use sample or verified bundle | ACCEPTED_BASELINE |
| EMPTY | Use verified initial definition | INITIAL_DRAFT |
| INITIAL_DRAFT | Admit initial run | SOLVING_INITIAL |
| SOLVING_INITIAL | Complete verified feasible result, including reuse | INITIAL_PROPOSAL |
| SOLVING_INITIAL | Cancel, no feasible result, invalid input, or failure | INITIAL_DRAFT |
| INITIAL_PROPOSAL | Explicit successful acceptance | ACCEPTED_BASELINE |
| INITIAL_PROPOSAL | Discard or revise | INITIAL_DRAFT |
| ACCEPTED_BASELINE | Start repair draft | REPAIR_DRAFT |
| REPAIR_DRAFT | Edit/revert availability or pins | REPAIR_DRAFT |
| REPAIR_DRAFT | Admit repair run | SOLVING_REPAIR |
| SOLVING_REPAIR | Complete verified feasible result, including reuse | REPAIR_PROPOSAL |
| SOLVING_REPAIR | Cancel, no feasible result, invalid input, or failure | REPAIR_DRAFT |
| REPAIR_PROPOSAL | Explicit successful acceptance | ACCEPTED_BASELINE |
| REPAIR_PROPOSAL | Discard or revise | REPAIR_DRAFT |
| REPAIR_DRAFT | Explicit draft discard | ACCEPTED_BASELINE |
| ACCEPTED_BASELINE | Start manual editing | MANUAL_DRAFT |
| MANUAL_DRAFT | Edit, revert, or encounter blocking conflict | MANUAL_DRAFT |
| MANUAL_DRAFT | Explicit valid publication or confirmed discard | ACCEPTED_BASELINE |
| Any non-solving state | Confirm replacement and successfully use sample/bundle | ACCEPTED_BASELINE |
| Any non-solving state | Confirm replacement and successfully use initial definition | INITIAL_DRAFT |
| Any non-solving state | Explicitly clear the workspace under its version guard | EMPTY |

Inspection and permitted export are self-transitions. Failed/refused replacement or acceptance leaves the existing
state/data unchanged, except stale proposals are invalidated and return to their corresponding draft. Session end
removes the entire workspace rather than recovering a draft. Cache hits pass through the same run/proposal guards
even when the solving state is too brief to see. All transitions not listed are refused without mutation or disclosure.

### Visitor run and shared computation lifecycle

A permitted request either obtains a verified cache hit, attaches to a matching queued/running computation, or reserves
allowance and enters the queue as new work. Refusal creates no run. A run's active phases are Queued, Running, and
Checking result; terminal outcomes are Feasible, Not found within budget, Invalid input, Failed, Cancelled, and Expired.
Only verified Feasible creates a proposal. A subscriber joining running work sees its actual current phase.

New physical computations progress Queued → Running → Checking result → verified feasible artifact or unsuccessful
terminal outcome. Queued/running/checking work can be cancelled or fail. Each visitor cancellation, queued
abandonment, or session expiry detaches only that run; the physical work is removed/stopped only when no subscribers
remain. Shutdown stops all work. Late or duplicate completion cannot update an inactive run or a changed workspace.
Completed artifacts live independently until fixed TTL expiry, memory eviction, configuration disabling reuse, or
restart. Packaged samples and their startup evidence are outside that visitor-cache lifecycle.

## UC-1 - Understand a search that ends before all lessons are placed

- Goal: Distinguish an insufficient search budget from a kernel failure.
- Primary actor: Kernel client
- Supporting actors: School Kernel
- Trigger: The client requests a time-limited planning or repair search.
- Preconditions: The complete input is valid.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The client requests a bounded initial or repair search.
2. The budget ends before a complete candidate exists.
3. The kernel reports `NO_FEASIBLE_SOLUTION_FOUND`, termination reason `TIME_LIMIT`, and a diagnostic explaining that
   the budget ended before all lessons were placed.
4. The client presents an unsuccessful search within this budget and offers an ordinary subsequent request.

### Extensions

- 2a. If construction completes and a complete hard-feasible result exists, the normal feasible result is returned;
  end.
- 2b. If a complete candidate exists but is not hard-feasible at termination, the normal no-feasible outcome applies;
  end.
- 2c. If input is invalid or a real internal/transport failure occurs, its distinct existing outcome is reported;
  end.

### Guarantees

- G1. Initial planning and repair use the same insufficient-construction classification. A partial timetable is never
  published as feasible, reviewable, or accepted.
- G2. The new classification changes neither input validity nor the meaning of successful feasibility, revisions,
  lineage, or time budgets. It is not a proof that the school has no feasible timetable.
- G3. Packaged-kernel evidence exercises both operations with insufficient construction time and distinguishes real
  failures. The client never treats this outcome as an internal crash or a reusable negative result.

### Postconditions

- Success: The client can explain the unsuccessful bounded search accurately without receiving a partial timetable.
- Minimal guarantee: No incomplete candidate becomes a proposal or changes any accepted baseline.

## UC-2 - Open and retain a private temporary demo workspace

- Goal: Try the public application with an isolated workspace and understand when work will disappear.
- Primary actor: Visitor
- Supporting actors: Railway; browser storage
- Trigger: The visitor opens the public demo or returns to an existing session.
- Preconditions: The demo is serving requests.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor opens the stable public HTTPS address.
2. The demo creates or reopens only that browser session's workspace and shows its current lifecycle state.
3. A new workspace shows the definition panel, Sample schools and Saved in this browser groups, Full preselected,
   and its preview. It loads no baseline and starts no search merely by selection.
4. The visitor reads the persistent retention banner/privacy note and continues inspection or another permitted goal.
5. Requests retain that workspace until the configured idle or absolute expiry in [Demo limits](#demo-limits).

### Extensions

- 2a. If a new workspace would exceed capacity, the visitor sees that the demo is full and may try again later; no
  workspace is created and existing visitors keep their work; end.
- 2b. If the session ended through inactivity, absolute expiry, deploy, or restart, the demo explains that it ended
  and offers starting again, browser-local versions, and accepted-bundle import; no failed mutation is replayed;
  end.
- 2c. If a request targets another owner's run or supplies a guessed identifier, access is denied without confirming
  that owner's existence, status, input, or output; end.
- 2d. If host, scheme trust, origin, CSRF, or permitted-route checks fail, access/mutation is denied with a safe
  problem response and no workspace change or school-data disclosure; end.
- 4a. If the visitor uses another tab in the same browser session, it shares that workspace; stale changes are refused
  and a reload of current state is offered; resume at step 2.
- 4b. If the visitor explicitly clears the workspace, the demo names the work to discard and offers accepted export;
  confirmation clears only that workspace to EMPTY under its version guard and returns to the definition panel,
  retaining browser versions and the independent bounded cache; end.
- 4c. If clearing is declined, stale, or attempted with an active run, nothing is cleared; the demo explains any need
  to reload or cancel/wait first; end.

### Guarantees

- G1. Session ownership applies to every existing and new data/action route, full snapshots, run polling, review,
  acceptance, import, export, and cancellation. No URL exposes a workspace identifier. Bundled public sample content
  is the only shared readable school content.
- G2. The required session cookie is HttpOnly, SameSite=Strict, and Secure on the demo. Public access preserves CSP,
  CSRF, same-origin mutation checks, response-type protections, and no-store for visitor data. A configured public
  host allowlist and narrowly trusted edge scheme forwarding cannot be bypassed by spoofed forwarded headers.
- G3. Only the health route accepts the platform healthcheck host. It reveals no visitor data and creates no session.
  Healthchecking during deploy is not presented as continuous runtime monitoring.
- G4. The banner says accepted timetables must be exported to keep them and unfinished repair/manual work disappears
  with the session. The note distinguishes browser storage, workspace expiry, the bounded shared cache that may
  outlive a session, and provider metadata retention under [Privacy disclosure](#privacy-disclosure).
- G5. Polling counts as activity but never extends absolute expiry. Expiry/restart never auto-accepts or exposes a
  proposal. Expiry detaches requests under UC-6; a cache artifact may remain under its independent retention policy.
- G6. No authoring entry, structured authoring action, catalog-upgrade flow, or return-to-authoring is available;
  those routes return 404, and `DEFINITION_DRAFT` is unreachable. The local non-demo profile keeps its existing
  access, authoring, persistence, and restart behavior.
- G7. An isolated two-browser journey proves private imports, drafts, runs, cancellation, and exports; direct access
  attempts reveal no other workspace. A separate same-session stale-tab path demonstrates the version guard.

### Postconditions

- Success: The visitor has one private workspace and understands its retention and export boundary.
- Minimal guarantee: Refused/ended sessions expose no other visitor's data and do not replace or recover lost work.

## UC-3 - Try a bundled timetable immediately (primary)

- Goal: Inspect and try repairing a realistic fictional school without uploading data or waiting for a solve.
- Primary actor: Visitor
- Supporting actors: School Kernel startup verification
- Trigger: The visitor explicitly uses a selected bundled sample.
- Preconditions: UC-2 provides an active workspace; the sample passed deployment verification.
- Relations:
  - Requires: UC-2, because a private workspace exists
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor previews one of the exact [Bundled samples](#bundled-samples), including description, fictional-person
   notice, size, JSON, and the default preset for a later solve.
2. The visitor requests Use this sample.
3. If work would be replaced, the demo names the current timetable, proposal, repair/manual draft, or initial draft
   that will be discarded, offers export where an accepted baseline exists, and obtains explicit confirmation.
4. The demo loads the complete packaged definition/result pair using its startup verification evidence and creates
   the visitor's independent `ACCEPTED_BASELINE` under the workspace version guard.
5. The accepted whole-school timetable opens in the existing inspection surface, ready for repair, manual editing,
   and export; the chosen entry is marked In use.

### Extensions

- 1a. If selection changes while the editor holds unsaved text, discard confirmation is required before changing the
  selection; declining retains the text and selection; end.
- 2a. If unsaved changes exist, using the entry is unavailable and the panel explains saving or reverting first; end.
- 2b. If a run is active, use is unavailable and the panel offers waiting or cancelling; end.
- 3a. If confirmation is declined, nothing is replaced; end.
- 4a. If the identifier is unknown, `404 SAMPLE_NOT_FOUND` is returned and the workspace is unchanged; end.
- 4b. If the workspace version changed, replacement is refused without discarding intervening work and reloading
  current state is offered; end.

### Guarantees

- G1. Sample use starts zero visitor-triggered kernel processes and consumes no daily allowance or solver/verification
  slot, including when the visitor cache is disabled, full, expired, or cleared. It works at daily allowance exhaustion.
- G2. Packaged samples have the current catalog at packaging, matching complete verified feasible results, exact
  definition/timetable revisions, and current scoring evidence. A failed startup pair prevents readiness. Scoring
  changes require regeneration; assignment verification alone does not establish stored-score freshness.
- G3. Sample people use a committed pool of distinct familiar demo names such as John Doe and Jane Doe; school names
  are fictional. Identifiers/references use a consistent one-to-one mapping preserving identifier ordering and array
  order. Nonidentifying scheduling structure is retained. Generation evidence demonstrates inverse-map fidelity
  before catalog upgrade and verified validity afterward; package checks exclude source names/identifiers and the
  real-name examples from every shipped artifact.
- G4. A sample edit or accepted repair changes only this visitor's copy. Bundled definitions/results and other visitors
  remain unchanged. Selection alone only previews; it never replaces Current.
- G5. Current, draft, and proposal remain visibly distinct. Whole-school Week/Day, filters/lenses, details, names,
  availability, and narrow read-only inspection retain the current contracts; no new mobile editing promise is made.
- G6. Opening a precomputed accepted baseline records a sample open, never an explicit proposal acceptance. Public
  sample reads may revalidate by ETag; visitor state remains no-store.
- G7. Real-browser evidence loads all three samples and the Full inspection/repair/export entry points; it demonstrates
  no visitor process launches and no change to the source artifacts or a second visitor's workspace.

### Postconditions

- Success: The selected verified timetable is Current in this visitor's workspace without computation.
- Minimal guarantee: Refusal or cancelled replacement preserves prior work, selection/text where applicable, and privacy.

## UC-4 - Keep and edit definition versions in this browser

- Goal: Adjust definition JSON and retain named copies, including unfinished valid JSON objects, on this device.
- Primary actor: Visitor
- Supporting actors: Browser storage
- Trigger: The visitor edits a selected definition or opens a local definition file.
- Preconditions: UC-2 provides the definition panel and sample catalog.
- Relations:
  - Requires: UC-2, because its panel and private workspace context exist
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor selects a sample/saved version or opens a local JSON file within the document byte limit.
2. The plain monospace textarea shows the exact stored/file text, with Unchanged or Changed, not saved status.
3. The visitor edits the text; syntax checking reports the first error's line/column without starting kernel work.
4. The visitor requests Save as, chooses an identifier under [Saved versions](#saved-versions), and confirms any
   replacement of an existing saved version identified by that label.
5. The browser stores the complete envelope and exact text, selects the saved version, and lists it most recently
   saved first. Other open tabs refresh the list.
6. The visitor can reload, select the saved version, download its exact text, revert unsaved changes to stored text,
   or delete a named version/all saved versions after confirmation.

### Extensions

- 1a. If stored data cannot be read, the entry is shown as unreadable and can only be deleted; it is not used; end.
- 1b. If selection/file opening would discard unsaved text, confirmation is required; declining retains that text;
  end.
- 1c. If the file exceeds the limit or cannot be read, it is refused and existing text/storage are unchanged; end.
- 4a. If syntax is invalid, the root is not an object, the UTF-8 limit is exceeded, or the identifier is invalid or
  reserved, saving is refused with an explanation and the edited text retained; resume at step 3.
- 4b. If replacing a saved version is declined, its stored text remains intact and edited text remains available; end.
- 5a. If storage is unavailable/full, nothing is written or overwritten, text remains in the editor, and downloading
  and deleting older versions are offered; end.
- 6a. If deletion is declined, nothing is deleted; end.

### Guarantees

- G1. Saving checks syntax/object shape and size, not kernel validity. It starts no kernel process, sends no unused
  document to the server, and can retain unfinished school content. Whitespace differences count as edits.
- G2. The preserved envelope fields, origin semantics, identifier rules, and local-storage key are exactly those in
  [Saved versions](#saved-versions). Unsupported/corrupt envelopes are not silently normalized or executed. Labels
  are independent of school identity and are never sent to the server or application logs.
- G3. Samples cannot be overwritten/deleted. Saved entries have one version per identifier, no history, and no
  application count ceiling beyond browser storage. Revert restores exact stored text, not reserialized JSON.
- G4. Download preserves the editor's text even when it is unfinished; a downloaded sample also serves as a format
  example. Opening a file and downloading are browser-local operations.
- G5. Local versions belong to one origin/browser/device, survive session expiry and server restart, and can be lost
  through site-data clearing, browser eviction, or private-window closure. The panel explains this and offers
  Download as the durable copy. No cross-device sync or tracking data is added.
- G6. In use is tied to the actual loaded version and survives page reload. Replacing/deleting that saved version or
  accepting a definition-changing repair clears the mark; imported bundles show their own origin without marking
  an unrelated dropdown entry. Declining any confirmation preserves existing work.
- G7. Real-browser evidence covers exact-text reload, replacement, reserved IDs, syntax-valid kernel-invalid work,
  storage failure, corrupt entries, unsaved-change confirmation, deletion, and cross-tab list refresh.

### Postconditions

- Success: A selected named local version preserves the exact JSON text and provenance for later use/download.
- Minimal guarantee: Failed or declined operations lose neither existing stored versions nor the current edited text.

## UC-5 - Start a new timetable from a custom definition

- Goal: Use a saved edited/uploaded school definition as a new initial-planning draft.
- Primary actor: Visitor
- Supporting actors: School Kernel
- Trigger: The visitor requests Use this definition for a saved version.
- Preconditions: UC-4 produced a readable stored version; UC-2's session is active.
- Relations:
  - Requires: UC-4, because edited/file definitions must be saved before use
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor selects a saved version whose exact stored text is shown without unsaved edits and requests its use.
2. If existing work would be replaced, the demo identifies everything to discard, offers export of Current, and
   obtains confirmation before replacing anything.
3. The demo safely parses the submitted text, enforces document and complexity limits, and obtains successful
   verification for an initial definition, reusing exact evidence when enabled and available.
4. Under the workspace version guard, the demo replaces the workspace with that complete verified definition in
   `INITIAL_DRAFT`, records its safe origin, and shows the school summary and default effort preset.
5. The visitor can request initial planning; no timetable is yet Current.

### Extensions

- 1a. If text is unsaved or a run is active, use is unavailable and the panel explains saving/reverting or waiting/
  cancelling first; end.
- 2a. If replacement is declined, the existing workspace and editor remain unchanged; end.
- 3a. If safe parsing, byte size, or complexity fails, the demo explains the reason and changes no workspace data;
  the local version stays editable; end.
- 3b. If kernel validity, supported schema/catalog, initial-definition lineage, identity, or references fail, located
  validation errors are shown and the saved version/workspace are unchanged; resume at step 1.
- 3c. If new verification lacks daily allowance or a verification slot cannot be obtained within its wait limit, the
  operation is refused with the corresponding daily-limit or busy response; no replacement occurs; end.
- 3d. If verification cannot start, times out, fails, or is interrupted, a safe failure explains that the definition
  was not used; the prior workspace and local text remain intact; end.
- 4a. If the session or workspace version changed while validation ran, the result is not installed; an ended-session
  or stale-state explanation is shown without overwriting newer work; end.

### Guarantees

- G1. The complete text is sent unchanged, independently of the browser label. The server preserves all supported
  definition fields, optional-field distinctions, identifiers, references, and array order during canonical storage.
  Unsupported/unmappable values produce located validation failures rather than silent drops or invented defaults.
- G2. Definition-only use requires an initial definition without repair ancestry. It never creates an accepted
  timetable or seeded repair, even when derived from a sample or a formerly accepted definition.
- G3. Complexity is checked before expensive kernel work with the independent W/lesson gates in
  [Complexity envelope](#complexity-envelope); document bytes are a separate guard. A tiny file that exceeds W or
  lessons is still refused. Counting is overflow-safe; malformed collections are rejected, not miscounted as zero.
- G4. New verification is bounded, reserves process time upfront, and is counted/settled under
  [Admission and kernel-time accounting](#admission-and-kernel-time-accounting). Exact evidence follows
  [Reuse identity and retention](#reuse-identity-and-retention). Cache availability never bypasses current limits or
  workspace guards.
- G5. Only successful verification plus a current version can atomically replace prior work. Origin distinguishes
  known source sample from file/custom input without trusting it for validation or logging browser labels.
- G6. Real-browser evidence edits Full, saves/reloads it, uses it, and reaches initial planning. Invalid, oversized,
  verification-busy, daily-exhausted, and stale replacements preserve a previously accepted baseline exactly.

### Postconditions

- Success: The custom definition is the private initial draft, with no accepted result and no solve started by use.
- Minimal guarantee: A failed/refused replacement leaves prior accepted state, drafts/proposals, and local versions intact.

## UC-6 - Obtain an initial or repair proposal within demo capacity

- Goal: Obtain a verified proposal with truthful status while respecting shared demo resources.
- Primary actor: Visitor
- Supporting actors: School Kernel
- Trigger: The visitor requests Quick or Thorough planning from an eligible draft.
- Preconditions: UC-2's session is active; a verified initial draft or valid ordinary repair draft exists; no visitor
  run is active in this workspace.
- Relations:
  - Requires: UC-2, because status and proposals belong to its private workspace
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor reviews the selected effort and requests a proposal for the exact initial or compiled repair input.
2. The demo checks the lifecycle/version and effective complexity limits, identifies the complete request, and finds
   neither a reusable verified artifact nor matching queued/running work.
3. The demo evaluates both queue gates and reserves enough remaining daily allowance for the solve and required
   verification. Admission creates a private run, invalidates any older proposal, and promptly returns its identity
   in the appropriate solving state without holding the request until completion.
4. The visitor sees queue position and an approximate start estimate, then Running with elapsed time against the
   actual budget and Checking result while complete local verification is obtained.
5. The kernel produces a complete feasible result and the demo verifies its complete documents and request evidence,
   including operation, input/baseline, revisions, actual duration budget, and configured solve provenance.
6. Only the exact still-active run updates its workspace with a separately owned initial/repair proposal, retains the
   immutable artifact for eligible reuse, settles actual kernel time, and releases unused reservation.
7. The visitor opens the proposal for review; Current remains unchanged until UC-8 acceptance.

### Extensions

- 2a. If a verified eligible artifact exists, the demo creates a new private run/proposal with current workspace
  guards, starts no process or reservation, explicitly says the result was reused, and preserves the original solve
  provenance/duration separately from this request's wait; continue at step 7.
- 2b. If an identical computation is queued, running, or being verified, the new private run subscribes to its actual
  phase without adding a queue entry or reservation; continue at step 4.
- 2c. If state/version/input/complexity is invalid, the request is refused with a safe explanation and no new run,
  reservation, process, or draft mutation; end.
- 3a. If either queue gate fails, the demo returns 429 with Retry-After and a busy explanation without creating a run
  or reserving allowance; end.
- 3b. If remaining allowance cannot cover the complete reservation, the demo returns 429 with a daily-limit
  explanation before queueing/starting anything, retaining the draft unchanged; end.
- 4a. If the visitor cancels while queued/running/checking, only their run detaches and becomes Cancelled, returning
  to its original draft. Physical work continues for remaining subscribers; if none remain it is removed/stopped and
  its actual time settled; end.
- 4b. If a queued visitor has not polled for the abandonment limit, or their session ends, that run becomes Expired
  and detaches. Other subscribers continue; if none remain the computation is removed/stopped; end.
- 4c. If shutdown/restart occurs, no proposal is accepted; jobs and sessions end and the next visit explains the loss;
  end.
- 5a. If the search finds no complete feasible timetable within the budget, the demo reports that outcome (including
  UC-1's construction diagnostic where applicable), restores the draft, settles time, and offers an ordinary later
  request; no negative result is cached; end.
- 5b. If the kernel reports invalid input, the demo shows safe located errors, restores the draft, settles time, and
  produces no proposal/cache artifact; end.
- 5c. If process startup, timeout, transport, result integrity, or verification fails, the demo reports a safe failure,
  restores the draft, settles all actual process time, and publishes/caches no candidate; end.
- 6a. If a run is no longer active, a version/baseline/intent changed, or completion is duplicate/late, it cannot write
  a proposal or resurrect cancelled work; other still-eligible subscribers may receive their own result; end.

### Guarantees

- G1. [Search efforts](#search-efforts), [Demo limits](#demo-limits),
  [Admission and kernel-time accounting](#admission-and-kernel-time-accounting), and
  [Reuse identity and retention](#reuse-identity-and-retention) apply on every path. Cache disabling is an operator
  configuration-file choice; no visitor action bypasses reuse or exposes seed/step/arbitrary-budget controls.
- G2. One solver slot serves distinct queued computations in admission order, including their required result
  verification before releasing that slot. Separately requested verification remains bounded by its own slots.
  Queue counting counts computations, not their subscribers. Joining existing work uses no extra solve slot.
- G3. Estimates use remaining running effort, distinct jobs ahead, and measured overhead. They are refreshed and
  explicitly approximate, never an upper-bound or finish-time promise. A shared job has the same position/phase for
  its subscribers. Reused computation time is not displayed as newly spent time.
- G4. Polling uses a small run-status response about once/second with backoff while queued. It does not repeatedly
  fetch the complete school or CSRF token. A full snapshot is fetched when the run finishes; JSON is compressed.
  Status is session-owned and never reveals another subscriber or their data.
- G5. Each workspace has independent run/proposal IDs, review, acceptance, and cancellation. Cached artifacts and
  original kernel documents are immutable. Attaching a subscriber never reuses another owner's workspace version,
  review state, acceptance, or session/run authority.
- G6. Cancellation first detaches the logical run. The last subscriber's departure removes waiting work or requests
  graceful termination, followed by forced termination after two seconds. Reservations remain held until process
  termination/time settlement; abandoned work is never charged as though it consumed its full search budget.
- G7. Every visitor-triggered kernel process counts, including failed/cancelled solves and verification. Successful
  evidence/hits/coalescing introduce no additional process-time reservation. At exhausted allowance, available
  packaged/cached use, inspection, and export still work; required new kernel work is refused.
- G8. A queued admission reserves its own future verification; later traffic cannot strand it by consuming that
  allowance. All successful mutations use the current owner/version/run guards even when computation is shared.
  Non-feasible, partial, unverifiable, and failed outputs never become reviewable timetables or accepted state.
- G9. Boundary evidence covers both operations, both presets, cache hits, disabled reuse, simultaneous coalescing,
  subscriber cancellation/expiry, last-subscriber termination, queue gates, insufficient reservations, process failures,
  stale completions, exact verification, and restart. Distinct browsers prove private proposals for shared work.

### Postconditions

- Success: One complete verified initial/repair proposal belongs to the visitor and is eligible for review.
- Minimal guarantee: Current is unchanged; a surviving session retains its draft on unsuccessful execution, no
  ineligible candidate is published, and consumed/reserved allowance remains reconciled.

## UC-7 - Prepare a protected repair of Current

- Goal: Describe weekly resource unavailability and protect accepted placements before requesting repair.
- Primary actor: Visitor
- Supporting actors: School Kernel validation
- Trigger: The visitor starts or revises a repair draft from Current.
- Preconditions: UC-2's session contains an accepted baseline or an existing ordinary repair draft/proposal; no run
  is active and no manual draft is active.
- Relations:
  - Requires: UC-2, because the accepted baseline and staging are private workspace state
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor starts repair from the complete Current baseline or revises its retained repair draft.
2. The demo shows teacher/room weekly availability, accepted assignments, persistent policy, and current draft intent.
3. The visitor stages declared-period unavailability and individual or previewed bulk period/room pins.
4. The demo preserves exact target identities, reports direct effects and conflicting protections, and retains the
   complete draft separately from Current.
5. The visitor reviews the draft summary and can request an effort through UC-6.

### Extensions

- 1a. If no accepted baseline exists, repair is unavailable and initial planning/acceptance is explained; end.
- 1b. If revising a generated repair proposal, it is invalidated immediately before draft changes; resume at step 2.
- 3a. If a bulk preview is cancelled, no pin is applied and the draft is retained; resume at step 3.
- 3b. If identity, declared period, pin/policy, or version checks fail, the change is refused with an explanation and
  the prior draft/Current preserved; end.
- 4a. If the visitor undoes staging or pins, their exact prior draft values are restored without changing Current;
  resume at step 3.
- 5a. If the visitor confirms draft discard, staging and attempt-scoped pins are removed and Current is shown; end.
- 5b. If the visitor requests a school/teacher-definition change, the demo explains using JSON starts initial planning
  and does not transfer pending repair intent; continue with UC-4.

### Guarantees

- G1. Existing protected-repair semantics remain: weekly declared-period availability, accepted-value pins, explicit
  bulk snapshot/confirmation, direct effects distinct from ripple effects, and no invented substitute teachers.
- G2. Persistent policy locks and attempt-scoped pins retain separate provenance. Starting a subsequent repair expires
  previous attempt pins; persistent policy remains. Repair ancestry uses accepted input revision, not timetable revision.
- G3. Draft reload within the session preserves complete staging and pins. Session end/restart loses them; the demo
  promises no durable recovery. Draft changes invalidate a previous proposal immediately.
- G4. Current stays exact while draft values change. All counts, names, periods, target IDs, pin dimensions, policy
  provenance, and optional metadata retain existing field fidelity; missing labels use stable IDs with an explicit
  unavailable-name cue, never guessed identities. Compiled successor size is rechecked under UC-6 before work.
- G5. Shared session/privacy, version guards, process accounting for any required kernel work, and safe-log rules
  apply. Browser evidence exercises availability, individual/bulk pins, conflict refusal, undo, and draft discard on
  a precomputed baseline, preserving Current exactly.

### Postconditions

- Success: A private repair draft precisely represents the visitor's changes/protections and can request UC-6.
- Minimal guarantee: Current is unchanged and no refused staging or pending definition change is silently applied.

## UC-8 - Decide whether a generated proposal becomes Current

- Goal: Understand an initial/repair proposal and explicitly accept or discard it.
- Primary actor: Visitor
- Supporting actors: School Kernel verification
- Trigger: The visitor reviews a proposal produced by UC-6, including a reused result.
- Preconditions: UC-6 produced a proposal still eligible for this workspace's baseline, definition, intent, and run.
- Relations:
  - Requires: UC-6, because a verified private proposal exists
  - Includes: none
  - Extends: none

### Main success scenario

1. The demo identifies Proposal separately from Current, the effort/termination evidence, whether computation was
   reused, and the original computation duration separately from this visitor's wait.
2. For a repair, the visitor reviews direct effects and every [Proposal change category](#proposal-change-categories),
   unique changed lessons, old/proposed values, and class/teacher/room/day groupings and contextual navigation.
   For initial planning, the visitor sees the complete proposed first timetable and the absence of Current.
3. The visitor explicitly confirms the existing acceptance acknowledgement.
4. The demo checks current owner/version, complete proposal identity and lineage, and successful local verification,
   reusing exact evidence or obtaining bounded reserved verification when required.
5. The complete matching definition/result/provenance becomes Current atomically within this private workspace;
   proposal/draft state clears and attempt-scoped pins retain their proper accepted provenance for later expiry.
6. The demo shows Current · accepted, ready for another repair or export, and records an actual acceptance event.

### Extensions

- 1a. If there is no eligible proposal, review/acceptance is refused without disclosing a cached artifact or changing
  Current; end.
- 2a. If a review category is empty, an explicit zero is shown without hiding its meaning; resume at step 2.
- 3a. If the visitor discards the proposal or revises intent, the proposal becomes ineligible and the corresponding
  draft returns; Current stays unchanged; end.
- 3b. If acknowledgement is absent, acceptance is refused and the unchanged proposal remains reviewable; end.
- 4a. If any baseline/definition/intent/result/timetable identity is stale or mismatched, acceptance is refused, the
  proposal is invalidated, and the corresponding draft returns; Current stays unchanged; end.
- 4b. If required new verification lacks allowance/slots, fails, or times out, acceptance does not complete; Current
  stays exact and the unchanged eligible proposal remains reviewable for retry; end.
- 5a. If atomic publication fails or the session ends, no partial baseline or implied acceptance is published; an
  active unchanged proposal is retryable, while an ended session follows UC-2's loss explanation; end.

### Guarantees

- G1. Reuse is not acceptance. Every visitor separately confirms the complete current proposal. No other subscriber's
  review, acceptance, discard, or cancellation changes this decision. Sample opens and bundle imports are not
  counted as explicit proposal acceptance.
- G2. The exact complete input/result, supported versions, school and lesson identities, revisions, lineage, original
  solve/termination evidence, and pin provenance remain bound together. Missing display metadata uses an ID and
  unavailable-name cue; authoritative values are not dropped, relabeled, or invented.
- G3. Unique changed-lesson totals are not sums of overlapping categories/groupings. Old, proposed, and both-context
  groupings remain labeled; directly affected lessons can appear in explanations without duplicate headline counts.
- G4. Acceptance without a fresh verification process is allowed only under exact trusted identity/mode/build rules.
  A submitted hash, claimed feasibility, revision, cached result label, or browser confirmation alone is insufficient.
  New verification follows the slots/reservation rules; no bypass exists at daily exhaustion.
- G5. Until atomic acceptance, Current's complete definition/result remain unchanged. Failed/refused actions leak no
  other visitor data. Existing review navigation, confirmation, and field-fidelity behavior are preserved.
- G6. Real-browser evidence accepts an initial proposal before starting repair, then reviews and accepts the repair,
  distinguishing reused and computed proposals. Stale, unchecked, failed-verification, and duplicate acceptance paths
  demonstrate no unauthorized or partial Current change.

### Postconditions

- Success: Only the explicitly accepted complete verified proposal is now Current for this visitor.
- Minimal guarantee: A failed/refused decision never changes Current or makes another user's acceptance authoritative.

## UC-9 - Adjust periods and rooms manually and publish valid changes

- Goal: Try the existing manual-edit workflow without corrupting Current or promising durable draft storage.
- Primary actor: Visitor
- Supporting actors: School Kernel validation/verification
- Trigger: The visitor starts manual editing of Current.
- Preconditions: UC-2's active workspace contains an accepted baseline, no active run, and no repair draft/proposal.
- Relations:
  - Requires: UC-2, because manual work belongs to its private accepted workspace
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor opens a manual draft that initially matches Current exactly.
2. The visitor changes a lesson's declared period or room and inspects automatic conflict explanations.
3. The draft retains exact edits and blocking-conflict state separately from Current, including across page reload
   during the same active session.
4. After resolving all blocking conflicts, the visitor explicitly requests publication.
5. The demo checks draft/version identity and complete hard validity, obtaining/reusing exact local verification
   under the common limits, and atomically publishes the complete accepted baseline.
6. The accepted timetable opens with the manual draft closed.

### Extensions

- 1a. If a competing draft/proposal/run is active, manual entry is refused with guidance to finish or explicitly
  discard that workflow; end.
- 2a. If an edit has hard conflicts, it remains a visibly blocked draft and cannot be published; resume at step 2.
- 2b. If the visitor reverts a lesson, the full accepted assignment is restored and conflicts recomputed; resume at
  step 2.
- 2c. If the visitor requests teacher reassignment, no teacher change is made; the demo explains definition editing
  and reset to initial planning, without automatically transferring or discarding manual work; end.
- 3a. If the visitor confirms draft discard, all manual changes are removed and unchanged Current opens; end.
- 5a. If conflicts, teacher differences, stale identity, verification failure, insufficient allowance, or verification
  saturation prevent publication, the demo explains the reason, preserves Current and the surviving draft, and offers
  correction/retry; end.
- 5b. If session expiry/restart intervenes, unfinished work is lost as disclosed and the ended-session explanation is
  shown; no draft is published; end.

### Guarantees

- G1. Manual period/room behavior, per-lesson revert, conflict identities/explanations, and explicit publication follow
  the current manual contract. There is no solver optimization or manual teacher-publication path.
- G2. Definition, matching result, versions, parent lineage, and persistent-policy/attempt-pin provenance remain exact
  on publication. Invalid or unmappable edits are refused; display fallback uses stable IDs with unavailable-name cues.
- G3. Current is immutable before successful publication. Draft persistence lasts only through the session; no server
  restart recovery is claimed. All ownership/version, verification-cache, process-budget, and safe-log rules apply.
- G4. Browser evidence demonstrates edit/conflict/revert/publish/discard, teacher-change guidance, and a failed
  publication while retaining Current. A second visitor's accepted sample is unchanged throughout.

### Postconditions

- Success: The visitor's complete conflict-free manual changes are explicitly published as Current.
- Minimal guarantee: Current is never partially or invalidly published; a surviving session retains failed draft work.

## UC-10 - Keep or share an accepted timetable as a file

- Goal: Retain Current beyond the demo session or give someone else the complete accepted timetable.
- Primary actor: Visitor
- Supporting actors: Browser download
- Trigger: The visitor requests export.
- Preconditions: UC-2's active workspace has an accepted baseline.
- Relations:
  - Requires: UC-2, because export reads only its private accepted baseline
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor requests export of Current.
2. The demo identifies the exact accepted definition/result/provenance and creates the archive defined under
   [Accepted-bundle archive](#accepted-bundle-archive).
3. The visitor receives the complete file with school/version identity, suitable for re-import or external exchange.
4. The workspace stays on the same Current baseline and an export event is counted.

### Extensions

- 1a. If no Current exists, export is unavailable and the demo explains acceptance/import/sample use is needed; end.
- 2a. If a repair/manual draft or proposal exists, export still contains only Current, and the demo explicitly says
  that unfinished/unaccepted work is excluded; continue at step 3.
- 2b. If archive creation/integrity/publication fails, no partial download is represented as complete and Current is
  unchanged; end.
- 2c. If a stale/ended/foreign session requests export, no school data is returned and the safe refusal/expiry response
  is shown; end.

### Guarantees

- G1. Exact complete accepted fields, identifiers, assignments, revisions, supported versions, and lock/pin provenance
  survive export/re-import. Integrity checks never silently alter or drop content.
- G2. Export needs no new solve or kernel process and remains available when the daily allowance is exhausted. It
  discloses no browser version labels, other visitor state, cookies, or shared-cache contents.
- G3. Users may exchange the file themselves; no live share link or shared workspace is created. The demo's export
  promise covers accepted work only; local JSON text is kept separately through UC-4 download.
- G4. Browser evidence exports a sample and an explicitly accepted repair, then uses UC-11 in an isolated fresh
  session to reproduce exact Current while excluding drafts/proposals.

### Postconditions

- Success: The visitor has a complete portable accepted bundle; Current is unchanged.
- Minimal guarantee: No unsuccessful export masquerades as a complete file or changes/discloses unauthorized state.

## UC-11 - Open an accepted timetable bundle without solving

- Goal: Render a previously exported or externally shared accepted timetable immediately after validation.
- Primary actor: Visitor
- Supporting actors: School Kernel
- Trigger: The visitor imports a complete definition/result pair or accepted-bundle archive.
- Preconditions: UC-2's active workspace has no active run; supplied documents fit upload limits.
- Relations:
  - Requires: UC-2, because imported data becomes its private workspace
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor supplies the complete accepted documents, optionally in the supported archive.
2. If existing work would be replaced, the demo names what will be discarded, offers export, and obtains confirmation.
3. The demo safely parses the complete input, enforces byte/complexity limits, and obtains local successful
   verification for that exact definition/result and mode, reusing trusted evidence where available.
4. Under the current workspace version guard, the complete baseline and provenance are installed atomically as
   `ACCEPTED_BASELINE`, without solving.
5. The accepted timetable renders; its origin is imported bundle, with no unrelated dropdown entry marked In use.

### Extensions

- 1a. If only a result is supplied, the demo requests the matching complete definition and imports nothing; end.
- 1b. If an archive has unsafe paths, duplicate/unexpected/missing entries, unreadable content, or excessive expanded
  size, it is refused before kernel work without replacing anything; end.
- 2a. If confirmation is declined, existing Current/drafts/proposals are retained; end.
- 3a. If limits, supported versions, school identity, complete assignments, feasible status, revisions, lineage, or
  manifest provenance fail, the demo shows a safe reason and imports nothing; end.
- 3b. If new verification has insufficient allowance or cannot obtain a slot within the limit, the corresponding
  refusal is shown with prior workspace unchanged; end.
- 3c. If verification fails, times out, or cannot run, import does not complete and prior work is unchanged; end.
- 4a. If session/version changed while verification ran, installation is refused without overwriting intervening work;
  end.

### Guarantees

- G1. Import starts no solve. A cold/unseen pair may require bounded budgeted verification; an exact trusted hit may
  render without any new kernel process. Claims/hashes/revisions from the file do not establish trust by themselves.
- G2. Complete definition/result fidelity, manifest versions/revisions, policy/pin provenance, and all authoritative
  fields survive import. Unsupported/unmappable content is refused rather than silently repaired. Without a manifest,
  imported locks are persistent policy; no attempt provenance is guessed.
- G3. An imported accepted pair may contain legitimate repair ancestry; unlike definition-only initial use, import
  validates that exact matching pair rather than discarding ancestry to force initial planning.
- G4. Imported artifacts establish only this visitor's baseline, never another visitor's identity, saved labels,
  drafts, reviews, or ownership. Import events count use, not verified same-person return visits or proposal acceptance.
- G5. Safe archive/JSON parsing, exact verification identity, complexity, global verification slots/reservations,
  ownership/version guards, safe logs, and atomic replacement apply on every path.
- G6. A fresh isolated browser imports a shared result and renders the exact accepted timetable without solving.
  Evidence covers a verification hit and miss, changed result/mode/build, unsafe archive, stale replacement, and
  missing metadata; incorrect input leaves an existing baseline byte-for-byte unchanged.

### Postconditions

- Success: The imported verified accepted timetable is Current for this visitor and can be inspected/repaired/exported.
- Minimal guarantee: A refused, interrupted, or invalid import installs no partial baseline or unverified timetable.

## UC-12 - Launch a demo with measured capacity and spend protection

- Goal: Make the public demo usable within a demonstrated capacity envelope and a configured financial backstop.
- Primary actor: Demo operator
- Supporting actors: Railway; School Kernel
- Trigger: The operator prepares a new deployment or changes its build/effective capacity configuration.
- Preconditions: A Hobby account, deployable demo artifact, fictional samples, and isolated validation data exist.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The operator configures one Hobby replica in an EU service region, a stable generated HTTPS domain, sleeping off,
   effective limits, cache settings, and the workspace spending alert/hard limit under [Demo limits](#demo-limits).
2. Deployment validates all packaged sample definition/result pairs before declaring readiness and reports the
   effective build, settings, and verification outcomes without school payloads.
3. With reuse disabled, the operator runs the cold sample and representative larger-school measurements under
   [Capacity qualification](#capacity-qualification), including construction, result verification, and aggregate memory.
4. The operator records the effective W/lesson limits, Thorough budget, process maxima/reservations, memory limits,
   and supported workspace ceiling that passed those measurements, then enables default reuse.
5. Isolated public-browser journeys demonstrate sample use, custom initial planning/acceptance, repair/acceptance,
   manual editing, export, session expiry, and verified re-import, while a second browser proves private ownership.
6. The operator confirms platform resource/spend views and controls, the host/scheme/health boundary, and unchanged
   local-profile behavior, then makes the stable URL available for the demo's lifetime.

### Extensions

- 2a. If sample validity or current scoring evidence fails, deployment is not ready; no invalid sample is offered and
  an existing healthy deployment may continue serving; end.
- 3a. If 60-second Thorough fails the intended larger-school target, the operator increases it up to 120 seconds and
  repeats the relevant cold measurements; resume at step 3.
- 3b. If the target still fails at 120 seconds or cannot fit the Hobby capacity envelope, launch is withheld and the
  capacity target is revisited explicitly; it is not silently reduced to obtain passing evidence; end.
- 3c. If the starting workspace ceiling exceeds measured memory headroom, the operator lowers that ceiling and
  repeats combined-load validation without reducing the intended individual-school target; resume at step 3.
- 4a. If a process maximum/reservation cannot conservatively cover real termination and verification, work is not
  admitted under those settings and launch is withheld until the bound is demonstrated; end.
- 5a. If lifecycle/privacy/host checks fail, the deployment is not qualified for public use; existing local state and
  unrelated services are not used as test data; end.
- 6a. If later deploy/shutdown occurs, admissions stop, all child work is stopped/settled, sessions/jobs/caches end,
  and the next visit follows UC-2's explanation; no continuity is promised; end.
- 6b. If Railway's workspace hard spending limit is reached, platform workload shutdown is accepted; neither useful
  application logs nor an in-memory daily allowance are claimed to prevent that financial backstop; end.

### Guarantees

- G1. Provisional complexity limits are benchmark inputs, not published proof of capacity. Qualification demonstrates
  the intended larger-school dimensions, not merely MVK or precomputed/cache hits. Final limits state the measured
  combinations they support; independent maxima are not advertised as every possible combination.
- G2. Aggregate memory includes the web service, full visitor cache, the largest permitted workspaces up to the
  ceiling, a large solve and two standalone verification processes, plus non-heap/runtime overhead. Initial resource
  limits are starting settings; local MVK evidence alone cannot prove larger-school safety or prevent container OOM.
- G3. No more than one replica serves this in-memory service. A restart clears all application allowance counters and
  visitor caches/workspaces; packaged samples remain available after new startup verification. No durable queue or
  financial ledger is implied.
- G4. The Railway alert/hard limit covers the whole workspace, including CPU, memory, storage, and network egress;
  the hard limit can stop other workloads there. Included Hobby usage is a cost target, not a guaranteed ceiling.
- G5. The public-edge boundary accepts configured hosts and only required scheme forwarding; deployed spoof-header
  evidence establishes trust instead of assuming all client forwarded headers are overwritten. The healthcheck host
  is permitted only on the health route. Visitor routes retain the private-session/security guarantees of UC-2.
- G6. Samples/logs contain no source-name data; temporary process input/output is private and cleaned up on all exit
  paths. Packaging does not derive samples from real-name files during ordinary builds or ship those files.
- G7. Specification validation uses packaged real-kernel/public-browser and deployed resource evidence. The normal
  local persistence, authoring, solver integration, and restart behavior are regression-checked without sharing its
  database or approvals with this feature. No synthetic passing result replaces a failed cold computation.

### Postconditions

- Success: The deployed demo has a measured declared capacity envelope, verified samples, effective limits, safe
  public access, and workspace-level financial protection.
- Minimal guarantee: Unqualified capacity or invalid samples are not represented as ready; no visitor data is silently
  migrated, persisted, or accepted during deployment failure/shutdown.

## UC-13 - Explain demo usage, resource consumption, and cost

- Goal: Understand whether visitors use the demo and why capacity/cost is consumed without collecting school data.
- Primary actor: Demo operator
- Supporting actors: Railway
- Trigger: The operator reviews live activity, a capacity incident, or the weekly demand signals.
- Preconditions: UC-12 produced a deployed demo with effective limits and Hobby resource/spend controls.
- Relations:
  - Requires: UC-12, because measured service capacity and effective settings provide the review context
  - Includes: none
  - Extends: none

### Main success scenario

1. The operator opens Hobby service resource graphs and current/projected workspace usage/spend.
2. Structured single-line application events expose startup configuration, safe lifecycle outcomes, cache/coalescing
   behavior, and actual proposal acceptances, exports, and imports under [Application events](#application-events).
3. At the configured interval, initially every minute, aggregate summaries report workspaces, distinct queue depth,
   running kernel processes, cache occupancy, and consumed/reserved/remaining daily allowance plus interval counts.
4. The operator correlates work events using safe computation/correlation IDs and separates queue wait, original
   solve duration, verification duration, cache reuse, and actual physical process consumption.
5. During the Hobby log viewing window, the operator reviews counts, outcomes, refusal reasons, preset use, cache
   rates, wait percentiles, custom-definition origins, explicit acceptance, exports, and imports to assess demand.

### Extensions

- 1a. If a resource-threshold monitor requires Pro, it is not configured for this demo; Hobby cost controls and
  deployment/crash notifications remain the available operational signals; continue at step 2.
- 2a. If repeated polling occurs, it emits no per-poll application event or repeated school payload; continue at step 3.
- 3a. If counters reset on restart or UTC allowance rolls over, the startup/summary context distinguishes the reset
  and reconciles retained reservations; it does not claim durable historical usage; continue at step 4.
- 5a. If logs are outside the Hobby viewing window, their absence from the UI is not described as deletion; the
  provider storage disclosure still applies; end.
- 5b. If bundle imports or accepted sample opens are observed, imports are interpreted as file exchange/continuation
  and sample opens as sample use; neither establishes same-person return or explicit proposal acceptance; end.

### Guarantees

- G1. Events/summaries preserve exact effective limits, operation/outcome classifications, counts, process durations,
  and reservation settlement; unknown provenance is labeled custom/unknown rather than invented. Cache hits and
  logical subscribers are distinguishable from real computations so they cannot inflate compute-cost estimates.
- G2. Application logs never contain personal names, raw definitions/results, addresses, session IDs, cookies, or
  browser-local labels. Correlation IDs do not grant access or encode visitor identity. Product conversion/session
  aggregates are computed in memory and emitted only as counts. Every visitor use case shares this log boundary.
- G3. The exact event families and summary fields under [Application events](#application-events) are available in
  structured single-line JSON; the interval is configurable in the application configuration file. No extra metrics
  backend, analytics cookie, per-address counter, or Pro subscription is introduced.
- G4. Railway service CPU/memory/disk/network graphs describe combined service/container consumption, with up to
  30 days of metrics, and workspace usage provides current/projected cost. Application events distinguish work;
  service graphs alone are not claimed to attribute child processes independently.
- G5. One summary/minute is 1,440/day and 10,080/week. At 1 KiB/line the raw summary payload is approximately
  1.4 MiB/day and 9.8 MiB/week; other events add volume. The reviewed platform cap is 500 lines/second/replica.
  No unverified total-GB quota, unlimited-storage promise, or volume-storage charge is inferred from those numbers.
- G6. Hobby's seven-day viewing window differs from staff-reported 90-day US West storage and undeletable provider
  request metadata. The operator and visitor disclosure use [Privacy disclosure](#privacy-disclosure) consistently.
- G7. Deployment evidence reconciles queue/cache/budget summaries through computation, coalescing, cancellation,
  rejection, and rollover; confirms actual acceptance versus sample-open counts; and inspects logs for prohibited
  content and poll spam. Usage conclusions do not infer accounts or persistence demand solely from imports.

### Postconditions

- Success: The operator can explain observed utilization/refusals/cost and assess usage with bounded safe evidence.
- Minimal guarantee: Missing logs, reset counters, platform limits, or ambiguous usage never produce invented identity,
  false retention claims, or private school-data disclosure.

## Normative data

### Bundled samples

The catalog contains exactly these rows - no more, no fewer - in this order. Every sample has a matching packaged
precomputed verified feasible timetable and current-catalog definition; the listed counts describe the starting
definitions and displayed sizes are computed from the packaged documents, not separately maintained literals.
UC-3 and UC-12 consume this catalog.

| Title | Identifier | Origin | Starting lessons / classes / teachers / rooms / periods |
|---|---|---|---|
| Small school | small | Synthetic small example | 2 / 1 / 1 / 2 / 3 |
| Two-class school | two-class | Fictionalized MV5 | 42 / 2 / 12 / 13 / 35 |
| Full school | full | Fictionalized MVK | 520 / 23 / 51 / 42 / 50 |

New visitors preview Full; no other sample is implicitly loaded. Catalog fields are identifier, title, one-line
description including fictional-person notice, display order, and computed lesson/class/teacher/room/period counts.
Public sample content is immutable within a deployment and revalidates by ETag. The current catalog at specification
time is 9; packaging follows the shipped kernel's current catalog, with deliberate upgrade/verification before commit.

### Saved versions

UC-4 preserves exactly these envelope fields - no more, no fewer - under
`school-kernel.definition.v1.<identifier>` in browser local storage. Existing view-preference storage is separate.

| Field | Preserved meaning |
|---|---|
| Format version | Version 1 of the saved-definition envelope |
| Identifier | Visitor-chosen browser label |
| Saved time | Time of the explicit save, shown to the visitor |
| Derived from | Source sample ID, source saved-version ID, or opened-file origin, preserving derivation |
| Document text | Exact stored JSON text, including formatting |

Identifiers match `[a-z0-9][a-z0-9-]{0,39}`. `small`, `two-class`, and `full` are reserved. Save suggestions are the
sample ID with `-1`, the selected saved version's current ID, or an ID derived from an opened filename. One entry per
ID is replaced only after named confirmation; samples cannot be modified/deleted. There is no version history.
The panel lists saved entries newest first, shows their saved time/derivation, and stores its last-used mark locally.
Safe server origin is the recognized original sample ID or file/custom; labels/filenames never become log fields.

### Search efforts

UC-3, UC-5, UC-6, and UC-12 use exactly these presets - no more, no fewer.

| Preset | Initial actual construction/search budget | Selection rule |
|---|---|---|
| Quick search | 5 seconds, configurable | Default at or below the configured lesson threshold |
| Thorough search | 60 seconds, configurable up to 120 seconds after qualification | Default above the configured lesson threshold, initially 200 lessons |

Both initial and repair requests use this same vocabulary. A large-school Quick selection shows that it is usually
too short for this size. More time often helps but guarantees nothing; the actual configured duration is visible.
Budgets include construction/search, excluding queueing, JVM startup/stop, and verification. No separate longer repair
retry is offered. Loading a sample does not run either preset. Kernel request/result evidence must match the actual
selected effort, including a qualified 120-second Thorough budget.

### Demo limits

These are exactly the initial limits/settings - no more, no fewer in this table. Tunable values remain configurable;
launch qualifications in UC-12 govern any final measured changes. The table is consumed by UC-2 through UC-13 where
session, input, computation, cache, or operational limits apply.

| Setting | Initial effective value or rule |
|---|---|
| Replicas / solver slots | 1 / 1 |
| Workspace expiry | 60 minutes idle; 8 hours absolute; polling refreshes idle only |
| Concurrent workspaces | 50, lowered if the largest allowed workspaces fail memory qualification; refuse newcomers |
| Active visitor runs per workspace | 1 |
| Estimated-start admission threshold | 2 minutes; both this and queue count must pass for new distinct computation |
| Waiting computations | At most 8 distinct jobs, excluding the running job and coalesced subscribers |
| Queued subscriber abandonment | No run poll for 2 minutes |
| Daily visitor kernel allowance | 4 process-hours per UTC day, reserved upfront; resets on service restart/deploy |
| Standalone verification | At most 2 concurrent processes; wait at most 10 seconds for a slot |
| JSON documents | At most 2 MiB UTF-8 per definition/result; manifest remains at most 1 MiB |
| Accepted archive | At most 5 MiB compressed and 5 MiB total expanded, with per-document bounds above |
| Visitor result/verification cache | Enabled by default; configuration-file disable switch; shared 64 MiB initial ceiling |
| Visitor artifact TTL | At most 1 hour after computation/verification; fixed, never renewed by access |
| Summary interval | 1 minute, configurable |
| Starting replica resources | 2 vCPU / 3 GB; aggregate qualification required |
| Platform spend controls | $5 workspace email alert / $15 workspace hard limit |
| Public hosting | Hobby, one EU-region service, stable generated domain, sleeping off |

### Complexity envelope

UC-5, UC-6, UC-11, and UC-12 apply independent `W ≤ W_max` and `lesson_count ≤ L_max` gates after bounded parsing
and before expensive kernel work. Recheck the compiled successor if a repair changes relevant counts. Byte limits
remain independent, even on a cache hit. Both gates are inclusive; one unit above either configured maximum is refused.

W uses all declared planning periods and rooms as a conservative placement-domain proxy, not inferred availability
or favorable pin constraints. Lesson-pair diagnostics justify a separate lesson cap. Cohort and teacher counts are
benchmark/observability dimensions, not additional unagreed quotas.

Exactly these reference rows - no more, no fewer - establish the arithmetic examples; they are not additional samples.

| Input | Lessons | Planning periods | Rooms | W |
|---|---:|---:|---:|---:|
| Small | 2 | 3 | 2 | 12 |
| Two-class | 42 | 35 | 13 | 19,110 |
| Full | 520 | 50 | 42 | 1,092,000 |
| Larger-school reference | 2,160 | 50 | 80 | 8,640,000 |
| Larger-school reference with more periods | 2,160 | 60 | 80 | 10,368,000 |

Starting benchmark settings are **W_max = 11,000,000** and **L_max = 2,500**. These are provisional inputs to UC-12,
not a claim that the target is already supported. The target is up to 12 grades × 5–6 parallel cohorts (60–72 total),
about 100 teachers, 70+ rooms, and roughly 2,000–2,500 lessons/week. For example, 2,500 × 50 × 100 = 12,500,000
fails the starting W limit despite passing L_max. Final advertised combinations require measured qualification.

### Admission and kernel-time accounting

UC-5, UC-6, UC-7, UC-8, UC-9, UC-11, UC-12, and UC-13 obey these rules whenever kernel work is required.

1. Check workspace ownership, lifecycle/version, safe parse, and effective size limits first. A cached hit or existing
   computation may then be reused when enabled. No per-address quota or counter exists.
2. For new distinct solve work, both estimated-start and distinct-queue gates must pass. Compute the estimate from
   remaining running effort, queued distinct work, and observed process/verification overhead; it remains approximate.
3. Reserve a conservative maximum of process elapsed time for the solve plus required verification before queueing;
   standalone verification reserves its own maximum before admission. Bounds include startup, enforced watchdog,
   and termination/cleanup time, not merely search effort or typical observed overhead.
4. `remaining = daily allowance − consumed − reserved`, floored at zero for admission. Consumed includes accrued
   elapsed process time; reserved is the still-unspent maximum held for queued/running work. Accruing time transfers
   it from reserved to consumed rather than counting it twice. Overlapping visitor process lifetimes are summed;
   queue/slot waiting without a process does not count. Startup packaged-sample verification is deployment work and
   excluded; every visitor-triggered kernel command, including unsuccessful work, is included.
5. Settle actual elapsed process time and release unused reservation on completion/cancellation or pre-start refusal.
   Do not release a
   running process's reservation merely because its subscriber detached. Already-admitted verification retains its
   allowance; another job cannot consume it. Reuse/coalescing reserves no duplicate allowance.
6. At UTC rollover, attribute actual elapsed time to the UTC day in which it occurs, carry still-needed outstanding
   reservations into the new allowance, and do not grant the same allowance twice. Summaries expose the current
   day's consumed/reserved/remaining values; restart explicitly resets consumed/reserved to zero and remaining to
   the full daily allowance.
7. Exhaustion refuses required new kernel work before queueing/starting, rather than accepting work that cannot
   finish verification. Available cache/packaged use, inspection, and accepted export remain available. Allowance can
   recover through unused reservation release or UTC reset; restart resets are deliberately accepted.
8. New solve gate or budget refusals are 429 with safe reasons (queue refusals also Retry-After). Verification slot
   exhaustion after at most 10 seconds is 503 with a busy message. Existing invalid-workspace-transition responses
   remain for an active run or invalid lifecycle. No refusal changes the draft or exposes another owner.

Reservations and the daily cap are best-effort process control, not a durable monthly cost ceiling. CPU/RAM billing,
egress, cache/session memory, and restart resets make platform spend control necessary.

### Reuse identity and retention

UC-3, UC-5, UC-6, UC-8, UC-9, UC-11, UC-12, and UC-13 consume these exact identity dimensions - no more, no fewer
for the named operation's required correctness. Incidental private labels/ownership cannot substitute for any of them.

| Operation | Complete server-computed identity |
|---|---|
| Initial solve | Complete initial definition; operation; actual effort duration; seed and effective solver configuration; kernel build |
| Repair solve | Complete accepted baseline definition and result; complete compiled successor including availability/pins; operation; actual effort duration; seed and effective solver configuration; kernel build |
| Verification | All complete documents supplied to verification, including a result when applicable; exact verification mode; kernel build |

Canonical object formatting may ignore object-key order/whitespace, but preserves array order and every supported
value/omission. A definition revision that sorts arrays is not sufficient cache identity. Request identity is computed
by the server, never accepted solely from a submitted hash, FEASIBLE label, or timetable revision. Manifest/proposal/
owner/version checks still run independently; kernel evidence does not validate workspace metadata by itself.

Reuse is default-on and configurable off in the application configuration file for both result and verification
reuse; packaged startup evidence remains usable. Eligible identical in-flight solves coalesce when reuse is enabled.
Always use an available eligible verified feasible artifact; no visitor force-fresh bypass exists. Negative no-feasible
outcomes, invalid input, cancellations, crashes, timeouts, and transport failures are not cached. Verification caches
only success.

The total resident visitor-cache allowance counts retained keys/complete input and result documents/evidence, not only
entry count. Fixed TTL starts when computation/verification completes, never extends on reads, and is at most one
hour. Memory pressure evicts earlier; an artifact too large for the ceiling is not retained. A solve artifact and its
required successful verification evidence form one eligible reuse unit: absent/expired evidence makes the artifact
ineligible for a hit. Restart clears all visitor entries.
Packaged samples/evidence are maintained independently and cannot be evicted by visitor traffic. Disabling reuse
prevents visitor hits/coalescing; it does not invalidate a visitor's already accepted baseline.

### Accepted-bundle archive

UC-10 and UC-11 use exactly these root entries - no more, no fewer. Separate definition/result import may omit a
manifest, with imported locks treated as persistent policy; an archive requires all three entries.

| Entry | Preserved content |
|---|---|
| school-definition.json | Complete exact accepted definition |
| timetable-result.json | Complete exact matching verified feasible result |
| workspace-manifest.json | Supported schema/catalog versions, accepted revisions, and policy/attempt-pin provenance |

Unsafe paths, directories, duplicates, extra/missing entries, ambiguous mixed import modes, malformed/duplicate-key
JSON, trailing documents, unsupported versions, and compressed/expanded/document size violations are refused. Safe
parse refusals precede expensive verification and do not replace an existing workspace. Export excludes draft,
proposal, browser-version, identity/cookie, and other-session content.

### Proposal change categories

UC-8 presents exactly these rows - no more, no fewer - including explicit zeros and existing overlap semantics.

| Category | Meaning |
|---|---|
| Additions | Lesson IDs only in the proposed timetable |
| Cancellations | Lesson IDs only in Current |
| Teacher changes | Common lesson IDs whose teacher differs, independently of assignment movement |
| Forced moves | Common lesson dimensions changed by authoritative locks |
| Period moves | Common non-forced assignments whose period changes, with old/proposed period and room |
| Room-only moves | Common non-forced assignments whose period stays and room changes |

### Application events

UC-13 requires exactly these event families - no more, no fewer as product/operational categories; multiple safe
event names may implement a family. All visitor use cases retain the prohibition on private/personal payloads.

| Family | Required safe information |
|---|---|
| Startup | Build, effective limits, search/process budgets, cache config, sample verification outcomes, counter reset |
| Session lifecycle | Creation/expiry counts and safe end reason, without session identifiers |
| Definition/sample use | Sample identifier or custom definition and safe sample/file origin; counts/complexity buckets and verification outcome |
| Work lifecycle | Admission/refusal reason, queue/start/finish/cancel, operation, preset, outcome, wait, solve and verification durations, computation/correlation IDs |
| Reuse | Hits/misses/coalescing, logical request versus physical computation, original computation provenance |
| Explicit decisions and files | Actual generated-proposal acceptance, manual publication, accepted export, bundle import; sample opens remain distinct |
| Periodic aggregate | Current workspaces/queue/processes/cache/allowance and interval activity counts |

Periodic aggregates contain exactly these field groups - no more, no fewer as required groups. No school/cookie/
address/session-ID field is permitted.

| Group | Required values |
|---|---|
| Context | Timestamp, build/config context, interval and current UTC allowance day/reset context |
| Workspaces | Active count and interval creation/expiry counts |
| Queue | Distinct waiting count and interval admissions/refusals by reason |
| Processes | Running solve and verification counts; interval completions/outcomes/errors |
| Cache | Entries and resident bytes; interval hits/misses/coalesced requests |
| Allowance | Consumed/reserved/remaining process time with interval settlement/reset information |
| Product use | Interval sample/custom-origin use, explicit acceptances/manual publications, exports/imports, and anonymous conversion aggregates |

Logical-request outcomes and wait durations support p50/p90 review; original cached solve duration is not a new
physical run. The log format is single-line structured JSON. No per-poll application events, personal names, school
IDs/display names, raw input/result, IPs, cookies, session IDs, or browser labels are logged. Safe sample identifiers
are catalog IDs only. Summaries aggregate session behavior in memory without emitted visitor identity.

### Privacy disclosure

UC-2, UC-3, UC-4, UC-12, and UC-13 distinguish exactly these retention scopes - no more, no fewer in the disclosure.

| Scope | Required disclosure |
|---|---|
| Workspace and process files | In-memory workspace expires by idle/absolute limit and every restart/deploy; private temporary process files are cleaned up; unfinished repair/manual work is lost |
| Shared visitor artifacts | Complete identical inputs/results and successful verification may be retained up to one hour after computation, bounded by memory/eviction, independently of originating session; restart clears them |
| Browser versions | Stored on this origin/browser/device until removed/evicted; shared computers expose them to other users of that browser; download is the durable copy |
| Provider logs | Railway edge metadata includes IP, user agent, and URL and cannot be disabled/redacted; Hobby viewing is seven days while staff report underlying storage for 90 days, deletion only afterward, in US West even for EU services |

Visitors are asked not to submit personal data they are not permitted to share. Documents/personal details do not
belong in URLs. Application logs exclude school/private payloads and only the necessary application session cookie
is used; no analytics/tracking cookie is introduced. Railway retention facts are the checked-at-specification policy
documented with sources in [proposal observability](proposal.md#observability); changes to that policy require an
accurate disclosure update rather than an assumption of EU-only or session-only retention.

### Capacity qualification

UC-12 performs real cold computations on the deployed configured service with reuse disabled. Packaged hits are not
benchmark evidence. Qualification covers exactly these dimensions - no more, no fewer as required measurement groups.

| Dimension | Observable qualification |
|---|---|
| Bundled search | Quick yields verified feasible results for Small and Two-class in 5/5 consecutive cold runs; Thorough does so for Full in 5/5 |
| Larger-school target | Representative fictional 60–72-cohort schools around 100 teachers, 70+ rooms, and 2,000–2,500 weekly lessons complete construction and verified feasible search at qualified Thorough, at most 120 s |
| Complexity boundary | Both inclusive thresholds and one-over cases, including small-byte/large-work inputs; compiled repairs and bundle imports cannot bypass gates |
| Process bounds | Observe startup/search/verification/termination/cleanup and prove conservative reservations with enforced maxima, failures, cancellation, and simultaneous work |
| Aggregate memory | Full visitor cache and largest permitted workspaces up to configured ceiling plus one large solve/two standalone verifications fit measured container headroom |
| Public journey and ownership | Real packaged kernel and isolated browsers complete sample/custom/repair/manual/export/import/expiry paths and reject foreign/stale requests |
| Platform boundary and spend | Configured host/scheme/health protections, service graphs, workspace alert/hard limit, and actual idle/load/egress cost evidence |
| Default-profile regression | Existing local persistence, structured authoring, planning/repair, and restart behavior remain valid |

Final effective values are recorded with build, deployment configuration, representative definition dimensions,
outcomes, raw durations/memory, and the qualifying boundaries. If the intended school target cannot pass at 120 s,
capacity is explicitly reconsidered before launch; no quiet reduction or cached fixture substitutes for it.

## Out of scope

- Accounts, durable school/workspace storage, a database in the demo, or a usage ledger surviving restarts.
- Multiple replicas, autoscaling, zero-loss deployments, uptime guarantees, or production school operation.
- Structured authoring, catalog-upgrade UI, seeded authoring repair, manual teacher changes, or draft migration into
  definition editing.
- Budgets beyond qualified Thorough, a separate two-minute retry, public force-fresh, seed/step controls, or per-IP
  quotas/counters.
- Durable unfinished repair/manual work, saved-version history, cross-device version sync, share links, or shared
  live workspaces. Exchanging definition JSON or accepted result files remains supported.
- A hosted solver, separate metrics backend, Pro-only monitoring, payment/usage plans, or identity analytics.
- Extra bundled/visitor-contributed samples or deletion of real-name source examples from repository history/tests;
  excluding those examples and their names from the shipped image is in scope.
- New mobile editing/acceptance capabilities or changes to kernel scheduling meanings beyond UC-1.

## External dependencies

- Railway Hobby service/domain/account and the operator's workspace spending configuration must be available for
  UC-12/UC-13 deployed evidence. Without them, public launch is unqualified; local tests cannot substitute for it.
- Platform log storage/edge behavior and Hobby limits follow the checked provider policy linked from the proposal.
  A material provider-policy change requires refreshed evidence/disclosure; the default remains the policy recorded
  here, with no Pro purchase or durable telemetry service assumed.
- Browser local-storage capacity/eviction policies are external. Failures follow UC-4's preserve-text/download path;
  the application makes no guaranteed browser-retention duration or fixed saved-version-count promise.
- Representative larger-school fixtures and current scored sample results must exist before launch qualification.
  Their absence blocks that measurement/launch outcome, not specification completeness or unrelated use cases.
