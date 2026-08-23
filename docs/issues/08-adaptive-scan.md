# Add adaptive per-player scanning

## Problem

Scanning every player with a ray trace every tick is wasteful, especially for AFK players whose position and view have not changed.

## Relationship

Child of Issue 01; integrates Issues 02, 04, 06, and 07 into the first runtime loop.

## Scope

- `PlayerScanTask` running at the configured scan period, initially 2 ticks.
- Position/yaw/pitch `ViewSample` comparison.
- Configured movement thresholds.
- Idle probe, initially 10 ticks.
- Permission and enabled checks.
- Focus/read/render pipeline invocation in dependency order.
- Scan invalidation on lifecycle reset.

## Non-goals

- Nearby block scans.
- Global sign cache.
- Per-tick ActionBar sends.
- Performance release sign-off; that is Issue 11.

## Acceptance criteria

- [x] Unchanged position/view skips ray tracing between idle probes.
- [x] Meaningful movement or rotation triggers detection.
- [x] Idle probe eventually rechecks the view.
- [x] Players without `signlens.use` are skipped.
- [x] Disabled plugin/session state causes an immediate safe return.
- [x] Pipeline remains detector -> focus -> reader -> renderer.
- [x] A focused player does not cause an ActionBar send merely because a scan tick ran.

## Verification

Unit tests cover position/rotation/world thresholds, yaw wrap-around, and idle scheduling. The Paper 26.2 integration probe passes all four sign shapes, both reader sides, and `RUNTIME_PIPELINE=PASS` after a real player join, teleport, entity-owned scan, focus dwell, read, format, and render-policy pass. The pure runtime test verifies unchanged view scans do not re-ray-trace or resend ActionBar content.
