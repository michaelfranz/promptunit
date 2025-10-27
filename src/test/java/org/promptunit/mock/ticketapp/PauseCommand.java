package org.promptunit.mock.ticketapp;

import org.promptunit.tools.command.AbstractCommand;
import org.promptunit.tools.command.CommandPriority;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a request to pause processing for a ticket (HIGH priority).
 */
public final class PauseCommand extends AbstractCommand {
    private final String ticketId;
    private final String reason;

    public PauseCommand(String ticketId, String reason) {
        super(CommandPriority.HIGH, Optional.ofNullable(ticketId));
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    @Override
    public String getName() {
        return "Pause";
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    protected String fingerprintPayload() {
        return ticketId + "|" + reason;
    }
}
