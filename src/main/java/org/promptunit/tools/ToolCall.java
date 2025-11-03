package org.promptunit.tools;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Provider-agnostic representation of a tool call emitted by the LLM/engine.
 */
public interface ToolCall {
    String name();
    Optional<String> version();
    JsonNode args();
}


