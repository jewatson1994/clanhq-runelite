package com.clanhq.verifier.event.transport;

public final class EventJoinResult
{
    private final boolean joined;
    private final String message;

    public EventJoinResult(boolean joined, String message)
    {
        this.joined = joined;
        this.message = message;
    }

    public boolean isJoined()
    {
        return joined;
    }

    public String getMessage()
    {
        return message;
    }
}
