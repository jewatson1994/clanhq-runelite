package com.clanhq.verifier.character;

public final class CharacterSyncResult
{
    private final boolean successful;
    private final String message;

    public CharacterSyncResult(boolean successful, String message)
    {
        this.successful = successful;
        this.message = message;
    }

    public boolean isSuccessful() { return successful; }
    public String getMessage() { return message; }
}
