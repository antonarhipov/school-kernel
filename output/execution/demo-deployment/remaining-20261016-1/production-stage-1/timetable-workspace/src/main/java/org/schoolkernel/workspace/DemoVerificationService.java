package org.schoolkernel.workspace;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Exact successful verification evidence; standalone slots are separate from the solver's reserved verification. */
@Service
@Profile("demo")
class DemoVerificationService {
    @org.springframework.beans.factory.annotation.Autowired(required = false) private DemoEvents events;
    private final KernelVerifier verifier;
    private final DemoKernelAllowance allowance;
    private final Semaphore slots = new Semaphore(2, true);
    private final DemoArtifactCache cache;
    private final long waitMillis;
    private final Duration timeout;
    private final String build;
    private final boolean ownsCache;

    @org.springframework.beans.factory.annotation.Autowired
    DemoVerificationService(KernelVerifier verifier, DemoKernelAllowance allowance, DemoArtifactCache cache,
            @Value("${demo.kernel.verification-wait:10s}") Duration wait,
            @Value("${demo.kernel.verification-timeout:30s}") Duration timeout,
            @Value("${demo.kernel-artifact:kernel-cli/target/school-kernel.jar}") String artifact) throws Exception {
        this(verifier, allowance, cache, wait, timeout, artifact, false);
    }
    DemoVerificationService(KernelVerifier verifier, DemoKernelAllowance allowance, boolean reuse, long maxBytes,
            Duration ttl, Duration wait, Duration timeout, String artifact) throws Exception {
        this(verifier, allowance, new DemoArtifactCache(reuse, maxBytes, ttl), wait, timeout, artifact, true);
    }
    private DemoVerificationService(KernelVerifier verifier, DemoKernelAllowance allowance, DemoArtifactCache cache,
            Duration wait, Duration timeout, String artifact, boolean ownsCache) throws Exception {
        if (wait.isNegative() || wait.compareTo(Duration.ofSeconds(10)) > 0 || timeout.isNegative()
                || timeout.isZero() || timeout.compareTo(Duration.ofSeconds(30)) > 0)
            throw new IllegalArgumentException("Invalid demo verification bounds");
        this.verifier = verifier; this.allowance = allowance; this.cache = cache; this.ownsCache = ownsCache;
        this.waitMillis = wait.toMillis(); this.timeout = timeout;
        this.build = DemoSamples.kernelBuild(Path.of(KernelVerifier.resolveExecutable(artifact)));
    }
    String build() { return build; }
    Duration timeout() { return timeout; }
    KernelVerifier.Verification verify(ImportDocuments documents) {
        String key = key(documents);
        var hit = (KernelVerifier.Verification) cache.get(key);
        if (events != null) events.reuse(hit != null, false, null, false);
        if (hit != null) return hit;
        try (var reservation = allowance.reserve(timeout.plusSeconds(4))) {
            boolean acquired;
            try { acquired = slots.tryAcquire(waitMillis, TimeUnit.MILLISECONDS); }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new WorkspaceProblem(HttpStatus.SERVICE_UNAVAILABLE, "KERNEL_UNAVAILABLE", "Verification was interrupted. The definition was not used.");
            }
            if (!acquired) throw new WorkspaceProblem(HttpStatus.SERVICE_UNAVAILABLE, "VERIFICATION_BUSY",
                    "Verification is busy. The definition was not used. Try again shortly.");
            try {
                hit = (KernelVerifier.Verification) cache.get(key);
                if (hit != null) { if (events != null) events.reuse(true, true, null, false); return hit; }
                var verified = physicalVerify(documents, reservation);
                cache.put(key, verified, 1024);
                return verified;
            } finally { slots.release(); }
        }
    }
    // Required checking stays inside the single solver slot and uses its already-held allowance.
    KernelVerifier.Verification verifyReserved(ImportDocuments documents, DemoKernelAllowance.Reservation reservation) {
        var verified = physicalVerify(documents, reservation);
        cache.put(key(documents), verified, 1024);
        return verified;
    }
    private KernelVerifier.Verification physicalVerify(ImportDocuments documents, DemoKernelAllowance.Reservation reservation) {
        var physical = new java.util.concurrent.atomic.AtomicReference<DemoEvents.ProcessSpan>();
        String correlation = java.util.UUID.randomUUID().toString();
        String outcome = "FAILED";
        try {
            var result = verifier.verify(documents, timeout, before -> {
                reservation.started(before);
                if (events != null) physical.set(events.process(DemoEvents.ProcessKind.VERIFICATION, correlation, null));
            }, () -> {
                reservation.stopped();
                if (physical.get() != null) physical.get().stopped();
            });
            outcome = "VERIFIED";
            return result;
        } catch (WorkspaceProblem rejected) {
            outcome = rejected.status().is4xxClientError() ? "INVALID_INPUT" : "FAILED";
            throw rejected;
        } finally { if (physical.get() != null) physical.get().end(outcome); }
    }
    private String key(ImportDocuments documents) {
        return build + ":" + documents.mode() + ":" + new String(CanonicalJson.bytes(documents.definition()), java.nio.charset.StandardCharsets.UTF_8)
                + ":" + (documents.result() == null ? "absent" : new String(CanonicalJson.bytes(documents.result()), java.nio.charset.StandardCharsets.UTF_8));
    }
    @jakarta.annotation.PreDestroy void close() { if (ownsCache) cache.close(); }
}
