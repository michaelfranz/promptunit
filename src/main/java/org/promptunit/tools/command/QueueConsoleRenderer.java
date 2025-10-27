package org.promptunit.tools.command;

import java.util.List;

/**
 * Renders queue state to a human-friendly string for debugging and test output.
 */
public final class QueueConsoleRenderer {

	private QueueConsoleRenderer() {
	}

	public static String render(CommandQueue queue) {
		StringBuilder sb = new StringBuilder();
		sb.append("Command Queue\n").append("Pending (").append(queue.sizePending()).append(")\n");
		renderCommands(sb, queue.getPendingSnapshot());

		sb.append("Running (").append(queue.sizeRunning()).append(")\n");
		renderCommands(sb, queue.getRunningSnapshot());

		sb.append("Completed (").append(queue.sizeCompleted()).append(")\n");
		renderCommands(sb, queue.getCompletedSnapshot(), true);

		return sb.toString();
	}

	private static void renderCommands(StringBuilder sb, List<Command> running) {
		renderCommands(sb, running, false);
	}

	private static void renderCommands(StringBuilder sb, List<Command> running, boolean includeStatus) {
		for (int i = 0; i < running.size(); i++) {
			Command c = running.get(i);
			sb.append("  [")
					.append(i)
					.append("] ")
					.append(c.getPriority())
					.append(" ")
					.append(c.getName())
					.append("#")
					.append(c.getId());
			c.getAffinityKey().ifPresent(k -> sb.append(" @").append(k));
			if (includeStatus) sb.append(" status=").append(c.getStatus());
			sb.append("\n");
		}
	}
}


