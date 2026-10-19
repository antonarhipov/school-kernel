package org.schoolkernel.workspace;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.*;
import tools.jackson.databind.node.ObjectNode;

/** One physical FIFO, private logical runs, and immutable exact-request artifacts. No owner IDs enter cache or logs. */
@Service
@Profile("demo")
class DemoPlanningService {
    @org.springframework.beans.factory.annotation.Autowired(required = false) private DemoEvents events;
    private final WorkspaceStore store;
    private final WorkspaceMutation mutation;
    private final DemoWorkspaces owners;
    private final RepairDraftService drafts;
    private final ProposalReviewService reviews;
    private final DemoDefinitionLimits limits;
    private final DemoSearchSettings settings;
    private final DemoVerificationService verification;
    private final DemoKernelAllowance allowance;
    private final DemoArtifactCache cache;
    private final DemoKernelRunner runner;
    private final ObjectMapper json;
    private final Map<UUID, Subscriber> active = new HashMap<>();
    private final ArrayDeque<Job> waiting = new ArrayDeque<>();
    private final Map<String, Job> matching = new HashMap<>();
    private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("demo-run-expiry").factory());
    private Job running;
    private boolean closed;
    private double observedOverheadMs = 10000;

    DemoPlanningService(WorkspaceStore store, WorkspaceMutation mutation, DemoWorkspaces owners, RepairDraftService drafts,
            ProposalReviewService reviews, DemoDefinitionLimits limits, DemoSearchSettings settings,
            DemoVerificationService verification, DemoKernelAllowance allowance, DemoArtifactCache cache,
            DemoKernelRunner runner, ObjectMapper json) {
        this.store = store; this.mutation = mutation; this.owners = owners; this.drafts = drafts; this.reviews = reviews;
        this.limits = limits; this.settings = settings; this.verification = verification; this.allowance = allowance;
        this.cache = cache; this.runner = runner; this.json = json;
        owners.runCancellation(ignored -> {}); // The reaper detaches logical subscribers; legacy run IDs cannot cancel shared work.
        reaper.scheduleWithFixedDelay(this::expireSafely, 1, 1, TimeUnit.SECONDS);
    }

    synchronized WorkspaceAggregate start(String ifMatch, JsonNode request) {
        if (closed) throw new WorkspaceProblem(HttpStatus.SERVICE_UNAVAILABLE, "DEMO_STOPPING", "The demo is stopping. No run started.");
        WorkspaceAggregate current = store.load();
        long sourceVersion = ImportService.requireMatchingVersion(ifMatch, current);
        boolean repair = current.state() == WorkspaceState.REPAIR_DRAFT;
        if (!repair && current.state() != WorkspaceState.INITIAL_DRAFT) throw transition("Start a run only from a ready initial or repair draft.");
        ObjectNode document = (ObjectNode) current.document().deepCopy();
        JsonNode definition, baselineDefinition = null, baselineResult = null;
        if (repair) {
            if (RepairDraftService.seeded(document) || drafts.hasUnsavedBrowserState(sourceVersion)) throw transition("Resolve the ordinary repair draft before requesting a proposal.");
            drafts.refreshSavedDraft(document);
            JsonNode draft = document.path("repairDraft");
            if (!draft.path("readyToSolve").asBoolean() || !draft.path("conflicts").isEmpty()) throw transition("Resolve every repair conflict before requesting a proposal.");
            definition = drafts.compiledDefinition(document);
            baselineDefinition = document.path("acceptedBaseline").path("definition").deepCopy();
            baselineResult = document.path("acceptedBaseline").path("result").deepCopy();
            if (!baselineResult.path("inputRevision").equals(definition.path("basedOnRevision"))) throw transition("The compiled repair no longer matches Current.");
        } else definition = document.path("initialDefinition").deepCopy();
        limits.check(definition, !repair);
        if (baselineResult != null && (CanonicalJson.bytes(baselineResult).length > DemoDefinitionLimits.DOCUMENT_BYTES
                || CanonicalJson.bytes(baselineDefinition).length > DemoDefinitionLimits.DOCUMENT_BYTES)) throw transition("Current exceeds the demo document limit.");
        String preset = preset(request, DemoDefinitionLimits.sessionCount(definition));
        var input = new DemoKernelRunner.Input(repair ? "REPAIR" : "INITIAL", definition, baselineDefinition, baselineResult, settings.effort(preset));
        String key = identity(input);
        var artifact = (DemoKernelRunner.Artifact) cache.get(key);
        Job job = artifact == null && cache.enabled() ? matching.get(key) : null;
        boolean fresh = artifact == null && job == null;
        DemoKernelAllowance.Reservation reservation = null;
        if (fresh) {
            if (running != null && waiting.size() >= settings.maxWaiting() || estimatedStartMs() > settings.estimatedStartLimit().toMillis()) {
                throw new WorkspaceProblem(HttpStatus.TOO_MANY_REQUESTS, "DEMO_BUSY", "The demo is busy. Try again shortly; no run was created.");
            }
            reservation = allowance.reserve(input.effort().plus(settings.processOverhead()).plusSeconds(4).plus(verification.timeout()).plusSeconds(4));
            job = new Job(key, input, reservation);
        }
        UUID id = UUID.randomUUID();
        Subscriber subscriber = new Subscriber(id, owners.current(), sourceVersion, document.deepCopy(), input, preset);
        document.remove("proposal");
        ObjectNode run = document.putObject("run");
        run.put("id", id.toString()); run.put("kind", input.operation()); run.put("status", artifact != null ? "FEASIBLE" : fresh ? running == null ? "RUNNING" : "QUEUED" : job.phase);
        run.put("queuePosition", fresh && running != null ? waiting.size() + 1 : 0);
        run.put("estimatedStartMs", estimatedStartMs()); run.put("approximate", true);
        run.put("sourceWorkspaceVersion", sourceVersion); run.put("limit", input.effort().toString()); run.put("effort", preset);
        run.put("acceptedTimetableRevision", repair ? baselineResult.path("timetableRevision").asString() : "NO_BASELINE");
        run.put("acceptedInputRevision", repair ? baselineResult.path("inputRevision").asString() : document.path("definitionRevision").asString());
        run.put("intentRevision", repair ? document.path("repairDraft").path("intentRevision").asString() : "NO_INTENT");
        WorkspaceAggregate started;
        try {
            started = repair ? mutation.startRepairRun(sourceVersion, id, document) : mutation.startInitialRun(sourceVersion, id, document);
        } catch (RuntimeException refusal) { if (fresh) reservation.close(); throw refusal; }
        subscriber.runningVersion = started.version();
        active.put(id, subscriber);
        if (events != null) {
            events.work("admitted", artifact != null ? artifact.computationId() : job.id, input.operation(), preset, "QUEUED");
            events.reuse(artifact != null, !fresh && artifact == null, artifact != null ? artifact.computationId() : job.id, true);
        }
        if (artifact != null) {
            subscriber.waitMs = 0;
            complete(subscriber, new DemoKernelRunner.Outcome(artifact, "FEASIBLE", null, null, null), true);
            active.remove(id);
            return store.load();
        }
        subscriber.job = job; job.subscribers.add(subscriber);
        if (!fresh && !"QUEUED".equals(job.phase)) subscriber.waitMs = 0;
        if (fresh) { if (cache.enabled()) matching.put(key, job); waiting.add(job); dispatch(); }
        return started;
    }

    synchronized JsonNode run(UUID id) {
        WorkspaceAggregate current = store.load();
        Subscriber subscriber = active.get(id);
        if (subscriber != null && subscriber.owner == owners.current() && id.equals(current.activeRunId())) {
            subscriber.lastPoll = System.nanoTime(); return status(subscriber);
        }
        JsonNode last = current.document().path("lastRun");
        if (id.toString().equals(last.path("id").asString(""))) return last.deepCopy();
        throw new WorkspaceProblem(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "The planning run does not exist.");
    }

    synchronized WorkspaceAggregate cancel(String ifMatch, UUID id) {
        WorkspaceAggregate current = store.load(); long version = ImportService.requireMatchingVersion(ifMatch, current);
        Subscriber subscriber = active.get(id);
        if (subscriber == null || subscriber.owner != owners.current() || !id.equals(current.activeRunId())) throw transition("Only this workspace's active run can be cancelled.");
        finishWithoutProposal(subscriber, current, version, "CANCELLED", "Planning was cancelled. The draft is retained.");
        detach(subscriber); return store.load();
    }

    private String preset(JsonNode request, long lessons) {
        if (request == null || request.isNull() || request.isObject() && request.isEmpty()) return lessons > settings.lessonThreshold() ? "THOROUGH" : "QUICK";
        if (!request.isObject() || request.size() != 1 || !Set.of("QUICK", "THOROUGH").contains(request.path("effort").asString("")))
            throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_SEARCH_EFFORT", "Choose Quick search or Thorough search. Arbitrary limits and retries are unavailable.");
        return request.path("effort").asString();
    }
    private String identity(DemoKernelRunner.Input input) {
        ObjectNode value = json.createObjectNode();
        value.put("operation", input.operation()); value.put("duration", input.effort().toString()); value.put("seed", 0);
        value.put("solverConfiguration", "packaged-timefold-default"); value.put("kernelBuild", verification.build());
        value.set("definition", input.definition());
        if (input.baselineDefinition() != null) { value.set("baselineDefinition", input.baselineDefinition()); value.set("baselineResult", input.baselineResult()); }
        return "solve:" + new String(CanonicalJson.bytes(value), StandardCharsets.UTF_8);
    }
    private long estimatedStartMs() {
        double overhead = observedOverheadMs;
        double estimate = running == null ? 0 : Math.max(0, running.input.effort().toMillis() + overhead - elapsed(running.started));
        for (Job job : waiting) estimate += job.input.effort().toMillis() + overhead;
        return (long)Math.ceil(estimate);
    }
    private ObjectNode status(Subscriber subscriber) {
        Job job = subscriber.job;
        ObjectNode response = json.createObjectNode();
        response.put("id", subscriber.id.toString()); response.put("kind", subscriber.input.operation()); response.put("status", job.phase);
        response.put("limit", subscriber.input.effort().toString()); response.put("effort", subscriber.preset);
        response.put("waitMs", subscriber.waitMs < 0 ? elapsed(subscriber.created) : subscriber.waitMs); response.put("reused", false);
        response.put("elapsedTimeMs", job.started == 0 ? 0 : elapsed(job.started));
        response.put("approximate", true);
        int position = 0; double estimate = 0;
        if (running != null && running != job) estimate = Math.max(0, running.input.effort().toMillis() + observedOverheadMs - elapsed(running.started));
        for (Job queued : waiting) { position++; if (queued == job) break; estimate += queued.input.effort().toMillis() + observedOverheadMs; }
        response.put("queuePosition", "QUEUED".equals(job.phase) ? position : 0);
        response.put("estimatedStartMs", "QUEUED".equals(job.phase) ? (long)Math.ceil(estimate) : 0);
        return response;
    }
    private void dispatch() {
        if (closed || running != null || waiting.isEmpty()) return;
        Job job = waiting.removeFirst(); running = job; job.phase = "RUNNING"; job.started = System.nanoTime();
        for (Subscriber subscriber : job.subscribers) subscriber.waitMs = elapsed(subscriber.created);
        event("started", job.id, job.input.operation(), null, "RUNNING");
        job.thread = Thread.ofVirtual().name("demo-computation-" + job.id).unstarted(() -> execute(job)); job.thread.start();
    }
    private void execute(Job job) {
        DemoKernelRunner.Outcome outcome;
        try { outcome = runner.run(job.id, job.input, job.reservation, () -> checking(job)); }
        catch (RuntimeException failure) { outcome = new DemoKernelRunner.Outcome(null, "FAILED", "KERNEL_FAILED", "The computation could not complete safely.", null); }
        finally { job.reservation.close(); }
        Thread.interrupted();
        synchronized (this) {
            if (outcome.artifact() != null && !job.subscribers.isEmpty() && !closed) {
                // Verification and artifact are retained together as a single eligible unit.
                cache.put(job.key, outcome.artifact(), 8L * CanonicalJson.bytes(outcome.artifact().result()).length + 2048);
                observedOverheadMs = .75 * observedOverheadMs + .25 * Math.max(0, elapsed(job.started) - job.input.effort().toMillis());
            }
            for (Subscriber subscriber : List.copyOf(job.subscribers)) {
                DemoKernelRunner.Outcome result = outcome;
                try { owners.bound(subscriber.owner, () -> complete(subscriber, result, false)); }
                catch (RuntimeException invalidProposal) {
                    owners.bound(subscriber.owner, () -> complete(subscriber, new DemoKernelRunner.Outcome(null,
                            "FAILED", "REJECTED_OUTPUT", "The result could not form a complete proposal. The draft is retained.", null), false));
                } finally { active.remove(subscriber.id); }
            }
            matching.remove(job.key, job); job.subscribers.clear(); running = null;
            event("finished", job.id, job.input.operation(), null, outcome.status()); dispatch();
        }
    }
    private synchronized void checking(Job job) { job.phase = "CHECKING_RESULT"; }

    private void complete(Subscriber subscriber, DemoKernelRunner.Outcome outcome, boolean reused) {
        try {
            if (closed || active.get(subscriber.id) != subscriber) { logical(subscriber, "EXPIRED", null, false); return; }
            WorkspaceAggregate current = store.load();
            if (current.version() != subscriber.runningVersion || !subscriber.id.equals(current.activeRunId())
                    || !eligible(subscriber, current.document())) { logical(subscriber, "EXPIRED", null, false); return; }
            ObjectNode document = (ObjectNode) current.document().deepCopy(); document.remove("run"); document.remove("proposal");
            ObjectNode last = document.putObject("lastRun");
            last.put("id", subscriber.id.toString()); last.put("kind", subscriber.input.operation()); last.put("status", outcome.status());
            last.put("limit", subscriber.input.effort().toString()); last.put("effort", subscriber.preset); last.put("reused", reused);
            last.put("waitMs", subscriber.waitMs < 0 ? elapsed(subscriber.created) : subscriber.waitMs);
            last.put("requestDurationMs", elapsed(subscriber.created));
            last.put("intentRevision", subscriber.source.path("repairDraft").path("intentRevision").asString("NO_INTENT"));
            if (outcome.artifact() == null) {
                last.put("feasible", false); last.put("code", outcome.code()); last.put("message", outcome.message());
                if (outcome.evidence() != null) last.setAll((ObjectNode)outcome.evidence().deepCopy());
                finish(subscriber, current.version(), false, document); return;
            }
            var artifact = outcome.artifact(); JsonNode result = artifact.result();
            last.put("feasible", true); last.put("terminationReason", result.path("terminationReason").asString());
            last.put("elapsedTimeMs", result.path("elapsedTimeMs").asLong());
            last.put("originalSolveProcessMs", artifact.solveProcessMs()); last.put("originalVerificationProcessMs", artifact.verificationProcessMs());
            ObjectNode proposal = document.putObject("proposal");
            proposal.setAll(last.deepCopy()); proposal.remove("id"); proposal.put("runId", subscriber.id.toString());
            proposal.put("id", UUID.randomUUID().toString()); proposal.put("sourceWorkspaceVersion", subscriber.sourceVersion);
            proposal.put("acceptedTimetableRevision", subscriber.input.baselineResult() == null ? "NO_BASELINE" : subscriber.input.baselineResult().path("timetableRevision").asString());
            proposal.put("successorDefinitionRevision", artifact.verification().definitionRevision());
            proposal.put("proposedTimetableRevision", artifact.verification().timetableRevision());
            proposal.set("definition", subscriber.input.definition().deepCopy()); proposal.set("result", result.deepCopy());
            ObjectNode provenance = proposal.putObject("solveProvenance"); provenance.put("computationId", artifact.computationId());
            provenance.put("kernelBuild", verification.build()); provenance.put("operation", subscriber.input.operation());
            provenance.put("duration", subscriber.input.effort().toString()); provenance.put("seed", 0); provenance.put("solverConfiguration", "packaged-timefold-default");
            ObjectNode counts = proposal.putObject("changeCounts");
            if ("REPAIR".equals(subscriber.input.operation())) {
                for (String field : ProposalReviewService.categories(result)) counts.put(field, result.path("changeReport").path(field).size());
                proposal.set("review", reviews.create(document.path("acceptedBaseline"), document.path("repairDraft"), subscriber.input.definition(), result));
            } else counts.put("lessonCount", result.path("timetable").path("assignments").size());
            finish(subscriber, current.version(), true, document);
        } catch (WorkspaceProblem endedOrStale) { logical(subscriber, "EXPIRED", null, false); /* No late write. */ }
    }
    private boolean eligible(Subscriber subscriber, JsonNode document) {
        if ("INITIAL".equals(subscriber.input.operation())) return subscriber.input.definition().equals(document.path("initialDefinition"));
        ObjectNode checked = (ObjectNode) document.deepCopy(); drafts.refreshSavedDraft(checked);
        return subscriber.source.path("acceptedBaseline").equals(document.path("acceptedBaseline"))
                && subscriber.source.path("repairDraft").path("intentRevision").equals(checked.path("repairDraft").path("intentRevision"))
                && subscriber.input.definition().equals(drafts.compiledDefinition(checked));
    }
    synchronized int waitingCount() { return waiting.size(); }
    private void finish(Subscriber subscriber, long version, boolean feasible, ObjectNode document) {
        if ("REPAIR".equals(subscriber.input.operation())) mutation.finishRepairRun(version, subscriber.id,
                feasible ? WorkspaceState.REPAIR_PROPOSAL : WorkspaceState.REPAIR_DRAFT, document);
        else mutation.finishInitialRun(version, subscriber.id, feasible ? WorkspaceState.INITIAL_PROPOSAL : WorkspaceState.INITIAL_DRAFT, document);
        var last = document.path("lastRun");
        var original = feasible ? new DemoKernelRunner.Artifact(null, null, last.path("originalSolveProcessMs").asLong(),
                last.path("originalVerificationProcessMs").asLong(), document.path("proposal").path("solveProvenance").path("computationId").asString()) : null;
        logical(subscriber, last.path("status").asString(), original, last.path("reused").asBoolean());
    }
    private void logical(Subscriber subscriber, String outcome, DemoKernelRunner.Artifact original, boolean reused) {
        if (events == null || subscriber.logged) return;
        subscriber.logged = true;
        events.logical(subscriber.correlation, original != null ? original.computationId() : subscriber.job == null ? null : subscriber.job.id,
                subscriber.input.operation(), subscriber.preset, outcome, subscriber.waitMs < 0 ? elapsed(subscriber.created) : subscriber.waitMs,
                elapsed(subscriber.created), original, reused);
    }

    private void finishWithoutProposal(Subscriber subscriber, WorkspaceAggregate current, long version, String status, String message) {
        ObjectNode document = (ObjectNode) current.document().deepCopy(); document.remove("run"); document.remove("proposal");
        ObjectNode last = document.putObject("lastRun"); last.put("id", subscriber.id.toString()); last.put("kind", subscriber.input.operation());
        last.put("status", status); last.put("limit", subscriber.input.effort().toString()); last.put("effort", subscriber.preset);
        last.put("message", message); last.put("feasible", false); finish(subscriber, version, false, document);
    }
    private void detach(Subscriber subscriber) {
        active.remove(subscriber.id); Job job = subscriber.job;
        logical(subscriber, "EXPIRED", null, false);
        job.subscribers.remove(subscriber);
        event("detached", job.id, subscriber.input.operation(), subscriber.preset, "CANCELLED_OR_EXPIRED");
        if (!job.subscribers.isEmpty()) return;
        matching.remove(job.key, job);
        if (waiting.remove(job)) job.reservation.close();
        else if (job.thread != null) job.thread.interrupt(); // Hold the slot/reservation until the physical boundary settles.
    }
    private synchronized void expireSafely() {
        try {
            for (Subscriber subscriber : List.copyOf(active.values())) {
                boolean expired = subscriber.owner.ended || "QUEUED".equals(subscriber.job.phase)
                        && System.nanoTime() - subscriber.lastPoll >= settings.abandonment().toNanos();
                if (!expired) continue;
                owners.bound(subscriber.owner, () -> {
                    try {
                        var current = store.load();
                        if (subscriber.id.equals(current.activeRunId())) finishWithoutProposal(subscriber, current, current.version(), "EXPIRED", "The queued run expired without polling. The draft is retained.");
                    } catch (WorkspaceProblem ended) {}
                });
                detach(subscriber);
            }
        } catch (RuntimeException failure) { event("expiry-check", null, null, null, "FAILED"); }
    }
    private static long elapsed(long start) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start); }
    private void event(String event, String computation, String operation, String preset, String outcome) {
        if (events != null) events.work(event, computation, operation, preset, outcome);
    }
    private static WorkspaceProblem transition(String message) { return new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION", message); }
    @jakarta.annotation.PreDestroy void close() {
        List<Thread> threads;
        synchronized (this) {
            closed = true; reaper.shutdownNow();
            for (Subscriber subscriber : active.values()) logical(subscriber, "CANCELLED", null, false);
            for (Job job : waiting) job.reservation.close(); waiting.clear(); matching.clear(); active.clear();
            threads = running == null || running.thread == null ? List.of() : List.of(running.thread);
            threads.forEach(Thread::interrupt);
        }
        for (Thread thread : threads) { try { thread.join(10000); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); } }
    }
    private static final class Job {
        final String id = UUID.randomUUID().toString(), key;
        final DemoKernelRunner.Input input; final DemoKernelAllowance.Reservation reservation;
        final Set<Subscriber> subscribers = new LinkedHashSet<>();
        String phase = "QUEUED"; long started; Thread thread;
        Job(String key, DemoKernelRunner.Input input, DemoKernelAllowance.Reservation reservation) { this.key = key; this.input = input; this.reservation = reservation; }
    }
    private static final class Subscriber {
        final UUID id; final DemoWorkspaces.Entry owner; final long sourceVersion, created = System.nanoTime();
        final ObjectNode source; final DemoKernelRunner.Input input; final String preset;
        final String correlation = UUID.randomUUID().toString();
        long runningVersion, lastPoll = System.nanoTime(), waitMs = -1; Job job; boolean logged;
        Subscriber(UUID id, DemoWorkspaces.Entry owner, long version, ObjectNode source, DemoKernelRunner.Input input, String preset) {
            this.id = id; this.owner = owner; this.sourceVersion = version; this.source = source; this.input = input; this.preset = preset;
        }
    }
}
