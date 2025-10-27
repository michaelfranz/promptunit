package org.promptunit.tools.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An in-memory, non-executing command queue with:
 * - Priority ordering with HIGHEST overriding placement
 * - Affinity-aware preemption semantics for insertion
 * - Successive deduplication by fingerprint, keeping the higher-priority command
 * The queue never runs commands; it exists purely for inspection and assertions.
 */
public final class CommandQueue {

    private final List<Command> pending = new ArrayList<>();
    private final List<Command> running = new ArrayList<>();
    private final List<Command> completed = new ArrayList<>();

    /**
     * Enqueue with rules:
     * - If the new command has priority HIGHEST, insert at head of the pending queue.
     * - Otherwise insert according to priority ordering (HIGHEST > HIGH > MEDIUM > LOW).
     * - Preemption on insert: higher priority may be placed ahead of lower priority commands if:
     *   a) Different affinity key; or
     *   b) Same affinity key and the lower-priority command is not currently running.
     * - Successive dedupe: if the immediately previous enqueued pending command is fingerprint-identical,
     *   retain the higher-priority one and drop the lower-priority one.
     */
    public synchronized EnqueueResult enqueue(Command command) {
        Objects.requireNonNull(command, "command");

        boolean deduplicated = applySuccessiveDedupe(command);
        if (deduplicated) {
            // After dedupe, the command didn't get inserted; report position as -1.
            return new EnqueueResult(command.getId(), -1, true);
        }

        int position = insertWithPriorityAndAffinity(command);
        return new EnqueueResult(command.getId(), position, false);
    }

    public synchronized List<Command> getPendingSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(pending));
    }

    public synchronized List<Command> getRunningSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(running));
    }

    public synchronized List<Command> getCompletedSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(completed));
    }

    public synchronized int sizePending() {
        return pending.size();
    }

    public synchronized int sizeRunning() {
        return running.size();
    }

    public synchronized int sizeCompleted() {
        return completed.size();
    }

    /**
     * Mark a command as running (moves first matching from pending to running) – useful for tests
     * that need to model preemption rules relative to running commands.
     */
    public synchronized boolean startRunning(String commandId) {
        for (int i = 0; i < pending.size(); i++) {
            if (pending.get(i).getId().equals(commandId)) {
                Command c = pending.remove(i);
                running.add(c);
                return true;
            }
        }
        return false;
    }

    /**
     * Mark a command as no longer running and remove it from the running list.
     */
    public synchronized boolean finishRunning(String commandId) {
        for (int i = 0; i < running.size(); i++) {
            Command c = running.get(i);
            if (c.getId().equals(commandId)) {
                running.remove(i);
                completed.add(c);
                return true;
            }
        }
        return false;
    }

    private boolean applySuccessiveDedupe(Command incoming) {
        if (pending.isEmpty()) {
            return false;
        }
        Command last = pending.get(pending.size() - 1);
        if (!last.fingerprint().equals(incoming.fingerprint())) {
            // Not identical by fingerprint, nothing to dedupe.
            return false;
        }

        // If identical fingerprints, keep the higher priority command.
        // Replace the last one if incoming is higher priority; otherwise drop incoming.
        if (priorityComparator().compare(incoming.getPriority(), last.getPriority()) < 0) {
            // Incoming has higher priority (comparator orders HIGHEST first)
            pending.set(pending.size() - 1, incoming);
        }
        // If priorities are equal or incoming lower, drop incoming.
        return true;
    }

    private int insertWithPriorityAndAffinity(Command incoming) {
        if (incoming.getPriority() == CommandPriority.HIGHEST) {
            pending.add(0, incoming);
            return 0;
        }

        // Determine target position based on priority and preemption rules.
        int insertIndex = pending.size();
        Optional<String> incomingAffinity = incoming.getAffinityKey();
        for (int i = 0; i < pending.size(); i++) {
            Command existing = pending.get(i);
            int cmp = priorityComparator().compare(incoming.getPriority(), existing.getPriority());
            if (cmp < 0) {
                // incoming higher priority than existing
                boolean differentAffinity = !incomingAffinity.isPresent() || !existing.getAffinityKey().isPresent()
                        || !incomingAffinity.get().equals(existing.getAffinityKey().get());
                boolean sameAffinityAndExistingNotRunning = incomingAffinity.isPresent()
                        && existing.getAffinityKey().isPresent()
                        && incomingAffinity.get().equals(existing.getAffinityKey().get())
                        && running.stream().noneMatch(r -> r.getId().equals(existing.getId()));

                if (differentAffinity || sameAffinityAndExistingNotRunning) {
                    insertIndex = i;
                    break;
                }
            } else if (cmp == 0) {
                // same priority – stable ordering, insert after same-priority block
                continue;
            } else {
                // incoming lower priority, keep scanning to insert later
            }
        }

        pending.add(insertIndex, incoming);
        return insertIndex;
    }

    private static Comparator<CommandPriority> priorityComparator() {
        // Order: HIGHEST < HIGH < MEDIUM < LOW (so lower is "greater priority")
        return Comparator.comparingInt(p -> switch (p) {
            case HIGHEST -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
            default -> 4;
        });
    }
}


