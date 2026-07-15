package com.clanhq.verifier.transport;

import com.clanhq.verifier.model.VerificationSnapshot;

public interface VerificationTransport
{
    VerificationTransportResult submit(VerificationSnapshot snapshot);
}
