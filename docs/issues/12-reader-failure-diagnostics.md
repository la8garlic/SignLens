# Make `PaperSignReader` failures diagnosable without breaking scans

## Problem

`PaperSignReader` must tolerate a sign becoming unavailable between detection
and reading, but catching every `RuntimeException` currently hides unexpected
Paper API or plugin defects. Operators cannot tell a normal stale-sign race
from a real reader failure.

## Relationship

This is a 0.1.x reliability follow-up after the completed 0.1.0 Issue 11
release gate. It preserves the Issue 03 reader contract and the Issue 10
on-demand diagnostics surface.

## Scope

- Treat `IllegalStateException` and `IllegalArgumentException` as expected
  transient sign unavailability.
- Count expected unavailability and unexpected reader failures separately.
- Keep unexpected failures safe for the player scan, but report them through
  the plugin logger at most once per 30 seconds.
- Show both counters in `/signlens debug`.

## Non-goals

- No remote telemetry, persistent error store, or per-player preference.
- No NMS, version-specific workaround, Dialog work, or change to sign-side
  selection.
- No log line for ordinary non-sign blocks or expected stale/unavailable state.

## Acceptance criteria

- [x] Expected sign-state failures return empty and increment the unavailable
  counter.
- [x] Unexpected runtime failures return empty, increment a separate failure
  counter, and are rate-limited to one report per 30 seconds.
- [x] Reader failures do not terminate the player scan pipeline or leave stale
  rendered content active.
- [x] `/signlens debug` includes both reader counters.
- [x] Unit tests cover expected failures, unexpected failures, and rate
  limiting.
- [x] Full Gradle and integration-probe build passes.

## Verification

Verification completed with `./gradlew.bat clean build integrationProbe
--console=plain --no-daemon`, the focused reader/metrics/command tests, and the
real Paper 26.2 build 112 protocol probe. The probe reported all four sign
shapes, both reader sides, and `RUNTIME_PIPELINE=PASS`; the unit suite covers
the injected failure paths and stale-content clear behavior.
