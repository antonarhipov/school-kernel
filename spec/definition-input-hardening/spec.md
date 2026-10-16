# Definition input hardening

## Feature summary

Visitors can safely edit and check demo school definitions without excessive request concurrency, malformed values
or failed browser storage damaging existing work or exposing internal error details.

## Scope and resolved decisions

- Implements the user's approved recommendations 1–5 and 8 only. No public deployment is authorized here.
- Demo input limits supplement existing document-byte and placement-work limits; they do not change the headless
  kernel's school-definition contract or the normal local workspace's school-size limits.
- The four existing bundled schools and all their optional fields remain supported without changes.
- Admission is immediate, without a waiting queue: at most four input-bearing requests globally and two per session.
  This is separate from solver and verifier admission. Excess requests receive 429 with a retry hint before decoding.
- Definition arrays allow at most 512 subjects, rooms or periods; 1,024 teachers or cohorts; the configured lesson
  ceiling (currently 2,500); 4,096 room policies; seven preference overrides; and 2,048 entries per reference list.
- Every definition string is at most 512 UTF-16 code units; field names are at most 128; objects at most 32 fields.
  Parsing permits depth 64, strings up to 1 MiB, numbers up to 100 characters and at most 150,000 tokens per document.
- Schema checking stops on the first error. Diagnostic locations are retained; invalidity is never treated as success.
- Editor autosaving waits 300 ms after the last input, checks the 2 MiB UTF-8 limit before a storage write, and flushes
  eligible pending edits on explicit editor close, navigation within the page, save and page exit. Oversized text stays
  in the open editor with a visible explanation while the previous durable editor copy remains intact.
- Routine limits are conservative initial safety bounds, not new claims of production capacity qualification.

## Actors and domain terms

- Visitor: a person using the demo in one browser session.
- Existing work: all saved schools, timetables, drafts and server workspace contents preceding a request.
- Input-bearing request: a definition check/use, timetable review, accepted-bundle import or definition upload.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Safely edit and check a school definition | Visitor | None |

## UC-1 - Safely edit and check a school definition (primary)

- Goal: Keep an edited school copy after a bounded, trustworthy check.
- Primary actor: Visitor
- Supporting actors: none
- Trigger: The visitor opens the demo school-definition editor or submits school data directly.
- Preconditions: The demo and an active private session are available.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The visitor opens a bundled school's definition and edits a copy.
2. Eligible editor text is retained in this browser after a short debounce; closing or reloading retains pending text.
3. The visitor names and submits the copy. Admission occurs before any input decoding.
4. The complete definition is checked within the size, shape, parsing, schema and semantic limits.
5. A separate browser-local school copy is available with exactly the submitted definition, without starting a solve.

### Extensions

- 2a. Text exceeds the editor byte limit. Explain that it is retained only in the open editor, skip persistence and
  preserve the previous durable text; resume at step 1.
- 2b. Storage fails or another tab changes it. Explain the failure, retain open editor text and prior durable work,
  and allow a later retry without repeatedly attempting storage on every keystroke; resume at step 1.
- 3a. Global or session admission is full. Refuse immediately with 429 and a retry hint, without reading the body,
  starting a kernel process or changing existing work; resume at step 3.
- 4a. Parsing, field size, collection size, schema, canonicalization or semantic validation fails. Return a bounded
  client-error response, preserve existing work and locate the error where available; resume at step 1.
- 4b. An unexpected internal error occurs. Return a fixed public explanation and correlation identifier, without
  exception details or submitted text; existing work remains unchanged; resume at step 3.
- 4c. The editor changes while checking. Refuse to save stale content and retain the latest text; resume at step 3.

### Guarantees

- G1. Host, origin, CSRF, session ownership, version guards and existing solver/verifier bounds remain in force.
- G2. Rejected input neither changes existing work nor launches verification after an earlier parsing/shape refusal.
- G3. Every admitted slot is released after success, refusal, interruption or unexpected failure. Safe reads and
  cancellation remain usable while input slots are occupied.
- G4. Definition names and SQL-like/HTML-like text remain data; accepted strings and optional values are preserved.
- G5. Browser storage failures never repeatedly serialize oversized text; eligible edits recover when storage recovers.
- G6. The rendered editor and its feedback remain usable at desktop and 390px widths. Real browser evidence and
  isolated production HTTP checks own approval, with deterministic concurrency tests supplementing them.

### Postconditions

- Success: A separate, checked school copy is stored without modifying its source or starting a solve.
- Minimal guarantee: Refusal/failure discloses no internal exception text, preserves prior work, and releases admission.

## Out of scope

PostgreSQL persistence/ownership, Unicode compatibility policy for a future database, cache privacy changes, accounts,
new solver budgets, public redeployment and distributed admission across replicas.
