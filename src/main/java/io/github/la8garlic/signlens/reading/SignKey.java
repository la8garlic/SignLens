package io.github.la8garlic.signlens.reading;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.block.sign.Side;

/** Stable identity of one side of one sign block. */
public record SignKey(
        UUID worldId,
        int x,
        int y,
        int z,
        Side side
) {

    public SignKey {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(side, "side");
    }
}
