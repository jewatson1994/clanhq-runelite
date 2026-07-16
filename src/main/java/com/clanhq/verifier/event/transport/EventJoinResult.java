package com.clanhq.verifier.event.transport;

public final class EventJoinResult
{
    private final boolean joined;
    private final String message;
    private final String teamName;

    public EventJoinResult(boolean joined, String message, String teamName)
    {
        this.joined = joined;
        this.message = message;
        this.teamName = teamName;
    }

    public boolean isJoined()
    {
        return joined;
    }

    public String getMessage()
    {
        return message;
    }

    public String getTeamName()
    {
        return teamName;
    }
}
