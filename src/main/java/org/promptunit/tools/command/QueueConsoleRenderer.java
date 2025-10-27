package org.promptunit.tools.command;

import java.util.List;

/**
 * Renders queue state to a human-friendly string for debugging and test output.
 */
public final class QueueConsoleRenderer {

    private QueueConsoleRenderer() {}

    public static String render(CommandQueue queue) {
        StringBuilder sb = new StringBuilder();
        sb.append("Command Queue\n");
        sb.append("Pending (" + queue.sizePending() + ")\n");
        List<Command> pending = queue.getPendingSnapshot();
        for (int i = 0; i < pending.size(); i++) {
            Command c = pending.get(i);
            sb.append("  [").append(i).append("] ")
              .append(c.getPriority()).append(" ")
              .append(c.getName()).append("#").append(c.getId());
            c.getAffinityKey().ifPresent(k -> sb.append(" @").append(k));
            sb.append("\n");
        }

        sb.append("Running (" + queue.sizeRunning() + ")\n");
        List<Command> running = queue.getRunningSnapshot();
        for (int i = 0; i < running.size(); i++) {
            Command c = running.get(i);
            sb.append("  [").append(i).append("] ")
              .append(c.getPriority()).append(" ")
              .append(c.getName()).append("#").append(c.getId());
            c.getAffinityKey().ifPresent(k -> sb.append(" @").append(k));
            sb.append("\n");
        }

        sb.append("Completed (" + queue.sizeCompleted() + ")\n");
        List<Command> completed = queue.getCompletedSnapshot();
        for (int i = 0; i < completed.size(); i++) {
            Command c = completed.get(i);
            sb.append("  [").append(i).append("] ")
              .append(c.getPriority()).append(" ")
              .append(c.getName()).append("#").append(c.getId());
            c.getAffinityKey().ifPresent(k -> sb.append(" @").append(k));
            sb.append(" status=").append(c.getStatus());
            sb.append("\n");
        }

        return sb.toString();
    }
}


