package com.clanhq.verifier.daily.transport;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import com.clanhq.verifier.service.ApiDestinationService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

public final class DailyTasksApiClient
{
    private static final MediaType JSON =
        MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ClanHQVerifierConfig config;
    private final ApiDestinationService destinationService;

    public DailyTasksApiClient(OkHttpClient httpClient,
        ClanHQVerifierConfig config,
        ApiDestinationService destinationService)
    {
        this.httpClient = httpClient;
        this.config = config;
        this.destinationService = destinationService;
    }

    public CompletableFuture<DailyTasksResult> fetch()
    {
        CompletableFuture<DailyTasksResult> future = new CompletableFuture<>();
        Request request = authenticatedRequest("/api/v1/daily-tasks");
        if (request == null)
        {
            future.complete(new DailyTasksResult(null,
                "Pair this RuneLite installation to load daily tasks."));
            return future;
        }
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new DailyTasksResult(null,
                    "ClanHQ could not be reached."));
            }

            @Override
            public void onResponse(Call call, Response response)
                throws IOException
            {
                try (Response closeable = response)
                {
                    String body = body(response);
                    if (!response.isSuccessful())
                    {
                        future.complete(new DailyTasksResult(null,
                            responseMessage(body, response.code())));
                        return;
                    }
                    future.complete(new DailyTasksResult(
                        DailyTasksSnapshot.fromJson(body),
                        "Daily tasks loaded."));
                }
                catch (IOException | RuntimeException exception)
                {
                    future.complete(new DailyTasksResult(null,
                        "ClanHQ returned invalid daily tasks."));
                }
            }
        });
        return future;
    }

    public CompletableFuture<DailyActionResult> claim(String category)
    {
        CompletableFuture<DailyActionResult> future = new CompletableFuture<>();
        Request base = authenticatedRequest("/api/v1/daily-tasks/claim");
        if (base == null)
        {
            future.complete(new DailyActionResult(false,
                "Pair this RuneLite installation before claiming.", 0));
            return future;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("category", category);
        Request request = base.newBuilder()
            .post(RequestBody.create(JSON, payload.toString()))
            .build();
        sendAction(request, future, "ClanHQ could not verify daily tasks.",
            null);
        return future;
    }

    private void sendAction(Request request,
        CompletableFuture<DailyActionResult> future,
        String failureMessage,
        String rewardField)
    {
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                future.complete(new DailyActionResult(false, failureMessage, 0));
            }

            @Override
            public void onResponse(Call call, Response response)
                throws IOException
            {
                try (Response closeable = response)
                {
                    String body = body(response);
                    int reward = response.isSuccessful()
                        ? rewardAmount(body, rewardField) : 0;
                    future.complete(new DailyActionResult(
                        response.isSuccessful(),
                        responseMessage(body, response.code()),
                        reward));
                }
                catch (IOException | RuntimeException exception)
                {
                    future.complete(new DailyActionResult(false,
                        "ClanHQ returned an invalid response.", 0));
                }
            }
        });
    }

    private Request authenticatedRequest(String path)
    {
        String baseUrl = destinationService.normalize(config.apiBaseUrl());
        String token = normalized(config.installationToken());
        if (baseUrl == null || token.isEmpty())
        {
            return null;
        }
        return new Request.Builder()
            .url(baseUrl + path)
            .header("Authorization", "Bearer " + token)
            .get()
            .build();
    }

    private static int rewardAmount(String body, String rewardField)
    {
        JsonObject value = parse(body);
        if (rewardField != null)
        {
            return value.has(rewardField) ? value.get(rewardField).getAsInt() : 0;
        }
        int total = 0;
        JsonArray tasks = value.has("tasks")
            ? value.getAsJsonArray("tasks") : new JsonArray();
        for (JsonElement element : tasks)
        {
            total += element.getAsJsonObject().get("awarded").getAsInt();
        }
        if (value.has("completion") && !value.get("completion").isJsonNull())
        {
            total += value.getAsJsonObject("completion")
                .get("awarded").getAsInt();
        }
        return total;
    }

    private static String responseMessage(String body, int status)
    {
        try
        {
            JsonObject value = parse(body);
            if (value.has("message"))
            {
                return value.get("message").getAsString();
            }
        }
        catch (RuntimeException ignored)
        {
            // Fall through to a stable HTTP status message.
        }
        return "ClanHQ returned HTTP " + status;
    }

    private static JsonObject parse(String body)
    {
        return new JsonParser().parse(body).getAsJsonObject();
    }

    private static String body(Response response) throws IOException
    {
        return response.body() == null ? "" : response.body().string();
    }

    private static String normalized(String value)
    {
        return value == null ? "" : value.trim();
    }
}
