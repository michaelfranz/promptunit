package org.promptunit.providers.llama;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.promptunit.LLMEngine;
import org.promptunit.LLMInvocationException;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;

public class OllamaEngine implements LLMEngine {

	public static final String DEFAULT_MODEL = "TBD";

	private final String model;

	public OllamaEngine() {
		this(DEFAULT_MODEL);
	}

	public OllamaEngine(String model) {
		this.model = model;
	}

	@Override
	public final String provider() {
		return "llama";
	}

	@Override
	public String model() {
		return model;
	}

	@Override
	public PromptResult invokeOnce(PromptInstance promptInstance, long timeoutMs) {
		if (promptInstance.provider() != null && !promptInstance.provider().equals(provider())) {
			throw new IllegalArgumentException("Invalid provider " + promptInstance.provider() + "; expected " + provider());
		}

		final String effectiveModel = promptInstance.model() != null && !promptInstance.model().isBlank() ? promptInstance.model() : this.model;

		try {
			OllamaOptions ollamaOptions = OllamaOptions.builder()
					.withTemperature(promptInstance.temperature())
					.withTopP(promptInstance.topP())
					// .withMaxTokens() Not supported by LLAMA
					.build();


			OllamaApi ollamaApi = new OllamaApi();
			OllamaChatModel chatModel = new OllamaChatModel(ollamaApi, ollamaOptions);

			List<Message> messages = new ArrayList<>();
			if (promptInstance.systemMessage() != null && !promptInstance.systemMessage().isBlank()) {
				messages.add(new SystemMessage(promptInstance.systemMessage()));
			}
			if (promptInstance.userMessage() != null) {
				messages.add(new UserMessage(Objects.toString(promptInstance.userMessage(), "")));
			}

			Prompt prompt = new Prompt(messages, ollamaOptions);

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

			return new PromptResult(output, latencyMs, PromptResult.UNKNOWN_COST, tokenUsage);
		} catch (LLMInvocationException e) {
			throw e;
		} catch (Exception e) {
			throw new LLMInvocationException("Error invoking Ollama: " + e.getMessage(), e);
		}
	}
}
