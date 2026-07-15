package com.clanhq.verifier.transport;

import java.util.Objects;

public final class VerificationTransportResult
{
    private final boolean submitted;
    private final String message;

    public VerificationTransportResult(boolean submitted, String message)
    {
        this.submitted = submitted;
        this.message = Objects.requireNonNull(message);
    }

    public boolean isSubmitted()
    {
        return submitted;
    }

    public String getMessage()
    {
        return message;
    }
}
