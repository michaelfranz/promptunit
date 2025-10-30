package org.promptunit.guardrails;

import java.util.regex.Pattern;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public class PiiLeakageGuardrailRule implements GuardrailRule {

	private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
	private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s\\-]{7,}\\d");
	private static final Pattern CREDIT_CARD = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
	private static final Pattern API_KEY = Pattern.compile("sk-[a-zA-Z0-9]{32}");
	private final GuardrailRule delegate;

	public PiiLeakageGuardrailRule() {
		this.delegate = new DisallowedRegexGuardrailRule().disallowedPatterns(EMAIL, PHONE, CREDIT_CARD, API_KEY);
	}

	@Override
	public GuardrailResult evaluatePromptInstance(PromptInstance promptInstance) {
		return delegate.evaluatePromptInstance(promptInstance);
	}

	@Override
	public GuardrailResult evaluatePromptResult(PromptResult result) {
		return delegate.evaluatePromptResult(result);
	}

	@Override
	public String getName() {
		return "PII-LEAKAGE-GUARDRAIL";
	}
}
