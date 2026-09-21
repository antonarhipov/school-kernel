package org.schoolkernel.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.contract.CurrentTimetableReader.Assignment;
import org.schoolkernel.contract.CurrentTimetableReader.CurrentTimetable;

class BaselineVerifierTest {
    @Test
    @DisplayName("RULE-26: direct baseline verifier independently rejects every hard catalog row")
    void rejectsEveryHardConstraintMutation() {
        List<Case> cases = List.of(
                collision("hard.teacher-period", "t1", "c2", "r2"),
                collision("hard.cohort-period", "t2", "c1", "r2"),
                collision("hard.room-period", "t2", "c2", "r1"),
                single("hard.teacher-availability", Set.of("p2"), Set.of("p1", "p2"),
                        Set.of("p1", "p2"), 20, Set.of("lab"), Set.of(), null, null),
                single("hard.cohort-availability", Set.of("p1", "p2"), Set.of("p2"),
                        Set.of("p1", "p2"), 20, Set.of("lab"), Set.of(), null, null),
                single("hard.room-availability", Set.of("p1", "p2"), Set.of("p1", "p2"),
                        Set.of("p2"), 20, Set.of("lab"), Set.of(), null, null),
                single("hard.room-capacity", Set.of("p1", "p2"), Set.of("p1", "p2"),
                        Set.of("p1", "p2"), 10, Set.of("lab"), Set.of(), null, null),
                single("hard.room-capability", Set.of("p1", "p2"), Set.of("p1", "p2"),
                        Set.of("p1", "p2"), 20, Set.of(), Set.of("lab"), null, null),
                single("hard.period-lock", Set.of("p1", "p2"), Set.of("p1", "p2"),
                        Set.of("p1", "p2"), 20, Set.of("lab"), Set.of(), "p2", null),
                single("hard.room-lock", Set.of("p1", "p2"), Set.of("p1", "p2"),
                        Set.of("p1", "p2"), 20, Set.of("lab"), Set.of(), null, "r2"));

        BaselineVerifier verifier = new BaselineVerifier();
        for (Case mutation : cases) {
            ValidationReport report = verifier.verify(mutation.definition(), mutation.timetable());
            assertTrue(report.errors().stream().anyMatch(error -> error.message().contains(mutation.constraintId())),
                    mutation.constraintId() + ": " + report.errors());
        }
    }

    private static Case collision(String constraint, String secondTeacher, String secondCohort, String secondRoom) {
        SchoolDefinition definition = definition(
                Set.of("p1", "p2"), Set.of("p1", "p2"), Set.of("p1", "p2"),
                20, Set.of("lab"), Set.of(), null, null,
                List.of(
                        new SchoolDefinition.Lesson(
                                "l1", "Lesson 1", "s", "c1", "t1", null,
                                Set.of(), Set.of(), Set.of(), null, null),
                        new SchoolDefinition.Lesson(
                                "l2", "Lesson 2", "s", secondCohort, secondTeacher, null,
                                Set.of(), Set.of(), Set.of(), null, null)));
        return new Case(constraint, definition, timetable(List.of(
                assignment("l1", "c1", "t1", "r1"),
                assignment("l2", secondCohort, secondTeacher, secondRoom))));
    }

    private static Case single(
            String constraint,
            Set<String> teacherAvailability,
            Set<String> cohortAvailability,
            Set<String> roomAvailability,
            int roomCapacity,
            Set<String> roomCapabilities,
            Set<String> requiredCapabilities,
            String periodLock,
            String roomLock) {
        SchoolDefinition.Lesson lesson = new SchoolDefinition.Lesson(
                "l1", "Lesson 1", "s", "c1", "t1", null,
                requiredCapabilities, Set.of(), Set.of(), periodLock, roomLock);
        return new Case(
                constraint,
                definition(
                        teacherAvailability, cohortAvailability, roomAvailability,
                        roomCapacity, roomCapabilities, requiredCapabilities, periodLock, roomLock,
                        List.of(lesson)),
                timetable(List.of(assignment("l1", "c1", "t1", "r1"))));
    }

    private static SchoolDefinition definition(
            Set<String> teacherAvailability,
            Set<String> cohortAvailability,
            Set<String> roomAvailability,
            int roomCapacity,
            Set<String> roomCapabilities,
            Set<String> ignoredRequiredCapabilities,
            String ignoredPeriodLock,
            String ignoredRoomLock,
            List<SchoolDefinition.Lesson> lessons) {
        Set<String> allPeriods = Set.of("p1", "p2");
        return new SchoolDefinition(
                1, 1, "school", "School", null,
                List.of(new SchoolDefinition.Subject("s", "Subject")),
                List.of(
                        new SchoolDefinition.Teacher("t1", "Teacher 1", Set.of("s"), teacherAvailability, Set.of()),
                        new SchoolDefinition.Teacher("t2", "Teacher 2", Set.of("s"), allPeriods, Set.of())),
                List.of(
                        new SchoolDefinition.Cohort("c1", "Cohort 1", 20, cohortAvailability, Set.of()),
                        new SchoolDefinition.Cohort("c2", "Cohort 2", 20, allPeriods, Set.of())),
                List.of(
                        new SchoolDefinition.Room("r1", "Room 1", roomCapacity, roomCapabilities, roomAvailability),
                        new SchoolDefinition.Room("r2", "Room 2", 20, Set.of("lab"), allPeriods)),
                List.of(
                        new SchoolDefinition.Period("p1", "Period 1", DayOfWeek.MONDAY, 1, null, null),
                        new SchoolDefinition.Period("p2", "Period 2", DayOfWeek.MONDAY, 2, null, null)),
                lessons,
                KernelCatalog.defaultSoftWeights());
    }

    private static CurrentTimetable timetable(List<Assignment> assignments) {
        return new CurrentTimetable(1, "school", "revision", "timetable", assignments);
    }

    private static Assignment assignment(String lesson, String cohort, String teacher, String room) {
        return new Assignment(lesson, "s", cohort, teacher, "p1", room);
    }

    private record Case(String constraintId, SchoolDefinition definition, CurrentTimetable timetable) {}
}
