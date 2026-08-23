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

- [ ] A short hit remains `CANDIDATE` and does not focus.
- [ ] Stable observation beyond dwell emits a focus transition once.
- [ ] A miss inside lost grace does not immediately clear focus.
- [ ] A continued miss beyond lost grace emits a clear/end transition.
- [ ] Switching signs cannot reuse the old candidate dwell.
- [ ] Repeated identical inputs do not emit duplicate transitions.

## Verification

Pure unit tests cover 100 ms candidate, 250 ms focus, 100 ms grace miss, and 400 ms full loss scenarios with deterministic timestamps.
