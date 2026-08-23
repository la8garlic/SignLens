# Add edge-triggered ActionBar rendering

## Problem

Sending ActionBar text every tick wastes packets and overwrites other plugins' HUD output. SignLens should render only when the user's reading state or content changes.

## Relationship

Child of Issue 01; consumes formatted content from Issue 05 and focus transitions from Issue 04.

## Scope

- `SignRenderer` interface.
- `ActionBarRenderer` implementation.
- `RenderPolicy` for focus entry, content change, keepalive, and clear.
- Configured keepalive TTL, initially about 2500 ms.
- One clear operation when focus ends.

## Non-goals

- Dialog, BossBar, TextDisplay, or resource-pack renderers.
- Per-tick resending.
- ActionBar ownership or coordination with unrelated plugins.

## Acceptance criteria

- [ ] Focus entry sends once.
- [ ] Stable focus does not send once per tick.
- [ ] Content change sends the new content once.
- [ ] Keepalive is bounded by the configured TTL and only applies while focused.
- [ ] Focus end clears once.
- [ ] Empty snapshots do not send visible content.
- [ ] Renderer depends on `SignSnapshot`/formatted content, not Bukkit ray tracing.

## Verification

Unit-test `RenderPolicy` with fake clock/timestamps and an in-memory renderer spy. Include an assertion on send count for a multi-second stable focus.
