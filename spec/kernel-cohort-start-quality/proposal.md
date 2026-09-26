# Proposal: Cohort day-start quality

The administrator wants every cohort with lessons on a day to begin no later than that day's third declared class slot. A 30-second initial plan of `examples/mvk.json` with no internal cohort gaps still has seven cohort-days beginning late: four at slot four, one at slot five, and two at slot six. The start position is a distinct quality measure that must be visible in the planning result and influence the search.

The measure applies to the earliest assigned lesson for each cohort and weekday with at least one lesson. A day with no assigned lessons has no first class and is excluded. The third slot refers to the third declared period of the school day, ordered by the definition's period order, so nonconsecutive order values do not change the threshold. Existing catalog versions and accepted definition/result pairs remain readable. New planning and repair results identify a successor catalog and report the new measure alongside the existing quality rows.

The administrator currently values eliminating internal cohort gaps and timely cohort starts above preferred-room placement. The relative priority of internal gaps and late starts is being resolved before the behavior contract is finalized.
