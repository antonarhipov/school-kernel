package org.schoolkernel.workspace;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/** The physical demo boundary. It never receives workspace authority or mutable subscriber state. */
@Component
@Profile("demo")
class DemoKernelRunner {
    @org.springframework.beans.factory.annotation.Autowired(required = false) private DemoEvents events;
    private static final JsonMapper JSON = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private final String executable;
    private final KernelProcessLauncher launcher;
    private final DemoVerificationService verification;
    private final DemoSearchSettings settings;
    DemoKernelRunner(@Value("${workspace.kernel-executable}") String executable, KernelProcessLauncher launcher,
            DemoVerificationService verification, DemoSearchSettings settings) {
        this.executable = KernelVerifier.resolveExecutable(executable); this.launcher = launcher;
        this.verification = verification; this.settings = settings;
    }
    record Input(String operation, JsonNode definition, JsonNode baselineDefinition, JsonNode baselineResult, Duration effort) {}
    record Artifact(JsonNode result, KernelVerifier.Verification verification, long solveProcessMs, long verificationProcessMs, String computationId) {}
    record Outcome(Artifact artifact, String status, String code, String message, JsonNode evidence) {}

    Outcome run(String computationId, Input input, DemoKernelAllowance.Reservation reservation, Runnable checking) {
        Path directory = null; Process process = null;
        long solveMs = 0, verifyMs = 0;
        DemoEvents.ProcessSpan physical = null;
        try {
            directory = Files.createTempDirectory("school-demo-solve-", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            Path definition = write(directory, "definition.json", input.definition());
            Path output = directory.resolve("result.json");
            String correlation = UUID.randomUUID().toString();
            List<String> args = new ArrayList<>(List.of(executable, "INITIAL".equals(input.operation()) ? "plan" : "replan",
                    "--definition", definition.toString(), "--output", output.toString(), "--time-limit", input.effort().toString(),
                    "--seed", "0", "--correlation-id", correlation));
            if ("REPAIR".equals(input.operation())) {
                args.addAll(List.of("--current-definition", write(directory, "current-definition.json", input.baselineDefinition()).toString(),
                        "--current", write(directory, "current-result.json", input.baselineResult()).toString()));
            }
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            Instant before = Instant.now(); long start = System.nanoTime();
            process = launcher.start(List.copyOf(args)); reservation.started(before);
            if (events != null) physical = events.process(DemoEvents.ProcessKind.SOLVE, correlation, computationId);
            Process launched = process;
            Thread stdout = Thread.ofVirtual().start(() -> drain(launched.getInputStream()));
            Thread stderr = Thread.ofVirtual().start(() -> drain(launched.getErrorStream()));
            if (!process.waitFor(input.effort().plus(settings.processOverhead()).toMillis(), TimeUnit.MILLISECONDS))
                return failure("KERNEL_TIMEOUT", "The search did not finish within its bounded process time.");
            reservation.stopped();
            if (physical != null) physical.end(switch (process.exitValue()) { case 0 -> "EXIT_0"; case 2 -> "EXIT_2"; case 3 -> "EXIT_3"; default -> "EXIT_OTHER"; });
            solveMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
            stdout.join(1000); stderr.join(1000);
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            if (!Files.isRegularFile(output) || Files.size(output) > DemoDefinitionLimits.DOCUMENT_BYTES)
                return failure("REJECTED_OUTPUT", "School Kernel returned no complete bounded result.");
            JsonNode result = JSON.readTree(Files.readAllBytes(output));
            String status = result.path("status").asString(""); int exit = process.exitValue();
            if (exit == 3 && "NO_FEASIBLE_SOLUTION_FOUND".equals(status))
                return new Outcome(null, "NOT_FOUND", status,
                        result.path("safeMessage").asString("").equals("The search budget ended before all lessons were placed. A later search may find a feasible timetable.")
                                ? "The search budget ended before all lessons were placed. A later search may find a feasible timetable."
                                : "No feasible timetable was found within this search budget. More time may help but guarantees nothing.", safeEvidence(result));
            if (exit == 2 && "INVALID_INPUT".equals(status))
                return new Outcome(null, "INVALID_INPUT", status, "School Kernel rejected the input. Review the located validation errors.", safeEvidence(result));
            if (exit != 0 || !"FEASIBLE".equals(status)) return failure("KERNEL_FAILED", "School Kernel could not complete this search.");
            if (result.path("schemaVersion").asInt() != input.definition().path("schemaVersion").asInt()
                    || result.path("catalogVersion").asInt() != input.definition().path("catalogVersion").asInt()
                    || !correlation.equals(result.path("correlationId").asString(""))
                    || !input.definition().path("schoolId").equals(result.path("schoolId"))
                    || !result.path("seed").isIntegralNumber() || result.path("seed").asLong() != 0
                    || !"TIME".equals(result.path("limit").path("type").asString(""))
                    || !input.effort().toString().equals(result.path("limit").path("duration").asString(""))
                    || !java.util.Set.of("TIME_LIMIT", "SEARCH_EXHAUSTED").contains(result.path("terminationReason").asString(""))
                    || !result.path("elapsedTimeMs").isIntegralNumber() || result.path("elapsedTimeMs").asLong() < 0
                    || !result.path("timetable").path("assignments").isArray()
                    || "REPAIR".equals(input.operation()) && (!completeChanges(result.path("changeReport"))
                        || !input.baselineResult().path("inputRevision").equals(input.definition().path("basedOnRevision"))))
                return failure("REJECTED_OUTPUT", "School Kernel returned mismatched solve evidence.");
            checking.run();
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            long startVerify = System.nanoTime();
            var verified = verification.verifyReserved(new ImportDocuments(input.definition(), result, null,
                    ImportDocuments.ImportMode.ACCEPTED_BASELINE), reservation);
            verifyMs = Duration.ofNanos(System.nanoTime() - startVerify).toMillis();
            return new Outcome(new Artifact(result.deepCopy(), verified, solveMs, verifyMs, computationId), "FEASIBLE", null, null, null);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); return failure("CANCELLED", "The computation was stopped.");
        } catch (WorkspaceProblem rejected) {
            return failure("REJECTED_OUTPUT", "The complete result could not be verified. No proposal was published.");
        } catch (IOException | tools.jackson.core.JacksonException failure) {
            return failure("KERNEL_UNAVAILABLE", "School Kernel could not start or return a usable result.");
        } finally {
            if (process != null) terminate(process);
            if (physical != null) physical.end("TERMINATED");
            reservation.stopped();
            if (directory != null) {
                try (var paths = Files.walk(directory)) { paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                }); } catch (IOException ignored) {}
            }
        }
    }
    private static JsonNode safeEvidence(JsonNode result) {
        var evidence = JSON.createObjectNode();
        for (String field : List.of("terminationReason", "searchDiagnostics", "validationReport"))
            if (result.has(field)) evidence.set(field, result.path(field).deepCopy());
        return evidence;
    }
    private static boolean completeChanges(JsonNode report) {
        var fields = Set.of("additions", "cancellations", "teacherChanges", "forcedMoves", "periodMoves", "roomOnlyMoves");
        return report.isObject() && (report.size() == fields.size() || report.size() == fields.size() + 1 && report.path("unmatchedChanges").isArray()) && fields.stream().allMatch(field -> report.path(field).isArray());
    }
    private static Outcome failure(String code, String message) { return new Outcome(null, "FAILED", code, message, null); }
    private static Path write(Path directory, String name, JsonNode node) throws IOException {
        Path path = directory.resolve(name); Files.write(path, CanonicalJson.bytes(node));
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------")); return path;
    }
    private static void drain(java.io.InputStream input) { try (input) { input.transferTo(java.io.OutputStream.nullOutputStream()); } catch (IOException ignored) {} }
    private static void terminate(Process process) {
        boolean interrupted = Thread.interrupted();
        try {
            if (process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroy); process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.descendants().forEach(ProcessHandle::destroyForcibly); process.destroyForcibly(); process.waitFor();
                }
            }
        } catch (InterruptedException again) { interrupted = true; process.destroyForcibly(); process.onExit().join(); }
        finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
}
