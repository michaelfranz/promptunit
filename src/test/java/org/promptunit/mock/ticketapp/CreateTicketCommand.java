package org.promptunit.mock.ticketapp;

import org.promptunit.tools.command.AbstractCommand;
import org.promptunit.tools.command.CommandPriority;

import java.util.Objects;
import java.util.Optional;

public final class CreateTicketCommand extends AbstractCommand {
    private final String projectKey;
    private final String summary;
    private final String description;

    public CreateTicketCommand(String projectKey, String summary, String description, CommandPriority priority) {
        super(priority, Optional.ofNullable(projectKey));
        this.projectKey = Objects.requireNonNull(projectKey, "projectKey");
        this.summary = Objects.requireNonNull(summary, "summary");
        this.description = Objects.requireNonNull(description, "description");
    }

    @Override
    public String getName() {
        return "CreateTicket";
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    @Override
    protected String fingerprintPayload() {
        return projectKey + "|" + summary + "|" + description;
    }
}
