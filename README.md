## PromptUnit

**Mission**

PromptUnit provides a simple, extensible framework for testing LLM prompts by making clear, readable assertions against model responses. The goal is to bring unit-test-like confidence to prompts: validate structure, content, and budgets (latency, tokens, cost), and catch regressions early.

In addition to single-call assertions, PromptUnit includes early-stage support for testing agentic flows. These tests express the expected interactions and outcomes across tool-augmented or multi-step chains where LLM outputs influence subsequent steps.

### Key Features

- **Assertion-focused DSL**: Write tests that assert on raw output, JSON validity, JSONPath presence, schema conformance, performance budgets, and semantic similarity.
- **Schema-first validation**: Validate responses against JSON Schema, with automatic parsing of fenced JSON blocks.
- **Semantic assertions**: Optional embeddings-based similarity checks (via Spring AI) for fuzzy/semantic validations.
- **Provider-agnostic**: Engines for OpenAI, Anthropic, and Ollama are included; adding more is straightforward.
- **Guardrails**: Pluggable rules (PII leakage, disallowed regex, content moderation) that can be asserted in tests.
- **Agentic flows (experimental)**: Load tool catalogs (OpenAPI, OpenAI JSON) and test multi-step tool-using flows.

### Quick Start

Prerequisites: Java 21+, internet access for dependency resolution, and optionally provider API keys.

If working with LLM providers such as OpenAI, Anthropic etc., copy the file .env.example to .env and fill in the required values.

Build the project:

```bash
./gradlew build
```

Run tests (integration tests are skipped unless API keys are present):

```bash
./gradlew test
```

FInd simple examples in the folder `src/test/java/org/promptunit/examples`.

To enable integration tests, set provider environment variables before running:

```bash
export OPENAI_API_KEY=...       # for OpenAI tests
export ANTHROPIC_API_KEY=...    # for Anthropic tests
export OLLAMA_BASE_URL=...      # for local Ollama, if used
./gradlew test
```

### Project Structure

- `org.promptunit.core` — Core domain types: `PromptInstance`, `PromptResult`, `OutputSchema`.
- `org.promptunit.dsl` — Test DSL: `PromptAssert`, `PromptAssertions`, helpers for JSON extraction and semantic checks.
- `org.promptunit.providers` — Engines for providers:
  - `openai` — `OpenAIEngine`, embeddings adapter
  - `anthropic` — `AnthropicEngine`
  - `ollama`/`llama` — `OllamaEngine`
- `org.promptunit.evaluation` — Validation utilities (schema, JSONPath, semantic validators).
- `org.promptunit.guardrails` — Guardrail rules and results (PII, disallowed regex, moderation).
- `org.promptunit.tools` — Agentic flow primitives: tool catalogs, loaders (OpenAPI, OpenAI), command queue/dispatch.
- `org.promptunit.embedding` — Embedding model abstraction and Spring AI adapter.
- `org.promptunit.registry` — Prompt library and repository abstractions.

### Status

- Core assertion DSL is stable for day‑to‑day prompt testing.
- Agentic flow testing is experimental and evolving; APIs may change.

### Contributing

We welcome issues and pull requests. See `CONTRIBUTING.md` for the technical overview and contributor guide.

### License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). See the `LICENSE` file for details.
