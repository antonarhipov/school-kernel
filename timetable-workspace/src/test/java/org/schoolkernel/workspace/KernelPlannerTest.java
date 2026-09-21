package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@ExtendWith(OutputCaptureExtension.class)
class KernelPlannerTest {
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    @TempDir
    Path temporaryRoot;

    @Test
    @DisplayName("UC-2 RULE-10/12/13: planner uses explicit private files, the 30-second preset, bounded process channels, and complete cleanup")
    void usesPrivateBoundedProcessBoundaryAndCleansIt() throws Exception {
        AtomicBoolean verified = new AtomicBoolean();
        KernelProcessLauncher launcher = new ResultWritingLauncher(false);
        KernelVerifier verifier = new KernelVerifier("kernel", launcher, temporaryRoot) {
            @Override
            public Verification verify(ImportDocuments documents) {
                verified.set(true);
                assertEquals("FEASIBLE", documents.result().path("status").stringValue());
                return new Verification("demo-school", revision('1'), revision('2'));
            }
        };
        KernelPlanner planner = new KernelPlanner("kernel", launcher, verifier, JSON, temporaryRoot);

        KernelPlanner.Outcome outcome = planner.plan(java.util.UUID.randomUUID(), definition());

        assertEquals(KernelPlanner.Kind.FEASIBLE, outcome.kind());
        assertTrue(verified.get());
        try (var paths = Files.list(temporaryRoot)) {
            assertTrue(paths.findAny().isEmpty(), "temporary invocation directory must be removed");
        }
    }

    @Test
    @DisplayName("UC-2 extension 3b and RULE-17: mismatched execution evidence is rejected before verification with no candidate")
    void rejectsMismatchedResultEvidence() throws Exception {
        AtomicBoolean verified = new AtomicBoolean();
        KernelProcessLauncher launcher = new ResultWritingLauncher(true);
        KernelVerifier verifier = new KernelVerifier("kernel", launcher, temporaryRoot) {
            @Override
            public Verification verify(ImportDocuments documents) {
                verified.set(true);
                throw new AssertionError("mismatched output must not reach verification");
            }
        };
        KernelPlanner planner = new KernelPlanner("kernel", launcher, verifier, JSON, temporaryRoot);

        KernelPlanner.Outcome outcome = planner.plan(java.util.UUID.randomUUID(), definition());

        assertEquals(KernelPlanner.Kind.FAILED, outcome.kind());
        assertEquals("REJECTED_OUTPUT", outcome.code());
        assertFalse(verified.get());
        assertEquals(null, outcome.result());
        try (var paths = Files.list(temporaryRoot)) {
            assertTrue(paths.findAny().isEmpty());
        }
    }

    @Test
    @DisplayName("UC-2 extension 2a and RULE-12: cancellation escalates after two seconds and publishes no proposal")
    void forceCancelsAnUncooperativeProcess() throws Exception {
        StubbornProcess process = new StubbornProcess();
        CountDownLatch started = new CountDownLatch(1);
        KernelProcessLauncher launcher = new KernelProcessLauncher() {
            @Override
            public Process start(List<String> arguments) {
                started.countDown();
                return process;
            }
        };
        KernelVerifier verifier = new KernelVerifier("kernel", launcher, temporaryRoot) {
            @Override
            public Verification verify(ImportDocuments documents) {
                throw new AssertionError("a cancelled run must never verify output");
            }
        };
        KernelPlanner planner = new KernelPlanner("kernel", launcher, verifier, JSON, temporaryRoot);
        UUID runId = UUID.randomUUID();
        CompletableFuture<KernelPlanner.Outcome> outcome = CompletableFuture.supplyAsync(
                () -> planner.plan(runId, definition()));
        assertTrue(started.await(2, TimeUnit.SECONDS));

        planner.cancel(runId);

        assertEquals(KernelPlanner.Kind.CANCELLED, outcome.get(5, TimeUnit.SECONDS).kind());
        assertTrue(process.destroyCalled);
        assertTrue(process.forceCalled);
        try (var paths = Files.list(temporaryRoot)) {
            assertTrue(paths.findAny().isEmpty());
        }
    }

