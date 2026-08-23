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

- [ ] Authorized players can run `/signlens debug`.
- [ ] Unauthorized players are denied without leaking session data.
- [ ] Focused output includes the agreed diagnostic fields.
- [ ] Miss/no-focus output explains the current high-level state.
- [ ] Coordinates and world identifiers are readable but not exposed to other players.
- [ ] Command output does not alter focus or rendering state.

## Verification

Command tests cover permission, focused, candidate, idle, and no-session states. Manual Paper verification confirms the output is useful while investigating a distance or dwell failure.
