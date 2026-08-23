package io.github.la8garlic.signlens.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.la8garlic.signlens.SignLensPlugin;
import io.github.la8garlic.signlens.focus.FocusTarget;
import io.github.la8garlic.signlens.metrics.PerformanceCounters;
import io.github.la8garlic.signlens.reading.SignContent;
import io.github.la8garlic.signlens.reading.SignKey;
import io.github.la8garlic.signlens.reading.SignSnapshot;
import io.github.la8garlic.signlens.render.FormattedContent;
import io.github.la8garlic.signlens.session.PlayerSession;
import io.github.la8garlic.signlens.session.SessionRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.DyeColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SignLensCommandTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void deniesUnauthorizedSenderBeforeReadingSessionData() {
        SignLensPlugin plugin = mock(SignLensPlugin.class);
        Player sender = mock(Player.class);
        when(sender.hasPermission(SignLensCommand.DEBUG_PERMISSION)).thenReturn(false);

        new SignLensCommand(plugin).onCommand(sender, mock(Command.class), "signlens", new String[]{"debug"});

        String message = sentMessage(sender);
        assertTrue(message.contains("permission"));
        verify(plugin, never()).sessions();
    }

    @Test
    void reportsFocusedStateAndDiagnosticFieldsToAuthorizedPlayer() {
        SignLensPlugin plugin = mock(SignLensPlugin.class);
        Player sender = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        UUID worldId = UUID.randomUUID();
        SessionRegistry registry = new SessionRegistry();
        PerformanceCounters counters = new PerformanceCounters();
        PlayerSession session = registry.getOrCreate(playerId);
        FocusTarget target = new FocusTarget(worldId, 1, 64, 2);
        session.focusController().observe(Optional.of(target), START);
        session.focusController().observe(Optional.of(target), START.plusMillis(200));
        SignSnapshot snapshot = new SignSnapshot(
                new SignKey(worldId, 1, 64, 2, org.bukkit.block.sign.Side.FRONT),
                new SignContent(List.of(Component.text("123"), Component.text("456")), DyeColor.WHITE, false)
        );
        session.lastSnapshot(snapshot);
        session.recordRayTrace(START.plusMillis(90), 1_000_000L, OptionalDouble.of(2.5));
        session.renderPolicy().observe(
                Optional.of(new FormattedContent(List.of(Component.text("123"), Component.text("456")))),
                true,
                START.plusMillis(200)
        );
        counters.recordScan(1_000_000L);
        counters.recordRayTrace(true, 1_000_000L);

        when(sender.getUniqueId()).thenReturn(playerId);
        when(sender.hasPermission(SignLensCommand.DEBUG_PERMISSION)).thenReturn(true);
        when(plugin.debugEnabled()).thenReturn(true);
        when(plugin.sessions()).thenReturn(registry);
        when(plugin.performanceCounters()).thenReturn(counters);

        new SignLensCommand(plugin).onCommand(sender, mock(Command.class), "signlens", new String[]{"debug"});

        String message = sentMessage(sender);
        assertTrue(message.contains("state: FOCUSED"));
        assertTrue(message.contains(worldId.toString()));
        assertTrue(message.contains("1,64,2"));
        assertTrue(message.contains("side=FRONT"));
        assertTrue(message.contains("distance=2.50 blocks"));
        assertTrue(message.contains("lines=2, chars=6"));
        assertTrue(message.contains("p95-scan=1.000 ms"));
        assertTrue(message.contains("avg-ray=1.000 ms"));
        assertTrue(message.contains("read-unavailable=0"));
        assertTrue(message.contains("read-failures=0"));
    }

    @Test
    void reportsNoSessionWithoutExposingCoordinates() {
        SignLensPlugin plugin = mock(SignLensPlugin.class);
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(UUID.randomUUID());
        when(sender.hasPermission(SignLensCommand.DEBUG_PERMISSION)).thenReturn(true);
        when(plugin.debugEnabled()).thenReturn(true);
        when(plugin.sessions()).thenReturn(new SessionRegistry());
        when(plugin.performanceCounters()).thenReturn(new PerformanceCounters());

        new SignLensCommand(plugin).onCommand(sender, mock(Command.class), "signlens", new String[]{"debug"});

        String message = sentMessage(sender);
        assertTrue(message.contains("state: IDLE"));
        assertTrue(message.contains("target: none"));
        assertTrue(message.contains("content: lines=0, chars=0"));
        assertTrue(!message.contains("side="));
    }

    @Test
    void configurationCanDisableDebugOutputWithoutReadingSession() {
        SignLensPlugin plugin = mock(SignLensPlugin.class);
        Player sender = mock(Player.class);
        when(sender.hasPermission(SignLensCommand.DEBUG_PERMISSION)).thenReturn(true);
        when(plugin.debugEnabled()).thenReturn(false);

        new SignLensCommand(plugin).onCommand(sender, mock(Command.class), "signlens", new String[]{"debug"});

        assertTrue(sentMessage(sender).contains("disabled"));
        verify(plugin, never()).sessions();
    }

    private static String sentMessage(Player sender) {
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(captor.capture());
        return PlainTextComponentSerializer.plainText().serialize(captor.getValue());
    }
}
