package io.github.la8garlic.signlens.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PerformanceCountersTest {

    @Test
    void snapshotsRuntimeCountsAndAverageRayTraceTime() {
        PerformanceCounters counters = new PerformanceCounters();

        counters.recordScanSkip();
        counters.recordIdleProbe();
        counters.recordRayTrace(true, 1_000_000L);
        counters.recordRayTrace(false, 3_000_000L);
        counters.recordSnapshotRead();
        counters.recordFormatterInvocation();
        counters.recordActionBarSend();
        counters.recordActionBarClear();

        PerformanceCounters.Snapshot snapshot = counters.snapshot();
        assertEquals(1L, snapshot.scanSkips());
        assertEquals(1L, snapshot.idleProbes());
        assertEquals(2L, snapshot.rayTraces());
        assertEquals(1L, snapshot.rayTraceHits());
        assertEquals(1L, snapshot.rayTraceMisses());
        assertEquals(2_000_000.0, snapshot.averageRayTraceNanos());
        assertEquals(1L, snapshot.snapshotReads());
        assertEquals(1L, snapshot.formatterInvocations());
        assertEquals(1L, snapshot.actionBarSends());
        assertEquals(1L, snapshot.actionBarClears());
    }

    @Test
    void resetClearsAllCounters() {
        PerformanceCounters counters = new PerformanceCounters();
        counters.recordRayTrace(true, 1L);
        counters.recordActionBarSend();

        counters.reset();

        PerformanceCounters.Snapshot snapshot = counters.snapshot();
        assertEquals(0L, snapshot.rayTraces());
        assertEquals(0L, snapshot.actionBarSends());
        assertEquals(0.0, snapshot.averageRayTraceNanos());
    }
}
