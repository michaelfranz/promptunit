package org.promptunit.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

class PiiLeakageGuardrailRuleTest {

	private static PiiLeakageGuardrailRule rule;

	@BeforeAll
	static void setup() {
		rule = new PiiLeakageGuardrailRule();
	}

	@Nested
	class PromptInstanceGuardrails {
		@Test
		void evaluateSafePromptInstance() {
			PromptInstance safePrompt = PromptInstance.builder()
					.addSystemMessage("Provide search data")
					.addUserMessage("List some nice cars")
					.addAssistantMessage("What kind of cars?")
					.addUserMessage("Sports cars")
					.build();
			GuardrailResult result = rule.evaluatePromptInstance(safePrompt);
			assertThat(result.passed()).isTrue();
		}

		@Test
		void evaluateUnsafePromptInstanceEmail() {
			PromptInstance unsafePrompt = PromptInstance.builder()
					.addSystemMessage("Tell me about yourself")
					.addUserMessage("My email address is: naive.dude@my.house")
					.build();
			GuardrailResult result = rule.evaluatePromptInstance(unsafePrompt);
			assertThat(result.passed()).isFalse();
		}

		@Test
		void evaluateUnsafePromptCreditCard() {
			PromptInstance unsafePrompt = PromptInstance.builder()
					.addSystemMessage("Tell me about yourself")
					.addUserMessage("My cc number is: 4574 9283 5176 3040")
					.build();
			GuardrailResult result = rule.evaluatePromptInstance(unsafePrompt);
			assertThat(result.passed()).isFalse();
		}

		@Test
		void evaluateUnsafePromptPhoneNumber() {
			PromptInstance unsafePrompt = PromptInstance.builder()
					.addSystemMessage("Tell me about yourself")
					.addUserMessage("My phone number is: +41 61 352 34 27")
					.build();
			GuardrailResult result = rule.evaluatePromptInstance(unsafePrompt);
			assertThat(result.passed()).isFalse();
		}


		@Test
		void evaluateUnsafePromptAPIKey() {
			PromptInstance unsafePrompt = PromptInstance.builder()
					.addSystemMessage("Tell me about yourself")
					.addUserMessage("My API key is: sk-5kj56kjhsvGFKSDJkkcvjscvbnvoisewe98743khjughTRES")
					.build();
			GuardrailResult result = rule.evaluatePromptInstance(unsafePrompt);
			assertThat(result.passed()).isFalse();
		}
	}

	@Nested
	class PromptResultGuardrails {

		@Test
		void evaluateSafePromptResult() {
			PromptResult promptResult = new PromptResult("Nothing going on here", 0, 9, 0);
			GuardrailResult result = rule.evaluatePromptResult(promptResult);
			assertThat(result.passed()).isTrue();
		}

		@Test
		void evaluateUnsafePromptResultEmail() {
			PromptResult unsafeResult = new PromptResult("My email address is: naive.dude@my.house", 0, 9, 0);
			GuardrailResult result = rule.evaluatePromptResult(unsafeResult);
			assertThat(result.passed()).isFalse();
		}

		@Test
		void evaluateUnsafePromptCreditCard() {
			PromptResult unsafeResult = new PromptResult("My cc number is: 4574 9283 5176 3040", 0, 9, 0);
			GuardrailResult result = rule.evaluatePromptResult(unsafeResult);
			assertThat(result.passed()).isFalse();
		}

		@Test
		void evaluateUnsafePromptPhoneNumber() {
			PromptResult unsafeResult = new PromptResult("My phone number is: +41 61 352 34 27", 0, 9, 0);
			GuardrailResult result = rule.evaluatePromptResult(unsafeResult);
			assertThat(result.passed()).isFalse();
		}

		@Test
		void evaluateUnsafePromptAPIKey() {
			PromptResult unsafeResult = new PromptResult("My API key is: sk-5kj56kjhsvGFKSDJkkcvjscvbnvoisewe98743khjughTRES", 0, 9, 0);
			GuardrailResult result = rule.evaluatePromptResult(unsafeResult);
			assertThat(result.passed()).isFalse();
		}
	}

	@Test
	void getName() {
		assertThat(rule.getName()).isEqualTo("PII-LEAKAGE-GUARDRAIL");
	}
}