package org.promptunit.tools.command.factory;

import java.util.List;
import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.Command;
import org.promptunit.tools.command.dispatch.CommandMappingPolicy;

public interface ToolCommandFactory {

    boolean supports(String provider, String model, String toolId, String toolVersion);

    List<Command> create(String provider,
						 String model,
						 String toolId,
						 String toolVersion,
						 ToolInvocation invocation,
						 CommandMappingPolicy policy);
}


