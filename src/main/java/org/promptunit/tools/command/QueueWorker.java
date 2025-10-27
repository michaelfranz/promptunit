package org.promptunit.tools.command;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Optional worker that executes commands from a {@link CommandQueue}.
 * - Concurrency: configurable thread count (default 1)
 * - Affinity: never run two commands with the same affinity concurrently
 * - Preemption: does not abort running commands; requestCancel() is invoked on lower-priority running commands if needed by caller
 * - Errors: exceptions are captured by changing command status to COMPLETED_WITH_ERROR
 * - Completion: upon finish, command is moved to completed list in CommandQueue
 * - Latency: optional delay between scheduling starts to simulate user pacing
 */
public final class QueueWorker {
    private final CommandQueue queue;
    private final int workerThreads;
    private final long interStartLatencyMillis;

    private volatile boolean running = false;
    private ExecutorService executor;
    private final Set<String> activeAffinities = new HashSet<>();

    public QueueWorker(CommandQueue queue, int workerThreads, Duration interStartLatency) {
        this.queue = queue;
        this.workerThreads = Math.max(1, workerThreads);
        this.interStartLatencyMillis = interStartLatency == null ? 0 : Math.max(0, interStartLatency.toMillis());
    }

    public QueueWorker(CommandQueue queue) {
        this(queue, 1, Duration.ZERO);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        executor = Executors.newFixedThreadPool(workerThreads);
        for (int i = 0; i < workerThreads; i++) {
            executor.submit(this::workerLoop);
        }
    }

    public synchronized void stopGracefully() {
        running = false;
        if (executor != null) {
            executor.shutdown();
        }
    }

    public synchronized boolean awaitIdle(long timeout, TimeUnit unit) throws InterruptedException {
        if (executor == null) return true;
        return executor.awaitTermination(timeout, unit);
    }

    private void workerLoop() {
        while (running) {
            try {
                Command next = pollNextEligible();
                if (next == null) {
                    sleepQuietly(10);
                    continue;
                }

                if (interStartLatencyMillis > 0) {
                    sleepQuietly(interStartLatencyMillis);
                }

                runCommand(next);
            } catch (Throwable t) {
                // Keep worker alive
            }
        }
    }

    private Command pollNextEligible() {
        for (Command c : queue.getPendingSnapshot()) {
            Optional<String> k = c.getAffinityKey();
            if (k.isPresent()) {
                synchronized (activeAffinities) {
                    if (activeAffinities.contains(k.get())) {
                        continue;
                    } else {
                        // reserve affinity and move to running list
                        if (queue.startRunning(c.getId())) {
                            activeAffinities.add(k.get());
                            return c;
                        }
                    }
                }
            } else {
                if (queue.startRunning(c.getId())) {
                    return c;
                }
            }
        }
        return null;
    }

    private void runCommand(Command c) {
        try {
            if (c instanceof AbstractCommand ac) ac.markRunning();
            c.execute();
            if (c instanceof AbstractCommand ac) ac.markCompleted();
        } catch (Throwable e) {
            if (c instanceof AbstractCommand ac) ac.markCompletedWithError();
        } finally {
            queue.finishRunning(c.getId());
            // release affinity reservation
            c.getAffinityKey().ifPresent(k -> {
                synchronized (activeAffinities) { activeAffinities.remove(k); }
            });
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}


