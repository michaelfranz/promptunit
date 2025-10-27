package org.promptunit.providers.prompz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.promptunit.LLMEngine;
import org.promptunit.LLMEngineInfo;
import org.promptunit.LLMInvocationException;
import org.promptunit.LLMTimeoutException;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public class PrompzEngine implements LLMEngine, LLMEngineInfo {

	private static final String DEFAULT_PROMPZ_API_URL = "http://localhost:8080/api/v1";
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String serverUrl;

	public PrompzEngine() {
		this(DEFAULT_PROMPZ_API_URL);
	}

	public PrompzEngine(String prompzApiUrl) {
		this.serverUrl = prompzApiUrl;
		this.httpClient = HttpClient.newHttpClient();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public boolean supportsOutputSchema() {
		return false;
	}

	@Override
	public PromptResult invokeOnce(PromptInstance instance, long timeoutMs) {
		try {
			// Serialize the PromptInstance to JSON
			String requestBody = objectMapper.writeValueAsString(instance);

			// Build the HTTP request
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(serverUrl + "/executePrompt"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.timeout(Duration.ofMillis(timeoutMs))
					.build();

			// Send the request and get the response
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			// Check for HTTP errors
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new LLMInvocationException("HTTP error " + response.statusCode() + ": " + response.body());
			}

			// Parse the response body as PromptResultDAO
			JsonNode resultNode = objectMapper.readTree(response.body());

			// Convert JSON response to PromptResult
			return new PromptResult(
					resultNode.get("rawOutput").asText(),
					resultNode.get("latencyMs").asLong(),
					resultNode.get("cost").asDouble(),
					resultNode.get("tokenUsage").asInt(),
					instance,
					this
			);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LLMTimeoutException("LLM invocation timed out after " + timeoutMs + "ms", e);
		} catch (LLMTimeoutException | LLMInvocationException e) {
			// Re-throw our custom exceptions as-is
			throw e;
		} catch (Exception e) {
			throw new LLMInvocationException("Error invoking LLM engine: " + e.getMessage(), e);
		}
	}

	@Override
	public String provider() {
		return "Promtz";
	}

	@Override
	public String model() {
		return "All";
	}
}
