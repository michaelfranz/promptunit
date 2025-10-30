package org.promptunit.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.promptunit.dsl.PromptAssertions.assertThatResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.promptunit.core.OutputSchema;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;
import org.promptunit.providers.openai.OpenAIEngine;


class PromptResultAssertTest {

	private PromptResult validJsonResult;
	private PromptResult invalidJsonResult;
	private PromptResult complexJsonResult;

	@BeforeEach
	void setUp() {
		validJsonResult = new PromptResult(
				"""
						{
							"summary": "Test review",
							"suggestions": ["Use better names", "Add documentation"],
							"scorecard": {"quality": 8, "coverage": 95}
						}""",
				50,
				0.5,
				150
		);

		invalidJsonResult = new PromptResult(
				"This is not valid JSON at all!",
				30,
				0.1,
				50
		);

		complexJsonResult = new PromptResult(
				"""
						{
							"data": {
								"users": [
									{"id": 1, "name": "Alice", "roles": ["admin"]},
									{"id": 2, "name": "Bob", "roles": ["user"]}
								],
								"metadata": {"version": "1.0"}
							},
							"status": "success"
						}""",
				100,
				1.0,
				500
		);
	}

	@Nested
	class HasValidSchemaTests {
		@Test
		void shouldPassWhenOutputIsValidJson() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(assert_::containsValidJson)
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowWhenOutputIsInvalidJson() {
			PromptResultAssert assert_ = assertThatResult(invalidJsonResult);
			assertThatThrownBy(assert_::containsValidJson)
					.isInstanceOf(AssertionError.class)
					.hasMessageContaining("Expected raw output to contain valid JSON content");
		}

