package io.github.la8garlic.signlens.render;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Sends already-formatted reader output to a player. */
public interface SignRenderer {

    void show(Player player, Component content);

    void clear(Player player);
}
