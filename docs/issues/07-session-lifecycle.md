# Add player session lifecycle and entity-owned scan task boundary

## Problem

Per-player focus and rendering state must not leak across quits, teleports, world changes, reloads, or plugin disable. A global task iterating all players also works against the Paper/Folia scheduling model.

## Relationship

Child of Issue 01; hosts the runtime state that later adaptive scanning uses.

## Scope

- `PlayerSession` state holder.
- `SessionRegistry` create/get/remove operations.
- Join/quit lifecycle integration.
- Session reset for teleport and world change.
- Player-owned `EntityScheduler` task start/retire behavior.
- Plugin disable cleanup.

## Non-goals

- Implementing the detector or focus algorithm.
- A global scheduler that loops over all players.
- Persistent player preferences.

## Acceptance criteria

- [ ] Join creates exactly one session for a player.
- [ ] Quit removes the session and retires its task.
- [ ] Teleport/world change resets candidate, focus, snapshot, and view state.
- [ ] Disable retires all tasks and clears the registry.
- [ ] The registry retains no player after removal.
- [ ] Shared Paper/Folia code uses the player/entity scheduler boundary.

## Verification

Lifecycle tests use fake sessions/tasks where possible; Paper integration verifies join, quit, teleport, world change, and disable behavior.
