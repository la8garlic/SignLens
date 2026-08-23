package io.github.la8garlic.signlens;

import io.github.la8garlic.signlens.detection.RayTraceSignDetector;
import io.github.la8garlic.signlens.reading.PaperSignReader;
import io.github.la8garlic.signlens.render.ActionBarRenderer;
import io.github.la8garlic.signlens.render.ContentFormatter;
import io.github.la8garlic.signlens.render.RenderDecision;
import io.github.la8garlic.signlens.render.RenderDecisionType;
import io.github.la8garlic.signlens.render.SignRenderer;
import io.github.la8garlic.signlens.scan.PlayerScanTask;
import io.github.la8garlic.signlens.scan.ScanSettings;
import io.github.la8garlic.signlens.session.PlayerSession;
import io.github.la8garlic.signlens.session.SessionRegistry;
import java.util.Objects;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        sessions = new SessionRegistry();
        scanSettings = new ScanSettings(
                getConfig().getDouble("detection.max-distance", 8.0),
                getConfig().getInt("detection.scan-period-ticks", 2),
                getConfig().getInt("performance.idle-probe-ticks", 10),
                getConfig().getDouble("detection.position-threshold", 0.02),
                (float) getConfig().getDouble("detection.rotation-threshold-degrees", 1.0)
        );
        detector = new RayTraceSignDetector(scanSettings.maxDistance());
        reader = new PaperSignReader();
        formatter = new ContentFormatter(
                getConfig().getInt("render.soft-limit", ContentFormatter.DEFAULT_SOFT_LIMIT),
                getConfig().getInt("render.max-length", ContentFormatter.DEFAULT_HARD_LIMIT)
        );
        renderer = new ActionBarRenderer();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getOnlinePlayers().forEach(this::startScan);
        getLogger().info("SignLens " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (sessions != null) {
            sessions.clear();
            sessions = null;
        }
        detector = null;
        reader = null;
        formatter = null;
        renderer = null;
        scanSettings = null;
        getLogger().info("SignLens disabled.");
    }

    public SessionRegistry sessions() {
        if (sessions == null) {
            throw new IllegalStateException("SignLens is not enabled");
        }
        return sessions;
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
                () -> getConfig().getBoolean("enabled", true)
        ).start();
    }

    private void resetSession(Player player) {
        RenderDecision decision = sessions().getOrCreate(player).reset();
        if (decision.type() == RenderDecisionType.CLEAR) {
            Objects.requireNonNull(renderer, "renderer").clear(player);
        }
    }
}
