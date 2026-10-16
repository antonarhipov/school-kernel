package org.schoolkernel.workspace;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** A closed vocabulary: callers cannot supply documents, names, addresses, labels or workspace authority. */
@Component
@Profile("demo")
class DemoEvents {
    enum Product { SAMPLE_OPEN, DEFINITION_SAMPLE, DEFINITION_FILE, DEFINITION_CUSTOM,
        ACCEPT_INITIAL, ACCEPT_REPAIR, MANUAL_PUBLISH, EXPORT, IMPORT }
    enum ProcessKind { SOLVE, VERIFICATION }
    private final ObjectMapper json;
    private final Environment environment;
    private final Consumer<String> sink;
    private final String reset = UUID.randomUUID().toString();
    private final Map<String, Long> counts = new TreeMap<>();
    // Ten millisecond upper-bound bins, capped at ten minutes. Bounded independently of traffic.
    private final long[] waits = new long[60001];
    private final ScheduledExecutorService summaries = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("demo-summary").factory());
    private long intervalStart = System.nanoTime(), resets, settlements, released;
    private int solveRunning, verificationRunning;
    private String build = "NOT_READY", config = "NOT_READY";
    @Autowired private ObjectProvider<DemoWorkspaces> owners;
    @Autowired private ObjectProvider<DemoPlanningService> planning;
    @Autowired private ObjectProvider<DemoArtifactCache> artifacts;
    @Autowired private ObjectProvider<DemoKernelAllowance> allowance;
    @Autowired private ObjectProvider<DemoVerificationService> verifier;
    @Autowired private ObjectProvider<DemoSamples> samples;

    @Autowired DemoEvents(ObjectMapper json, Environment environment) {
        this(json, environment, line -> LoggerFactory.getLogger("DemoEvents").info("{}", line));
    }
    DemoEvents(ObjectMapper json, Environment environment, Consumer<String> sink) {
        this.json = json; this.environment = environment; this.sink = sink;
    }
    @EventListener(ApplicationReadyEvent.class) void ready() {
        build = verifier.getObject().build();
        ObjectNode effective = json.createObjectNode();
        // Only configuration keys from this fixed list may enter logs. Never enumerate the environment.
        for (String key : List.of("demo.idle-timeout", "demo.absolute-timeout", "demo.max-workspaces",
                "demo.search.lesson-threshold", "demo.search.quick-seconds", "demo.search.thorough-seconds",
                "demo.complexity.work-max", "demo.complexity.lesson-max", "demo.queue.max-waiting",
                "demo.queue.estimated-start-limit", "demo.queue.abandonment", "demo.kernel.solve-overhead",
                "demo.kernel.daily-allowance", "demo.kernel.verification-timeout", "demo.kernel.verification-wait",
                "demo.reuse.enabled", "demo.reuse.max-bytes", "demo.reuse.ttl", "demo.observability.summary-interval")) {
            effective.put(key, environment.getRequiredProperty(key));
        }
        config = digest(CanonicalJson.bytes(effective));
        ObjectNode line = line("startup", "ready"); line.put("kernelBuild", build); line.set("effective", effective);
        var source = new org.springframework.boot.system.ApplicationHome(WorkspaceApplication.class).getSource();
        if (source != null && source.isFile()) {
            try (var input = java.nio.file.Files.newInputStream(source.toPath())) {
                var hash = java.security.MessageDigest.getInstance("SHA-256");
                input.transferTo(new java.security.DigestOutputStream(java.io.OutputStream.nullOutputStream(), hash));
                line.put("serviceArtifactSha256", HexFormat.of().formatHex(hash.digest()));
            } catch (Exception failed) { throw new IllegalStateException("Cannot identify demo service artifact", failed); }
        }
        line.put("solveSlots", 1); line.put("standaloneVerificationSlots", 2);
        line.putObject("processBounds").put("solveMaximum", "preset + solve-overhead")
                .put("solveReservationExtraSeconds", 8).put("standaloneVerificationReservationExtraSeconds", 4)
                .put("terminationSoftWaitSeconds", 2).put("reservationRelease", "AFTER_REAP");
        line.put("counterReset", reset); line.put("utcAllowanceDay", LocalDate.now(ZoneOffset.UTC).toString());
        ObjectNode verified = line.putObject("sampleVerification");
        samples.getObject().catalog().forEach(sample -> verified.put(sample.path("identifier").asString(), "VERIFIED"));
        emit(line);
        Duration interval = org.springframework.boot.convert.DurationStyle.detectAndParse(
                environment.getProperty("demo.observability.summary-interval", "1m"));
        if (interval.compareTo(Duration.ofSeconds(1)) < 0) throw new IllegalArgumentException("Summary interval must be at least one second");
        intervalStart = System.nanoTime();
        summaries.scheduleWithFixedDelay(this::summary, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }
    synchronized void session(boolean created, String reason) {
        String safe = created ? "CREATED" : Set.of("IDLE", "ABSOLUTE", "SHUTDOWN").contains(reason) ? reason : "ENDED";
        count(created ? "workspace.created" : "workspace.expired." + safe);
        ObjectNode line = line("session", created ? "created" : "ended"); line.put("count", 1); line.put("reason", safe); emit(line);
    }
    void product(Product action) { product(action, null, null); }
    void product(Product action, String sample, tools.jackson.databind.JsonNode definition) {
        boolean converted = false;
        if (owners != null) {
            try {
                var entry = owners.getObject().current();
                boolean used = entry.milestones.contains(Product.SAMPLE_OPEN) || entry.milestones.contains(Product.DEFINITION_SAMPLE)
                        || entry.milestones.contains(Product.DEFINITION_FILE) || entry.milestones.contains(Product.DEFINITION_CUSTOM);
                boolean first = entry.milestones.add(action);
                converted = first && used && Set.of(Product.ACCEPT_INITIAL, Product.ACCEPT_REPAIR, Product.MANUAL_PUBLISH, Product.EXPORT).contains(action);
            } catch (WorkspaceProblem ended) { /* Successful operation already happened; telemetry adds no authority. */ }
        }
        synchronized (this) {
            count("product." + action); if (converted) count("product.conversion." + action);
            boolean use = Set.of(Product.SAMPLE_OPEN, Product.DEFINITION_SAMPLE, Product.DEFINITION_FILE, Product.DEFINITION_CUSTOM).contains(action);
            ObjectNode line = line(use ? "definition" : "decision", action.name());
            if (sample != null && Set.of("small", "two-class", "full").contains(sample)) line.put("sample", sample);
            if (definition != null) {
                long lessons = definition.path("lessons").size(), work = lessons * definition.path("rooms").size() * definition.path("periods").size();
                line.put("lessons", lessons); line.put("placementWork", work);
                line.put("complexityBucket", work <= 100000 ? "SMALL" : work <= 1000000 ? "MEDIUM" : "LARGE");
                line.put("verification", "VERIFIED");
            }
            emit(line);
        }
    }
    synchronized void reuse(boolean hit, boolean coalesced, String computation, boolean solve) {
        String result = coalesced ? "COALESCED" : hit ? "HIT" : "MISS";
        count("cache." + result); ObjectNode line = line("reuse", result);
        line.put("operation", solve ? "SOLVE" : "VERIFICATION"); id(line, "computationId", computation);
        line.put("physicalProcessStarted", false); emit(line);
    }
    synchronized void work(String event, String computation, String operation, String preset, String outcome) {
        if (!Set.of("admitted", "started", "finished", "detached", "expiry-check").contains(event)) return;
        if (event.equals("admitted")) count("queue.admissions");
        ObjectNode line = line("work", event); id(line, "computationId", computation);
        if (Set.of("INITIAL", "REPAIR").contains(operation == null ? "" : operation)) line.put("operation", operation);
        if (Set.of("QUICK", "THOROUGH").contains(preset == null ? "" : preset)) { line.put("preset", preset); if (event.equals("admitted")) count("queue.preset." + preset); }
        line.put("outcome", safeOutcome(outcome)); emit(line);
    }
    synchronized void logical(String correlation, String computation, String operation, String preset, String outcome,
            long waitMs, long durationMs, DemoKernelRunner.Artifact original, boolean reused) {
        count("queue.logical." + safeOutcome(outcome)); waits[(int)Math.min(60000, (Math.max(0, waitMs) + 9) / 10)]++;
        ObjectNode line = line("work", "logical_finished"); id(line, "correlationId", correlation); id(line, "computationId", computation);
        line.put("operation", "REPAIR".equals(operation) ? "REPAIR" : "INITIAL");
        line.put("preset", "THOROUGH".equals(preset) ? "THOROUGH" : "QUICK"); line.put("outcome", safeOutcome(outcome));
        line.put("waitMs", Math.max(0, waitMs)); line.put("requestDurationMs", durationMs); line.put("reused", reused);
        if (original != null) { line.put("originalSolveProcessMs", original.solveProcessMs()); line.put("originalVerificationProcessMs", original.verificationProcessMs()); }
        line.put("accounting", "PHYSICAL_PROCESS_EVENTS_ONLY"); emit(line);
    }
    synchronized void refusal(String code) {
        // Callers pass error codes only; arbitrary messages and request values are never retained.
        String safe = Set.of("ACCEPTANCE_CONFIRMATION_REQUIRED", "ACCEPTED_BASELINE_REQUIRED", "ACCEPTED_BUNDLE_REQUIRED", "CATALOG_UPGRADE_CONFIRMATION_REQUIRED",
                "CONFIRMATION_REQUIRED", "DAILY_KERNEL_LIMIT", "DEFINITION_COMPLEXITY_LIMIT", "DEFINITION_DRAFT_ACTIVE",
                "DEFINITION_TOO_LARGE", "DEMO_BUSY", "DEMO_FULL", "DEMO_STOPPING",
                "DISCARD_CONFIRMATION_REQUIRED", "DRAFT_CONFLICT", "DRAFT_NOT_DURABLE", "EXPORT_FAILED",
                "EXPORT_VERIFICATION_FAILED", "IMPORT_TOO_LARGE", "INVALID_ACTION", "INVALID_AUTHORING_SOURCE",
                "INVALID_IMPORT", "INVALID_INITIAL_DEFINITION", "INVALID_INPUT", "INVALID_LESSON",
                "INVALID_MANUAL_DRAFT", "INVALID_PERIOD", "INVALID_REPAIR_DRAFT", "INVALID_ROOM",
                "INVALID_RUN_LIMIT", "INVALID_SCHOOL_FIELDS", "INVALID_SCHOOL_ID", "INVALID_SEARCH_EFFORT",
                "INVALID_WORKSPACE_MANIFEST", "INVALID_WORKSPACE_STATE", "INVALID_WORKSPACE_TRANSITION", "KERNEL_UNAVAILABLE",
                "KERNEL_VERIFICATION_FAILED", "MALFORMED_REQUEST", "PRECONDITION_REQUIRED", "REPLACEMENT_CONFIRMATION_REQUIRED",
                "RETRY_NOT_AVAILABLE", "RUN_NOT_FOUND", "SAMPLE_NOT_FOUND", "SAVED_DEFINITION_REQUIRED",
                "SCHOOL_ID_IMMUTABLE", "SCHOOL_ID_RESERVED", "SEEDED_REPAIR_ACTIVE", "SESSION_ACTIVE",
                "SESSION_ENDED", "STALE_PROPOSAL", "STALE_RUN", "STALE_WORKSPACE_STATE",
                "STALE_WORKSPACE_VERSION", "TEACHER_CHANGE_REQUIRES_AUTHORING", "UNRESOLVED_CONFLICTS", "UNSUPPORTED_DEFINITION_SOURCE",
                "VERIFICATION_BUSY", "VERIFICATION_FAILED").contains(code) ? code : "OTHER_REFUSAL";
        count("queue.refusal." + safe); ObjectNode line = line("work", "refused"); line.put("reason", safe); emit(line);
    }
    ProcessSpan process(ProcessKind kind, String correlation, String computation) {
        synchronized (this) {
            if (kind == ProcessKind.SOLVE) solveRunning++; else verificationRunning++;
            ObjectNode line = line("work", "process_started"); line.put("kind", kind.name());
            id(line, "correlationId", correlation); id(line, "computationId", computation); emit(line);
        }
        return new ProcessSpan(kind, correlation, computation);
    }
    final class ProcessSpan {
        final ProcessKind kind; final String correlation, computation; final long start = System.nanoTime(); final AtomicBoolean ended = new AtomicBoolean(); final AtomicBoolean stopped = new AtomicBoolean(); long duration;
        ProcessSpan(ProcessKind kind, String correlation, String computation) { this.kind = kind; this.correlation = correlation; this.computation = computation; }
        void stopped() {
            synchronized (DemoEvents.this) {
                if (!stopped.compareAndSet(false, true)) return;
                if (kind == ProcessKind.SOLVE) solveRunning--; else verificationRunning--;
                duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            }
        }
        void end(String outcome) {
            if (!ended.compareAndSet(false, true)) return;
            stopped();
            synchronized (DemoEvents.this) {
                String safe = safeOutcome(outcome);
                count("process.completed." + kind + "." + safe); counts.merge("process.durationMs." + kind, duration, Long::sum);
                ObjectNode line = line("work", "process_finished"); line.put("kind", kind.name()); line.put("outcome", safe); line.put("processMs", duration);
                id(line, "correlationId", correlation); id(line, "computationId", computation); emit(line);
            }
        }
    }
    void summary() {
        // Acquire service snapshots before the telemetry monitor: no event->service lock inversion.
        int active = owners.getObject().count(), queued = planning.getObject().waitingCount();
        var occupancy = artifacts.getObject().occupancy(); var budget = allowance.getObject().telemetry();
        synchronized (this) {
            long now = System.nanoTime(); ObjectNode line = line("aggregate", "interval");
            ObjectNode context = line.putObject("context"); context.put("timestamp", Instant.now().toString());
            context.put("build", build); context.put("config", config); context.put("counterReset", reset);
            context.put("intervalMs", TimeUnit.NANOSECONDS.toMillis(now - intervalStart)); context.put("utcAllowanceDay", budget.day().toString());
            ObjectNode workspaces = group(line, "workspaces", "workspace."); workspaces.put("active", active);
            ObjectNode queue = group(line, "queue", "queue."); queue.put("distinctWaiting", queued);
            queue.put("waitP50UpperMs", percentile(.5)); queue.put("waitP90UpperMs", percentile(.9)); queue.put("waitBinMs", 10);
            ObjectNode processes = group(line, "processes", "process."); processes.put("runningSolve", solveRunning); processes.put("runningVerification", verificationRunning);
            ObjectNode cache = group(line, "cache", "cache."); cache.put("entries", occupancy.entries()); cache.put("residentBytes", occupancy.bytes());
            ObjectNode limits = line.putObject("allowance"); limits.put("consumedMs", budget.consumedNanos() / 1000000);
            limits.put("reservedMs", budget.reservedNanos() / 1000000); limits.put("remainingMs", budget.remainingNanos() / 1000000);
            limits.put("utcResets", budget.resets() - resets); limits.put("settlements", budget.settlements() - settlements);
            limits.put("releasedReservationMs", (budget.releasedNanos() - released) / 1000000);
            resets = budget.resets(); settlements = budget.settlements(); released = budget.releasedNanos();
            group(line, "productUse", "product."); emit(line); counts.clear(); Arrays.fill(waits, 0); intervalStart = now;
        }
    }
    private ObjectNode group(ObjectNode line, String group, String prefix) {
        ObjectNode values = line.putObject(group);
        counts.forEach((key, value) -> { if (key.startsWith(prefix)) values.put(key.substring(prefix.length()), value); }); return values;
    }
    private long percentile(double quantile) {
        long total = Arrays.stream(waits).sum(); if (total == 0) return 0;
        long target = (long)Math.ceil(total * quantile), sum = 0;
        for (int i = 0; i < waits.length; i++) { sum += waits[i]; if (sum >= target) return i * 10L; } return 600000;
    }
    private void count(String key) { counts.merge(key, 1L, Long::sum); }
    private ObjectNode line(String family, String event) {
        ObjectNode line = json.createObjectNode(); line.put("family", family); line.put("event", event); line.put("timestamp", Instant.now().toString()); return line;
    }
    private static String digest(byte[] value) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private void emit(ObjectNode line) { sink.accept(json.writeValueAsString(line)); }
    private static void id(ObjectNode line, String field, String id) {
        if (id != null) { try { line.put(field, UUID.fromString(id).toString()); } catch (IllegalArgumentException ignored) {} }
    }
    private static String safeOutcome(String value) {
        return value != null && Set.of("QUEUED", "RUNNING", "CHECKING_RESULT", "FEASIBLE", "VERIFIED", "NOT_FOUND", "INVALID_INPUT", "FAILED", "CANCELLED",
                "EXPIRED", "CANCELLED_OR_EXPIRED", "EXIT_0", "EXIT_2", "EXIT_3", "EXIT_OTHER", "TERMINATED", "STOPPED").contains(value) ? value : "FAILED";
    }
    @PreDestroy void close() { summaries.shutdownNow(); }
}
