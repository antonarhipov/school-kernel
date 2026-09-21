package org.schoolkernel.contract;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KernelMetadataTest {
    @Test
    @DisplayName("RULE-27: malformed version metadata fails before result construction")
    void malformedVersionIsRejected() {
        String previous = System.getProperty("school.kernel.version");
        System.setProperty("school.kernel.version", "not a packaged version");
        try {
            assertThrows(IllegalStateException.class, KernelMetadata::version);
        } finally {
            if (previous == null) {
                System.clearProperty("school.kernel.version");
            } else {
                System.setProperty("school.kernel.version", previous);
            }
        }
    }
}
