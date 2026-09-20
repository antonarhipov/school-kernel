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
}
