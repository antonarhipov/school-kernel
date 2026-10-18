package org.schoolkernel.workspace;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/** Resources opened by name rather than referenced by the Spring bean graph. */
class DemoRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("demo/timetables/*.json");
        hints.resources().registerPattern("schema/*.json");
        hints.resources().registerPattern("demo-logback.xml");
    }
}
