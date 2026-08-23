# SignLens 0.1 UX contract

## Primary experience

SignLens is intentionally quiet. A player who merely walks past a sign or sweeps the crosshair over it should see nothing. Only a stable view for approximately 200 ms should produce an ActionBar.

```text
ordinary pass       -> no output
quick crosshair hit -> no output
stable focus        -> compact ActionBar
focus leaves        -> output clears after grace
```

The result should feel like reading assistance, not a replacement HUD competing with every other plugin.

## Content presentation

Four sign lines are joined into one compact line. Empty lines are removed. The default separator is ` · ` with subdued styling.

```text
WELCOME  ·  TO  ·  SPAWN
```

The formatter must retain the meaningful visual structure of the source components, including color and emphasis. It must not preserve interaction events in the rendered copy.

## Length policy

The first implementation should use a soft visual limit and a hard safety limit:

| Visual length | Behavior |
| ---: | --- |
| below soft limit | show complete formatted content |
| soft to hard limit | compact/truncate using an ellipsis |
| above hard limit | show a safe summary/truncated result |

The baseline hard limit is 120 visual characters. `[more]` or a deep-reading affordance is not part of 0.1.

## Focus transitions

`CANDIDATE` exists to prevent accidental flashes. `LOST_GRACE` exists to absorb small mouse movement and one-tick ray-trace misses.

Expected behavior:

| Event | Expected result |
| --- | --- |
| same sign for 100 ms | remains candidate |
| same sign for 250 ms | focused and rendered |
| miss for 100 ms | remains visible during grace |
| miss for 400 ms | focus ends and output clears |
| change to another sign | old focus ends; new candidate starts |
| player teleports/world-changes | session resets immediately |
| sign becomes empty | no output |

## Interaction compatibility

0.1 is passive. It must not consume or cancel right-click/left-click sign interactions, open a Dialog, issue commands, or change the player's inventory or camera. A later deep-reading gesture belongs to 0.2 and must be separately configurable.

## Accessibility and future direction

The renderer interface exists so a future Dialog, BossBar, resource-pack, or client-bridge renderer can be added without moving detection or reading logic. 0.1 does not promise those renderers or player preference storage.
