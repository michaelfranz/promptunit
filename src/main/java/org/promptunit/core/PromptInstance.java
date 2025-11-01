package org.promptunit.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

public record PromptInstance(
		List<Message> conversation,
		String model,
		String provider,
		Double temperature,
		Double topP,
		Integer maxTokens,
		Optional<OutputSchema> outputSchema
) {

	public static Builder builder() {
		return new Builder();
	}

	public String systemMessagesAsString() {
		return conversation.stream()
				.filter(m -> m instanceof SystemMessage)
				.map(Message::getText)
				.collect(Collectors.joining("\n"));
	}

	public String userMessagesAsString() {
		return conversation.stream()
				.filter(m -> m instanceof UserMessage)
				.map(Message::getText)
				.collect(Collectors.joining("\n"));
	}

	public String assistantMessagesAsString() {
		return conversation.stream()
				.filter(m -> m instanceof AssistantMessage)
				.map(Message::getText)
				.collect(Collectors.joining("\n"));
	}

	public String conversaionAsString() {
		return conversation.stream()
				.map(m -> switch (m) {
					case SystemMessage sm -> "system: " + sm.getText();
					case UserMessage um -> "user: " + um.getText();
					case AssistantMessage am -> "assistant: " + am.getText();
					default -> m.getText();
				})
				.collect(Collectors.joining("\n\n"));
	}

	public static final class Builder {
		private final List<Message> conversation = new ArrayList<>();
		private String model;
		private String provider;
		private Double temperature;
		private Double topP;
		private Integer maxTokens;
		private OutputSchema outputSchema;

		public Builder addSystemMessage(String message) {
			this.conversation.add(new SystemMessage(message));
			return this;
		}

		public Builder addUserMessage(String message) {
			this.conversation.add(new UserMessage(message));
			return this;
		}

		public Builder addAssistantMessage(String message) {
			this.conversation.add(new AssistantMessage(message));
			return this;
		}

		public Builder withModel(String model) {
			this.model = model;
			return this;
		}

		public Builder withProvider(String provider) {
			this.provider = provider;
			return this;
		}

		public Builder withTemperature(Double temperature) {
			this.temperature = temperature;
			return this;
		}

		public Builder withTopP(Double topP) {
			this.topP = topP;
			return this;
		}

		public Builder withMaxTokens(Integer maxTokens) {
			this.maxTokens = maxTokens;
			return this;
		}

		public Builder withOutputSchema(OutputSchema schema) {
			this.outputSchema = schema;
			return this;
		}

		public PromptInstance build() {
			boolean hasSystem = conversation.stream().anyMatch(m -> m instanceof SystemMessage && !m.getText().isBlank());
			boolean hasUser = conversation.stream().anyMatch(m -> m instanceof UserMessage && !m.getText().isBlank());
			if (!hasSystem && !hasUser) {
				throw new IllegalStateException("PromptInstance requires at least a non-blank systemMessage or userMessage");
			}
			return new PromptInstance(Collections.unmodifiableList(conversation), model, provider, temperature, topP, maxTokens, Optional.ofNullable(outputSchema));
		}

	}
}



