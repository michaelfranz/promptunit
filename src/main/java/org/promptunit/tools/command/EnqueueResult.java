package org.promptunit.tools.command;

/**
 * Result of enqueuing a command, for diagnostics and assertions.
 */
public record EnqueueResult(String commandId, int position, boolean deduplicated) {}


