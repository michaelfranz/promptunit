package org.promptunit.providers.ollama;

import java.util.List;
import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.Command;
import org.promptunit.tools.command.dispatch.CommandMappingPolicy;
import org.promptunit.tools.command.factory.ToolCommandFactory;

/**
 * Scaffold for Ollama provider. Implement create() to map tool invocations to Commands.
 */
public class OllamaToolCommandFactory implements ToolCommandFactory {
    @Override
    public boolean supports(String provider, String model, String toolId, String toolVersion) {
        return "Ollama".equals(provider);
    }

    @Override
    public List<Command> create(String provider, String model, String toolId, String toolVersion, ToolInvocation invocation, CommandMappingPolicy policy) {
        throw new UnsupportedOperationException("OllamaToolCommandFactory is a scaffold. Provide a concrete implementation.");
    }
}


