package org.promptunit.tools.catalog.loaders.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.promptunit.tools.catalog.ToolCatalog;
import org.promptunit.tools.catalog.ToolCatalogLoader;
import org.promptunit.tools.catalog.ToolSpec;

public final class OpenAiToolsJsonLoader implements ToolCatalogLoader {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public boolean supports(String contentTypeOrFilename) {
		String n = contentTypeOrFilename.toLowerCase();
		return n.endsWith(".json");
	}

	@Override
	public ToolCatalog load(InputStream in, URI source) throws IOException {
		JsonNode root = MAPPER.readTree(in);
		JsonNode tools = root.get("tools");
		List<ToolSpec> specs = new ArrayList<>();
		if (tools != null && tools.isArray()) {
			for (JsonNode tool : tools) {
				JsonNode fn = tool.get("function");
				if (fn == null) continue;
				String name = text(fn, "name");
				String description = text(fn, "description");
				JsonNode params = fn.get("parameters");
				Map<String, Object> risk = asMap(fn.get("x-risk"));
				Map<String, Object> guardrails = asMap(fn.get("x-guardrails"));
				specs.add(new ToolSpec(name, description, params, risk, guardrails));
			}
		}
		Map<String,Object> meta = new HashMap<>();
		meta.put("source", source.toString());
		return new ToolCatalog("openai-tools-json", specs, meta);
	}

	private static String text(JsonNode node, String field) {
		JsonNode v = node.get(field);
		return v == null || v.isNull() ? null : v.asText();
	}

	private static Map<String, Object> asMap(JsonNode node) throws IOException {
		if (node == null || node.isNull()) return null;
		return MAPPER.convertValue(node, MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
	}
}
