package io.github.la8garlic.signlens.detection;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

/**
 * Detects signs using the server's precise block ray-trace API.
 *
 * <p>The detector does not read sign content, track focus, schedule work, or
 * render output. It also never scans nearby blocks.</p>
 */
public final class RayTraceSignDetector implements SignDetector {

    private final double maxDistance;
    private final FluidCollisionMode fluidCollisionMode;

    public RayTraceSignDetector(double maxDistance) {
        this(maxDistance, FluidCollisionMode.NEVER);
    }

    public RayTraceSignDetector(double maxDistance, FluidCollisionMode fluidCollisionMode) {
        if (!Double.isFinite(maxDistance) || maxDistance <= 0.0) {
            throw new IllegalArgumentException("maxDistance must be finite and greater than zero");
        }
        this.maxDistance = maxDistance;
        this.fluidCollisionMode = Objects.requireNonNull(fluidCollisionMode, "fluidCollisionMode");
    }

    public double maxDistance() {
        return maxDistance;
    }

    public FluidCollisionMode fluidCollisionMode() {
        return fluidCollisionMode;
    }

    @Override
    public Optional<DetectedSign> detect(Player player) {
        Objects.requireNonNull(player, "player");

        RayTraceResult result = player.rayTraceBlocks(maxDistance, fluidCollisionMode);
        if (result == null) {
            return Optional.empty();
        }

        Block hitBlock = result.getHitBlock();
        if (hitBlock == null || !(hitBlock.getState() instanceof Sign)) {
            return Optional.empty();
        }

        return Optional.of(new DetectedSign(
                player.getWorld().getUID(),
                hitBlock.getX(),
                hitBlock.getY(),
                hitBlock.getZ(),
                result.getHitBlockFace()
        ));
    }
}
