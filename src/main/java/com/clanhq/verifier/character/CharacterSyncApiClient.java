package com.clanhq.verifier.character;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.bingo.model.BingoCharacterSubmission;
import com.clanhq.verifier.bingo.transport.BingoCharacterPayloadFactory;
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

public final class CharacterSyncApiClient
{
    private static final MediaType JSON =
        MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ClanHQVerifierConfig config;
    private final ApiDestinationService destinationService;

    public CharacterSyncApiClient(OkHttpClient httpClient,
        ClanHQVerifierConfig config, ApiDestinationService destinationService)
    {
        this.httpClient = httpClient;
        this.config = config;
        this.destinationService = destinationService;
    }

    public CompletableFuture<CharacterSyncResult> submit(
        BingoCharacterSubmission submission)
    {
        CompletableFuture<CharacterSyncResult> future = new CompletableFuture<>();
        String baseUrl = destinationService.normalize(config.apiBaseUrl());
        String token = normalized(config.installationToken());
        if (baseUrl == null || token.isEmpty())
        {
            future.complete(new CharacterSyncResult(false,
                "Pair this RuneLite installation first."));
            return future;
        }
        Request request = new Request.Builder()
            .url(baseUrl + "/api/v1/characters")
            .header("Authorization", "Bearer " + token)
            .post(RequestBody.create(JSON,
                BingoCharacterPayloadFactory.create(submission).toString()))
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new CharacterSyncResult(false,
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
                    future.complete(new CharacterSyncResult(
                        response.isSuccessful(),
                        responseMessage(body, response.code())));
                }
                catch (IOException | RuntimeException exception)
                {
                    future.complete(new CharacterSyncResult(false,
                        "ClanHQ returned an invalid response."));
                }
            }
        });
        return future;
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
        }
        return "ClanHQ returned HTTP " + status;
    }

    private static String normalized(String value)
    {
        return value == null ? "" : value.trim();
    }
}
