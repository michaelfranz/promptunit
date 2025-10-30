package org.promptunit.providers.openai;

import static org.promptunit.providers.openai.OpenAIEngine.API_KEY;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import org.promptunit.ApiKeyAccess;
import org.promptunit.LLMInvocationException;
import org.promptunit.ModerationResult;
import org.promptunit.ModerationService;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public class OpenAIModerationService implements ModerationService {

	@Override
	public ModerationResult moderatePromptInstance(PromptInstance promptInstance) {
		String input = promptInstance != null ? promptInstance.conversaionAsString() : "";
		return getModerationResult(input);
	}

	@Override
	public ModerationResult moderatePromptResult(PromptResult promptResult) {
		String input = promptResult != null ? String.valueOf(promptResult.rawOutput()) : "";
		return getModerationResult(input);
	}

	@NotNull
	private static ModerationResult getModerationResult(String input) {
		final String apiKey = ApiKeyAccess.getApiKey(API_KEY);
		String requestJson = "{\"model\":\"omni-moderation-latest\",\"input\":" +
				toJsonString(input) + "}";
		try {

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://api.openai.com/v1/moderations"))
					.header("Authorization", "Bearer " + apiKey)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
					.build();

			HttpResponse<String> response;
			try (HttpClient client = HttpClient.newHttpClient()) {
				response = client.send(request, HttpResponse.BodyHandlers.ofString());
			}

			if (response.statusCode() / 100 != 2) {
				throw new LLMInvocationException("OpenAI moderation returned status " + response.statusCode() + ": " + response.body());
			}

			JsonNode root = new ObjectMapper().readTree(response.body());
			// Response shape: { results: [ { flagged: bool, categories: {...}, category_scores: {...} } ] }
			JsonNode first = root.path("results").isArray() && !root.path("results").isEmpty()
					? root.path("results").get(0)
					: null;
			if (first == null || first.isNull()) {
				return new ModerationResult(0.0, new String[] {}, "openai", "no results");
			}

			// Build categories list where category value is true
			JsonNode categoriesNode = first.path("categories");
			java.util.List<String> cats = new java.util.ArrayList<>();
			if (categoriesNode != null && categoriesNode.isObject()) {
				categoriesNode.fieldNames().forEachRemaining(name -> {
					if (categoriesNode.path(name).asBoolean(false)) {
						cats.add(name);
					}
				});
			}

			// Severity heuristic: max of category_scores; fallback to 0 if absent
			double severity = 0.0;
			JsonNode scores = first.path("category_scores");
			if (scores != null && scores.isObject()) {
				java.util.Iterator<String> it = scores.fieldNames();
				while (it.hasNext()) {
					String k = it.next();
					double v = scores.path(k).asDouble(0.0);
					if (v > severity) severity = v;
				}
			}

			boolean flagged = first.path("flagged").asBoolean(false);
			if (flagged && severity < 0.5) {
				// If flagged but all scores are low, bump minimally.
				severity = Math.max(severity, 0.5);
			}

			String explanation = flagged ? "flagged by OpenAI with severity=" + severity : "not flagged";
			return new ModerationResult(severity, cats.toArray(String[]::new), "openai", explanation);
		} catch (LLMInvocationException e) {
			throw e;
		} catch (Exception e) {
			throw new LLMInvocationException("Error calling OpenAI moderation: " + e.getMessage(), e);
		}
	}

	private static String toJsonString(String s) {
		// naive JSON string quoting without external deps beyond Jackson already present
		String escaped = s
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r");
		return "\"" + escaped + "\"";
	}

}
