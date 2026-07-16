package com.clanhq.verifier.transport;

import com.clanhq.verifier.model.ProgressionEvaluation;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.util.concurrent.CompletableFuture;

public interface VerificationTransport
{
    CompletableFuture<VerificationTransportResult> submit(
        VerificationSnapshot snapshot,
        ProgressionEvaluation progression);
}
