package org.promptunit;

import java.util.Optional;

public interface LLMEngineInfo {
	String provider();
	String model();
	boolean supportsOutputSchema();
	default Optional<String> version() { return Optional.empty(); }
}
