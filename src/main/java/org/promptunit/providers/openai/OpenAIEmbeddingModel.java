package org.promptunit.providers.openai;

import org.promptunit.ApiKeyAccess;
import org.promptunit.embedding.EmbeddingModel;
import org.promptunit.embedding.SpringAIEmbeddingAdapter;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;

public class OpenAIEmbeddingModel implements EmbeddingModel {

	private final SpringAIEmbeddingAdapter delegate;

	public static final String API_KEY = "OPENAI_API_KEY";
	public static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small"; // 1536 dims, cost-effective

	public OpenAIEmbeddingModel() {
		this(DEFAULT_EMBEDDING_MODEL);
	}

	public OpenAIEmbeddingModel(String model) {
		String apiKey = ApiKeyAccess.getApiKey(API_KEY);
		OpenAiApi api = OpenAiApi.builder().apiKey(apiKey).build();
		org.springframework.ai.openai.OpenAiEmbeddingModel embeddingModel =
				new org.springframework.ai.openai.OpenAiEmbeddingModel(
						api,
						MetadataMode.ALL,
						OpenAiEmbeddingOptions.builder().model(model).build()
				);
		this.delegate = new SpringAIEmbeddingAdapter(embeddingModel);
	}

	@Override
	public float[] embed(String text) {
		return delegate.embed(text);
	}

	@Override
	public double similarity(String text1, String text2) {
		return delegate.similarity(text1, text2);
	}
}
