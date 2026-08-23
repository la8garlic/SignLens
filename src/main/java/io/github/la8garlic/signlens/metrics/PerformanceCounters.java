package io.github.la8garlic.signlens.metrics;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.Arrays;

/**
 * Local, resettable runtime counters used by diagnostics and validation.
 *
 * <p>The counters contain no player or world references and do not publish
 * telemetry. LongAdder keeps recording cheap when several player-owned tasks
 * report at the same time.</p>
 */
public final class PerformanceCounters {

    private static final int SCAN_SAMPLE_CAPACITY = 256;

    private final LongAdder scanSkips = new LongAdder();
    private final LongAdder idleProbes = new LongAdder();
    private final LongAdder scanCycles = new LongAdder();
    private final LongAdder scanNanos = new LongAdder();
    private final AtomicLong scanSampleSequence = new AtomicLong();
    private final AtomicLongArray scanSamples = new AtomicLongArray(SCAN_SAMPLE_CAPACITY);
    private final LongAdder rayTraces = new LongAdder();
    private final LongAdder rayTraceHits = new LongAdder();
    private final LongAdder rayTraceMisses = new LongAdder();
    private final LongAdder rayTraceNanos = new LongAdder();
    private final LongAdder snapshotReads = new LongAdder();
    private final LongAdder signReadUnavailable = new LongAdder();
    private final LongAdder signReadFailures = new LongAdder();
    private final LongAdder formatterInvocations = new LongAdder();
    private final LongAdder actionBarSends = new LongAdder();
    private final LongAdder actionBarClears = new LongAdder();

    public void recordScanSkip() {
        scanSkips.increment();
    }

    public void recordScan(long durationNanos) {
        if (durationNanos < 0) {
            throw new IllegalArgumentException("durationNanos must not be negative");
        }
        scanCycles.increment();
        scanNanos.add(durationNanos);
        long sequence = scanSampleSequence.getAndIncrement();
        scanSamples.set((int) (sequence % SCAN_SAMPLE_CAPACITY), durationNanos);
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

    public void recordSignReadUnavailable() {
        signReadUnavailable.increment();
    }

    public void recordSignReadFailure() {
        signReadFailures.increment();
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
        long scans = scanCycles.sum();
        return new Snapshot(
                scanSkips.sum(),
                idleProbes.sum(),
                scans,
                scanNanos.sum(),
                p95ScanNanos(scans),
                traces,
                rayTraceHits.sum(),
                rayTraceMisses.sum(),
                rayTraceNanos.sum(),
                snapshotReads.sum(),
                signReadUnavailable.sum(),
                signReadFailures.sum(),
                formatterInvocations.sum(),
                actionBarSends.sum(),
                actionBarClears.sum()
        );
    }

    public void reset() {
        scanSkips.reset();
        idleProbes.reset();
        scanCycles.reset();
        scanNanos.reset();
        scanSampleSequence.set(0L);
        for (int index = 0; index < SCAN_SAMPLE_CAPACITY; index++) {
            scanSamples.set(index, 0L);
        }
        rayTraces.reset();
        rayTraceHits.reset();
        rayTraceMisses.reset();
        rayTraceNanos.reset();
        snapshotReads.reset();
        signReadUnavailable.reset();
        signReadFailures.reset();
        formatterInvocations.reset();
        actionBarSends.reset();
        actionBarClears.reset();
    }

    public record Snapshot(
            long scanSkips,
            long idleProbes,
            long scanCycles,
            long scanNanos,
            long p95ScanNanos,
            long rayTraces,
            long rayTraceHits,
            long rayTraceMisses,
            long rayTraceNanos,
            long snapshotReads,
            long signReadUnavailable,
            long signReadFailures,
            long formatterInvocations,
            long actionBarSends,
            long actionBarClears
    ) {

        public double averageScanNanos() {
            return scanCycles == 0 ? 0.0 : (double) scanNanos / scanCycles;
        }

        public double averageRayTraceNanos() {
            return rayTraces == 0 ? 0.0 : (double) rayTraceNanos / rayTraces;
        }
    }

    private long p95ScanNanos(long scanCount) {
        int sampleCount = (int) Math.min(scanCount, SCAN_SAMPLE_CAPACITY);
        if (sampleCount == 0) {
            return 0L;
        }

        long sequence = scanSampleSequence.get();
        long[] samples = new long[sampleCount];
        long firstSequence = Math.max(0L, sequence - sampleCount);
        for (int index = 0; index < sampleCount; index++) {
            samples[index] = scanSamples.get((int) ((firstSequence + index) % SCAN_SAMPLE_CAPACITY));
        }
        Arrays.sort(samples);
        int p95Index = Math.max(0, (int) Math.ceil(samples.length * 0.95) - 1);
        return samples[p95Index];
    }
}
