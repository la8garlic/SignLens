# Read the viewer-facing sign side into an immutable snapshot

## Problem

The renderer must not receive live Bukkit `Sign` objects or duplicate front/back orientation math. It needs safe, immutable content from the side the player is actually viewing.

## Relationship

Child of Issue 01; consumes the detected sign from Issue 02.

## Scope

- `SignKey` including world UUID, block coordinates, and side.
- `SignSnapshot` and sign content value types.
- `SignReader` using the Paper viewer-facing side API.
- Front/back handling through `SignSide.lines()` or the Paper 26.2 equivalent.
- Color/glowing metadata capture where useful to the renderer.
- Empty/whitespace-only content classification.

## Non-goals

- Formatting policy or ActionBar output.
- Global caching.
- Sign mutation or event interception.

## Acceptance criteria

- [x] Front and back content are correctly distinguished.
- [x] Standing, wall, hanging, and wall-hanging signs are covered by integration tests.
- [x] The snapshot is immutable from the renderer's point of view.
- [x] Adventure `Component` lines are preserved; no `toString()` flattening.
- [x] Empty content is represented as non-renderable.
- [x] A broken/unavailable sign ends the read cleanly.

## Verification

Run `tools/run-paper-raytrace-integration.ps1` against Paper 26.2. The real-player probe reads both sides of standing, wall, hanging, and wall-hanging signs, preserving Adventure components while checking color, formatting, glowing metadata, empty/whitespace classification, and unavailable-state handling in unit tests.
