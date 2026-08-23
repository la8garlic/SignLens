package io.github.la8garlic.signlens.render;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Immutable, line-aware content shared by formatters, render policy, and
 * output renderers.
 *
 * <p>The source line structure remains available to renderers. A renderer is
 * responsible for projecting that structure onto its own output surface.</p>
 */
public record FormattedContent(List<Component> lines) {

    public static final String ACTION_BAR_LINE_BREAK = "↵";

    private static final PlainTextComponentSerializer PLAIN_TEXT =
            PlainTextComponentSerializer.plainText();

    public FormattedContent {
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        if (lines.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("lines must not contain null components");
        }
        lines = List.copyOf(lines);
    }

    public boolean multiline() {
        return lines.size() > 1;
    }

    /** Returns the source text length without counting line boundaries. */
    public int visualLength() {
        return lines.stream()
                .mapToInt(FormattedContent::visualLength)
                .sum();
    }

    /**
     * Projects this content onto the single-line ActionBar surface.
     * Minecraft's native ActionBar does not lay out newline components as
     * separate rows, so a visible return marker is used instead.
     */
    public Component toActionBarComponent() {
        return Component.join(
                JoinConfiguration.separator(Component.text(ACTION_BAR_LINE_BREAK)),
                lines
        );
    }

    private static int visualLength(Component component) {
        return (int) PLAIN_TEXT.serialize(component).codePoints()
                .filter(codePoint -> codePoint != '\n' && codePoint != '\r')
                .count();
    }
}
