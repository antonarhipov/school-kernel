# Proposal: Cohort daily lesson balance

The MVK school wants each cohort's lessons distributed more evenly across its available weekdays. In a normal
30-second run of `examples/mvk.json`, 6B has `2,5,5,3,4` lessons from Monday through Friday. Its busiest and quietest
days differ by three lessons; a spread of at most two, such as `3,4,5,3,4`, would be preferable. The same run gives
9C a spread of five.

The school needs a cohort-level maximum daily lesson spread in its definition. It is a scheduling preference: a
bounded search may return a feasible timetable that exceeds the target, with the excess visible in the existing
weekly-balance score row. The setting must be usable by initial planning and repair, must not reinterpret previously
accepted definitions or results, and must keep the existing hard and repair-stability priorities.

For MVK, the target is two lessons for every cohort. The existing gap and late-start priorities remain stronger than
one weekly-balance match; the weekly-balance weight may be raised from its current value of one to give the new
target a useful effect within the ordinary-preference score.
