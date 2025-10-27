package org.promptunit.mock.ticketapp;

import org.promptunit.tools.command.AbstractCommand;
import org.promptunit.tools.command.CommandPriority;

import java.util.Objects;
import java.util.Optional;

public final class UpdateStatusCommand extends AbstractCommand {
    private final String ticketId;
    private final String status;

    public UpdateStatusCommand(String ticketId, String status, CommandPriority priority) {
        super(priority, ticketId);
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.status = Objects.requireNonNull(status, "status");
    }

    @Override
    public String getName() {
        return "UpdateStatus";
    }

    public String getTicketId() {
        return ticketId;
    }

    @Override
    protected String fingerprintPayload() {
        return ticketId + "|" + status;
    }
}
