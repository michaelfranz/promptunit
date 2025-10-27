package org.promptunit.core;

import java.util.Optional;

public record PromptInstance(
		String systemMessage,
		String userMessage,
		String model,
		String provider,
		Float temperature,
		Float topP,
		Integer maxTokens,
		Optional<OutputSchema> outputSchema
) {

	public static Builder builder() { return new Builder(); }

	public static final class Builder {
		private String systemMessage;
		private String userMessage;
		private String model;
		private String provider;
		private Float temperature;
		private Float topP;
		private Integer maxTokens;
		private Optional<OutputSchema> outputSchema = Optional.empty();

		public Builder withSystemMessage(String systemMessage) {
			this.systemMessage = systemMessage;
			return this;
		}

		public Builder withUserMessage(String userMessage) {
			this.userMessage = userMessage;
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

		public Builder withTemperature(Float temperature) {
			this.temperature = temperature;
			return this;
		}

		public Builder withTopP(Float topP) {
			this.topP = topP;
			return this;
		}

		public Builder withMaxTokens(Integer maxTokens) {
			this.maxTokens = maxTokens;
			return this;
		}

		public Builder withOutputSchema(OutputSchema schema) {
			this.outputSchema = Optional.ofNullable(schema);
			return this;
		}

		public PromptInstance build() {
			boolean hasSystem = systemMessage != null && !systemMessage.isBlank();
			boolean hasUser = userMessage != null && !userMessage.isBlank();
			if (!hasSystem && !hasUser) {
				throw new IllegalStateException("PromptInstance requires at least a non-blank systemMessage or userMessage");
			}
			return new PromptInstance(systemMessage, userMessage, model, provider, temperature, topP, maxTokens, outputSchema);
		}
	}
}



