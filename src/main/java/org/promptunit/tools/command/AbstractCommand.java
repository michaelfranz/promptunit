package org.promptunit.tools.command;

import static java.util.Optional.ofNullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Base class for commands, implementing common non-domain specifics like id, priority, affinity, and fingerprinting.
 */
public abstract class AbstractCommand implements Command {
    private final String id;
    private final CommandPriority priority;
    private final String affinityKey;
    private volatile CommandStatus status = CommandStatus.PENDING;
    private volatile boolean cancelRequested = false;

    protected AbstractCommand(CommandPriority priority, String affinityKey) {
        this(UUID.randomUUID().toString(), priority, affinityKey);
    }

    protected AbstractCommand(String id, CommandPriority priority, String affinityKey) {
        this.id = Objects.requireNonNull(id, "id");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.affinityKey = affinityKey;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public CommandPriority getPriority() {
        return priority;
    }

    @Override
    public Optional<String> getAffinityKey() {
        return ofNullable(affinityKey);
    }

    @Override
    public String fingerprint() {
        // Priority is intentionally excluded from the fingerprint so that two commands
        // with identical semantic content but different priorities are considered duplicates
        // for the successive dedupe rule (the higher priority wins).
        String content = getName() + "|" + ofNullable(affinityKey).orElse("") + "|" + fingerprintPayload();
        return sha256(content);
    }

    protected abstract String fingerprintPayload();

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // Status/cancellation helpers for worker integration
    public void markRunning() {
        this.status = CommandStatus.RUNNING;
    }

    public void markCompleted() {
        this.status = CommandStatus.COMPLETED;
    }

    public void markCompletedWithError() {
        this.status = CommandStatus.COMPLETED_WITH_ERROR;
    }

    public void markCancelled() {
        this.status = CommandStatus.CANCELLED;
    }

    @Override
    public CommandStatus getStatus() {
        return status;
    }

    @Override
    public void requestCancel() {
        this.cancelRequested = true;
    }

    protected boolean isCancelRequested() {
        return cancelRequested;
    }
}


