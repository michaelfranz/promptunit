package org.promptunit.evaluation;

import org.promptunit.core.PromptResult;

public interface JsonPathValidator extends PromptValidator {
    @Override
    AssertionResult validate(PromptResult result);
}


