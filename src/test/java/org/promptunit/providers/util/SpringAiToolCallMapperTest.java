package org.promptunit.providers.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.promptunit.tools.ToolCall;

public class SpringAiToolCallMapperTest {

    // Fake minimal shapes to drive reflection-based extraction
    static class FakeFunction {
        private final String name;
        private final String arguments;
        FakeFunction(String name, String arguments) { this.name = name; this.arguments = arguments; }
        public String getName() { return name; }
        public String getArguments() { return arguments; }
    }

    static class FakeToolCall {
        private final FakeFunction function;
        FakeToolCall(FakeFunction function) { this.function = function; }
        public FakeFunction getFunction() { return function; }
    }

    static class FakeAssistantMessage {
        private final java.util.List<FakeToolCall> toolCalls;
        FakeAssistantMessage(java.util.List<FakeToolCall> toolCalls) { this.toolCalls = toolCalls; }
        public java.util.List<FakeToolCall> getToolCalls() { return toolCalls; }
    }

    @Test
    void mapsFunctionToolCalls() {
        FakeAssistantMessage msg = new FakeAssistantMessage(List.of(
                new FakeToolCall(new FakeFunction("balance", "{\"includePending\":true}")),
                new FakeToolCall(new FakeFunction("addComment", "{\"text\":\"hi\"}"))
        ));

        List<ToolCall> out = SpringAiToolCallMapper.fromAssistantMessage(msg, new ObjectMapper());
        assertThat(out).isNotNull();
        assertThat(out).hasSize(2);
        assertThat(out.get(0).name()).isEqualTo("balance");
        assertThat(out.get(0).args().get("includePending").asBoolean()).isTrue();
        assertThat(out.get(1).name()).isEqualTo("addComment");
        assertThat(out.get(1).args().get("text").asText()).isEqualTo("hi");
    }

    @Test
    void returnsNullWhenNoMethodPresent() {
        Object notAssistant = new Object();
        List<ToolCall> out = SpringAiToolCallMapper.fromAssistantMessage(notAssistant, new ObjectMapper());
        assertThat(out).isNull();
    }
}


