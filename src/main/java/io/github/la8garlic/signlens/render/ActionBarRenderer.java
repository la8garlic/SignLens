package io.github.la8garlic.signlens.render;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Paper ActionBar implementation; cadence decisions remain in {@link RenderPolicy}. */
public final class ActionBarRenderer implements SignRenderer {

    @Override
    public void show(Player player, Component content) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(content, "content");
        player.sendActionBar(content);
    }

    @Override
    public void clear(Player player) {
        Objects.requireNonNull(player, "player");
        player.sendActionBar(Component.empty());
    }
}
