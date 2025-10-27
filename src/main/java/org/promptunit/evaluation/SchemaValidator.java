package org.promptunit.evaluation;

import org.promptunit.core.PromptResult;

public interface SchemaValidator extends PromptValidator {
    @Override
    AssertionResult validate(PromptResult result);
}


