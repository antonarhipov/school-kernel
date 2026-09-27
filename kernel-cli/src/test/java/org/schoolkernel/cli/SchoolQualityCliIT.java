package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.ResultFactory;
import org.schoolkernel.contract.RevisionService;
import org.schoolkernel.contract.SchoolDefinitionDto;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.domain.KernelCatalog;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.solver.PlanningMapper;
import org.schoolkernel.solver.ScheduleEvaluator;
import org.schoolkernel.solver.SchoolSchedule;
import org.schoolkernel.solver.SolverAdapter;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class SchoolQualityCliIT {
    private static final Path JAR = Path.of("target", "school-kernel.jar").toAbsolutePath();

    @TempDir
    Path temporaryDirectory;

    @Test
    void catalogThreePackagedPlanReportsLateStartAndZeroWeightWithoutChangingLegacyRows() throws Exception {
        ObjectNode definition = lateStartDefinition();
        Path definitionPath = write("late-start-definition.json", definition);
        Path output = temporaryDirectory.resolve("late-start-result.json");

        ProcessResult process = run("plan", "--definition", definitionPath.toString(),
                "--output", output.toString(), "--step-limit", "100");
        assertEquals(0, process.exitCode(), process.stderr());
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("FEASIBLE", result.path("status").stringValue());
        assertEquals(3, result.path("catalogVersion").intValue());
        assertEquals(7, result.path("score").path("constraintBreakdown").size());
        assertEquals(1, matches(result, KernelCatalog.COHORT_LATE_START.id()));
        assertEquals(1, constraint(result, KernelCatalog.COHORT_LATE_START.id())
                .path("aggregatePenalty").longValue());
        assertEquals(1, result.path("effectiveSoftWeights")
                .path(KernelCatalog.COHORT_LATE_START.id()).longValue());

        Path verification = temporaryDirectory.resolve("late-start-verified.json");
        ProcessResult verified = run("verify", "--definition", definitionPath.toString(),
                "--result", output.toString(), "--output", verification.toString());
        assertEquals(0, verified.exitCode(), verified.stderr());
        assertEquals("VERIFIED", JsonSupport.mapper().readTree(verification).path("status").stringValue());

        ObjectNode zeroWeight = definition.deepCopy();
        zeroWeight.putArray("softConstraintOverrides").addObject()
                .put("constraintId", KernelCatalog.COHORT_LATE_START.id()).put("weight", 0);
        Path zeroPath = write("late-start-zero-weight.json", zeroWeight);
        Path zeroOutput = temporaryDirectory.resolve("late-start-zero-weight-result.json");
        ProcessResult zeroRun = run("plan", "--definition", zeroPath.toString(),
                "--output", zeroOutput.toString(), "--step-limit", "100");
        assertEquals(0, zeroRun.exitCode(), zeroRun.stderr());
        JsonNode zeroResult = JsonSupport.mapper().readTree(zeroOutput);
        assertEquals(1, matches(zeroResult, KernelCatalog.COHORT_LATE_START.id()));
        assertEquals(0, constraint(zeroResult, KernelCatalog.COHORT_LATE_START.id())
                .path("aggregatePenalty").longValue());
    }

    @Test
    void catalogThreePackagedRepairKeepsCatalogTwoPredecessorUnchanged() throws Exception {
        ObjectNode predecessor = lateStartDefinition();
        predecessor.put("catalogVersion", 2);
        ((ObjectNode) predecessor.withArray("lessons").get(0)).put("periodLock", "mon-1");
        Path predecessorPath = write("start-predecessor.json", predecessor);
        Path currentPath = temporaryDirectory.resolve("start-current.json");
        ProcessResult initial = run("plan", "--definition", predecessorPath.toString(),
                "--output", currentPath.toString(), "--step-limit", "100");
        assertEquals(0, initial.exitCode(), initial.stderr());
        byte[] currentBytes = Files.readAllBytes(currentPath);
        JsonNode current = JsonSupport.mapper().readTree(currentPath);
        assertEquals(2, current.path("catalogVersion").intValue());
        assertEquals(6, current.path("score").path("constraintBreakdown").size());
        assertTrue(!current.path("effectiveSoftWeights").has(KernelCatalog.COHORT_LATE_START.id()));

        ObjectNode successor = lateStartDefinition();
        successor.put("basedOnRevision", current.path("inputRevision").stringValue());
        Path successorPath = write("start-successor.json", successor);
        Path output = temporaryDirectory.resolve("start-proposal.json");
        ProcessResult repair = run("replan", "--current-definition", predecessorPath.toString(),
                "--current", currentPath.toString(), "--definition", successorPath.toString(),
                "--output", output.toString(), "--step-limit", "100");
        assertEquals(0, repair.exitCode(), repair.stderr());
        JsonNode proposal = JsonSupport.mapper().readTree(output);
        assertEquals("FEASIBLE", proposal.path("status").stringValue());
        assertEquals(3, proposal.path("catalogVersion").intValue());
        assertEquals(1, matches(proposal, KernelCatalog.COHORT_LATE_START.id()));
        assertArrayEquals(currentBytes, Files.readAllBytes(currentPath));
    }

    @Test
    void catalogThreeFailureResultsDoNotPublishTimetables() throws Exception {
        ObjectNode invalid = lateStartDefinition();
        invalid.putArray("softConstraintOverrides").addObject()
                .put("constraintId", KernelCatalog.COHORT_LATE_START.id()).put("weight", 1_000_001);
        Path invalidPath = write("start-invalid.json", invalid);
        Path invalidOutput = temporaryDirectory.resolve("start-invalid-result.json");
        ProcessResult rejected = run("plan", "--definition", invalidPath.toString(),
                "--output", invalidOutput.toString(), "--step-limit", "10");
        assertEquals(2, rejected.exitCode(), rejected.stderr());
        JsonNode invalidResult = JsonSupport.mapper().readTree(invalidOutput);
        assertEquals("INVALID_INPUT", invalidResult.path("status").stringValue());
        assertTrue(!invalidResult.has("timetable"));

        ObjectNode impossible = (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "no-room-plan.json"));
        impossible.put("catalogVersion", 3);
        Path impossiblePath = write("start-impossible.json", impossible);
        Path impossibleOutput = temporaryDirectory.resolve("start-impossible-result.json");
        ProcessResult unsuccessful = run("plan", "--definition", impossiblePath.toString(),
                "--output", impossibleOutput.toString(), "--step-limit", "10");
        assertEquals(3, unsuccessful.exitCode(), unsuccessful.stderr());
        JsonNode unsuccessfulResult = JsonSupport.mapper().readTree(impossibleOutput);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", unsuccessfulResult.path("status").stringValue());
        assertTrue(!unsuccessfulResult.has("timetable"));
    }

    @Test
    void fullMv5InitialPlanningBalancesFiveAWithoutClassGaps() throws Exception {
        ObjectNode definition = mv5();
        Path definitionPath = write("quality-initial.json", definition);
        Path output = temporaryDirectory.resolve("quality-initial-result.json");

        ProcessResult process = run("plan", "--definition", definitionPath.toString(),
                "--output", output.toString(), "--step-limit", "300000");
        assertEquals(0, process.exitCode(), process.stderr());
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("FEASIBLE", result.path("status").stringValue());
        assertEquals(2, result.path("catalogVersion").intValue());
        assertEquals(6, result.path("score").path("constraintBreakdown").size());
        assertEquals(0, matches(result, KernelCatalog.COHORT_GAP.id()));
        assertEquals(0, matches(result, KernelCatalog.COHORT_WEEK_BALANCE.id()));
        Map<String, Long> counts = java.util.stream.StreamSupport.stream(
                        result.path("timetable").path("assignments").spliterator(), false)
                .filter(assignment -> "5a".equals(assignment.path("cohortId").stringValue()))
                .collect(Collectors.groupingBy(
                        assignment -> assignment.path("periodId").stringValue().substring(0, 3),
                        Collectors.counting()));
        assertEquals(Map.of("mon", 5L, "tue", 5L, "wed", 4L, "thu", 4L, "fri", 4L), counts);
    }

    @Test
    void zeroClassQualityWeightsStillReportGapsAndWeeklyImbalance() throws Exception {
        ObjectNode definition = mv5();
        var lessons = definition.withArray("lessons");
        var selected = new ArrayList<JsonNode>();
        lessons.forEach(lesson -> {
            String id = lesson.path("id").stringValue();
            if (id.equals("5a.int-o.01") || id.equals("5a.literature.01")) {
                ObjectNode copy = (ObjectNode) lesson.deepCopy();
                copy.put("periodLock", id.equals("5a.int-o.01") ? "mon-0" : "mon-2");
                copy.put("roomLock", id.equals("5a.int-o.01") ? "b216" : "a119");
                selected.add(copy);
            }
        });
        lessons.removeAll();
        selected.forEach(lessons::add);
        var overrides = definition.putArray("softConstraintOverrides");
        overrides.addObject().put("constraintId", KernelCatalog.COHORT_GAP.id()).put("weight", 0);
        overrides.addObject().put("constraintId", KernelCatalog.COHORT_WEEK_BALANCE.id()).put("weight", 0);
        Path definitionPath = write("zero-quality-weights.json", definition);
        Path output = temporaryDirectory.resolve("zero-quality-weights-result.json");

        ProcessResult process = run("plan", "--definition", definitionPath.toString(),
                "--output", output.toString(), "--step-limit", "1000");
        assertEquals(0, process.exitCode(), process.stderr());
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("FEASIBLE", result.path("status").stringValue());
        assertEquals(1, matches(result, KernelCatalog.COHORT_GAP.id()));
        assertEquals(4, matches(result, KernelCatalog.COHORT_WEEK_BALANCE.id()));
        for (String id : List.of(KernelCatalog.COHORT_GAP.id(), KernelCatalog.COHORT_WEEK_BALANCE.id())) {
            assertEquals(0, result.path("effectiveSoftWeights").path(id).longValue());
            assertEquals(0, constraint(result, id).path("aggregatePenalty").longValue());
        }
    }

    @Test
    void catalogFourPackagedPlanningAndRepairUseTheCohortDailySpread() throws Exception {
        ObjectNode configured = twoLessonDailySpreadDefinition();
        Path configuredPath = write("cohort-spread-two.json", configured);
        Path configuredOutput = temporaryDirectory.resolve("cohort-spread-two-result.json");
        ProcessResult planned = run("plan", "--definition", configuredPath.toString(),
                "--output", configuredOutput.toString(), "--step-limit", "100");
        assertEquals(0, planned.exitCode(), planned.stderr());
        JsonNode candidate = JsonSupport.mapper().readTree(configuredOutput);
        assertEquals("FEASIBLE", candidate.path("status").stringValue());
        assertEquals(4, candidate.path("catalogVersion").intValue());
        assertEquals(7, candidate.path("score").path("constraintBreakdown").size());
        assertEquals(0, matches(candidate, KernelCatalog.COHORT_WEEK_BALANCE.id()));
        Path configuredVerification = temporaryDirectory.resolve("cohort-spread-two-verified.json");
        ProcessResult verified = run("verify", "--definition", configuredPath.toString(),
                "--result", configuredOutput.toString(), "--output", configuredVerification.toString());
        assertEquals(0, verified.exitCode(), verified.stderr());
        assertEquals("VERIFIED", JsonSupport.mapper().readTree(configuredVerification).path("status").stringValue());

        ObjectNode predecessor = configured.deepCopy();
        predecessor.put("catalogVersion", 3);
        cohort(predecessor, "5a").remove("maxDailyLessonSpread");
        Path predecessorPath = write("cohort-spread-predecessor.json", predecessor);
        Path currentPath = temporaryDirectory.resolve("cohort-spread-current.json");
        ProcessResult prior = run("plan", "--definition", predecessorPath.toString(),
                "--output", currentPath.toString(), "--step-limit", "100");
        assertEquals(0, prior.exitCode(), prior.stderr());
        byte[] priorBytes = Files.readAllBytes(currentPath);
        JsonNode priorResult = JsonSupport.mapper().readTree(currentPath);
        assertEquals(3, priorResult.path("catalogVersion").intValue());
        assertEquals(4, matches(priorResult, KernelCatalog.COHORT_WEEK_BALANCE.id()));

        ObjectNode successor = configured.deepCopy();
        successor.put("basedOnRevision", priorResult.path("inputRevision").stringValue());
        Path successorPath = write("cohort-spread-successor.json", successor);
        Path repairOutput = temporaryDirectory.resolve("cohort-spread-repair.json");
        ProcessResult repaired = run("replan", "--current-definition", predecessorPath.toString(),
                "--current", currentPath.toString(), "--definition", successorPath.toString(),
                "--output", repairOutput.toString(), "--step-limit", "100");
        assertEquals(0, repaired.exitCode(), repaired.stderr());
        JsonNode proposal = JsonSupport.mapper().readTree(repairOutput);
        assertEquals("FEASIBLE", proposal.path("status").stringValue());
        assertEquals(4, proposal.path("catalogVersion").intValue());
        assertEquals(0, matches(proposal, KernelCatalog.COHORT_WEEK_BALANCE.id()));
        assertEquals(0, proposal.path("score").path("periodMoves").longValue());
        assertEquals(0, proposal.path("score").path("roomOnlyMoves").longValue());
        assertArrayEquals(priorBytes, Files.readAllBytes(currentPath));

        ObjectNode disabled = configured.deepCopy();
        cohort(disabled, "5a").put("maxDailyLessonSpread", 1);
        disabled.putArray("softConstraintOverrides").addObject()
                .put("constraintId", KernelCatalog.COHORT_WEEK_BALANCE.id()).put("weight", 0);
        Path disabledPath = write("cohort-spread-disabled.json", disabled);
        Path disabledOutput = temporaryDirectory.resolve("cohort-spread-disabled-result.json");
        ProcessResult zeroWeighted = run("plan", "--definition", disabledPath.toString(),
                "--output", disabledOutput.toString(), "--step-limit", "100");
        assertEquals(0, zeroWeighted.exitCode(), zeroWeighted.stderr());
        JsonNode zeroResult = JsonSupport.mapper().readTree(disabledOutput);
        assertEquals(4, matches(zeroResult, KernelCatalog.COHORT_WEEK_BALANCE.id()));
        assertEquals(0, constraint(zeroResult, KernelCatalog.COHORT_WEEK_BALANCE.id())
                .path("aggregatePenalty").longValue());
    }

    @Test
    void catalogFourRejectsInvalidDailySpreadWithoutPublishingATimetable() throws Exception {
        ObjectNode configured = twoLessonDailySpreadDefinition();
        for (int index = 0; index < 3; index++) {
            ObjectNode invalid = configured.deepCopy();
            ObjectNode cohort = cohort(invalid, "5a");
            switch (index) {
                case 0 -> cohort.put("maxDailyLessonSpread", -1);
                case 1 -> cohort.put("maxDailyLessonSpread", 1.5);
                default -> invalid.put("catalogVersion", 3);
            }
            Path invalidPath = write("cohort-spread-invalid-" + index + ".json", invalid);
            Path output = temporaryDirectory.resolve("cohort-spread-invalid-result-" + index + ".json");
            ProcessResult rejected = run("plan", "--definition", invalidPath.toString(),
                    "--output", output.toString(), "--step-limit", "10");
            assertEquals(2, rejected.exitCode(), rejected.stderr());
            JsonNode result = JsonSupport.mapper().readTree(output);
            assertEquals("INVALID_INPUT", result.path("status").stringValue());
            assertTrue(!result.has("timetable"));
            assertTrue(!rejected.stderr().contains("Solving started"));
        }

        ObjectNode impossible = (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "no-room-plan.json"));
        impossible.put("catalogVersion", 4);
        ((ObjectNode) impossible.withArray("cohorts").get(0)).put("maxDailyLessonSpread", 2);
        Path impossiblePath = write("cohort-spread-impossible.json", impossible);
        Path impossibleOutput = temporaryDirectory.resolve("cohort-spread-impossible-result.json");
        ProcessResult unsuccessful = run("plan", "--definition", impossiblePath.toString(),
                "--output", impossibleOutput.toString(), "--step-limit", "10");
        assertEquals(3, unsuccessful.exitCode(), unsuccessful.stderr());
        JsonNode unsuccessfulResult = JsonSupport.mapper().readTree(impossibleOutput);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", unsuccessfulResult.path("status").stringValue());
        assertTrue(!unsuccessfulResult.has("timetable"));
    }

    @Test
    void catalogFiveForbidsCohortGapsUnlessTheCohortAllowsThem() throws Exception {
        ObjectNode forcedGap = twoLessonDailySpreadDefinition();
        forcedGap.put("catalogVersion", 5);
        Path forcedPath = write("cohort-gap-forced.json", forcedGap);
        Path forcedOutput = temporaryDirectory.resolve("cohort-gap-forced-result.json");
        ProcessResult rejected = run("plan", "--definition", forcedPath.toString(),
                "--output", forcedOutput.toString(), "--step-limit", "10");
        assertEquals(3, rejected.exitCode(), rejected.stderr());
        JsonNode rejectedResult = JsonSupport.mapper().readTree(forcedOutput);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", rejectedResult.path("status").stringValue());
        assertTrue(!rejectedResult.has("timetable"));
        JsonNode diagnostic = rejectedResult.path("searchDiagnostics").path("constraints").get(0);
        assertEquals(KernelCatalog.COHORT_DAILY_GAPS.id(), diagnostic.path("constraintId").stringValue());
        assertEquals("[\"5a\",\"mon-1\"]", diagnostic.path("examples").get(0).toString());

        cohort(forcedGap, "5a").put("maxDailyGaps", 1);
        Path allowedPath = write("cohort-gap-allowed.json", forcedGap);
        Path allowedOutput = temporaryDirectory.resolve("cohort-gap-allowed-result.json");
        ProcessResult allowed = run("plan", "--definition", allowedPath.toString(),
                "--output", allowedOutput.toString(), "--step-limit", "10");
        assertEquals(0, allowed.exitCode(), allowed.stderr());
        JsonNode allowedResult = JsonSupport.mapper().readTree(allowedOutput);
        assertEquals(5, allowedResult.path("catalogVersion").intValue());
        assertEquals(1, matches(allowedResult, KernelCatalog.COHORT_GAP.id()));
        Path verification = temporaryDirectory.resolve("cohort-gap-allowed-verification.json");
        ProcessResult verified = run("verify", "--definition", allowedPath.toString(),
                "--result", allowedOutput.toString(), "--output", verification.toString());
        assertEquals(0, verified.exitCode(), verified.stderr());

        cohort(forcedGap, "5a").remove("maxDailyGaps");
        forcedGap.put("catalogVersion", 4);
        Path legacyPath = write("cohort-gap-legacy.json", forcedGap);
        Path legacyOutput = temporaryDirectory.resolve("cohort-gap-legacy-result.json");
        ProcessResult legacy = run("plan", "--definition", legacyPath.toString(),
                "--output", legacyOutput.toString(), "--step-limit", "10");
        assertEquals(0, legacy.exitCode(), legacy.stderr());
    }

    @Test
    void catalogTwoRejectedAndUnsuccessfulPlanningPublishesNoCandidate() throws Exception {
        ObjectNode invalid = mv5();
        invalid.putArray("softConstraintOverrides").addObject()
                .put("constraintId", KernelCatalog.COHORT_GAP.id()).put("weight", 1_000_001);
        Path invalidPath = write("quality-invalid.json", invalid);
        Path invalidOutput = temporaryDirectory.resolve("quality-invalid-result.json");
        ProcessResult rejected = run("plan", "--definition", invalidPath.toString(),
                "--output", invalidOutput.toString(), "--step-limit", "10");
        assertEquals(2, rejected.exitCode(), rejected.stderr());
        JsonNode rejectedResult = JsonSupport.mapper().readTree(invalidOutput);
        assertEquals("INVALID_INPUT", rejectedResult.path("status").stringValue());
        assertTrue(!rejectedResult.has("timetable"));

        ObjectNode impossible = (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "no-room-plan.json"));
        impossible.put("catalogVersion", 2);
        Path impossiblePath = write("quality-impossible.json", impossible);
        Path impossibleOutput = temporaryDirectory.resolve("quality-impossible-result.json");
        ProcessResult unsuccessful = run("plan", "--definition", impossiblePath.toString(),
                "--output", impossibleOutput.toString(), "--step-limit", "10");
        assertEquals(3, unsuccessful.exitCode(), unsuccessful.stderr());
        JsonNode unsuccessfulResult = JsonSupport.mapper().readTree(impossibleOutput);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", unsuccessfulResult.path("status").stringValue());
        assertTrue(!unsuccessfulResult.has("timetable"));
    }

    @Test
    void mv5LegacyBaselineRepairsMondayUnavailabilityWithoutLongClassGap() throws Exception {
        ObjectNode currentDefinition = mv5();
        currentDefinition.put("catalogVersion", 1);
        ObjectNode current = acceptedMv5Result(currentDefinition);
        assertEquals(4, current.path("score").path("constraintBreakdown").size());
        assertEquals(Map.of("mon", 7L, "tue", 7L, "wed", 5L, "thu", 2L, "fri", 1L),
                java.util.stream.StreamSupport.stream(
                                current.path("timetable").path("assignments").spliterator(), false)
                        .filter(assignment -> "5a".equals(assignment.path("cohortId").stringValue()))
                        .collect(Collectors.groupingBy(
                                assignment -> assignment.path("periodId").stringValue().substring(0, 3),
                                Collectors.counting())));
        SchoolDefinition acceptedDomain = new DefinitionValidator().validateForPlan(
                JsonSupport.mapper().treeToValue(currentDefinition, SchoolDefinitionDto.class)).definition();
        SchoolSchedule acceptedSchedule = assignedMv5Schedule(acceptedDomain);
        SchoolSchedule classOnly = new SchoolSchedule(
                acceptedSchedule.getPeriods(), acceptedSchedule.getRooms(),
                acceptedSchedule.getLessons().stream()
                        .filter(lesson -> "5a".equals(lesson.getCohortId())).toList(),
                acceptedSchedule.getConstraintWeights());
        assertEquals(25, new ScheduleEvaluator().evaluate(
                classOnly, acceptedDomain.softWeights())
                .softMatchCounts().get(KernelCatalog.COHORT_WEEK_BALANCE.id()));
        Path definitionPath = write("mv5-current-definition.json", currentDefinition);
        Path currentPath = write("mv5-current.json", current);
        byte[] originalCurrent = Files.readAllBytes(currentPath);
        Path legacyVerification = temporaryDirectory.resolve("mv5-legacy-verified.json");
        ProcessResult legacyVerify = run("verify", "--definition", definitionPath.toString(),
                "--result", currentPath.toString(), "--output", legacyVerification.toString());
        assertEquals(0, legacyVerify.exitCode(), legacyVerify.stderr());
        assertEquals(1, JsonSupport.mapper().readTree(legacyVerification).path("catalogVersion").intValue());

        ObjectNode successor = currentDefinition.deepCopy();
        successor.put("catalogVersion", 2);
        successor.put("basedOnRevision", current.path("inputRevision").stringValue());
        var available = ((ObjectNode) successor.withArray("teachers").get(0)).putArray("availablePeriodIds");
        successor.withArray("periods").forEach(period -> {
            if (!"MONDAY".equals(period.path("weekday").stringValue())) {
                available.add(period.path("id").stringValue());
            }
        });
        SchoolDefinition successorDomain = new DefinitionValidator().validateForReplan(
                JsonSupport.mapper().treeToValue(successor, SchoolDefinitionDto.class)).definition();
        SchoolSchedule candidate = assignedMv5Schedule(successorDomain);
        var affected = candidate.getLessons().stream()
                .filter(lesson -> lesson.getId().equals("5a.int-o.01")).findFirst().orElseThrow();
        Map<String, org.schoolkernel.solver.PeriodValue> candidatePeriods = candidate.getPeriods().stream()
                .collect(Collectors.toMap(org.schoolkernel.solver.PeriodValue::id, value -> value));
        affected.setPeriod(candidatePeriods.get("fri-6"));
        var late = new ScheduleEvaluator().evaluate(candidate, successorDomain.softWeights());
        affected.setPeriod(candidatePeriods.get("fri-1"));
        var adjacent = new ScheduleEvaluator().evaluate(candidate, successorDomain.softWeights());
        assertTrue(late.feasible() && adjacent.feasible());
        assertEquals(5, late.softMatchCounts().get(KernelCatalog.COHORT_GAP.id()));
        assertEquals(0, adjacent.softMatchCounts().get(KernelCatalog.COHORT_GAP.id()));
        assertTrue(adjacent.ordinaryPreferencePenalty() < late.ordinaryPreferencePenalty());
        Path successorPath = write("mv5-successor.json", successor);
        Path output = temporaryDirectory.resolve("mv5-proposal.json");

        ProcessResult process = run("replan", "--current-definition", definitionPath.toString(),
                "--current", currentPath.toString(), "--definition", successorPath.toString(),
                "--output", output.toString(), "--step-limit", "300000");
        assertEquals(0, process.exitCode(), process.stderr());
        JsonNode proposal = JsonSupport.mapper().readTree(output);
        assertEquals("FEASIBLE", proposal.path("status").stringValue());
        assertEquals(2, proposal.path("catalogVersion").intValue());
        assertEquals(6, proposal.path("score").path("constraintBreakdown").size());
        assertTrue(!assignment(proposal, "5a.int-o.01").path("periodId").stringValue().startsWith("mon-"));
        assertTrue(matches(proposal, KernelCatalog.COHORT_GAP.id()) <= 1,
                proposal.path("score").toString());
        assertEquals(1, proposal.path("score").path("periodMoves").longValue(),
                proposal.path("score").toString());
        assertEquals(0, proposal.path("score").path("roomOnlyMoves").longValue(),
                proposal.path("score").toString());
        assertArrayEquals(originalCurrent, Files.readAllBytes(currentPath));

        Path verification = temporaryDirectory.resolve("mv5-verified.json");
        ProcessResult verify = run("verify", "--definition", successorPath.toString(),
                "--result", output.toString(), "--output", verification.toString());
        assertEquals(0, verify.exitCode(), verify.stderr());
        assertEquals("VERIFIED", JsonSupport.mapper().readTree(verification).path("status").stringValue());
    }

    private ObjectNode acceptedMv5Result(ObjectNode definition) throws Exception {
        var dto = JsonSupport.mapper().treeToValue(definition, SchoolDefinitionDto.class);
        var domain = new DefinitionValidator().validateForPlan(dto).definition();
        var schedule = assignedMv5Schedule(domain);
        var evaluation = new ScheduleEvaluator().evaluate(schedule, domain.softWeights());
        assertTrue(evaluation.feasible());
        return new ResultFactory().feasible(
                "mv5-accepted", 0, domain, new RevisionService().definitionRevision(definition),
                new SolverAdapter.ExecutionControls(null, 1, 0), "STEP_LIMIT", schedule, evaluation);
    }

    private SchoolSchedule assignedMv5Schedule(SchoolDefinition domain) throws Exception {
        var schedule = new PlanningMapper().toPlanningProblem(domain);
        Map<String, org.schoolkernel.solver.PeriodValue> periods = schedule.getPeriods().stream()
                .collect(Collectors.toMap(org.schoolkernel.solver.PeriodValue::id, value -> value));
        Map<String, org.schoolkernel.solver.RoomValue> rooms = schedule.getRooms().stream()
                .collect(Collectors.toMap(org.schoolkernel.solver.RoomValue::id, value -> value));
        Map<String, String[]> assignments;
        try (InputStream input = getClass().getResourceAsStream("/fixtures/mv5-accepted-assignments.tsv")) {
            assignments = Arrays.stream(new String(input.readAllBytes(), StandardCharsets.UTF_8).strip().split("\\R"))
                    .map(line -> line.split("\\t"))
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts));
        }
        assertEquals(schedule.getLessons().size(), assignments.size());
        schedule.getLessons().forEach(lesson -> {
            String[] placement = assignments.get(lesson.getId());
            lesson.setPeriod(periods.get(placement[1]));
            lesson.setRoom(rooms.get(placement[2]));
        });
        return schedule;
    }

    private static JsonNode assignment(JsonNode result, String lessonId) {
        return java.util.stream.StreamSupport.stream(
                        result.path("timetable").path("assignments").spliterator(), false)
                .filter(value -> lessonId.equals(value.path("lessonId").stringValue()))
                .findFirst().orElseThrow();
    }

    private static long matches(JsonNode result, String constraintId) {
        return constraint(result, constraintId).path("matchCount").longValue();
    }

    private static JsonNode constraint(JsonNode result, String constraintId) {
        return java.util.stream.StreamSupport.stream(
                        result.path("score").path("constraintBreakdown").spliterator(), false)
                .filter(row -> constraintId.equals(row.path("constraintId").stringValue()))
                .findFirst().orElseThrow();
    }

    private static ObjectNode mv5() throws Exception {
        return (ObjectNode) JsonSupport.mapper().readTree(Path.of("..", "examples", "mv5.json"));
    }

    private static ObjectNode lateStartDefinition() throws Exception {
        ObjectNode definition = (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "valid-plan.json"));
        definition.put("catalogVersion", 3);
        ((ObjectNode) definition.withArray("periods").get(0)).put("order", 10);
        for (int slot = 2; slot <= 4; slot++) {
            definition.withArray("periods").addObject()
                    .put("id", "mon-" + slot)
                    .put("displayName", "Monday " + slot)
                    .put("weekday", "MONDAY")
                    .put("order", slot * 10);
        }
        ((ObjectNode) definition.withArray("lessons").get(0)).put("periodLock", "mon-4");
        return definition;
    }

    private static ObjectNode twoLessonDailySpreadDefinition() throws Exception {
        ObjectNode definition = mv5();
        definition.put("catalogVersion", 4);
        var lessons = definition.withArray("lessons");
        var selected = new ArrayList<JsonNode>();
        lessons.forEach(lesson -> {
            String id = lesson.path("id").stringValue();
            if (id.equals("5a.int-o.01") || id.equals("5a.literature.01")) {
                ObjectNode copy = (ObjectNode) lesson.deepCopy();
                copy.put("periodLock", id.equals("5a.int-o.01") ? "mon-0" : "mon-2");
                copy.put("roomLock", id.equals("5a.int-o.01") ? "b216" : "a119");
                selected.add(copy);
            }
        });
        lessons.removeAll();
        selected.forEach(lessons::add);
        cohort(definition, "5a").put("maxDailyLessonSpread", 2);
        return definition;
    }

    private static ObjectNode cohort(ObjectNode definition, String cohortId) {
        return java.util.stream.StreamSupport.stream(definition.withArray("cohorts").spliterator(), false)
                .map(ObjectNode.class::cast)
                .filter(value -> cohortId.equals(value.path("id").stringValue()))
                .findFirst().orElseThrow();
    }

    private Path write(String name, JsonNode value) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        Files.write(path, JsonSupport.mapper().writeValueAsBytes(value));
        return path;
    }

    private static ProcessResult run(String... arguments) throws Exception {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-jar");
        command.add(JAR.toString());
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Packaged kernel did not terminate in 60 seconds");
        }
        return new ProcessResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
