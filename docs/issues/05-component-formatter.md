# Sanitize and format sign components for readable output

## Problem

Sign lines are Adventure components, and four separate lines do not fit naturally in an ActionBar. Flattening components loses important presentation; copying interaction events can change behavior.

## Relationship

Child of Issue 01; consumes `SignSnapshot` from Issue 03 and supplies content to Issue 06.

## Scope

- `ComponentSanitizer`.
- `ContentFormatter`.
- Remove pure empty lines.
- Join non-empty lines with configurable low-presence separator ` · `.
- Preserve presentation formatting.
- Remove or neutralize click, hover, and insertion metadata.
- Soft/hard visual length limits with ellipsis truncation.

## Non-goals

- A full MiniMessage configuration language.
- `[more]` interaction or deep-reading UI.
- Translating or rewriting sign text.

## Acceptance criteria

- [ ] Four lines become one readable formatted component.
- [ ] Pure empty lines are omitted.
- [ ] Colors, bold, italic, underline, strikethrough, and font are preserved where supported.
- [ ] Interaction metadata does not escape into the rendered copy.
- [ ] Empty/whitespace-only input produces no renderable content.
- [ ] Long content follows the configured soft/hard limits without unsafe unbounded output.
- [ ] Formatter tests compare component structure, not only plain text.

## Verification

Pure unit tests cover empty lines, color/emphasis, interaction metadata, normal length, soft-limit truncation, and hard-limit truncation.
