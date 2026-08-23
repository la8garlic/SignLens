# Preserve sign line breaks in rendered output

## Problem

The previous formatter passed a raw newline component to the ActionBar. The
server packet preserved `123\n456`, but the native client ActionBar rendered
that control character as a replacement glyph instead of a second row.

## Relationship

Correctness follow-up to Issues 05 and 08. This issue must be resolved before
the debug command and final performance sign-off so diagnostics and manual
verification describe the actual viewer-facing output.

## Scope

- Preserve the order and boundary of sign lines in the viewer-facing output.
- Keep a line-aware representation until the renderer boundary and define an
  ActionBar-safe projection for the selected output surface.
- Preserve Adventure components, formatting, color, and glowing metadata while
  keeping line boundaries intact.
- Make filtering, visual-length calculation, and truncation line-aware.
- Add unit tests and a real Paper/manual verification case for a multi-line
  sign, including the `123` / `456` example.

## Non-goals

- Adding Dialog, chat, book, or resource-pack output.
- Changing sign reading, front/back side detection, or focus timing.
- Adding a global content cache or changing the scan schedule.

## Acceptance criteria

- [x] A sign containing `123` on line one and `456` on line two is rendered
      with the explicit visible `↵` boundary, never as a replacement glyph or
      punctuation-joined inline text.
- [x] Line order and internal empty-line boundaries are preserved according to
      the documented `FormattedContent`/ActionBar representation.
- [x] A single-line sign keeps its current readable output.
- [x] Per-line Adventure formatting, color, and decorations survive the
      line-aware formatting path.
- [x] Long multi-line content is truncated deterministically without merging
      two lines or dropping a line boundary unexpectedly.
- [x] Unit tests cover one-line, multi-line, empty-line, styled, and truncated
      content cases.
- [ ] Manual Paper verification confirms the local preview visibly renders
      `123↵456` without the replacement glyph. Automated Paper verification
      already confirms the runtime packet contains `123↵456` and no raw
      newline.

## Verification

Record the chosen line-aware representation in the UX/architecture
documentation, run the formatter and integration test suites, and verify the
result in the local Paper preview with a sign containing at least two distinct
lines. The 0.1 acceptance representation is `123↵456`; exact stacked rows are
reserved for the future Dialog renderer.
