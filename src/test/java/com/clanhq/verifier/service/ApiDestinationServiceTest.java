package com.clanhq.verifier.service;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ApiDestinationServiceTest
{
    private final ApiDestinationService service = new ApiDestinationService();

    @Test
    public void acceptsHttpsAndNormalizesTrailingSlashes()
    {
        assertEquals("https://clanhq.example/api/v1",
            service.normalize(" https://clanhq.example/api/v1/// "));
    }

    @Test
    public void allowsLocalDevelopmentHttpOnly()
    {
        assertEquals("http://localhost:8080/api/v1",
            service.normalize("http://localhost:8080/api/v1"));
        assertNull(service.normalize("http://clanhq.example/api/v1"));
    }

    @Test
    public void rejectsCredentialsAndQueryParameters()
    {
        assertNull(service.normalize("https://user:secret@clanhq.example/api"));
        assertNull(service.normalize("https://clanhq.example/api?token=secret"));
    }

    @Test
    public void describesOnlyTheSubmissionHost()
    {
        assertEquals("Submits to: clanhq.example",
            service.describe("https://clanhq.example/private/path"));
        assertEquals("Submits to: localhost:8765",
            service.describe("http://localhost:8765"));
        assertEquals("API: Not configured", service.describe(" "));
        assertEquals("API: Invalid destination",
            service.describe("http://clanhq.example"));
    }
}
