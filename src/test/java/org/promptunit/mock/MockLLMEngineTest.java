package org.promptunit.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.promptunit.MockLLMEngine;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

class MockLLMEngineTest {

	@Test
	void returnsDeterministicMockOutput() {
		MockLLMEngine engine = new MockLLMEngine();
		PromptInstance instance = PromptInstance.builder()
				.addSystemMessage("You are a code reviewer")
				.addUserMessage("Review this code")
				.withModel("ChatGPT-3.5")
				.withProvider("OpenAI")
				.build();

		PromptResult result = engine.execute(instance);
		assertTrue(result.rawOutput().contains("Mock review output"));
		assertEquals(50, result.latencyMs());
		assertEquals(0.0, result.cost(), 0.0001);
	}
}


