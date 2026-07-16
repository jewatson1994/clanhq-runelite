package com.clanhq.verifier.event.transport;

import com.clanhq.verifier.event.model.ClanEventSummary;
import java.util.Optional;

public final class EventLookupResult
{
    private final ClanEventSummary event;
    private final String message;

    public EventLookupResult(ClanEventSummary event, String message)
    {
        this.event = event;
        this.message = message;
    }

    public Optional<ClanEventSummary> getEvent()
    {
        return Optional.ofNullable(event);
    }

    public String getMessage()
    {
        return message;
    }
}
