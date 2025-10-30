package org.promptunit.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

class PromptInstanceTest {

    @Nested
    class BuilderValidation {
        @Test
        void build_withSystemOnly_succeeds() {
            PromptInstance pi = PromptInstance.builder()
                    .addSystemMessage("You are a helpful assistant")
                    .withModel("gpt-4o")
                    .withProvider("openai")
                    .withTemperature(0.2f)
                    .withTopP(0.9f)
                    .withMaxTokens(256)
                    .build();

            assertThat(pi.conversation()).hasSize(1);
            assertThat(pi.conversation().getFirst()).isInstanceOf(SystemMessage.class);
            assertThat(pi.model()).isEqualTo("gpt-4o");
            assertThat(pi.provider()).isEqualTo("openai");
            assertThat(pi.temperature()).isEqualTo(0.2f);
            assertThat(pi.topP()).isEqualTo(0.9f);
            assertThat(pi.maxTokens()).isEqualTo(256);
            assertThat(pi.outputSchema()).isEmpty();
        }

        @Test
        void build_withUserOnly_succeeds() {
            PromptInstance pi = PromptInstance.builder()
                    .addUserMessage("Hi")
                    .build();

            assertThat(pi.conversation()).hasSize(1);
            assertThat(pi.conversation().getFirst()).isInstanceOf(UserMessage.class);
        }

        @Test
        void build_withAssistantOnly_throws() {
            assertThatThrownBy(() -> PromptInstance.builder()
                    .addAssistantMessage("Hello from assistant only")
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires at least a non-blank systemMessage or userMessage");
        }

        @Test
        void build_withBlankSystemAndBlankUser_throws() {
            assertThatThrownBy(() -> PromptInstance.builder()
                    .addSystemMessage("  \t\n")
                    .addUserMessage(" ")
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires at least a non-blank systemMessage or userMessage");
        }

        @Test
        void outputSchema_presentWhenProvided_andEmptyOtherwise() {
            OutputSchema schema = new OutputSchema("{\n  \"type\": \"object\"\n}");
            PromptInstance withSchema = PromptInstance.builder()
                    .addUserMessage("Hi")
                    .withOutputSchema(schema)
                    .build();
            assertThat(withSchema.outputSchema()).isPresent();
            assertThat(withSchema.outputSchema()).contains(schema);

            PromptInstance noSchema = PromptInstance.builder()
                    .addUserMessage("Hi")
                    .withOutputSchema(null)
                    .build();
            assertThat(noSchema.outputSchema()).isEmpty();
        }

        @Test
        void conversation_isImmutableAfterBuild() {
            PromptInstance pi = PromptInstance.builder()
                    .addSystemMessage("S")
                    .addUserMessage("U")
                    .addAssistantMessage("A")
                    .build();

            List<org.springframework.ai.chat.messages.Message> snapshot = pi.conversation();
            assertThat(snapshot).hasSize(3);
            assertThatThrownBy(() -> snapshot.add(new SystemMessage("X")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class AccessorsAndFormatting {
        @Test
        void messageTypeAggregations_joinWithNewlines() {
            PromptInstance pi = PromptInstance.builder()
                    .addSystemMessage("sys1")
                    .addSystemMessage("sys2")
                    .addUserMessage("u1")
                    .addUserMessage("u2")
                    .addAssistantMessage("a1")
                    .addAssistantMessage("a2")
                    .build();

            assertThat(pi.systemMessagesAsString()).isEqualTo("sys1\nsys2");
            assertThat(pi.userMessagesAsString()).isEqualTo("u1\nu2");
            assertThat(pi.assistantMessagesAsString()).isEqualTo("a1\na2");
        }

        @Test
        void conversationAsString_labeledAndSeparatedByBlankLines() {
            PromptInstance pi = PromptInstance.builder()
                    .addSystemMessage("S")
                    .addUserMessage("U")
                    .addAssistantMessage("A")
                    .build();

            String expected = String.join("\n\n",
                    "system: S",
                    "user: U",
                    "assistant: A");

            // Note: method is intentionally spelled 'conversaionAsString' in source
            assertThat(pi.conversaionAsString()).isEqualTo(expected);
        }

        @Test
        void conversation_preservesOrderAndTypes() {
            PromptInstance pi = PromptInstance.builder()
                    .addSystemMessage("S1")
                    .addUserMessage("U1")
                    .addAssistantMessage("A1")
                    .addSystemMessage("S2")
                    .build();

            assertThat(pi.conversation())
                    .hasSize(4)
                    .satisfies(list -> {
                        assertThat(list.get(0)).isInstanceOf(SystemMessage.class);
                        assertThat(list.get(1)).isInstanceOf(UserMessage.class);
                        assertThat(list.get(2)).isInstanceOf(AssistantMessage.class);
                        assertThat(list.get(3)).isInstanceOf(SystemMessage.class);
                    });
        }
    }

    @Test
    void builder_isFluent_andSetsAllFields() {
        OutputSchema schema = new OutputSchema("{\"type\":\"object\"}");
        PromptInstance pi = PromptInstance.builder()
                .addSystemMessage("S")
                .addUserMessage("U")
                .addAssistantMessage("A")
                .withModel("m")
                .withProvider("p")
                .withTemperature(0.5f)
                .withTopP(0.7f)
                .withMaxTokens(1024)
                .withOutputSchema(schema)
                .build();

        assertThat(pi.model()).isEqualTo("m");
        assertThat(pi.provider()).isEqualTo("p");
        assertThat(pi.temperature()).isEqualTo(0.5f);
        assertThat(pi.topP()).isEqualTo(0.7f);
        assertThat(pi.maxTokens()).isEqualTo(1024);
        assertThat(pi.outputSchema()).isEqualTo(Optional.of(schema));
        assertThat(pi.systemMessagesAsString()).isEqualTo("S");
        assertThat(pi.userMessagesAsString()).isEqualTo("U");
        assertThat(pi.assistantMessagesAsString()).isEqualTo("A");
    }
}
