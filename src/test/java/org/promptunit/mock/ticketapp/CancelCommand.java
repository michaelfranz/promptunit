package org.promptunit.mock.ticketapp;

import org.promptunit.tools.command.AbstractCommand;
import org.promptunit.tools.command.CommandPriority;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a request to cancel a ticket operation (HIGHEST priority).
 */
public final class CancelCommand extends AbstractCommand {
    private final String ticketId;
    private final String reason;

    public CancelCommand(String ticketId, String reason) {
        super(CommandPriority.HIGHEST, Optional.ofNullable(ticketId));
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    @Override
    public String getName() {
        return "Cancel";
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
