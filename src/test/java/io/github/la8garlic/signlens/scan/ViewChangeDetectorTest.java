package io.github.la8garlic.signlens.scan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.la8garlic.signlens.session.ViewSample;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ViewChangeDetectorTest {

    private static final UUID WORLD = UUID.randomUUID();

    @Test
    void unchangedViewSkipsUntilIdleProbe() {
        ViewChangeDetector detector = new ViewChangeDetector(0.02, 1.0f, 10);
        ViewSample view = new ViewSample(WORLD, 1, 2, 3, 0, 0);

        assertTrue(detector.shouldTrace(view, 2));
        detector.recordTrace(view);
        assertFalse(detector.shouldTrace(view, 2));
        assertFalse(detector.shouldTrace(view, 2));
        assertFalse(detector.shouldTrace(view, 2));
        assertFalse(detector.shouldTrace(view, 2));
        assertTrue(detector.shouldTrace(view, 2));
        assertTrue(detector.lastDecisionWasIdleProbe());
    }

    @Test
    void meaningfulPositionRotationAndWorldChangesTriggerDetection() {
        ViewChangeDetector detector = new ViewChangeDetector(0.02, 1.0f, 10);
        ViewSample base = new ViewSample(WORLD, 1, 2, 3, 0, 0);
        detector.recordTrace(base);

        assertTrue(detector.shouldTrace(new ViewSample(WORLD, 1.03, 2, 3, 0, 0), 2));
        detector.recordTrace(base);
        assertTrue(detector.shouldTrace(new ViewSample(WORLD, 1, 2, 3, 2, 0), 2));
        detector.recordTrace(base);
        assertTrue(detector.shouldTrace(new ViewSample(UUID.randomUUID(), 1, 2, 3, 0, 0), 2));
    }

    @Test
    void angleWrapAroundDoesNotLookLikeAFullTurn() {
        ViewChangeDetector detector = new ViewChangeDetector(0.02, 5.0f, 10);
        ViewSample base = new ViewSample(WORLD, 1, 2, 3, 179, 0);
        detector.recordTrace(base);

        assertFalse(detector.shouldTrace(new ViewSample(WORLD, 1, 2, 3, -179, 0), 2));
    }

    @Test
    void rejectsInvalidThresholdsAndElapsedTicks() {
        assertThrows(IllegalArgumentException.class, () -> new ViewChangeDetector(-1, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> new ViewChangeDetector(1, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> new ViewChangeDetector(1, 1, 0));

        ViewChangeDetector detector = new ViewChangeDetector(1, 1, 10);
        assertThrows(IllegalArgumentException.class,
                () -> detector.shouldTrace(new ViewSample(WORLD, 1, 2, 3, 0, 0), 0));
    }
}
