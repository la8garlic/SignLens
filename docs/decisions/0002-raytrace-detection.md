# ADR-0002: Detect the sign under the player's view with ray tracing

- Status: accepted for 0.1
- Date: 2026-08-23

## Context

The feature is about reading what a player is looking at. Searching nearby blocks and calculating angles scans thousands of irrelevant blocks and makes cost depend on local sign density.

## Decision

Use the supported Paper/Bukkit block ray-trace API with a short configured maximum distance. The detector returns a sign hit or miss and performs no content reading or rendering.

## Consequences

Positive:

- the query matches player intent;
- cost is tied to ray length, not nearby block volume;
- block collision shapes are delegated to the server API;
- no world-wide or nearby-sign index is needed.

Risks and mitigations:

- ray tracing may cause chunk loading; characterize it in the performance test plan;
- repeated calls can still be expensive; use view-change thresholds and idle probes.

## Rejected alternatives

- `getNearbyBlocks()` plus angle checks: unnecessary work and density-sensitive cost.
- manual vector stepping: duplicates collision logic and is less precise.
- global sign indexing: premature invalidation and lifecycle complexity.
