package com.clanhq.verifier.bingo.transport;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.bingo.model.BingoDrop;
import com.clanhq.verifier.bingo.model.BingoManifest;
import com.clanhq.verifier.service.ApiDestinationService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class BingoApiClient
{
    private static final MediaType JSON =
        MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ClanHQVerifierConfig config;
    private final ApiDestinationService destinationService;

    public BingoApiClient(OkHttpClient httpClient,
        ClanHQVerifierConfig config,
        ApiDestinationService destinationService)
    {
        this.httpClient = httpClient;
        this.config = config;
        this.destinationService = destinationService;
    }

    public CompletableFuture<BingoManifestResult> fetchManifest()
    {
        CompletableFuture<BingoManifestResult> future =
            new CompletableFuture<>();
        String baseUrl = configuredBaseUrl();
        if (baseUrl == null)
        {
            future.complete(new BingoManifestResult(null,
                "Configure the ClanHQ API URL and clan code first."));
            return future;
        }
        Request request = request(baseUrl + "/api/v1/bingo/manifest")
            .get()
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new BingoManifestResult(null,
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
                        future.complete(new BingoManifestResult(null,
                            responseMessage(body, response.code())));
                        return;
                    }
                    future.complete(new BingoManifestResult(
                        BingoManifest.fromJson(body),
                        "Bingo board loaded."));
                }
                catch (IOException | RuntimeException exception)
                {
                    future.complete(new BingoManifestResult(null,
                        "ClanHQ returned an invalid Bingo board."));
                }
            }
        });
        return future;
    }

    public CompletableFuture<BingoTransportResult> submit(BingoDrop drop)
    {
        CompletableFuture<BingoTransportResult> future =
            new CompletableFuture<>();
        String baseUrl = configuredBaseUrl();
        if (baseUrl == null)
        {
            future.complete(new BingoTransportResult(false,
                "Configure the ClanHQ API URL and clan code first."));
            return future;
        }
        Request request = request(baseUrl + "/api/v1/bingo/drops")
            .post(RequestBody.create(JSON, payload(drop).toString()))
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new BingoTransportResult(false,
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
                    future.complete(new BingoTransportResult(
                        response.isSuccessful(),
                        responseMessage(body, response.code())));
                }
                catch (IOException | RuntimeException exception)
                {
                    future.complete(new BingoTransportResult(false,
                        "ClanHQ returned an invalid response."));
                }
            }
        });
        return future;
    }

    private Request.Builder request(String url)
    {
        return new Request.Builder()
            .url(url)
            .header("X-ClanHQ-Code", config.clanCode().trim());
    }

    private String configuredBaseUrl()
    {
        String baseUrl = destinationService.normalize(config.apiBaseUrl());
        String code = config.clanCode() == null ? "" : config.clanCode().trim();
        return baseUrl == null || code.isEmpty() ? null : baseUrl;
    }

    static JsonObject payload(BingoDrop drop)
    {
        JsonObject value = new JsonObject();
        value.addProperty("schema_version", 1);
        value.addProperty("submission_id", drop.getSubmissionId());
        value.addProperty("event_id", drop.getEventId());
        value.addProperty("rsn", drop.getRsn());
        value.addProperty("item_id", drop.getItem().getItemId());
        value.addProperty("item_name", drop.getItem().getName());
        value.addProperty("quantity", drop.getQuantity());
        value.addProperty("source_type", drop.getSourceType());
        value.addProperty("source_name", drop.getSourceName());
        value.addProperty("occurred_at", drop.getOccurredAt().toString());
        return value;
    }

    private static String responseMessage(String body, int status)
    {
        try
        {
            JsonObject parsed = new JsonParser().parse(body).getAsJsonObject();
            if (parsed.has("message"))
            {
                return parsed.get("message").getAsString();
            }
        }
        catch (RuntimeException ignored)
        {
            // Fall through to a stable HTTP error.
        }
        return "ClanHQ returned HTTP " + status;
    }
}
