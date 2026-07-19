package com.clanhq.verifier.overview;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.service.ApiDestinationService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

public final class IdentityApiClient
{
    private static final MediaType JSON =
        MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ClanHQVerifierConfig config;
    private final ApiDestinationService destinationService;

    public IdentityApiClient(OkHttpClient httpClient,
        ClanHQVerifierConfig config, ApiDestinationService destinationService)
    {
        this.httpClient = httpClient;
        this.config = config;
        this.destinationService = destinationService;
    }

    public CompletableFuture<IdentityResult> fetch()
    {
        CompletableFuture<IdentityResult> future = new CompletableFuture<>();
        String baseUrl = destinationService.normalize(config.apiBaseUrl());
        String token = normalized(config.installationToken());
        if (baseUrl == null || token.isEmpty())
        {
            future.complete(new IdentityResult(null,
                "Pair this installation from Overview."));
            return future;
        }
        Request request = new Request.Builder()
            .url(baseUrl + "/api/v1/identity")
            .header("Authorization", "Bearer " + token)
            .get()
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new IdentityResult(null,
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
                        future.complete(new IdentityResult(null,
                            responseMessage(body, response.code())));
                        return;
                    }
                    future.complete(new IdentityResult(
                        IdentitySnapshot.fromJson(body), "Connected"));
                }
                catch (IOException | RuntimeException exception)
                {
                    future.complete(new IdentityResult(null,
                        "ClanHQ returned invalid identity information."));
                }
            }
        });
        return future;
    }

    public CompletableFuture<PairingResult> pair(
        String code,
        String label,
        String installationToken)
    {
        CompletableFuture<PairingResult> future = new CompletableFuture<>();
        String baseUrl = destinationService.normalize(config.apiBaseUrl());
        if (baseUrl == null || normalized(code).isEmpty())
        {
            future.complete(new PairingResult(false,
                "Configure the ClanHQ API URL and pairing code first.", 0));
            return future;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("pairing_code", code.trim());
        payload.addProperty("installation_label",
            normalized(label).isEmpty() ? "RuneLite" : label.trim());
        payload.addProperty("installation_token", installationToken);
        Request request = new Request.Builder()
            .url(baseUrl + "/api/v1/installations/pair")
            .post(RequestBody.create(JSON, payload.toString()))
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new PairingResult(false,
                    "ClanHQ could not pair this installation.", 0));
            }

            @Override
            public void onResponse(Call call, Response response)
                throws IOException
            {
                try (Response closeable = response)
                {
                    String body = response.body() == null
                        ? "" : response.body().string();
                    JsonObject value = new JsonParser().parse(body)
                        .getAsJsonObject();
                    future.complete(new PairingResult(
                        response.isSuccessful(),
                        value.has("message")
                            ? value.get("message").getAsString()
                            : "ClanHQ returned HTTP " + response.code(),
                        response.isSuccessful() && value.has("reward_amount")
                            ? value.get("reward_amount").getAsInt() : 0));
                }
                catch (IOException | RuntimeException exception)
                {
                    future.complete(new PairingResult(false,
                        "ClanHQ returned an invalid pairing response.", 0));
                }
            }
        });
        return future;
    }

    public CompletableFuture<DisconnectResult> disconnect()
    {
        CompletableFuture<DisconnectResult> future = new CompletableFuture<>();
        String baseUrl = destinationService.normalize(config.apiBaseUrl());
        String token = normalized(config.installationToken());
        if (baseUrl == null || token.isEmpty())
        {
            future.complete(new DisconnectResult(false,
                "No paired ClanHQ installation is configured."));
            return future;
        }
        Request request = new Request.Builder()
            .url(baseUrl + "/api/v1/installations/current")
            .header("Authorization", "Bearer " + token)
            .delete()
            .build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new DisconnectResult(false,
                    "ClanHQ could not revoke this installation."));
            }

            @Override
            public void onResponse(Call call, Response response)
                throws IOException
            {
                try (Response closeable = response)
                {
                    String body = response.body() == null
                        ? "" : response.body().string();
                    boolean disconnected = response.isSuccessful()
                        || response.code() == 401;
                    future.complete(new DisconnectResult(
                        disconnected,
                        response.code() == 401
                            ? "The server no longer recognizes this device; "
                                + "its local pairing was removed."
                            : responseMessage(body, response.code())));
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
