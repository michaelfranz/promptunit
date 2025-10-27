package org.promptunit;

public record ModerationResult(
		double severity,          // 0.0–1.0
		String[] categories,      // e.g. ["hate", "sexual"]
		String provider,          // "openai", "perspective"
		String explanation        // optional human-readable reason
) {
}
