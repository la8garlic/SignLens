package io.github.la8garlic.signlens;

import io.github.la8garlic.signlens.session.SessionRegistry;
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
 * <p>Session lifecycle is registered here; scanning is attached to each
 * player's entity scheduler by the later runtime pipeline.</p>
 */
public final class SignLensPlugin extends JavaPlugin implements Listener {

    private SessionRegistry sessions;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        sessions = new SessionRegistry();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SignLens " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (sessions != null) {
            sessions.clear();
            sessions = null;
        }
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
        sessions().getOrCreate(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions().remove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        sessions().getOrCreate(event.getPlayer()).reset();
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        sessions().getOrCreate(event.getPlayer()).reset();
    }
}
