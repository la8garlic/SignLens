# Bootstrap Paper 26.2 project and accept the SignLens 0.1 contract

## Type

Design parent + bootstrap

## Problem

SignLens needs a stable, narrow contract before runtime code is added. Without an explicit target and non-goals, later work can accidentally introduce client dependencies, world mutations, ActionBar spam, or legacy compatibility burden.

## Outcome

Create the initial Gradle project and plugin descriptor for Paper 26.2 / Java 25, verify that an empty plugin loads, and accept the architecture and UX contract in this repository.

## Scope

- Gradle Kotlin DSL project skeleton.
- Paper 26.2 API dependency and Java 25 toolchain target.
- Minimal plugin main class and descriptor.
- README, architecture, UX, performance, and ADR documents.
- The 0.1 command/configuration/permission contract.
- A test source set ready for pure unit tests.

## Non-goals

- Implementing sign detection or rendering.
- Supporting old Paper versions.
- Dialog, deep-reading gestures, resource packs, Bedrock/Geyser, NMS, ProtocolLib, or databases.

## Acceptance criteria

- [x] The project builds on Java 25.
- [x] Paper 26.2 is the explicit first target.
- [x] A clean Paper server loads the plugin without warnings caused by SignLens.
- [x] The plugin descriptor declares the main class, version, permissions, and command contract.
- [x] The four 0.1 ADRs and architecture boundaries are present.
- [x] Completed child slices are linked to their remote Issues:
  - [x] Detection abstraction — [Issue #3](https://github.com/la8garlic/SignLens/issues/3)
  - [x] Sign side/content reader — [Issue #6](https://github.com/la8garlic/SignLens/issues/6)
  - [ ] Focus state machine
  - [ ] Component formatter
  - [ ] ActionBar renderer
  - [ ] Session lifecycle
  - [ ] Adaptive scan
  - [ ] Debug command
  - [ ] Performance validation

## Verification evidence

- `.\gradlew.bat clean build --console=plain --no-daemon` passed on Java 25.
- `SignLens-0.1.0.jar` loaded on a clean Paper 26.2 build 112 server.
- The server reached `Done`, logged `SignLens 0.1.0 enabled.`, and generated the default plugin configuration.
- No SignLens-specific error, exception, or failed-load message was present in the startup log.
- The EULA warning in the smoke log came from the explicitly supplied Paper test flag and is unrelated to SignLens.

## Compatibility and safety

Use supported Paper APIs only. This bootstrap must not add NMS or ProtocolLib. No world, block, sign, entity, or interaction behavior is changed.

## Closure

This parent closes when the contract and build foundation are accepted. It does not close the implementation series.
