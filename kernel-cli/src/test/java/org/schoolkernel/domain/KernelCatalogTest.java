package org.schoolkernel.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KernelCatalogTest {
    @Test
    @DisplayName("RULE-27: catalog version, IDs, order, categories, and defaults are exact")
    void catalogIsExact() {
        assertEquals(7, KernelCatalog.VERSION);
        assertEquals(6, KernelCatalog.DAY_EDGES_VERSION);
        assertEquals(5, KernelCatalog.NO_GAPS_VERSION);
        assertEquals(4, KernelCatalog.DAILY_BALANCE_VERSION);
        assertEquals(3, KernelCatalog.START_QUALITY_VERSION);
        assertEquals(2, KernelCatalog.CLASS_QUALITY_VERSION);
        assertEquals(1, KernelCatalog.LEGACY_VERSION);
        assertEquals(List.of(
                "soft.teacher-gap", "soft.series-same-day", "soft.undesirable-period", "soft.non-preferred-room"),
                KernelCatalog.softConstraintIds(KernelCatalog.LEGACY_VERSION));
        assertEquals(4, KernelCatalog.defaultSoftWeights(KernelCatalog.LEGACY_VERSION).size());
        assertEquals(List.of(
                "soft.teacher-gap", "soft.cohort-gap", "soft.cohort-week-balance",
                "soft.series-same-day", "soft.undesirable-period", "soft.non-preferred-room"),
                KernelCatalog.softConstraintIds(KernelCatalog.CLASS_QUALITY_VERSION));
        assertEquals(7, KernelCatalog.defaultSoftWeights(KernelCatalog.VERSION).size());
        assertEquals(KernelCatalog.softConstraintIds(KernelCatalog.START_QUALITY_VERSION),
                KernelCatalog.softConstraintIds(KernelCatalog.VERSION));
        assertEquals(List.of(
                row("hard.teacher-period", "HARD", null),
                row("hard.cohort-period", "HARD", null),
                row("hard.room-period", "HARD", null),
                row("hard.teacher-availability", "HARD", null),
                row("hard.cohort-availability", "HARD", null),
                row("hard.room-availability", "HARD", null),
                row("hard.room-capacity", "HARD", null),
                row("hard.room-capability", "HARD", null),
                row("hard.period-lock", "HARD", null),
                row("hard.room-lock", "HARD", null),
                row("hard.cohort-gap", "HARD", null),
                row("hard.reserved-period", "HARD", null),
                row("hard.cohort-late-start", "HARD", null),
                row("hard.subject-day-edge", "HARD", null),
                row("hard.subject-daily-limit", "HARD", null),
                row("hard.subject-reserved-limit", "HARD", null),
                row("hard.cohort-daily-spread", "HARD", null),
                row("stability.period-move", "PERIOD_STABILITY", null),
                row("stability.room-only-move", "ROOM_STABILITY", null),
                row("soft.teacher-gap", "ORDINARY_PREFERENCE", 1L),
                row("soft.cohort-gap", "ORDINARY_PREFERENCE", 1L),
                row("soft.cohort-late-start", "ORDINARY_PREFERENCE", 1L),
                row("soft.cohort-week-balance", "ORDINARY_PREFERENCE", 1L),
                row("soft.series-same-day", "ORDINARY_PREFERENCE", 1L),
                row("soft.undesirable-period", "ORDINARY_PREFERENCE", 1L),
                row("soft.non-preferred-room", "ORDINARY_PREFERENCE", 1L)),
                KernelCatalog.CONSTRAINTS.stream()
                        .map(value -> row(value.id(), value.category().name(), value.defaultWeight()))
                        .toList());
    }

    private static List<Object> row(String id, String category, Long defaultWeight) {
        return List.of(id, category, defaultWeight == null ? "-" : defaultWeight);
    }
}
