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

The next follow-up shows that the tighter daily-load target can leave empty periods between a cohort's first and last
lesson. The school treats these within-day holes as far more disruptive than the other ordinary preferences. MVK
therefore uses the largest supported weight for the existing cohort-gap penalty, while keeping it soft so that a
hard-feasible candidate remains publishable when a gap cannot be avoided. Empty time before the first lesson or after
the last lesson is not a cohort gap.
