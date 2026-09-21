package org.schoolkernel.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The immutable single source of truth for the version 1 constraint catalog. */
public final class KernelCatalog {
    public static final int VERSION = 1;

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
    public static final ConstraintDescriptor PERIOD_MOVE =
            new ConstraintDescriptor("stability.period-move", Category.PERIOD_STABILITY, null);
    public static final ConstraintDescriptor ROOM_ONLY_MOVE =
            new ConstraintDescriptor("stability.room-only-move", Category.ROOM_STABILITY, null);
    public static final ConstraintDescriptor TEACHER_GAP = soft("soft.teacher-gap");
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
            PERIOD_MOVE,
            ROOM_ONLY_MOVE,
            TEACHER_GAP,
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

    public static Map<String, Long> defaultSoftWeights() {
        return CONSTRAINTS.stream()
                .filter(descriptor -> descriptor.category() == Category.ORDINARY_PREFERENCE)
                .collect(Collectors.toUnmodifiableMap(
                        ConstraintDescriptor::id,
                        ConstraintDescriptor::defaultWeight));
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
