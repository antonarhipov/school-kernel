package org.schoolkernel.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SchoolDefinition(
        int schemaVersion,
        int catalogVersion,
        String schoolId,
        String displayName,
        String basedOnRevision,
        List<Subject> subjects,
        List<Teacher> teachers,
        List<Cohort> cohorts,
        List<Room> rooms,
        List<Period> periods,
        Set<String> reservedPeriodIds,
        List<Lesson> lessons,
        Map<String, Long> softWeights) {

    public record Subject(
            String id,
            String displayName,
            boolean reservedPeriodsAllowed,
            int maxWeeklyReservedLessonsPerCohort,
            boolean dayEdgeOnly,
            int maxDailyLessonsPerCohort) {
        public static final int UNLIMITED = Integer.MAX_VALUE;

        public Subject(String id, String displayName) {
            this(id, displayName, false, UNLIMITED, false, UNLIMITED);
        }
    }

    public record Teacher(
            String id,
            String displayName,
            Set<String> qualifiedSubjectIds,
            Set<String> availablePeriodIds,
            Set<String> undesirablePeriodIds) {}

    public record Cohort(
            String id,
            String displayName,
            int size,
            Set<String> availablePeriodIds,
            Set<String> undesirablePeriodIds,
            int maxDailyLessonSpread,
            int maxDailyGaps,
            int latestStartSlot,
            int preferredLatestStartSlot) {
        /** Catalogs before version 5 place no hard limit on cohort gaps. */
        public static final int UNLIMITED_GAPS = Integer.MAX_VALUE;
        /** Without a declared bound, any start slot is feasible. */
        public static final int NO_START_BOUND = Integer.MAX_VALUE;
        /** The catalog 3 late-start threshold. */
        public static final int DEFAULT_PREFERRED_START_SLOT = 3;

        public Cohort(String id, String displayName, int size, Set<String> availablePeriodIds,
                Set<String> undesirablePeriodIds) {
            this(id, displayName, size, availablePeriodIds, undesirablePeriodIds, 1);
        }

        public Cohort(String id, String displayName, int size, Set<String> availablePeriodIds,
                Set<String> undesirablePeriodIds, int maxDailyLessonSpread) {
            this(id, displayName, size, availablePeriodIds, undesirablePeriodIds, maxDailyLessonSpread,
                    UNLIMITED_GAPS);
        }

        public Cohort(String id, String displayName, int size, Set<String> availablePeriodIds,
                Set<String> undesirablePeriodIds, int maxDailyLessonSpread, int maxDailyGaps) {
            this(id, displayName, size, availablePeriodIds, undesirablePeriodIds, maxDailyLessonSpread,
                    maxDailyGaps, NO_START_BOUND, DEFAULT_PREFERRED_START_SLOT);
        }
    }

    public record Room(
            String id,
            String displayName,
            int capacity,
            Set<String> capabilityIds,
            Set<String> availablePeriodIds) {}

    public record Period(
            String id,
            String displayName,
            DayOfWeek weekday,
            int order,
            LocalTime startTime,
            LocalTime endTime) {}

    public record Lesson(
            String id,
            String displayName,
            String subjectId,
            String cohortId,
            String teacherId,
            String seriesId,
            Set<String> requiredRoomCapabilityIds,
            Set<String> preferredRoomIds,
            Set<String> undesirablePeriodIds,
            String periodLock,
            String roomLock) {}
}
