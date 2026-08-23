package io.github.la8garlic.signlens.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
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
