package org.promptunit;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import org.promptunit.core.PromptInstance;
import org.promptunit.core.PromptResult;

public interface LLMEngine {
    default PromptResult execute(PromptInstance instance) {
        return execute(instance, Long.MAX_VALUE);
    }

    default PromptResult execute(PromptInstance instance, long timeoutMs) {
        return invokeOnce(instance, timeoutMs);
    }

    default Set<PromptResult> execute(PromptInstance instance, long timeoutMs, int nTimes) {
        if (nTimes <= 0) throw new IllegalArgumentException("nTimes must be greater than 0");

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Callable<PromptResult> task = () -> invokeOnce(instance, timeoutMs);
        Set<Future<PromptResult>> allResponseFutures = new HashSet<>();
        IntStream.range(0, nTimes).forEach(i -> allResponseFutures.add(executor.submit(task)));

        long deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        Set<PromptResult> results = new HashSet<>();
        try {
            for (Future<PromptResult> future : allResponseFutures) {
                long remainingNs = deadlineNs - System.nanoTime();
                if (remainingNs <= 0) {
                    throw new LLMTimeoutException("LLM invocation timed out after " + timeoutMs + "ms");
                }
                PromptResult r = future.get(remainingNs, TimeUnit.NANOSECONDS);
                results.add(r);
            }
            return results;
        } catch (TimeoutException e) {
            throw new LLMTimeoutException("LLM invocation timed out after " + timeoutMs + "ms", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof LLMTimeoutException) throw (LLMTimeoutException) cause;
            if (cause instanceof LLMInvocationException) throw (LLMInvocationException) cause;
            throw new LLMInvocationException("Error invoking LLM: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMTimeoutException("LLM invocation interrupted after " + timeoutMs + "ms", e);
        } finally {
            for (Future<PromptResult> f : allResponseFutures) {
                f.cancel(true);
            }
            executor.shutdownNow();
        }
    }

    PromptResult invokeOnce(PromptInstance instance, long timeoutMs);

    String provider();

    String model();

}


