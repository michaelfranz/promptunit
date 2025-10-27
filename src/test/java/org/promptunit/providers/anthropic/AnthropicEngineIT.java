package org.promptunit.providers.anthropic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.promptunit.LLMInvocationException;
import org.promptunit.LLMTimeoutException;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

class AnthropicEngineIT {

	@Test
	void invokeAnthropicWithSimplePrompt() {
		// Skip test if API key is not available
		String apiKey = System.getenv("ANTHROPIC_API_KEY");
		Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), 
				"ANTHROPIC_API_KEY environment variable not set; skipping integration test");

		AnthropicEngine engine = new AnthropicEngine("gpt-3.5-turbo");
		PromptInstance instance = PromptInstance.builder()
				.withSystemMessage("You are a helpful assistant.")
				.withUserMessage("Say hello.")
				.withModel("gpt-3.5-turbo")
				.withProvider("anthropic")
				.build();

		PromptResult result = engine.execute(instance, 30_000);

		assertThat(result).isNotNull();
		assertThat(result.rawOutput())
				.isNotNull()
				.isNotBlank();
		assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
		assertThat(result.cost()).isEqualTo(PromptResult.UNKNOWN_COST);
		assertThat(result.tokenUsage()).isEqualTo(PromptResult.UNKNOWN_TOKENS_USED);
	}

	@Test
	void invokeAnthropicWithMultipleExecutions() {
		// Skip test if API key is not available
		String apiKey = System.getenv("ANTHROPIC_API_KEY");
		Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), 
				"ANTHROPIC_API_KEY environment variable not set; skipping integration test");

		AnthropicEngine engine = new AnthropicEngine("gpt-3.5-turbo");
		PromptInstance instance = PromptInstance.builder()
				.withSystemMessage("You are a concise assistant.")
				.withUserMessage("What is 2 + 2?")
				.withModel("gpt-3.5-turbo")
				.withProvider("anthropic")
				.build();

		var results = engine.execute(instance, 30_000, 2);

		assertThat(results)
				.isNotNull()
				.hasSize(2)
				.allSatisfy(result -> {
					assertThat(result).isNotNull();
					assertThat(result.rawOutput())
							.isNotNull()
							.isNotBlank();
					assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
				});
	}

	@Test
	void throwsExceptionWhenApiKeyMissing() {
		AnthropicEngine engine = new AnthropicEngine("gpt-3.5-turbo");
		PromptInstance instance = PromptInstance.builder()
				.withSystemMessage("You are a helpful assistant.")
				.withUserMessage("Say hello.")
				.withModel("gpt-3.5-turbo")
				.withProvider("anthropic")
				.build();

		assertThatThrownBy(() -> engine.execute(instance, 5_000))
				.isInstanceOf(LLMInvocationException.class)
				.hasMessageContaining("ANTHROPIC_API_KEY");
	}

	@Test
	void respectsTimeoutOnSlowRequest() {
		// Skip test if API key is not available
		String apiKey = System.getenv("ANTHROPIC_API_KEY");
		Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), 
				"ANTHROPIC_API_KEY environment variable not set; skipping integration test");

		AnthropicEngine engine = new AnthropicEngine("gpt-3.5-turbo");
		PromptInstance instance = PromptInstance.builder()
				.withSystemMessage("You are a helpful assistant.")
				.withUserMessage("Write a very long essay about the history of computer science.")
				.withModel("gpt-3.5-turbo")
				.withProvider("anthropic")
				.build();

		assertThatThrownBy(() -> engine.execute(instance, 100))
				.isInstanceOf(LLMTimeoutException.class)
				.hasMessageContaining("timed out");
	}

}
