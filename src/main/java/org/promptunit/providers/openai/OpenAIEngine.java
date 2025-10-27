package org.promptunit.providers.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.promptunit.ApiKeyAccess;
import org.promptunit.LLMEngine;
import org.promptunit.LLMEngineInfo;
import org.promptunit.LLMInvocationException;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.ResponseFormat;

public class OpenAIEngine implements LLMEngine, LLMEngineInfo {

	public static final String DEFAULT_MODEL = "gpt-3.5-turbo";
	public static final String API_KEY = "OPENAI_API_KEY";

	private final String model;
	private static final ObjectMapper objectMapper = new ObjectMapper();

	public OpenAIEngine() {
		this(DEFAULT_MODEL);
	}

	public OpenAIEngine(String model) {
		this.model = model;
	}

	@Override
	public final String provider() {
		return "openai";
	}

	@Override
	public String model() {
		return model;
	}

	@Override
	public boolean supportsOutputSchema() {
		return true;
	}

	@Override
	public PromptResult invokeOnce(PromptInstance promptInstance, long timeoutMs) {
		final String apiKey = ApiKeyAccess.getApiKey(API_KEY);
		if (promptInstance.provider() != null && !promptInstance.provider().equals(provider())) {
			throw new IllegalArgumentException("Invalid provider " + promptInstance.provider() + "; expected " + provider());
		}

		final String effectiveModel = promptInstance.model() != null && !promptInstance.model().isBlank() ? promptInstance.model() : this.model;

		try {
			OpenAiApi openAiApi = new OpenAiApi(apiKey);
			OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi);

			OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder().withModel(effectiveModel);
			if (promptInstance.temperature() != null) {
				optionsBuilder.withTemperature(promptInstance.temperature());
			}
			if (promptInstance.topP() != null) {
				optionsBuilder.withTopP(promptInstance.topP());
			}
			if (promptInstance.maxTokens() != null) {
				optionsBuilder.withMaxTokens(promptInstance.maxTokens());
			}

			// Configure response format based on schema presence and model capability
			if (promptInstance.outputSchema() != null && promptInstance.outputSchema().isPresent()) {
				ResponseFormat responseFormat;
				if (supportsJsonSchemaResponseFormat(effectiveModel)) {
					responseFormat = buildOpenAIResponseFormat(promptInstance.outputSchema().get().jsonSchema());
				} else {
					// Fallback to simple JSON object mode for models without structured outputs support
					responseFormat = new ResponseFormat(ResponseFormat.Type.JSON_OBJECT);
				}
				optionsBuilder.withResponseFormat(responseFormat);
			}

			List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
			if (promptInstance.systemMessage() != null && !promptInstance.systemMessage().isBlank()) {
				messages.add(new SystemMessage(promptInstance.systemMessage()));
			}
			if (promptInstance.userMessage() != null) {
				messages.add(new UserMessage(Objects.toString(promptInstance.userMessage(), "")));
			}

			Prompt prompt = new Prompt(messages, optionsBuilder.build());

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
			throw new LLMInvocationException("Error invoking OpenAI: " + e.getMessage(), e);
		}
	}

	private boolean supportsJsonSchemaResponseFormat(String modelName) {
		if (modelName == null) return false;
		String m = modelName.trim().toLowerCase();
		// Allow-list known structured-outputs capable models; expand as needed
		// Matches: gpt-4o, gpt-4o-mini, gpt-4o-*, gpt-4.1, gpt-4.1-*, o4, o4-*
		return m.matches("^(gpt-4o($|-.*))|(gpt-4\\.1($|-.*))|(o4($|-.*))$");
	}

	private ResponseFormat buildOpenAIResponseFormat(String outputContract) {
		if (outputContract == null || outputContract.trim().isEmpty()) {
			return new ResponseFormat(ResponseFormat.Type.JSON_OBJECT);
		}
		try {
			JsonNode schemaNode = objectMapper.readTree(outputContract);
			// Require a valid JSON Schema root with type: object
			if (schemaNode.isObject() && schemaNode.has("type") && "object".equalsIgnoreCase(schemaNode.get("type").asText())) {
				// Pass the schema itself (not wrapped) so OpenAI sees the root type correctly
				@SuppressWarnings("unchecked")
				Map<String, Object> schemaMap = objectMapper.convertValue(schemaNode, Map.class);
				return new ResponseFormat(
						ResponseFormat.Type.JSON_SCHEMA,
						new ResponseFormat.JsonSchema("OutputResponse", schemaMap, true)
				);
			}
			// Fallback if schema root is not an object schema
			return new ResponseFormat(ResponseFormat.Type.JSON_OBJECT);
		} catch (Exception e) {
			return new ResponseFormat(ResponseFormat.Type.JSON_OBJECT);
		}
	}
}
