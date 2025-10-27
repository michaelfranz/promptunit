package org.promptunit.evaluation;

import org.promptunit.core.PromptResult;

public interface SemanticValidator extends PromptValidator {
    @Override
    AssertionResult validate(PromptResult result);
}


