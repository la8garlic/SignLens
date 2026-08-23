package io.github.la8garlic.signlens;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * SignLens plugin entry point.
 *
 * <p>Issue 01 intentionally provides only the lifecycle bootstrap. Runtime
 * detection, focus, reading, and rendering are implemented by later issues.</p>
 */
public final class SignLensPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("SignLens " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SignLens disabled.");
    }
}
