package org.promptunit.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.Arrays;
import java.util.List;
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
import org.promptunit.tools.ToolCall;
import org.promptunit.tools.ToolRef;


public class PromptResultAssert {

	private final PromptResult result;
	private JsonNode readTree;
	private EmbeddingModel embeddingModel;

	PromptResultAssert(PromptResult result) {
		this.result = result;
	}

	public PromptResultAssert containsValidJson() {
		JsonNode parsed = tryParse(result.rawOutput());
		if (parsed == null) {
			String extracted = JsonExtractors.extract(result.rawOutput());
			if (extracted != null && !extracted.isBlank()) {
				parsed = tryParse(extracted);
			} else {
				throw new AssertionError("Expected raw output to contain valid JSON content");
			}
		}
		if (parsed == null) {
			throw new AssertionError("Expected raw output to contain valid JSON content");
		}
		this.readTree = parsed;
		return this;
	}

	public PromptResultAssert conformsToSchema() {
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

	public PromptResultAssert jsonPathExists(String path) {
		try {
			JsonPath.parse(result.rawOutput()).read(path);
		} catch (PathNotFoundException e) {
			throw new AssertionError("Expected raw output to contain node at path: " + path, e);
		}
		return this;
	}

	public PromptResultAssert conformsToSchema(String schema) {
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

	public PromptResultAssert contains(String substring) {
		if (!result.rawOutput().contains(substring))
			throw new AssertionError("Expected raw output to contain " + substring);
		return this;
	}

	public PromptResultAssert containsCaseInsensitive(String substring) {
		if (!result.rawOutput().toLowerCase().contains(substring.toLowerCase()))
			throw new AssertionError("Expected raw output to contain " + substring);
		return this;
	}


	public PromptResultAssert latencyBelow(long millis) {
		if (result.latencyMs() > millis)
			throw new AssertionError("Expected latency < " + millis + "ms but got " + result.latencyMs());
		return this;
	}

	public PromptResultAssert tokenUsageBelow(int tokens) {
		if (result.tokenUsage() > tokens)
			throw new AssertionError("Expected token usage < " + tokens + " but got " + result.tokenUsage());
		return this;
	}

	public PromptResultAssert costBelow(double amount) {
		if (result.cost() > amount)
			throw new AssertionError("Expected cost usage < " + amount + " but got " + result.cost());
		return this;
	}

	public PromptResultAssert withEmbeddingModel(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
		return this;
	}

	public PromptResultAssert semanticallySimilarTo(String similar, float threshold) {
		if (embeddingModel == null) throw new IllegalStateException("Embedding model not set");
		double similarityScore = embeddingModel.similarity(result.rawOutput(), similar);
		if (similarityScore < threshold)
			throw new AssertionError("Expected raw output '%s' to be semantically similar to '%s' but %f did not meet threshold %f"
					.formatted(result.rawOutput(), similar, similarityScore, threshold));
		return this;
	}

	public PromptResultAssert containsSemanticallySimilarTo(String similar, float threshold) {
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

	public PromptResultAssert conformsToGuardrail(GuardrailRule rule) {
		GuardrailResult guardrailResult = rule.evaluatePromptResult(result);
		if (!guardrailResult.passed())
			throw new AssertionError("Expected raw output to conform to guardrail %s: %s"
					.formatted(rule.getName(), guardrailResult.failReason()));
		return this;
	}

	public PromptResultAssert violatesToGuardrail(GuardrailRule rule) {
		GuardrailResult guardrailResult = rule.evaluatePromptResult(result);
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

	public <T> T toResult(Class<T> targetType) {
		JsonNode jsonNode = jsonNode();
		ObjectMapper mapper = new ObjectMapper();
		return mapper.convertValue(jsonNode, targetType);
	}

	// --- Tool call assertions ---
	public ToolCallAssert hasToolCall(ToolRef toolRef) {
		if (result.toolCalls() == null)
			throw new AssertionError("No tool call metadata available on PromptResult; engine did not provide tool calls");
		long count = result.toolCalls().stream().filter(tc -> toolRef.name().equals(tc.name())).count();
		if (count != 1)
			throw new AssertionError("Expected exactly one call to tool '" + toolRef.name() + "' but found " + count);
		ToolCall call = result.toolCalls().stream().filter(tc -> toolRef.name().equals(tc.name())).findFirst().orElseThrow();
		return new ToolCallAssert(call);
	}

	public PromptResultAssert hasToolCallsExactlyInAnyOrder(ToolRef... refs) {
		if (result.toolCalls() == null)
			throw new AssertionError("No tool call metadata available on PromptResult; engine did not provide tool calls");
		List<String> expected = Arrays.stream(refs).map(ToolRef::name).sorted().toList();
		List<String> actual = result.toolCalls().stream().map(ToolCall::name).sorted().toList();
		if (!expected.equals(actual)) {
			throw new AssertionError("Expected tool calls exactly (any order): " + expected + ", but got: " + actual);
		}
		return this;
	}

	public PromptResultAssert hasToolCallsExactlyInOrder(ToolRef... refs) {
		if (result.toolCalls() == null)
			throw new AssertionError("No tool call metadata available on PromptResult; engine did not provide tool calls");
		List<String> expected = Arrays.stream(refs).map(ToolRef::name).toList();
		List<String> actual = result.toolCalls().stream().map(ToolCall::name).toList();
		if (!expected.equals(actual)) {
			throw new AssertionError("Expected tool calls exactly in order: " + expected + ", but got: " + actual);
		}
		return this;
	}

    public final class ToolCallAssert {
        private final ToolCall call;

		ToolCallAssert(ToolCall call) {
			this.call = call;
		}

		public PromptResultAssert withArgsDeepEqual(String json) {
			JsonNode expected = parse(json);
			if (!expected.equals(call.args()))
				throw new AssertionError("Expected tool args to equal provided JSON, but they differ. expected=" + expected + ", actual=" + call.args());
			return parent();
		}

		public PromptResultAssert withArgsSubset(String jsonSubset) {
			JsonNode subset = parse(jsonSubset);
			if (!deepContains(call.args(), subset))
				throw new AssertionError("Expected tool args to contain subset " + subset + ", actual=" + call.args());
			return parent();
		}

		public PromptResultAssert withArgsMatching(String jsonPath, Object expected) {
			Object actual;
			try {
				actual = com.jayway.jsonpath.JsonPath.read(call.args().toString(), jsonPath);
			} catch (Exception e) {
				throw new AssertionError("Failed to read JSONPath '" + jsonPath + "' from tool args", e);
			}
			if (!java.util.Objects.equals(actual, expected))
				throw new AssertionError("Expected JSONPath " + jsonPath + " == " + expected + ", but was " + actual);
			return parent();
		}

		public PromptResultAssert withArgsSatisfying(java.util.function.Predicate<com.fasterxml.jackson.databind.JsonNode> predicate) {
			boolean ok;
			try {
				ok = predicate.test(call.args());
			} catch (Exception e) {
				throw new AssertionError("Predicate threw while evaluating tool args", e);
			}
			if (!ok) throw new AssertionError("Args predicate did not hold for tool args: " + call.args());
			return parent();
		}

		private JsonNode parse(String json) {
			try {
				return new ObjectMapper().readTree(json);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid JSON provided: " + e.getMessage(), e);
			}
		}

		private boolean deepContains(JsonNode superset, JsonNode subset) {
			if (subset == null) return true;
			if (superset == null) return false;
			if (subset.isObject()) {
				if (!superset.isObject()) return false;
				java.util.Iterator<String> names = subset.fieldNames();
				while (names.hasNext()) {
					String n = names.next();
					JsonNode ev = subset.get(n);
					JsonNode av = superset.get(n);
					if (av == null) return false;
					if (!deepContains(av, ev)) return false;
				}
				return true;
			}
			if (subset.isArray()) {
				if (!superset.isArray()) return false;
				if (subset.size() > superset.size()) return false;
				for (int i = 0; i < subset.size(); i++) {
					if (!deepContains(superset.get(i), subset.get(i))) return false;
				}
				return true;
			}
			return superset.equals(subset);
		}

        private PromptResultAssert parent() { return PromptResultAssert.this; }
	}
}


