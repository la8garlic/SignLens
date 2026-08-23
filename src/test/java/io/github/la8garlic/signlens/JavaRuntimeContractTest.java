package io.github.la8garlic.signlens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JavaRuntimeContractTest {

    @Test
    void projectRunsOnTheJava25Baseline() {
        assertEquals(25, Runtime.version().feature());
    }
}
