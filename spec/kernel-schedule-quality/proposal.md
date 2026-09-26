# Proposal: Class timetable quality

The administrator wants generated and repaired timetables to avoid idle periods inside a class day and to distribute a class's lessons reasonably evenly across its available weekdays. These are default preferences, not new feasibility rules. Schools may reduce or disable either preference through the existing soft-weight mechanism.

The MV5 case is the reference regression: an accepted 5A timetable has seven Monday lessons and one Friday lesson in the first period. After all Monday periods become unavailable to Ene Avson, her 5A lesson must move. The observed proposal put it in Friday's seventh period, leaving five 5A gaps, even though Friday's second period and the preferred room were free. The same proposal needlessly changed an unrelated 5B room. A repair should prefer the adjacent Friday slot among equally stable feasible choices and retain unrelated assignments when search can do so.

Existing catalog version 1 results and accepted workspace baselines must remain readable. New planning and repair results use a new catalog version with explicit class-gap and weekly-balance penalty rows. The accepted timetable remains unchanged until a proposal is explicitly accepted.
