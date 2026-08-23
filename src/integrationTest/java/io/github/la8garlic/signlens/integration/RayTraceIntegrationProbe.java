package io.github.la8garlic.signlens.integration;

import io.github.la8garlic.signlens.SignLensPlugin;
import io.github.la8garlic.signlens.detection.DetectedSign;
import io.github.la8garlic.signlens.detection.RayTraceSignDetector;
import io.github.la8garlic.signlens.focus.FocusState;
import io.github.la8garlic.signlens.metrics.PerformanceCounters;
import io.github.la8garlic.signlens.reading.PaperSignReader;
import io.github.la8garlic.signlens.session.PlayerSession;
import io.github.la8garlic.signlens.reading.SignSnapshot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.DyeColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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

    private boolean performanceProbeStarted;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (performanceMode()) {
            startPerformanceProbe();
            return;
        }
        getServer().getScheduler().runTaskLater(this, () -> runProbe(player), 20L);
    }

    private boolean performanceMode() {
        return "performance".equalsIgnoreCase(System.getProperty("signlens.integration.mode", ""));
    }

    private void startPerformanceProbe() {
        if (performanceProbeStarted) {
            return;
        }
        performanceProbeStarted = true;
        getServer().getScheduler().runTaskLater(this, this::beginPerformanceMeasurement, 100L);
    }

    private void beginPerformanceMeasurement() {
        SignLensPlugin plugin = JavaPlugin.getPlugin(SignLensPlugin.class);
        String scenario = System.getProperty("signlens.performance.scenario", "unknown");
        int durationSeconds = Math.max(1, Integer.getInteger("signlens.performance.duration-seconds", 20));
        int durationTicks = durationSeconds * 20;
        int signs = preparePerformanceWorld(scenario);
        PerformanceCounters counters = plugin.performanceCounters();
        counters.reset();

        List<Long> tickIntervals = new ArrayList<>();
        long[] previousTick = {System.nanoTime()};
        int[] samples = {0};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.nanoTime();
                tickIntervals.add(now - previousTick[0]);
                previousTick[0] = now;
                samples[0]++;
                if (samples[0] >= durationTicks) {
                    task[0].cancel();
                    writePerformanceResult(scenario, durationSeconds, signs, counters.snapshot(), tickIntervals);
                }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    private int preparePerformanceWorld(String scenario) {
        if (!"sign-density".equalsIgnoreCase(scenario)) {
            return 0;
        }

        Player firstPlayer = getServer().getOnlinePlayers().stream().findFirst().orElse(null);
        if (firstPlayer == null) {
            return 0;
        }

        Location origin = new Location(firstPlayer.getWorld(), 0.0, 300.0, 0.0);
        int placed = 0;
        for (int x = -20; x <= 20 && placed < 1000; x++) {
            for (int z = -20; z <= 20 && placed < 1000; z++) {
                origin.getWorld().getBlockAt(x, 301, z).setType(Material.OAK_SIGN);
                placed++;
            }
        }
        return placed;
    }

    private void writePerformanceResult(
            String scenario,
            int durationSeconds,
            int signs,
            PerformanceCounters.Snapshot counters,
            List<Long> tickIntervals
    ) {
        long intervalTotal = tickIntervals.stream().mapToLong(Long::longValue).sum();
        double averageTickMillis = tickIntervals.isEmpty()
                ? 0.0
                : intervalTotal / (double) tickIntervals.size() / 1_000_000.0;
        long p95TickNanos = percentile(tickIntervals, 0.95);
        String result = String.format(
                Locale.ROOT,
                "PERFORMANCE PASS scenario=%s players=%d signs=%d duration-s=%d "
                        + "scans=%d traces=%d hits=%d misses=%d skipped=%d idle=%d "
                        + "avg-scan-ms=%.3f p95-scan-ms=%.3f avg-ray-ms=%.3f "
                        + "actionbar-sends=%d actionbar-clears=%d avg-tick-ms=%.3f p95-tick-ms=%.3f",
                scenario,
                getServer().getOnlinePlayers().size(),
                signs,
                durationSeconds,
                counters.scanCycles(),
                counters.rayTraces(),
                counters.rayTraceHits(),
                counters.rayTraceMisses(),
                counters.scanSkips(),
                counters.idleProbes(),
                counters.averageScanNanos() / 1_000_000.0,
                counters.p95ScanNanos() / 1_000_000.0,
                counters.averageRayTraceNanos() / 1_000_000.0,
                counters.actionBarSends(),
                counters.actionBarClears(),
                averageTickMillis,
                p95TickNanos / 1_000_000.0
        );
        getLogger().info(result);
        writeResult(result);
    }

    private static long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int index = Math.max(0, (int) Math.ceil(sorted.size() * percentile) - 1);
        return sorted.get(index);
    }

    private void runProbe(Player player) {
        player.setOp(true);
        player.setGameMode(GameMode.CREATIVE);
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

        getServer().getScheduler().runTaskLater(this, () -> recordReaderCases(player, signLocation, results), 1L);
    }

    private void recordReaderCases(Player player, Location signLocation, List<String> results) {
        Block block = signLocation.getBlock();
        for (Material material : SIGN_MATERIALS) {
            block.setType(material);
            Sign sign = (Sign) block.getState();
            Component formattedFront = Component.text("Front", NamedTextColor.RED).decorate(TextDecoration.BOLD);
            SignSide front = sign.getSide(Side.FRONT);
            front.line(0, formattedFront);
            front.line(1, Component.text("coloured"));
            front.setColor(DyeColor.RED);
            front.setGlowingText(true);

            SignSide back = sign.getSide(Side.BACK);
            back.line(0, Component.text("  "));
            back.line(1, Component.text("\t"));
            back.setColor(DyeColor.BLUE);
            back.setGlowingText(false);
            sign.update(true, false);

            recordReaderForMaterial(player, block, material, results);
        }

        getServer().getScheduler().runTaskLater(this, () -> finishReaderCases(player, signLocation, results), 1L);
    }

    private void finishReaderCases(Player player, Location signLocation, List<String> results) {
        boolean passed = results.stream().noneMatch(result -> result.contains("FAIL"));
        getServer().getScheduler().runTaskLater(
                this,
                () -> recordRuntimePipeline(player, signLocation, results, passed),
                20L
        );
    }

    private void recordRuntimePipeline(
            Player player,
            Location signLocation,
            List<String> results,
            boolean previousCasesPassed
    ) {
        Block block = signLocation.getBlock();
        block.setType(Material.OAK_SIGN);
        Sign sign = (Sign) block.getState();
        sign.getSide(Side.FRONT).line(0, Component.text("123", NamedTextColor.GREEN));
        sign.getSide(Side.FRONT).line(1, Component.text("456"));
        sign.update(true, false);

        player.getWorld().getBlockAt(0, 299, 0).setType(Material.STONE);
        player.getWorld().setSpawnLocation(0, 300, 0, 0.0f);
        player.teleport(new Location(player.getWorld(), 0.5, 300.0, 0.5, 0.0f, 0.0f));
        getServer().getScheduler().runTaskLater(this, () -> finishRuntimePipeline(
                player,
                results,
                previousCasesPassed
        ), 20L);
    }

    private void finishRuntimePipeline(Player player, List<String> results, boolean previousCasesPassed) {
        SignLensPlugin plugin = JavaPlugin.getPlugin(SignLensPlugin.class);
        Optional<PlayerSession> session = plugin.sessions().find(player.getUniqueId());
        boolean renderedLineBoundary = session.isPresent()
                && session.orElseThrow().renderPolicy().lastSentContent().map(content ->
                        content.lines().size() == 2
                                && plain(content.lines().get(0)).equals("123")
                                && plain(content.lines().get(1)).equals("456")
                                && !plain(content.toActionBarComponent()).contains("\n")
                ).orElse(false);
        boolean pipelinePassed = session.isPresent()
                && session.orElseThrow().focusController().state() == FocusState.FOCUSED
                && session.orElseThrow().lastSnapshot().isPresent()
                && renderedLineBoundary;
        results.add("RUNTIME_PIPELINE=" + (pipelinePassed ? "PASS" : "FAIL"));

        boolean passed = previousCasesPassed && pipelinePassed;
        String result = (passed ? "PASS " : "FAIL ") + String.join("; ", results);
        getLogger().info("RAYTRACE_INTEGRATION " + result);
        writeResult(result);
    }

    private void recordReaderForMaterial(
            Player player,
            Block block,
            Material material,
            List<String> results
    ) {
        Sign sign = (Sign) block.getState();
        Location firstSideLocation = new Location(player.getWorld(), 0.5, 300.0, 0.5);
        Location secondSideLocation = new Location(player.getWorld(), 0.5, 300.0, 6.5);
        player.teleport(firstSideLocation);
        Side firstSide = sign.getInteractableSideFor(player);
        Optional<SignSnapshot> firstSnapshot = new PaperSignReader().read(
                player,
                new DetectedSign(player.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), BlockFace.NORTH)
        );

        player.teleport(secondSideLocation);
        Side secondSide = sign.getInteractableSideFor(player);
        Optional<SignSnapshot> secondSnapshot = new PaperSignReader().read(
                player,
                new DetectedSign(player.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), BlockFace.NORTH)
        );

        boolean sideChanged = firstSide != secondSide;
        boolean firstMatches = matchesExpectedSide(firstSnapshot, firstSide, sign);
        boolean secondMatches = matchesExpectedSide(secondSnapshot, secondSide, sign);
        results.add(material.name() + "_READER="
                + (sideChanged && firstMatches && secondMatches ? "PASS" : "FAIL"));
    }

    private boolean matchesExpectedSide(Optional<SignSnapshot> snapshot, Side side, Sign sign) {
        if (snapshot.isEmpty() || snapshot.orElseThrow().key().side() != side) {
            return false;
        }

        SignSnapshot value = snapshot.orElseThrow();
        SignSide expected = sign.getSide(side);
        String firstLine = PlainTextComponentSerializer.plainText().serialize(value.content().lines().get(0));
        if (side == Side.FRONT) {
            Component component = value.content().lines().get(0);
            return firstLine.equals("Front")
                    && NamedTextColor.RED.equals(component.style().color())
                    && component.style().hasDecoration(TextDecoration.BOLD)
                    && value.content().color() == expected.getColor()
                    && value.content().glowingText() == expected.isGlowingText()
                    && value.renderable();
        }

        return firstLine.isBlank()
                && value.content().color() == expected.getColor()
                && !value.content().glowingText()
                && !value.renderable();
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

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
