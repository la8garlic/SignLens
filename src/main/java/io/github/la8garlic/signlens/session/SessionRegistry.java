package io.github.la8garlic.signlens.session;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Owns one lifecycle-bound session per online player. */
public final class SessionRegistry {

    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    public PlayerSession getOrCreate(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return sessions.computeIfAbsent(playerId, PlayerSession::new);
    }

    public PlayerSession getOrCreate(Player player) {
        Objects.requireNonNull(player, "player");
        return getOrCreate(player.getUniqueId());
    }

    public Optional<PlayerSession> find(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return Optional.ofNullable(sessions.get(playerId));
    }

    public Optional<PlayerSession> remove(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        PlayerSession removed = sessions.remove(playerId);
        if (removed != null) {
            removed.retireTask();
        }
        return Optional.ofNullable(removed);
    }

    public Optional<PlayerSession> remove(Player player) {
        Objects.requireNonNull(player, "player");
        return remove(player.getUniqueId());
    }

    public void clear() {
        sessions.values().forEach(PlayerSession::retireTask);
        sessions.clear();
    }

    public int size() {
        return sessions.size();
    }
}
