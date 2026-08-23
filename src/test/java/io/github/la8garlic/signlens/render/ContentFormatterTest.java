package io.github.la8garlic.signlens.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.la8garlic.signlens.reading.SignContent;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.DyeColor;
import org.junit.jupiter.api.Test;

class ContentFormatterTest {

    private static final PlainTextComponentSerializer PLAIN_TEXT =
            PlainTextComponentSerializer.plainText();

    @Test
    void joinsFourLinesAndOmitsPureWhitespaceLines() {
        ContentFormatter formatter = new ContentFormatter(" · ", 100, 120);

        Optional<Component> formatted = formatter.format(content(
                Component.text("WELCOME"),
                Component.text(" "),
                Component.text("TO"),
                Component.text("SPAWN")
        ));

        assertEquals("WELCOME · TO · SPAWN", plain(formatted));
    }

    @Test
    void preservesPresentationButRemovesInteractionMetadataRecursively() {
        Component child = Component.text(" child")
                .color(NamedTextColor.BLUE)
                .clickEvent(ClickEvent.runCommand("/secret"));
        Component source = Component.text("READ")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .font(Key.key("minecraft", "uniform"))
                .append(child)
                .clickEvent(ClickEvent.runCommand("/secret"))
                .insertion("secret");

        Optional<Component> formatted = new ContentFormatter(" · ", 100, 120)
                .format(content(source));

        Component result = formatted.orElseThrow();
        TextComponent read = findText(result, "READ");
        TextComponent nested = findText(result, " child");
        assertEquals(NamedTextColor.GOLD, read.color());
        assertTrue(read.hasDecoration(TextDecoration.BOLD));
        assertEquals(Key.key("minecraft", "uniform"), read.font());
        assertEquals(NamedTextColor.BLUE, nested.color());
        assertNull(result.clickEvent());
        assertNull(result.insertion());
        assertNull(read.clickEvent());
        assertNull(read.insertion());
        assertNull(nested.clickEvent());
        assertFalse(containsInteraction(result));
    }

    @Test
    void emptyOrWhitespaceOnlyContentIsNotRenderable() {
        ContentFormatter formatter = new ContentFormatter();

        assertTrue(formatter.format(content(
                Component.text("  "),
                Component.text("\n\t"),
                Component.empty()
        )).isEmpty());
    }

    @Test
    void contentBeyondSoftLimitGetsAStyledStructureAndEllipsis() {
        ContentFormatter formatter = new ContentFormatter(" · ", 8, 20);

        Component result = formatter.format(content(
                Component.text("123456789").color(NamedTextColor.GREEN)
        )).orElseThrow();

        assertEquals("1234567…", plain(result));
        assertEquals(8, visualLength(result));
        assertEquals(NamedTextColor.GREEN, findText(result, "1234567").color());
    }

    @Test
    void contentBeyondHardLimitNeverEscapesTheSafetyCap() {
        ContentFormatter formatter = new ContentFormatter(" · ", 8, 12);

        Component result = formatter.format(content(
                Component.text("012345678901234567890123456789")
        )).orElseThrow();

        assertTrue(visualLength(result) <= 12);
        assertTrue(plain(result).endsWith("…"));
    }

    @Test
    void normalLengthContentIsNotTruncated() {
        ContentFormatter formatter = new ContentFormatter(" / ", 20, 30);

        Component result = formatter.format(content(
                Component.text("LEFT"),
                Component.text("RIGHT")
        )).orElseThrow();

        assertEquals("LEFT / RIGHT", plain(result));
        assertFalse(plain(result).contains("…"));
    }

    @Test
    void rejectsInvalidLengthConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new ContentFormatter(" · ", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new ContentFormatter(" · ", 10, 9));
    }

    private static SignContent content(Component... lines) {
        return new SignContent(List.of(lines), DyeColor.WHITE, false);
    }

    private static String plain(Optional<Component> component) {
        return PLAIN_TEXT.serialize(component.orElseThrow());
    }

    private static String plain(Component component) {
        return PLAIN_TEXT.serialize(component);
    }

    private static int visualLength(Component component) {
        String plain = PLAIN_TEXT.serialize(component);
        return plain.codePointCount(0, plain.length());
    }

    private static TextComponent findText(Component component, String content) {
        if (component instanceof TextComponent text && text.content().equals(content)) {
            return text;
        }
        for (Component child : component.children()) {
            try {
                return findText(child, content);
            } catch (AssertionError ignored) {
                // Continue searching siblings.
            }
        }
        throw new AssertionError("Missing text component: " + content);
    }

    private static boolean containsInteraction(Component component) {
        if (component.clickEvent() != null || component.hoverEvent() != null || component.insertion() != null) {
            return true;
        }
        return component.children().stream().anyMatch(ContentFormatterTest::containsInteraction);
    }
}
