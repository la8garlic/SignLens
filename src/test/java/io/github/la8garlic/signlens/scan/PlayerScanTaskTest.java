package io.github.la8garlic.signlens.scan;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.la8garlic.signlens.detection.DetectedSign;
import io.github.la8garlic.signlens.detection.SignDetector;
import io.github.la8garlic.signlens.focus.FocusController;
import io.github.la8garlic.signlens.reading.SignContent;
import io.github.la8garlic.signlens.reading.SignKey;
import io.github.la8garlic.signlens.reading.SignReader;
import io.github.la8garlic.signlens.reading.SignSnapshot;
import io.github.la8garlic.signlens.render.ContentFormatter;
import io.github.la8garlic.signlens.render.FormattedContent;
import io.github.la8garlic.signlens.render.SignRenderer;
import io.github.la8garlic.signlens.session.PlayerSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class PlayerScanTaskTest {

    private static final UUID WORLD_ID = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void pipelineFocusesThenRendersAndSkipsUnchangedViewTicks() {
        Plugin plugin = mock(Plugin.class);
        Player player = mock(Player.class);
        World world = mock(World.class);
        SignDetector detector = mock(SignDetector.class);
        SignReader reader = mock(SignReader.class);
        SignRenderer renderer = mock(SignRenderer.class);
        MutableClock clock = new MutableClock(START);
        Location[] currentLocation = {new Location(world, 0, 0, 0, 0, 0)};

        when(plugin.isEnabled()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);
        when(player.getWorld()).thenReturn(world);
        when(world.getUID()).thenReturn(WORLD_ID);
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission(PlayerScanTask.USE_PERMISSION)).thenReturn(true);
        when(player.getLocation()).thenAnswer(ignored -> currentLocation[0]);

        DetectedSign detectedSign = new DetectedSign(WORLD_ID, 1, 2, 3, BlockFace.NORTH);
        SignSnapshot snapshot = new SignSnapshot(
                new SignKey(WORLD_ID, 1, 2, 3, Side.FRONT),
                new SignContent(List.of(Component.text("Hello")), DyeColor.WHITE, false)
        );
        when(detector.detect(player)).thenReturn(Optional.of(detectedSign));
        when(reader.read(player, detectedSign)).thenReturn(Optional.of(snapshot));

        PlayerSession session = new PlayerSession(
                PLAYER_ID,
                new FocusController(Duration.ofMillis(100), Duration.ofMillis(100)),
                new io.github.la8garlic.signlens.render.RenderPolicy(Duration.ofMillis(100))
        );
        PlayerScanTask task = new PlayerScanTask(
                plugin,
                player,
                session,
                detector,
                reader,
                new ContentFormatter(96, 120),
                renderer,
                new ScanSettings(8, 2, 10, 0.02, 1.0f),
                () -> true,
                clock
        );

        task.runScan();
        clock.advance(Duration.ofMillis(100));
        currentLocation[0] = new Location(world, 0.1, 0, 0, 0, 0);
        task.runScan();
        task.runScan();

        verify(detector, times(2)).detect(player);
        verify(reader).read(player, detectedSign);
        verify(renderer).show(player, new FormattedContent(List.of(Component.text("Hello"))));
        assertTrue(session.renderPolicy().lastSentContent().isPresent());
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
