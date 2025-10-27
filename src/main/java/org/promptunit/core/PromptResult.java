package org.promptunit.core;

import org.promptunit.LLMEngineInfo;

public record PromptResult(
		String rawOutput,
		long latencyMs,
		double cost,
		int tokenUsage,
		PromptInstance promptInstance,
		LLMEngineInfo engineInfo
) {

	public PromptResult(String rawOutput, long latencyMs, double cost, int tokenUsage) {
		this(rawOutput, latencyMs, cost, tokenUsage, null, null);
	}

	PromptResult(String rawOutput) {
		this(rawOutput, UNKNOWN_RESPONSE_TIME, UNKNOWN_COST, UNKNOWN_TOKENS_USED, null, null);
	}

	public static long UNKNOWN_RESPONSE_TIME = -1;
	public static int UNKNOWN_COST = -1;
	public static int UNKNOWN_TOKENS_USED = -1;

}


