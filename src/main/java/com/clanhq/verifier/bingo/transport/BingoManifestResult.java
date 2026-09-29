package com.clanhq.verifier.bingo.transport;

import com.clanhq.verifier.bingo.model.BingoManifest;
import java.util.Optional;

public final class BingoManifestResult
{
    private final BingoManifest manifest;
    private final String message;
    private final String siteUrl;

    public BingoManifestResult(BingoManifest manifest, String message)
    {
        this(manifest, message, manifest == null ? null : manifest.getSiteUrl());
    }

    public BingoManifestResult(BingoManifest manifest, String message,
        String siteUrl)
    {
        this.manifest = manifest;
        this.message = message;
        this.siteUrl = siteUrl == null || siteUrl.trim().isEmpty()
            ? null : siteUrl.trim();
    }

    public Optional<BingoManifest> getManifest()
    {
        return Optional.ofNullable(manifest);
    }

    public String getMessage()
    {
        return message;
    }

    public Optional<String> getSiteUrl()
    {
        return Optional.ofNullable(siteUrl);
    }
}
