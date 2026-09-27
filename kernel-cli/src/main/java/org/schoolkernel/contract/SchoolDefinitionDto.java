package org.schoolkernel.contract;

import java.util.List;

public record SchoolDefinitionDto(
        int schemaVersion,
        int catalogVersion,
        String schoolId,
        String displayName,
        String basedOnRevision,
        List<SubjectDto> subjects,
        List<TeacherDto> teachers,
        List<CohortDto> cohorts,
        List<RoomDto> rooms,
        List<PeriodDto> periods,
        List<String> reservedPeriodIds,
        List<LessonDto> lessons,
        List<SoftConstraintOverrideDto> softConstraintOverrides) {

    public record SubjectDto(
            String id,
            String displayName,
            Boolean reservedPeriodsAllowed,
            Integer maxWeeklyReservedLessonsPerCohort,
            Boolean dayEdgeOnly,
            Integer maxDailyLessonsPerCohort,
            Boolean curatorLesson) {}

    public record TeacherDto(
            String id,
            String displayName,
            List<String> qualifiedSubjectIds,
            List<String> availablePeriodIds,
            List<String> undesirablePeriodIds) {}

    public record CohortDto(
            String id,
            String displayName,
            int size,
            List<String> availablePeriodIds,
            List<String> undesirablePeriodIds,
            Integer maxDailyLessonSpread,
            Integer maxDailyGaps,
            Integer latestStartSlot,
            Integer preferredLatestStartSlot,
            Integer dailyLessonSpreadLimit,
            String curatorTeacherId,
            String homeRoomId) {}

    public record RoomDto(
            String id,
            String displayName,
            int capacity,
            List<String> capabilityIds,
            List<String> availablePeriodIds) {}

    public record PeriodDto(
            String id,
            String displayName,
            String weekday,
            int order,
            String startTime,
            String endTime) {}

    public record LessonDto(
            String id,
            String displayName,
            String subjectId,
            String cohortId,
            String teacherId,
            String seriesId,
            List<String> requiredRoomCapabilityIds,
            List<String> preferredRoomIds,
            List<String> undesirablePeriodIds,
            String periodLock,
            String roomLock) {}

    public record SoftConstraintOverrideDto(String constraintId, long weight) {}
}
