package org.schoolkernel.workspace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class KernelPlanner {
    private static final Logger LOG = LoggerFactory.getLogger(KernelPlanner.class);
    private static final int CHANNEL_LIMIT = 64 * 1024;
    private static final long RESULT_LIMIT = 10L * 1024 * 1024;

    private final String executable;
    private final KernelProcessLauncher processes;
    private final KernelVerifier verifier;
    private final ObjectMapper json;
    private final Path temporaryRoot;
    private final ConcurrentHashMap<UUID, ActiveProcess> active = new ConcurrentHashMap<>();

    @Autowired
    public KernelPlanner(
            @Value("${workspace.kernel-executable}") String executable,
            KernelProcessLauncher processes,
            KernelVerifier verifier,
            ObjectMapper json) {
        this(executable, processes, verifier, json, Path.of(System.getProperty("java.io.tmpdir")));
    }

    KernelPlanner(
            String executable,
            KernelProcessLauncher processes,
            KernelVerifier verifier,
            ObjectMapper json,
            Path temporaryRoot) {
        this.executable = KernelVerifier.resolveExecutable(executable);
        this.processes = processes;
        this.verifier = verifier;
        this.json = json;
        this.temporaryRoot = temporaryRoot;
    }

    public Outcome plan(UUID runId, JsonNode definition) {
        String correlationId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        Path directory = null;
        ActiveProcess holder = new ActiveProcess();
        ActiveProcess existing = active.putIfAbsent(runId, holder);
        if (existing != null) {
            if (existing.cancelled) {
                active.remove(runId, existing);
                return Outcome.cancelled();
            }
            return Outcome.failed("INTERNAL_ERROR", "The planning run could not be started.");
        }
        try {
            if (holder.cancelled) {
                return Outcome.cancelled();
            }
            directory = Files.createTempDirectory(temporaryRoot, "school-workspace-plan-");
            restrict(directory, true);
            Path definitionPath = directory.resolve("school-definition.json");
            Path outputPath = directory.resolve("timetable-result.json");
            writePrivate(definitionPath, CanonicalJson.bytes(definition));
            List<String> arguments = List.of(
                    executable,
                    "plan",
                    "--definition", definitionPath.toString(),
                    "--output", outputPath.toString(),
                    "--time-limit", "60s",
                    "--correlation-id", correlationId);
            Process process = processes.start(arguments);
            holder.process = process;
            if (holder.cancelled) {
                terminate(process);
                return Outcome.cancelled();
            }
            Thread stdout = Thread.ofVirtual().start(() -> drain(process.getInputStream()));
            Thread stderr = Thread.ofVirtual().start(() -> drain(process.getErrorStream()));
            boolean completed = process.waitFor(70, TimeUnit.SECONDS);
            if (!completed) {
                terminate(process);
                join(stdout, stderr);
                log(correlationId, runId, "TIMEOUT", started, null, false);
                return Outcome.failed("KERNEL_TIMEOUT", "Planning did not finish within its bounded run.");
            }
            join(stdout, stderr);
            if (holder.cancelled) {
                log(correlationId, runId, "CANCELLED", started, null, false);
                return Outcome.cancelled();
            }
            int exit = process.exitValue();
            JsonNode result = readResult(outputPath);
            String status = result.path("status").stringValue();
            if (exit == 0 && "FEASIBLE".equals(status)) {
                if (result.path("schemaVersion").intValue() != 1
                        || !correlationId.equals(result.path("correlationId").stringValue())
                        || !definition.path("schoolId").stringValue().equals(result.path("schoolId").stringValue())
                        || result.path("seed").longValue() != 0L
                        || !"TIME".equals(result.path("limit").path("type").stringValue())
                        || !"PT1M".equals(result.path("limit").path("duration").stringValue())
                        || "STEP_LIMIT".equals(result.path("terminationReason").stringValue())
                        || !result.path("timetable").path("assignments").isArray()) {
                    throw new WorkspaceProblem(
                            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                            "KERNEL_UNAVAILABLE",
                            "School Kernel returned mismatched planning evidence.");
                }
                verifier.verify(new ImportDocuments(
                        definition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
                log(correlationId, runId, "FEASIBLE", started,
                        result.path("terminationReason").stringValue(), true);
                return Outcome.feasible(result);
            }
            if (exit == 3 && "NO_FEASIBLE_SOLUTION_FOUND".equals(status)) {
                log(correlationId, runId, status, started,
                        result.path("terminationReason").stringValue(), false);
                return Outcome.failed(status, "No feasible timetable was found within this run.", result);
            }
            if (exit == 2 && "INVALID_INPUT".equals(status)) {
                log(correlationId, runId, status, started, null, false);
                return Outcome.failed(status, "School Kernel rejected the initial definition.", result);
            }
            log(correlationId, runId, "INTERNAL_ERROR", started, null, false);
            return Outcome.failed("INTERNAL_ERROR", "School Kernel could not complete planning.");
        } catch (WorkspaceProblem problem) {
            log(correlationId, runId, "REJECTED_OUTPUT", started, null, false);
            return Outcome.failed("REJECTED_OUTPUT", "School Kernel returned an unverified result.");
        } catch (IOException exception) {
            log(correlationId, runId, "TRANSPORT", started, null, false);
            return Outcome.failed("TRANSPORT_FAILURE", "School Kernel could not be started.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            Process process = holder.process;
            if (process != null) terminate(process);
            log(correlationId, runId, "INTERRUPTED", started, null, false);
            return Outcome.failed("INTERRUPTED", "Planning was interrupted.");
        } finally {
            active.remove(runId, holder);
            deleteTree(directory);
        }
    }

    public Outcome replan(
            UUID runId,
            JsonNode currentDefinition,
            JsonNode currentResult,
            JsonNode successorDefinition,
            String limit) {
        if (!"PT1M".equals(limit) && !"PT2M".equals(limit)) {
            throw new IllegalArgumentException("Unsupported repair limit");
        }
        String correlationId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        Path directory = null;
        ActiveProcess holder = new ActiveProcess();
        ActiveProcess existing = active.putIfAbsent(runId, holder);
        if (existing != null) {
            if (existing.cancelled) {
                active.remove(runId, existing);
                return Outcome.cancelled();
            }
            return Outcome.failed("INTERNAL_ERROR", "The repair run could not be started.");
        }
        try {
            if (holder.cancelled) return Outcome.cancelled();
            directory = Files.createTempDirectory(temporaryRoot, "school-workspace-replan-");
            restrict(directory, true);
            Path currentDefinitionPath = directory.resolve("accepted-definition.json");
            Path currentResultPath = directory.resolve("accepted-result.json");
            Path successorPath = directory.resolve("successor-definition.json");
            Path outputPath = directory.resolve("repair-result.json");
            writePrivate(currentDefinitionPath, CanonicalJson.bytes(currentDefinition));
            writePrivate(currentResultPath, CanonicalJson.bytes(currentResult));
            writePrivate(successorPath, CanonicalJson.bytes(successorDefinition));
            String cliLimit = "PT2M".equals(limit) ? "120s" : "60s";
            List<String> arguments = List.of(
                    executable,
                    "replan",
                    "--current-definition", currentDefinitionPath.toString(),
                    "--current", currentResultPath.toString(),
                    "--definition", successorPath.toString(),
                    "--output", outputPath.toString(),
                    "--time-limit", cliLimit,
                    "--correlation-id", correlationId);
            Process process = processes.start(arguments);
            holder.process = process;
            if (holder.cancelled) {
                terminate(process);
                return Outcome.cancelled();
            }
            Thread stdout = Thread.ofVirtual().start(() -> drain(process.getInputStream()));
            Thread stderr = Thread.ofVirtual().start(() -> drain(process.getErrorStream()));
            long watchdogSeconds = "PT2M".equals(limit) ? 130 : 70;
            boolean completed = process.waitFor(watchdogSeconds, TimeUnit.SECONDS);
            if (!completed) {
                terminate(process);
                join(stdout, stderr);
                logRepair(correlationId, runId, "TIMEOUT", started, limit, null, false, null);
                return Outcome.failed("KERNEL_TIMEOUT", "Repair generation did not finish within its bounded run.");
            }
            join(stdout, stderr);
            if (holder.cancelled) {
                logRepair(correlationId, runId, "CANCELLED", started, limit, null, false, null);
                return Outcome.cancelled();
            }
            int exit = process.exitValue();
            JsonNode result = readResult(outputPath);
            String status = result.path("status").stringValue();
            if (exit == 0 && "FEASIBLE".equals(status)) {
                if (result.path("schemaVersion").intValue() != 1
                        || !correlationId.equals(result.path("correlationId").stringValue())
                        || !successorDefinition.path("schoolId").stringValue()
                                .equals(result.path("schoolId").stringValue())
                        || result.path("seed").longValue() != 0L
                        || !"TIME".equals(result.path("limit").path("type").stringValue())
                        || !limit.equals(result.path("limit").path("duration").stringValue())
                        || "STEP_LIMIT".equals(result.path("terminationReason").stringValue())
                        || !result.path("timetable").path("assignments").isArray()
                        || !completeChangeReport(result.path("changeReport"))) {
                    throw new WorkspaceProblem(
                            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                            "KERNEL_UNAVAILABLE",
                            "School Kernel returned mismatched repair evidence.");
                }
                verifier.verify(new ImportDocuments(
                        successorDefinition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
                logRepair(correlationId, runId, "FEASIBLE", started, limit,
                        result.path("terminationReason").stringValue(), true, result.path("changeReport"));
                return Outcome.feasible(result);
            }
            if (exit == 3 && "NO_FEASIBLE_SOLUTION_FOUND".equals(status)) {
                logRepair(correlationId, runId, status, started, limit,
                        result.path("terminationReason").stringValue(), false, null);
                return Outcome.failed(status, "No feasible repair was found within this run.", result);
            }
            if (exit == 2 && "INVALID_INPUT".equals(status)) {
                logRepair(correlationId, runId, status, started, limit, null, false, null);
                return Outcome.failed(status, "School Kernel rejected the repair definition.", result);
            }
            logRepair(correlationId, runId, "INTERNAL_ERROR", started, limit, null, false, null);
            return Outcome.failed("INTERNAL_ERROR", "School Kernel could not complete repair generation.");
        } catch (WorkspaceProblem problem) {
            logRepair(correlationId, runId, "REJECTED_OUTPUT", started, limit, null, false, null);
            return Outcome.failed("REJECTED_OUTPUT", "School Kernel returned an unverified repair result.");
        } catch (IOException exception) {
            logRepair(correlationId, runId, "TRANSPORT", started, limit, null, false, null);
            return Outcome.failed("TRANSPORT_FAILURE", "School Kernel could not be started.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            Process process = holder.process;
            if (process != null) terminate(process);
            logRepair(correlationId, runId, "INTERRUPTED", started, limit, null, false, null);
            return Outcome.failed("INTERRUPTED", "Repair generation was interrupted.");
        } finally {
            active.remove(runId, holder);
            deleteTree(directory);
        }
    }

    private static boolean completeChangeReport(JsonNode report) {
        return report.isObject()
                && report.path("additions").isArray()
                && report.path("cancellations").isArray()
                && report.path("teacherChanges").isArray()
                && report.path("forcedMoves").isArray()
                && report.path("periodMoves").isArray()
                && report.path("roomOnlyMoves").isArray()
                && report.size() == 6;
    }

    public boolean cancel(UUID runId) {
        ActiveProcess holder = active.computeIfAbsent(runId, ignored -> new ActiveProcess());
        holder.cancelled = true;
        Process process = holder.process;
        if (process != null) terminate(process);
        return true;
    }

    private JsonNode readResult(Path output) {
        try {
            if (!Files.isRegularFile(output) || Files.size(output) > RESULT_LIMIT) {
                throw new IOException("Missing or oversized result");
            }
            return json.readTree(Files.readAllBytes(output));
        } catch (IOException | JacksonException exception) {
            throw new WorkspaceProblem(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "KERNEL_UNAVAILABLE",
                    "School Kernel returned no usable result.");
        }
    }

    private static void terminate(Process process) {
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private static void join(Thread... threads) throws InterruptedException {
        for (Thread thread : threads) thread.join();
    }

    private static void writePrivate(Path path, byte[] bytes) throws IOException {
        Files.write(path, bytes);
        restrict(path, false);
    }

    private static void restrict(Path path, boolean directory) throws IOException {
        FileStore store = Files.getFileStore(path);
        if (store.supportsFileAttributeView("posix")) {
            Set<PosixFilePermission> permissions = directory
                    ? EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE)
                    : EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, permissions);
        }
    }

    private static void drain(InputStream stream) {
        try (stream; ByteArrayOutputStream retained = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                int keep = Math.min(read, CHANNEL_LIMIT - retained.size());
                if (keep > 0) retained.write(buffer, 0, keep);
            }
        } catch (IOException ignored) {
            // Structured result and exit code are authoritative.
        }
    }

    private static void log(
            String correlationId,
            UUID runId,
            String exitClass,
            long started,
            String terminationReason,
            boolean feasible) {
        LOG.info(
                "correlationId={} runId={} kernelCommand=plan exitClass={} elapsedTimeMs={} configuredLimit=PT1M terminationReason={} feasible={}",
                correlationId, runId, exitClass,
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                terminationReason == null ? "NONE" : terminationReason,
                feasible);
    }

    private static void logRepair(
            String correlationId,
            UUID runId,
            String exitClass,
            long started,
            String limit,
            String terminationReason,
            boolean feasible,
            JsonNode changes) {
        LOG.info(
                "correlationId={} runId={} kernelCommand=replan exitClass={} elapsedTimeMs={} configuredLimit={} terminationReason={} feasible={} additions={} cancellations={} teacherChanges={} forcedMoves={} periodMoves={} roomOnlyMoves={}",
                correlationId, runId, exitClass,
                Duration.ofNanos(System.nanoTime() - started).toMillis(), limit,
                terminationReason == null ? "NONE" : terminationReason,
                feasible,
                changes == null ? 0 : changes.path("additions").size(),
                changes == null ? 0 : changes.path("cancellations").size(),
                changes == null ? 0 : changes.path("teacherChanges").size(),
                changes == null ? 0 : changes.path("forcedMoves").size(),
                changes == null ? 0 : changes.path("periodMoves").size(),
                changes == null ? 0 : changes.path("roomOnlyMoves").size());
    }

    private static void deleteTree(Path directory) {
        if (directory == null) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Private temporary data is never accepted workspace state.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup only.
        }
    }

    private static final class ActiveProcess {
        volatile Process process;
        volatile boolean cancelled;
    }

    public record Outcome(Kind kind, JsonNode result, String code, String message) {
        public static Outcome feasible(JsonNode result) {
            return new Outcome(Kind.FEASIBLE, result, null, null);
        }

        public static Outcome failed(String code, String message) {
            return new Outcome(Kind.FAILED, null, code, message);
        }

        public static Outcome failed(String code, String message, JsonNode evidence) {
            return new Outcome(Kind.FAILED, evidence, code, message);
        }

        public static Outcome cancelled() {
            return new Outcome(Kind.CANCELLED, null, "CANCELLED", "Planning was cancelled.");
        }
    }

    public enum Kind { FEASIBLE, FAILED, CANCELLED }
}
