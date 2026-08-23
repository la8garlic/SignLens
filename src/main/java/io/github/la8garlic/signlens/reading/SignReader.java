package io.github.la8garlic.signlens.reading;

import java.util.Optional;
import org.bukkit.entity.Player;
import io.github.la8garlic.signlens.detection.DetectedSign;

/** Reads immutable content from the side of a detected sign facing a player. */
public interface SignReader {

    Optional<SignSnapshot> read(Player viewer, DetectedSign detectedSign);
}
