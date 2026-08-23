package io.github.la8garlic.signlens.session;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Immutable player view sample used by the adaptive scanner. */
public record ViewSample(
        UUID worldId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    public ViewSample {
        Objects.requireNonNull(worldId, "worldId");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("view coordinates and rotation must be finite");
        }
    }

    public static ViewSample from(Player player) {
        Objects.requireNonNull(player, "player");
        Location location = player.getLocation();
        return new ViewSample(
                player.getWorld().getUID(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }
}
