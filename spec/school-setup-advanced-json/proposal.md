# Complete school documents and Advanced JSON in Setup

## Problem and intended outcome

The timetable's Utilities panel exposes an independent kernel JSON editor, school-copy save and generation workflow alongside School setup. The kernel document omits authoring information such as unassigned demand, extended eligibility and named non-teaching intervals. Moving that independent buffer would preserve competing workflows and lossy exchange.

Forms and Advanced JSON will edit one complete, versioned school document. The document represents every definition and constraint available in Setup, including incomplete demand, interval labels and times, lunch, teacher qualification/class eligibility, optional values and explicit empty sets, room policies, individual occurrences and consecutive-block rules. Solver input is generated internally. UI navigation, storage identity and validation/check provenance are not editable school settings.

## Agreed behavior

School setup is available only in the desktop layout (above 700 CSS pixels) for now. Mobile hides its entry; resizing to mobile closes the editor and preserves unfinished work. Advanced is collapsed by default inside the existing animated Setup overlay. JSON changes stay in a durable draft-specific buffer until Apply to setup. Syntax, duplicate-key and structural failures refuse Apply without changing the school. Structurally valid incomplete or semantically inconsistent school data can be applied as draft work, with precise field paths and actionable issues. School saving and generation require full authoritative validation; readiness does not prove schedulability.

Unapplied JSON must be explicitly applied or discarded before forms can be edited. Closing or reloading preserves unfinished JSON. Forms and JSON share one school-copy save lifecycle. Bundled schools remain immutable and editing produces a browser-local copy.

Existing kernel JSON remains importable through a conversion preview explaining defaults and missing authoring information, with confirmation before adoption. Unsupported data is refused explicitly. New exports use the complete school format and retain incomplete work. Existing recovery files remain supported.

Remove Utilities from the timetable. Generation controls, effort, progress and cancellation live in Setup Review. Advanced contains JSON and import/export, draft recovery and a separate confirmed browser-local cleanup action. A compact active-generation indicator reopens the appropriate Setup Review. The timetable remains the clean landing screen with bundled precomputed schools and the left-side School setup entry.

## Boundaries

This supersedes the separate advanced-buffer exchange and Utilities placement in the school-setup-editor proposal. Existing solver contracts, public-origin security, bundled artifacts, local-only persistence, cleanup rollback/epoch guards, save-copy semantics and candidate naming remain authoritative. No deployment, account system, backend persistence or solver redesign is included.
