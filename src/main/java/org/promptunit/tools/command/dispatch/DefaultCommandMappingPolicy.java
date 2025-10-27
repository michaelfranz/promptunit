package org.promptunit.tools.command.dispatch;

import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.CommandPriority;

import java.util.Map;
import java.util.Optional;

public final class DefaultCommandMappingPolicy implements CommandMappingPolicy {
    private final Map<String, CommandPriority> priorityByTool;

    public DefaultCommandMappingPolicy(Map<String, CommandPriority> priorityByTool) {
        this.priorityByTool = priorityByTool;
    }

    @Override
    public CommandPriority resolvePriority(ToolInvocation invocation) {
        return priorityByTool.getOrDefault(invocation.getTool(), CommandPriority.MEDIUM);
    }

    @Override
    public Optional<String> resolveAffinity(ToolInvocation invocation) {
        // Default: try args.ticketId or args.projectKey if present
        if (invocation.getArgs() != null) {
            if (invocation.getArgs().hasNonNull("ticketId")) {
                return Optional.of(invocation.getArgs().get("ticketId").asText());
            }
            if (invocation.getArgs().hasNonNull("projectKey")) {
                return Optional.of(invocation.getArgs().get("projectKey").asText());
            }
        }
        return Optional.empty();
    }
}


