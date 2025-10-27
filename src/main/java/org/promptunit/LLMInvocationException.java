package org.promptunit;

/**
 * Thrown when an error occurs while invoking an LLM engine,
 * including HTTP errors, connection failures, and parsing errors.
 */
public class LLMInvocationException extends RuntimeException {

	public LLMInvocationException(String message) {
		super(message);
	}

	public LLMInvocationException(String message, Throwable cause) {
		super(message, cause);
	}
}
