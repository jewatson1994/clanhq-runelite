package com.clanhq.verifier.daily.model;

import java.time.Instant;

/** Server-provided presentation and timing metadata for an active task set. */
public final class TaskContext
{
    private final String id;
    private final String type;
    private final String title;
    private final String description;
    private final Instant startsAt;
    private final Instant endsAt;
    private final Instant rotationEndsAt;

    public TaskContext(String id, String type, String title, String description,
        Instant startsAt, Instant endsAt, Instant rotationEndsAt)
    {
        this.id = id == null ? "" : id;
        this.type = type == null ? "" : type;
        this.title = title == null ? "" : title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.rotationEndsAt = rotationEndsAt;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getRotationEndsAt() { return rotationEndsAt; }
}
