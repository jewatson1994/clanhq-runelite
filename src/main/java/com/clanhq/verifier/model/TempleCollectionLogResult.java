package com.clanhq.verifier.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class TempleCollectionLogResult
{
    public enum Status
    {
        FRESH,
        NOT_SYNCED,
        UNAVAILABLE
    }

    private final Status status;
    private final CollectionLogEvidence evidence;
    private final Instant lastChecked;
    private final String message;

    private TempleCollectionLogResult(Status status,
        CollectionLogEvidence evidence, Instant lastChecked, String message)
    {
        this.status = Objects.requireNonNull(status);
        this.evidence = Objects.requireNonNull(evidence);
        this.lastChecked = lastChecked;
        this.message = Objects.requireNonNull(message);
    }

    public static TempleCollectionLogResult fresh(
        CollectionLogEvidence evidence, Instant lastChecked, String message)
    {
        return new TempleCollectionLogResult(Status.FRESH, evidence,
            lastChecked, message);
    }

    public static TempleCollectionLogResult notSynced(String message)
    {
        return new TempleCollectionLogResult(Status.NOT_SYNCED,
            CollectionLogEvidence.empty(), null, message);
    }

    public static TempleCollectionLogResult unavailable(String message)
    {
        return new TempleCollectionLogResult(Status.UNAVAILABLE,
            CollectionLogEvidence.empty(), null, message);
    }

    public Status getStatus() { return status; }
    public CollectionLogEvidence getEvidence() { return evidence; }
    public Optional<Instant> getLastChecked()
    {
        return Optional.ofNullable(lastChecked);
    }
    public String getMessage() { return message; }
    public boolean isFresh() { return status == Status.FRESH; }
}
