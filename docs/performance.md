# SignLens 0.1 performance plan and release evidence

Status: Issue 11 validation completed on 2026-08-23 for the documented local
environment. The measurements are characterization evidence, not a universal
CPU or MSPT guarantee.

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

Counters should be local to the process and resettable by the validation
harness; no remote telemetry or database is in 0.1.

Issue 11 adds scan-cycle timing to the existing local counters. The runtime
keeps a fixed 256-sample window for scan p95, so the measurement path has
bounded memory and does not become a telemetry system. `/signlens debug`
reports scan count, average scan time, p95 scan time, ray-trace average, and
ActionBar counts.

## Microbenchmarks

Benchmark or characterize, as available:

1. block ray-trace invocation;
2. snapshot creation;
3. component sanitization and formatting;
4. snapshot equality/content comparison;
5. view-change threshold checks.

Microbenchmarks guide optimization but do not establish release readiness.

The release evidence below uses the real Paper pipeline instead of a synthetic
microbenchmark. Unit tests characterize counter math and the existing tests
cover view thresholds, formatter work, snapshot equality, and ActionBar cadence.

## Reproducible validation harness

The integration probe can run a local multi-client workload against Paper
26.2 build 112:

```powershell
./tools/run-paper-performance.ps1 -Scenario ordinary -PlayerCount 20 -DurationSeconds 10
./tools/run-paper-performance.ps1 -Scenario churn -PlayerCount 100 -DurationSeconds 10
./tools/run-paper-performance.ps1 -Scenario idle -PlayerCount 100 -DurationSeconds 10
./tools/run-paper-performance.ps1 -Scenario sign-density -PlayerCount 1 -DurationSeconds 10
```

The harness waits five seconds before resetting counters and sampling. It
records the active-player count, sign count, scan/ray-trace rates, ActionBar
counts, average/p95 scan cost, and average/p95 wall-clock tick interval. The
tick interval is a local MSPT proxy; use spark or JFR for a release audit on a
different host. Add `-SignLensEnabled $false` for an enabled/disabled control
run.

## Server scenarios

Use Paper profiling tools, spark/JFR where available, and MSPT observations. Record server version, Java version, view distance, hardware, player count, and sign density.

### Scenario A: ordinary movement

```text
20 players
normal survival movement
many signs in the world
```

Target: SignLens contributes less than 0.2% of server tick time under the chosen test conditions.

Measured with 20 protocol clients for 10 seconds, with a 20-client disabled
control on the same machine:

| SignLens | scans | ray traces | skipped | avg scan | p95 scan | avg tick interval |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| enabled | 2,000 | 780 (78/s) | 1,220 (122/s) | 0.005 ms | 0.019 ms | 49.997 ms |
| disabled | 2,000 | 0 | 0 | 0.001 ms | 0.001 ms | 49.989 ms |

The observed enabled-minus-disabled interval was 0.008 ms, approximately
0.016% of the 49.989 ms control interval, below the 0.2% target for this
sample. It is not a cross-hardware guarantee.

### Scenario B: view churn

```text
100 synthetic players
continuous yaw/pitch movement
8-block ray trace
```

Target: no material MSPT spike attributable to SignLens. Report ray-trace rate and p95 scan duration rather than only a pass/fail label.

Measured with 100 protocol clients rotating their view every 100 ms for 10
seconds. SignLens recorded 10,000 scans, 9,400 ray traces (940/s), 600
unchanged-view skips, 0.002 ms average scan time, 0.002 ms p95 scan time, and
0.001 ms average ray-trace time. The enabled tick interval was 49.994 ms
(p95 51.299 ms); the disabled control was 49.993 ms (p95 51.107 ms). This
short local run showed no material spike.

### Scenario C: idle players

```text
100 idle players
unchanged position and view
```

Target: ray-trace rate approaches the idle-probe floor; unchanged-view scans must be skipped.

Measured with 100 unchanged protocol clients for 10 seconds: 10,000 scans,
2,000 ray traces (200/s, exactly two per player per second), 8,000 skips, and
2,000 idle probes. This is the configured ten-tick idle-probe floor.

### Scenario D: sign density

```text
1000 signs around spawn
player focuses on one sign
```

Target: cost remains approximately independent of total sign count because no nearby/world sign scan occurs.

Measured with one player and 1,000 placed signs for 10 seconds: 100 scans,
38 ray traces, 62 skips, 0.082 ms average scan time, 0.156 ms p95 scan time,
and 0.088 ms average ray-trace time. The one-player, zero-sign idle control
recorded 0.067 ms average scan time, 0.161 ms p95 scan time, and 0.098 ms
average ray-trace time. The result is consistent with no cost proportional to
the sign count; the sample is too short to treat the small timing difference
as a benchmark result.

## Chunk-load safety

Paper documents that block ray tracing can cause chunk loading. SignLens uses a
single `rayTraceBlocks(8.0, FluidCollisionMode.NEVER)` call and does not add
preloading or a global cache. The local harness uses view distance 4 and a
short eight-block ray; no chunk-load workaround was added. Chunk-load counts
are not exposed by the Paper API path used here, so a production release audit
should supplement this report with spark/JFR or server-level chunk-load data.

## Caching decision

0.1 intentionally has no global `SignSnapshotCache`. The expected read volume is small, while invalidation would need to cover sign changes, block breaks, chunk unloads, and world unloads. Add a cache only after measurements demonstrate snapshot creation is a bottleneck, and record the decision in a new ADR.

## Release evidence

Environment for the measurements above:

- Windows 10 Enterprise 10.0.19045.
- AMD Ryzen 5 5600, 6 cores / 12 logical processors, approximately 16 GiB RAM.
- Microsoft OpenJDK `25.0.4+7-LTS`.
- Paper `26.2` build `112`; plugin API `26.2.build.112-stable`.
- `view-distance=4`, `simulation-distance=4`, 10-second measurement windows
  after a five-second warm-up, offline protocol clients, no spark plugin.
- ActionBar sends and clears were zero in miss-only load scenarios. Existing
  focus/render tests verify transition and keepalive cadence; no per-tick send
  path exists.

Acceptance status:

- [x] Idle unchanged-view scans reduce the ray-trace rate.
- [x] ActionBar sends are transition/TTL driven, not per tick.
- [x] The runtime contains no nearby-block scan.
- [x] Scenario A was evaluated and reported.
- [x] Scenario B showed no material local tick-interval spike.
- [x] Scenario C reached the idle-probe floor.
- [x] Scenario D was compared against a zero-sign control.
- [x] Ray-trace chunk-load behavior and its measurement limitation are documented.
- [x] Known limitations and follow-up work are recorded.

Known limitations and follow-up work:

- The harness measures wall-clock tick intervals, not a profiler-attributed
  SignLens MSPT slice. Operators can add spark/JFR when validating a different
  production host or workload.
- The p95 scan value is a bounded recent sample, not a long-term percentile.
- The scenarios use offline protocol clients and do not model vanilla client
  rendering, network latency, or a production world generator.
- No global sign snapshot cache is justified by these measurements; revisit
  only with a demonstrated snapshot-creation bottleneck and a new ADR.
