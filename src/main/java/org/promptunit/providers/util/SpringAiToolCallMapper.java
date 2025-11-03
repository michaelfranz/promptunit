package org.promptunit.providers.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.promptunit.tools.NormalizedToolCall;
import org.promptunit.tools.ToolCall;

/**
 * Best-effort reflection-based mapper from Spring AI AssistantMessage/ToolCall models
 * to PromptUnit's provider-agnostic {@link ToolCall}.
 *
 * Avoids tight coupling to specific Spring AI versions by using reflection.
 */
public final class SpringAiToolCallMapper {
    private SpringAiToolCallMapper() {}

    public static List<ToolCall> fromAssistantMessage(Object assistantMessage, ObjectMapper mapper) {
        if (assistantMessage == null) return null;
        try {
            Method getToolCalls = findMethod(assistantMessage.getClass(), "getToolCalls");
            if (getToolCalls == null) return null;
            Object rawList = getToolCalls.invoke(assistantMessage);
            if (!(rawList instanceof List<?> list) || list.isEmpty()) return Collections.emptyList();
            List<ToolCall> out = new ArrayList<>();
            for (Object tc : list) {
                String name = extractName(tc);
                JsonNode args = extractArgs(tc, mapper);
                if (name != null && args != null) {
                    out.add(new NormalizedToolCall(name, args));
                }
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            try {
                Method m = type.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ex) {
                Class<?> sup = type.getSuperclass();
                if (sup != null) return findMethod(sup, name, params);
                return null;
            }
        }
    }

    private static String extractName(Object toolCall) {
        try {
            // Try tc.getFunction().getName()
            Method getFunction = findMethod(toolCall.getClass(), "getFunction");
            if (getFunction != null) {
                Object func = getFunction.invoke(toolCall);
                if (func != null) {
                    Method getName = findMethod(func.getClass(), "getName");
                    if (getName != null) {
                        Object n = getName.invoke(func);
                        return n == null ? null : String.valueOf(n);
                    }
                }
            }
            // Fallback: tc.getName()
            Method getName = findMethod(toolCall.getClass(), "getName");
            if (getName != null) {
                Object n = getName.invoke(toolCall);
                return n == null ? null : String.valueOf(n);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static JsonNode extractArgs(Object toolCall, ObjectMapper mapper) {
        try {
            // Try tc.getFunction().getArguments()
            Method getFunction = findMethod(toolCall.getClass(), "getFunction");
            if (getFunction != null) {
                Object func = getFunction.invoke(toolCall);
                if (func != null) {
                    Method getArguments = findMethod(func.getClass(), "getArguments");
                    if (getArguments != null) {
                        Object args = getArguments.invoke(func);
                        return coerceToJsonNode(args, mapper);
                    }
                }
            }
            // Fallback: tc.getArguments()
            Method getArguments = findMethod(toolCall.getClass(), "getArguments");
            if (getArguments != null) {
                Object args = getArguments.invoke(toolCall);
                return coerceToJsonNode(args, mapper);
            }
        } catch (Exception ignored) {}
        return mapper.createObjectNode();
    }

    private static JsonNode coerceToJsonNode(Object value, ObjectMapper mapper) {
        try {
            if (value == null) return mapper.createObjectNode();
            if (value instanceof CharSequence cs) {
                String s = cs.toString();
                if (s.isBlank()) return mapper.createObjectNode();
                return mapper.readTree(s);
            }
            if (value instanceof JsonNode jn) return jn;
            return mapper.valueToTree(value);
        } catch (Exception e) {
            ObjectNode err = mapper.createObjectNode();
            err.put("_unparsed", String.valueOf(value));
            return err;
        }
    }
}


