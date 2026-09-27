package org.schoolkernel.solver;

import org.schoolkernel.domain.SchoolDefinition;

/**
 * Catalog 6 through 8 cohort day-shape bounds and subject placement rules, copied onto each lesson. A curator lesson
 * of a cohort with a home room carries that room as {@code homeRoomId}; every other lesson carries {@code null}.
 */
public record PlacementRules(
        int latestStartSlot,
        int preferredLatestStartSlot,
        boolean reservedPeriodsAllowed,
        int maxWeeklyReservedLessons,
        boolean dayEdgeOnly,
        int maxDailyLessons,
        int dailyLessonSpreadLimit,
        String homeRoomId) {
    public static final int UNLIMITED = Integer.MAX_VALUE;
    public static final PlacementRules NONE = new PlacementRules(
            SchoolDefinition.Cohort.NO_START_BOUND, SchoolDefinition.Cohort.DEFAULT_PREFERRED_START_SLOT,
            false, UNLIMITED, false, UNLIMITED);

    public PlacementRules(
            int latestStartSlot,
            int preferredLatestStartSlot,
            boolean reservedPeriodsAllowed,
            int maxWeeklyReservedLessons,
            boolean dayEdgeOnly,
            int maxDailyLessons) {
        this(latestStartSlot, preferredLatestStartSlot, reservedPeriodsAllowed, maxWeeklyReservedLessons,
                dayEdgeOnly, maxDailyLessons, SchoolDefinition.Cohort.NO_SPREAD_LIMIT);
    }

    public PlacementRules(
            int latestStartSlot,
            int preferredLatestStartSlot,
            boolean reservedPeriodsAllowed,
            int maxWeeklyReservedLessons,
            boolean dayEdgeOnly,
            int maxDailyLessons,
            int dailyLessonSpreadLimit) {
        this(latestStartSlot, preferredLatestStartSlot, reservedPeriodsAllowed, maxWeeklyReservedLessons,
                dayEdgeOnly, maxDailyLessons, dailyLessonSpreadLimit, null);
    }

    public static PlacementRules of(SchoolDefinition.Cohort cohort, SchoolDefinition.Subject subject) {
        return new PlacementRules(
                cohort.latestStartSlot(), cohort.preferredLatestStartSlot(),
                subject.reservedPeriodsAllowed(), subject.maxWeeklyReservedLessonsPerCohort(),
                subject.dayEdgeOnly(), subject.maxDailyLessonsPerCohort(), cohort.dailyLessonSpreadLimit(),
                subject.curatorLesson() ? cohort.homeRoomId() : null);
    }
}
