# SignLens 0.1 performance plan

Status: acceptance plan; measurements must be collected before release.

## Performance model

SignLens cost should scale with active player sessions and sampled ray length, not with the total number of signs in loaded worlds.

```text
O(active viewers × sampled ray length)
```

The implementation must not call `getNearbyBlocks()` or enumerate an 8-block cube. A 17³ search is the wrong shape for a single view query.

## Polling and rendering budgets

The default scan runs every 2 ticks, but it is not equivalent to a ray trace every 2 ticks. A ray trace occurs only after meaningful position/view change or when an idle probe is due.

ActionBar output is event-like:

- focus transition: one send;
- formatted content change: one send;
- focused keepalive: approximately once per 2.5 seconds when policy permits;
- focus lost: clear once.

No code path may send an ActionBar on every tick merely because focus remains active.

## Metrics

`PerformanceCounters` should provide enough data for debug and profiling without becoming a telemetry system:

- ray traces attempted and hit/miss counts;
- ray traces skipped due to unchanged view;
- idle probes;
- snapshot reads;
- formatter invocations;
- ActionBar sends and clears;
- session count;
- last and rolling-average durations where measurement is cheap.

Counters should be local to the process and resettable on reload; no remote telemetry or database is in 0.1.

## Microbenchmarks

Benchmark or characterize, as available:

1. block ray-trace invocation;
2. snapshot creation;
3. component sanitization and formatting;
4. snapshot equality/content comparison;
5. view-change threshold checks.

Microbenchmarks guide optimization but do not establish release readiness.

## Server scenarios

Use Paper profiling tools, spark/JFR where available, and MSPT observations. Record server version, Java version, view distance, hardware, player count, and sign density.

### Scenario A: ordinary movement

```text
20 players
normal survival movement
many signs in the world
```

Target: SignLens contributes less than 0.2% of server tick time under the chosen test conditions.

### Scenario B: view churn

```text
100 synthetic players
continuous yaw/pitch movement
8-block ray trace
```

Target: no material MSPT spike attributable to SignLens. Report ray-trace rate and p95 scan duration rather than only a pass/fail label.

### Scenario C: idle players

```text
100 idle players
unchanged position and view
```

Target: ray-trace rate approaches the idle-probe floor; unchanged-view scans must be skipped.

### Scenario D: sign density

```text
1000 signs around spawn
player focuses on one sign
```

Target: cost remains approximately independent of total sign count because no nearby/world sign scan occurs.

## Chunk-load safety

Paper documents that ray tracing can cause chunk loading. Verify that the configured short ray does not cause surprising synchronous chunk loads in the tested scenarios. If evidence shows a problematic edge case, record it as a design change rather than silently adding a cache or broad preloading behavior.

## Caching decision

0.1 intentionally has no global `SignSnapshotCache`. The expected read volume is small, while invalidation would need to cover sign changes, block breaks, chunk unloads, and world unloads. Add a cache only after measurements demonstrate snapshot creation is a bottleneck, and record the decision in a new ADR.

## Release evidence

Before 0.1 release, attach a short report containing:

- environment and exact build;
- scenario inputs;
- ActionBar send rate;
- ray-trace rate and skipped-scan rate;
- average/p95 scan cost;
- MSPT comparison with SignLens disabled/enabled;
- any chunk-load observations;
- known limitations and follow-up issues.
