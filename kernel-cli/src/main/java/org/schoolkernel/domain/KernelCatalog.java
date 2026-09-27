package org.schoolkernel.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Versioned constraint catalog, retaining exact prior preference subsets for legacy results. */
public final class KernelCatalog {
    public static final int LEGACY_VERSION = 1;
    public static final int CLASS_QUALITY_VERSION = 2;
    public static final int START_QUALITY_VERSION = 3;
    public static final int DAILY_BALANCE_VERSION = 4;
    public static final int NO_GAPS_VERSION = 5;
    public static final int DAY_EDGES_VERSION = 6;
    public static final int VERSION = 7;

    public enum Category {
        HARD,
        PERIOD_STABILITY,
        ROOM_STABILITY,
        ORDINARY_PREFERENCE
    }

    public record ConstraintDescriptor(String id, Category category, Long defaultWeight) {
        public ConstraintDescriptor {
            if (id == null || id.isBlank() || category == null) {
                throw new IllegalArgumentException("Catalog descriptors require an ID and category");
            }
            if (category == Category.ORDINARY_PREFERENCE && defaultWeight == null) {
                throw new IllegalArgumentException("Ordinary preferences require a default weight");
            }
            if (category != Category.ORDINARY_PREFERENCE && defaultWeight != null) {
                throw new IllegalArgumentException("Only ordinary preferences have configurable weights");
            }
        }
    }

    public static final ConstraintDescriptor TEACHER_PERIOD = hard("hard.teacher-period");
    public static final ConstraintDescriptor COHORT_PERIOD = hard("hard.cohort-period");
    public static final ConstraintDescriptor ROOM_PERIOD = hard("hard.room-period");
    public static final ConstraintDescriptor TEACHER_AVAILABILITY = hard("hard.teacher-availability");
    public static final ConstraintDescriptor COHORT_AVAILABILITY = hard("hard.cohort-availability");
    public static final ConstraintDescriptor ROOM_AVAILABILITY = hard("hard.room-availability");
    public static final ConstraintDescriptor ROOM_CAPACITY = hard("hard.room-capacity");
    public static final ConstraintDescriptor ROOM_CAPABILITY = hard("hard.room-capability");
    public static final ConstraintDescriptor PERIOD_LOCK = hard("hard.period-lock");
    public static final ConstraintDescriptor ROOM_LOCK = hard("hard.room-lock");
    public static final ConstraintDescriptor COHORT_DAILY_GAPS = hard("hard.cohort-gap");
    public static final ConstraintDescriptor RESERVED_PERIOD = hard("hard.reserved-period");
    public static final ConstraintDescriptor COHORT_LATEST_START = hard("hard.cohort-late-start");
    public static final ConstraintDescriptor SUBJECT_DAY_EDGE = hard("hard.subject-day-edge");
    public static final ConstraintDescriptor SUBJECT_DAILY_LIMIT = hard("hard.subject-daily-limit");
    public static final ConstraintDescriptor SUBJECT_RESERVED_LIMIT = hard("hard.subject-reserved-limit");
    public static final ConstraintDescriptor COHORT_DAILY_SPREAD = hard("hard.cohort-daily-spread");
    public static final ConstraintDescriptor PERIOD_MOVE =
            new ConstraintDescriptor("stability.period-move", Category.PERIOD_STABILITY, null);
    public static final ConstraintDescriptor ROOM_ONLY_MOVE =
            new ConstraintDescriptor("stability.room-only-move", Category.ROOM_STABILITY, null);
    public static final ConstraintDescriptor TEACHER_GAP = soft("soft.teacher-gap");
    public static final ConstraintDescriptor COHORT_GAP = soft("soft.cohort-gap");
    public static final ConstraintDescriptor COHORT_LATE_START = soft("soft.cohort-late-start");
    public static final ConstraintDescriptor COHORT_WEEK_BALANCE = soft("soft.cohort-week-balance");
    public static final ConstraintDescriptor SERIES_SAME_DAY = soft("soft.series-same-day");
    public static final ConstraintDescriptor UNDESIRABLE_PERIOD = soft("soft.undesirable-period");
    public static final ConstraintDescriptor NON_PREFERRED_ROOM = soft("soft.non-preferred-room");

