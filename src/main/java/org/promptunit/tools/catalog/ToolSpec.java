package org.promptunit.tools.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;

public final class ToolSpec {
	private final String name;
	private final String description;
	private final JsonNode parametersSchema;
	private final Map<String, Object> risk;
	private final Map<String, Object> guardrails;

	public ToolSpec(String name,
	               String description,
	               JsonNode parametersSchema,
	               Map<String, Object> risk,
	               Map<String, Object> guardrails) {
		this.name = Objects.requireNonNull(name, "name");
		this.description = description;
		this.parametersSchema = parametersSchema;
		this.risk = risk;
		this.guardrails = guardrails;
	}

	public String getName() { return name; }
	public String getDescription() { return description; }
	public JsonNode getParametersSchema() { return parametersSchema; }
	public Map<String, Object> getRisk() { return risk; }
	public Map<String, Object> getGuardrails() { return guardrails; }
}
