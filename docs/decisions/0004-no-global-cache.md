# ADR-0004: Do not add a global sign cache before measurement

- Status: accepted for 0.1
- Date: 2026-08-23

## Context

The initial read volume is one focused sign per active viewer. A global cache would require invalidation for sign changes, block breaks, chunk unloads, and world unloads, while introducing stale-data and lifecycle failure modes.

## Decision

Keep only per-player state and the last per-session snapshot in 0.1. Measure first. Add a global cache only if profiling demonstrates snapshot creation is a real bottleneck.

## Consequences

Positive:

- simple correctness model;
- no global invalidation network;
- no stale cross-player content;
- easier disable/reload behavior.

Trade-off:

- two players reading the same sign may independently create snapshots.

## Follow-up gate

Any future cache requires a new ADR describing keying, invalidation, lifecycle, and benchmark evidence.
