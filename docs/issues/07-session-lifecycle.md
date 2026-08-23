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

- [x] Join creates exactly one session for a player.
- [x] Quit removes the session and retires its task.
- [x] Teleport/world change resets candidate, focus, snapshot, and view state.
- [x] Disable retires all tasks and clears the registry.
- [x] The registry retains no player after removal.
- [x] Shared Paper/Folia code uses the player/entity scheduler boundary.

## Verification

Lifecycle unit tests use fake sessions/tasks for singleton creation, reset, retirement, and registry cleanup. Plugin event handlers bind join, quit, teleport, and world-change events to the registry; the real Paper lifecycle pass is included with the first runtime integration.
