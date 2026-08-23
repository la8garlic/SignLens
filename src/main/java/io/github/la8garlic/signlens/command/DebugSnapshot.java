package io.github.la8garlic.signlens.command;

import io.github.la8garlic.signlens.focus.FocusController;
import io.github.la8garlic.signlens.focus.FocusState;
import io.github.la8garlic.signlens.focus.FocusTarget;
import io.github.la8garlic.signlens.metrics.PerformanceCounters;
import io.github.la8garlic.signlens.reading.SignKey;
import io.github.la8garlic.signlens.session.PlayerSession;
import io.github.la8garlic.signlens.render.FormattedContent;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/** Immutable, sender-local diagnostic view of one player session. */
public record DebugSnapshot(
        FocusState state,
        Optional<FocusTarget> target,
        Optional<SignKey> signKey,
        OptionalDouble distance,
        Optional<Duration> activeDuration,
        Optional<Duration> lastRayTraceAge,
        Optional<Duration> lastRenderAge,
        int lineCount,
        int visualCharacterCount,
        PerformanceCounters.Snapshot counters
) {

    public DebugSnapshot {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(signKey, "signKey");
        Objects.requireNonNull(distance, "distance");
        Objects.requireNonNull(activeDuration, "activeDuration");
        Objects.requireNonNull(lastRayTraceAge, "lastRayTraceAge");
        Objects.requireNonNull(lastRenderAge, "lastRenderAge");
        Objects.requireNonNull(counters, "counters");
        if (lineCount < 0 || visualCharacterCount < 0) {
            throw new IllegalArgumentException("content counts must not be negative");
        }
    }

    public static DebugSnapshot capture(PlayerSession session, PerformanceCounters counters, Instant now) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(counters, "counters");
        Objects.requireNonNull(now, "now");

        FocusController focus = session.focusController();
        FocusState state = focus.state();
        Optional<FocusTarget> target = focus.currentTarget();
        Optional<SignKey> signKey = state == FocusState.IDLE
                ? Optional.empty()
                : session.lastSnapshot().map(snapshot -> snapshot.key());
        Optional<FormattedContent> content = session.renderPolicy().lastSentContent();
        return new DebugSnapshot(
                state,
                target,
                signKey,
                target.isPresent() ? session.lastDistance() : OptionalDouble.empty(),
                age(focus.activeSince(), now),
                age(session.lastRayTraceAt(), now),
                age(session.renderPolicy().lastSentAt(), now),
                content.map(value -> value.lines().size()).orElse(0),
                content.map(FormattedContent::visualLength).orElse(0),
                counters.snapshot()
        );
    }

    public static DebugSnapshot noSession(PerformanceCounters counters) {
        Objects.requireNonNull(counters, "counters");
        return new DebugSnapshot(
                FocusState.IDLE,
                Optional.empty(),
                Optional.empty(),
                OptionalDouble.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                0,
                counters.snapshot()
        );
    }

    private static Optional<Duration> age(Optional<Instant> timestamp, Instant now) {
        return timestamp.map(value -> value.isAfter(now) ? Duration.ZERO : Duration.between(value, now));
    }
}
