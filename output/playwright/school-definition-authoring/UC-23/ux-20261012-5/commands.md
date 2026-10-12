# Reproduction commands

Broad affected suite (initial42, failures retained in attempt1; corrected repeat22):

```sh
mvn -pl timetable-workspace -am verify '-Dtest=*Test' -Dsurefire.failIfNoSpecifiedTests=false '-Dit.test=DefinitionAuthoringUxBrowserIT,DefinitionTeachersBrowserIT,DefinitionRequirementsBrowserIT,DefinitionCohortsBrowserIT,DefinitionReviewBrowserIT' -Dfailsafe.failIfNoSpecifiedTests=false
```

Final focused verification after focus fix and after presentation polish (each33+2 PASS):

```sh
mvn -pl timetable-workspace -am verify '-Dtest=*Test' -Dsurefire.failIfNoSpecifiedTests=false '-Dit.test=DefinitionAuthoringUxBrowserIT' -Dfailsafe.failIfNoSpecifiedTests=false
mvn -pl timetable-workspace -am package -DskipTests
```

Initial broad42-case command additionally includes DefinitionSubjectsBrowserIT,DefinitionRoomsBrowserIT,DefinitionAuthoringBrowserIT,SavedDefinitionDraftBrowserIT. Their unaffected20 PASS cases remain applicable. Final focused assertions were strengthened with actual focus/expansion checks.

CLI uses installed cached `@playwright/cli` via node, Chrome, a named isolated session and `run-code` with journey.js contents. Main journey targets18112 disposable school; operator supplement targets8080 with read-only navigation and no saves.
