package io.github.la8garlic.signlens.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.la8garlic.signlens.focus.FocusController;
import io.github.la8garlic.signlens.focus.FocusState;
import io.github.la8garlic.signlens.render.RenderDecisionType;
import io.github.la8garlic.signlens.render.RenderPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.junit.jupiter.api.Test;

class PlayerSessionTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void resetClearsControllersSnapshotViewAndRenderOutput() {
        PlayerSession session = new PlayerSession(
                UUID.randomUUID(),
                new FocusController(Duration.ofMillis(100), Duration.ofMillis(100)),
                new RenderPolicy(Duration.ofMillis(100))
        );
        ViewSample view = new ViewSample(UUID.randomUUID(), 1, 2, 3, 4, 5);
        session.lastView(view);

        session.focusController().observe(
                java.util.Optional.of(new io.github.la8garlic.signlens.focus.FocusTarget(
                        view.worldId(), 1, 2, 3)),
                START
        );
        session.focusController().observe(
                java.util.Optional.of(new io.github.la8garlic.signlens.focus.FocusTarget(
                        view.worldId(), 1, 2, 3)),
                START.plusMillis(100)
        );
        session.renderPolicy().observe(java.util.Optional.of(Component.text("shown")), true, START);

        assertEquals(RenderDecisionType.CLEAR, session.reset().type());
        assertEquals(FocusState.IDLE, session.focusController().state());
        assertTrue(session.lastView().isEmpty());
        assertTrue(session.lastSnapshot().isEmpty());
        assertTrue(session.renderPolicy().lastSentContent().isEmpty());
    }

    @Test
    void replacingOrRetiringTaskCancelsThePreviousEntityTask() {
        PlayerSession session = new PlayerSession(UUID.randomUUID());
        ScheduledTask first = mock(ScheduledTask.class);
        ScheduledTask second = mock(ScheduledTask.class);

        session.scanTask(first);
        session.scanTask(second);
        verify(first).cancel();
        session.retireTask();
        verify(second).cancel();
        assertTrue(session.scanTask().isEmpty());
    }
}
