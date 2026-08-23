# Implement the focus state machine

## Problem

Immediate rendering on every ray-trace hit flashes while a player sweeps the crosshair. Immediate clearing on one miss makes small view jitter unpleasant.

## Relationship

Child of Issue 01; consumes detection results from Issue 02 and emits transitions for later reader/render integration.

## Scope

- `FocusState`: `IDLE`, `CANDIDATE`, `FOCUSED`, and `LOST_GRACE`.
- `FocusController` with configurable dwell and lost-grace durations.
- `FocusTransition` result type.
- Candidate reset when the sign key changes.
- Focus reset on session/world/teleport invalidation.

## Non-goals

- Scheduling scans.
- Reading sign components.
- Sending or clearing ActionBars.

## Acceptance criteria

- [x] A short hit remains `CANDIDATE` and does not focus.
- [x] Stable observation beyond dwell emits a focus transition once.
- [x] A miss inside lost grace does not immediately clear focus.
- [x] A continued miss beyond lost grace emits a clear/end transition.
- [x] Switching signs cannot reuse the old candidate dwell.
- [x] Repeated identical inputs do not emit duplicate transitions.

## Verification

Pure unit tests cover deterministic candidate, dwell, grace-recovery, full-loss, target-switch, reset, duplicate-transition, and timestamp-validation scenarios. The durations are configurable and the default baseline is 200 ms dwell with 300 ms lost grace.
