package org.promptunit.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import org.promptunit.LLMEngineInfo;
import org.promptunit.core.OutputSchema;
import org.promptunit.core.PromptResult;
import org.promptunit.embedding.EmbeddingModel;
import org.promptunit.guardrails.GuardrailResult;
import org.promptunit.guardrails.GuardrailRule;

public class PromptAssert {

	private final PromptResult result;
	private JsonNode readTree;
	private EmbeddingModel embeddingModel;

	PromptAssert(PromptResult result) {
		this.result = result;
	}

	public PromptAssert containsValidJson() {
		JsonNode parsed = tryParse(result.rawOutput());
		if (parsed == null) {
			String extracted = JsonExtractors.extract(result.rawOutput());
			if (extracted != null) parsed = tryParse(extracted);
		}
		if (parsed == null) {
			throw new AssertionError("Expected raw output to contain valid JSON content");
		}
		this.readTree = parsed;
		return this;
	}

	public PromptAssert conformsToSchema() {
		LLMEngineInfo engineInfo = result.engineInfo();
		if (engineInfo == null)
			throw new IllegalStateException("Engine info not available on PromptResult; cannot infer schema capability");
		Optional<OutputSchema> schemaOpt = result.promptInstance() != null
				? result.promptInstance().outputSchema()
				: Optional.empty();
		if (schemaOpt.isEmpty()) {
			throw new IllegalStateException("No OutputSchema present on PromptInstance; cannot validate conformity");
		}
		String schemaJson = schemaOpt.get().jsonSchema();

		JsonNode jsonNode;
		// If engine declares schema support, try direct parse first
		if (engineInfo.supportsOutputSchema()) {
			jsonNode = tryParse(result.rawOutput());
			if (jsonNode == null) {
				jsonNode = extractThenParse(result.rawOutput());
				if (jsonNode == null) failNotParsable();
			}
		} else {
			jsonNode = extractThenParse(result.rawOutput());
			if (jsonNode == null) failNotParsable();
		}

		validateAgainstSchema(jsonNode, schemaJson);
		this.readTree = jsonNode;
		return this;
	}

	private void failNotParsable() {
		throw new AssertionError("Could not locate or parse valid JSON from LLM output for schema validation");
	}

	private JsonNode tryParse(String text) {
		try {
			return new ObjectMapper().readTree(text);
		} catch (Exception e) {
			return null;
		}
	}

	private JsonNode extractThenParse(String text) {
		String extracted = JsonExtractors.extract(text);
		if (extracted == null) return null;
		return tryParse(extracted);
	}

