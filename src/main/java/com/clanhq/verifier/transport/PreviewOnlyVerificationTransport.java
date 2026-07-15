package com.clanhq.verifier.transport;

import com.clanhq.verifier.model.VerificationSnapshot;

public final class PreviewOnlyVerificationTransport
    implements VerificationTransport
{
    @Override
    public VerificationTransportResult submit(VerificationSnapshot snapshot)
    {
        return new VerificationTransportResult(
            false,
            "Preview only — no data was sent.");
    }
}
