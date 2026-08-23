package io.github.la8garlic.signlens.render;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure edge-triggered policy for formatted sign output.
 *
 * <p>The policy remembers only the last content actually sent. Callers apply
 * the returned decision to a {@link SignRenderer}; this class never talks to
 * Bukkit and therefore cannot send once per scan tick by itself.</p>
 */
public final class RenderPolicy {

    public static final Duration DEFAULT_KEEPALIVE = Duration.ofMillis(2500);

    private final Duration keepalive;
    private boolean focused;
    private FormattedContent lastSentContent;
    private Instant lastSentAt;
    private Instant lastObservedAt;

    public RenderPolicy() {
        this(DEFAULT_KEEPALIVE);
    }

    public RenderPolicy(Duration keepalive) {
        this.keepalive = requirePositive(keepalive, "keepalive");
    }

    public Duration keepalive() {
        return keepalive;
    }

    public boolean focused() {
        return focused;
    }

    public Optional<FormattedContent> lastSentContent() {
        return Optional.ofNullable(lastSentContent);
    }

    public Optional<Instant> lastSentAt() {
        return Optional.ofNullable(lastSentAt);
    }

    public RenderDecision observe(Optional<FormattedContent> content, boolean focusedNow, Instant now) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(now, "now");
        ensureMonotonic(now);

        RenderDecision decision;
        if (!focusedNow) {
            decision = lastSentContent == null ? RenderDecision.none() : RenderDecision.clear();
            focused = false;
            lastSentContent = null;
            lastSentAt = null;
        } else if (content.isEmpty()) {
            decision = lastSentContent == null ? RenderDecision.none() : RenderDecision.clear();
            focused = true;
            lastSentContent = null;
            lastSentAt = null;
        } else {
            FormattedContent current = content.orElseThrow();
            boolean focusEntry = !focused;
            boolean contentChanged = !current.equals(lastSentContent);
            boolean keepaliveDue = lastSentAt == null
                    || !now.isBefore(lastSentAt.plus(keepalive));
            decision = focusEntry || contentChanged || keepaliveDue
                    ? RenderDecision.show(current)
                    : RenderDecision.none();
            focused = true;
            if (decision.type() == RenderDecisionType.SHOW) {
                lastSentContent = current;
                lastSentAt = now;
            }
        }

        lastObservedAt = now;
        return decision;
    }

    public RenderDecision reset() {
        RenderDecision decision = lastSentContent == null
                ? RenderDecision.none()
                : RenderDecision.clear();
        focused = false;
        lastSentContent = null;
        lastSentAt = null;
        lastObservedAt = null;
        return decision;
    }

    private void ensureMonotonic(Instant now) {
        if (lastObservedAt != null && now.isBefore(lastObservedAt)) {
            throw new IllegalArgumentException("observation timestamps must be monotonic");
        }
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
        return value;
    }
}
