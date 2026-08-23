# Add `/signlens debug` observability

## Problem

When a sign does not trigger, users need to distinguish distance, focus timing, permission, view stability, and rendering policy problems without guessing.

## Relationship

Child of Issue 01; reads session/counter state from Issues 07 and 08 without owning runtime behavior.

## Scope

- `/signlens debug` command and permission.
- On-demand current-player diagnostic output.
- Debug enable/disable behavior according to config/permission.
- State, target world/coordinates/side, distance, dwell, last ray trace age, average ray-trace time where available, last render age, line count, and visual character count.
- Clear indication when there is no active session/focus.

## Non-goals

- Remote telemetry.
- A web panel or persistent metrics database.
- Sending diagnostic ActionBars every tick.

## Acceptance criteria

- [x] Authorized players can run `/signlens debug` when `debug.enabled` is true.
- [x] Unauthorized players are denied without leaking session data.
- [x] Focused output includes state, target side/coordinates, distance, dwell,
      last/average ray-trace timing, render age, content counts, and counters.
- [x] Miss/no-focus output explains the current high-level state and reports
      `target: none` when no session is active.
- [x] Coordinates and world identifiers are readable only in the sender's
      on-demand response; the command never broadcasts session data.
- [x] Command output does not alter focus, session, or rendering state.

## Verification

Command tests cover permission, focused, configuration-disabled, and no-session
states. The local Paper preview can be used to inspect the output while
investigating a distance or dwell failure; performance counters are also
consumed by Issue 11 validation.
