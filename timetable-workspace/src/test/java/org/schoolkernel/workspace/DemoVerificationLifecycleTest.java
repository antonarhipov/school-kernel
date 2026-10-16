package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoVerificationLifecycleTest {
    @TempDir Path temporary;
    @Test void shutdownReapsTwoRealProcessesCancelsWaitingCallAndSettlesBeforeReturning() throws Exception {
        var processes = new ConcurrentLinkedQueue<Process>();
        var verifier = new KernelVerifier("ignored", new KernelProcessLauncher() {
            @Override public Process start(List<String> arguments) throws java.io.IOException {
                Process process = new ProcessBuilder("/bin/sleep", "60").start(); processes.add(process); return process;
            }
        }, temporary);
        var budget = new DemoKernelAllowance(Duration.ofHours(1));
        var service = new DemoVerificationService(verifier, budget, false, 67108864, Duration.ofHours(1),
                Duration.ofSeconds(10), Duration.ofSeconds(30), "../kernel-cli/target/school-kernel.jar");
        var documents = new ImportDocuments(new DemoCustomDefinitionTest().sample(), null, null, ImportDocuments.ImportMode.INITIAL_DEFINITION);
        try (var executor = Executors.newFixedThreadPool(3)) {
            var results = new ArrayList<Future<WorkspaceProblem>>();
            for (int i = 0; i < 3; i++) results.add(executor.submit(() -> assertThrows(WorkspaceProblem.class, () -> service.verify(documents))));
            DemoPlanningTest.await(() -> processes.size() == 2 && budget.snapshot().reservedNanos() > Duration.ofSeconds(95).toNanos());
            service.close(); service.close();
            for (var result : results) assertEquals("KERNEL_UNAVAILABLE", result.get(3, TimeUnit.SECONDS).code());
            assertEquals(2, processes.size()); assertTrue(processes.stream().noneMatch(Process::isAlive));
            assertEquals(0, budget.snapshot().reservedNanos()); assertTrue(budget.snapshot().consumedNanos() > 0);
            try (var paths = Files.list(temporary)) { assertEquals(0, paths.count(), "Private process documents are gone before shutdown returns"); }
            assertEquals("KERNEL_UNAVAILABLE", assertThrows(WorkspaceProblem.class, () -> service.verify(documents)).code());
            assertEquals(2, processes.size());
        } finally { service.close(); processes.forEach(Process::destroyForcibly); }
    }
}
