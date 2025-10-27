# Current Status and Next Steps

This document captures the state of the emerging, platform‑neutral command framework and its supporting components, along with the rationale behind design choices aimed at enabling agentically assisted flows. It ends with a concise next‑steps checklist.

## What we built and why

### Goals (agentic flows, testability, clean boundaries)
- Provide a minimal, in‑memory command framework that can be inspected deterministically in tests without external dependencies.
- Keep the framework platform‑neutral: it knows nothing about LLMs, SDKs, or frontends. An LLM is treated like any other producer of commands.
- Support the typical requirements observed in agentic execution models:
  - Prioritization (including a “HIGHEST” tier that always wins)
  - Affinity‑based serialization (no two commands with the same affinity run concurrently)
  - Preemption semantics at insertion time (without aborting running work)
  - Deduplication (successive duplicates by semantic fingerprint, higher priority retained)
  - Clear observability via a simple, queryable API and a console renderer

### Clean architecture stance
- The command framework is application‑centric. It exposes a `Command` abstraction and a `CommandQueue`; producers (including LLM tool responders) are adapters that translate into commands.
- Provider/model specifics live in provider‑scoped factories. Core code remains independent of any vendor.

## Implemented components (main sources)

### Core command model (package: `org.promptunit.tools.command`)
- `Command`
  - Immutable description of work with: `getId()`, `getName()`, `getPriority()`, `getAffinityKey()`, `fingerprint()`.
  - Default no‑op `execute()` to make the framework plausible for real workers.
  - Status/cancellation hooks: `getStatus()`, `requestCancel()` (default no‑ops; realized by `AbstractCommand`).
- `AbstractCommand`
  - Implements id/priority/affinity storage, deterministic fingerprinting (excludes priority, includes affinity + payload),
    and status transitions: `PENDING`, `RUNNING`, `COMPLETED`, `COMPLETED_WITH_ERROR`, `CANCELLED`.
  - Cooperative cancellation flag accessible to implementations.
- `CommandPriority`: `LOW`, `MEDIUM`, `HIGH`, `HIGHEST`.
- `CommandStatus`: lifecycle states enumerated above.
- `CommandQueue`
  - Pending/Running/Completed lists with snapshot APIs for test assertions.
  - Insertion rules:
    - `HIGHEST` inserts at the head.
    - Priority ordering with affinity‑aware preemption at insertion time:
      - Higher priority can overtake lower if different affinity, or same affinity where the lower is not running.
    - Successive dedupe by `fingerprint()`; higher priority replaces the last identical pending item.
- `QueueWorker` (optional)
  - Executes commands from the queue with configurable concurrency and inter‑start latency.
  - Enforces affinity serialization (never run two with the same key concurrently).
  - No forced preemption: running tasks are not aborted; producers may call `requestCancel()` for cooperative stop.
  - On completion, moves items to Completed with appropriate status.
- `QueueConsoleRenderer`: human‑readable dump of Pending/Running/Completed.
- `EnqueueResult`: id, position, deduplicated flag.

### Tool→Command instantiation (provider‑neutral SPI)
- `org.promptunit.tools.command.factory.ToolCommandFactory`
  - SPI for mapping a canonical `ToolInvocation` to one or more `Command` instances.
  - `supports(provider, model, toolId, Optional<toolVersion>)` and `create(...)` methods.
- `ToolCommandFactoryRegistry`
  - Resolves a factory; supports `usingServiceLoader()` discovery and programmatic `of(...)` construction.
- `org.promptunit.tools.command.dispatch.CommandMappingPolicy`
  - Derives `CommandPriority` and affinity from a `ToolInvocation`.
  - `DefaultCommandMappingPolicy` maps priority by tool name (configurable) and affinity from common args (e.g., `ticketId`, `projectKey`).
- `ToolToCommandDispatcher`
  - Orchestrates: for each `ToolInvocation`, finds a factory strictly by `(provider, model, toolId, Optional<toolVersion>)`,
    creates commands, enqueues each independently. Missing factory → AssertionError (no “best effort”).
- `DispatchResult`
  - Collects `(ToolInvocation, Command, EnqueueResult)` tuples for diagnostics.

