package com.clanhq.verifier.bingo.transport;

import java.util.Objects;

public final class BingoTransportResult
{
    private final boolean successful;
    private final String message;

    public BingoTransportResult(boolean successful, String message)
    {
        this.successful = successful;
        this.message = Objects.requireNonNull(message);
    }

    public boolean isSuccessful()
    {
        return successful;
    }

    public String getMessage()
    {
        return message;
    }
}
