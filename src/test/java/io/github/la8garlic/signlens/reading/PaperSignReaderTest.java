package io.github.la8garlic.signlens.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.la8garlic.signlens.detection.DetectedSign;
import io.github.la8garlic.signlens.metrics.PerformanceCounters;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class PaperSignReaderTest {

    private final PaperSignReader reader = new PaperSignReader();

    @Test
    void readsViewerFacingFrontAndPreservesComponents() {
        UUID worldId = UUID.randomUUID();
        Player player = mockPlayer(worldId);
        Sign sign = mockSign(player, Side.FRONT);
        Component colored = Component.text("Front", NamedTextColor.RED);
        SignSide front = mock(SignSide.class);
        when(front.lines()).thenReturn(List.of(colored, Component.text("second")));
        when(front.getColor()).thenReturn(DyeColor.RED);
        when(front.isGlowingText()).thenReturn(true);
        when(sign.getSide(Side.FRONT)).thenReturn(front);

        var snapshot = reader.read(player, detectedSign(worldId)).orElseThrow();

        assertEquals(new SignKey(worldId, 4, 65, -2, Side.FRONT), snapshot.key());
        assertEquals(List.of(colored, Component.text("second")), snapshot.content().lines());
        assertSame(colored, snapshot.content().lines().get(0));
        assertEquals(DyeColor.RED, snapshot.content().color());
        assertTrue(snapshot.content().glowingText());
        assertTrue(snapshot.renderable());
        verify(sign).getInteractableSideFor(player);
        verify(sign).getSide(Side.FRONT);
    }

    @Test
    void readsBackSideAsASeparateKey() {
        UUID worldId = UUID.randomUUID();
        Player player = mockPlayer(worldId);
        Sign sign = mockSign(player, Side.BACK);
        SignSide back = mock(SignSide.class);
        when(back.lines()).thenReturn(List.of(Component.text("Back")));
        when(back.getColor()).thenReturn(DyeColor.BLUE);
        when(back.isGlowingText()).thenReturn(false);
        when(sign.getSide(Side.BACK)).thenReturn(back);

        var snapshot = reader.read(player, detectedSign(worldId)).orElseThrow();

        assertEquals(Side.BACK, snapshot.key().side());
        assertEquals(Component.text("Back"), snapshot.content().lines().get(0));
        assertEquals(DyeColor.BLUE, snapshot.content().color());
        assertFalse(snapshot.content().glowingText());
    }

    @Test
    void whitespaceOnlyContentIsNotRenderable() {
        UUID worldId = UUID.randomUUID();
        Player player = mockPlayer(worldId);
        Sign sign = mockSign(player, Side.FRONT);
        SignSide front = mock(SignSide.class);
        when(front.lines()).thenReturn(List.of(
                Component.text("  "),
                Component.empty(),
                Component.text("\n\t")
        ));
        when(front.getColor()).thenReturn(DyeColor.BLACK);
        when(front.isGlowingText()).thenReturn(false);
        when(sign.getSide(Side.FRONT)).thenReturn(front);

        var snapshot = reader.read(player, detectedSign(worldId)).orElseThrow();

        assertFalse(snapshot.renderable());
    }

    @Test
    void snapshotLinesCannotBeMutated() {
        SignContent content = new SignContent(List.of(Component.text("safe")), DyeColor.WHITE, false);

        assertThrows(UnsupportedOperationException.class, () -> content.lines().add(Component.text("nope")));
    }

    @Test
    void unavailableOrNonSignBlockReturnsEmpty() {
        UUID worldId = UUID.randomUUID();
        Player player = mockPlayer(worldId);
        Block block = mock(Block.class);
        when(player.getWorld().getBlockAt(4, 65, -2)).thenReturn(block);

        assertTrue(reader.read(player, detectedSign(worldId)).isEmpty());
        verify(block).getState();
    }

    @Test
    void unavailableSignStateReturnsEmpty() {
        UUID worldId = UUID.randomUUID();
        Player player = mockPlayer(worldId);
        Sign sign = mockSign(player, Side.FRONT);
        when(sign.getInteractableSideFor(player)).thenThrow(new IllegalStateException("state unavailable"));

        assertTrue(reader.read(player, detectedSign(worldId)).isEmpty());
    }

    @Test
    void expectedUnavailableStateIsCountedWithoutReportingAnError() {
        UUID worldId = UUID.randomUUID();
        Player player = mockPlayer(worldId);
        Sign sign = mockSign(player, Side.FRONT);
        when(sign.getInteractableSideFor(player)).thenThrow(new IllegalArgumentException("state unavailable"));
        PerformanceCounters counters = new PerformanceCounters();
        List<RuntimeException> reports = new ArrayList<>();
        PaperSignReader reader = new PaperSignReader(
                counters,
                (ignoredSign, failure) -> reports.add(failure),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)
        );

        assertTrue(reader.read(player, detectedSign(worldId)).isEmpty());

        assertEquals(1L, counters.snapshot().signReadUnavailable());
        assertEquals(0L, counters.snapshot().signReadFailures());
        assertTrue(reports.isEmpty());
    }

    @Test
    void unexpectedReaderFailureIsCountedAndReported() {
        UUID worldId = UUID.randomUUID();
        Player player = mockPlayer(worldId);
        Sign sign = mockSign(player, Side.FRONT);
        NullPointerException failure = new NullPointerException("broken side");
        when(sign.getSide(Side.FRONT)).thenThrow(failure);
        PerformanceCounters counters = new PerformanceCounters();
        List<RuntimeException> reports = new ArrayList<>();
        PaperSignReader reader = new PaperSignReader(
                counters,
                (ignoredSign, reportedFailure) -> reports.add(reportedFailure),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)
        );

        assertTrue(reader.read(player, detectedSign(worldId)).isEmpty());

        assertEquals(0L, counters.snapshot().signReadUnavailable());
        assertEquals(1L, counters.snapshot().signReadFailures());
        assertEquals(List.of(failure), reports);
    }

    @Test
    void unexpectedReaderReportsAreRateLimited() {
        UUID worldId = UUID.randomUUID();
        Player player = mockPlayer(worldId);
        Sign sign = mockSign(player, Side.FRONT);
        when(sign.getSide(Side.FRONT)).thenThrow(new NullPointerException("broken side"));
        List<RuntimeException> reports = new ArrayList<>();
        PaperSignReader reader = new PaperSignReader(
                new PerformanceCounters(),
                (ignoredSign, failure) -> reports.add(failure),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)
        );

        reader.read(player, detectedSign(worldId));
        reader.read(player, detectedSign(worldId));

        assertEquals(1, reports.size());
    }

    @Test
    void differentWorldReturnsEmptyWithoutLookingUpASecondBlock() {
        UUID playerWorldId = UUID.randomUUID();
        Player player = mockPlayer(playerWorldId);

        assertTrue(reader.read(player, detectedSign(UUID.randomUUID())).isEmpty());
        verify(player.getWorld(), never()).getBlockAt(4, 65, -2);
    }

    private static Player mockPlayer(UUID worldId) {
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldId);
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        return player;
    }

    private static Sign mockSign(Player player, Side side) {
        Block block = mock(Block.class);
        Sign sign = mock(Sign.class);
        when(player.getWorld().getBlockAt(4, 65, -2)).thenReturn(block);
        when(block.getState()).thenReturn(sign);
        when(sign.getInteractableSideFor(player)).thenReturn(side);
        return sign;
    }

    private static DetectedSign detectedSign(UUID worldId) {
        return new DetectedSign(worldId, 4, 65, -2, BlockFace.NORTH);
    }
}
