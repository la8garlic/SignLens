package io.github.la8garlic.signlens.focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FocusControllerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final FocusTarget FIRST = new FocusTarget(UUID.randomUUID(), 1, 64, 1);
    private static final FocusTarget SECOND = new FocusTarget(FIRST.worldId(), 2, 64, 1);

    @Test
    void shortHitRemainsCandidateWithoutStartingFocus() {
        FocusController controller = controller(Duration.ofMillis(200), Duration.ofMillis(300));

        FocusTransition first = controller.observe(hit(FIRST), at(0));
        FocusTransition beforeDwell = controller.observe(hit(FIRST), at(199));

        assertEquals(FocusState.CANDIDATE, first.currentState());
        assertEquals(FocusState.CANDIDATE, beforeDwell.currentState());
        assertEquals(FocusTransitionType.NONE, beforeDwell.type());
        assertTrue(controller.candidate().isPresent());
    }

    @Test
    void dwellStartsFocusExactlyOnce() {
        FocusController controller = controller(Duration.ofMillis(200), Duration.ofMillis(300));

        controller.observe(hit(FIRST), at(0));
        FocusTransition started = controller.observe(hit(FIRST), at(200));
        FocusTransition repeated = controller.observe(hit(FIRST), at(400));

        assertTrue(started.focusStarted());
        assertEquals(FocusState.FOCUSED, started.currentState());
        assertEquals(FIRST, started.currentTarget().orElseThrow());
        assertEquals(FocusTransitionType.NONE, repeated.type());
        assertEquals(FocusState.FOCUSED, repeated.currentState());
    }

    @Test
    void missEntersGraceThenEndsFocusAfterGrace() {
        FocusController controller = controller(Duration.ofMillis(200), Duration.ofMillis(300));

        controller.observe(hit(FIRST), at(0));
        controller.observe(hit(FIRST), at(200));
        FocusTransition grace = controller.observe(miss(), at(250));
        FocusTransition stillGrace = controller.observe(miss(), at(349));
        FocusTransition ended = controller.observe(miss(), at(550));

        assertEquals(FocusState.LOST_GRACE, grace.currentState());
        assertEquals(FocusTransitionType.NONE, grace.type());
        assertEquals(FocusState.LOST_GRACE, stillGrace.currentState());
        assertTrue(ended.focusEnded());
        assertEquals(FocusState.IDLE, ended.currentState());
        assertEquals(FIRST, ended.previousTarget().orElseThrow());
    }

    @Test
    void sameTargetDuringGraceRecoversWithoutDuplicateStart() {
        FocusController controller = controller(Duration.ofMillis(200), Duration.ofMillis(100));

        controller.observe(hit(FIRST), at(0));
        controller.observe(hit(FIRST), at(200));
        controller.observe(miss(), at(250));
        FocusTransition recovered = controller.observe(hit(FIRST), at(300));

        assertEquals(FocusState.FOCUSED, recovered.currentState());
        assertEquals(FocusTransitionType.NONE, recovered.type());
    }

    @Test
    void switchingCandidateResetsDwellClock() {
        FocusController controller = controller(Duration.ofMillis(200), Duration.ofMillis(300));

        controller.observe(hit(FIRST), at(0));
        FocusTransition switched = controller.observe(hit(SECOND), at(150));
        FocusTransition beforeSecondDwell = controller.observe(hit(SECOND), at(349));
        FocusTransition started = controller.observe(hit(SECOND), at(350));

        assertEquals(FocusState.CANDIDATE, switched.currentState());
        assertEquals(SECOND, switched.currentTarget().orElseThrow());
        assertEquals(FocusState.CANDIDATE, beforeSecondDwell.currentState());
        assertTrue(started.focusStarted());
        assertEquals(SECOND, started.currentTarget().orElseThrow());
    }

    @Test
    void changingTargetWhileFocusedEndsOldFocusAndStartsNewCandidate() {
        FocusController controller = controller(Duration.ofMillis(100), Duration.ofMillis(100));

        controller.observe(hit(FIRST), at(0));
        controller.observe(hit(FIRST), at(100));
        FocusTransition changed = controller.observe(hit(SECOND), at(101));

        assertTrue(changed.focusEnded());
        assertEquals(FocusState.CANDIDATE, changed.currentState());
        assertEquals(FIRST, changed.previousTarget().orElseThrow());
        assertEquals(SECOND, changed.currentTarget().orElseThrow());
    }

    @Test
    void resetEndsFocusAndAllowsAnewTimestampTimeline() {
        FocusController controller = controller(Duration.ofMillis(100), Duration.ofMillis(100));

        controller.observe(hit(FIRST), at(0));
        controller.observe(hit(FIRST), at(100));
        FocusTransition reset = controller.reset();
        FocusTransition afterReset = controller.observe(hit(SECOND), at(0));

        assertTrue(reset.focusEnded());
        assertEquals(FocusState.IDLE, reset.currentState());
        assertEquals(FocusState.CANDIDATE, afterReset.currentState());
    }

    @Test
    void rejectsInvalidDurationsAndNonMonotonicTimestamps() {
        assertThrows(IllegalArgumentException.class,
                () -> new FocusController(Duration.ZERO, Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class,
                () -> new FocusController(Duration.ofMillis(100), Duration.ofMillis(-1)));

        FocusController controller = controller(Duration.ofMillis(100), Duration.ofMillis(100));
        controller.observe(hit(FIRST), at(100));
        assertThrows(IllegalArgumentException.class, () -> controller.observe(hit(FIRST), at(99)));
    }

    private static FocusController controller(Duration dwell, Duration grace) {
        return new FocusController(dwell, grace);
    }

    private static Optional<FocusTarget> hit(FocusTarget target) {
        return Optional.of(target);
    }

    private static Optional<FocusTarget> miss() {
        return Optional.empty();
    }

    private static Instant at(long milliseconds) {
        return START.plusMillis(milliseconds);
    }
}
