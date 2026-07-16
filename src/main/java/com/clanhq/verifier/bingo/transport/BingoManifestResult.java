package com.clanhq.verifier.bingo.transport;

import com.clanhq.verifier.bingo.model.BingoManifest;
import java.util.Optional;

public final class BingoManifestResult
{
    private final BingoManifest manifest;
    private final String message;

    public BingoManifestResult(BingoManifest manifest, String message)
    {
        this.manifest = manifest;
        this.message = message;
    }

    public Optional<BingoManifest> getManifest()
    {
        return Optional.ofNullable(manifest);
    }

    public String getMessage()
    {
        return message;
    }
}
