package com.clanhq.verifier.event.transport;

public final class EventObservationResult
{
    private final boolean recorded;
    private final String message;

    public EventObservationResult(boolean recorded, String message)
    {
        this.recorded = recorded;
        this.message = message;
    }

    public boolean isRecorded()
    {
        return recorded;
    }

    public String getMessage()
    {
        return message;
    }
}
