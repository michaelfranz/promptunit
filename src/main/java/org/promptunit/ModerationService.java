package org.promptunit;

import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public interface ModerationService {

	ModerationResult moderatePromptInstance(PromptInstance result);
	ModerationResult moderatePromptResult(PromptResult result);

}
