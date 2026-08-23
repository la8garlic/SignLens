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
    void preservesLineBoundariesAndOmitsLeadingAndTrailingWhitespaceLines() {
        ContentFormatter formatter = new ContentFormatter(100, 120);

        Optional<FormattedContent> formatted = formatter.format(content(
                Component.text("WELCOME"),
                Component.text(" "),
                Component.text("TO"),
                Component.text("SPAWN"),
                Component.empty()
        ));

        assertEquals("WELCOME\n\nTO\nSPAWN", sourcePlain(formatted.orElseThrow()));
        assertEquals("WELCOME↵↵TO↵SPAWN", actionBarPlain(formatted.orElseThrow()));
    }

    @Test
    void preservesTheAcceptanceExampleAsTwoLines() {
        FormattedContent result = new ContentFormatter(100, 120).format(content(
                Component.text("123"),
                Component.text("456")
        )).orElseThrow();

        assertEquals("123\n456", sourcePlain(result));
        assertEquals("123↵456", actionBarPlain(result));
        assertFalse(actionBarPlain(result).contains("\n"));
    }

    @Test
    void preservesFormattingOnEachLine() {
        FormattedContent result = new ContentFormatter(100, 120).format(content(
                Component.text("123", NamedTextColor.RED),
                Component.text("456", NamedTextColor.BLUE)
        )).orElseThrow();

        assertEquals("123\n456", sourcePlain(result));
        assertEquals(NamedTextColor.RED, findText(result.lines().get(0), "123").color());
        assertEquals(NamedTextColor.BLUE, findText(result.lines().get(1), "456").color());
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

        Optional<FormattedContent> formatted = new ContentFormatter(100, 120)
                .format(content(source));

        FormattedContent result = formatted.orElseThrow();
        Component line = result.lines().get(0);
        TextComponent read = findText(line, "READ");
        TextComponent nested = findText(line, " child");
        assertEquals(NamedTextColor.GOLD, read.color());
        assertTrue(read.hasDecoration(TextDecoration.BOLD));
        assertEquals(Key.key("minecraft", "uniform"), read.font());
        assertEquals(NamedTextColor.BLUE, nested.color());
        assertNull(line.clickEvent());
        assertNull(line.insertion());
        assertNull(read.clickEvent());
        assertNull(read.insertion());
        assertNull(nested.clickEvent());
        assertFalse(containsInteraction(line));
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
        ContentFormatter formatter = new ContentFormatter(8, 20);

        FormattedContent result = formatter.format(content(
                Component.text("123456789").color(NamedTextColor.GREEN)
        )).orElseThrow();

        assertEquals("1234567…", sourcePlain(result));
        assertEquals(8, result.visualLength());
        assertEquals(NamedTextColor.GREEN, findText(result.lines().get(0), "1234567").color());
    }

    @Test
    void contentBeyondHardLimitNeverEscapesTheSafetyCap() {
        ContentFormatter formatter = new ContentFormatter(8, 12);

        FormattedContent result = formatter.format(content(
                Component.text("012345678901234567890123456789")
        )).orElseThrow();

        assertTrue(result.visualLength() <= 12);
        assertTrue(sourcePlain(result).endsWith("…"));
    }

    @Test
    void truncatesMultilineContentWithoutFlatteningTheLineBoundary() {
        FormattedContent result = new ContentFormatter(5, 10).format(content(
                Component.text("123"),
                Component.text("456")
        )).orElseThrow();

        assertEquals("123\n4…", sourcePlain(result));
        assertEquals("123↵4…", actionBarPlain(result));
        assertEquals(5, result.visualLength());
    }

    @Test
    void normalLengthContentIsNotTruncated() {
        ContentFormatter formatter = new ContentFormatter(20, 30);

        FormattedContent result = formatter.format(content(
                Component.text("LEFT"),
                Component.text("RIGHT")
        )).orElseThrow();

        assertEquals("LEFT\nRIGHT", sourcePlain(result));
        assertFalse(sourcePlain(result).contains("…"));
    }

    @Test
    void rejectsInvalidLengthConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new ContentFormatter(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new ContentFormatter(10, 9));
    }

    private static SignContent content(Component... lines) {
        return new SignContent(List.of(lines), DyeColor.WHITE, false);
    }

    private static String sourcePlain(FormattedContent content) {
        return content.lines().stream()
                .map(PLAIN_TEXT::serialize)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String actionBarPlain(FormattedContent content) {
        return PLAIN_TEXT.serialize(content.toActionBarComponent());
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
