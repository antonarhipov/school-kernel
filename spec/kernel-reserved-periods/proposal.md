# Proposal: Reserve school periods from ordinary scheduling

MVK calls the first chronological slot of each weekday period `0` and uses it only for exceptional cases. Ordinary planning and repair must not assign lessons there. The school needs an explicit list of reserved period IDs in its definition, rather than a rule inferred from ID text, display names, clock times, or order numbers. For MVK the list is `mon-0`, `tue-0`, `wed-0`, `thu-0`, and `fri-0`.

A reserved period remains a declared timetable slot, but is outside the ordinary lesson scheduling range. It must not become a cohort gap, teacher gap, available teaching day, or one of the three regular slots used by the current cohort late-start measure. The feature must preserve existing definitions and accepted results without reservation settings. A repair may move an accepted assignment out of a newly reserved period, while retaining the accepted predecessor unchanged. If no legal timetable is found without reserved periods, report failure and publish no candidate.

Rules allowing specific cohorts or lessons to use a reserved period, and per-cohort or age-based latest-start targets, are deferred.
