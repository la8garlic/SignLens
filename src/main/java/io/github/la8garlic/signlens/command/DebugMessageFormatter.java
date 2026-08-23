package io.github.la8garlic.signlens.command;

import io.github.la8garlic.signlens.focus.FocusTarget;
import io.github.la8garlic.signlens.metrics.PerformanceCounters;
import io.github.la8garlic.signlens.reading.SignKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;

/** Formats diagnostics as on-demand chat output without any runtime side effects. */
public final class DebugMessageFormatter {

    public Component format(DebugSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add("SignLens debug");
        lines.add("state: " + snapshot.state());
        lines.add("target: " + formatTarget(snapshot));
        lines.add("dwell: " + formatDuration(snapshot.activeDuration()));
        lines.add("last ray trace: " + formatDuration(snapshot.lastRayTraceAge()));
        lines.add("last render: " + formatDuration(snapshot.lastRenderAge()));
        lines.add("content: lines=" + snapshot.lineCount()
                + ", chars=" + snapshot.visualCharacterCount());
        lines.add("counters: " + formatCounters(snapshot.counters()));
        return Component.text(String.join("\n", lines));
    }

    private String formatTarget(DebugSnapshot snapshot) {
        if (snapshot.target().isEmpty()) {
            return "none";
        }

        FocusTarget target = snapshot.target().orElseThrow();
        String world = target.worldId().toString();
        String block = world + " @ " + target.x() + "," + target.y() + "," + target.z();
        String side = snapshot.signKey().map(SignKey::side).map(Enum::name).orElse("unknown-side");
        String distance = snapshot.distance().isPresent()
                ? String.format(Locale.ROOT, "%.2f blocks", snapshot.distance().getAsDouble())
                : "unknown distance";
        return block + " side=" + side + " distance=" + distance;
    }

    private String formatCounters(PerformanceCounters.Snapshot counters) {
        return "traces=" + counters.rayTraces()
                + ", hits=" + counters.rayTraceHits()
                + ", misses=" + counters.rayTraceMisses()
                + ", skipped=" + counters.scanSkips()
                + ", idle=" + counters.idleProbes()
                + ", scans=" + counters.scanCycles()
                + ", avg-scan=" + formatNanos(counters.averageScanNanos())
                + ", p95-scan=" + formatNanos(counters.p95ScanNanos())
                + ", avg-ray=" + String.format(Locale.ROOT, "%.3f ms", counters.averageRayTraceNanos() / 1_000_000.0)
                + ", actionbar=" + counters.actionBarSends() + "/" + counters.actionBarClears();
    }

    private String formatNanos(double nanos) {
        return String.format(Locale.ROOT, "%.3f ms", nanos / 1_000_000.0);
    }

    private static String formatDuration(java.util.Optional<java.time.Duration> duration) {
        return duration.map(value -> value.toMillis() + " ms ago").orElse("n/a");
    }
}
