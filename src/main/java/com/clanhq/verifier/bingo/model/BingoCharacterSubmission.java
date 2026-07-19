package com.clanhq.verifier.bingo.model;

import com.clanhq.verifier.model.VerificationSnapshot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class BingoCharacterSubmission
{
    private final String submissionId;
    private final String eventId;
    private final Instant capturedAt;
    private final VerificationSnapshot snapshot;

    public BingoCharacterSubmission(String eventId,
        VerificationSnapshot snapshot)
    {
        this(UUID.randomUUID().toString(), eventId, Instant.now(), snapshot);
    }

    BingoCharacterSubmission(String submissionId, String eventId,
        Instant capturedAt, VerificationSnapshot snapshot)
    {
        this.submissionId = Objects.requireNonNull(submissionId);
        this.eventId = Objects.requireNonNull(eventId);
        this.capturedAt = Objects.requireNonNull(capturedAt);
        this.snapshot = Objects.requireNonNull(snapshot);
    }

    public String getSubmissionId() { return submissionId; }
    public String getEventId() { return eventId; }
    public Instant getCapturedAt() { return capturedAt; }
    public VerificationSnapshot getSnapshot() { return snapshot; }
}
