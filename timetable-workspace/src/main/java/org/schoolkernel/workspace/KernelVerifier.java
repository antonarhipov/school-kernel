package org.schoolkernel.workspace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class KernelVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(KernelVerifier.class);
    private static final int OUTPUT_LIMIT = 64 * 1024;
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private final String executable;

    public KernelVerifier(@Value("${workspace.kernel-executable}") String executable) {
        this.executable = executable;
    }

    public Verification verify(ImportDocuments documents) {
        String correlationId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        Path directory = null;
        try {
            directory = Files.createTempDirectory("school-workspace-verify-");
            restrict(directory, true);
            Path definition = directory.resolve("school-definition.json");
            Path output = directory.resolve("verification-result.json");
            writePrivate(definition, CanonicalJson.bytes(documents.definition()));
            List<String> arguments = new ArrayList<>(List.of(
                    executable,
                    "verify",
                    "--definition", definition.toString(),
                    "--output", output.toString(),
                    "--correlation-id", correlationId));
            if (documents.result() != null) {
                Path result = directory.resolve("timetable-result.json");
                writePrivate(result, CanonicalJson.bytes(documents.result()));
                arguments.add(4, "--result");
                arguments.add(5, result.toString());
            }
            Process process = new ProcessBuilder(arguments).start();
            Thread stdout = Thread.ofVirtual().start(() -> drain(process.getInputStream()));
            Thread stderr = Thread.ofVirtual().start(() -> drain(process.getErrorStream()));
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
                throw unavailable("Kernel verification did not finish in time.");
            }
            stdout.join();
            stderr.join();
            int exit = process.exitValue();
            JsonNode verification = readVerification(output);
            LOG.info("correlationId={} kernelCommand=verify exitClass={} elapsedTimeMs={}",
                    correlationId, exitClass(exit), elapsedMillis(started));
            if (exit == 0 && "VERIFIED".equals(verification.path("status").stringValue())) {
                return trusted(verification, documents);
            }
            if (exit == 2 && "INVALID_INPUT".equals(verification.path("status").stringValue())) {
                throw new WorkspaceProblem(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "KERNEL_VERIFICATION_FAILED",
                        safeValidationMessage(verification));
            }
            throw unavailable("School Kernel could not verify the import.");
        } catch (WorkspaceProblem problem) {
            throw problem;
        } catch (IOException exception) {
            throw unavailable("School Kernel could not be started.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("School Kernel verification was interrupted.");
        } finally {
            deleteTree(directory);
        }
    }

    private static Verification trusted(JsonNode output, ImportDocuments documents) {
        String mode = output.path("mode").stringValue();
        String expectedMode = documents.mode().name();
        String schoolId = output.path("schoolId").stringValue();
        String definitionRevision = output.path("definitionRevision").stringValue();
        if (!expectedMode.equals(mode)
                || schoolId == null
                || !schoolId.equals(documents.definition().path("schoolId").stringValue())
                || definitionRevision == null
                || output.path("catalogVersion").intValue() != 1
                || output.path("schemaVersion").intValue() != 1) {
            throw unavailable("School Kernel returned mismatched verification evidence.");
        }
        String timetableRevision = null;
        if (documents.mode() == ImportDocuments.ImportMode.ACCEPTED_BASELINE) {
            timetableRevision = output.path("timetableRevision").stringValue();
            if (timetableRevision == null
                    || !definitionRevision.equals(documents.result().path("inputRevision").stringValue())
                    || !timetableRevision.equals(documents.result().path("timetableRevision").stringValue())) {
                throw unavailable("School Kernel returned mismatched baseline evidence.");
            }
        }
        return new Verification(schoolId, definitionRevision, timetableRevision);
    }

    private static JsonNode readVerification(Path output) {
        try {
            if (!Files.isRegularFile(output) || Files.size(output) > 1024 * 1024) {
                throw unavailable("School Kernel did not publish bounded verification evidence.");
            }
            return JSON.readTree(Files.readAllBytes(output));
        } catch (JacksonException exception) {
            throw unavailable("School Kernel published malformed verification evidence.");
        } catch (IOException exception) {
            throw unavailable("School Kernel verification evidence could not be read.");
        }
    }

    private static String safeValidationMessage(JsonNode verification) {
        JsonNode errors = verification.path("validationReport").path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            String message = errors.get(0).path("message").stringValue();
            if (message != null && !message.isBlank()) {
                return "Import verification failed: " + message;
            }
        }
        return "Import verification failed. Check the definition and matching result.";
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
        try (stream; ByteArrayOutputStream bounded = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            int retained = 0;
            while ((read = stream.read(buffer)) >= 0) {
                int keep = Math.min(read, OUTPUT_LIMIT - retained);
                if (keep > 0) {
                    bounded.write(buffer, 0, keep);
                    retained += keep;
                }
            }
        } catch (IOException ignored) {
            // Process outcome and structured file remain authoritative.
        }
    }

    private static String exitClass(int exit) {
        return switch (exit) {
            case 0 -> "VERIFIED";
            case 2 -> "INVALID_INPUT";
            case 4 -> "INTERNAL_ERROR";
            default -> "TRANSPORT";
        };
    }

    private static long elapsedMillis(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    private static WorkspaceProblem unavailable(String message) {
        return new WorkspaceProblem(HttpStatus.SERVICE_UNAVAILABLE, "KERNEL_UNAVAILABLE", message);
    }

    private static void deleteTree(Path directory) {
        if (directory == null) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // A private temporary artifact is never accepted workspace state.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup is retried by the operating system's temporary-file policy.
        }
    }

    public record Verification(String schoolId, String definitionRevision, String timetableRevision) {}
}
