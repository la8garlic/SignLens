# Preserve sign line breaks in rendered output

## Problem

The current formatter treats each non-empty sign line as inline content and joins
the lines with a punctuation-like separator. A sign written as `123` on the
first line and `456` on the second line is therefore rendered as `123 · 456`
(or an equivalent inline separator) instead of preserving the sign's line
structure.

## Relationship

Correctness follow-up to Issues 05 and 08. This issue must be resolved before
the debug command and final performance sign-off so diagnostics and manual
verification describe the actual viewer-facing output.

## Scope

- Preserve the order and boundary of sign lines in the viewer-facing output.
- Define a line-aware representation that is supported by the selected output
  surface; do not silently replace line breaks with punctuation.
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

- [ ] A sign containing `123` on line one and `456` on line two is rendered
      with an explicit line boundary, never as punctuation-joined inline text.
- [ ] Line order and internal empty-line boundaries are preserved according to
      the documented output representation.
- [ ] A single-line sign keeps its current readable output.
- [ ] Per-line Adventure formatting, color, and decorations survive the
      line-aware formatting path.
- [ ] Long multi-line content is truncated deterministically without merging
      two lines or dropping a line boundary unexpectedly.
- [ ] Unit tests cover one-line, multi-line, empty-line, styled, and truncated
      content cases.
- [ ] Manual Paper verification confirms the rendered result matches the sign
      layout for the multi-line acceptance case.

## Verification

Record the chosen line-aware representation in the UX/architecture
documentation, run the formatter and integration test suites, and verify the
result in the local Paper preview with a sign containing at least two distinct
lines.
