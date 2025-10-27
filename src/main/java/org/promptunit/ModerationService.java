package org.promptunit;

import org.promptunit.core.PromptResult;

public interface ModerationService {

	ModerationResult moderate(PromptResult result);

}
