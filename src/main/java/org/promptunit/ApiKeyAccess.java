package org.promptunit;

import org.promptunit.env.DotenvEnvironmentUtil;

public class ApiKeyAccess {
	public static String getApiKey(String keyName) {
		String key = DotenvEnvironmentUtil.getEnvVarStringValue(keyName);
		if (key == null || key.isBlank()) {
			throw new LLMInvocationException("Missing environment variable " + keyName);
		}
		return key;
	}
}
