package org.schoolkernel.workspace;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Only session objects carry workspace authority; it is never accepted from a URL or request document. */
@Component
@Profile("demo")
public class DemoWorkspaces {
    @org.springframework.beans.factory.annotation.Autowired(required = false) private DemoEvents events;
    private static final String ATTRIBUTE = DemoWorkspaces.class.getName();
    private final HashMap<UUID, Entry> active = new HashMap<>();
    private final ThreadLocal<Entry> context = new ThreadLocal<>();
    private final DemoSettings settings;
    private final ObjectMapper json;
    private final ObjectProvider<KernelPlanner> planner;
    private final Clock clock;
    private java.util.function.Consumer<UUID> runCancellation;
    void runCancellation(java.util.function.Consumer<UUID> cancellation) { this.runCancellation = cancellation; }
    private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("demo-expiry").factory());

    @org.springframework.beans.factory.annotation.Autowired
    public DemoWorkspaces(DemoSettings settings, ObjectMapper json, ObjectProvider<KernelPlanner> planner) {
        this(settings, json, planner, Clock.systemUTC());
    }

    DemoWorkspaces(DemoSettings settings, ObjectMapper json, ObjectProvider<KernelPlanner> planner, Clock clock) {
        this.settings = settings;
        this.json = json;
        this.planner = planner;
        this.runCancellation = id -> planner.getObject().cancel(id);
        this.clock = clock;
        reaper.scheduleWithFixedDelay(this::expire, 1, 1, TimeUnit.SECONDS);
    }

    synchronized Entry open(HttpSession session, boolean create) {
        expire();
        Entry entry = (Entry) session.getAttribute(ATTRIBUTE);
        if (entry != null && entry.ended) throw ended();
        if (entry == null) {
            if (!create) throw ended();
            if (active.size() >= settings.maxWorkspaces()) {
                if (events != null) events.refusal("DEMO_FULL");
                throw new WorkspaceProblem(HttpStatus.SERVICE_UNAVAILABLE, "DEMO_FULL",
                        "The demo is full. Please try again later; existing visitors keep their work.");
            }
            entry = new Entry(clock.instant(), new WorkspaceAggregate(WorkspaceState.EMPTY, 0, null, json.createObjectNode()));
            active.put(entry.key, entry);
            session.setAttribute(ATTRIBUTE, entry);
            if (events != null) events.session(true, "CREATED");
        }
        entry.lastActivity = clock.instant();
        return entry;
    }

    synchronized void restart(HttpSession session) {
        Entry old = (Entry) session.getAttribute(ATTRIBUTE);
        if (old != null && !old.ended) throw new WorkspaceProblem(HttpStatus.CONFLICT, "SESSION_ACTIVE",
                "This workspace is still active. Clear it explicitly instead.");
        session.removeAttribute(ATTRIBUTE);
        open(session, true);
    }

    Entry current() {
        Entry entry = context.get();
        if (entry == null || entry.ended || expired(entry, clock.instant())) throw ended();
        return entry;
    }

    void bound(Entry owner, Runnable action) {
        Entry previous = context.get();
        context.set(owner);
        try { action.run(); }
        finally { if (previous == null) context.remove(); else context.set(previous); }
    }

    void bind(Entry entry) { context.set(entry); }
    void unbind() { context.remove(); }

    synchronized int count() { expire(); return active.size(); }

    synchronized void expire() {
        Instant now = clock.instant();
        var iterator = active.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (!expired(entry, now)) continue;
            entry.lock.lock();
            try {
                entry.ended = true;
                if (events != null) events.session(false, !now.isBefore(entry.created.plus(settings.absoluteTimeout())) ? "ABSOLUTE" : "IDLE");
                UUID run = entry.aggregate.activeRunId();
                if (run != null) runCancellation.accept(run);
                entry.aggregate = new WorkspaceAggregate(WorkspaceState.EMPTY, 0, null, json.createObjectNode());
                iterator.remove();
            } finally { entry.lock.unlock(); }
        }
    }

    private boolean expired(Entry entry, Instant now) {
        return !now.isBefore(entry.created.plus(settings.absoluteTimeout()))
                || !now.isBefore(entry.lastActivity.plus(settings.idleTimeout()));
    }

    static WorkspaceProblem ended() {
        return new WorkspaceProblem(HttpStatus.GONE, "SESSION_ENDED",
                "This temporary workspace ended through expiry, restart or deployment. "
                + "Unfinished work is lost. Start again or import an accepted bundle; browser versions remain on this device.");
    }

    @PreDestroy void close() {
        reaper.shutdownNow();
        synchronized (this) {
            active.values().forEach(entry -> {
                entry.ended = true;
                if (events != null) events.session(false, "SHUTDOWN");
                if (entry.aggregate.activeRunId() != null) runCancellation.accept(entry.aggregate.activeRunId());
                entry.aggregate = new WorkspaceAggregate(WorkspaceState.EMPTY, 0, null, json.createObjectNode());
            });
            active.clear();
        }
    }

    static final class Entry {
        final UUID key = UUID.randomUUID();
        final java.util.Set<DemoEvents.Product> milestones = java.util.concurrent.ConcurrentHashMap.newKeySet();
        final Instant created;
        final ReentrantLock lock = new ReentrantLock();
        volatile Instant lastActivity;
        volatile boolean ended;
        long failedRepairSaveVersion = -1;
        WorkspaceAggregate aggregate;
        Entry(Instant now, WorkspaceAggregate aggregate) {
            created = now;
            lastActivity = now;
            this.aggregate = aggregate;
        }
    }
}
