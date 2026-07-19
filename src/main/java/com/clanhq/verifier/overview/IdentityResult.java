package com.clanhq.verifier.overview;

import java.util.Optional;

public final class IdentityResult
{
    private final IdentitySnapshot identity;
    private final String message;

    public IdentityResult(IdentitySnapshot identity, String message)
    {
        this.identity = identity;
        this.message = message;
    }

    public Optional<IdentitySnapshot> getIdentity()
    {
        return Optional.ofNullable(identity);
    }

    public String getMessage() { return message; }
}
