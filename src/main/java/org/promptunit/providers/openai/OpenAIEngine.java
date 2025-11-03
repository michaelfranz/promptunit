package org.promptunit.providers.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.promptunit.ApiKeyAccess;
import org.promptunit.LLMEngine;
import org.promptunit.LLMEngineInfo;
import org.promptunit.LLMInvocationException;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;
import java.util.List;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;

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
			OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey).build();
			OpenAiChatModel chatModel = OpenAiChatModel.builder().openAiApi(openAiApi).build();

			OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder().model(effectiveModel);
			if (promptInstance.temperature() != null) {
				optionsBuilder.temperature(promptInstance.temperature());
			}
			if (promptInstance.topP() != null) {
				optionsBuilder.topP(promptInstance.topP());
			}
			if (promptInstance.maxTokens() != null) {
				optionsBuilder.maxTokens(promptInstance.maxTokens());
			}

			// Configure response format based on schema presence and model capability
			if (promptInstance.outputSchema() != null && promptInstance.outputSchema().isPresent()) {
				ResponseFormat responseFormat;
				if (supportsJsonSchemaResponseFormat(effectiveModel)) {
					responseFormat = buildOpenAIResponseFormat(promptInstance.outputSchema().get().jsonSchema());
				} else {
					// Fallback to simple JSON object mode for models without structured outputs support
					responseFormat = ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build();
				}
				optionsBuilder.responseFormat(responseFormat);
			}

			Prompt prompt = new Prompt(promptInstance.conversation(), optionsBuilder.build());

			long startNs = System.nanoTime();
			ChatResponse response = chatModel.call(prompt);
			long endNs = System.nanoTime();

            String output;
			try {
				output = response.getResult().getOutput().getText();
			} catch (Exception ignored) {
				output = "";
			}

			long latencyMs = TimeUnit.NANOSECONDS.toMillis(endNs - startNs);
			int tokenUsage = PromptResult.UNKNOWN_TOKENS_USED;

            // Map tool calls if present via Spring AI models
            List<org.promptunit.tools.ToolCall> toolCalls = org.promptunit.providers.util.SpringAiToolCallMapper
                    .fromAssistantMessage(response.getResult().getOutput(), objectMapper);

            return new PromptResult(output, latencyMs, PromptResult.UNKNOWN_COST, tokenUsage, promptInstance, this, toolCalls);
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
			return ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build();
		}
		try {
			JsonNode schemaNode = objectMapper.readTree(outputContract);
			// Require a valid JSON Schema root with type: object
			if (schemaNode.isObject() && schemaNode.has("type") && "object".equalsIgnoreCase(schemaNode.get("type").asText())) {
				// Pass the schema itself (not wrapped) so OpenAI sees the root type correctly
				@SuppressWarnings("unchecked")
				Map<String, Object> schemaMap = objectMapper.convertValue(schemaNode, Map.class);
				return ResponseFormat.builder()
						.type(ResponseFormat.Type.JSON_SCHEMA)
						.jsonSchema(ResponseFormat.JsonSchema.builder().name("OutputResponse").schema(schemaMap).strict(true).build())
						.build();
			}
			// Fallback if schema root is not an object schema
			return ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build();
		} catch (Exception e) {
			return ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build();
		}
	}
}
