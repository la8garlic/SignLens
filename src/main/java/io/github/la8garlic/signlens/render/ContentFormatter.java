package io.github.la8garlic.signlens.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import io.github.la8garlic.signlens.reading.SignContent;
import io.github.la8garlic.signlens.reading.SignSnapshot;

/** Converts immutable sign content into safe, line-aware renderer content. */
public final class ContentFormatter {

    public static final int DEFAULT_SOFT_LIMIT = 96;
    public static final int DEFAULT_HARD_LIMIT = 120;

    private static final String ELLIPSIS = "…";
    private static final PlainTextComponentSerializer PLAIN_TEXT =
            PlainTextComponentSerializer.plainText();

    private final ComponentSanitizer sanitizer;
    private final int softLimit;
    private final int hardLimit;

    public ContentFormatter() {
        this(DEFAULT_SOFT_LIMIT, DEFAULT_HARD_LIMIT);
    }

    public ContentFormatter(int softLimit, int hardLimit) {
        this(softLimit, hardLimit, new ComponentSanitizer());
    }

    public ContentFormatter(
            int softLimit,
            int hardLimit,
            ComponentSanitizer sanitizer
    ) {
        if (softLimit <= 0) {
            throw new IllegalArgumentException("softLimit must be greater than zero");
        }
        if (hardLimit < softLimit) {
            throw new IllegalArgumentException("hardLimit must not be less than softLimit");
        }
        this.softLimit = softLimit;
        this.hardLimit = hardLimit;
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
    }

    public int softLimit() {
        return softLimit;
    }

    public int hardLimit() {
        return hardLimit;
    }

    public Optional<FormattedContent> format(SignSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return format(snapshot.content());
    }

    public Optional<FormattedContent> format(SignContent content) {
        Objects.requireNonNull(content, "content");

        List<Component> lines = meaningfulLines(content);
        if (lines.isEmpty()) {
            return Optional.empty();
        }

        int visualLength = lines.stream().mapToInt(ContentFormatter::visualLength).sum();
        if (visualLength <= softLimit) {
            return Optional.of(new FormattedContent(lines));
        }

        int targetLimit = visualLength > hardLimit ? hardLimit : softLimit;
        return Optional.of(new FormattedContent(truncate(lines, targetLimit)));
    }

    private List<Component> meaningfulLines(SignContent content) {
        List<Component> source = content.lines();
        int firstMeaningful = -1;
        int lastMeaningful = -1;
        for (int index = 0; index < source.size(); index++) {
            if (!isBlank(source.get(index))) {
                if (firstMeaningful < 0) {
                    firstMeaningful = index;
                }
                lastMeaningful = index;
            }
        }

        if (firstMeaningful < 0) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>(lastMeaningful - firstMeaningful + 1);
        for (int index = firstMeaningful; index <= lastMeaningful; index++) {
            Component line = source.get(index);
            lines.add(isBlank(line) ? Component.empty() : sanitizer.sanitize(line));
        }
        return List.copyOf(lines);
    }

    private static boolean isBlank(Component line) {
        return PLAIN_TEXT.serialize(line).isBlank();
    }

    private List<Component> truncate(List<Component> lines, int limit) {
        int remaining = Math.max(0, limit - visualLength(Component.text(ELLIPSIS)));
        List<Component> result = new ArrayList<>();

        for (Component line : lines) {
            Clip clip = clip(line, remaining);
            result.add(clip.component());
            remaining -= clip.used();
            if (clip.used() < visualLength(line)) {
                break;
            }
        }

        if (result.isEmpty()) {
            result.add(Component.empty());
        }
        int last = result.size() - 1;
        result.set(last, result.get(last).append(Component.text(ELLIPSIS)));
        return List.copyOf(result);
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
        Component result = component;
        int used = 0;

        if (component instanceof TextComponent text) {
            String clipped = prefixByVisualLength(text.content(), remaining);
            result = text.content(clipped);
            used = visualLength(clipped);
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
            return visualLength(text.content());
        }
        return 0;
    }

    private static int visualLength(Component component) {
        String plain = PLAIN_TEXT.serialize(component);
        return visualLength(plain);
    }

    private static int visualLength(String text) {
        return (int) text.codePoints()
                .filter(codePoint -> codePoint != '\n' && codePoint != '\r')
                .count();
    }

    private static String prefixByVisualLength(String text, int limit) {
        if (limit <= 0) {
            return "";
        }

        int used = 0;
        int end = 0;
        while (end < text.length()) {
            int codePoint = text.codePointAt(end);
            int codePointLength = Character.charCount(codePoint);
            boolean lineBreak = codePoint == '\n' || codePoint == '\r';
            if (!lineBreak && used == limit) {
                break;
            }
            end += codePointLength;
            if (!lineBreak) {
                used++;
            }
        }
        return text.substring(0, end);
    }

    private record Clip(Component component, int used) {
    }
}
