package io.github.la8garlic.signlens.focus;

/** Lifecycle state of one player's current sign focus. */
public enum FocusState {
    IDLE,
    CANDIDATE,
    FOCUSED,
    LOST_GRACE
}
