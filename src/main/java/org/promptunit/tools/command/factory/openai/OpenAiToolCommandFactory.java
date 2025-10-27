package org.promptunit.tools.command.factory.openai;

import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.Command;
import org.promptunit.tools.command.dispatch.CommandMappingPolicy;
import org.promptunit.tools.command.factory.ToolCommandFactory;

import java.util.List;

/**
 * Scaffold for OpenAI provider. Implement create() to map tool invocations to Commands.
 */
public class OpenAiToolCommandFactory implements ToolCommandFactory {
    @Override
    public boolean supports(String provider, String model, String toolId, String toolVersion) {
        return "OpenAI".equals(provider);
    }

    @Override
    public List<Command> create(String provider, String model, String toolId, String toolVersion, ToolInvocation invocation, CommandMappingPolicy policy) {
        throw new UnsupportedOperationException("OpenAiToolCommandFactory is a scaffold. Provide a concrete implementation.");
    }
}


