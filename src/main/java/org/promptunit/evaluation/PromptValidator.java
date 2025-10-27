package org.promptunit.evaluation;

import org.promptunit.core.PromptResult;

public interface PromptValidator {
    AssertionResult validate(PromptResult result);
}


