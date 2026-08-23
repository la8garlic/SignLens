package io.github.la8garlic.signlens.render;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class ActionBarRendererTest {

    @Test
    void sendsFormattedAdventureContentAndUsesEmptyComponentToClear() {
        Player player = mock(Player.class);
        Component content = Component.text("WELCOME");
        ActionBarRenderer renderer = new ActionBarRenderer();

        renderer.show(player, content);
        renderer.clear(player);

        verify(player).sendActionBar(content);
        verify(player).sendActionBar(Component.empty());
    }
}
