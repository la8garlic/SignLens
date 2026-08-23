package io.github.la8garlic.signlens.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class RenderPolicyTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Component FIRST = Component.text("first");
    private static final Component SECOND = Component.text("second");

    @Test
    void focusEntrySendsOnceAndStableFocusDoesNotResendEachTick() {
        RenderPolicy policy = new RenderPolicy(Duration.ofMillis(2500));

        assertEquals(RenderDecisionType.SHOW, policy.observe(show(FIRST), true, at(0)).type());
        assertEquals(RenderDecisionType.NONE, policy.observe(show(FIRST), true, at(50)).type());
        assertEquals(RenderDecisionType.NONE, policy.observe(show(FIRST), true, at(100)).type());
        assertEquals(RenderDecisionType.NONE, policy.observe(show(FIRST), true, at(2499)).type());
        assertEquals(RenderDecisionType.SHOW, policy.observe(show(FIRST), true, at(2500)).type());
    }

    @Test
    void contentChangeSendsNewContentOnce() {
        RenderPolicy policy = new RenderPolicy();

        policy.observe(show(FIRST), true, at(0));
        RenderDecision changed = policy.observe(show(SECOND), true, at(100));
        RenderDecision repeated = policy.observe(show(SECOND), true, at(101));

        assertEquals(RenderDecisionType.SHOW, changed.type());
        assertEquals(SECOND, changed.content().orElseThrow());
        assertEquals(RenderDecisionType.NONE, repeated.type());
    }

    @Test
    void emptyContentNeverShowsAndClearsExistingContentOnce() {
        RenderPolicy policy = new RenderPolicy();

        policy.observe(show(FIRST), true, at(0));
        assertEquals(RenderDecisionType.CLEAR, policy.observe(Optional.empty(), true, at(100)).type());
        assertEquals(RenderDecisionType.NONE, policy.observe(Optional.empty(), true, at(101)).type());
        assertEquals(RenderDecisionType.SHOW, policy.observe(show(SECOND), true, at(102)).type());
    }

    @Test
    void focusEndClearsOnceAndOnlyWhilePreviouslyRendered() {
        RenderPolicy policy = new RenderPolicy();

        policy.observe(show(FIRST), true, at(0));
        assertEquals(RenderDecisionType.CLEAR, policy.observe(Optional.empty(), false, at(100)).type());
        assertEquals(RenderDecisionType.NONE, policy.observe(Optional.empty(), false, at(101)).type());

        RenderPolicy emptyFocus = new RenderPolicy();
        emptyFocus.observe(Optional.empty(), true, at(0));
        assertEquals(RenderDecisionType.NONE, emptyFocus.observe(Optional.empty(), false, at(100)).type());
    }

    @Test
    void resetClearsOnceAndRestartsTimeline() {
        RenderPolicy policy = new RenderPolicy(Duration.ofMillis(100));

        policy.observe(show(FIRST), true, at(0));
        assertEquals(RenderDecisionType.CLEAR, policy.reset().type());
        assertEquals(RenderDecisionType.NONE, policy.reset().type());
        assertEquals(RenderDecisionType.SHOW, policy.observe(show(SECOND), true, at(0)).type());
    }

    @Test
    void rejectsInvalidDurationsAndNonMonotonicObservations() {
        assertThrows(IllegalArgumentException.class, () -> new RenderPolicy(Duration.ZERO));

        RenderPolicy policy = new RenderPolicy();
        policy.observe(show(FIRST), true, at(100));
        assertThrows(IllegalArgumentException.class,
                () -> policy.observe(show(FIRST), true, at(99)));
    }

    private static Optional<Component> show(Component component) {
        return Optional.of(component);
    }

    private static Instant at(long milliseconds) {
        return START.plusMillis(milliseconds);
    }
}
