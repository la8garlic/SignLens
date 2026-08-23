package io.github.la8garlic.signlens.reading;

import io.github.la8garlic.signlens.detection.DetectedSign;
import io.github.la8garlic.signlens.metrics.PerformanceCounters;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

/** Paper implementation of {@link SignReader}. */
public final class PaperSignReader implements SignReader {

    private static final long UNEXPECTED_FAILURE_REPORT_INTERVAL_MILLIS = 30_000L;

    private final PerformanceCounters counters;
    private final BiConsumer<DetectedSign, RuntimeException> unexpectedFailureReporter;
    private final Clock clock;
    private final AtomicLong lastFailureReportMillis = new AtomicLong(Long.MIN_VALUE);

    public PaperSignReader() {
        this(new PerformanceCounters(), (ignoredSign, ignoredFailure) -> {}, Clock.systemUTC());
    }

    public PaperSignReader(
            PerformanceCounters counters,
            BiConsumer<DetectedSign, RuntimeException> unexpectedFailureReporter
    ) {
        this(counters, unexpectedFailureReporter, Clock.systemUTC());
    }

    PaperSignReader(
            PerformanceCounters counters,
            BiConsumer<DetectedSign, RuntimeException> unexpectedFailureReporter,
            Clock clock
    ) {
        this.counters = Objects.requireNonNull(counters, "counters");
        this.unexpectedFailureReporter = Objects.requireNonNull(
                unexpectedFailureReporter,
                "unexpectedFailureReporter"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Optional<SignSnapshot> read(Player viewer, DetectedSign detectedSign) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(detectedSign, "detectedSign");

        if (!viewer.getWorld().getUID().equals(detectedSign.worldId())) {
            return Optional.empty();
        }

        try {
            Block block = viewer.getWorld().getBlockAt(detectedSign.x(), detectedSign.y(), detectedSign.z());
            if (!(block.getState() instanceof Sign sign)) {
                return Optional.empty();
            }

            Side side = sign.getInteractableSideFor(viewer);
            SignSide signSide = sign.getSide(side);
            SignContent content = new SignContent(
                    signSide.lines(),
                    signSide.getColor(),
                    signSide.isGlowingText()
            );
            SignKey key = new SignKey(
                    detectedSign.worldId(),
                    detectedSign.x(),
                    detectedSign.y(),
                    detectedSign.z(),
                    side
            );
            return Optional.of(new SignSnapshot(key, content));
        } catch (IllegalStateException | IllegalArgumentException unavailableSign) {
            counters.recordSignReadUnavailable();
            return Optional.empty();
        } catch (RuntimeException unexpectedFailure) {
            counters.recordSignReadFailure();
            reportUnexpectedFailure(detectedSign, unexpectedFailure);
            return Optional.empty();
        }
    }

    private void reportUnexpectedFailure(DetectedSign detectedSign, RuntimeException failure) {
        long now = clock.millis();
        while (true) {
            long previous = lastFailureReportMillis.get();
            if (previous != Long.MIN_VALUE && now - previous < UNEXPECTED_FAILURE_REPORT_INTERVAL_MILLIS) {
                return;
            }
            if (lastFailureReportMillis.compareAndSet(previous, now)) {
                unexpectedFailureReporter.accept(detectedSign, failure);
                return;
            }
        }
    }
}
