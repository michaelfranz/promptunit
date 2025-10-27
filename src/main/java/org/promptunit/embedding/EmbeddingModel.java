package org.promptunit.embedding;

public interface EmbeddingModel {
	float[] embed(String text);
	double similarity(String text1, String text2);
}
