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

- [ ] The project builds on Java 25.
- [ ] Paper 26.2 is the explicit first target.
- [ ] A clean Paper server loads the plugin without warnings caused by SignLens.
- [ ] The plugin descriptor declares the main class, version, permissions, and command contract.
- [ ] The four 0.1 ADRs and architecture boundaries are present.
- [ ] The following child slices are linked when remote Issues are created:
  - [ ] Detection abstraction
  - [ ] Sign side/content reader
  - [ ] Focus state machine
  - [ ] Component formatter
  - [ ] ActionBar renderer
  - [ ] Session lifecycle
  - [ ] Adaptive scan
  - [ ] Debug command
  - [ ] Performance validation

## Compatibility and safety

Use supported Paper APIs only. This bootstrap must not add NMS or ProtocolLib. No world, block, sign, entity, or interaction behavior is changed.

## Closure

This parent closes when the contract and build foundation are accepted. It does not close the implementation series.
