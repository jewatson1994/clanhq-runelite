package com.clanhq.verifier.bingo.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class BingoDrop
{
    private final String submissionId;
    private final String eventId;
    private final String rsn;
    private final BingoItem item;
    private final int quantity;
    private final String sourceType;
    private final String sourceName;
    private final Instant occurredAt;

    public BingoDrop(String eventId, String rsn, BingoItem item, int quantity,
        String sourceType, String sourceName, Instant occurredAt)
    {
        this(UUID.randomUUID().toString(), eventId, rsn, item, quantity,
            sourceType, sourceName, occurredAt);
    }

    BingoDrop(String submissionId, String eventId, String rsn, BingoItem item,
        int quantity, String sourceType, String sourceName, Instant occurredAt)
    {
        this.submissionId = Objects.requireNonNull(submissionId);
        this.eventId = Objects.requireNonNull(eventId);
        this.rsn = Objects.requireNonNull(rsn);
        this.item = Objects.requireNonNull(item);
        this.quantity = quantity;
        this.sourceType = Objects.requireNonNull(sourceType);
        this.sourceName = Objects.requireNonNull(sourceName);
        this.occurredAt = Objects.requireNonNull(occurredAt);
    }

    public String getSubmissionId() { return submissionId; }
    public String getEventId() { return eventId; }
    public String getRsn() { return rsn; }
    public BingoItem getItem() { return item; }
    public int getQuantity() { return quantity; }
    public String getSourceType() { return sourceType; }
    public String getSourceName() { return sourceName; }
    public Instant getOccurredAt() { return occurredAt; }
}
