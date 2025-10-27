package org.promptunit.mock.ticketapp;

import org.promptunit.tools.command.AbstractCommand;
import org.promptunit.tools.command.CommandPriority;

import java.util.Objects;
import java.util.Optional;

public final class AddCommentCommand extends AbstractCommand {
    private final String ticketId;
    private final String comment;

    public AddCommentCommand(String ticketId, String comment, CommandPriority priority) {
        super(priority, ticketId);
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.comment = Objects.requireNonNull(comment, "comment");
    }

    @Override
    public String getName() {
        return "AddComment";
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getComment() {
        return comment;
    }

    @Override
    protected String fingerprintPayload() {
        return ticketId + "|" + comment;
    }
}
