package org.promptunit.dsl;

import java.util.Objects;
import org.promptunit.LLMEngine;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public final class PromptExecutor {
	private final LLMEngine engine;
	private PromptInstance instance;
	private long timeoutMs = Long.MAX_VALUE;

	public PromptExecutor(LLMEngine engine) {
		this.engine = Objects.requireNonNull(engine, "engine");
	}

	public PromptExecutor withInstance(PromptInstance instance) {
		this.instance = Objects.requireNonNull(instance, "instance");
		return this;
	}

	public PromptExecutor withTimeoutMs(long timeoutMs) {
		if (timeoutMs <= 0) throw new IllegalArgumentException("timeoutMs must be > 0");
		this.timeoutMs = timeoutMs;
		return this;
	}

	public PromptAssert execute() {
		if (instance == null) throw new IllegalStateException("PromptInstance not set. Call withInstance(...) first.");
		PromptResult result = engine.execute(instance, timeoutMs);
		return new PromptAssert(result);
	}

}
