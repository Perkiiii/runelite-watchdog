# Watchdog Domain Context

## Core Concepts

**Alert** — A rule that watches for a specific in-game event (chat message, stat change, XP drop, spawned object, etc.) and fires a list of Notifications in response. Alerts have debounce controls and an enabled flag.

**AlertGroup** — A named container that holds a list of Alerts (which may themselves be AlertGroups). Shares the same base `Alert` class but has no Notifications of its own.

**AdvancedAlert** — An Alert variant that replaces the linear Notification list with a visual node graph. Triggers are represented as TriggerNodes, actions as ActionNodes. Execution flows through the graph via exec-signal connections.

**Notification** — A single action fired by an Alert (e.g., play sound, screen flash, TTS, webhook). Notifications belong to one Alert and fire in order, with optional per-notification delays.

**Alert Conversion** — The process of transforming a standard Alert or AlertGroup into a semantically equivalent AdvancedAlert. Each source alert becomes a TriggerNode; each of its Notifications becomes an ActionNode (with an injected DelayNode when the Notification carried a delay). The original Alert is preserved so the user can revert.

**Node Graph** — The visual execution model inside an AdvancedAlert. Nodes communicate via typed ports: triangular ports carry exec-signal flow (control flow), circular ports carry data (numbers, booleans, strings, etc.).

**Per-Trigger Row Layout** — The spatial arrangement used when converting an Alert(Group) to a graph: each source alert occupies one horizontal row (TriggerNode on the left, its action chain extending right), with rows stacked top-to-bottom.
