package com.clanhq.verifier.daily.transport;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class DailyTasksApiClientTest
{
    @Test
    public void explainsTransportFailuresWithoutExposingExceptionMessages()
    {
        IOException[] failures = {
            new java.net.UnknownHostException("secret-token"),
            new javax.net.ssl.SSLHandshakeException("secret-token"),
            new java.net.ConnectException("secret-token"),
            new java.net.SocketTimeoutException("secret-token"),
            new java.net.SocketException("secret-token"),
            new java.io.EOFException("secret-token"),
            new IOException("secret-token")
        };
        String[] codes = {"DNS_FAILURE", "TLS_FAILURE", "CONNECT_FAILURE",
            "TIMEOUT_OR_INTERRUPTED", "CONNECTION_LOST", "CONNECTION_LOST", "NETWORK_IO"};
        for (int i = 0; i < failures.length; i++)
        {
            String message = DailyTasksApiClient.transportFailure(failures[i], false);
            assertTrue(message.contains(codes[i]));
            assertFalse(message.contains("secret-token"));
        }
        assertTrue(DailyTasksApiClient.transportFailure(failures[0], true).contains("CANCELLED"));
    }
}
