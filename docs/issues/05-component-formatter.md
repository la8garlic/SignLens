# Sanitize and format sign components for readable output

## Problem

Sign lines are Adventure components, and flattening four separate lines into punctuation-joined text loses important presentation. Copying interaction events can also change behavior.

## Relationship

Child of Issue 01; consumes `SignSnapshot` from Issue 03 and supplies content to Issue 06.

## Scope

- `ComponentSanitizer`.
- `ContentFormatter`.
- Omit leading and trailing empty lines while preserving internal line boundaries.
- Build a line-aware component without replacing line breaks with punctuation.
- Preserve presentation formatting.
- Remove or neutralize click, hover, and insertion metadata.
- Soft/hard visual length limits with ellipsis truncation.

## Non-goals

- A full MiniMessage configuration language.
- `[more]` interaction or deep-reading UI.
- Translating or rewriting sign text.

## Acceptance criteria

- [x] Four lines become one readable line-aware component.
- [x] Leading/trailing empty lines are omitted and internal empty-line boundaries are preserved.
- [x] Colors, bold, italic, underline, strikethrough, and font are preserved where supported.
- [x] Interaction metadata does not escape into the rendered copy.
- [x] Empty/whitespace-only input produces no renderable content.
- [x] Long content follows the configured soft/hard limits without unsafe unbounded output.
- [x] Formatter tests compare component structure, not only plain text.

## Verification

Pure unit tests cover empty lines, color/emphasis/font preservation, recursive interaction metadata removal, normal length, soft-limit truncation, hard-limit truncation, invalid configuration, and component-structure assertions.
