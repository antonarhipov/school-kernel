package org.schoolkernel.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.SchoolDefinitionDto;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.domain.SchoolDefinition;

class ComponentBoundaryTest {
    @Test
    @DisplayName("RULE-1: public DTO and validated domain signatures contain no Timefold types or annotations")
    void dtoAndDomainDoNotLeakTimefold() {
        List<Class<?>> publicModelTypes = List.of(
                SchoolDefinitionDto.class,
                SchoolDefinitionDto.SubjectDto.class,
                SchoolDefinitionDto.TeacherDto.class,
                SchoolDefinitionDto.CohortDto.class,
                SchoolDefinitionDto.RoomDto.class,
                SchoolDefinitionDto.PeriodDto.class,
                SchoolDefinitionDto.LessonDto.class,
                SchoolDefinitionDto.SoftConstraintOverrideDto.class,
                SchoolDefinition.class,
                SchoolDefinition.Subject.class,
                SchoolDefinition.Teacher.class,
                SchoolDefinition.Cohort.class,
                SchoolDefinition.Room.class,
                SchoolDefinition.Period.class,
                SchoolDefinition.Lesson.class);

        for (Class<?> type : publicModelTypes) {
            assertFalse(type.getName().startsWith("ai.timefold"));
            assertFalse(Arrays.stream(type.getAnnotations())
                    .anyMatch(annotation -> annotation.annotationType().getName().startsWith("ai.timefold")));
            assertFalse(Arrays.stream(type.getRecordComponents())
                    .anyMatch(component -> component.getGenericType().getTypeName().contains("ai.timefold")));
        }
    }

    @Test
    @DisplayName("RULE-1: validated domain maps into a separate planning model by value")
    void domainMapsToPlanningModelByValue() throws Exception {
        SchoolDefinition definition;
        try (InputStream input = ComponentBoundaryTest.class.getResourceAsStream("/fixtures/valid-plan.json")) {
            var dto = JsonSupport.mapper().treeToValue(
                    JsonSupport.mapper().readTree(input), SchoolDefinitionDto.class);
            definition = new DefinitionValidator().validateForPlan(dto).definition();
        }

        SchoolSchedule schedule = new PlanningMapper().toPlanningProblem(definition);
        PlanningLesson lesson = schedule.getLessons().getFirst();

        assertEquals(definition.periods().getFirst().id(), schedule.getPeriods().getFirst().id());
        assertEquals(definition.rooms().getFirst().id(), schedule.getRooms().getFirst().id());
        assertEquals(definition.lessons().getFirst().id(), lesson.getId());
        assertEquals(definition.lessons().getFirst().subjectId(), lesson.getSubjectId());
        assertEquals(definition.lessons().getFirst().cohortId(), lesson.getCohortId());
        assertEquals(definition.lessons().getFirst().teacherId(), lesson.getTeacherId());
        assertEquals(definition.softWeights().keySet(), schedule.getConstraintWeights().getKnownConstraintIds());
    }
}
