package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class KernelVerifierTest {
    @TempDir
    Path temporaryRoot;

    @Test
    @DisplayName("UC-1 G5 and RULE-13: interruption is safe and removes every private process artifact")
    void interruptionLeavesNoArtifact(CapturedOutput output) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        KernelProcessLauncher launcher = new KernelProcessLauncher() {
            @Override
            public Process start(List<String> arguments) {
                started.countDown();
                return new BlockingProcess();
            }
        };
        KernelVerifier verifier = new KernelVerifier("kernel", launcher, temporaryRoot);
        ImportDocuments documents = initialDocuments();
        AtomicReference<WorkspaceProblem> observed = new AtomicReference<>();
        Thread verification = Thread.ofVirtual().start(() -> {
            try {
                verifier.verify(documents);
            } catch (WorkspaceProblem problem) {
                observed.set(problem);
            }
        });

        assertTrue(started.await(5, TimeUnit.SECONDS));
        verification.interrupt();
        verification.join();

        assertEquals("KERNEL_UNAVAILABLE", observed.get().code());
        assertTrue(verification.isInterrupted());
        assertDirectoryEmpty();
        assertTrue(output.getAll().contains("kernelCommand=verify exitClass=INTERRUPTED"));
        assertFalse(output.getAll().contains("School"));
    }

    @Test
    @DisplayName("RULE-13: process arguments are explicit, files are owner-only, channels are bounded, and cleanup is complete")
    void privateBoundedProcessBoundary(CapturedOutput output) throws Exception {
        AtomicReference<List<String>> command = new AtomicReference<>();
        KernelProcessLauncher launcher = new KernelProcessLauncher() {
            @Override
            public Process start(List<String> arguments) throws java.io.IOException {
                command.set(arguments);
                Path definition = Path.of(arguments.get(arguments.indexOf("--definition") + 1));
                Path output = Path.of(arguments.get(arguments.indexOf("--output") + 1));
                assertPrivate(definition.getParent(), true);
                assertPrivate(definition, false);
                Files.writeString(output, """
                        {"schemaVersion":1,"status":"VERIFIED","mode":"INITIAL_DEFINITION",
                         "schoolId":"school","catalogVersion":1,
                         "definitionRevision":"sha256:0000000000000000000000000000000000000000000000000000000000000000"}
                        """);
                return new CompletedProcess(0, new byte[70 * 1024], new byte[70 * 1024]);
            }
        };

        KernelVerifier.Verification result =
                new KernelVerifier("kernel", launcher, temporaryRoot).verify(initialDocuments());

        assertEquals("school", result.schoolId());
        assertEquals(List.of("kernel", "verify", "--definition"), command.get().subList(0, 3));
        assertTrue(command.get().contains("--correlation-id"));
        assertFalse(command.get().toString().contains("sh -c"));
        assertDirectoryEmpty();
        assertTrue(output.getAll().contains("kernelCommand=verify exitClass=VERIFIED"));
        assertFalse(output.getAll().contains("school-definition.json"));
    }

    private ImportDocuments initialDocuments() throws Exception {
        return new ImportDocuments(
                JsonMapper.builder().build().readTree("""
                        {"schemaVersion":1,"catalogVersion":1,"schoolId":"school","displayName":"School"}
                        """),
                null,
                null,
                ImportDocuments.ImportMode.INITIAL_DEFINITION);
    }

    private void assertDirectoryEmpty() throws Exception {
        try (var entries = Files.list(temporaryRoot)) {
            assertEquals(0, entries.count());
        }
    }

    private static void assertPrivate(Path path, boolean directory) throws java.io.IOException {
        FileStore store = Files.getFileStore(path);
        if (!store.supportsFileAttributeView("posix")) {
            return;
        }
        Set<PosixFilePermission> expected = directory
                ? EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE)
                : EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        assertEquals(expected, Files.getPosixFilePermissions(path));
    }

    private static class CompletedProcess extends Process {
        private final int exit;
        private final InputStream stdout;
        private final InputStream stderr;

        CompletedProcess(int exit, byte[] stdout, byte[] stderr) {
            this.exit = exit;
            this.stdout = new ByteArrayInputStream(stdout);
            this.stderr = new ByteArrayInputStream(stderr);
        }

        @Override public OutputStream getOutputStream() { return new ByteArrayOutputStream(); }
        @Override public InputStream getInputStream() { return stdout; }
        @Override public InputStream getErrorStream() { return stderr; }
        @Override public int waitFor() { return exit; }
        @Override public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException { return true; }
        @Override public int exitValue() { return exit; }
        @Override public void destroy() {}
    }

    private static final class BlockingProcess extends CompletedProcess {
        private final CountDownLatch release = new CountDownLatch(1);

        BlockingProcess() {
            super(0, new byte[0], new byte[0]);
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            return release.await(timeout, unit);
        }

        @Override public void destroy() { release.countDown(); }
        @Override public Process destroyForcibly() { release.countDown(); return this; }
    }
}
