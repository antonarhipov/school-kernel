import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.schoolkernel.application.*;
import org.schoolkernel.contract.*;
import org.schoolkernel.domain.*;
import org.schoolkernel.solver.*;

/** Maintenance only: evaluate the stored assignments, never solve or accept claimed scores. */
class VerifySampleScores {
    public static void main(String[] args) throws Exception {
        var json = JsonSupport.mapper();
        var loader = new DefinitionLoader(new FileBoundary(), new DefinitionSchemaValidator(),
                new DefinitionValidator(), new RevisionService());
        var evidence = json.createObjectNode().put("catalogVersion", KernelCatalog.VERSION);
        var samples = evidence.putObject("samples");
        Path root = Path.of(args[0]);
        for (String id : List.of("two-class", "full", "star-wars", "english")) {
            var loaded = loader.load(root.resolve(id + "-definition.json"), DefinitionLoader.Mode.INITIAL);
            if (loaded instanceof DefinitionLoader.Accepted audience && AudienceDefinition.enabled(audience.document())) {
                for (String variant : List.of("a", "b")) {
                    var path = root.resolve(id + "-" + variant + "-result.json");
                    var result = json.readTree(Files.readAllBytes(path));
                    var reviewed = new ReviewService(loader, new FileBoundary()).handle(root.resolve(id + "-definition.json"), path, "maintenance");
                    if (!(reviewed instanceof CommandOutcome.Succeeded success) || !success.document().path("valid").asBoolean()
                            || success.document().path("assertedQualityMismatch").asBoolean()
                            || !success.document().path("result").path("timetableRevision").equals(result.path("timetableRevision"))
                            || !AudiencePairValidator.equivalent(success.document().path("result").path("effectiveSoftWeights"), result.path("effectiveSoftWeights")))
                        throw new IllegalStateException("Invalid or stale audience sample: " + id + "/" + variant + " " + reviewed);
                    samples.set(id + "/" + variant, result.path("score"));
                }
                continue;
            }
            if (!(loaded instanceof DefinitionLoader.Accepted accepted)
                    || accepted.definition().catalogVersion() != KernelCatalog.ROOM_ASSIGNMENT_VERSION)
                throw new IllegalStateException("Sample is not a valid legacy sample-catalog initial definition: " + id);
            for (String variant : List.of("a", "b")) {
            var result = json.readTree(Files.readAllBytes(root.resolve(id + "-" + variant + "-result.json")));
            var current = new CurrentTimetableReader().read(result);
            if (!current.report().isValid() || !new BaselineVerifier().verify(accepted.definition(), current.timetable()).isValid()
                    || !accepted.revision().equals(result.path("inputRevision").asString()))
                throw new IllegalStateException("Invalid sample result: " + id);
            var schedule = new PlanningMapper().toPlanningProblem(accepted.definition());
            var assignments = new HashMap<String, CurrentTimetableReader.Assignment>();
            current.timetable().assignments().forEach(a -> assignments.put(a.lessonId(), a));
            for (var lesson : schedule.getLessons()) {
                var assignment = assignments.get(lesson.getId());
                lesson.setPeriod(schedule.getPeriods().stream().filter(p -> p.id().equals(assignment.periodId())).findFirst().orElseThrow());
                lesson.setRoom(schedule.getRooms().stream().filter(r -> r.id().equals(assignment.roomId())).findFirst().orElseThrow());
            }
            var evaluation = new ScheduleEvaluator().evaluate(schedule, accepted.definition().softWeights());
            var rescored = new ResultFactory().feasible("maintenance", 0, accepted.definition(), accepted.revision(),
                    new SolverAdapter.ExecutionControls(Duration.ofSeconds(5), null, 0), "TIME_LIMIT", schedule, evaluation);
            if (!evaluation.complete() || !evaluation.feasible() || !json.readTree(json.writeValueAsString(rescored.path("score"))).equals(result.path("score"))
                    || !rescored.path("timetableRevision").equals(result.path("timetableRevision"))
                    || !json.readTree(json.writeValueAsString(rescored.path("effectiveSoftWeights"))).equals(result.path("effectiveSoftWeights")))
                throw new IllegalStateException("Stored scoring is stale; regenerate the result: " + id);
            samples.set(id + "/" + variant, result.path("score"));
            }
        }
        System.out.println(json.writeValueAsString(evidence));
    }
}
