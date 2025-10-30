package org.promptunit.guardrails;

import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public interface GuardrailRule {

	String getName();

	GuardrailResult evaluatePromptInstance(PromptInstance promptInstance);

	GuardrailResult evaluatePromptResult(PromptResult result);
}
