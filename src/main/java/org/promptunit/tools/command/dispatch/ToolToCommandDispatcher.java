package org.promptunit.tools.command.dispatch;

import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.Command;
import org.promptunit.tools.command.CommandQueue;
import org.promptunit.tools.command.EnqueueResult;
import org.promptunit.tools.command.factory.ToolCommandFactory;
import org.promptunit.tools.command.factory.ToolCommandFactoryRegistry;

import java.util.List;
import java.util.Optional;

public final class ToolToCommandDispatcher {
    private final ToolCommandFactoryRegistry registry;
    private final String provider;
    private final String model;

    public ToolToCommandDispatcher(ToolCommandFactoryRegistry registry, String provider, String model) {
        this.registry = registry;
        this.provider = provider;
        this.model = model;
    }

    public DispatchResult dispatch(List<ToolInvocation> invocations, CommandQueue queue, CommandMappingPolicy policy) {
        DispatchResult result = new DispatchResult();
        for (ToolInvocation inv : invocations) {
            String toolId = inv.getTool();
            Optional<String> toolVersion = inv.getToolVersion();
            ToolCommandFactory factory = registry.findFactory(provider, model, toolId, toolVersion)
                    .orElseThrow(() -> new AssertionError("No ToolCommandFactory for provider=" + provider + ", model=" + model + ", tool=" + toolId));

            List<Command> commands = factory.create(provider, model, toolId, toolVersion, inv, policy);
            for (Command c : commands) {
                EnqueueResult r = queue.enqueue(c);
                result.add(inv, c, r);
            }
        }
        return result;
    }
}


