package org.promptunit.providers.anthropic;

import java.util.concurrent.TimeUnit;
import org.promptunit.ApiKeyAccess;
import org.promptunit.LLMEngine;
import org.promptunit.LLMEngineInfo;
import org.promptunit.LLMInvocationException;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

public class AnthropicEngine implements LLMEngine, LLMEngineInfo {

	public static final String DEFAULT_MODEL = "claude-3-5-sonnet-20241022";
	public static final String API_KEY = "ANTHROPIC_API_KEY";

	private final String model;

	public AnthropicEngine() {
		this(DEFAULT_MODEL);
	}

	public AnthropicEngine(String model) {
		this.model = model;
	}

	@Override
	public final String provider() {
		return "anthropic";
	}

	@Override
	public String model() {
		return model;
	}

	@Override
	public boolean supportsOutputSchema() {
		return false;
	}

	@Override
	public PromptResult invokeOnce(PromptInstance promptInstance, long timeoutMs) {
		final String apiKey = ApiKeyAccess.getApiKey(API_KEY);

		if (promptInstance.provider() != null && !promptInstance.provider().equals(provider())) {
			throw new IllegalArgumentException("Invalid provider " + promptInstance.provider() + "; expected " + provider());
		}

		final String effectiveModel = promptInstance.model() != null && !promptInstance.model().isBlank() ? promptInstance.model() : this.model;

		try {
			AnthropicApi anthropicApi = new AnthropicApi(apiKey);
			AnthropicChatModel chatModel = new AnthropicChatModel(anthropicApi);

			AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder().withModel(effectiveModel);
			if (promptInstance.temperature() != null) {
				optionsBuilder.withTemperature(promptInstance.temperature());
			}
			if (promptInstance.topP() != null) {
				optionsBuilder.withTopP(promptInstance.topP());
			}
			if (promptInstance.maxTokens() != null) {
				optionsBuilder.withMaxTokens(promptInstance.maxTokens());
			}

			Prompt prompt = new Prompt(promptInstance.conversation(), optionsBuilder.build());

			long startNs = System.nanoTime();
			ChatResponse response = chatModel.call(prompt);
			long endNs = System.nanoTime();

			String output;
			try {
				output = response.getResult().getOutput().getContent();
			} catch (Exception ignored) {
				output = "";
			}

			long latencyMs = TimeUnit.NANOSECONDS.toMillis(endNs - startNs);
			int tokenUsage = PromptResult.UNKNOWN_TOKENS_USED;

			return new PromptResult(output, latencyMs, PromptResult.UNKNOWN_COST, tokenUsage, promptInstance, this);
		} catch (LLMInvocationException e) {
			throw e;
		} catch (Exception e) {
			throw new LLMInvocationException("Error invoking Anthropic: " + e.getMessage(), e);
		}
	}
}

