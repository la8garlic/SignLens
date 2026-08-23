# SignLens 0.1 architecture

Status: proposed baseline for implementation.

## Problem and boundary

SignLens is a zero-client-dependency reading aid. It observes the player's current view, recognizes a likely reading intent, extracts the visible sign side, and renders a compact readable representation. It does not replace the sign, own its content, or alter the sign's normal game behavior.

The runtime should be `O(active viewers × sampled ray length)`, not `O(signs in the world)`.

## Modules

```text
signlens
├── SignLensPlugin
├── detection
│   ├── SignDetector
│   ├── RayTraceSignDetector
│   ├── ViewSample
│   └── DetectedSign
├── focus
│   ├── FocusController
│   ├── FocusState
│   └── FocusTransition
├── sign
│   ├── SignReader
│   ├── SignSnapshot
│   ├── SignKey
│   └── SignContent
├── render
│   ├── SignRenderer
│   ├── ActionBarRenderer
│   ├── ComponentSanitizer
│   ├── ContentFormatter
│   ├── FormattedContent
│   └── RenderPolicy
├── session
│   ├── PlayerSession
│   └── SessionRegistry
├── scheduler
│   └── PlayerScanTask
├── command
│   ├── SignLensCommand
│   ├── DebugSnapshot
│   └── DebugMessageFormatter
├── config
│   └── SignLensConfig
└── metrics
    └── PerformanceCounters
```

The package names describe ownership, not a generic `listeners/managers/utils` bucket. Event listeners should translate Bukkit/Paper events into lifecycle operations and then delegate to the relevant domain object.

## Dependency rules

```text
Detector
   ↓
FocusController
   ↓
SignReader
   ↓
Renderer
```

Forbidden dependencies:

- `SignDetector -> ActionBarRenderer`
- `Renderer -> RayTraceSignDetector`
- `SignReader -> PlayerScanTask`
- `PlayerSession -> Bukkit scheduler`
- domain formatting code -> live Bukkit `Sign` or `Block`

The `SignSnapshot` boundary is important: after reading, rendering and formatting operate on immutable project-owned data. `FormattedContent` keeps the individual lines available until the selected renderer projects them onto its output surface.

## Detection

```java
Optional<DetectedSign> detect(Player player);
```

`SignDetector` answers one question: is the player currently looking at a sign? It does not read lines, track dwell, save state, or render.

`RayTraceSignDetector` should use the Paper/Bukkit block ray-trace API with a short maximum distance and the configured fluid collision mode. It must not implement a vector-step loop or search nearby blocks. Because ray tracing may load chunks, chunk-load behavior must be characterized in performance validation.

The detected value should contain only what the focus step needs, such as the sign block location/key and hit information. It should not contain a mutable Bukkit `Sign` object unless a narrow API boundary requires it.

## Focus state machine

```text
IDLE -- sign hit --> CANDIDATE -- dwell met --> FOCUSED
FOCUSED -- miss --> LOST_GRACE -- grace expired --> IDLE
LOST_GRACE -- same sign hit --> FOCUSED
```

The controller receives a timestamped detection result and returns a transition. It owns no scheduler and sends no packets.

Initial default values:

| Setting | Baseline |
| --- | ---: |
| Maximum distance | 8 blocks |
| Scan period | 2 ticks |
| Focus dwell | 200 ms |
| Lost grace | 300 ms |
| ActionBar keepalive | 2500 ms |
| Idle probe | 10 ticks |

The controller must treat a changed sign key as a new candidate. A miss during lost grace must not immediately clear a focused sign.

## Sign reading and sides

`SignReader` converts the live Paper sign state into an immutable `SignSnapshot`:

```java
record SignKey(UUID worldId, int x, int y, int z, Side side) {}

record SignSnapshot(
    SignKey key,
    List<Component> lines,
    DyeColor color,
    boolean glowing
) {}
```

The exact API types should be confirmed against the Paper 26.2 API during bootstrap. Side selection must use the viewer-facing side API where available; SignLens must not duplicate face-orientation math from block rotation and player yaw.

