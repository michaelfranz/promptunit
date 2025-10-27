package org.promptunit.examples;

import org.junit.jupiter.api.Test;
import org.promptunit.LLMEngine;
import org.promptunit.MockLLMEngine;
import org.promptunit.core.PromptInstance;
import org.promptunit.dsl.PromptAssertions;

class CodeReviewPromptTest {

	@Test
	void testMockLLMOutput() {
		LLMEngine engine = new MockLLMEngine();
		PromptInstance mockInstance = PromptInstance.builder()
				.withSystemMessage("You are a Java code reviewer")
				.withUserMessage("Critique the design of Java's java.lang.Boolean class.")
				.withModel("ChatGPT-3.5")
				.withProvider("OpenAI")
				.build();

		PromptAssertions.usingEngine(engine)
				.withInstance(mockInstance)
				.execute()
				.containsValidJson()
				.contains("Mock review output")
				.latencyBelow(100)
				.tokenUsageBelow(2000)
				.costBelow(1.0);
	}

}


