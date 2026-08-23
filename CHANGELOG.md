# Changelog

All notable changes to SignLens are documented in this file.

## 0.1.0 - 2026-08-23

### Added

- Passive sign detection using a short Paper block ray trace.
- Dwell and lost-grace focus states to avoid accidental ActionBar flashes.
- Front/back reading for standing, wall, hanging, and wall-hanging signs.
- Immutable, Adventure-native sign snapshots with presentation formatting
  preserved and interaction metadata removed.
- Line-aware formatting with a visible `↵` marker for the single-row native
  ActionBar.
- Edge-triggered rendering with configurable keepalive timing.
- Player-owned Paper/Folia scheduler tasks and adaptive idle probing.
- On-demand `/signlens debug` session and performance diagnostics.
- Automated unit, integration-probe, and real Paper validation harnesses.
- Reproducible performance scenarios for ordinary movement, view churn, idle
  players, and high sign density.

### Known limitations

- Exact stacked multi-line output is deferred to a future Dialog renderer.
- The supported runtime is Paper 26.2 on Java 25.
- Configuration changes require a server restart.
- Ray tracing may load chunks at the edge of the server view distance; no
  nearby-block scan or global cache is used.
