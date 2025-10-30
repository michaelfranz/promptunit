package org.promptunit.tools.command.dispatch;

import java.util.Map;
import java.util.Optional;
import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.CommandPriority;

public final class DefaultCommandMappingPolicy implements CommandMappingPolicy {
    private final Map<String, CommandPriority> priorityByTool;

    public DefaultCommandMappingPolicy(Map<String, CommandPriority> priorityByTool) {
        this.priorityByTool = priorityByTool;
    }

    @Override
    public CommandPriority resolvePriority(ToolInvocation invocation) {
        return priorityByTool.getOrDefault(invocation.tool(), CommandPriority.MEDIUM);
    }

    @Override
    public Optional<String> resolveAffinity(ToolInvocation invocation) {
        return Optional.empty();
    }
}


