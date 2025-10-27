package org.promptunit.tools.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * @param toolVersion  optional, may be null */
public record ToolInvocation(String tool, String toolVersion, JsonNode args) {
	public ToolInvocation(String tool, JsonNode args) {
		this(tool, null, args);
	}


	public Optional<String> getToolVersion() {
		return Optional.ofNullable(toolVersion);
	}


}
