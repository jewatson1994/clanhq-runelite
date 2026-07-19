package com.clanhq.verifier.overview;

public final class DisconnectResult
{
    private final boolean successful;
    private final String message;

    DisconnectResult(boolean successful, String message)
    {
        this.successful = successful;
        this.message = message;
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
