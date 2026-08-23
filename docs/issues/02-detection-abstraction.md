# Add sign detection abstraction

## Problem

The runtime needs to identify the sign under a player's view without coupling detection to text reading, focus timing, or rendering.

## Relationship

Child of Issue 01. Establishes the first boundary in `Detector -> FocusController -> SignReader -> Renderer`.

## Scope

- `SignDetector` interface.
- `DetectedSign` value type.
- `ViewSample` value type if needed by the detector boundary.
- `RayTraceSignDetector` using the supported block ray-trace API.
- Configured maximum distance and fluid collision behavior.
- No nearby-block scan and no manual vector stepping.

## Non-goals

- Reading sign lines or side content.
- Dwell/lost-grace state.
- ActionBar output.
- Global sign indexing or caching.

## Acceptance criteria

- [ ] A ray aimed at a sign returns a detected sign.
- [ ] A miss or non-sign block returns empty.
- [ ] Detection respects the configured maximum distance.
- [ ] Detection covers standing, wall, hanging, and wall-hanging sign block states.
- [ ] Detector tests prove it does not call a nearby-block scan.
- [ ] The detector has no renderer or scheduler dependency.

## Verification

Unit-test the value boundary and use a Paper integration test for block ray tracing and collision behavior. Record any chunk-load observation for Issue 10.
