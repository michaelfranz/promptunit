package org.promptunit.tools.command;

import java.util.Optional;

/**
 * Core command abstraction for the in-memory command framework.
 * Commands are immutable descriptions of work and are never executed in this framework.
 * They carry enough state for test assertions and queue behaviors (priority, affinity, dedupe).
 */
public interface Command {

    /**
     * A stable identifier for this command instance. Implementations may generate a UUID or a natural key.
     */
    String getId();

    /**
     * Human-friendly name for diagnostics (e.g., "CreateTicket").
     */
    String getName();

    /**
     * Priority controls ordering and preemption.
     */
    CommandPriority getPriority();

    /**
     * Optional affinity key. Commands with the same key serialize execution in a real system.
     */
    Optional<String> getAffinityKey();

    /**
     * A deterministic fingerprint of the command's semantic content for deduplication.
     * Two commands with identical fingerprints are considered identical for dedupe checks.
     */
    String fingerprint();

    /**
     * Optional execution hook to make the framework plausible. Default is a no-op.
     */
    default void execute() {
        // no-op by default
    }

    /**
     * Status inspection for worker-driven flows.
     */
    default CommandStatus getStatus() {
        return CommandStatus.PENDING;
    }

    /**
     * Request cancellation of a running command. Implementations should honor this cooperatively.
     */
    default void requestCancel() {
        // no-op by default
    }
}


