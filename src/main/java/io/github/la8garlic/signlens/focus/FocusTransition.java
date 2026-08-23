package io.github.la8garlic.signlens.focus;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of one timestamped focus observation.
 *
 * <p>The state may change without an edge event, for example when a focused
 * target enters lost grace. Consumers should use {@link #type()} for work
 * such as showing or clearing rendered content.</p>
 */
public record FocusTransition(
        FocusState previousState,
        FocusState currentState,
        Optional<FocusTarget> previousTarget,
        Optional<FocusTarget> currentTarget,
        FocusTransitionType type
) {

    public FocusTransition {
        Objects.requireNonNull(previousState, "previousState");
        Objects.requireNonNull(currentState, "currentState");
        Objects.requireNonNull(previousTarget, "previousTarget");
        Objects.requireNonNull(currentTarget, "currentTarget");
        Objects.requireNonNull(type, "type");
    }

    public boolean focusStarted() {
        return type == FocusTransitionType.FOCUS_STARTED;
    }

    public boolean focusEnded() {
        return type == FocusTransitionType.FOCUS_ENDED;
    }
}
