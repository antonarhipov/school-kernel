package org.schoolkernel.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.schoolkernel.contract.CurrentTimetableReader;
import org.schoolkernel.solver.PeriodValue;
import org.schoolkernel.solver.PlanningLesson;
import org.schoolkernel.solver.PlanningMapper;
import org.schoolkernel.solver.RoomValue;
import org.schoolkernel.solver.ScheduleEvaluator;

/** Direct, non-solving validation of a complete definition/result pair. */
public final class BaselineVerifier {
    private final PlanningMapper mapper = new PlanningMapper();
    private final ScheduleEvaluator evaluator = new ScheduleEvaluator();

    public ValidationReport verify(
            SchoolDefinition definition,
            CurrentTimetableReader.CurrentTimetable timetable) {
        List<ValidationError> errors = new ArrayList<>();
        Map<String, SchoolDefinition.Lesson> lessons = definition.lessons().stream()
                .collect(Collectors.toMap(SchoolDefinition.Lesson::id, Function.identity()));
        Map<String, CurrentTimetableReader.Assignment> assignments = new HashMap<>();
        timetable.assignments().forEach(assignment -> assignments.put(assignment.lessonId(), assignment));

        Set<String> missing = new HashSet<>(lessons.keySet());
        missing.removeAll(assignments.keySet());
        missing.stream().sorted().forEach(id -> errors.add(new ValidationError(
                "/timetable/assignments", List.of(id), "accepted baseline is missing a lesson assignment")));
        assignments.keySet().stream().filter(id -> !lessons.containsKey(id)).sorted().forEach(id -> errors.add(
                new ValidationError(
                        "/timetable/assignments", List.of(id), "accepted baseline contains an unknown lesson")));

        lessons.keySet().stream().filter(assignments::containsKey).sorted().forEach(id -> {
            SchoolDefinition.Lesson lesson = lessons.get(id);
            CurrentTimetableReader.Assignment assignment = assignments.get(id);
            compare(errors, id, "subjectId", lesson.subjectId(), assignment.subjectId());
            compare(errors, id, "cohortId", lesson.cohortId(), assignment.cohortId());
            compare(errors, id, "teacherId", lesson.teacherId(), assignment.teacherId());
        });
        if (!errors.isEmpty()) {
            return ValidationReport.from(errors);
        }

        var schedule = mapper.toPlanningProblem(definition);
        Map<String, PeriodValue> periods = schedule.getPeriods().stream()
                .collect(Collectors.toMap(PeriodValue::id, Function.identity()));
        Map<String, RoomValue> rooms = schedule.getRooms().stream()
                .collect(Collectors.toMap(RoomValue::id, Function.identity()));
        for (PlanningLesson lesson : schedule.getLessons()) {
            CurrentTimetableReader.Assignment assignment = assignments.get(lesson.getId());
            PeriodValue period = periods.get(assignment.periodId());
            RoomValue room = rooms.get(assignment.roomId());
            if (definition.reservedPeriodIds().contains(assignment.periodId())) {
                errors.add(new ValidationError(
                        "/timetable/assignments", List.of(lesson.getId(), assignment.periodId()),
                        "accepted baseline assignment uses a reserved period"));
            } else if (period == null) {
                errors.add(new ValidationError(
                        "/timetable/assignments", List.of(lesson.getId(), assignment.periodId()),
                        "accepted baseline references an unknown period"));
            } else {
                lesson.setPeriod(period);
            }
            if (room == null) {
                errors.add(new ValidationError(
                        "/timetable/assignments", List.of(lesson.getId(), assignment.roomId()),
                        "accepted baseline references an unknown room"));
            } else {
                lesson.setRoom(room);
            }
        }
        if (!errors.isEmpty()) {
            return ValidationReport.from(errors);
        }
        ScheduleEvaluator.Evaluation evaluation = evaluator.evaluate(schedule, definition.softWeights());
        if (!evaluation.complete()) {
            errors.add(new ValidationError(
                    "/timetable/assignments", List.of(), "accepted baseline assignments are incomplete"));
        }
        evaluation.hardMatchCounts().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> errors.add(new ValidationError(
                        "/timetable/assignments", List.of(),
                        "accepted baseline violates " + entry.getKey() + " (" + entry.getValue() + ")")));
        return ValidationReport.from(errors);
    }

    private static void compare(
            List<ValidationError> errors, String lessonId, String field, String expected, String actual) {
        if (!expected.equals(actual)) {
            errors.add(new ValidationError(
                    "/timetable/assignments/" + field,
                    List.of(lessonId, expected, actual),
                    "accepted baseline assignment does not match the definition"));
        }
    }
}
