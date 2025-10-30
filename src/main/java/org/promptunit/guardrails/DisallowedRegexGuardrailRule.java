package org.promptunit.guardrails;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public class DisallowedRegexGuardrailRule implements GuardrailRule {

	private String[] disallowedRegexes;

	public DisallowedRegexGuardrailRule() {
	}

	public GuardrailRule disallowedRegexes(String... regexes) {
		disallowedRegexes = regexes;
		return this;
	}

	public GuardrailRule disallowedPatterns(Pattern... pattern) {
		disallowedRegexes = new String[pattern.length];
		for (int i = 0; i < pattern.length; i++) {
			disallowedRegexes[i] = pattern[i].pattern();
		}
		return this;
	}

	@Override
	public String getName() {
		return "DISALLOWED-REGEX-GUARDRAIL";
	}

	@Override
	public GuardrailResult evaluatePromptInstance(PromptInstance promptInstance) {
		return getGuardrailResult(promptInstance.conversaionAsString());
	}

	@Override
	public GuardrailResult evaluatePromptResult(PromptResult promptResult) {
		return getGuardrailResult(promptResult.rawOutput());
	}

	@NotNull
	private GuardrailResult getGuardrailResult(String text) {
		if (disallowedRegexes == null || disallowedRegexes.length == 0)
			throw new IllegalStateException("No regexes specified.");
		List<String> violations = new ArrayList<>();
		for (String regex : disallowedRegexes) {
			Matcher matcher = Pattern.compile(regex).matcher(text);
			if (matcher.find()) {
				violations.add("The regex `" + regex + "` matched content in the output.");
			}
		}
		return violations.isEmpty()
				? GuardrailResult.pass()
				: GuardrailResult.fail("Detected PII: " + String.join(", ", violations));
	}

}
