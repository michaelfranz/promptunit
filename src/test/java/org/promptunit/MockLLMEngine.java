package org.promptunit;

import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public class MockLLMEngine implements LLMEngine, LLMEngineInfo {

	@Override
	public String provider() {
		return "mock-llm-provider";
	}

	@Override
	public String model() {
		return "mock-llm-model";
	}

	@Override
	public boolean supportsOutputSchema() {
		return false;
	}

	@Override
	public PromptResult invokeOnce(PromptInstance instance, long timeoutMs) {
		return new PromptResult(
				"""
						{ "summary": "Mock review output", "suggestions": ["Use better names"], "scorecard": {} }""",
				50,
				0.0,
				100,
				instance,
				this);
	}

}


