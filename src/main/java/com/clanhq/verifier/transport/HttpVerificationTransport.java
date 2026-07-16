package com.clanhq.verifier.transport;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.model.ProgressionEvaluation;
import com.clanhq.verifier.model.VerificationSnapshot;
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

public final class HttpVerificationTransport implements VerificationTransport
{
    private static final MediaType JSON =
        MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ClanHQVerifierConfig config;
    private final ApiDestinationService destinationService;

    public HttpVerificationTransport(OkHttpClient httpClient,
        ClanHQVerifierConfig config,
        ApiDestinationService destinationService)
    {
        this.httpClient = httpClient;
        this.config = config;
        this.destinationService = destinationService;
    }

    @Override
    public CompletableFuture<VerificationTransportResult> submit(
        VerificationSnapshot snapshot,
        ProgressionEvaluation progression)
    {
        CompletableFuture<VerificationTransportResult> result =
            new CompletableFuture<>();
        String baseUrl = destinationService.normalize(config.apiBaseUrl());
        String clanCode = config.clanCode() == null
            ? "" : config.clanCode().trim();
        if (baseUrl == null || clanCode.isEmpty())
        {
            result.complete(new VerificationTransportResult(false,
                "Configure the ClanHQ API URL and clan code first."));
            return result;
        }

        String json = VerificationPayloadFactory.create(snapshot, progression)
            .toString();
        Request request = new Request.Builder()
            .url(baseUrl + "/api/v1/verifications")
            .header("X-ClanHQ-Code", clanCode)
            .post(RequestBody.create(JSON, json))
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                result.complete(new VerificationTransportResult(false,
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
                    JsonObject parsed = parseObject(body);
                    String message = parsed != null && parsed.has("message")
                        ? parsed.get("message").getAsString()
                        : "ClanHQ returned HTTP " + response.code();
                    if (response.isSuccessful() && parsed != null
                        && parsed.has("ticket_url"))
                    {
                        message += " — "
                            + parsed.get("ticket_url").getAsString();
                    }
                    result.complete(new VerificationTransportResult(
                        response.isSuccessful(), message));
                }
                catch (IOException | RuntimeException exception)
                {
                    result.complete(new VerificationTransportResult(false,
                        "ClanHQ returned an invalid response."));
                }
            }
        });
        return result;
    }

    private static JsonObject parseObject(String json)
    {
        try
        {
            return new JsonParser().parse(json).getAsJsonObject();
        }
        catch (RuntimeException exception)
        {
            return null;
        }
    }
}
