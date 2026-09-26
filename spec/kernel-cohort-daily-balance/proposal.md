# Proposal: Cohort daily lesson balance

The MVK school wants each cohort's lessons distributed more evenly across its available weekdays. In a normal
30-second run of `examples/mvk.json`, 6B has `2,5,5,3,4` lessons from Monday through Friday. Its busiest and quietest
days differ by three lessons; a spread of at most two, such as `3,4,5,3,4`, would be preferable. The same run gives
9C a spread of five.

The school needs a cohort-level maximum daily lesson spread in its definition. It is a scheduling preference: a
bounded search may return a feasible timetable that exceeds the target, with the excess visible in the existing
weekly-balance score row. The setting must be usable by initial planning and repair, must not reinterpret previously
accepted definitions or results, and must keep the existing hard and repair-stability priorities.

The first delivered MVK configuration used a target of two and a weekly-balance weight of four. The follow-up asks
for the tightest practical daily load and more even starts. This revision sets the cohort target to one and gives
weekly balance and late starts greater weight, while retaining explicit gap, teacher-gap, and room preferences.
It remains a data-only tuning of existing soft preferences: exact variation among starts in the first three regular
periods is not scored by the current catalog.
