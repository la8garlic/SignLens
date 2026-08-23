package io.github.la8garlic.signlens.scan;

import io.github.la8garlic.signlens.session.ViewSample;
import java.util.Objects;
import java.util.Optional;

/** Pure threshold and idle-probe decision maker for view-driven scanning. */
public final class ViewChangeDetector {

    private final double positionThreshold;
    private final float rotationThresholdDegrees;
    private final int idleProbeTicks;
    private ViewSample lastTracedView;
    private int ticksSinceTrace;
    private boolean lastDecisionIdleProbe;

    public ViewChangeDetector(double positionThreshold, float rotationThresholdDegrees, int idleProbeTicks) {
        if (!Double.isFinite(positionThreshold) || positionThreshold < 0.0) {
            throw new IllegalArgumentException("positionThreshold must be finite and non-negative");
        }
        if (!Float.isFinite(rotationThresholdDegrees) || rotationThresholdDegrees < 0.0f) {
            throw new IllegalArgumentException("rotationThresholdDegrees must be finite and non-negative");
        }
        if (idleProbeTicks <= 0) {
            throw new IllegalArgumentException("idleProbeTicks must be greater than zero");
        }
        this.positionThreshold = positionThreshold;
        this.rotationThresholdDegrees = rotationThresholdDegrees;
        this.idleProbeTicks = idleProbeTicks;
    }

    public boolean shouldTrace(ViewSample current, int elapsedTicks) {
        Objects.requireNonNull(current, "current");
        if (elapsedTicks <= 0) {
            throw new IllegalArgumentException("elapsedTicks must be greater than zero");
        }
        if (lastTracedView == null || meaningfullyChanged(lastTracedView, current)) {
            lastDecisionIdleProbe = false;
            return true;
        }
        ticksSinceTrace = Math.min(idleProbeTicks, ticksSinceTrace + elapsedTicks);
        lastDecisionIdleProbe = ticksSinceTrace >= idleProbeTicks;
        return lastDecisionIdleProbe;
    }

    public void recordTrace(ViewSample view) {
        lastTracedView = Objects.requireNonNull(view, "view");
        ticksSinceTrace = 0;
    }

    public void reset() {
        lastTracedView = null;
        ticksSinceTrace = 0;
        lastDecisionIdleProbe = false;
    }

    public boolean lastDecisionWasIdleProbe() {
        return lastDecisionIdleProbe;
    }

    public Optional<ViewSample> lastTracedView() {
        return Optional.ofNullable(lastTracedView);
    }

    private boolean meaningfullyChanged(ViewSample previous, ViewSample current) {
        if (!previous.worldId().equals(current.worldId())) {
            return true;
        }
        double dx = current.x() - previous.x();
        double dy = current.y() - previous.y();
        double dz = current.z() - previous.z();
        double thresholdSquared = positionThreshold * positionThreshold;
        if (dx * dx + dy * dy + dz * dz > thresholdSquared) {
            return true;
        }
        return angleDifference(previous.yaw(), current.yaw()) > rotationThresholdDegrees
                || Math.abs(current.pitch() - previous.pitch()) > rotationThresholdDegrees;
    }

    private static float angleDifference(float first, float second) {
        float delta = Math.abs(first - second) % 360.0f;
        return delta > 180.0f ? 360.0f - delta : delta;
    }
}
