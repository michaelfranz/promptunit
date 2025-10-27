# Contributing to PromptUnit

Thank you for your interest in contributing! This guide explains the project's technical architecture and how to get set up to contribute effectively.

## Project Goals

- Deliver a simple, readable assertion DSL for LLM prompt testing.
- Provide guardrails and schema validation to make outputs reliable.
- Evolve support for testing agentic flows (tool use, command dispatch, multi-step interactions).

## Architecture Overview

- `org.promptunit.core`
  - `PromptInstance`: Immutable prompt request (provider, model, messages, budgets, optional OutputSchema).
  - `PromptResult`: Execution result (raw output, latency, token usage, cost, reference to `PromptInstance` and engine info).
  - `OutputSchema`: JSON Schema wrapper for structured-output expectations.
- `org.promptunit.dsl`
  - `PromptAssert` and `PromptAssertions`: Assertion DSL (JSON validity, JSONPath, schema conformance, latency/tokens/cost, semantic similarity, guardrails).
  - `JsonExtractors`: Robust JSON extraction from raw LLM output (including fenced code blocks).
- `org.promptunit.providers`
  - Provider engines adapt PromptUnit to specific backends (OpenAI, Anthropic, Ollama). Engines implement `LLMEngine` and `LLMEngineInfo`.
  - Engines use Spring AI clients under the hood where possible.
- `org.promptunit.guardrails`
  - Guardrail rules (PII leakage, disallowed regex, moderation) evaluated against `PromptResult`.
- `org.promptunit.tools`
  - Agentic flow building blocks: tool catalogs, loaders (OpenAPI, OpenAI), command queue & dispatch, tool-to-command mapping (experimental).
- `org.promptunit.embedding`
  - Embedding integration and adapters for semantic assertions.

## Tech Stack

- Java 21, Gradle (Kotlin DSL)
- Spring AI (provider clients, embeddings)
- JUnit 5, AssertJ
- JSON libraries: Jackson, networknt JSON Schema, Jayway JsonPath

## Getting Started

1. Prereqs: Java 21+, Gradle wrapper, internet access.
2. Build without tests or with default test set:
   - `./gradlew build`
   - `./gradlew test`
3. Enable integration tests by setting API keys:
   - `export OPENAI_API_KEY=...`
   - `export ANTHROPIC_API_KEY=...`
   - (Optional) `export OLLAMA_BASE_URL=...`

## Running Tests

- Unit tests run by default.
- Integration tests which call providers are guarded with environment-variable assumptions and will be skipped if keys are missing.

## Development Tips

- Prefer adding assertions to the DSL over duplicating logic in tests.
- When adding provider features, keep `LLMEngine`/`LLMEngineInfo` consistent and provider-agnostic.
- For schema validations, prefer strict JSON Schemas and keep test fixtures minimal but representative.
- For agentic features, keep APIs experimental and document breaking changes in PR descriptions.

## Adding a New Provider Engine

1. Create a new package under `org.promptunit.providers.<name>`.
2. Implement `LLMEngine` and `LLMEngineInfo`.
3. Wire to a client (prefer Spring AI if available), map options (model, temperature, topP, maxTokens).
4. Ensure errors raise `LLMInvocationException` and timeouts raise `LLMTimeoutException`.
5. Add basic unit tests and conditional integration tests.

## Adding Assertions or Guardrails

- Extend `PromptAssert` with small, composable methods (return `this` for fluency).
- Only add comments for non-obvious rationale; keep implementations readable.
- Add tests in `src/test/java` covering success and failure cases.

## Style & Quality

- Follow the existing code style: descriptive names, early returns, minimal nesting, avoid unnecessary try/catch.
- Keep comments concise and valuable; prefer readable code over heavy comments.
- Ensure no linter errors and that builds pass.

## Submitting Changes

1. Fork and create a feature branch.
2. Write tests alongside your changes.
3. Run `./gradlew build` locally.
4. Open a PR:
   - Describe the change, rationale, and any tradeoffs.
   - Note any breaking changes (especially in experimental agentic APIs).
   - Include screenshots or snippets when helpful.

## Issue Reporting

- Use issues for bugs, feature requests, and questions.
- Provide steps to reproduce, expected vs actual behavior, and environment details.

## Code of Conduct

- Be respectful and collaborative. We welcome contributions from all backgrounds and experience levels.
