package io.github.la8garlic.signlens.focus;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable block identity used before the reader resolves a front or back side.
 * Hit face and live Bukkit objects deliberately do not participate in identity.
 */
public record FocusTarget(UUID worldId, int x, int y, int z) {

    public FocusTarget {
        Objects.requireNonNull(worldId, "worldId");
    }
}
