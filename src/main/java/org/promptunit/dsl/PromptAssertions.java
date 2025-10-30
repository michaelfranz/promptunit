package org.promptunit.dsl;

import org.promptunit.LLMEngine;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public final class PromptAssertions {
    private PromptAssertions() {}

	public static PromptInstanceAssert assertThatPrompt(PromptInstance prompt) {
		return new PromptInstanceAssert(prompt);
	}

	public static PromptResultAssert assertThatResult(PromptResult result) {
        return new PromptResultAssert(result);
    }

    public static PromptExecutor usingEngine(LLMEngine engine) {
        return new PromptExecutor(engine);
    }
}


