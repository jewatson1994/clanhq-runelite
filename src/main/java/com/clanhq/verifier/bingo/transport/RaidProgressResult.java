package com.clanhq.verifier.bingo.transport;

import com.clanhq.verifier.bingo.model.RaidProgress;
import java.util.Optional;

public final class RaidProgressResult
{
    private final RaidProgress progress;
    private final String message;

    public RaidProgressResult(RaidProgress progress, String message)
    {
        this.progress = progress;
        this.message = message;
    }

    public Optional<RaidProgress> getProgress()
    {
        return Optional.ofNullable(progress);
    }

    public String getMessage()
    {
        return message;
    }
}
