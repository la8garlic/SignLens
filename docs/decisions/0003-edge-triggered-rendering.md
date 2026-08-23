# ADR-0003: Render on transitions, not every tick

- Status: accepted for 0.1
- Date: 2026-08-23

## Context

An ActionBar send every tick creates unnecessary network traffic and overwrites other plugins' ActionBar content. A focused sign's content is normally stable.

## Decision

Use edge-triggered rendering. Send on focus entry, content change, and an infrequent focused keepalive when the render policy allows it. Clear once when focus ends.

## Consequences

Positive:

- far fewer packets;
- less ActionBar contention;
- rendering policy is testable without a server tick loop;
- content refresh behavior is explicit.

Trade-off:

- another plugin may overwrite the ActionBar between sends; the keepalive is a bounded compromise, not a guarantee of ownership.

## Rejected alternative

An unconditional per-tick sender is forbidden, even when it appears to make the text persistent.
