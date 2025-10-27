package org.promptunit.mock.commands.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.CommandQueue;
import org.promptunit.tools.command.CommandPriority;
import org.promptunit.tools.command.dispatch.CommandMappingPolicy;
import org.promptunit.tools.command.dispatch.DefaultCommandMappingPolicy;
import org.promptunit.tools.command.dispatch.ToolToCommandDispatcher;
import org.promptunit.tools.command.factory.ToolCommandFactoryRegistry;
import org.promptunit.tools.command.factory.anthropic.AnthropicToolCommandFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicToolCommandFactoryTest {

    @Test
    void registrySelectsAnthropicFactory_andDispatchThrowsUntilImplemented() {
        ToolCommandFactoryRegistry registry = ToolCommandFactoryRegistry.of(List.of(new AnthropicToolCommandFactory()));
        ToolToCommandDispatcher dispatcher = new ToolToCommandDispatcher(registry, "Anthropic", "claude-3");
        CommandMappingPolicy policy = new DefaultCommandMappingPolicy(Map.of("create_ticket", CommandPriority.MEDIUM));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode args = mapper.createObjectNode()
                .put("projectKey", "PRJ")
                .put("summary", "Bug");
        ToolInvocation inv = new ToolInvocation("create_ticket", args);

        CommandQueue queue = new CommandQueue();

        assertThatThrownBy(() -> dispatcher.dispatch(List.of(inv), queue, policy))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(queue.sizePending()).isZero();
    }
}


