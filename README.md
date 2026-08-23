# SignLens

> A lightweight, zero-client world text accessibility layer for Paper.

SignLens helps a player read Minecraft signs without changing the world, adding client mods, or taking ownership of sign interactions. It detects the sign under the player's view, waits until the player appears to be reading it, and presents the sign's content in an ActionBar.

## 0.1 scope

The first release is intentionally a passive reader:

- Paper 26.2 as the first supported server target.
- Java 25, as required by the Paper 26.1+ target line.
- Standing, wall, hanging, and wall-hanging signs.
- Front/back side selection through the Paper API.
- Adventure `Component` content with presentation formatting preserved.
- Focus detection with dwell and lost-grace states.
- Edge-triggered ActionBar rendering; no per-tick ActionBar spam.
- Adaptive per-player ray tracing with an idle probe.
- Paper/Folia-compatible scheduling through player-owned `EntityScheduler` tasks.
- No NMS, ProtocolLib, resource pack, database, generated entity, block mutation, or sign interaction interception.

Dialog/deep-reading gestures are explicitly deferred to 0.2.

## Development

Requirements: Java 25 or newer and no separately installed Gradle is needed.

On Windows:

```powershell
./gradlew.bat clean build
```

On macOS/Linux:

```bash
./gradlew clean build
```

The plugin JAR is written to `build/libs/SignLens-0.1.0.jar`.

## Runtime flow

```text
Player EntityScheduler
        |
        v
  PlayerScanTask -- view changed? --> RayTraceSignDetector
        |                                  |
        |                                  v
        +--------------------------> FocusController
                                           |
                                  focused sign confirmed
                                           v
                                      SignReader
                                           v
                                      SignSnapshot
                                           v
                                 ContentFormatter
                                           v
                                   RenderPolicy
                                           v
                                  ActionBarRenderer
```

The dependency direction is deliberate:

```text
Detector -> FocusController -> SignReader -> Renderer
```

The detector does not read text or render. The renderer does not ray trace. The reader does not schedule tasks.

## Configuration baseline

```yaml
enabled: true

detection:
  max-distance: 8.0
  scan-period-ticks: 2

focus:
  dwell-millis: 200
  lost-grace-millis: 300

render:
  mode: action-bar
  separator: " · "
  max-length: 120
  keepalive-millis: 2500

performance:
  idle-probe-ticks: 10

debug:
  enabled: false
```

These defaults are experiment starting points, not a promise that the values are final. They must be validated in-game and with profiling before 0.1 is released.

## Commands and permissions

```text
/signlens
/signlens toggle
/signlens reload
/signlens debug
```

```text
signlens.use                 (default: true)
signlens.command.toggle
signlens.command.reload
signlens.command.debug
```

## Project status

This repository contains the 0.1 engineering specification and a locally validated Issue 01 Paper/Java bootstrap. Sign-reading runtime behavior is intentionally still deferred to Issues 02–10. See:

- [Architecture](docs/architecture.md)
- [UX contract](docs/ux.md)
- [Performance plan](docs/performance.md)
- [Architecture decision records](docs/decisions/)
- [Issue series](docs/issues/)

## Non-negotiable invariants

1. The Minecraft world remains the source of truth.
2. SignLens never edits signs, blocks, PDC, metadata, or entities.
3. SignLens never intercepts or cancels ordinary sign interaction in 0.1.
4. Detection follows the player's view ray; it never scans nearby blocks.
5. ActionBar output is sent on focus/content transitions and an infrequent keepalive only when policy requires it.
6. A session owns state only; controllers own behavior.
7. No global sign cache is introduced before profiling demonstrates a need.

## References

- [Minecraft Java Edition 26.2](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2)
- [Supporting Paper and Folia](https://docs.papermc.io/paper/dev/folia-support/)
- [Paper Dialog API](https://docs.papermc.io/paper/dev/dialogs/)
- [Paper plugin configuration](https://docs.papermc.io/paper/dev/plugin-configurations/)
- [Paper commands](https://docs.papermc.io/paper/reference/commands/)
