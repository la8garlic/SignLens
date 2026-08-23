package io.github.la8garlic.signlens.metrics;

import java.util.concurrent.atomic.LongAdder;

/**
 * Local, resettable runtime counters used by diagnostics and validation.
 *
 * <p>The counters contain no player or world references and do not publish
 * telemetry. LongAdder keeps recording cheap when several player-owned tasks
 * report at the same time.</p>
 */
public final class PerformanceCounters {

    private final LongAdder scanSkips = new LongAdder();
    private final LongAdder idleProbes = new LongAdder();
    private final LongAdder rayTraces = new LongAdder();
    private final LongAdder rayTraceHits = new LongAdder();
    private final LongAdder rayTraceMisses = new LongAdder();
    private final LongAdder rayTraceNanos = new LongAdder();
    private final LongAdder snapshotReads = new LongAdder();
    private final LongAdder formatterInvocations = new LongAdder();
    private final LongAdder actionBarSends = new LongAdder();
    private final LongAdder actionBarClears = new LongAdder();

    public void recordScanSkip() {
        scanSkips.increment();
    }

    public void recordIdleProbe() {
        idleProbes.increment();
    }

    public void recordRayTrace(boolean hit, long durationNanos) {
        if (durationNanos < 0) {
            throw new IllegalArgumentException("durationNanos must not be negative");
        }
        rayTraces.increment();
        (hit ? rayTraceHits : rayTraceMisses).increment();
        rayTraceNanos.add(durationNanos);
    }

    public void recordSnapshotRead() {
        snapshotReads.increment();
    }

    public void recordFormatterInvocation() {
        formatterInvocations.increment();
    }

    public void recordActionBarSend() {
        actionBarSends.increment();
    }

    public void recordActionBarClear() {
        actionBarClears.increment();
    }

    public Snapshot snapshot() {
        long traces = rayTraces.sum();
        return new Snapshot(
                scanSkips.sum(),
                idleProbes.sum(),
                traces,
                rayTraceHits.sum(),
                rayTraceMisses.sum(),
                rayTraceNanos.sum(),
                snapshotReads.sum(),
                formatterInvocations.sum(),
                actionBarSends.sum(),
                actionBarClears.sum()
        );
    }

    public void reset() {
        scanSkips.reset();
        idleProbes.reset();
        rayTraces.reset();
        rayTraceHits.reset();
        rayTraceMisses.reset();
        rayTraceNanos.reset();
        snapshotReads.reset();
        formatterInvocations.reset();
        actionBarSends.reset();
        actionBarClears.reset();
    }

    public record Snapshot(
            long scanSkips,
            long idleProbes,
            long rayTraces,
            long rayTraceHits,
            long rayTraceMisses,
            long rayTraceNanos,
            long snapshotReads,
            long formatterInvocations,
            long actionBarSends,
            long actionBarClears
    ) {

        public double averageRayTraceNanos() {
            return rayTraces == 0 ? 0.0 : (double) rayTraceNanos / rayTraces;
        }
    }
}
