# Proposal: Hard cohort daily lesson spread

Catalog 4 lets each cohort declare `maxDailyLessonSpread`, but only as a preference. MVK sets it to one lesson for
every cohort, and a normal 30-second catalog 6 run still publishes timetables that ignore it. Seed 0 gives 5B
`2,5,3,6,4` lessons from Monday to Friday and 2A `6,4,3,5,4`. On 2A's quiet Wednesday the last lesson ends at 10:25.
Seven of 23 cohorts exceed the target.

Weight cannot fix this. Raising the weekly-balance weight to the supported maximum of 1,000,000 still left four to
six cohorts over target across three seeds, and late starts rose from zero to about 25. The pattern matches the one
catalog 5 addressed for gaps: a soft penalty ranks below every hard rule, and a bounded search may stop before it
pays the penalty down.

For younger cohorts an even week is a care arrangement as much as a quality measure. A predictable school day lets
families plan pick-up and lets the school add after-school activities. Catalog 7 therefore adds a hard per-cohort
`dailyLessonSpreadLimit` alongside the existing preference. MVK limits grades 1 through 4 to one lesson and grades 5
through 9 to two, and keeps `maxDailyLessonSpread: 1` everywhere as the target within that limit.

The tighter timetable needs more search. MVK plans feasibly in all five seeds at 60 seconds but in about one of three
at 30 seconds. The kernel keeps its 30-second default, and the workspace moves its normal run to one minute.

The first delivered configuration balanced every week but let some cohort days start at 13:50 and run to 17:15:
5B `4,4,4,4,4` with a Wednesday from period 6, 6B with a Thursday from period 5. Only grades 1 through 3 had a hard
start bound. Beneath the late starts sat a resource limit: MVK's single music room serves 29 lessons and was full on
every weekday from period 2 to 5 in every run, so some cohort had to take music in the afternoon. MVK now bounds
grades 4 through 9 at start slot 4 (11:50) and lets classroom K2 host music as well. A start bound of slot 3 was not
reached in 60 or 120 seconds, even with the second music room.
