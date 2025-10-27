package org.promptunit.guardrails;

public record GuardrailResult(boolean passed, String failReason) {

	public static GuardrailResult pass() {
		return new GuardrailResult(true);
	}

	public static GuardrailResult fail(String info) {
		return new GuardrailResult(false, info);
	}

	private GuardrailResult(boolean passed) {
		this(passed, null);
	}
}