### Provider scaffolds (main)
- `org.promptunit.providers.openai.OpenAiToolCommandFactory` (scaffold)
- `org.promptunit.providers.anthropic.AnthropicToolCommandFactory` (scaffold)
- `org.promptunit.providers.ollama.OllamaToolCommandFactory` (scaffold)
- Service discovery is wired via `META-INF/services/org.promptunit.tools.command.factory.ToolCommandFactory`.

### Canonical tool request (main)
- `org.promptunit.tools.catalog.ToolInvocation`
  - Fields: `tool` (toolId), `Optional<String> toolVersion`, `args` (JsonNode).
  - Version is optional and provider‑dependent; matching is strict when present.

## Test support (test sources)

### Mock domain commands (ticketapp)
- `org.promptunit.mock.ticketapp.*`: `CreateTicketCommand`, `AddCommentCommand`, `UpdateStatusCommand`, `AssignUserCommand`, `LinkTicketsCommand`, `PauseCommand (HIGH)`, `CancelCommand (HIGHEST)`, `GetUserConfirmationCommand (MEDIUM)`.
- Purpose: exercise priority, affinity, dedupe, renderer, and dispatcher behavior.

### Unit tests (highlights)
- `CommandQueueTest`: single enqueue, priority insertion, affinity‑aware preemption, successive dedupe, renderer output.
- `OpenAiToolCommandFactoryTest`: example factory creates a `CreateTicketCommand` and enqueues it with policy‑derived priority.
- `AnthropicToolCommandFactoryTest` / `OllamaToolCommandFactoryTest`: provider selection present; `create(...)` intentionally unimplemented (TDD scaffolds).
- `ToolCommandFactoryRegistryVersioningTest`: verifies strict matching for versioned and unversioned tools.

## Key design decisions (and how they enable agentic flows)
- **Strict identity matching**: `(provider, model, toolId, Optional<version>)` must resolve a factory; otherwise the dispatcher fails fast with an AssertionError. This prevents silent drift in tool/command wiring.
- **Non‑executing by default**: the core queue is purely inspectable, enabling deterministic assertions during test authoring. A worker can be plugged in when simulating real execution.
- **Priority + Affinity semantics**: cover realistic scheduling constraints common in agentic patterns (urgent interrupts, sharded resources, serialized updates per resource).
- **Successive dedupe**: the common LLM/tool loop can re‑emit identical work; dedupe keeps test expectations stable while preserving higher priority.
- **Provider isolation**: factories live under `org.promptunit.providers.*`, so adding a new provider is predictable—create a factory, register it, and write tests—without touching core.

## Next steps (actionable checklist)
1. Implement OpenAI factory mappings (happy path)
   - Map concrete `toolId`s (e.g., `create_ticket`, `add_comment`, `update_status`, …) to ticketapp commands using `DefaultCommandMappingPolicy` for priority/affinity.
   - Add unit tests per tool ensuring fields and priorities are correct.
2. Flesh out Anthropic and Ollama factories
   - Mirror OpenAI mappings or add provider‑specific examples where appropriate.
   - Keep tests red/green TDD style: start with failing creation tests, then implement.
3. ServiceLoader integration tests
   - Add tests that build a `ToolCommandFactoryRegistry.usingServiceLoader()` and verify discovery + selection without manual `of(...)` wiring.
4. Worker coverage
   - Add tests for: affinity serialization under concurrency, graceful shutdown waiting for running tasks, inter‑start latency pacing, cooperative `requestCancel()` handling.
5. Versioned tool support end‑to‑end
   - Add a versioned tool example (`toolVersion` present) and a factory branch to assert strict selection.
6. Developer docs
   - Short HOWTO: creating a custom `Command`, implementing a provider `ToolCommandFactory`, registering via ServiceLoader, and dispatching tool responses into the queue.
7. Optional: DSL integration
   - Add convenience assertions/helpers that compose with AssertJ for typical queue inspections (size, order, affinity groups, dedupe events) without hiding the raw list access.
8. Optional: richer observability
   - Add lightweight metrics hooks or counters (enqueues, dedup drops, preempt insertions) exposed via the renderer or a small inspection API.

---

If you only have 5 minutes when you return, start with “Implement OpenAI factory mappings (happy path)”—it unlocks realistic agentic demos using the dispatcher + queue, and turns several current TDD tests green.


