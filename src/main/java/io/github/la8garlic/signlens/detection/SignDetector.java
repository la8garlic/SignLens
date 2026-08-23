package io.github.la8garlic.signlens.detection;

import java.util.Optional;
import org.bukkit.entity.Player;

/** Detects the sign currently under a player's view ray. */
public interface SignDetector {

    Optional<DetectedSign> detect(Player player);
}
