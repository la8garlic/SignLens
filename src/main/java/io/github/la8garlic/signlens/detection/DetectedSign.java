package io.github.la8garlic.signlens.detection;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.block.BlockFace;

/**
 * Immutable result of sign detection.
 *
 * <p>This type deliberately contains no live Bukkit {@code Block} or
 * {@code Sign} object. Reading the sign is a separate domain step.</p>
 */
public record DetectedSign(
        UUID worldId,
        int x,
        int y,
        int z,
        BlockFace hitFace
) {

    public DetectedSign {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(hitFace, "hitFace");
    }
}
