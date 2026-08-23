package io.github.la8garlic.signlens.render;

import io.github.la8garlic.signlens.metrics.PerformanceCounters;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Paper ActionBar implementation; cadence decisions remain in {@link RenderPolicy}. */
public final class ActionBarRenderer implements SignRenderer {

    private final PerformanceCounters counters;

    public ActionBarRenderer() {
        this(new PerformanceCounters());
    }

    public ActionBarRenderer(PerformanceCounters counters) {
        this.counters = Objects.requireNonNull(counters, "counters");
    }

    @Override
    public void show(Player player, FormattedContent content) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(content, "content");
        player.sendActionBar(content.toActionBarComponent());
        counters.recordActionBarSend();
    }

    @Override
    public void clear(Player player) {
        Objects.requireNonNull(player, "player");
        player.sendActionBar(Component.empty());
        counters.recordActionBarClear();
    }
}
