package io.github.la8garlic.signlens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JavaRuntimeContractTest {

    @Test
    void projectRunsOnTheJava25Baseline() {
        assertEquals(25, Runtime.version().feature());
    }

    @Test
    void pluginDescriptorAdvertisesOnlyImplementedCommands() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(stream);
            String descriptor = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(descriptor.contains("version: '0.1.0'"));
            assertTrue(descriptor.contains("usage: /signlens debug"));
            assertTrue(descriptor.contains("signlens.command.debug:"));
            assertFalse(descriptor.contains("toggle"));
            assertFalse(descriptor.contains("reload"));
        }
    }
}