    @Test
    @DisplayName("UC-2 extension 3a: invalid input, unsuccessful search, internal error, and transport failure disclose no candidate")
    void mapsKernelFailureClassesWithoutCandidate(CapturedOutput output) throws Exception {
        KernelPlanner.Outcome invalid = planner(new StructuredFailureLauncher(2, "INVALID_INPUT"))
                .plan(UUID.randomUUID(), definition());
        assertEquals(KernelPlanner.Kind.FAILED, invalid.kind());
        assertEquals("INVALID_INPUT", invalid.code());
        assertTrue(invalid.result().path("validationReport").isObject());

        KernelPlanner.Outcome unsuccessful = planner(
                new StructuredFailureLauncher(3, "NO_FEASIBLE_SOLUTION_FOUND"))
                .plan(UUID.randomUUID(), definition());
        assertEquals(KernelPlanner.Kind.FAILED, unsuccessful.kind());
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", unsuccessful.code());
        assertEquals("No feasible timetable was found within this run.", unsuccessful.message());
        assertFalse(unsuccessful.result().has("timetable"));

        KernelPlanner.Outcome internal = planner(new StructuredFailureLauncher(4, "INTERNAL_ERROR"))
                .plan(UUID.randomUUID(), definition());
        assertEquals(KernelPlanner.Kind.FAILED, internal.kind());
        assertEquals("INTERNAL_ERROR", internal.code());
        assertEquals(null, internal.result());

        KernelPlanner.Outcome transport = planner(new KernelProcessLauncher() {
            @Override
            public Process start(List<String> arguments) throws IOException {
                throw new IOException("injected transport failure");
            }
        }).plan(UUID.randomUUID(), definition());
        assertEquals(KernelPlanner.Kind.FAILED, transport.kind());
        assertEquals("TRANSPORT_FAILURE", transport.code());
        assertEquals(null, transport.result());

        assertFalse(output.getAll().contains(StructuredFailureLauncher.RAW_DIAGNOSTIC));
        try (var paths = Files.list(temporaryRoot)) {
            assertTrue(paths.findAny().isEmpty());
        }
    }

