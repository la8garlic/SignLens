# Validate SignLens 0.1 performance and release readiness

## Problem

“Lightweight” is not an acceptance criterion. The implementation needs evidence for ray-trace frequency, scan cost, ActionBar send rate, MSPT impact, and chunk-load behavior.

## Relationship

Final acceptance child of Issue 01; validates the integrated runtime from Issues 02–10.

## Scope

- Microbenchmarks/characterization for ray tracing, snapshot creation, formatting, equality, and view checks where practical.
- Paper server scenarios: 20 normal players, 100 rotating synthetic players, 100 idle players, and 1000 signs with one active reader.
- spark/JFR/Paper MSPT evidence.
- Enabled/disabled comparison and environment capture.
- Short report in `docs/performance.md` or a linked report artifact.
- Release checklist against functional, compatibility, world-safety, performance, and quality criteria.

## Non-goals

- Optimizing from an unrepresentative microbenchmark alone.
- Adding a global cache without evidence.
- Claiming a universal CPU guarantee across all hardware.

## Acceptance criteria

- [x] Idle unchanged-view scans show a reduced ray-trace rate.
- [x] ActionBar sends are transition/TTL driven, not per tick.
- [x] No nearby-block scan is present in the runtime path.
- [x] Scenario A target is evaluated and reported.
- [x] Scenario B shows no material SignLens MSPT spike under documented conditions.
- [x] Scenario C approaches the idle-probe floor.
- [x] Scenario D is approximately independent of total sign count.
- [x] Ray-trace chunk-load behavior is documented.
- [x] Known limitations and follow-up work are recorded.

## Verification

See [`docs/performance.md`](../performance.md) for reproducible commands,
exact Paper/Java/hardware details, scenario results, scan/ray-trace rates,
ActionBar counts, tick-interval controls, chunk-load limitations, and the
release checklist. This Issue is the 0.1 performance gate, not merely a
documentation task.
