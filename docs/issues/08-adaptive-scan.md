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
- Performance release sign-off; that is Issue 10.

## Acceptance criteria

- [ ] Unchanged position/view skips ray tracing between idle probes.
- [ ] Meaningful movement or rotation triggers detection.
- [ ] Idle probe eventually rechecks the view.
- [ ] Players without `signlens.use` are skipped.
- [ ] Disabled plugin/session state causes an immediate safe return.
- [ ] Pipeline remains detector -> focus -> reader -> renderer.
- [ ] A focused player does not cause an ActionBar send merely because a scan tick ran.

## Verification

Unit-test threshold comparisons and idle scheduling. Run a Paper test with an idle player and a rotating player, recording ray-trace and ActionBar counts.
