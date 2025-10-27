package org.promptunit.guardrails;

import org.promptunit.core.PromptResult;

public interface GuardrailRule {

	String getName();

	GuardrailResult evaluate(PromptResult result);
}
