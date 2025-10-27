package org.promptunit.mock.ticketapp;

import org.promptunit.tools.command.AbstractCommand;
import org.promptunit.tools.command.CommandPriority;

import java.util.Objects;
import java.util.Optional;

public final class AssignUserCommand extends AbstractCommand {
    private final String ticketId;
    private final String userId;

    public AssignUserCommand(String ticketId, String userId, CommandPriority priority) {
        super(priority, Optional.ofNullable(ticketId));
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.userId = Objects.requireNonNull(userId, "userId");
    }

    @Override
    public String getName() {
        return "AssignUser";
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    protected String fingerprintPayload() {
        return ticketId + "|" + userId;
    }
}
