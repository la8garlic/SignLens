package io.github.la8garlic.signlens.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import io.github.la8garlic.signlens.reading.SignContent;
import io.github.la8garlic.signlens.reading.SignSnapshot;

/** Converts immutable sign content into safe, compact ActionBar-ready content. */
public final class ContentFormatter {

    public static final String DEFAULT_SEPARATOR = " · ";
    public static final int DEFAULT_SOFT_LIMIT = 96;
    public static final int DEFAULT_HARD_LIMIT = 120;

    private static final String ELLIPSIS = "…";
    private static final PlainTextComponentSerializer PLAIN_TEXT =
            PlainTextComponentSerializer.plainText();

    private final ComponentSanitizer sanitizer;
    private final String separator;
    private final Component separatorComponent;
    private final int softLimit;
    private final int hardLimit;

    public ContentFormatter() {
        this(DEFAULT_SEPARATOR, DEFAULT_SOFT_LIMIT, DEFAULT_HARD_LIMIT);
    }

    public ContentFormatter(String separator, int softLimit, int hardLimit) {
        this(separator, softLimit, hardLimit, new ComponentSanitizer());
    }

    public ContentFormatter(
            String separator,
            int softLimit,
            int hardLimit,
            ComponentSanitizer sanitizer
    ) {
        this.separator = Objects.requireNonNull(separator, "separator");
        if (softLimit <= 0) {
            throw new IllegalArgumentException("softLimit must be greater than zero");
        }
        if (hardLimit < softLimit) {
            throw new IllegalArgumentException("hardLimit must not be less than softLimit");
        }
        this.softLimit = softLimit;
        this.hardLimit = hardLimit;
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.separatorComponent = Component.text(separator, Style.style(NamedTextColor.DARK_GRAY));
    }

    public String separator() {
        return separator;
    }

    public int softLimit() {
        return softLimit;
    }

    public int hardLimit() {
        return hardLimit;
    }

    public Optional<Component> format(SignSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return format(snapshot.content());
    }

    public Optional<Component> format(SignContent content) {
        Objects.requireNonNull(content, "content");

        List<Component> lines = content.lines().stream()
                .filter(line -> !PLAIN_TEXT.serialize(line).isBlank())
                .map(sanitizer::sanitize)
                .toList();
        if (lines.isEmpty()) {
            return Optional.empty();
        }

        Component joined = Component.join(JoinConfiguration.separator(separatorComponent), lines);
        int visualLength = visualLength(joined);
        if (visualLength <= softLimit) {
            return Optional.of(joined);
        }

        int targetLimit = visualLength > hardLimit ? hardLimit : softLimit;
        return Optional.of(truncate(joined, targetLimit));
    }

    private Component truncate(Component component, int limit) {
        Clip clip = clip(component, Math.max(0, limit - visualLength(Component.text(ELLIPSIS))));
        Component result = clip.component();
        return result.append(Component.text(ELLIPSIS));
    }

    private Clip clip(Component component, int remaining) {
        if (remaining == 0) {
            return new Clip(Component.empty().style(component.style()), 0);
        }

        if (!(component instanceof TextComponent)) {
            int length = visualLength(component);
            if (length <= remaining) {
                return new Clip(component, length);
            }
            return new Clip(Component.empty().style(component.style()), 0);
        }

        int ownLength = ownVisualLength(component);
        int ownCharacters = Math.min(ownLength, remaining);
        Component result = component;
        int used = 0;

        if (component instanceof TextComponent text) {
            String content = text.content();
            int keepCodePoints = Math.min(content.codePointCount(0, content.length()), ownCharacters);
            int end = content.offsetByCodePoints(0, keepCodePoints);
            result = text.content(content.substring(0, end));
            used = keepCodePoints;
        } else {
            used = ownLength;
        }

        List<Component> children = new ArrayList<>();
        int childRemaining = remaining - used;
        for (Component child : component.children()) {
            if (childRemaining == 0) {
                break;
            }
            Clip childClip = clip(child, childRemaining);
            if (childClip.used() == 0 && visualLength(child) > 0) {
                break;
            }
            children.add(childClip.component());
            used += childClip.used();
            childRemaining = remaining - used;
            if (childClip.used() < visualLength(child)) {
                break;
            }
        }

        return new Clip(result.children(children), used);
    }

    private static int ownVisualLength(Component component) {
        if (component instanceof TextComponent text) {
            return text.content().codePointCount(0, text.content().length());
        }
        return 0;
    }

    private static int visualLength(Component component) {
        String plain = PLAIN_TEXT.serialize(component);
        return plain.codePointCount(0, plain.length());
    }

    private record Clip(Component component, int used) {
    }
}
