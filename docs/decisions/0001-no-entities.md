# ADR-0001: Do not create entities

- Status: accepted for 0.1
- Date: 2026-08-23

## Context

SignLens needs to present sign content, but the world itself must remain unchanged. ArmorStands, TextDisplays, and Interaction entities introduce tracking, cleanup, visual clutter, and lifecycle complexity.

## Decision

SignLens MUST NOT create or manage entities for reading output. The 0.1 renderer uses the player's ActionBar only.

## Consequences

Positive:

- disabling the plugin leaves no entity residue;
- no entity tracking or persistence burden;
- no visual replacement of the source sign;
- no cleanup race during reload or world unload.

Trade-off:

- the ActionBar is a constrained rendering surface and can compete with other plugins, so edge-triggered policy is required.

## Rejected alternatives

- ArmorStand/TextDisplay text: violates world-safety and adds entity cost.
- persistent display entities: makes SignLens own world state.
- resource-pack-only output: violates the zero-client-dependency goal.
