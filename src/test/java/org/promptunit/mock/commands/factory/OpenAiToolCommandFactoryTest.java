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
import org.promptunit.tools.command.factory.ToolCommandFactory;
import org.promptunit.tools.command.factory.ToolCommandFactoryRegistry;
import org.promptunit.mock.ticketapp.CreateTicketCommand;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiToolCommandFactoryTest {

    static class OpenAiCreateTicketFactory implements ToolCommandFactory {
        @Override
        public boolean supports(String provider, String model, String toolId, String toolVersion) {
            return "OpenAI".equals(provider) && "create_ticket".equals(toolId);
        }

        @Override
        public List<org.promptunit.tools.command.Command> create(String provider, String model, String toolId, String toolVersion, ToolInvocation invocation, CommandMappingPolicy policy) {
            CommandPriority prio = policy.resolvePriority(invocation);
            Optional<String> affinity = policy.resolveAffinity(invocation);
            ObjectNode args = (ObjectNode) invocation.args();
            String projectKey = args.get("projectKey").asText();
            String summary = args.get("summary").asText();
            String description = args.get("description").asText();
            return List.of(new CreateTicketCommand(projectKey, summary, description, prio));
        }
    }

    @Test
    void dispatches_openai_create_ticket_into_queue() {
        ToolCommandFactoryRegistry registry = ToolCommandFactoryRegistry.of(List.of(new OpenAiCreateTicketFactory()));
        ToolToCommandDispatcher dispatcher = new ToolToCommandDispatcher(registry, "OpenAI", "gpt-4o");
        CommandMappingPolicy policy = new DefaultCommandMappingPolicy(Map.of("create_ticket", CommandPriority.HIGH));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode args = mapper.createObjectNode()
                .put("projectKey", "PRJ")
                .put("summary", "Bug")
                .put("description", "Fix it");
        ToolInvocation inv = new ToolInvocation("create_ticket", args);

        CommandQueue queue = new CommandQueue();
        dispatcher.dispatch(List.of(inv), queue, policy);

        assertThat(queue.sizePending()).isEqualTo(1);
        assertThat(queue.getPendingSnapshot().getFirst()).isInstanceOf(CreateTicketCommand.class);
        CreateTicketCommand c = (CreateTicketCommand) queue.getPendingSnapshot().getFirst();
        assertThat(c.getProjectKey()).isEqualTo("PRJ");
        assertThat(c.getPriority()).isEqualTo(CommandPriority.HIGH);
    }
}


