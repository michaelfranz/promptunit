package org.promptunit.guardrails;

import org.promptunit.ModerationResult;
import org.promptunit.ModerationService;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public class ContentModerationGuardrail implements GuardrailRule {

	private final ModerationService service;
	private final float threshold;

	public ContentModerationGuardrail(ModerationService service, float threshold) {
		this.service = service;
		this.threshold = threshold;
	}

	@Override
	public String getName() {
		return "CONTENT_MODERATION_" + service.getClass().getSimpleName().toUpperCase();
	}

	@Override
	public GuardrailResult evaluatePromptInstance(PromptInstance promptInstance) {
		ModerationResult moderation = service.moderatePromptInstance(promptInstance);
		if (moderation.severity() >= threshold) {
			return GuardrailResult.fail(
					"Moderation score " + moderation.severity()
							+ " (threshold " + threshold + ") — categories: "
							+ String.join(", ", moderation.categories())
			);
		}
		return GuardrailResult.pass();
	}

	@Override
	public GuardrailResult evaluatePromptResult(PromptResult result) {
		ModerationResult moderation = service.moderatePromptResult(result);
		if (moderation.severity() >= threshold) {
			return GuardrailResult.fail(
					"Moderation score " + moderation.severity()
							+ " (threshold " + threshold + ") — categories: "
							+ String.join(", ", moderation.categories())
			);
		}
		return GuardrailResult.pass();
	}

}