    @Test
    @DisplayName("UC-2 extensions 3a and 3b: interruption, missing output, and malformed output are safe and disclose no candidate")
    void rejectsInterruptedMissingAndMalformedOutput() throws Exception {
        InterruptingProcess interruptedProcess = new InterruptingProcess();
        KernelPlanner.Outcome interrupted;
        try {
            interrupted = planner(new KernelProcessLauncher() {
                @Override
                public Process start(List<String> arguments) {
                    return interruptedProcess;
                }
            }).plan(UUID.randomUUID(), definition());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertEquals(KernelPlanner.Kind.FAILED, interrupted.kind());
        assertEquals("INTERRUPTED", interrupted.code());
        assertEquals(null, interrupted.result());
        assertTrue(interruptedProcess.destroyCalled);
        assertTrue(interruptedProcess.forceCalled);

        KernelPlanner.Outcome missing = planner(new KernelProcessLauncher() {
            @Override
            public Process start(List<String> arguments) {
                return new CompletedProcess(0);
            }
        }).plan(UUID.randomUUID(), definition());
        assertEquals(KernelPlanner.Kind.FAILED, missing.kind());
        assertEquals("REJECTED_OUTPUT", missing.code());
        assertEquals(null, missing.result());

        KernelPlanner.Outcome malformed = planner(new KernelProcessLauncher() {
            @Override
            public Process start(List<String> arguments) throws IOException {
                Path output = Path.of(arguments.get(arguments.indexOf("--output") + 1));
                Files.writeString(output, "not-json");
                return new CompletedProcess(0);
            }
        }).plan(UUID.randomUUID(), definition());
        assertEquals(KernelPlanner.Kind.FAILED, malformed.kind());
        assertEquals("REJECTED_OUTPUT", malformed.code());
        assertEquals(null, malformed.result());

        try (var paths = Files.list(temporaryRoot)) {
            assertTrue(paths.findAny().isEmpty());
        }
    }

    @Test
    @DisplayName("UC-5 main/extensions and RULE-10/12/13/17: replan uses four private paths, exact presets, independent verification, and authoritative changes")
    void invokesVerifiedReplanBoundaryWithExactPresetAndChangeReport() throws Exception {
        AtomicBoolean verified = new AtomicBoolean();
        KernelProcessLauncher launcher = new ReplanResultWritingLauncher();
        KernelVerifier verifier = new KernelVerifier("kernel", launcher, temporaryRoot) {
            @Override
            public Verification verify(ImportDocuments documents) {
                verified.set(true);
                assertEquals("FEASIBLE", documents.result().path("status").stringValue());
                assertEquals(revision('1'), documents.result().path("inputRevision").stringValue());
                return new Verification("demo-school", revision('1'), revision('2'));
            }
        };
        KernelPlanner planner = new KernelPlanner("kernel", launcher, verifier, JSON, temporaryRoot);

        KernelPlanner.Outcome outcome = planner.replan(
                UUID.randomUUID(), definition(), JSON.createObjectNode(), definition(), "PT2M");

        assertEquals(KernelPlanner.Kind.FEASIBLE, outcome.kind());
        assertTrue(verified.get());
        assertEquals(6, outcome.result().path("changeReport").size());
        try (var paths = Files.list(temporaryRoot)) {
            assertTrue(paths.findAny().isEmpty());
        }

        KernelPlanner.Outcome unsuccessful = planner(new StructuredFailureLauncher(3, "NO_FEASIBLE_SOLUTION_FOUND"))
                .replan(UUID.randomUUID(), definition(), JSON.createObjectNode(), definition(), "PT30S");
        assertEquals(KernelPlanner.Kind.FAILED, unsuccessful.kind());
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", unsuccessful.code());
        assertFalse(unsuccessful.result().has("timetable"));
    }

    @Test
    @DisplayName("UC-2 extension 3a and RULE-12: watchdog expiry force-terminates the process and publishes no proposal")
    void watchdogForceTerminatesHungProcess() throws Exception {
        ImmediateTimeoutProcess process = new ImmediateTimeoutProcess();
        KernelPlanner planner = planner(new KernelProcessLauncher() {
            @Override
            public Process start(List<String> arguments) {
                return process;
            }
        });

        KernelPlanner.Outcome outcome = planner.plan(UUID.randomUUID(), definition());

        assertEquals(KernelPlanner.Kind.FAILED, outcome.kind());
        assertEquals("KERNEL_TIMEOUT", outcome.code());
        assertEquals(null, outcome.result());
        assertTrue(process.destroyCalled);
        assertTrue(process.forceCalled);
        try (var paths = Files.list(temporaryRoot)) {
            assertTrue(paths.findAny().isEmpty());
        }
    }

    private KernelPlanner planner(KernelProcessLauncher launcher) {
        KernelVerifier verifier = new KernelVerifier("kernel", launcher, temporaryRoot) {
            @Override
            public Verification verify(ImportDocuments documents) {
                throw new AssertionError("a failed planning result must never reach verification");
            }
        };
        return new KernelPlanner("kernel", launcher, verifier, JSON, temporaryRoot);
    }

    private static ObjectNode definition() {
        ObjectNode definition = JSON.createObjectNode();
        definition.put("schoolId", "demo-school");
        return definition;
    }

    private static String revision(char digit) {
        return "sha256:" + String.valueOf(digit).repeat(64);
    }

    private final class ResultWritingLauncher extends KernelProcessLauncher {
        private final boolean mismatch;

        private ResultWritingLauncher(boolean mismatch) {
            this.mismatch = mismatch;
        }

        @Override
        public Process start(List<String> arguments) throws java.io.IOException {
            assertEquals("plan", arguments.get(1));
            assertTrue(arguments.containsAll(List.of("--definition", "--output", "--time-limit", "30s", "--correlation-id")));
            assertFalse(arguments.contains("--debug"));
            assertFalse(arguments.contains("--seed"));
            assertFalse(arguments.contains("--step-limit"));
            Path definition = Path.of(arguments.get(arguments.indexOf("--definition") + 1));
            Path output = Path.of(arguments.get(arguments.indexOf("--output") + 1));
            assertFalse(definition.equals(output));
            Set<PosixFilePermission> directoryPermissions = Files.getPosixFilePermissions(definition.getParent());
            Set<PosixFilePermission> definitionPermissions = Files.getPosixFilePermissions(definition);
            assertFalse(directoryPermissions.contains(PosixFilePermission.GROUP_READ));
            assertFalse(directoryPermissions.contains(PosixFilePermission.OTHERS_READ));
            assertFalse(definitionPermissions.contains(PosixFilePermission.GROUP_READ));
            assertFalse(definitionPermissions.contains(PosixFilePermission.OTHERS_READ));
            String correlationId = arguments.get(arguments.indexOf("--correlation-id") + 1);
            ObjectNode result = JSON.createObjectNode();
            result.put("schemaVersion", 1);
            result.put("status", "FEASIBLE");
            result.put("correlationId", correlationId);
            result.put("schoolId", "demo-school");
            result.put("seed", 0);
            ObjectNode limit = result.putObject("limit");
            limit.put("type", "TIME");
            limit.put("duration", mismatch ? "PT2M" : "PT30S");
            result.put("terminationReason", "TIME_LIMIT");
            result.put("inputRevision", revision('1'));
            result.put("timetableRevision", revision('2'));
            result.put("elapsedTimeMs", 1);
            result.putObject("timetable").putArray("assignments");
            Files.write(output, JSON.writeValueAsBytes(result));
            return new ProcessBuilder("/usr/bin/true").start();
        }
    }

    private final class ReplanResultWritingLauncher extends KernelProcessLauncher {
        @Override
        public Process start(List<String> arguments) throws IOException {
            assertEquals("replan", arguments.get(1));
            assertTrue(arguments.containsAll(List.of(
                    "--current-definition", "--current", "--definition", "--output",
                    "--time-limit", "120s", "--correlation-id")));
            assertFalse(arguments.contains("--debug"));
            assertFalse(arguments.contains("--seed"));
            assertFalse(arguments.contains("--step-limit"));
            Set<Path> paths = Set.of(
                    Path.of(arguments.get(arguments.indexOf("--current-definition") + 1)),
                    Path.of(arguments.get(arguments.indexOf("--current") + 1)),
                    Path.of(arguments.get(arguments.indexOf("--definition") + 1)),
                    Path.of(arguments.get(arguments.indexOf("--output") + 1)));
            assertEquals(4, paths.size());
            paths.stream().filter(Files::exists).forEach(path -> {
                try {
                    assertFalse(Files.getPosixFilePermissions(path).contains(PosixFilePermission.GROUP_READ));
                } catch (IOException exception) {
                    throw new AssertionError(exception);
                }
            });
            String correlationId = arguments.get(arguments.indexOf("--correlation-id") + 1);
            ObjectNode result = JSON.createObjectNode();
            result.put("schemaVersion", 1).put("status", "FEASIBLE").put("correlationId", correlationId)
                    .put("schoolId", "demo-school").put("seed", 0).put("terminationReason", "TIME_LIMIT")
                    .put("inputRevision", revision('1')).put("timetableRevision", revision('2')).put("elapsedTimeMs", 1);
            result.putObject("limit").put("type", "TIME").put("duration", "PT2M");
            result.putObject("timetable").putArray("assignments");
            ObjectNode report = result.putObject("changeReport");
            for (String category : List.of("additions", "cancellations", "teacherChanges", "forcedMoves", "periodMoves", "roomOnlyMoves")) {
                report.putArray(category);
            }
            Path output = Path.of(arguments.get(arguments.indexOf("--output") + 1));
            Files.write(output, JSON.writeValueAsBytes(result));
            return new CompletedProcess(0);
        }
    }

    private final class StructuredFailureLauncher extends KernelProcessLauncher {
        private static final String RAW_DIAGNOSTIC =
                "jdbc:postgresql://secret/path SQL stack trace Teacher One assignment";
        private final int exit;
        private final String status;

        private StructuredFailureLauncher(int exit, String status) {
            this.exit = exit;
            this.status = status;
        }

        @Override
        public Process start(List<String> arguments) throws IOException {
            Path output = Path.of(arguments.get(arguments.indexOf("--output") + 1));
            ObjectNode result = JSON.createObjectNode();
            result.put("status", status);
            result.put("elapsedTimeMs", 1);
            result.put("terminationReason", "TIME_LIMIT");
            if ("INVALID_INPUT".equals(status)) {
                result.putObject("validationReport").putArray("errors");
            }
            Files.write(output, JSON.writeValueAsBytes(result));
            return new CompletedProcess(exit, RAW_DIAGNOSTIC);
        }
    }

    private static final class CompletedProcess extends Process {
        private final int exit;
        private final InputStream stderr;

        private CompletedProcess(int exit) {
            this(exit, "");
        }

        private CompletedProcess(int exit, String stderr) {
            this.exit = exit;
            this.stderr = new ByteArrayInputStream(stderr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }

        @Override
        public InputStream getInputStream() { return InputStream.nullInputStream(); }

        @Override
        public InputStream getErrorStream() { return stderr; }

        @Override
        public int waitFor() { return exit; }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) { return true; }

        @Override
        public int exitValue() { return exit; }

        @Override
        public void destroy() {}

        @Override
        public Process destroyForcibly() { return this; }

        @Override
        public boolean isAlive() { return false; }
    }

    private static final class InterruptingProcess extends Process {
        private volatile boolean alive = true;
        private volatile boolean destroyCalled;
        private volatile boolean forceCalled;

        @Override
        public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }

        @Override
        public InputStream getInputStream() { return InputStream.nullInputStream(); }

        @Override
        public InputStream getErrorStream() { return InputStream.nullInputStream(); }

        @Override
        public int waitFor() throws InterruptedException {
            throw new InterruptedException("injected interruption");
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException("injected interruption");
        }

        @Override
        public int exitValue() {
            if (alive) throw new IllegalThreadStateException();
            return 137;
        }

        @Override
        public void destroy() { destroyCalled = true; }

        @Override
        public Process destroyForcibly() {
            forceCalled = true;
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() { return alive; }
    }

    private static final class ImmediateTimeoutProcess extends Process {
        private volatile boolean alive = true;
        private volatile boolean destroyCalled;
        private volatile boolean forceCalled;

        @Override
        public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }

        @Override
        public InputStream getInputStream() { return InputStream.nullInputStream(); }

        @Override
        public InputStream getErrorStream() { return InputStream.nullInputStream(); }

        @Override
        public int waitFor() throws InterruptedException {
            while (alive) Thread.sleep(10);
            return 137;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return !alive;
        }

        @Override
        public int exitValue() {
            if (alive) throw new IllegalThreadStateException();
            return 137;
        }

        @Override
        public void destroy() { destroyCalled = true; }

        @Override
        public Process destroyForcibly() {
            forceCalled = true;
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() { return alive; }
    }

    private static final class StubbornProcess extends Process {
        private volatile boolean alive = true;
        private volatile boolean destroyCalled;
        private volatile boolean forceCalled;

        @Override
        public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }

        @Override
        public InputStream getInputStream() { return InputStream.nullInputStream(); }

        @Override
        public InputStream getErrorStream() { return InputStream.nullInputStream(); }

        @Override
        public synchronized int waitFor() throws InterruptedException {
            while (alive) wait();
            return 137;
        }

        @Override
        public synchronized boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            if (!alive) return true;
            unit.timedWait(this, timeout);
            return !alive;
        }

        @Override
        public int exitValue() {
            if (alive) throw new IllegalThreadStateException();
            return 137;
        }

        @Override
        public void destroy() { destroyCalled = true; }

        @Override
        public synchronized Process destroyForcibly() {
            forceCalled = true;
            alive = false;
            notifyAll();
            return this;
        }

        @Override
        public boolean isAlive() { return alive; }
    }
}
