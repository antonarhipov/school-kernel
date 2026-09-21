package org.schoolkernel.contract;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.core.exc.StreamConstraintsException;

class JsonSupportLimitsTest {
    @Test
    @DisplayName("RULE-28: nesting is accepted below and at 64 and refused above")
    void nestingBoundary() {
        assertDoesNotThrow(() -> parseNested(63));
        assertDoesNotThrow(() -> parseNested(64));
        assertThrows(StreamConstraintsException.class, () -> parseNested(65));
    }

    @Test
    @DisplayName("RULE-28: string tokens are accepted below and at 1 MiB and refused above")
    void stringBoundary() {
        assertDoesNotThrow(() -> parseString(JsonSupport.MAXIMUM_STRING_LENGTH - 1));
        assertDoesNotThrow(() -> parseString(JsonSupport.MAXIMUM_STRING_LENGTH));
        assertThrows(StreamConstraintsException.class,
                () -> parseString(JsonSupport.MAXIMUM_STRING_LENGTH + 1));
    }

    @Test
    @DisplayName("RULE-28: numeric tokens are accepted below and at 100 characters and refused above")
    void numberBoundary() {
        assertDoesNotThrow(() -> parseNumber(JsonSupport.MAXIMUM_NUMBER_LENGTH - 1));
        assertDoesNotThrow(() -> parseNumber(JsonSupport.MAXIMUM_NUMBER_LENGTH));
        assertThrows(StreamConstraintsException.class,
                () -> parseNumber(JsonSupport.MAXIMUM_NUMBER_LENGTH + 1));
    }

    private static void parseNested(int depth) throws Exception {
        JsonSupport.mapper().readTree("[".repeat(depth) + "0" + "]".repeat(depth));
    }

    private static void parseString(int length) throws Exception {
        JsonSupport.mapper().readTree("\"" + "x".repeat(length) + "\"");
    }

    private static void parseNumber(int length) throws Exception {
        JsonSupport.mapper().readTree("1" + "0".repeat(length - 1));
    }
}
