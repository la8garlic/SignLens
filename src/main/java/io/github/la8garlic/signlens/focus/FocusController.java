package io.github.la8garlic.signlens.focus;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure, scheduler-free state machine for sign focus.
 *
 * <p>Callers provide a timestamp for every hit or miss. The controller does
 * not schedule a timeout; a later miss observation advances lost grace.</p>
 */
public final class FocusController {

    public static final Duration DEFAULT_DWELL = Duration.ofMillis(200);
    public static final Duration DEFAULT_LOST_GRACE = Duration.ofMillis(300);

    private final Duration dwell;
    private final Duration lostGrace;

    private FocusState state = FocusState.IDLE;
    private FocusTarget candidate;
    private Instant candidateSince;
    private FocusTarget focused;
    private Instant focusedSince;
    private Instant lostSince;
    private Instant lastObservedAt;

    public FocusController() {
        this(DEFAULT_DWELL, DEFAULT_LOST_GRACE);
    }

    public FocusController(Duration dwell, Duration lostGrace) {
        this.dwell = requirePositive(dwell, "dwell");
        this.lostGrace = requirePositive(lostGrace, "lostGrace");
    }

    public Duration dwell() {
        return dwell;
    }

    public Duration lostGrace() {
        return lostGrace;
    }

    public FocusState state() {
        return state;
    }

    public Optional<FocusTarget> candidate() {
        return Optional.ofNullable(candidate);
    }

    public Optional<FocusTarget> focused() {
        return Optional.ofNullable(focused);
    }

    public Optional<FocusTarget> currentTarget() {
        return activeTarget();
    }

    public Optional<Instant> activeSince() {
        return switch (state) {
            case CANDIDATE -> Optional.ofNullable(candidateSince);
            case FOCUSED, LOST_GRACE -> Optional.ofNullable(focusedSince);
            case IDLE -> Optional.empty();
        };
    }

    /**
     * Applies one hit or miss observation at {@code now}.
     *
     * @param observation the detected block, or empty for a miss
     * @param now a monotonic observation timestamp
     * @return the state and edge transition caused by the observation
     */
    public FocusTransition observe(Optional<FocusTarget> observation, Instant now) {
        Objects.requireNonNull(observation, "observation");
        Objects.requireNonNull(now, "now");
        ensureMonotonic(now);

        FocusState previousState = state;
        Optional<FocusTarget> previousTarget = activeTarget();
        FocusTransitionType type = FocusTransitionType.NONE;

        if (observation.isPresent()) {
            FocusTarget target = observation.orElseThrow();
            switch (state) {
                case IDLE -> {
                    candidate = target;
                    candidateSince = now;
                    state = FocusState.CANDIDATE;
                }
                case CANDIDATE -> {
                    if (!target.equals(candidate)) {
                        candidate = target;
                        candidateSince = now;
                    } else if (!now.isBefore(candidateSince.plus(dwell))) {
                        focused = target;
                        focusedSince = now;
                        candidate = null;
                        candidateSince = null;
                        state = FocusState.FOCUSED;
                        type = FocusTransitionType.FOCUS_STARTED;
                    }
                }
                case FOCUSED -> {
                    if (!target.equals(focused)) {
                        type = FocusTransitionType.FOCUS_ENDED;
                        focused = null;
                        focusedSince = null;
                        candidate = target;
                        candidateSince = now;
                        state = FocusState.CANDIDATE;
                    }
                }
                case LOST_GRACE -> {
                    if (target.equals(focused)) {
                        lostSince = null;
                        state = FocusState.FOCUSED;
                    } else {
                        type = FocusTransitionType.FOCUS_ENDED;
                        focused = null;
                        focusedSince = null;
                        candidate = target;
                        candidateSince = now;
                        lostSince = null;
                        state = FocusState.CANDIDATE;
                    }
                }
            }
        } else {
            switch (state) {
                case IDLE -> {
                    // Nothing to clear.
                }
                case CANDIDATE -> {
                    candidate = null;
                    candidateSince = null;
                    state = FocusState.IDLE;
                }
                case FOCUSED -> {
                    lostSince = now;
                    state = FocusState.LOST_GRACE;
                }
                case LOST_GRACE -> {
                    if (!now.isBefore(lostSince.plus(lostGrace))) {
                        type = FocusTransitionType.FOCUS_ENDED;
                        focused = null;
                        focusedSince = null;
                        lostSince = null;
                        state = FocusState.IDLE;
                    }
                }
            }
        }

        lastObservedAt = now;
        return new FocusTransition(previousState, state, previousTarget, activeTarget(), type);
    }

    /**
     * Resets this controller after a lifecycle invalidation such as teleport,
     * world change, quit, or session disposal.
     */
    public FocusTransition reset() {
        FocusState previousState = state;
        Optional<FocusTarget> previousTarget = activeTarget();
        boolean hadFocus = state == FocusState.FOCUSED || state == FocusState.LOST_GRACE;

        state = FocusState.IDLE;
        candidate = null;
        candidateSince = null;
        focused = null;
        focusedSince = null;
        lostSince = null;
        lastObservedAt = null;

        return new FocusTransition(
                previousState,
                FocusState.IDLE,
                previousTarget,
                Optional.empty(),
                hadFocus ? FocusTransitionType.FOCUS_ENDED : FocusTransitionType.NONE
        );
    }

    private Optional<FocusTarget> activeTarget() {
        return switch (state) {
            case CANDIDATE -> Optional.ofNullable(candidate);
            case FOCUSED, LOST_GRACE -> Optional.ofNullable(focused);
            case IDLE -> Optional.empty();
        };
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
