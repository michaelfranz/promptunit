package org.promptunit.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.promptunit.tools.ToolRefs.springAITool;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

public class ToolRefsSpringAITest {

    static class Customer {
        private final float balance;
        private final float pendingAmount;

        Customer(float balance, float pendingAmount) {
            this.balance = balance;
            this.pendingAmount = pendingAmount;
        }

        @Tool(description = "Find the balance of a customer by id")
        public float balance(boolean includePending) {
            return includePending ? balance + pendingAmount : balance;
        }

        @Tool(name = "overdraftLimit", description = "Get overdraft limit")
        public int limit() { return 5; }

        @Tool(description = "Arity 0")
        public int m0() { return 0; }

        @Tool(description = "Arity 1")
        public int m1(int a1) { return a1; }

        @Tool(description = "Arity 2")
        public int m2(int a1, String a2) { return a1 + a2.length(); }

        @Tool(description = "Arity 3")
        public int m3(int a1, String a2, boolean a3) { return a1 + a2.length() + (a3 ? 1 : 0); }

        @Tool(description = "Arity 4")
        public int m4(int a1, String a2, boolean a3, double a4) { return a1 + a2.length() + (a3 ? 1 : 0) + (int) a4; }

        // Static should be rejected
        @Tool(description = "Static not allowed")
        public static int s0() { return 0; }

        // Not annotated should be rejected
        public int notATool() { return -1; }
    }

    @Test
    void resolvesNameFromAnnotationNameWhenPresent() {
        ToolRef ref = springAITool(Customer::limit);
        assertThat(ref.provider()).isEqualTo("spring-ai");
        assertThat(ref.name()).isEqualTo("overdraftLimit");
        assertThat(ref.method()).isPresent();
        Method m = ref.method().orElseThrow();
        assertThat(m.getName()).isEqualTo("limit");
    }

    @Test
    void fallsBackToMethodNameWhenAnnotationNameMissing() {
        ToolRef ref = springAITool(Customer::balance);
        assertThat(ref.name()).isEqualTo("balance");
    }

    @Test
    void rejectsStaticMethods() {
        assertThatThrownBy(() -> springAITool((ToolRefs.MethodRef0<Customer, Integer>) (Customer c) -> Customer.s0()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instance method reference");
    }

    @Test
    void rejectsMethodsWithoutToolAnnotation() {
        assertThatThrownBy(() -> springAITool(Customer::notATool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not annotated");
    }

    @Test
    void supportsArity0Through4() {
        assertThat(springAITool(Customer::m0).name()).isEqualTo("m0");
        assertThat(springAITool(Customer::m1).name()).isEqualTo("m1");
        assertThat(springAITool(Customer::m2).name()).isEqualTo("m2");
        assertThat(springAITool(Customer::m3).name()).isEqualTo("m3");
        assertThat(springAITool(Customer::m4).name()).isEqualTo("m4");
    }
}


