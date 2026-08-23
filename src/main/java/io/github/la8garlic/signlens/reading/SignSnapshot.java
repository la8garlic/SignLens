package io.github.la8garlic.signlens.reading;

import java.util.Objects;

/** Immutable renderer-facing snapshot of one viewer-facing sign side. */
public record SignSnapshot(SignKey key, SignContent content) {

    public SignSnapshot {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(content, "content");
    }

    public boolean renderable() {
        return content.renderable();
    }
}
