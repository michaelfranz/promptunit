package org.promptunit.mock.commands.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.Command;
import org.promptunit.tools.command.CommandQueue;
import org.promptunit.tools.command.CommandPriority;
import org.promptunit.tools.command.dispatch.CommandMappingPolicy;
import org.promptunit.tools.command.dispatch.DefaultCommandMappingPolicy;
import org.promptunit.tools.command.dispatch.ToolToCommandDispatcher;
import org.promptunit.tools.command.factory.ToolCommandFactory;
import org.promptunit.tools.command.factory.ToolCommandFactoryRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolCommandFactoryRegistryVersioningTest {

    static class VersionedFactoryV1 implements ToolCommandFactory {
        @Override
        public boolean supports(String provider, String model, String toolId, String toolVersion) {
            return "OpenAI".equals(provider)
                    && "gpt-4o".equals(model)
                    && "create_ticket".equals(toolId)
                    && toolVersion != null && toolVersion.equals("v1");
        }

        @Override
        public List<Command> create(String provider, String model, String toolId, String toolVersion, ToolInvocation invocation, CommandMappingPolicy policy) {
            throw new UnsupportedOperationException("v1");
        }
    }

    static class VersionedFactoryV2 implements ToolCommandFactory {
        @Override
        public boolean supports(String provider, String model, String toolId, String toolVersion) {
            return "OpenAI".equals(provider)
                    && "gpt-4o".equals(model)
                    && "create_ticket".equals(toolId)
                    && toolVersion != null && toolVersion.equals("v1");
        }

        @Override
        public List<Command> create(String provider, String model, String toolId, String toolVersion, ToolInvocation invocation, CommandMappingPolicy policy) {
            throw new UnsupportedOperationException("v2");
        }
    }

    static class NoVersionFactory implements ToolCommandFactory {
        @Override
        public boolean supports(String provider, String model, String toolId, String toolVersion) {
            return "OpenAI".equals(provider)
                    && "gpt-4o".equals(model)
                    && "create_ticket".equals(toolId)
                    && toolVersion.isEmpty();
        }

        @Override
        public List<Command> create(String provider, String model, String toolId, String toolVersion, ToolInvocation invocation, CommandMappingPolicy policy) {
            throw new UnsupportedOperationException("noversion");
        }
    }

    @Test
    void exactVersion_match_selectsCorrectFactory() {
        ToolCommandFactoryRegistry registry = ToolCommandFactoryRegistry.of(List.of(new VersionedFactoryV1(), new VersionedFactoryV2()));
        ToolToCommandDispatcher dispatcher = new ToolToCommandDispatcher(registry, "OpenAI", "gpt-4o");
        CommandMappingPolicy policy = new DefaultCommandMappingPolicy(Map.of("create_ticket", CommandPriority.MEDIUM));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode args = mapper.createObjectNode().put("projectKey", "PRJ");
        ToolInvocation inv = new ToolInvocation("create_ticket", "v2", args);

        CommandQueue queue = new CommandQueue();

        assertThatThrownBy(() -> dispatcher.dispatch(List.of(inv), queue, policy))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("v2");
    }

    @Test
    void noVersion_match_requiresFactoriesThatSupportEmptyVersion() {
        ToolCommandFactoryRegistry registry = ToolCommandFactoryRegistry.of(List.of(new VersionedFactoryV1(), new NoVersionFactory()));
        ToolToCommandDispatcher dispatcher = new ToolToCommandDispatcher(registry, "OpenAI", "gpt-4o");
        CommandMappingPolicy policy = new DefaultCommandMappingPolicy(Map.of("create_ticket", CommandPriority.MEDIUM));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode args = mapper.createObjectNode().put("projectKey", "PRJ");
        ToolInvocation inv = new ToolInvocation("create_ticket", args); // no version

        CommandQueue queue = new CommandQueue();

        assertThatThrownBy(() -> dispatcher.dispatch(List.of(inv), queue, policy))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("noversion");
    }
}


