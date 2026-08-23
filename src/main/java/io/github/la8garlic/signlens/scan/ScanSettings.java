package io.github.la8garlic.signlens.scan;

/** Validated runtime settings for one player's entity-owned scan task. */
public record ScanSettings(
        double maxDistance,
        int scanPeriodTicks,
        int idleProbeTicks,
        double positionThreshold,
        float rotationThresholdDegrees
) {

    public ScanSettings {
        if (!Double.isFinite(maxDistance) || maxDistance <= 0.0) {
            throw new IllegalArgumentException("maxDistance must be finite and greater than zero");
        }
        if (scanPeriodTicks <= 0 || idleProbeTicks <= 0) {
            throw new IllegalArgumentException("scan periods must be greater than zero");
        }
        if (!Double.isFinite(positionThreshold) || positionThreshold < 0.0) {
            throw new IllegalArgumentException("positionThreshold must be finite and non-negative");
        }
        if (!Float.isFinite(rotationThresholdDegrees) || rotationThresholdDegrees < 0.0f) {
            throw new IllegalArgumentException("rotationThresholdDegrees must be finite and non-negative");
        }
    }
}
