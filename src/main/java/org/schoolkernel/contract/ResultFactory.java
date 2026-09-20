package org.schoolkernel.contract;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.domain.ValidationReport;
import org.schoolkernel.solver.ConstraintDiagnostic;
import org.schoolkernel.solver.SchoolSchedule;
import org.schoolkernel.solver.ScheduleEvaluator;
import org.schoolkernel.solver.SolverAdapter.ExecutionControls;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class ResultFactory {
    public static final String KERNEL_VERSION = "1.0.0-SNAPSHOT";

    private final RevisionService revisions = new RevisionService();

    public ObjectNode invalidInput(
            String correlationId,
            long elapsedMillis,
            ValidationReport report,
            String schoolId,
            Integer catalogVersion,
            String inputRevision,
            Map<String, Long> effectiveWeights,
            ExecutionControls controls) {
        ObjectNode result = envelope("INVALID_INPUT", correlationId, elapsedMillis);
        addDerivableMetadata(result, schoolId, catalogVersion, inputRevision, effectiveWeights);
        addExecutionMetadata(result, controls, null);
        ObjectNode validation = result.putObject("validationReport");
        validation.put("totalErrors", report.totalErrors());
        validation.put("truncated", report.truncated());
        ArrayNode errors = validation.putArray("errors");
        report.errors().forEach(error -> {
            ObjectNode detail = errors.addObject();
            detail.put("location", error.location());
            ArrayNode ids = detail.putArray("entityIds");
            error.entityIds().forEach(ids::add);
            detail.put("message", error.message());
        });
        return result;
    }

    public ObjectNode noFeasibleSolution(
            String correlationId,
            long elapsedMillis,
            SchoolDefinition definition,
            String inputRevision,
            ExecutionControls controls,
            String terminationReason,
            List<ConstraintDiagnostic> diagnostics) {
        ObjectNode result = envelope("NO_FEASIBLE_SOLUTION_FOUND", correlationId, elapsedMillis);
        addDerivableMetadata(
                result, definition.schoolId(), definition.catalogVersion(), inputRevision, definition.softWeights());
        addExecutionMetadata(result, controls, terminationReason);
        ObjectNode search = result.putObject("searchDiagnostics");
        long totalMatches = diagnostics.stream().mapToLong(ConstraintDiagnostic::matchCount).sum();
        search.put("totalMatches", totalMatches);
        long availableExamples = diagnostics.stream().mapToLong(diagnostic -> diagnostic.examples().size()).sum();
        search.put("truncated", availableExamples > 1_000 || totalMatches > availableExamples);
        ArrayNode constraints = search.putArray("constraints");
        int remainingExamples = 1_000;
        diagnostics.forEach(diagnostic -> {
            ObjectNode constraint = constraints.addObject();
            constraint.put("constraintId", diagnostic.constraintId());
            constraint.put("matchCount", diagnostic.matchCount());
            constraint.putArray("examples");
        });
        for (int index = 0; index < diagnostics.size() && remainingExamples > 0; index++) {
            ArrayNode examples = (ArrayNode) constraints.get(index).path("examples");
            for (List<String> exampleIds : diagnostics.get(index).examples()) {
                if (remainingExamples-- == 0) {
                    break;
                }
                ArrayNode example = examples.addArray();
                exampleIds.forEach(example::add);
            }
        }
        return result;
    }

    public ObjectNode feasible(
            String correlationId,
            long elapsedMillis,
            SchoolDefinition definition,
            String inputRevision,
            ExecutionControls controls,
            String terminationReason,
            SchoolSchedule schedule,
            ScheduleEvaluator.Evaluation evaluation) {
        ObjectNode result = envelope("FEASIBLE", correlationId, elapsedMillis);
        addDerivableMetadata(
                result, definition.schoolId(), definition.catalogVersion(), inputRevision, definition.softWeights());
        addExecutionMetadata(result, controls, terminationReason);
        ArrayNode assignments = result.putObject("timetable").putArray("assignments");
        schedule.getLessons().stream()
                .sorted(java.util.Comparator.comparing(org.schoolkernel.solver.PlanningLesson::getId))
                .forEach(lesson -> {
                    ObjectNode assignment = assignments.addObject();
                    assignment.put("lessonId", lesson.getId());
                    assignment.put("subjectId", lesson.getSubjectId());
                    assignment.put("cohortId", lesson.getCohortId());
                    assignment.put("teacherId", lesson.getTeacherId());
                    assignment.put("periodId", lesson.getPeriod().id());
                    assignment.put("roomId", lesson.getRoom().id());
                });
        result.put("timetableRevision",
                revisions.timetableRevision(1, definition.schoolId(), inputRevision, assignments));
        addProductScore(result, definition.softWeights(), evaluation);
        return result;
    }

    public ObjectNode emptyFeasible(
            String correlationId,
            long elapsedMillis,
            SchoolDefinition definition,
            String inputRevision,
            ExecutionControls controls) {
        var emptySchedule = new SchoolSchedule(List.of(), List.of(), List.of(),
                ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides.none());
        var evaluation = new ScheduleEvaluator.Evaluation(
                true,
                true,
                SchoolDefinition.HARD_CONSTRAINT_IDS.stream()
                        .collect(java.util.stream.Collectors.toMap(id -> id, ignored -> 0L)),
                SchoolDefinition.SOFT_CONSTRAINT_IDS.stream()
                        .collect(java.util.stream.Collectors.toMap(id -> id, ignored -> 0L)),
                0,
                0,
                0);
        return feasible(correlationId, elapsedMillis, definition, inputRevision, controls,
                "EMPTY_PROBLEM", emptySchedule, evaluation);
    }

    public ObjectNode replannedFeasible(
            String correlationId,
            long elapsedMillis,
            SchoolDefinition definition,
            String inputRevision,
            ExecutionControls controls,
            String terminationReason,
            SchoolSchedule schedule,
            ScheduleEvaluator.Evaluation evaluation,
            CurrentTimetableReader.CurrentTimetable current) {
        ObjectNode result = feasible(
                correlationId, elapsedMillis, definition, inputRevision, controls,
                terminationReason, schedule, evaluation);
        addChangeReport(result, definition, schedule, current);
        return result;
    }

    private static void addChangeReport(
            ObjectNode result,
            SchoolDefinition definition,
            SchoolSchedule schedule,
            CurrentTimetableReader.CurrentTimetable current) {
        ObjectNode report = result.putObject("changeReport");
        ArrayNode additions = report.putArray("additions");
        ArrayNode cancellations = report.putArray("cancellations");
        ArrayNode teacherChanges = report.putArray("teacherChanges");
        ArrayNode forcedMoves = report.putArray("forcedMoves");
        ArrayNode periodMoves = report.putArray("periodMoves");
        ArrayNode roomOnlyMoves = report.putArray("roomOnlyMoves");

        var oldById = new HashMap<String, CurrentTimetableReader.Assignment>();
        current.assignments().forEach(value -> oldById.put(value.lessonId(), value));
        var lessonById = new HashMap<String, SchoolDefinition.Lesson>();
        definition.lessons().forEach(value -> lessonById.put(value.id(), value));
        var solvedById = new HashMap<String, org.schoolkernel.solver.PlanningLesson>();
        schedule.getLessons().forEach(value -> solvedById.put(value.getId(), value));

        definition.lessons().stream().map(SchoolDefinition.Lesson::id).sorted()
                .filter(id -> !oldById.containsKey(id))
                .forEach(id -> additions.addObject().put("lessonId", id));
        current.assignments().stream().map(CurrentTimetableReader.Assignment::lessonId).sorted()
                .filter(id -> !lessonById.containsKey(id))
                .forEach(id -> cancellations.addObject().put("lessonId", id));

        oldById.keySet().stream().filter(lessonById::containsKey).sorted().forEach(id -> {
            var old = oldById.get(id);
            var lesson = lessonById.get(id);
            var solved = solvedById.get(id);
            if (!old.teacherId().equals(lesson.teacherId())) {
                teacherChanges.addObject()
                        .put("lessonId", id)
                        .put("oldTeacherId", old.teacherId())
                        .put("newTeacherId", lesson.teacherId());
            }
            boolean periodChanged = !old.periodId().equals(solved.getPeriod().id());
            boolean roomChanged = !old.roomId().equals(solved.getRoom().id());
            boolean forcedPeriod = periodChanged && lesson.periodLock() != null
                    && lesson.periodLock().equals(solved.getPeriod().id());
            boolean forcedRoom = roomChanged && lesson.roomLock() != null
                    && lesson.roomLock().equals(solved.getRoom().id());
            if (forcedPeriod || forcedRoom) {
                ObjectNode item = forcedMoves.addObject().put("lessonId", id);
                if (forcedPeriod) {
                    item.put("oldPeriodId", old.periodId()).put("newPeriodId", solved.getPeriod().id());
                }
                if (forcedRoom) {
                    item.put("oldRoomId", old.roomId()).put("newRoomId", solved.getRoom().id());
                }
            }
            if (periodChanged && !forcedPeriod) {
                periodMoves.addObject()
                        .put("lessonId", id)
                        .put("oldPeriodId", old.periodId())
                        .put("newPeriodId", solved.getPeriod().id())
                        .put("oldRoomId", old.roomId())
                        .put("newRoomId", solved.getRoom().id());
            } else if (!periodChanged && roomChanged && !forcedRoom) {
                roomOnlyMoves.addObject()
                        .put("lessonId", id)
                        .put("oldRoomId", old.roomId())
                        .put("newRoomId", solved.getRoom().id());
            }
        });
    }

    public ObjectNode internalError(
            String correlationId,
            long elapsedMillis,
            String schoolId,
            Integer catalogVersion,
            String inputRevision,
            Map<String, Long> effectiveWeights,
            ExecutionControls controls) {
        ObjectNode result = envelope("INTERNAL_ERROR", correlationId, elapsedMillis);
        addDerivableMetadata(result, schoolId, catalogVersion, inputRevision, effectiveWeights);
        addExecutionMetadata(result, controls, null);
        result.put("safeMessage", "An unexpected internal error occurred.");
        return result;
    }

    private static ObjectNode envelope(String status, String correlationId, long elapsedMillis) {
        ObjectNode result = JsonSupport.mapper().createObjectNode();
        result.put("schemaVersion", 1);
        result.put("status", status);
        result.put("kernelVersion", KERNEL_VERSION);
        result.put("correlationId", correlationId);
        result.put("elapsedTimeMs", elapsedMillis);
        return result;
    }

    private static void addDerivableMetadata(
            ObjectNode result,
            String schoolId,
            Integer catalogVersion,
            String inputRevision,
            Map<String, Long> effectiveWeights) {
        if (schoolId != null) {
            result.put("schoolId", schoolId);
        }
        if (catalogVersion != null) {
            result.put("catalogVersion", catalogVersion);
        }
        if (inputRevision != null) {
            result.put("inputRevision", inputRevision);
        }
        if (effectiveWeights != null) {
            ObjectNode weights = result.putObject("effectiveSoftWeights");
            SchoolDefinition.SOFT_CONSTRAINT_IDS.forEach(id -> weights.put(id, effectiveWeights.get(id)));
        }
    }

    private static void addExecutionMetadata(
            ObjectNode result,
            ExecutionControls controls,
            String terminationReason) {
        result.put("seed", controls.seed());
        ObjectNode limit = result.putObject("limit");
        if (controls.timeLimit() != null) {
            limit.put("type", "TIME");
            limit.put("duration", controls.timeLimit().toString());
        } else {
            limit.put("type", "STEP");
            limit.put("steps", controls.stepLimit());
        }
        if (terminationReason != null) {
            result.put("terminationReason", terminationReason);
        }
    }

    private static void addProductScore(
            ObjectNode result,
            Map<String, Long> weights,
            ScheduleEvaluator.Evaluation evaluation) {
        ObjectNode score = result.putObject("score");
        score.put("periodMoves", evaluation.periodMoves());
        score.put("roomOnlyMoves", evaluation.roomOnlyMoves());
        score.put("ordinaryPreferencePenalty", evaluation.ordinaryPreferencePenalty());
        ArrayNode breakdown = score.putArray("constraintBreakdown");
        SchoolDefinition.SOFT_CONSTRAINT_IDS.forEach(id -> {
            ObjectNode item = breakdown.addObject();
            long weight = weights.get(id);
            long matchCount = evaluation.softMatchCounts().get(id);
            item.put("constraintId", id);
            item.put("category", "ORDINARY_PREFERENCE");
            item.put("effectiveWeight", weight);
            item.put("matchCount", matchCount);
            item.put("aggregatePenalty", Math.multiplyExact(weight, matchCount));
        });
    }
}
