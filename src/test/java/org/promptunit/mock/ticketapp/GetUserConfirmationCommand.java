package org.promptunit.mock.ticketapp;

import org.promptunit.tools.command.AbstractCommand;
import org.promptunit.tools.command.CommandPriority;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a request to get user confirmation before proceeding with an action.
 * Modeled as MEDIUM priority by default.
 */
public final class GetUserConfirmationCommand extends AbstractCommand {
    private final String ticketId;
    private final String prompt;

    public GetUserConfirmationCommand(String ticketId, String prompt) {
        super(CommandPriority.MEDIUM, ticketId);
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
    }

    @Override
    public String getName() {
        return "GetUserConfirmation";
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getPrompt() {
        return prompt;
    }

    @Override
    protected String fingerprintPayload() {
        return ticketId + "|" + prompt;
    }
}
