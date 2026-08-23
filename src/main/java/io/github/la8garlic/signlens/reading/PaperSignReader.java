package io.github.la8garlic.signlens.reading;

import io.github.la8garlic.signlens.detection.DetectedSign;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

/** Paper implementation of {@link SignReader}. */
public final class PaperSignReader implements SignReader {

    @Override
    public Optional<SignSnapshot> read(Player viewer, DetectedSign detectedSign) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(detectedSign, "detectedSign");

        if (!viewer.getWorld().getUID().equals(detectedSign.worldId())) {
            return Optional.empty();
        }

        try {
            Block block = viewer.getWorld().getBlockAt(detectedSign.x(), detectedSign.y(), detectedSign.z());
            if (!(block.getState() instanceof Sign sign)) {
                return Optional.empty();
            }

            Side side = sign.getInteractableSideFor(viewer);
            SignSide signSide = sign.getSide(side);
            SignContent content = new SignContent(
                    signSide.lines(),
                    signSide.getColor(),
                    signSide.isGlowingText()
            );
            SignKey key = new SignKey(
                    detectedSign.worldId(),
                    detectedSign.x(),
                    detectedSign.y(),
                    detectedSign.z(),
                    side
            );
            return Optional.of(new SignSnapshot(key, content));
        } catch (RuntimeException unavailableSign) {
            return Optional.empty();
        }
    }
}
