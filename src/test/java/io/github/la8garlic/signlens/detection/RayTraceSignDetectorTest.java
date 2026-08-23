package io.github.la8garlic.signlens.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.bukkit.FluidCollisionMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.junit.jupiter.api.Test;

class RayTraceSignDetectorTest {

    private static final double MAX_DISTANCE = 8.0;

    private final RayTraceSignDetector detector = new RayTraceSignDetector(MAX_DISTANCE);

    @Test
    void missReturnsEmpty() {
        Player player = mock(Player.class);
        when(player.rayTraceBlocks(MAX_DISTANCE, FluidCollisionMode.NEVER)).thenReturn(null);

        assertTrue(detector.detect(player).isEmpty());
        verify(player).rayTraceBlocks(MAX_DISTANCE, FluidCollisionMode.NEVER);
    }

    @Test
    void nonSignHitReturnsEmptyWithoutNearbyBlockScan() {
        Player player = mock(Player.class);
        RayTraceResult result = mock(RayTraceResult.class);
        Block block = mock(Block.class);

        when(player.rayTraceBlocks(MAX_DISTANCE, FluidCollisionMode.NEVER)).thenReturn(result);
        when(result.getHitBlock()).thenReturn(block);

        assertTrue(detector.detect(player).isEmpty());
        verify(player).rayTraceBlocks(MAX_DISTANCE, FluidCollisionMode.NEVER);
        verify(block).getState();
        verify(block, never()).getWorld();
    }

    @Test
    void signHitReturnsImmutableDetectionCoordinates() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        RayTraceResult result = mock(RayTraceResult.class);
        Block block = mock(Block.class);
        Sign sign = mock(Sign.class);
        UUID worldId = UUID.randomUUID();

        when(player.rayTraceBlocks(MAX_DISTANCE, FluidCollisionMode.NEVER)).thenReturn(result);
        when(result.getHitBlock()).thenReturn(block);
        when(result.getHitBlockFace()).thenReturn(BlockFace.NORTH);
        when(block.getState()).thenReturn(sign);
        when(block.getX()).thenReturn(128);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(-91);
        when(player.getWorld()).thenReturn(world);
        when(world.getUID()).thenReturn(worldId);

        var detected = detector.detect(player);

        assertFalse(detected.isEmpty());
        assertEquals(new DetectedSign(worldId, 128, 64, -91, BlockFace.NORTH), detected.orElseThrow());
    }

    @Test
    void rejectsInvalidMaximumDistance() {
        assertThrows(IllegalArgumentException.class, () -> new RayTraceSignDetector(0.0));
        assertThrows(IllegalArgumentException.class, () -> new RayTraceSignDetector(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new RayTraceSignDetector(Double.POSITIVE_INFINITY));
    }
}
