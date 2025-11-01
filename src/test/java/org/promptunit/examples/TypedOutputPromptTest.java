package org.promptunit.examples;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.promptunit.LLMEngine;
import org.promptunit.MockLLMEngine;
import org.promptunit.core.OutputSchema;
import org.promptunit.core.PromptInstance;
import org.promptunit.dsl.PromptAssertions;
import org.promptunit.dsl.PromptResultAssert;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

class TypedOutputPromptTest {

    record OutputSchemaDefinition(String summary, @Schema(minLength = 1) List<String> suggestions,
                                  @JsonProperty() Object scorecard) {
    }

    @Test
    void testMockLLMTypedOutput() {
        LLMEngine engine = new MockLLMEngine();
        PromptInstance mockInstance = PromptInstance.builder()
                .addSystemMessage("You are a Java code reviewer")
                .addUserMessage("Critique the design of Java's java.lang.Boolean class.")
                .withModel("ChatGPT-3.5")
                .withProvider("OpenAI")
                .withOutputSchema(OutputSchema.from(OutputSchemaDefinition.class))
                .build();

        PromptResultAssert promptAssert = PromptAssertions.usingEngine(engine)
                .withInstance(mockInstance)
                .execute();
        OutputSchemaDefinition result = promptAssert.toResult(OutputSchemaDefinition.class);

        assertThat(result.summary).isEqualTo("Mock review output");
    }

    @Test
    void testTypedOutputSchemaCreation() {
        OutputSchema outputSchema = OutputSchema.from(OutputSchemaDefinition.class);
        assertThat(outputSchema.jsonSchema()).isEqualToIgnoringWhitespace(
                """
                        {
                          "$schema" : "https://json-schema.org/draft/2020-12/schema",
                          "type" : "object",
                          "properties" : {
                        	"scorecard" : { },
                        	"suggestions" : {
                        	  "minLength" : 1,
                        	  "type" : "array",
                        	  "items" : {
                        		"type" : "string"
                        	  }
                        	},
                        	"summary" : {
                        	  "type" : "string"
                        	}
                          },
                          "required" : [ "suggestions", "summary" ],
                          "additionalProperties" : false
                        }
                        """
        );
    }

    @Test
    void testTypedOutputSchemaCreationWithOptions() {
        OutputSchema outputSchema = OutputSchema.from(OutputSchemaDefinition.class, JsonSchemaGenerator.SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT);
        assertThat(outputSchema.jsonSchema()).isEqualToIgnoringWhitespace(
                """
                        {
                          "$schema" : "https://json-schema.org/draft/2020-12/schema",
                          "type" : "object",
                          "properties" : {
                        	"scorecard" : { },
                        	"suggestions" : {
                        	  "minLength" : 1,
                        	  "type" : "array",
                        	  "items" : {
                        		"type" : "string"
                        	  }
                        	},
                        	"summary" : {
                        	  "type" : "string"
                        	}
                          },
                          "required" : [ "suggestions", "summary" ]
                        }
                        """
        );
    }

}


