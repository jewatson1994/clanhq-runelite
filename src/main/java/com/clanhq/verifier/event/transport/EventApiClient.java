package com.clanhq.verifier.event.transport;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.event.model.ClanEventSummary;
import com.clanhq.verifier.service.ApiDestinationService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class EventApiClient
{
    private static final MediaType JSON =
        MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ClanHQVerifierConfig config;
    private final ApiDestinationService destinationService;

    public EventApiClient(OkHttpClient httpClient, ClanHQVerifierConfig config,
        ApiDestinationService destinationService)
    {
        this.httpClient = httpClient;
        this.config = config;
        this.destinationService = destinationService;
    }

    public CompletableFuture<EventLookupResult> fetchCurrentEvent()
    {
        CompletableFuture<EventLookupResult> future = new CompletableFuture<>();
        String baseUrl = destinationService.normalize(config.apiBaseUrl());
        String clanCode = normalized(config.clanCode());
        String eventCode = normalized(config.eventCode());
        if (baseUrl == null || clanCode.isEmpty() || eventCode.isEmpty())
        {
            future.complete(new EventLookupResult(null,
                "Configure the API URL, clan code, and event code."));
            return future;
        }
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/v1/events/current")
            .newBuilder()
            .addQueryParameter("code", eventCode)
            .build();
        Request request = new Request.Builder()
            .url(url)
            .header("X-ClanHQ-Code", clanCode)
            .get()
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new EventLookupResult(null,
                    "ClanHQ could not be reached."));
            }

            @Override
            public void onResponse(Call call, Response response)
                throws IOException
            {
                try (Response closeable = response)
                {
                    String body = response.body() == null
                        ? "" : response.body().string();
                    if (!response.isSuccessful())
                    {
                        future.complete(new EventLookupResult(null,
                            responseMessage(body, response.code())));
                        return;
                    }
                    future.complete(new EventLookupResult(
                        ClanEventSummary.fromJson(body),
                        "Event information loaded."));
                }
                catch (IOException | RuntimeException exception)
                {
                    future.complete(new EventLookupResult(null,
                        "ClanHQ returned invalid event information."));
                }
            }
        });
        return future;
    }

    public CompletableFuture<EventJoinResult> joinEvent(
        ClanEventSummary event,
        String rsn)
    {
        CompletableFuture<EventJoinResult> future = new CompletableFuture<>();
        String baseUrl = destinationService.normalize(config.apiBaseUrl());
        String clanCode = normalized(config.clanCode());
        if (baseUrl == null || clanCode.isEmpty() || normalized(rsn).isEmpty())
        {
            future.complete(new EventJoinResult(false,
                "Log in and configure the ClanHQ connection first.", null));
            return future;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("event_code", event.getEventCode());
        payload.addProperty("rsn", rsn.trim());
        Request request = new Request.Builder()
            .url(baseUrl + "/api/v1/events/join")
            .header("X-ClanHQ-Code", clanCode)
            .post(RequestBody.create(JSON, payload.toString()))
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new EventJoinResult(false,
                    "ClanHQ could not register event participation.", null));
            }

            @Override
            public void onResponse(Call call, Response response)
                throws IOException
            {
                try (Response closeable = response)
                {
                    String body = response.body() == null
                        ? "" : response.body().string();
                    JsonObject value = parseObject(body);
                    String team = value != null && value.has("team")
                        && !value.get("team").isJsonNull()
                            ? value.get("team").getAsString() : null;
                    future.complete(new EventJoinResult(
                        response.isSuccessful(),
                        responseMessage(body, response.code()),
                        team));
                }
                catch (IOException | RuntimeException exception)
                {
                    future.complete(new EventJoinResult(false,
                        "ClanHQ returned an invalid join response.", null));
                }
            }
        });
        return future;
    }

    private static String normalized(String value)
    {
        return value == null ? "" : value.trim();
    }

    private static String responseMessage(String body, int status)
    {
        try
        {
            JsonObject value = new JsonParser().parse(body).getAsJsonObject();
            if (value.has("message"))
            {
                return value.get("message").getAsString();
            }
        }
        catch (RuntimeException ignored)
        {
            // Use the stable HTTP fallback below.
        }
        return "ClanHQ returned HTTP " + status;
    }

    private static JsonObject parseObject(String body)
    {
        try
        {
            return new JsonParser().parse(body).getAsJsonObject();
        }
        catch (RuntimeException exception)
        {
            return null;
        }
    }
}
