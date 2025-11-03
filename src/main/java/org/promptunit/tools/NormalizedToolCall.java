package org.promptunit.tools;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Default normalized implementation of {@link ToolCall} with name, optional version, and JSON args.
 */
public record NormalizedToolCall(String name, String versionOrNull, JsonNode args) implements ToolCall {

    public NormalizedToolCall(String name, JsonNode args) {
        this(name, null, args);
    }

    @Override
    public Optional<String> version() {
        return Optional.ofNullable(versionOrNull);
    }
}


