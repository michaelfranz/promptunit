package org.promptunit.tools;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class NormalizedToolCallTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void constructsWithThreeArgs() throws Exception {
        JsonNode args = MAPPER.readTree("{\"includePending\":true}");
        NormalizedToolCall c = new NormalizedToolCall("balance", "v1", args);
        assertThat(c.name()).isEqualTo("balance");
        assertThat(c.version()).isPresent().contains("v1");
        assertThat(c.args()).isEqualTo(args);
    }

    @Test
    void constructsWithoutVersion() throws Exception {
        JsonNode args = MAPPER.readTree("{\"id\":123}");
        NormalizedToolCall c = new NormalizedToolCall("getCustomer", args);
        assertThat(c.name()).isEqualTo("getCustomer");
        assertThat(c.version()).isEmpty();
        assertThat(c.args()).isEqualTo(args);
    }
}


