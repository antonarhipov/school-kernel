package org.schoolkernel.workspace;

import java.time.Duration;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** One resident-byte ceiling and fixed completion TTL for visitor results and verification evidence. */
@Component
@Profile("demo")
class DemoArtifactCache {
    private final boolean enabled;
    private final long maximum, ttl;
    private long bytes;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
    private final java.util.concurrent.ScheduledExecutorService reaper = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("demo-artifact-expiry").factory());

    DemoArtifactCache(@Value("${demo.reuse.enabled:true}") boolean enabled,
            @Value("${demo.reuse.max-bytes:67108864}") long maximum,
            @Value("${demo.reuse.ttl:1h}") Duration ttl) {
        if (maximum < 0 || ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofHours(1)) > 0)
            throw new IllegalArgumentException("Invalid visitor cache bounds");
        this.enabled = enabled; this.maximum = maximum; this.ttl = ttl.toNanos();
        reaper.scheduleWithFixedDelay(this::expire, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
    }
    boolean enabled() { return enabled; }
    synchronized Object get(String key) { expire(); var entry = enabled ? entries.get(key) : null; return entry == null ? null : entry.value; }
    synchronized void put(String key, Object value, long payloadBytes) {
        if (!enabled) return;
        expire();
        long size = 2L * key.length() + payloadBytes + 512;
        if (size > maximum) return;
        var old = entries.remove(key); if (old != null) bytes -= old.bytes;
        while (bytes + size > maximum && !entries.isEmpty()) {
            var iterator = entries.values().iterator(); bytes -= iterator.next().bytes; iterator.remove();
        }
        entries.put(key, new Entry(value, System.nanoTime(), size)); bytes += size;
    }
    record Occupancy(int entries, long bytes) {}
    synchronized Occupancy occupancy() { expire(); return new Occupancy(entries.size(), bytes); }
    synchronized long residentBytes() { expire(); return bytes; }
    private synchronized void expire() {
        long now = System.nanoTime();
        entries.values().removeIf(entry -> { if (now - entry.completed < ttl) return false; bytes -= entry.bytes; return true; });
    }
    @jakarta.annotation.PreDestroy synchronized void close() { reaper.shutdownNow(); entries.clear(); bytes = 0; }
    private record Entry(Object value, long completed, long bytes) {}
}
