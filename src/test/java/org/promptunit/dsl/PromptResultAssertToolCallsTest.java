package org.promptunit.dsl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.promptunit.tools.ToolRefs.springAITool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.promptunit.core.PromptResult;
import org.promptunit.tools.NormalizedToolCall;
import org.promptunit.tools.ToolCall;
import org.springframework.ai.tool.annotation.Tool;

public class PromptResultAssertToolCallsTest {

    static class Customer {
        private final float balance;
        private final float pendingAmount;
        Customer(float balance, float pendingAmount) { this.balance = balance; this.pendingAmount = pendingAmount; }
        @Tool(description = "Find the balance of a customer by id")
        public float balance(boolean includePending) {
            return includePending ? balance + pendingAmount : balance;
        }
        @Tool(name = "addComment")
        public int addComment(String text) { return text.length(); }
    }

    private static final ObjectMapper M = new ObjectMapper();

    @Test
    void toolCallExactlyOnceAndArgsSubset() throws Exception {
        JsonNode args = M.readTree("{\"includePending\":true, \"id\": 123}");
        ToolCall c1 = new NormalizedToolCall("balance", args);
        PromptResult pr = new PromptResult("", 10, 0.0, 5, null, null, List.of(c1));

        PromptAssertions.assertThatResult(pr)
                .hasToolCall(springAITool(Customer::balance))
                .withArgsSubset("{\"includePending\":true}");
    }

    @Test
    void toolCallsExactlyInAnyOrder() throws Exception {
        ToolCall c1 = new NormalizedToolCall("balance", M.readTree("{\"includePending\":false}"));
        ToolCall c2 = new NormalizedToolCall("addComment", M.readTree("{\"text\":\"hi\"}"));
        PromptResult pr = new PromptResult("", 10, 0.0, 5, null, null, List.of(c2, c1));

        PromptAssertions.assertThatResult(pr)
                .hasToolCallsExactlyInAnyOrder(
                        springAITool(Customer::balance),
                        springAITool(Customer::addComment));
    }

    @Test
    void toolCallsExactlyInOrder() throws Exception {
        ToolCall c1 = new NormalizedToolCall("balance", M.readTree("{\"includePending\":true}"));
        ToolCall c2 = new NormalizedToolCall("addComment", M.readTree("{\"text\":\"hi\"}"));
        PromptResult pr = new PromptResult("", 10, 0.0, 5, null, null, List.of(c1, c2));

        PromptAssertions.assertThatResult(pr)
                .hasToolCallsExactlyInOrder(
                        springAITool(Customer::balance),
                        springAITool(Customer::addComment));
    }

    @Test
    void missingToolCallsMetadataFailsFast() {
        PromptResult pr = new PromptResult("", 10, 0.0, 5);
        assertThatThrownBy(() -> PromptAssertions.assertThatResult(pr).hasToolCall(springAITool(Customer::balance)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("No tool call metadata");
    }
}


