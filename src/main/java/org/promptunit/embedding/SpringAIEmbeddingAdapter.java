package org.promptunit.embedding;

import java.util.List;
import java.util.Objects;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

public class SpringAIEmbeddingAdapter implements EmbeddingModel {

	private final org.springframework.ai.embedding.EmbeddingModel delegate;

	public SpringAIEmbeddingAdapter(org.springframework.ai.embedding.EmbeddingModel delegate) {
		this.delegate = delegate;
	}

	@Override
	public float[] embed(String text) {
        var request = new EmbeddingRequest(List.of(Objects.toString(text, "")), null);
        EmbeddingResponse response = delegate.call(request);
        if (response == null || response.getResults().isEmpty()) {
            return new float[0];
        }
        var embedding = response.getResults().getFirst().getOutput();
        float[] vector = new float[embedding.length];
		System.arraycopy(embedding, 0, vector, 0, embedding.length);
        return vector;
	}

	@Override
	public double similarity(String text1, String text2) {
        float[] v1 = embed(text1);
        float[] v2 = embed(text2);
        if (v1.length == 0 || v2.length == 0 || v1.length != v2.length) {
            return 0.0d;
        }
        double dot = 0.0d;
        double n1 = 0.0d;
        double n2 = 0.0d;
        for (int i = 0; i < v1.length; i++) {
            double a = v1[i];
            double b = v2[i];
            dot += a * b;
            n1 += a * a;
            n2 += b * b;
        }
        if (n1 == 0.0d || n2 == 0.0d) return 0.0d;
        return dot / (Math.sqrt(n1) * Math.sqrt(n2));
	}
}