The reader must support standing, wall, hanging, and wall-hanging signs, including front and back sides. Empty and whitespace-only content becomes an empty snapshot and must not trigger rendering.

## Component safety

Sign text is Adventure `Component` data. Formatting must not be flattened through `Component#toString()` or by serializing everything to plain text.

`ComponentSanitizer` should preserve presentation properties such as text, color, bold, italic, underlining, strikethrough, obfuscation policy, and font where supported. It should remove or deliberately neutralize interaction metadata such as click events, hover events, and insertion. The formatter must also preserve line boundaries between meaningful sign lines. SignLens is a reading layer, so its contract is:

> Preserve presentation, not interaction.

## Rendering

```java
interface SignRenderer {
    void show(Player player, SignSnapshot snapshot);
    void clear(Player player);
}
```

`ActionBarRenderer` is the 0.1 implementation. It receives line-aware
`FormattedContent` and knows nothing about ray tracing or focus timing. Because
the native ActionBar is a single-line surface, it renders line boundaries as a
visible `↵` marker; it must never send a raw newline component. A future Dialog
renderer can consume the same line list without changing detection, reading, or
focus logic.

Rendering is edge-triggered:

- send when entering `FOCUSED`;
- send when the formatted content changes;
- send after the keepalive TTL only if the player is still actively focused and policy permits it;
- clear when the focus ends or the session is reset.

There must be no unconditional per-tick `sendActionBar` call.

## Sessions and scheduling

`PlayerSession` stores state only:

```java
final class PlayerSession {
    ViewSample lastView;
    SignKey candidate;
    long candidateSince;
    SignKey focused;
    SignSnapshot lastSnapshot;
    long lastRayTrace;
    long lastRender;
    long lastContentRefresh;
    FocusState state;
}
```

The actual fields may evolve, but the session must not become a manager god class.

On join, `SessionRegistry` creates a session and starts a player-owned task through `EntityScheduler`. On quit, teleport/world change, disable, or task retirement, the session is reset/disposed and no strong player reference is retained by the registry after removal.

Use Paper's scheduler model for shared Paper/Folia code:

- player/entity work: `EntityScheduler`;
- region-owned world work: `RegionScheduler` if later needed;
- truly global work: `GlobalScheduler` if later needed.

0.1 has no global task that loops through every online player.

## Adaptive scan

Every scan period, compare the current `ViewSample` with the last sampled position/yaw/pitch. Ray trace only when:

- the view changed beyond configured thresholds;
- an idle probe is due; or
- a lifecycle event explicitly invalidated the session.

Baseline thresholds:

```yaml
detection:
  movement:
    yaw-threshold: 0.4
    pitch-threshold: 0.4
    position-threshold: 0.05
  idle-probe:
    period-ticks: 10
```

## World safety

SignLens must never:

- spawn or manage an ArmorStand, TextDisplay, Interaction entity, or any other entity;
- modify a sign or block;
- write sign PDC or metadata;
- replace a block;
- create a global sign cache in 0.1;
- intercept, cancel, or replace ordinary sign interaction.

Disabling the plugin must leave no world-side artifact.

## Observability

`/signlens debug` exposes the current session rather than merely acknowledging
the command. It shows state, target key/side, distance, dwell, last ray-trace
age, average ray-trace time, render age, line count, visual character count,
and local counters. Permission is checked before session lookup, output is
sent only to the command sender, and the command is on-demand so it cannot
create a per-tick ActionBar stream. `debug.enabled` is the configuration gate.

`PerformanceCounters` is local and resettable. It records skipped scans, idle
probes, ray-trace hits/misses and duration, snapshot/formatter work, and
ActionBar sends/clears for Issue 11 validation.

## Testing boundary

Pure Java tests should cover `FocusController`, `ContentFormatter`, `RenderPolicy`, and view-change detection. Paper integration tests should cover all four sign shapes, both sides, empty/colored/formatted/glowing signs, lifecycle reset, broken signs, and ordinary interaction preservation.

The implementation should remain free of NMS and ProtocolLib so the public API surface stays on supported Paper APIs.
