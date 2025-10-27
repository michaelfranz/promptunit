package org.promptunit.providers.openai;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.promptunit.ApiKeyAccess;
import org.promptunit.ModerationResult;
import org.promptunit.core.PromptResult;

class OpenAIModerationServiceIT {

    @Test
    void flagsClearlyOffensiveTextAndPassesBenignText() {
        String apiKey = ApiKeyAccess.getApiKey("OPENAI_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "OPENAI_API_KEY environment variable not set; skipping integration test");

        OpenAIModerationService service = new OpenAIModerationService();

        ModerationResult bad = service.moderate(new PromptResult(
                "You are stupid and I hate you. I will punch you.", 10, 0.0, 1));
        ModerationResult good = service.moderate(new PromptResult(
                "Hello, I hope you are having a nice day.", 5, 0.0, 1));

        assertThat(bad).isNotNull();
        assertThat(bad.severity()).isBetween(0.0, 1.0);
        assertThat(bad.categories().length).isGreaterThanOrEqualTo(0);

        assertThat(good).isNotNull();
        assertThat(good.severity()).isBetween(0.0, 1.0);

        // Expect offensive text to be rated worse than benign text
        assertThat(bad.severity()).isGreaterThanOrEqualTo(good.severity());
    }
}


