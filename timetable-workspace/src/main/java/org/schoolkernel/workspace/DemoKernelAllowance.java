package org.schoolkernel.workspace;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** In-memory process elapsed-time reservations. Waiting has no process cost; UTC rollover carries outstanding work. */
@Component
@Profile("demo")
class DemoKernelAllowance {
    private final Clock clock;
    private final long dailyNanos;
    private LocalDate day;
    private long consumed, resetCount, settledCount, releasedNanos;
    private final Set<Reservation> reservations = new HashSet<>();

    @Autowired
    DemoKernelAllowance(@Value("${demo.kernel.daily-allowance:4h}") Duration allowance) {
        this(allowance, Clock.systemUTC());
    }
    DemoKernelAllowance(Duration allowance, Clock clock) {
        if (allowance.isNegative() || allowance.isZero()) throw new IllegalArgumentException("Positive daily allowance required");
        this.dailyNanos = allowance.toNanos();
        this.clock = clock;
        this.day = date(clock.instant());
    }
    synchronized Reservation reserve(Duration maximum) {
        Snapshot now = snapshot();
        long amount = maximum.toNanos();
        if (amount <= 0 || amount > now.remainingNanos()) throw new WorkspaceProblem(HttpStatus.TOO_MANY_REQUESTS,
                "DAILY_KERNEL_LIMIT", "The daily kernel allowance cannot cover this kernel operation. No work was started. Try later.");
        Reservation reservation = new Reservation(amount);
        reservations.add(reservation);
        return reservation;
    }
    synchronized Snapshot snapshot() {
        Instant now = clock.instant();
        if (!date(now).equals(day)) { day = date(now); consumed = 0; resetCount++; }
        long reserved = 0;
        for (Reservation reservation : reservations) {
            if (reservation.accruedAt != null) {
                long elapsed = Math.max(0, Duration.between(reservation.accruedAt, now).toNanos());
                Instant today = day.atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant from = reservation.accruedAt.isBefore(today) ? today : reservation.accruedAt;
                consumed = Math.addExact(consumed, Math.max(0, Duration.between(from, now).toNanos()));
                reservation.remaining = Math.max(0, reservation.remaining - elapsed);
                reservation.accruedAt = now;
            }
            reserved = Math.addExact(reserved, reservation.remaining);
        }
        return new Snapshot(consumed, reserved, Math.max(0, dailyNanos - consumed - reserved));
    }
    private static LocalDate date(Instant instant) { return instant.atOffset(ZoneOffset.UTC).toLocalDate(); }
    record Snapshot(long consumedNanos, long reservedNanos, long remainingNanos) {}
    synchronized Telemetry telemetry() {
        var value = snapshot();
        return new Telemetry(value.consumedNanos(), value.reservedNanos(), value.remainingNanos(), day, resetCount, settledCount, releasedNanos);
    }
    record Telemetry(long consumedNanos, long reservedNanos, long remainingNanos, LocalDate day, long resets, long settlements, long releasedNanos) {}

    final class Reservation implements AutoCloseable {
        private long remaining;
        private Instant accruedAt;
        private Reservation(long maximum) { remaining = maximum; }
        void started(Instant beforeStart) {
            synchronized (DemoKernelAllowance.this) { accruedAt = beforeStart; }
        }
        void stopped() {
            synchronized (DemoKernelAllowance.this) { snapshot(); accruedAt = null; }
        }
        @Override public void close() {
            synchronized (DemoKernelAllowance.this) {
                snapshot();
                if (reservations.remove(this)) { settledCount++; releasedNanos = Math.addExact(releasedNanos, remaining); }
            }
        }
    }
}
