package org.promptunit.dsl;

import org.promptunit.LLMEngine;
import org.promptunit.core.PromptResult;

public final class PromptAssertions {
    private PromptAssertions() {}

    public static PromptAssert assertThatResult(PromptResult result) {
        return new PromptAssert(result);
    }

    public static PromptExecutor usingEngine(LLMEngine engine) {
        return new PromptExecutor(engine);
    }
}


