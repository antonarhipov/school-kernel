package org.schoolkernel.solver;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.BendableScore;

@PlanningSolution
public class SchoolSchedule {
    private List<PeriodValue> periods;
    private List<RoomValue> rooms;
    private List<PlanningLesson> lessons;
    private ConstraintWeightOverrides<BendableScore> constraintWeights;
    private BendableScore score;

    public SchoolSchedule() {}

    public static SchoolSchedule empty() {
        return new SchoolSchedule(List.of(), List.of(), List.of(), ConstraintWeightOverrides.none());
    }

    public SchoolSchedule(
            List<PeriodValue> periods,
            List<RoomValue> rooms,
            List<PlanningLesson> lessons,
            ConstraintWeightOverrides<BendableScore> constraintWeights) {
        this.periods = periods;
        this.rooms = rooms;
        this.lessons = lessons;
        this.constraintWeights = constraintWeights;
    }

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "periodRange")
    public List<PeriodValue> getPeriods() {
        return periods;
    }

    public void setPeriods(List<PeriodValue> periods) {
        this.periods = periods;
    }

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    public List<RoomValue> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomValue> rooms) {
        this.rooms = rooms;
    }

    @PlanningEntityCollectionProperty
    public List<PlanningLesson> getLessons() {
        return lessons;
    }

    public void setLessons(List<PlanningLesson> lessons) {
        this.lessons = lessons;
    }

    public ConstraintWeightOverrides<BendableScore> getConstraintWeights() {
        return constraintWeights;
    }

    public void setConstraintWeights(ConstraintWeightOverrides<BendableScore> constraintWeights) {
        this.constraintWeights = constraintWeights;
    }

    @PlanningScore(bendableHardLevelsSize = 2, bendableSoftLevelsSize = 3)
    public BendableScore getScore() {
        return score;
    }

    public void setScore(BendableScore score) {
        this.score = score;
    }
}
