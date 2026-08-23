package io.github.la8garlic.signlens.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.la8garlic.signlens.focus.FocusController;
import io.github.la8garlic.signlens.render.RenderPolicy;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SessionRegistryTest {

    @Test
    void getOrCreateReturnsExactlyOneSessionPerPlayer() {
        SessionRegistry registry = new SessionRegistry();
        UUID playerId = UUID.randomUUID();

        PlayerSession first = registry.getOrCreate(playerId);
        PlayerSession second = registry.getOrCreate(playerId);

        assertSame(first, second);
        assertEquals(1, registry.size());
    }

    @Test
    void customFactoryAppliesRuntimeTimingSettings() {
        Duration dwell = Duration.ofMillis(350);
        Duration lostGrace = Duration.ofMillis(450);
        Duration keepalive = Duration.ofMillis(1800);
        SessionRegistry registry = new SessionRegistry(playerId -> new PlayerSession(
                playerId,
                new FocusController(dwell, lostGrace),
                new RenderPolicy(keepalive)
        ));

        PlayerSession session = registry.getOrCreate(UUID.randomUUID());

        assertEquals(dwell, session.focusController().dwell());
        assertEquals(lostGrace, session.focusController().lostGrace());
        assertEquals(keepalive, session.renderPolicy().keepalive());
    }

    @Test
    void concurrentLookupCreatesOneSessionForAPlayer() {
        UUID playerId = UUID.randomUUID();
        AtomicInteger creations = new AtomicInteger();
        SessionRegistry registry = new SessionRegistry(id -> {
            creations.incrementAndGet();
            return new PlayerSession(id);
        });

        List<PlayerSession> sessions = IntStream.range(0, 100)
                .parallel()
                .mapToObj(ignored -> registry.getOrCreate(playerId))
                .toList();

        assertTrue(sessions.stream().allMatch(session -> session == sessions.getFirst()));
        assertEquals(1, creations.get());
        assertEquals(1, registry.size());
    }

    @Test
    void removeRetiresTaskAndLeavesNoPlayer() {
        SessionRegistry registry = new SessionRegistry();
        UUID playerId = UUID.randomUUID();
        PlayerSession session = registry.getOrCreate(playerId);
        ScheduledTask task = mock(ScheduledTask.class);
        session.scanTask(task);

        assertTrue(registry.remove(playerId).isPresent());
        verify(task).cancel();
        assertTrue(registry.find(playerId).isEmpty());
        assertEquals(0, registry.size());
    }

    @Test
    void clearRetiresEveryTaskAndClearsRegistry() {
        SessionRegistry registry = new SessionRegistry();
        ScheduledTask first = mock(ScheduledTask.class);
        ScheduledTask second = mock(ScheduledTask.class);
        registry.getOrCreate(UUID.randomUUID()).scanTask(first);
        registry.getOrCreate(UUID.randomUUID()).scanTask(second);

        registry.clear();

        verify(first).cancel();
        verify(second).cancel();
        assertEquals(0, registry.size());
    }
}
