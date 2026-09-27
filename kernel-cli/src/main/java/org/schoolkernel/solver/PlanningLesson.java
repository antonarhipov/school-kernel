package org.schoolkernel.solver;

import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class PlanningLesson {
    private String id;
    private String subjectId;
    private String cohortId;
    private int cohortSize;
    private int cohortMaxDailyLessonSpread = 1;
    private int cohortMaxDailyGaps = Integer.MAX_VALUE;
    private String teacherId;
    private String seriesId;
    private Set<String> teacherAvailablePeriodIds;
    private Set<String> teacherUndesirablePeriodIds;
    private Set<String> cohortAvailablePeriodIds;
    private Set<String> cohortUndesirablePeriodIds;
    private Set<String> lessonUndesirablePeriodIds;
    private Set<String> requiredRoomCapabilityIds;
    private Set<String> preferredRoomIds;
    private Set<String> allowedRoomAssignmentIds = Set.of();
    private List<String> roomAssignmentPolicyIds = List.of();
    private String periodLock;
    private String roomLock;
    private String baselinePeriodId;
    private String baselineRoomId;
    private List<PeriodValue> periodCatalog;
    private PlacementRules placementRules = PlacementRules.NONE;
    private PeriodValue period;
    private RoomValue room;

    public PlanningLesson() {}

    public PlanningLesson(
            String id,
            String subjectId,
            String cohortId,
            int cohortSize,
            String teacherId,
            String seriesId,
            Set<String> teacherAvailablePeriodIds,
            Set<String> teacherUndesirablePeriodIds,
            Set<String> cohortAvailablePeriodIds,
            Set<String> cohortUndesirablePeriodIds,
            Set<String> lessonUndesirablePeriodIds,
            Set<String> requiredRoomCapabilityIds,
            Set<String> preferredRoomIds,
            String periodLock,
            String roomLock,
            List<PeriodValue> periodCatalog) {
        this(id, subjectId, cohortId, cohortSize, teacherId, seriesId,
                teacherAvailablePeriodIds, teacherUndesirablePeriodIds,
                cohortAvailablePeriodIds, cohortUndesirablePeriodIds, lessonUndesirablePeriodIds,
                requiredRoomCapabilityIds, preferredRoomIds, periodLock, roomLock,
                null, null, periodCatalog);
    }

    public PlanningLesson(
            String id,
            String subjectId,
            String cohortId,
            int cohortSize,
            String teacherId,
            String seriesId,
            Set<String> teacherAvailablePeriodIds,
            Set<String> teacherUndesirablePeriodIds,
            Set<String> cohortAvailablePeriodIds,
            Set<String> cohortUndesirablePeriodIds,
            Set<String> lessonUndesirablePeriodIds,
            Set<String> requiredRoomCapabilityIds,
            Set<String> preferredRoomIds,
            String periodLock,
            String roomLock,
            String baselinePeriodId,
            String baselineRoomId,
            List<PeriodValue> periodCatalog) {
        this(id, subjectId, cohortId, cohortSize, teacherId, seriesId,
                teacherAvailablePeriodIds, teacherUndesirablePeriodIds,
                cohortAvailablePeriodIds, cohortUndesirablePeriodIds, lessonUndesirablePeriodIds,
                requiredRoomCapabilityIds, preferredRoomIds, periodLock, roomLock,
                baselinePeriodId, baselineRoomId, periodCatalog, 1);
    }

    public PlanningLesson(
            String id,
            String subjectId,
            String cohortId,
            int cohortSize,
            String teacherId,
            String seriesId,
            Set<String> teacherAvailablePeriodIds,
            Set<String> teacherUndesirablePeriodIds,
            Set<String> cohortAvailablePeriodIds,
            Set<String> cohortUndesirablePeriodIds,
            Set<String> lessonUndesirablePeriodIds,
            Set<String> requiredRoomCapabilityIds,
            Set<String> preferredRoomIds,
            String periodLock,
            String roomLock,
            String baselinePeriodId,
            String baselineRoomId,
            List<PeriodValue> periodCatalog,
            int cohortMaxDailyLessonSpread) {
        this(id, subjectId, cohortId, cohortSize, teacherId, seriesId,
                teacherAvailablePeriodIds, teacherUndesirablePeriodIds,
                cohortAvailablePeriodIds, cohortUndesirablePeriodIds, lessonUndesirablePeriodIds,
                requiredRoomCapabilityIds, preferredRoomIds, periodLock, roomLock,
                baselinePeriodId, baselineRoomId, periodCatalog, cohortMaxDailyLessonSpread, Integer.MAX_VALUE);
    }

    public PlanningLesson(
            String id,
            String subjectId,
            String cohortId,
            int cohortSize,
            String teacherId,
            String seriesId,
            Set<String> teacherAvailablePeriodIds,
            Set<String> teacherUndesirablePeriodIds,
            Set<String> cohortAvailablePeriodIds,
            Set<String> cohortUndesirablePeriodIds,
            Set<String> lessonUndesirablePeriodIds,
            Set<String> requiredRoomCapabilityIds,
            Set<String> preferredRoomIds,
            String periodLock,
            String roomLock,
            String baselinePeriodId,
            String baselineRoomId,
            List<PeriodValue> periodCatalog,
            int cohortMaxDailyLessonSpread,
            int cohortMaxDailyGaps) {
        this(id, subjectId, cohortId, cohortSize, teacherId, seriesId,
                teacherAvailablePeriodIds, teacherUndesirablePeriodIds,
                cohortAvailablePeriodIds, cohortUndesirablePeriodIds, lessonUndesirablePeriodIds,
                requiredRoomCapabilityIds, preferredRoomIds, periodLock, roomLock,
                baselinePeriodId, baselineRoomId, periodCatalog, cohortMaxDailyLessonSpread, cohortMaxDailyGaps,
                PlacementRules.NONE);
    }

    public PlanningLesson(
            String id,
            String subjectId,
            String cohortId,
            int cohortSize,
            String teacherId,
            String seriesId,
            Set<String> teacherAvailablePeriodIds,
            Set<String> teacherUndesirablePeriodIds,
            Set<String> cohortAvailablePeriodIds,
            Set<String> cohortUndesirablePeriodIds,
            Set<String> lessonUndesirablePeriodIds,
            Set<String> requiredRoomCapabilityIds,
            Set<String> preferredRoomIds,
            String periodLock,
            String roomLock,
            String baselinePeriodId,
            String baselineRoomId,
            List<PeriodValue> periodCatalog,
            int cohortMaxDailyLessonSpread,
            int cohortMaxDailyGaps,
            PlacementRules placementRules) {
        this.id = id;
        this.subjectId = subjectId;
        this.cohortId = cohortId;
        this.cohortSize = cohortSize;
        this.cohortMaxDailyLessonSpread = cohortMaxDailyLessonSpread;
        this.cohortMaxDailyGaps = cohortMaxDailyGaps;
        this.teacherId = teacherId;
        this.seriesId = seriesId;
        this.teacherAvailablePeriodIds = teacherAvailablePeriodIds;
        this.teacherUndesirablePeriodIds = teacherUndesirablePeriodIds;
        this.cohortAvailablePeriodIds = cohortAvailablePeriodIds;
        this.cohortUndesirablePeriodIds = cohortUndesirablePeriodIds;
        this.lessonUndesirablePeriodIds = lessonUndesirablePeriodIds;
        this.requiredRoomCapabilityIds = requiredRoomCapabilityIds;
        this.preferredRoomIds = preferredRoomIds;
        this.periodLock = periodLock;
        this.roomLock = roomLock;
        this.baselinePeriodId = baselinePeriodId;
        this.baselineRoomId = baselineRoomId;
        this.periodCatalog = periodCatalog;
        this.placementRules = placementRules;
    }

    @PlanningId
    public String getId() {
        return id;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getCohortId() {
        return cohortId;
    }

    public int getCohortSize() {
        return cohortSize;
    }

    public int getCohortMaxDailyLessonSpread() {
        return cohortMaxDailyLessonSpread;
    }

    public int getCohortMaxDailyGaps() {
        return cohortMaxDailyGaps;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public Set<String> getTeacherAvailablePeriodIds() {
        return teacherAvailablePeriodIds;
    }

    public Set<String> getTeacherUndesirablePeriodIds() {
        return teacherUndesirablePeriodIds;
    }

    public Set<String> getCohortAvailablePeriodIds() {
        return cohortAvailablePeriodIds;
    }

    public Set<String> getCohortUndesirablePeriodIds() {
        return cohortUndesirablePeriodIds;
    }

    public Set<String> getLessonUndesirablePeriodIds() {
        return lessonUndesirablePeriodIds;
    }

    public Set<String> getRequiredRoomCapabilityIds() {
        return requiredRoomCapabilityIds;
    }

    public Set<String> getPreferredRoomIds() {
        return preferredRoomIds;
    }

    public String getPeriodLock() {
        return periodLock;
    }

    public String getRoomLock() {
        return roomLock;
    }

    public Set<String> getAllowedRoomAssignmentIds() {
        return allowedRoomAssignmentIds;
    }

    public List<String> getRoomAssignmentPolicyIds() {
        return roomAssignmentPolicyIds;
    }

    public void setRoomAssignment(Set<String> allowedRoomIds, List<String> policyIds) {
        this.allowedRoomAssignmentIds = Set.copyOf(allowedRoomIds);
        this.roomAssignmentPolicyIds = List.copyOf(policyIds);
    }

    public boolean roomAssignmentAllows(String roomId) {
        return roomAssignmentPolicyIds.isEmpty() || allowedRoomAssignmentIds.contains(roomId);
    }

    public String getBaselinePeriodId() {
        return baselinePeriodId;
    }

    public String getBaselineRoomId() {
        return baselineRoomId;
    }

    public List<PeriodValue> getPeriodCatalog() {
        return periodCatalog;
    }

    public PlacementRules getPlacementRules() {
        return placementRules;
    }

    public int getCohortLatestStartSlot() {
        return placementRules.latestStartSlot();
    }

    public int getCohortPreferredLatestStartSlot() {
        return placementRules.preferredLatestStartSlot();
    }

    @PlanningVariable(valueRangeProviderRefs = "periodRange")
    public PeriodValue getPeriod() {
        return period;
    }

    public void setPeriod(PeriodValue period) {
        this.period = period;
    }

    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    public RoomValue getRoom() {
        return room;
    }

    public void setRoom(RoomValue room) {
        this.room = room;
    }
}
