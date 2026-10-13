package com.clanhq.verifier.event.transport;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.event.model.ClanEventSummary;
import com.clanhq.verifier.service.ApiDestinationService;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EventApiClientTest
{
    @Test
    public void requestsAndParsesTbdEvents() throws Exception
    {
        OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain ->
        {
            assertEquals("true", chain.request().url().queryParameter("include_tbd"));
            return new Response.Builder().request(chain.request())
                .protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(ResponseBody.create(MediaType.parse("application/json"),
                    "{\"events\":[{\"event_id\":1,\"event_type\":\"BINGO\","
                        + "\"name\":\"Postponed Bingo\",\"start_date\":null,"
                        + "\"end_date\":null,\"status\":\"SCHEDULED\"}]}"))
                .build();
        }).build();
        ClanHQVerifierConfig config = new ClanHQVerifierConfig()
        {
            @Override public String apiBaseUrl() { return "https://example.com"; }
            @Override public String installationToken() { return "test-token"; }
        };
        try
        {
            EventListResult result = new EventApiClient(http, config,
                new ApiDestinationService()).fetchEvents().get(5, TimeUnit.SECONDS);
            assertTrue(result.getSnapshot().isPresent());
            ClanEventSummary event = result.getSnapshot().get().getEvents().get(0);
            assertEquals("Postponed Bingo", event.getName());
            assertNull(event.getStartDate());
            assertNull(event.getEndDate());
            assertNull(event.getStartAt());
            assertNull(event.getEndAt());
            assertFalse(event.isActive());
        }
        finally
        {
            http.dispatcher().executorService().shutdownNow();
            http.connectionPool().evictAll();
        }
    }
}
