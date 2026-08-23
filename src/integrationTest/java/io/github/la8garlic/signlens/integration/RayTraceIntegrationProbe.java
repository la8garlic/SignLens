package io.github.la8garlic.signlens.integration;

import io.github.la8garlic.signlens.detection.DetectedSign;
import io.github.la8garlic.signlens.detection.RayTraceSignDetector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Test-only plugin that exercises the detector against a real Paper player.
 */
public final class RayTraceIntegrationProbe extends JavaPlugin implements Listener {

    private static final double MAX_DISTANCE = 8.0;
    private static final List<Material> SIGN_MATERIALS = List.of(
            Material.OAK_SIGN,
            Material.OAK_WALL_SIGN,
            Material.OAK_HANGING_SIGN,
            Material.OAK_WALL_HANGING_SIGN
    );

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> runProbe(player), 20L);
    }

    private void runProbe(Player player) {
        Location origin = new Location(player.getWorld(), 0.5, 300.0, 0.5, 0.0f, 0.0f);
        Location signLocation = new Location(player.getWorld(), 0.0, 301.0, 3.0);
        player.teleport(origin);

        getServer().getScheduler().runTaskLater(this, () -> recordDetection(player, signLocation, 0, new ArrayList<>()), 2L);
    }

    private void recordDetection(Player player, Location signLocation, int index, List<String> results) {
        Material material = SIGN_MATERIALS.get(index);
        Block block = signLocation.getBlock();
        block.setType(material);
        if (block.getBlockData() instanceof Directional directional) {
            directional.setFacing(BlockFace.SOUTH);
            block.setBlockData(directional);
        }

        Optional<DetectedSign> detected = new RayTraceSignDetector(MAX_DISTANCE).detect(player);
        results.add(material.name() + "=" + detected
                .map(value -> "PASS " + value)
                .orElse("FAIL no sign detected"));

        if (index + 1 < SIGN_MATERIALS.size()) {
            getServer().getScheduler().runTaskLater(
                    this,
                    () -> recordDetection(player, signLocation, index + 1, results),
                    1L
            );
            return;
        }

        getServer().getScheduler().runTaskLater(this, () -> recordBoundaryCases(player, signLocation, results), 1L);
    }

    private void recordBoundaryCases(Player player, Location signLocation, List<String> results) {
        Block target = signLocation.getBlock();
        target.setType(Material.AIR);
        results.add("MISS=" + (new RayTraceSignDetector(MAX_DISTANCE).detect(player).isEmpty()
                ? "PASS"
                : "FAIL hit air"));

        target.setType(Material.STONE);
        results.add("NON_SIGN=" + (new RayTraceSignDetector(MAX_DISTANCE).detect(player).isEmpty()
                ? "PASS"
                : "FAIL hit stone"));

        target.setType(Material.AIR);
        Location farSignLocation = new Location(player.getWorld(), 0.0, 301.0, 10.0);
        farSignLocation.getBlock().setType(Material.OAK_SIGN);
        results.add("MAX_DISTANCE=" + (new RayTraceSignDetector(MAX_DISTANCE).detect(player).isEmpty()
                ? "PASS"
                : "FAIL hit beyond range"));

        boolean passed = results.stream().noneMatch(result -> result.contains("FAIL"));
        String result = (passed ? "PASS " : "FAIL ") + String.join("; ", results);
        getLogger().info("RAYTRACE_INTEGRATION " + result);
        writeResult(result);
    }

    private void writeResult(String result) {
        String resultPath = System.getProperty("signlens.integration.result");
        if (resultPath == null || resultPath.isBlank()) {
            return;
        }

        try {
            Files.writeString(Path.of(resultPath), result);
        } catch (IOException exception) {
            getLogger().severe("Could not write integration result: " + exception.getMessage());
        }
    }
}
