package org.promptunit.tools;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * A reference to a tool definition coming from a specific framework (e.g., Spring AI).
 * Instances are typically created via helper factories like {@code ToolRefs.springAITool(...)}.
 */
public interface ToolRef {
    /**
     * Canonical tool name used by the provider when reporting tool calls.
     */
    String name();

    /**
     * Identifier for the originating framework/provider (e.g., "spring-ai").
     */
    String provider();

    /**
     * Optionally expose the reflected {@link Method} backing this reference when available.
     */
    Optional<Method> method();
}


