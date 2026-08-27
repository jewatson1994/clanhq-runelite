package com.clanhq.verifier.event.transport;

import com.clanhq.verifier.event.model.ClanEventsSnapshot;
import java.util.Optional;

public final class EventListResult
{
    private final ClanEventsSnapshot snapshot;
    private final String message;

    public EventListResult(ClanEventsSnapshot snapshot, String message)
    {
        this.snapshot = snapshot;
        this.message = message;
    }

    public Optional<ClanEventsSnapshot> getSnapshot()
    {
        return Optional.ofNullable(snapshot);
    }

    public String getMessage()
    {
        return message;
    }
}
