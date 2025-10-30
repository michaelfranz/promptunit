package org.promptunit.tools.command.dispatch;

import java.util.Optional;
import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.CommandPriority;

public interface CommandMappingPolicy {
    CommandPriority resolvePriority(ToolInvocation invocation);
    Optional<String> resolveAffinity(ToolInvocation invocation);
}


