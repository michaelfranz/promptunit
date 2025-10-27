package org.promptunit.tools.command;

import org.junit.jupiter.api.Test;
import org.promptunit.mock.ticketapp.AddCommentCommand;
import org.promptunit.mock.ticketapp.CancelCommand;
import org.promptunit.mock.ticketapp.CreateTicketCommand;
import org.promptunit.mock.ticketapp.PauseCommand;
import org.promptunit.mock.ticketapp.UpdateStatusCommand;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandQueueTest {

    @Test
    void singleCommand_isEnqueued() {
        CommandQueue q = new CommandQueue();
        CreateTicketCommand c = new CreateTicketCommand("PRJ", "Summary", "Desc", CommandPriority.MEDIUM);

        EnqueueResult r = q.enqueue(c);

        assertThat(r.commandId()).isEqualTo(c.getId());
        assertThat(r.position()).isZero();
        assertThat(r.deduplicated()).isFalse();
        assertThat(q.sizePending()).isEqualTo(1);
        assertThat(q.getPendingSnapshot().getFirst()).isEqualTo(c);
    }

    @Test
    void priorityInsertion_placesHigherBeforeLower() {
        CommandQueue q = new CommandQueue();
        CreateTicketCommand low = new CreateTicketCommand("PRJ", "Low", "L", CommandPriority.LOW);
        q.enqueue(low);

        UpdateStatusCommand high = new UpdateStatusCommand("T-1", "In Progress", CommandPriority.HIGH);
        EnqueueResult rh = q.enqueue(high);

        assertThat(rh.position()).isZero();
        assertThat(q.getPendingSnapshot().getFirst()).isEqualTo(high);
        assertThat(q.getPendingSnapshot().get(1)).isEqualTo(low);
    }

    @Test
    void highestGoesToHead() {
        CommandQueue q = new CommandQueue();
        q.enqueue(new CreateTicketCommand("PRJ", "A", "x", CommandPriority.MEDIUM));
        q.enqueue(new CreateTicketCommand("PRJ", "B", "y", CommandPriority.HIGH));

        CancelCommand highest = new CancelCommand("T-2", "stop");
        EnqueueResult r = q.enqueue(highest);

        assertThat(r.position()).isZero();
        assertThat(q.getPendingSnapshot().getFirst()).isEqualTo(highest);
    }

    @Test
    void affinityPreemption_differentAffinity_canPreempt() {
        CommandQueue q = new CommandQueue();
        // Pending low for affinity PRJ
        CreateTicketCommand low = new CreateTicketCommand("PRJ", "Low", "L", CommandPriority.LOW);
        q.enqueue(low);

        // High with different affinity T-1 should preempt and go before low
        UpdateStatusCommand highOtherAffinity = new UpdateStatusCommand("T-1", "In Progress", CommandPriority.HIGH);
        EnqueueResult r = q.enqueue(highOtherAffinity);
        assertThat(r.position()).isZero();
        assertThat(q.getPendingSnapshot().getFirst()).isEqualTo(highOtherAffinity);
    }

    @Test
    void affinityPreemption_sameAffinity_notIfLowerIsRunning() {
        CommandQueue q = new CommandQueue();
        // Pending low for affinity PRJ, then mark it running
        CreateTicketCommand low = new CreateTicketCommand("PRJ", "Low", "L", CommandPriority.LOW);
        q.enqueue(low);
        q.startRunning(low.getId());

        // High with same affinity PRJ should not preempt the running low
        CreateTicketCommand highSameAffinity = new CreateTicketCommand("PRJ", "High", "H", CommandPriority.HIGH);
        EnqueueResult r = q.enqueue(highSameAffinity);
        assertThat(r.position()).isEqualTo(0); // still placed at head among pending, but low is running
        assertThat(q.getPendingSnapshot().getFirst()).isEqualTo(highSameAffinity);
        assertThat(q.getRunningSnapshot()).containsExactly(low);
    }

    @Test
    void successiveDedupe_keepsHigherPriority() {
        CommandQueue q = new CommandQueue();
        AddCommentCommand low = new AddCommentCommand("T-1", "same", CommandPriority.LOW);
        q.enqueue(low);

        AddCommentCommand highSame = new AddCommentCommand("T-1", "same", CommandPriority.HIGH);
        EnqueueResult r = q.enqueue(highSame);

        assertThat(r.deduplicated()).isTrue();
        assertThat(q.sizePending()).isEqualTo(1);
        Command only = q.getPendingSnapshot().getFirst();
        assertThat(only.fingerprint()).isEqualTo(highSame.fingerprint());
        assertThat(only.getPriority()).isEqualTo(CommandPriority.HIGH);
    }

    @Test
    void consoleRenderer_outputsReadableState() {
        CommandQueue q = new CommandQueue();
        q.enqueue(new CreateTicketCommand("PRJ", "A", "x", CommandPriority.MEDIUM));
        q.enqueue(new PauseCommand("T-123", "waiting"));

        String out = QueueConsoleRenderer.render(q);
        assertThat(out).contains("Command Queue");
        assertThat(out).contains("Pending (2)");
        assertThat(out).contains("Pause#");
    }
}


