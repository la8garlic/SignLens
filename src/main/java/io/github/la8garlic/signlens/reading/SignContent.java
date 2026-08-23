package io.github.la8garlic.signlens.reading;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.DyeColor;

/** Immutable content and visual metadata for one side of a sign. */
public record SignContent(
        List<Component> lines,
        DyeColor color,
        boolean glowingText
) {

    public SignContent {
        Objects.requireNonNull(lines, "lines");
        if (lines.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("lines must not contain null components");
        }
        lines = List.copyOf(lines);
    }

    /**
     * Returns whether at least one line contains non-whitespace plain text.
     * Components remain untouched; plain text is used only for classification.
     */
    public boolean renderable() {
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
        return lines.stream().anyMatch(line -> !serializer.serialize(line).isBlank());
    }
}
