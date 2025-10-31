package org.promptunit.core;

import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

public record OutputSchema(String jsonSchema) {

    /**
     * Helper to generate JSON Schemas from Java types.
     * <p>
     * Metadata such as descriptions and required properties can be specified using one of the
     * following supported annotations:
     * <p>
     * <ul>
     * <li>{@code @ToolParam(required = ..., description = ...)} - org.springframework.ai.tool.annotation.ToolParam</li>
     * <li>{@code @JsonProperty(required = ...)} - com.fasterxml.jackson.annotation.JsonProperty</li>
     * <li>{@code @JsonClassDescription(...)} - com.fasterxml.jackson.annotation.JsonClassDescription</li>
     * <li>{@code @JsonPropertyDescription(...)} - com.fasterxml.jackson.annotation.JsonPropertyDescription</li>
     * <li>{@code @Schema(required = ..., description = ...)} - io.swagger.v3.oas.annotations.media.Schema</li>
     * <li>{@code @Nullable} - org.springframework.lang.Nullable</li>
     * </ul>
     * <p>
     * If none of these annotations are present, the default behavior is to consider the
     * property as required and not to include a description.
     * <p>
     */
    public static OutputSchema from(Class<?> outputSchemaDefinitionClass, JsonSchemaGenerator.SchemaOption... schemaOptions) {
        String schemaJsonString = JsonSchemaGenerator.generateForType(outputSchemaDefinitionClass, schemaOptions);
        return new OutputSchema(schemaJsonString);
    }
}
