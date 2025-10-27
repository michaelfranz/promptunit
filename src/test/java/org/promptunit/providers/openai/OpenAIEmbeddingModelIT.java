package org.promptunit.providers.openai;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class OpenAIEmbeddingModelIT {

    @Test
    void embedsTextAndComputesSimilarity() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "OPENAI_API_KEY environment variable not set; skipping integration test");

        OpenAIEmbeddingModel model = new OpenAIEmbeddingModel();

        float[] vec = model.embed("hello world");
        assertThat(vec).isNotNull();
        assertThat(vec.length).isGreaterThan(0);

        double same = model.similarity("The quick brown fox", "The quick brown fox");
        double similar = model.similarity("The quick brown fox jumps over the lazy dog",
                "A fast brown fox leaps over a lazy dog");
        double dissimilar = model.similarity("The quick brown fox", "Quantum entanglement in particle physics");

        assertThat(same).isBetween(0.95, 1.0);
        assertThat(similar).isGreaterThan(dissimilar);
    }
}


