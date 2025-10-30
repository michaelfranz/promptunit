package org.promptunit.tools.command.dispatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.promptunit.tools.catalog.ToolInvocation;
import org.promptunit.tools.command.Command;
import org.promptunit.tools.command.EnqueueResult;

public final class DispatchResult {
    public static final class EnqueuedCommand {
        public final ToolInvocation source;
        public final Command command;
        public final EnqueueResult result;

        public EnqueuedCommand(ToolInvocation source, Command command, EnqueueResult result) {
            this.source = source;
            this.command = command;
            this.result = result;
        }
    }

    private final List<EnqueuedCommand> enqueued = new ArrayList<>();

    void add(ToolInvocation src, Command cmd, EnqueueResult res) {
        enqueued.add(new EnqueuedCommand(src, cmd, res));
    }

    public List<EnqueuedCommand> getEnqueued() {
        return Collections.unmodifiableList(enqueued);
    }
}


