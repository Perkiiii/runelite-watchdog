# 0002 — Zero out delayMilliseconds when injecting a DelayNode during Alert Conversion

**Status:** Accepted

## Context

`Notification.delayMilliseconds` controls how long `AlertProcessor` waits before firing a notification in the standard alert system. When converting an Alert to an AdvancedAlert, notifications with a delay get a `DelayNode` injected before their `ActionNode` in the exec chain — this is the canonical way to express delay in the graph execution model.

`ActionNode` has commented-out code that would wire `delayMilliseconds` directly into the node as an input port. If that code is ever uncommented, any notification whose `delayMilliseconds` is non-zero would fire the delay twice: once from the `DelayNode` and once from the `ActionNode`'s internal delay.

## Decision

During conversion, when a `DelayNode` is injected for a notification, the `delayMilliseconds` field on the cloned `Notification` object embedded in the resulting `ActionNode` is explicitly set to `0`. The `DelayNode` becomes the single source of truth for that delay in the graph.

## Consequences

- No risk of double-delay if `ActionNode`'s delay port is ever re-enabled.
- The converted graph is self-consistent: delay is visible and editable as a `DelayNode`.
- The original (preserved) Alert is unaffected — its `Notification` objects keep their original `delayMilliseconds` values.