    public static final List<ConstraintDescriptor> CONSTRAINTS = List.of(
            TEACHER_PERIOD,
            COHORT_PERIOD,
            ROOM_PERIOD,
            TEACHER_AVAILABILITY,
            COHORT_AVAILABILITY,
            ROOM_AVAILABILITY,
            ROOM_CAPACITY,
            ROOM_CAPABILITY,
            PERIOD_LOCK,
            ROOM_LOCK,
            COHORT_DAILY_GAPS,
            RESERVED_PERIOD,
            COHORT_LATEST_START,
            SUBJECT_DAY_EDGE,
            SUBJECT_DAILY_LIMIT,
            SUBJECT_RESERVED_LIMIT,
            COHORT_DAILY_SPREAD,
            PERIOD_MOVE,
            ROOM_ONLY_MOVE,
            TEACHER_GAP,
            COHORT_GAP,
            COHORT_LATE_START,
            COHORT_WEEK_BALANCE,
            SERIES_SAME_DAY,
            UNDESIRABLE_PERIOD,
            NON_PREFERRED_ROOM);

    private static final Map<String, ConstraintDescriptor> BY_ID = CONSTRAINTS.stream()
            .collect(Collectors.toUnmodifiableMap(ConstraintDescriptor::id, Function.identity()));

    private KernelCatalog() {}

    public static List<String> hardConstraintIds() {
        return ids(Category.HARD);
    }

    public static List<String> softConstraintIds() {
        return ids(Category.ORDINARY_PREFERENCE);
    }

    public static List<String> softConstraintIds(int catalogVersion) {
        requireVersion(catalogVersion);
        return softConstraintIds().stream()
                .filter(id -> catalogVersion >= START_QUALITY_VERSION
                        || (catalogVersion == CLASS_QUALITY_VERSION && !id.equals(COHORT_LATE_START.id()))
                        || (catalogVersion == LEGACY_VERSION
                                && !id.equals(COHORT_GAP.id())
                                && !id.equals(COHORT_LATE_START.id())
                                && !id.equals(COHORT_WEEK_BALANCE.id())))
                .toList();
    }

    public static Map<String, Long> defaultSoftWeights() {
        return defaultSoftWeights(VERSION);
    }

    public static Map<String, Long> defaultSoftWeights(int catalogVersion) {
        List<String> ids = softConstraintIds(catalogVersion);
        return CONSTRAINTS.stream()
                .filter(descriptor -> ids.contains(descriptor.id()))
                .collect(Collectors.toUnmodifiableMap(
                        ConstraintDescriptor::id,
                        ConstraintDescriptor::defaultWeight));
    }

    public static boolean supportsVersion(int catalogVersion) {
        return catalogVersion == LEGACY_VERSION
                || catalogVersion == CLASS_QUALITY_VERSION
                || catalogVersion == START_QUALITY_VERSION
                || catalogVersion == DAILY_BALANCE_VERSION
                || catalogVersion == NO_GAPS_VERSION
                || catalogVersion == DAY_EDGES_VERSION
                || catalogVersion == VERSION;
    }

    private static void requireVersion(int catalogVersion) {
        if (!supportsVersion(catalogVersion)) {
            throw new IllegalArgumentException("Unsupported catalog version: " + catalogVersion);
        }
    }

    public static ConstraintDescriptor require(String id) {
        ConstraintDescriptor descriptor = BY_ID.get(id);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown catalog constraint: " + id);
        }
        return descriptor;
    }

    private static List<String> ids(Category category) {
        return CONSTRAINTS.stream()
                .filter(descriptor -> descriptor.category() == category)
                .map(ConstraintDescriptor::id)
                .toList();
    }

    private static ConstraintDescriptor hard(String id) {
        return new ConstraintDescriptor(id, Category.HARD, null);
    }

    private static ConstraintDescriptor soft(String id) {
        return new ConstraintDescriptor(id, Category.ORDINARY_PREFERENCE, 1L);
    }
}
