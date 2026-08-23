package io.github.la8garlic.signlens.command;

import io.github.la8garlic.signlens.SignLensPlugin;
import io.github.la8garlic.signlens.session.PlayerSession;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles the on-demand SignLens command surface. */
public final class SignLensCommand implements CommandExecutor {

    public static final String DEBUG_PERMISSION = "signlens.command.debug";

    private final SignLensPlugin plugin;
    private final DebugMessageFormatter formatter;

    public SignLensCommand(SignLensPlugin plugin) {
        this(plugin, new DebugMessageFormatter());
    }

    SignLensCommand(SignLensPlugin plugin, DebugMessageFormatter formatter) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(args, "args");
        if (args.length != 1 || !args[0].equalsIgnoreCase("debug")) {
            sender.sendMessage(Component.text("Usage: /signlens debug"));
            return true;
        }

        if (!sender.hasPermission(DEBUG_PERMISSION)) {
            sender.sendMessage(Component.text("You do not have permission to inspect SignLens."));
            return true;
        }
        if (!plugin.debugEnabled()) {
            sender.sendMessage(Component.text("SignLens debug output is disabled in configuration."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This diagnostic command is player-only."));
            return true;
        }

        Optional<PlayerSession> session = plugin.sessions().find(player.getUniqueId());
        DebugSnapshot snapshot = session
                .map(value -> DebugSnapshot.capture(value, plugin.performanceCounters(), Instant.now()))
                .orElseGet(() -> DebugSnapshot.noSession(plugin.performanceCounters()));
        player.sendMessage(formatter.format(snapshot));
        return true;
    }
}
