# SignLens 0.1 issue series

These are local GitHub Issue drafts. They are not remote Issues and contain no guessed GitHub numbers. Issue 01 is the design/Bootstrap parent; Issues 02–11 are atomic implementation slices.

## Intended order

```text
01 Bootstrap + contract
 ├─ 02 Detection abstraction
 ├─ 03 Sign side/content reader
 ├─ 04 Focus state machine
 ├─ 05 Component formatter
 ├─ 06 ActionBar renderer
 ├─ 07 Session lifecycle
 ├─ 08 Adaptive scan
 ├─ 09 Preserve sign line breaks in rendered output
 ├─ 10 Debug command
 └─ 11 Performance validation
```

The order is a recommended delivery sequence, not a claim that every later Issue is hard-blocked by every earlier one. Native blocking metadata should be added only after the repository is connected to GitHub and the exact dependency is confirmed.

Issue 01 can close when the 0.1 contract, ADRs, build target, and decomposition are accepted. Closing it does not mean the whole feature is implemented; Issues 02–11 provide the delivery evidence.
