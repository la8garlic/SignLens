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

- [ ] Front and back content are correctly distinguished.
- [ ] Standing, wall, hanging, and wall-hanging signs are covered by integration tests.
- [ ] The snapshot is immutable from the renderer's point of view.
- [ ] Adventure `Component` lines are preserved; no `toString()` flattening.
- [ ] Empty content is represented as non-renderable.
- [ ] A broken/unavailable sign ends the read cleanly.

## Verification

Test a player on both sides of a sign and test colored, formatted, glowing, empty, and whitespace-only signs on Paper 26.2.
