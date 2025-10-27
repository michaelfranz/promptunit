package org.promptunit.mock.ticketapp;

import org.promptunit.tools.command.AbstractCommand;
import org.promptunit.tools.command.CommandPriority;

import java.util.Objects;
import java.util.Optional;

public final class LinkTicketsCommand extends AbstractCommand {
    private final String inwardTicketId;
    private final String outwardTicketId;
    private final String linkType;

    public LinkTicketsCommand(String inwardTicketId, String outwardTicketId, String linkType, CommandPriority priority) {
        super(priority, "link:" + Objects.requireNonNull(inwardTicketId) + ":" + Objects.requireNonNull(outwardTicketId));
        this.inwardTicketId = inwardTicketId;
        this.outwardTicketId = Objects.requireNonNull(outwardTicketId, "outwardTicketId");
        this.linkType = Objects.requireNonNull(linkType, "linkType");
    }

    @Override
    public String getName() {
        return "LinkTickets";
    }

    public String getInwardTicketId() {
        return inwardTicketId;
    }

    public String getOutwardTicketId() {
        return outwardTicketId;
    }

    public String getLinkType() {
        return linkType;
    }

    @Override
    protected String fingerprintPayload() {
        return inwardTicketId + "|" + outwardTicketId + "|" + linkType;
    }
}
