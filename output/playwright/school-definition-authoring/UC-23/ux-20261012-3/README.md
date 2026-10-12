# UC-23 attempt 3 — FAIL

Real isolated Chrome at18112, build authoring-ux-forms-r2, saved237 unchanged. The first availability-mode change closed its expanded group before focus could be restored. Native details toggle events are queued, allowing a redraw before expansion state reaches the Map. Timeout retained in journey.log; subsequent diagnostic confirmed hidden mode and document-body focus. No save occurred. Screenshot records subsequent inspection with group reopened; it is not the original failure instant.

Correction: capture actual group expansion synchronously before render; ignore toggle events from detached groups. Added real-browser focus/expansion regression. Full changed-build repeat required.
