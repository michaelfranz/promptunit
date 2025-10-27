package org.promptunit.tools.command.dispatch;

import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.CommandPriority;

import java.util.Optional;

public interface CommandMappingPolicy {
    CommandPriority resolvePriority(ToolInvocation invocation);
    Optional<String> resolveAffinity(ToolInvocation invocation);
}


