package com.clanhq.verifier.daily.transport;

import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import java.util.Optional;

public final class DailyTasksResult
{
    private final DailyTasksSnapshot snapshot;
    private final String message;

    public DailyTasksResult(DailyTasksSnapshot snapshot, String message)
    {
        this.snapshot = snapshot;
        this.message = message;
    }

    public Optional<DailyTasksSnapshot> getSnapshot()
    {
        return Optional.ofNullable(snapshot);
    }

    public String getMessage() { return message; }
}
