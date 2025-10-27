package org.promptunit.tools.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

public final class ToolInvocation {
	private final String tool;
	private final String toolVersion; // optional, may be null
	private final JsonNode args;

	public ToolInvocation(String tool, JsonNode args) { this(tool, null, args); }

	public ToolInvocation(String tool, String toolVersion, JsonNode args) {
		this.tool = tool;
		this.toolVersion = toolVersion;
		this.args = args;
	}

	public String getTool() { return tool; }
	public Optional<String> getToolVersion() { return Optional.ofNullable(toolVersion); }
	public JsonNode getArgs() { return args; }
}
