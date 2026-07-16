package com.clanhq.verifier.transport;

import com.clanhq.verifier.model.ProgressionEvaluation;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.util.concurrent.CompletableFuture;

public final class PreviewOnlyVerificationTransport
    implements VerificationTransport
{
    @Override
    public CompletableFuture<VerificationTransportResult> submit(
        VerificationSnapshot snapshot,
        ProgressionEvaluation progression)
    {
        return CompletableFuture.completedFuture(
            new VerificationTransportResult(
                false,
                "Preview only — no data was sent."));
    }
}
