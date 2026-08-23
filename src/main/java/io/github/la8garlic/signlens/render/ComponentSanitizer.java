package io.github.la8garlic.signlens.render;

import java.util.Objects;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;

/** Preserves presentation while removing interaction metadata from a component tree. */
public final class ComponentSanitizer {

    public Component sanitize(Component component) {
        Objects.requireNonNull(component, "component");

        Style safeStyle = component.style().toBuilder()
                .clickEvent(null)
                .hoverEvent(null)
                .insertion(null)
                .build();

        return component
                .style(safeStyle)
                .children(component.children().stream()
                        .map(this::sanitize)
                        .collect(Collectors.toUnmodifiableList()));
    }
}
