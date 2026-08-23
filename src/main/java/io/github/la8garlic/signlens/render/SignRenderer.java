package io.github.la8garlic.signlens.render;

import org.bukkit.entity.Player;

/** Sends already-formatted reader output to a player. */
public interface SignRenderer {

    void show(Player player, FormattedContent content);

    void clear(Player player);
}
