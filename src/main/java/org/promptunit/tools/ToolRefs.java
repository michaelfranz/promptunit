		package org.promptunit.tools;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Factory helpers to construct {@link ToolRef} instances from various ecosystems.
 */
public final class ToolRefs {
    private ToolRefs() {}

    // ---- Spring AI ----

    /**
     * Resolve an instance method annotated with @org.springframework.ai.tool.annotation.Tool (no params).
     */
    public static <T, R> ToolRef springAITool(MethodRef0<T, R> ref) {
        return springFromLambda(ref);
    }

    /**
     * Resolve an instance method annotated with @org.springframework.ai.tool.annotation.Tool (1 param).
     */
    public static <T, A1, R> ToolRef springAITool(MethodRef1<T, A1, R> ref) {
        return springFromLambda(ref);
    }

    /**
     * Resolve an instance method annotated with @org.springframework.ai.tool.annotation.Tool (2 params).
     */
    public static <T, A1, A2, R> ToolRef springAITool(MethodRef2<T, A1, A2, R> ref) {
        return springFromLambda(ref);
    }

    /**
     * Resolve an instance method annotated with @org.springframework.ai.tool.annotation.Tool (3 params).
     */
    public static <T, A1, A2, A3, R> ToolRef springAITool(MethodRef3<T, A1, A2, A3, R> ref) {
        return springFromLambda(ref);
    }

    /**
     * Resolve an instance method annotated with @org.springframework.ai.tool.annotation.Tool (4 params).
     */
    public static <T, A1, A2, A3, A4, R> ToolRef springAITool(MethodRef4<T, A1, A2, A3, A4, R> ref) {
        return springFromLambda(ref);
    }

    private static ToolRef springFromLambda(Serializable lambda) {
        Method method = resolveMethod(lambda);
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Expected an instance method reference, but got static method: " + method);
        }
        if (!hasAnnotationByName(method, "org.springframework.ai.tool.annotation.Tool")) {
            throw new IllegalArgumentException("Referenced method is not annotated with @org.springframework.ai.tool.annotation.Tool: " + method);
        }
        String toolName = extractAnnotationElement(method, "org.springframework.ai.tool.annotation.Tool", "name")
                .filter(n -> !n.isBlank())
                .orElse(method.getName());
        return new DefaultToolRef(toolName, "spring-ai", method);
    }

    private static boolean hasAnnotationByName(Method method, String fqcn) {
        return method != null && java.util.Arrays.stream(method.getAnnotations())
                .anyMatch(a -> a.annotationType().getName().equals(fqcn));
    }

    private static Optional<String> extractAnnotationElement(Method method, String fqcn, String element) {
        return java.util.Arrays.stream(method.getAnnotations())
                .filter(a -> a.annotationType().getName().equals(fqcn))
                .findFirst()
                .flatMap(a -> {
                    try {
                        Method m = a.annotationType().getMethod(element);
                        Object v = m.invoke(a);
                        return Optional.ofNullable(v == null ? null : String.valueOf(v));
                    } catch (ReflectiveOperationException e) {
                        return Optional.empty();
                    }
                });
    }

    private static Method resolveMethod(Serializable lambda) {
        SerializedLambda sl = extractSerializedLambda(lambda);
        String implClassName = sl.getImplClass().replace('/', '.');
        String methodName = sl.getImplMethodName();
        String methodSig = sl.getImplMethodSignature();
        int paramCount = countJvmDescriptorParams(methodSig);
        try {
            Class<?> implClass = Class.forName(implClassName);
            Method match = null;
            for (Method m : implClass.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == paramCount) {
                    match = m;
                    break;
                }
            }
            if (match == null) {
                // try public methods as a fallback (includes inherited)
                for (Method m : implClass.getMethods()) {
                    if (m.getName().equals(methodName) && m.getParameterCount() == paramCount) {
                        match = m;
                        break;
                    }
                }
            }
            if (match == null) {
                throw new IllegalArgumentException("Cannot resolve method from lambda: " + implClassName + "::" + methodName + methodSig);
            }
            try { match.setAccessible(true); } catch (Throwable ignored) {}
            return match;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot load class for lambda target: " + implClassName, e);
        }
    }

    private static int countJvmDescriptorParams(String desc) {
        // desc like (ZLjava/lang/String;I)F
        int i = desc.indexOf('(');
        int end = desc.indexOf(')');
        if (i < 0 || end < 0 || end <= i) return 0;
        int count = 0;
        for (int p = i + 1; p < end; ) {
            char c = desc.charAt(p);
            if (c == 'L') { // reference type: consume until ';'
                int semi = desc.indexOf(';', p);
                if (semi < 0) break;
                count++;
                p = semi + 1;
            } else if (c == '[') { // array: skip all '[' then one type
                p++;
                while (p < end && desc.charAt(p) == '[') p++;
                if (p < end && desc.charAt(p) == 'L') {
                    int semi = desc.indexOf(';', p);
                    if (semi < 0) break;
                    p = semi + 1;
                } else {
                    p++;
                }
                count++;
            } else { // primitive type
                count++;
                p++;
            }
        }
        return count;
    }

    private static SerializedLambda extractSerializedLambda(Serializable lambda) {
        try {
            Method m = lambda.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);
            Object ser = m.invoke(lambda);
            if (ser instanceof SerializedLambda sl) {
                return sl;
            }
        } catch (Exception e) {
            // fall through
        }
        throw new IllegalArgumentException("Provided reference is not a method reference lambda that can be inspected");
    }

    // ---- Serializable functional interfaces for method refs (instance methods) ----

    @FunctionalInterface public interface MethodRef0<T, R> extends Serializable { R invoke(T target); }
    @FunctionalInterface public interface MethodRef1<T, A1, R> extends Serializable { R invoke(T target, A1 a1); }
    @FunctionalInterface public interface MethodRef2<T, A1, A2, R> extends Serializable { R invoke(T target, A1 a1, A2 a2); }
    @FunctionalInterface public interface MethodRef3<T, A1, A2, A3, R> extends Serializable { R invoke(T target, A1 a1, A2 a2, A3 a3); }
    @FunctionalInterface public interface MethodRef4<T, A1, A2, A3, A4, R> extends Serializable { R invoke(T target, A1 a1, A2 a2, A3 a3, A4 a4); }

    // ---- Default ToolRef ----
    private static final class DefaultToolRef implements ToolRef {
        private final String name;
        private final String provider;
        private final Method method;

        private DefaultToolRef(String name, String provider, Method method) {
            this.name = name;
            this.provider = provider;
            this.method = method;
        }

        @Override public String name() { return name; }
        @Override public String provider() { return provider; }
        @Override public Optional<Method> method() { return Optional.ofNullable(method); }
        @Override public String toString() { return provider + ":" + name; }
    }
}


