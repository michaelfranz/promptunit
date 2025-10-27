package org.promptunit.tools.catalog;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ToolCatalog {
	private final String version;
	private final List<ToolSpec> tools;
	private final Map<String, Object> meta;

	public ToolCatalog(String version, List<ToolSpec> tools, Map<String, Object> meta) {
		this.version = version;
		this.tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
		this.meta = meta == null ? Collections.emptyMap() : Map.copyOf(meta);
	}

	public String getVersion() { return version; }
	public List<ToolSpec> getTools() { return tools; }
	public Map<String, Object> getMeta() { return meta; }
}
