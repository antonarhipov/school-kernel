package org.schoolkernel.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.solver.SolverAdapter.ExecutionControls;

class SolverConfigurationTest {
    @Test
    @DisplayName("UC-1 G7: deterministic controls use one thread, fixed seed, and local move budget")
    void deterministicStepControls() {
        var config = SolverAdapter.baseConfig(new ExecutionControls(null, 123, -7));

        assertEquals(-7, config.getRandomSeed());
        assertEquals("NONE", config.getMoveThreadCount());
        assertNull(config.getTerminationConfig());
        var localSearch = (LocalSearchPhaseConfig) config.getPhaseConfigList().get(1);
        assertEquals(123L, localSearch.getTerminationConfig().getMoveCountLimit());
    }

    @Test
    @DisplayName("UC-1 G7: production controls use exactly the selected wall-clock limit")
    void wallClockControls() {
        var config = SolverAdapter.baseConfig(new ExecutionControls(Duration.ofSeconds(30), null, 0));

        assertEquals(Duration.ofSeconds(30), config.getTerminationConfig().getSpentLimit());
        var localSearch = (LocalSearchPhaseConfig) config.getPhaseConfigList().get(1);
        assertNull(localSearch.getTerminationConfig());
    }

    @Test
    @DisplayName("RULE-6 and UC-2 G5: product scoring has two ordered hard and three ordered soft levels")
    void bendableProductLevels() {
        assertEquals(2, SchoolConstraintProvider.HARD.hardLevelsSize());
        assertEquals(3, SchoolConstraintProvider.HARD.softLevelsSize());
        assertEquals(2, SchoolConstraintProvider.DAY_SHAPE_HARD.hardLevelsSize());
        assertEquals(1, SchoolConstraintProvider.DAY_SHAPE_HARD.hardScore(1));
        assertEquals(2, SchoolConstraintProvider.PERIOD_MOVE.hardLevelsSize());
        assertEquals(3, SchoolConstraintProvider.PERIOD_MOVE.softLevelsSize());
        assertEquals(2, SchoolConstraintProvider.ROOM_ONLY_MOVE.hardLevelsSize());
        assertEquals(3, SchoolConstraintProvider.ROOM_ONLY_MOVE.softLevelsSize());
        assertEquals(2, SchoolConstraintProvider.PREFERENCE.hardLevelsSize());
        assertEquals(3, SchoolConstraintProvider.PREFERENCE.softLevelsSize());
    }
}
