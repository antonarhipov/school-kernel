package org.schoolkernel.application;

import java.nio.file.Path;
import java.util.*;
import org.schoolkernel.contract.*;
import org.schoolkernel.domain.*;
import org.schoolkernel.solver.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Stateless authoritative review of a complete manual candidate; never invokes the solver. */
public final class ReviewService {
    private final DefinitionLoader definitions;
    private final PlanFiles files;
    public ReviewService(DefinitionLoader definitions, PlanFiles files) { this.definitions = definitions; this.files = files; }

    public CommandOutcome handle(Path definitionPath, Path assignmentsPath, String correlation) {
        String id = correlation == null ? UUID.randomUUID().toString() : correlation;
        long started = System.nanoTime();
        var reports = new VerificationResultFactory();
        try {
            var loaded = definitions.load(definitionPath, DefinitionLoader.Mode.BASELINE);
            if (loaded instanceof DefinitionLoader.LimitExceeded) return new CommandOutcome.TransportFailure("Input exceeds parsing safeguards");
            if (loaded instanceof DefinitionLoader.Rejected rejected)
                return new CommandOutcome.InvalidInput(reports.invalid(id, elapsed(started), rejected.report()));
            var accepted = (DefinitionLoader.Accepted) loaded;
            JsonNode assignments;
            try { assignments = JsonSupport.mapper().readTree(files.read(assignmentsPath, FileBoundary.DEFAULT_MAX_INPUT_BYTES)); }
            catch (tools.jackson.core.JacksonException malformed) {
                return invalid(reports, id, started, List.of(new ValidationError("/assignments", List.of(), "Expected valid assignment JSON")));
            }
            var schedule = new PlanningMapper().toPlanningProblem(accepted.definition());
            var lessons = new HashMap<String, PlanningLesson>(); schedule.getLessons().forEach(l -> lessons.put(l.getId(), l));
            var periods = new HashMap<String, PeriodValue>(); schedule.getPeriods().forEach(p -> periods.put(p.id(), p));
            var rooms = new HashMap<String, RoomValue>(); schedule.getRooms().forEach(r -> rooms.put(r.id(), r));
            var errors = new ArrayList<ValidationError>(); var seen = new HashSet<String>();
            if (!assignments.isArray()) errors.add(new ValidationError("/assignments", List.of(), "Expected a complete assignment array"));
            else for (JsonNode a : assignments) {
                String lessonId = a.path("lessonId").asString(""); var lesson = lessons.get(lessonId);
                boolean shape = a.isObject() && a.size() == 6 && List.of("lessonId", "subjectId", "cohortId", "teacherId", "periodId", "roomId")
                        .stream().allMatch(f -> a.path(f).isTextual());
                if (!shape || lesson == null || !seen.add(lessonId)) {
                    errors.add(new ValidationError("/assignments", List.of(lessonId), "Every lesson needs exactly one complete assignment")); continue;
                }
                if (!lesson.getSubjectId().equals(a.path("subjectId").asString()) || !lesson.getCohortId().equals(a.path("cohortId").asString())
                        || !lesson.getTeacherId().equals(a.path("teacherId").asString()) || !periods.containsKey(a.path("periodId").asString())
                        || !rooms.containsKey(a.path("roomId").asString())) {
                    errors.add(new ValidationError("/assignments", List.of(lessonId), "Assignment must match the lesson and use a known room and time slot")); continue;
                }
                lesson.setPeriod(periods.get(a.path("periodId").asString())); lesson.setRoom(rooms.get(a.path("roomId").asString()));
            }
            lessons.keySet().stream().filter(l -> !seen.contains(l)).sorted().forEach(l -> errors.add(
                    new ValidationError("/assignments", List.of(l), "Lesson is missing from the timetable")));
            if (!errors.isEmpty()) return invalid(reports, id, started, errors);
            var evaluation = new ScheduleEvaluator().evaluate(schedule, accepted.definition().softWeights());
            if (!evaluation.complete()) throw new IllegalStateException("Incomplete mapping");
            ObjectNode review = reports.verifiedInitial(id, elapsed(started), accepted.dto().schoolId(), accepted.dto().catalogVersion(), accepted.revision());
            review.put("status", "REVIEWED"); review.remove("mode"); review.put("valid", evaluation.feasible());
            review.set("hardMatchCounts", JsonSupport.mapper().valueToTree(evaluation.hardMatchCounts()));
            review.set("softMatchCounts", JsonSupport.mapper().valueToTree(evaluation.softMatchCounts()));
            var diagnostics = review.putArray("conflicts");
            boolean truncated = false;
            for (var diagnostic : HardConstraintDiagnostics.from(schedule, evaluation)) {
                var item = diagnostics.addObject().put("constraintId", diagnostic.constraintId()).put("matchCount", diagnostic.matchCount());
                item.set("examples", JsonSupport.mapper().valueToTree(diagnostic.examples()));
                var affected = new TreeSet<String>();
                for (var example : diagnostic.examples()) {
                    for (String entity : example) if (lessons.containsKey(entity)) affected.add(entity);
                    // Collective constraints identify a class/day/subject rather than an individual lesson.
                    if (example.stream().noneMatch(lessons::containsKey)) {
                        var days = new HashSet<String>();
                        example.stream().filter(periods::containsKey).map(p -> periods.get(p).weekday().name()).forEach(days::add);
                        example.stream().filter(e -> Set.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY").contains(e)).forEach(days::add);
                        var subjects = accepted.definition().subjects().stream().map(SchoolDefinition.Subject::id).filter(example::contains).collect(java.util.stream.Collectors.toSet());
                        schedule.getLessons().stream().filter(l -> example.contains(l.getCohortId()))
                                .filter(l -> days.isEmpty() || days.contains(l.getPeriod().weekday().name()))
                                .filter(l -> subjects.isEmpty() || subjects.contains(l.getSubjectId()))
                                .map(PlanningLesson::getId).forEach(affected::add);
                    }
                }
                // If examples are bounded, mark the full schedule so no affected lesson silently disappears.
                boolean limited = diagnostic.examples().size() >= 1_000;
                if (limited) affected.addAll(lessons.keySet());
                truncated |= limited;
                item.set("lessonIds", JsonSupport.mapper().valueToTree(affected));
            }
            review.put("truncated", truncated);
            if (evaluation.feasible()) {
                var result = new ResultFactory().feasible(id, elapsed(started), accepted.definition(), accepted.revision(),
                        new SolverAdapter.ExecutionControls(null, 1, 0), "MANUAL_REVIEW", schedule, evaluation);
                result.putObject("limit").put("type", "NONE");
                review.set("result", result);
            }
            return new CommandOutcome.Succeeded(review);
        } catch (TransportException e) { return new CommandOutcome.TransportFailure(e.getMessage()); }
        catch (RuntimeException e) { return new CommandOutcome.InternalError(reports.internal(id, elapsed(started)), e); }
    }
    private static CommandOutcome invalid(VerificationResultFactory factory, String id, long started, List<ValidationError> errors) {
        return new CommandOutcome.InvalidInput(factory.invalid(id, elapsed(started), ValidationReport.from(errors)));
    }
    private static long elapsed(long started) { return Math.max(0, (System.nanoTime()-started)/1_000_000); }
}
