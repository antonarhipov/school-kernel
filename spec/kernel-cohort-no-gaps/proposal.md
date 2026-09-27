# Proposal: Gap-free cohort days

Catalog 4 made the cohort-gap penalty the heaviest ordinary preference MVK can express, yet timetables still show
empty periods between a cohort's first and last lesson on some days. A soft penalty ranks below hard feasibility and
repair stability, and a bounded search may stop before it removes every gap. Weight alone cannot promise "never".

The school wants cohort gaps to be absent unless the definition explicitly allows them. Catalog 5 therefore adds a
hard constraint, `hard.cohort-gap`, and an optional per-cohort `maxDailyGaps` allowance that defaults to zero. When
no timetable within the allowance is found, the kernel reports an unsuccessful search instead of publishing gaps.

Catalogs 1 through 4 keep their meanings, so previously accepted results that contain gaps remain verifiable. The
workspace compiles repair successors under catalog 5, so repairs close existing gaps.
