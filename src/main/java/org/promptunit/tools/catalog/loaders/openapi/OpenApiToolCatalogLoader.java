package org.promptunit.tools.catalog.loaders.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.promptunit.tools.catalog.ToolCatalog;
import org.promptunit.tools.catalog.ToolCatalogLoader;
import org.promptunit.tools.catalog.ToolSpec;

public final class OpenApiToolCatalogLoader implements ToolCatalogLoader {
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

	@Override
	public boolean supports(String contentTypeOrFilename) {
		String n = contentTypeOrFilename.toLowerCase();
		return n.endsWith(".yaml") || n.endsWith(".yml") || n.endsWith(".json");
	}

	@Override
	public ToolCatalog load(InputStream in, URI source) throws IOException {
		ObjectMapper mapper = choose(source.toString());
		JsonNode root = mapper.readTree(in);
		JsonNode paths = root.get("paths");
		List<ToolSpec> specs = new ArrayList<>();
		if (paths != null && paths.isObject()) {
			Iterator<String> it = paths.fieldNames();
			while (it.hasNext()) {
				String path = it.next();
				JsonNode item = paths.get(path);
				Iterator<String> methods = item.fieldNames();
				while (methods.hasNext()) {
					String method = methods.next();
					JsonNode op = item.get(method);
					if (!op.isObject()) continue;
					String name = text(op, "operationId");
					if (name == null) continue;
					String description = text(op, "description");
					JsonNode parametersSchema = extractParametersSchema(op);
					Map<String, Object> risk = extensions(op, "x-risk-");
					Map<String, Object> guardrails = asMap(op.get("x-guardrails"));
					specs.add(new ToolSpec(name, description, parametersSchema, risk, guardrails));
				}
			}
		}
		Map<String,Object> meta = new HashMap<>();
		meta.put("source", source.toString());
		return new ToolCatalog("openapi", specs, meta);
	}

	private static ObjectMapper choose(String name) {
		String n = name.toLowerCase();
		return (n.endsWith(".yaml") || n.endsWith(".yml")) ? YAML : JSON;
	}

	private static String text(JsonNode node, String field) {
		JsonNode v = node.get(field);
		return v == null || v.isNull() ? null : v.asText();
	}

	private static Map<String, Object> asMap(JsonNode node) throws IOException {
		if (node == null || node.isNull()) return null;
		ObjectMapper mapper = node.isTextual() ? JSON : JSON;
		return mapper.convertValue(node, mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
	}

	private static Map<String, Object> extensions(JsonNode op, String prefix) {
		Map<String, Object> out = new HashMap<>();
		Iterator<String> names = op.fieldNames();
		while (names.hasNext()) {
			String f = names.next();
			if (f.startsWith(prefix)) {
				out.put(f.substring(prefix.length()), op.get(f).asText());
			}
		}
		return out.isEmpty() ? null : out;
	}

	private static JsonNode extractParametersSchema(JsonNode op) {
		// Minimal: prefer requestBody schema; else build from parameters into an object
		JsonNode requestBody = op.get("requestBody");
		if (requestBody != null) {
			JsonNode content = requestBody.get("content");
			if (content != null && content.isObject()) {
				Iterator<String> types = content.fieldNames();
				while (types.hasNext()) {
					JsonNode media = content.get(types.next());
					JsonNode schema = media.get("schema");
					if (schema != null) return schema;
				}
			}
		}
		// Fallback: accumulate path/query params
		ObjectMapper m = JSON;
		Map<String, JsonNode> props = new HashMap<>();
		List<String> required = new ArrayList<>();
		JsonNode params = op.get("parameters");
		if (params != null && params.isArray()) {
			for (JsonNode p : params) {
				String name = text(p, "name");
				boolean req = p.get("required") != null && p.get("required").asBoolean(false);
				JsonNode schema = p.get("schema");
				if (name != null && schema != null) {
					props.put(name, schema);
					if (req) required.add(name);
				}
			}
		}
		Map<String, Object> root = new HashMap<>();
		root.put("type", "object");
		Map<String, Object> properties = new HashMap<>();
		for (Map.Entry<String, JsonNode> e : props.entrySet()) {
			properties.put(e.getKey(), m.convertValue(e.getValue(), Map.class));
		}
		root.put("properties", properties);
		if (!required.isEmpty()) root.put("required", required);
		root.put("additionalProperties", false);
		return m.valueToTree(root);
	}
}
