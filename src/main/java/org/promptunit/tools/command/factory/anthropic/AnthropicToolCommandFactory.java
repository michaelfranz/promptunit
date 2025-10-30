package org.promptunit.tools.command.factory.anthropic;

import java.util.List;
import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.Command;
import org.promptunit.tools.command.dispatch.CommandMappingPolicy;
import org.promptunit.tools.command.factory.ToolCommandFactory;

/**
 * Scaffold for Anthropic provider. Implement create() to map tool invocations to Commands.
 */
public class AnthropicToolCommandFactory implements ToolCommandFactory {
    @Override
    public boolean supports(String provider, String model, String toolId, String toolVersion) {
        return "Anthropic".equals(provider);
    }

    @Override
    public List<Command> create(String provider, String model, String toolId, String toolVersion, ToolInvocation invocation, CommandMappingPolicy policy) {
        throw new UnsupportedOperationException("AnthropicToolCommandFactory is a scaffold. Provide a concrete implementation.");
    }
}


