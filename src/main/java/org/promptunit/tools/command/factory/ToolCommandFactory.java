package org.promptunit.tools.command.factory;

import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.Command;
import org.promptunit.tools.command.dispatch.CommandMappingPolicy;

import java.util.List;
import java.util.Optional;

public interface ToolCommandFactory {

    boolean supports(String provider, String model, String toolId, Optional<String> toolVersion);

    List<Command> create(String provider,
                         String model,
                         String toolId,
                         Optional<String> toolVersion,
                         ToolInvocation invocation,
                         CommandMappingPolicy policy);
}


