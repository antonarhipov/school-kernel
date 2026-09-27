package org.schoolkernel.solver;

import org.schoolkernel.domain.SchoolDefinition;

/** Catalog 6 cohort start bounds and subject placement rules, copied onto each lesson. */
public record PlacementRules(
        int latestStartSlot,
        int preferredLatestStartSlot,
        boolean reservedPeriodsAllowed,
        int maxWeeklyReservedLessons,
        boolean dayEdgeOnly,
        int maxDailyLessons) {
    public static final int UNLIMITED = Integer.MAX_VALUE;
    public static final PlacementRules NONE = new PlacementRules(
            SchoolDefinition.Cohort.NO_START_BOUND, SchoolDefinition.Cohort.DEFAULT_PREFERRED_START_SLOT,
            false, UNLIMITED, false, UNLIMITED);

    public static PlacementRules of(SchoolDefinition.Cohort cohort, SchoolDefinition.Subject subject) {
        return new PlacementRules(
                cohort.latestStartSlot(), cohort.preferredLatestStartSlot(),
                subject.reservedPeriodsAllowed(), subject.maxWeeklyReservedLessonsPerCohort(),
                subject.dayEdgeOnly(), subject.maxDailyLessonsPerCohort());
    }
}
