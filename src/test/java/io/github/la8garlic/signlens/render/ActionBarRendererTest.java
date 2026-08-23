package io.github.la8garlic.signlens.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ActionBarRendererTest {

    @Test
    void sendsFormattedAdventureContentAndUsesEmptyComponentToClear() {
        Player player = mock(Player.class);
        FormattedContent content = new FormattedContent(java.util.List.of(Component.text("WELCOME")));
        ActionBarRenderer renderer = new ActionBarRenderer();

        renderer.show(player, content);
        renderer.clear(player);

        verify(player).sendActionBar(content.toActionBarComponent());
        verify(player).sendActionBar(Component.empty());
    }

    @Test
    void projectsMultilineContentToAnActionBarSafeVisibleBoundary() {
        Player player = mock(Player.class);
        FormattedContent content = new FormattedContent(java.util.List.of(
                Component.text("123"),
                Component.text("456")
        ));
        ActionBarRenderer renderer = new ActionBarRenderer();

        renderer.show(player, content);

        ArgumentCaptor<Component> sent = ArgumentCaptor.forClass(Component.class);
        verify(player).sendActionBar(sent.capture());
        String plain = PlainTextComponentSerializer.plainText().serialize(sent.getValue());
        assertEquals("123↵456", plain);
        assertFalse(plain.contains("\n"));
    }
}