		@Test
		void shouldSupportFluencyAfterValidation() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			PromptResultAssert result = assert_.containsValidJson();
			assertThat(result).isSameAs(assert_);
		}
	}

	@Nested
	class JsonPathExistsTests {
		@Test
		void shouldPassWhenPathExists() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.jsonPathExists("$.summary"))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldPassWhenNestedPathExists() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.jsonPathExists("$.scorecard.quality"))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldPassWhenArrayPathExists() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.jsonPathExists("$.suggestions[0]"))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowWhenPathDoesNotExist() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatThrownBy(() -> assert_.jsonPathExists("$.nonexistent"))
					.isInstanceOf(AssertionError.class)
					.hasMessageContaining("Expected raw output to contain node at path");
		}

		@Test
		void shouldThrowWhenValidatingPathInInvalidJson() {
			PromptResultAssert assert_ = assertThatResult(invalidJsonResult);
			assertThatThrownBy(() -> assert_.jsonPathExists("$.any"))
					.isInstanceOf(AssertionError.class);
		}

		@Test
		void shouldSupportFluencyAfterPathCheck() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			PromptResultAssert result = assert_.jsonPathExists("$.summary");
			assertThat(result).isSameAs(assert_);
		}

		@Test
		void shouldWorkWithComplexNestedPaths() {
			PromptResultAssert assert_ = assertThatResult(complexJsonResult);
			assertThatCode(() -> assert_.jsonPathExists("$.data.users[0].name"))
					.doesNotThrowAnyException();
		}
	}

	@Nested
	class ConformsToSchemaTests {
		private String simpleSchema;
		private String advancedSchema;

		@BeforeEach
		void setUpSchemas() {
			simpleSchema = """
					{
						"type": "object",
						"properties": {
							"summary": {"type": "string"},
							"suggestions": {"type": "array"},
							"scorecard": {"type": "object"}
						},
						"required": ["summary"]
					}""";

			advancedSchema = """
					{
						"type": "object",
						"properties": {
							"summary": {"type": "string", "minLength": 1},
							"suggestions": {
								"type": "array",
								"items": {"type": "string"}
							},
							"scorecard": {
								"type": "object",
								"properties": {
									"quality": {"type": "integer", "minimum": 0, "maximum": 10},
									"coverage": {"type": "integer", "minimum": 0, "maximum": 100}
								}
							}
						},
						"required": ["summary", "suggestions"]
					}""";
		}

		@Test
		void shouldPassWhenJsonConformsToSchema() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.conformsToSchema(simpleSchema))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldPassWithAdvancedSchema() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.conformsToSchema(advancedSchema))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowWhenJsonDoesNotConformToSchema() {
			PromptResult missingRequired = new PromptResult(
					"""
							{
								"suggestions": ["item1"],
								"scorecard": {}
							}""",
					50,
					0.5,
					150
			);
			PromptResultAssert assert_ = assertThatResult(missingRequired);
			assertThatThrownBy(() -> assert_.conformsToSchema(simpleSchema))
					.isInstanceOf(AssertionError.class)
					.hasMessageContaining("Expected raw output to conform to JSON schema");
		}

		@Test
		void shouldThrowWhenPropertyTypeIsWrong() {
			PromptResult wrongType = new PromptResult(
					"""
							{
								"summary": 123,
								"suggestions": ["item1"],
								"scorecard": {}
							}""",
					50,
					0.5,
					150
			);
			PromptResultAssert assert_ = assertThatResult(wrongType);
			assertThatThrownBy(() -> assert_.conformsToSchema(simpleSchema))
					.isInstanceOf(AssertionError.class)
					.hasMessageContaining("Expected raw output to conform to JSON schema");
		}

		@Test
		void shouldSupportFluencyAfterSchemaValidation() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			PromptResultAssert result = assert_.conformsToSchema(simpleSchema);
			assertThat(result).isSameAs(assert_);
		}

		@Test
		void shouldThrowWhenSchemaIsInvalid() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatThrownBy(() -> assert_.conformsToSchema("invalid schema"))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Invalid JSON schema provided");
		}

		@Test
		void shouldDistinguishBetweenInvalidSchemaAndValidationFailure() {
			// Invalid schema → IllegalArgumentException (bad argument)
			PromptResultAssert assert1 = assertThatResult(validJsonResult);
			assertThatThrownBy(() -> assert1.conformsToSchema("{invalid"))
					.isInstanceOf(IllegalArgumentException.class);

			// Valid schema but output doesn't conform → AssertionError (assertion failure)
			PromptResult nonConformingOutput = new PromptResult("{}", 10, 0.0, 0);
			PromptResultAssert assert2 = assertThatResult(nonConformingOutput);
			String schemaRequiringField = """
					{
						"type": "object",
						"required": ["required_field"]
					}""";
			assertThatThrownBy(() -> assert2.conformsToSchema(schemaRequiringField))
					.isInstanceOf(AssertionError.class);
		}
	}

	@Nested
	class ContainsTests {
		@Test
		void shouldPassWhenSubstringExists() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.contains("Test review"))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldPassWhenPartialSubstringExists() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.contains("review"))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowWhenSubstringDoesNotExist() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatThrownBy(() -> assert_.contains("nonexistent"))
					.isInstanceOf(AssertionError.class)
					.hasMessageContaining("Expected raw output to contain");
		}

		@Test
		void shouldBeCaseSensitive() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatThrownBy(() -> assert_.contains("test review"))
					.isInstanceOf(AssertionError.class);
		}

		@Test
		void shouldSupportFluencyAfterContains() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			PromptResultAssert result = assert_.contains("Test review");
			assertThat(result).isSameAs(assert_);
		}
	}

	@Nested
	class ContainsCaseInsensitiveTests {
		@Test
		void shouldPassWhenSubstringExistsWithDifferentCase() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.containsCaseInsensitive("TEST REVIEW"))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldPassWhenSubstringExistsWithSameCase() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.containsCaseInsensitive("test review"))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowWhenSubstringDoesNotExist() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatThrownBy(() -> assert_.containsCaseInsensitive("NONEXISTENT"))
					.isInstanceOf(AssertionError.class)
					.hasMessageContaining("Expected raw output to contain");
		}

		@Test
		void shouldSupportFluencyAfterContainsCaseInsensitive() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			PromptResultAssert result = assert_.containsCaseInsensitive("test review");
			assertThat(result).isSameAs(assert_);
		}
	}

	@Nested
	class LatencyBelowTests {
		@Test
		void shouldPassWhenLatencyIsBelow() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.latencyBelow(100))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldPassWhenLatencyEqualsThreshold() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.latencyBelow(50))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowWhenLatencyExceeds() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatThrownBy(() -> assert_.latencyBelow(30))
					.isInstanceOf(AssertionError.class)
					.hasMessageContaining("Expected latency < 30ms")
					.hasMessageContaining("50");
		}

		@Test
		void shouldSupportFluencyAfterLatencyCheck() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			PromptResultAssert result = assert_.latencyBelow(100);
			assertThat(result).isSameAs(assert_);
		}
	}

	@Nested
	class TokenUsageBelowTests {
		@Test
		void shouldPassWhenTokenUsageIsBelow() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.tokenUsageBelow(200))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldPassWhenTokenUsageEqualsThreshold() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.tokenUsageBelow(150))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowWhenTokenUsageExceeds() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatThrownBy(() -> assert_.tokenUsageBelow(100))
					.isInstanceOf(AssertionError.class)
					.hasMessageContaining("Expected token usage < 100")
					.hasMessageContaining("150");
		}

		@Test
		void shouldSupportFluencyAfterTokenUsageCheck() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			PromptResultAssert result = assert_.tokenUsageBelow(200);
			assertThat(result).isSameAs(assert_);
		}
	}

	@Nested
	class CostBelowTests {
		@Test
		void shouldPassWhenCostIsBelow() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.costBelow(1.0))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldPassWhenCostEqualsThreshold() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.costBelow(0.5))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowWhenCostExceeds() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatThrownBy(() -> assert_.costBelow(0.3))
					.isInstanceOf(AssertionError.class)
					.hasMessageContaining("Expected cost usage < 0.3")
					.hasMessageContaining("0.5");
		}

		@Test
		void shouldSupportFluencyAfterCostCheck() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			PromptResultAssert result = assert_.costBelow(1.0);
			assertThat(result).isSameAs(assert_);
		}
	}

	@Nested
	class FluentChainTests {
		@Test
		void shouldSupportChainOfMultipleAssertions() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() ->
					assert_.containsValidJson()
							.contains("Test review")
							.latencyBelow(100)
							.tokenUsageBelow(200)
							.costBelow(1.0)
			).doesNotThrowAnyException();
		}

		@Test
		void shouldSupportChainWithJsonPathAndSchema() {
			String schema = """
					{
						"type": "object",
						"properties": {
							"summary": {"type": "string"}
						},
						"required": ["summary"]
					}""";
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() ->
					assert_.containsValidJson()
							.jsonPathExists("$.summary")
							.conformsToSchema(schema)
							.contains("review")
			).doesNotThrowAnyException();
		}

		@Test
		void shouldFailChainAtFirstFailure() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatThrownBy(() ->
					assert_.containsValidJson()
							.contains("nonexistent")
							.latencyBelow(100)
			).isInstanceOf(AssertionError.class);
		}
	}

	@Nested
	class EdgeCaseTests {
		@Test
		void shouldHandleEmptyJsonObject() {
			PromptResult emptyJson = new PromptResult("{}", 10, 0.0, 0);
			PromptResultAssert assert_ = assertThatResult(emptyJson);
			assertThatCode(assert_::containsValidJson)
					.doesNotThrowAnyException();
		}

		@Test
		void shouldHandleEmptyJsonArray() {
			PromptResult emptyArray = new PromptResult("[]", 10, 0.0, 0);
			PromptResultAssert assert_ = assertThatResult(emptyArray);
			assertThatCode(assert_::containsValidJson)
					.doesNotThrowAnyException();
		}

		@Test
		void shouldHandleJsonWithSpecialCharacters() {
			PromptResult special = new PromptResult(
					"""
							{
								"text": "Hello\\nWorld\\t\\u0041\\u0042\\u0043",
								"emoji": "😀"
							}""",
					10,
					0.0,
					50
			);
			PromptResultAssert assert_ = assertThatResult(special);
			assertThatCode(assert_::containsValidJson)
					.doesNotThrowAnyException();
		}

		@Test
		void shouldHandleVeryLargeNumbers() {
			PromptResultAssert assert_ = assertThatResult(validJsonResult);
			assertThatCode(() -> assert_.latencyBelow(Long.MAX_VALUE))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldHandleZeroMetrics() {
			PromptResult zero = new PromptResult("{}", 0, 0.0, 0);
			PromptResultAssert assert_ = assertThatResult(zero);
			assertThatCode(() ->
					assert_.containsValidJson()
							.latencyBelow(1)
							.tokenUsageBelow(1)
							.costBelow(0.1)
			).doesNotThrowAnyException();
		}
	}

	@Nested
	class OpenAIConformanceIntegrationTest {
		@Test
		void conformsToSchemaOnlyWhenOpenAIKeyPresent() {
			String apiKey = System.getenv("OPENAI_API_KEY");
			Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
					"OPENAI_API_KEY not set; skipping OpenAI schema conformance test");

			String peopleSchema = """
					{
					  "type": "object",
					  "properties": {
					    "people": {
					      "type": "array",
					      "minItems": 3,
					      "items": {
					        "type": "object",
					        "properties": {
					          "name": {"type": "string"}
					        },
					        "required": ["name"],
					        "additionalProperties": false
					      }
					    }
					  },
					  "required": ["people"],
					  "additionalProperties": false
					}
				""";

			OpenAIEngine engine = new OpenAIEngine("gpt-4o-mini");
			PromptInstance instance = PromptInstance.builder()
					.addSystemMessage("You are a helpful assistant. Return only JSON.")
					.addUserMessage("Return a JSON object with a 'people' array of three records named Tom, Dick, and Harry.")
					.withOutputSchema(new OutputSchema(peopleSchema))
					.build();

			PromptResult result = engine.execute(instance, 30_000);

			assertThatResult(result)
					.conformsToSchema();
		}
	}

	@Nested
	class JsonAccessorsTests {

		@Test
		void jsonNodeShouldExtractFromFencedMarkdown() throws Exception {
			PromptResult fenced = new PromptResult(
					"""
					Here you go:

					```json
					{
					  "people": [
					    {"name": "Tom"},
					    {"name": "Dick"},
					    {"name": "Harry"}
					  ]
					}
					```
					""",
					25,
					0.0,
					42
			);

			JsonNode node = assertThatResult(fenced).jsonNode();
			assertThat(node.at("/people/0/name").asText()).isEqualTo("Tom");
			assertThat(node.at("/people/1/name").asText()).isEqualTo("Dick");
			assertThat(node.at("/people/2/name").asText()).isEqualTo("Harry");
		}

		@Test
		void jsonStringCompactAndFormattedShouldBothBeValidJson() throws Exception {
			PromptResult data = new PromptResult(
					"""
					{"a":1,"b":[2,3],"c":{"d":"x"}}
					""",
					10,
					0.0,
					10
			);

			PromptResultAssert pa = assertThatResult(data);
			String compact = pa.jsonString();
			String formatted = pa.jsonString(true);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode cNode = mapper.readTree(compact);
			JsonNode fNode = mapper.readTree(formatted);

			assertThat(cNode).isEqualTo(fNode);
			assertThat(formatted.length()).isGreaterThan(compact.length());
			assertThat(formatted).contains("\n");
		}
	}
}
