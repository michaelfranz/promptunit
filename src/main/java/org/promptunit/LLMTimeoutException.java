package org.promptunit;

/**
 * Thrown when an LLM operation exceeds the specified timeout duration.
 */
public class LLMTimeoutException extends RuntimeException {

	public LLMTimeoutException(String message) {
		super(message);
	}

	public LLMTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
}
