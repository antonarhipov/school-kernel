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
    private String teacherId;
    private String seriesId;
    private Set<String> teacherAvailablePeriodIds;
    private Set<String> teacherUndesirablePeriodIds;
    private Set<String> cohortAvailablePeriodIds;
    private Set<String> cohortUndesirablePeriodIds;
    private Set<String> lessonUndesirablePeriodIds;
    private Set<String> requiredRoomCapabilityIds;
    private Set<String> preferredRoomIds;
    private String periodLock;
    private String roomLock;
    private List<PeriodValue> periodCatalog;
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
        this.id = id;
        this.subjectId = subjectId;
        this.cohortId = cohortId;
        this.cohortSize = cohortSize;
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
        this.periodCatalog = periodCatalog;
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

    public List<PeriodValue> getPeriodCatalog() {
        return periodCatalog;
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
