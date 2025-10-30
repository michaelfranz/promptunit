package org.promptunit.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import org.promptunit.core.PromptInstance;
import org.promptunit.embedding.EmbeddingModel;
import org.promptunit.guardrails.GuardrailResult;
import org.promptunit.guardrails.GuardrailRule;

public class PromptInstanceAssert {

	private final PromptInstance promptInstance;
	private JsonNode readTree;
	private EmbeddingModel embeddingModel;

	PromptInstanceAssert(PromptInstance promptInstance) {
		this.promptInstance = promptInstance;
	}

	public PromptInstanceAssert systemMessageContains(String substring) {
		if (!promptInstance.systemMessagesAsString().contains(substring))
			throw new AssertionError("Expected system message to contain " + substring);
		return this;
	}

	public PromptInstanceAssert systemMessageContainsCaseInsensitive(String substring) {
		if (!promptInstance.systemMessagesAsString().toLowerCase().contains(substring.toLowerCase()))
			throw new AssertionError("Expected system message to contain " + substring);
		return this;
	}

	public PromptInstanceAssert userMessageContains(String substring) {
		if (!promptInstance.userMessagesAsString().contains(substring))
			throw new AssertionError("Expected user message to contain " + substring);
		return this;
	}

	public PromptInstanceAssert userMessageContainsCaseInsensitive(String substring) {
		if (!promptInstance.userMessagesAsString().toLowerCase().contains(substring.toLowerCase()))
			throw new AssertionError("Expected user message to contain " + substring);
		return this;
	}

	public PromptInstanceAssert assistantMessageContains(String substring) {
		if (!promptInstance.assistantMessagesAsString().contains(substring))
			throw new AssertionError("Expected assistant message to contain " + substring);
		return this;
	}

	public PromptInstanceAssert assistantMessageContainsCaseInsensitive(String substring) {
		if (!promptInstance.assistantMessagesAsString().toLowerCase().contains(substring.toLowerCase()))
			throw new AssertionError("Expected assistant message to contain " + substring);
		return this;
	}

	public PromptInstanceAssert systemMessageSemanticallySimilarTo(String similar, float threshold) {
		if (embeddingModel == null) throw new IllegalStateException("Embedding model not set");
		double similarityScore = embeddingModel.similarity(promptInstance.systemMessagesAsString(), similar);
		if (similarityScore < threshold)
			throw new AssertionError("Expected system message '%s' to be semantically similar to '%s' but %f did not meet threshold %f"
					.formatted(promptInstance.systemMessagesAsString(), similar, similarityScore, threshold));
		return this;
	}

	public PromptInstanceAssert userMessageSemanticallySimilarTo(String similar, float threshold) {
		if (embeddingModel == null) throw new IllegalStateException("Embedding model not set");
		double similarityScore = embeddingModel.similarity(promptInstance.userMessagesAsString(), similar);
		if (similarityScore < threshold)
			throw new AssertionError("Expected user message '%s' to be semantically similar to '%s' but %f did not meet threshold %f"
					.formatted(promptInstance.userMessagesAsString(), similar, similarityScore, threshold));
		return this;
	}

	public PromptInstanceAssert conformsToGuardrail(GuardrailRule rule) {
		GuardrailResult guardrailResult = rule.evaluatePromptInstance(promptInstance);
		if (!guardrailResult.passed())
			throw new AssertionError("Expected raw output to conform to guardrail %s: %s"
					.formatted(rule.getName(), guardrailResult.failReason()));
		return this;
	}

	public PromptInstanceAssert violatesGuardrail(GuardrailRule rule) {
		GuardrailResult guardrailResult = rule.evaluatePromptInstance(promptInstance);
		if (guardrailResult.passed())
			throw new AssertionError("Expected raw output to violate guardrail %s"
					.formatted(rule.getName()));
		return this;
	}

}


