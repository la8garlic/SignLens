package io.github.la8garlic.signlens;

import io.github.la8garlic.signlens.command.SignLensCommand;
import io.github.la8garlic.signlens.detection.RayTraceSignDetector;
import io.github.la8garlic.signlens.focus.FocusController;
import io.github.la8garlic.signlens.metrics.PerformanceCounters;
import io.github.la8garlic.signlens.reading.PaperSignReader;
import io.github.la8garlic.signlens.render.ActionBarRenderer;
import io.github.la8garlic.signlens.render.ContentFormatter;
import io.github.la8garlic.signlens.render.RenderDecision;
import io.github.la8garlic.signlens.render.RenderDecisionType;
import io.github.la8garlic.signlens.render.RenderPolicy;
import io.github.la8garlic.signlens.render.SignRenderer;
import io.github.la8garlic.signlens.scan.PlayerScanTask;
import io.github.la8garlic.signlens.scan.ScanSettings;
import io.github.la8garlic.signlens.session.PlayerSession;
import io.github.la8garlic.signlens.session.SessionRegistry;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SignLens plugin entry point.
 *
 * <p>Session lifecycle is registered here and scanning is attached to each
 * player's entity scheduler.</p>
 */
public final class SignLensPlugin extends JavaPlugin implements Listener {

    private SessionRegistry sessions;
    private ScanSettings scanSettings;
    private RayTraceSignDetector detector;
    private PaperSignReader reader;
    private ContentFormatter formatter;
    private SignRenderer renderer;
    private PerformanceCounters performanceCounters;
    private Duration focusDwell;
    private Duration focusLostGrace;
    private Duration renderKeepalive;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        performanceCounters = new PerformanceCounters();
        loadRuntimeConfiguration();
        sessions = new SessionRegistry(this::createSession);
        Objects.requireNonNull(getCommand("signlens"), "signlens command")
                .setExecutor(new SignLensCommand(this));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getOnlinePlayers().forEach(this::startScan);
        getLogger().info("SignLens " + getPluginMeta().getVersion() + " enabled.");
    }

    private void loadRuntimeConfiguration() {
        String renderMode = getConfig().getString("render.mode", "action-bar");
        if (!"action-bar".equalsIgnoreCase(renderMode)) {
            throw new IllegalArgumentException("render.mode must be 'action-bar' in SignLens 0.1");
        }
        scanSettings = new ScanSettings(
                getConfig().getDouble("detection.max-distance", 8.0),
                getConfig().getInt("detection.scan-period-ticks", 2),
                getConfig().getInt("performance.idle-probe-ticks", 10),
                getConfig().getDouble("detection.position-threshold", 0.02),
                (float) getConfig().getDouble("detection.rotation-threshold-degrees", 1.0)
        );
        focusDwell = configuredDuration("focus.dwell-millis", 200L);
        focusLostGrace = configuredDuration("focus.lost-grace-millis", 300L);
        renderKeepalive = configuredDuration("render.keepalive-millis", 2500L);
        detector = new RayTraceSignDetector(scanSettings.maxDistance());
        reader = new PaperSignReader(
                performanceCounters,
                (detectedSign, failure) -> getLogger().log(
                        Level.WARNING,
                        "Unexpected sign read failure at "
                                + detectedSign.x() + "," + detectedSign.y() + "," + detectedSign.z()
                                + "; scan continues.",
                        failure
                )
        );
        formatter = new ContentFormatter(
                getConfig().getInt("render.soft-limit", ContentFormatter.DEFAULT_SOFT_LIMIT),
                getConfig().getInt("render.max-length", ContentFormatter.DEFAULT_HARD_LIMIT)
        );
        renderer = new ActionBarRenderer(performanceCounters);
    }

    @Override
    public void onDisable() {
        if (sessions != null) {
            clearRenderedSessions();
            sessions.clear();
            sessions = null;
        }
        detector = null;
        reader = null;
        formatter = null;
        renderer = null;
        performanceCounters = null;
        scanSettings = null;
        focusDwell = null;
        focusLostGrace = null;
        renderKeepalive = null;
        getLogger().info("SignLens disabled.");
    }

    public SessionRegistry sessions() {
        if (sessions == null) {
            throw new IllegalStateException("SignLens is not enabled");
        }
        return sessions;
    }

    public PerformanceCounters performanceCounters() {
        if (performanceCounters == null) {
            throw new IllegalStateException("SignLens is not enabled");
        }
        return performanceCounters;
    }

    public boolean debugEnabled() {
        return getConfig().getBoolean("debug.enabled", true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        startScan(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions().remove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        resetSession(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        resetSession(event.getPlayer());
    }

    private void startScan(Player player) {
        PlayerSession session = sessions().getOrCreate(player);
        new PlayerScanTask(
                this,
                player,
                session,
                Objects.requireNonNull(detector, "detector"),
                Objects.requireNonNull(reader, "reader"),
                Objects.requireNonNull(formatter, "formatter"),
                Objects.requireNonNull(renderer, "renderer"),
                Objects.requireNonNull(scanSettings, "scanSettings"),
                () -> getConfig().getBoolean("enabled", true),
                Objects.requireNonNull(performanceCounters, "performanceCounters")
        ).start();
    }

    private PlayerSession createSession(UUID playerId) {
        return new PlayerSession(
                playerId,
                new FocusController(
                        Objects.requireNonNull(focusDwell, "focusDwell"),
                        Objects.requireNonNull(focusLostGrace, "focusLostGrace")
                ),
                new RenderPolicy(Objects.requireNonNull(renderKeepalive, "renderKeepalive"))
        );
    }

    private Duration configuredDuration(String path, long fallbackMillis) {
        long millis = getConfig().getLong(path, fallbackMillis);
        if (millis <= 0L) {
            throw new IllegalArgumentException(path + " must be greater than zero");
        }
        return Duration.ofMillis(millis);
    }

    private void clearRenderedSessions() {
        SignRenderer activeRenderer = renderer;
        if (activeRenderer == null) {
            return;
        }
        getServer().getOnlinePlayers().forEach(player ->
                sessions.find(player.getUniqueId()).ifPresent(session -> {
                    RenderDecision decision = session.reset();
                    if (decision.type() == RenderDecisionType.CLEAR) {
                        activeRenderer.clear(player);
                    }
                }));
    }

    private void resetSession(Player player) {
        RenderDecision decision = sessions().getOrCreate(player).reset();
        if (decision.type() == RenderDecisionType.CLEAR) {
            Objects.requireNonNull(renderer, "renderer").clear(player);
        }
    }
}
