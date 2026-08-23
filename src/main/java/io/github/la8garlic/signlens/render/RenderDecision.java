package io.github.la8garlic.signlens.render;

import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;

/** Immutable, side-effect-free render instruction. */
public record RenderDecision(RenderDecisionType type, Optional<Component> content) {

    public RenderDecision {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(content, "content");
        if (type == RenderDecisionType.SHOW && content.isEmpty()) {
            throw new IllegalArgumentException("SHOW requires content");
        }
        if (type != RenderDecisionType.SHOW && content.isPresent()) {
            throw new IllegalArgumentException("Only SHOW may carry content");
        }
    }

    public static RenderDecision none() {
        return new RenderDecision(RenderDecisionType.NONE, Optional.empty());
    }

    public static RenderDecision show(Component content) {
        return new RenderDecision(RenderDecisionType.SHOW, Optional.of(content));
    }

    public static RenderDecision clear() {
        return new RenderDecision(RenderDecisionType.CLEAR, Optional.empty());
    }
}
