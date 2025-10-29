package org.promptunit;

import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public class MockLLMEngine implements LLMEngine, LLMEngineInfo {

	public static final String DEFAULT_MOCK_OUTPUT = """
			{ "summary": "Mock review output", "suggestions": ["Use better names"], "scorecard": {} }""";

	private final String mockOutput;

	public MockLLMEngine() {
		this(DEFAULT_MOCK_OUTPUT);
	}

	public MockLLMEngine(String mockOutput) {
		this.mockOutput = mockOutput;
	}

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
				mockOutput,
				50,
				0.0,
				100,
				instance,
				this);
	}

}


