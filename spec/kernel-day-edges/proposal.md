# Proposal: Cohort start bounds and day-edge subjects

The late-start preference has one school-wide threshold, the third regular slot, and it is only a preference. MVK
needs its youngest cohorts, grades 1 through 3, to start every taught day by the second slot without exception. Other
cohorts may later need their own thresholds. Each cohort therefore needs two optional values: a hard latest start
slot that a feasible timetable must respect, and a preferred latest start slot that tunes the existing preference.

MVK also has a subject that does not fit the ordinary lesson shape. Õpiabi (learning support) may sit in the reserved
slot 0, may end a cohort's day, and must never sit in the middle of it. A cohort has at most one Õpiabi lesson per day
and at most one in slot 0 per week. Slot 0 is reserved school-wide, and the reserved-periods feature explicitly
deferred exceptions, so this is the first one.

Catalog 6 adds both capabilities as generic definition fields: cohort start bounds and subject placement rules. It
does not hard-code MVK's subject. Catalogs 1 through 5 keep their meanings, and workspace repair compiles catalog 6
successors.