	private void validateAgainstSchema(JsonNode output, String schemaJson) {
		JsonNode schemaNode;
		try {
			schemaNode = new ObjectMapper().readTree(schemaJson);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid JSON schema provided: " + e.getMessage(), e);
		}

		JsonSchema jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaNode);
		Set<ValidationMessage> validationMessages = jsonSchema.validate(output);
		if (!validationMessages.isEmpty()) {
			String errors = validationMessages.stream()
					.map(ValidationMessage::getMessage)
					.collect(Collectors.joining(", "));
			throw new AssertionError("Expected raw output to conform to JSON schema, but instead validation failed: " + errors);
		}
	}

	private synchronized JsonNode getReadTree() {
		if (readTree != null) return readTree;
		try {
			readTree = new ObjectMapper().readTree(result.rawOutput());
			return readTree;
		} catch (Exception e) {
			throw new AssertionError("Expected raw output to be valid JSON: " + result.rawOutput(), e);
		}
	}

	public PromptAssert jsonPathExists(String path) {
		try {
			JsonPath.parse(result.rawOutput()).read(path);
		} catch (PathNotFoundException e) {
			throw new AssertionError("Expected raw output to contain node at path: " + path, e);
		}
		return this;
	}

	public PromptAssert conformsToSchema(String schema) {
		JsonNode schemaNode;
		try {
			ObjectMapper mapper = new ObjectMapper();
			schemaNode = mapper.readTree(schema);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid JSON schema provided: " + e.getMessage(), e);
		}

		JsonSchema jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
				.getSchema(schemaNode);
		Set<ValidationMessage> validationMessages = jsonSchema.validate(getReadTree());
		if (!validationMessages.isEmpty()) {
			String errors = validationMessages.stream()
					.map(ValidationMessage::getMessage)
					.collect(Collectors.joining(", "));
			throw new AssertionError("Expected raw output to conform to JSON schema, but instead validation failed: " + errors);
		}
		return this;
	}

	public PromptAssert contains(String substring) {
		if (!result.rawOutput().contains(substring))
			throw new AssertionError("Expected raw output to contain " + substring);
		return this;
	}

	public PromptAssert containsCaseInsensitive(String substring) {
		if (!result.rawOutput().toLowerCase().contains(substring.toLowerCase()))
			throw new AssertionError("Expected raw output to contain " + substring);
		return this;
	}


	public PromptAssert latencyBelow(long millis) {
		if (result.latencyMs() > millis)
			throw new AssertionError("Expected latency < " + millis + "ms but got " + result.latencyMs());
		return this;
	}

	public PromptAssert tokenUsageBelow(int tokens) {
		if (result.tokenUsage() > tokens)
			throw new AssertionError("Expected token usage < " + tokens + " but got " + result.tokenUsage());
		return this;
	}

	public PromptAssert costBelow(double amount) {
		if (result.cost() > amount)
			throw new AssertionError("Expected cost usage < " + amount + " but got " + result.cost());
		return this;
	}

	public PromptAssert withEmbeddingModel(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
		return this;
	}

	public PromptAssert semanticallySimilarTo(String similar, float threshold) {
		if (embeddingModel == null) throw new IllegalStateException("Embedding model not set");
		double similarityScore = embeddingModel.similarity(result.rawOutput(), similar);
		if (similarityScore < threshold)
			throw new AssertionError("Expected raw output '%s' to be semantically similar to '%s' but %f did not meet threshold %f"
					.formatted(result.rawOutput(), similar, similarityScore, threshold));
		return this;
	}

	public PromptAssert containsSemanticallySimilarTo(String similar, float threshold) {
		if (embeddingModel == null) throw new IllegalStateException("Embedding model not set");
		StringTokenizer tokenizer = new StringTokenizer(result.rawOutput());
		double maxSimilarity = 0;
		while (tokenizer.hasMoreTokens() && maxSimilarity < threshold) {
			String nextToken = tokenizer.nextToken();
			double similarityScore = embeddingModel.similarity(nextToken, similar);
			maxSimilarity = Math.max(maxSimilarity, similarityScore);
		}
		if (maxSimilarity < threshold)
			throw new AssertionError("Expected raw output '%s' to contain a semantically similar string to '%s' but %f did not meet threshold %f"
					.formatted(result.rawOutput(), similar, maxSimilarity, threshold));
		return this;
	}

	public PromptAssert conformsToGuardrail(GuardrailRule rule) {
		GuardrailResult guardrailResult = rule.evaluate(result);
		if (!guardrailResult.passed())
			throw new AssertionError("Expected raw output to conform to guardrail %s: %s"
					.formatted(rule.getName(), guardrailResult.failReason()));
		return this;
	}

	public PromptAssert violatesToGuardrail(GuardrailRule rule) {
		GuardrailResult guardrailResult = rule.evaluate(result);
		if (guardrailResult.passed())
			throw new AssertionError("Expected raw output to violate guardrail %s"
					.formatted(rule.getName()));
		return this;
	}

	// --- New JSON accessors ---
	public JsonNode jsonNode() {
		return ensureJsonNode();
	}

	public String jsonString() {
		return jsonString(false);
	}

	public String jsonString(boolean formatted) {
		JsonNode node = ensureJsonNode();
		try {
			ObjectMapper mapper = new ObjectMapper();
			return formatted
					? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
					: mapper.writeValueAsString(node);
		} catch (Exception e) {
			throw new AssertionError("Failed to serialize JSON content", e);
		}
	}

	// --- Internals ---
	private JsonNode ensureJsonNode() {
		if (readTree != null) return readTree;
		JsonNode parsed = tryParse(result.rawOutput());
		if (parsed == null) {
			parsed = extractThenParse(result.rawOutput());
		}
		if (parsed == null) {
			throw new AssertionError("Expected raw output to contain valid JSON content");
		}
		this.readTree = parsed;
		return this.readTree;
	}
}


