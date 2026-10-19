import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.schoolkernel.application.*;
import org.schoolkernel.contract.*;
import org.schoolkernel.domain.*;
import org.schoolkernel.solver.*;
import tools.jackson.databind.*;
import tools.jackson.databind.node.*;

/** Maintenance only: full catalog evaluation and deterministic feasible lesson swaps, never claimed scores. */
class BuildTimetableVariants {
    public static void main(String[] args) throws Exception {
        var json = JsonSupport.mapper();
        var root = Path.of(args[0]);
        var loader = new DefinitionLoader(new FileBoundary(), new DefinitionSchemaValidator(),
                new DefinitionValidator(), new RevisionService());
        // An optional identifier regenerates only that school, preserving existing bundled results byte-for-byte.
        for (String id : args.length > 1 ? List.of(args[1]) : List.of("two-class", "full", "star-wars", "english")) {
            var loaded = loader.load(root.resolve(id + "-definition.json"), DefinitionLoader.Mode.INITIAL);
            if (!(loaded instanceof DefinitionLoader.Accepted accepted))
                throw new IllegalStateException("Invalid fictional curriculum: " + id + " " + loaded);
            if (AudienceDefinition.enabled(accepted.document())) {
                for (String variant : List.of("a", "b")) {
                    Path assignments = root.resolve("../../../../../../tools/demo/seeds/" + id + "-" + variant + "-assignments.json").normalize();
                    var reviewed = new ReviewService(loader, new FileBoundary()).handle(root.resolve(id + "-definition.json"), assignments, "bundled-"+id+"-"+variant);
                    if (!(reviewed instanceof CommandOutcome.Succeeded success) || !success.document().path("valid").asBoolean())
                        throw new IllegalStateException("Invalid audience seed: " + id + "/" + variant + " " + reviewed);
                    Files.writeString(root.resolve(id + "-" + variant + "-result.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(success.document().path("result"))+"\n");
                }
                System.out.println(id + ": two independently reviewed audience variants");
                continue;
            }
            var seed = id.equals("two-class")
                    ? json.readTree(Files.readAllBytes(root.resolve("../../../../../../tools/demo/seeds/two-class-result.json").normalize())).path("timetable").path("assignments")
                    : json.readTree(Files.readAllBytes(root.resolve("../../../../../../tools/demo/seeds/"
                            + (id.equals("english") || id.equals("star-wars") ? id : "full") + "-assignments.json").normalize()));
            var placements = new HashMap<String, JsonNode>(); seed.forEach(a -> placements.put(a.path("lessonId").asString(), a));
            var schedule = new PlanningMapper().toPlanningProblem(accepted.definition());
            for (var lesson : schedule.getLessons()) {
                var a = Objects.requireNonNull(placements.get(lesson.getId()));
                lesson.setPeriod(schedule.getPeriods().stream().filter(p -> p.id().equals(a.path("periodId").asString())).findFirst().orElseThrow());
                lesson.setRoom(schedule.getRooms().stream().filter(r -> r.id().equals(a.path("roomId").asString())).findFirst().orElseThrow());
            }
            var evaluator = new ScheduleEvaluator();
            var evaluation = evaluator.evaluate(schedule, accepted.definition().softWeights());
            if (!evaluation.feasible()) throw new IllegalStateException("Seed is invalid: " + id + " " + evaluation);
            save(root, id, "a", accepted, schedule, evaluation);
            int swaps = 0;
            var changed = new HashSet<String>();
            // Keep room choices with lessons; only accept a complete candidate with every hard rule satisfied.
            for (int left = 0; left < schedule.getLessons().size() && swaps < 16; left++) {
                var first = schedule.getLessons().get(left);
                if (changed.contains(first.getId())) continue;
                for (int right = left+1; right < schedule.getLessons().size(); right++) {
                    var second = schedule.getLessons().get(right);
                    if (changed.contains(second.getId()) || !first.getCohortId().equals(second.getCohortId())
                            || first.getPeriod().weekday() == second.getPeriod().weekday()
                            || first.getSubjectId().equals(second.getSubjectId())) continue;
                    var firstPeriod = first.getPeriod(); var secondPeriod = second.getPeriod();
                    first.setPeriod(secondPeriod); second.setPeriod(firstPeriod);
                    var candidate = evaluator.evaluate(schedule, accepted.definition().softWeights());
                    if (candidate.feasible()) {
                        changed.add(first.getId()); changed.add(second.getId()); swaps++; evaluation = candidate; break;
                    }
                    first.setPeriod(firstPeriod); second.setPeriod(secondPeriod);
                }
            }
            if (swaps < 2) throw new IllegalStateException("Need visibly different variants: " + id);
            save(root, id, "b", accepted, schedule, evaluation);
            System.out.println(id + ": two feasible variants, " + changed.size() + " lessons moved across weekdays");
        }
    }
    static void save(Path root, String id, String variant, DefinitionLoader.Accepted accepted,
            SchoolSchedule schedule, ScheduleEvaluator.Evaluation evaluation) throws Exception {
        var result = new ResultFactory().feasible("bundled-"+id+"-"+variant, 0, accepted.definition(), accepted.revision(),
                new SolverAdapter.ExecutionControls(null, 1, 0), "STEP_LIMIT", schedule, evaluation);
        Files.writeString(root.resolve(id + "-" + variant + "-result.json"), JsonSupport.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(result)+"\n");
    }
}
