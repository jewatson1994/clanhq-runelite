package com.clanhq.verifier.service;

import com.clanhq.verifier.model.CollectionLogEvidence;
import com.clanhq.verifier.model.TempleCollectionLogResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class TempleCollectionLogService
{
    private static final String ENDPOINT =
        "https://templeosrs.com/api/collection-log/player_collection_log.php";
    private static final String CATEGORIES = String.join(",",
        "chambers_of_xeric", "theatre_of_blood", "tombs_of_amascut",
        "yama", "doom_of_mokhaiotl");
    private static final Duration MAX_AGE = Duration.ofHours(24);
    private static final Map<String, String> PAGE_TITLES = pageTitles();
    private final OkHttpClient httpClient;

    @Inject
    public TempleCollectionLogService(OkHttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    public CompletableFuture<TempleCollectionLogResult> lookupAsync(String rsn)
    {
        CompletableFuture<TempleCollectionLogResult> result =
            new CompletableFuture<>();
        HttpUrl url = HttpUrl.parse(ENDPOINT).newBuilder()
            .addQueryParameter("player", rsn)
            .addQueryParameter("categories", CATEGORIES)
            .addQueryParameter("includenames", "1")
            .addQueryParameter("includemissingitems", "1")
            .addQueryParameter("dateformat", "unix")
            .build();
        Request request = new Request.Builder().url(url).get().build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                result.complete(TempleCollectionLogResult.unavailable(
                    "TempleOSRS Collection Log was unavailable"));
            }

            @Override
            public void onResponse(Call call, Response response)
                throws IOException
            {
                try (Response closeable = response)
                {
                    if (!response.isSuccessful() || response.body() == null)
                    {
                        result.complete(TempleCollectionLogResult.unavailable(
                            "TempleOSRS returned HTTP " + response.code()));
                        return;
                    }
                    result.complete(parseResponse(response.body().string(),
                        Instant.now()));
                }
                catch (IOException | RuntimeException exception)
                {
                    result.complete(TempleCollectionLogResult.unavailable(
                        "TempleOSRS returned an invalid Collection Log response"));
                }
            }
        });
        return result;
    }

    static TempleCollectionLogResult parseResponse(String json, Instant now)
    {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        JsonObject data = object(root, "data");
        if (data == null)
        {
            return TempleCollectionLogResult.notSynced(
                "No TempleOSRS Collection Log was found; synchronize the "
                    + "TempleOSRS RuneLite plugin and verify again");
        }

        long checkedEpoch = longValue(data, "last_checked");
        if (checkedEpoch <= 0)
        {
            return TempleCollectionLogResult.notSynced(
                "TempleOSRS Collection Log has never been synchronized");
        }
        Instant lastChecked = Instant.ofEpochSecond(checkedEpoch);
        if (lastChecked.isBefore(now.minus(MAX_AGE)))
        {
            return TempleCollectionLogResult.stale(lastChecked);
        }

        JsonObject categories = object(data, "items");
        if (categories == null || !PAGE_TITLES.keySet().stream()
            .allMatch(categories::has))
        {
            return TempleCollectionLogResult.unavailable(
                "TempleOSRS did not return every required Collection Log category");
        }

        CollectionLogEvidence evidence = CollectionLogEvidence.slotCount(
            (int) longValue(data, "total_collections_finished"));
        for (Map.Entry<String, String> page : PAGE_TITLES.entrySet())
        {
            evidence = evidence.merge(parsePage(categories,
                page.getKey(), page.getValue()));
        }
        long ageHours = Math.max(0,
            Duration.between(lastChecked, now).toHours());
        String age = ageHours == 0 ? "less than 1 hour ago"
            : ageHours + (ageHours == 1 ? " hour ago" : " hours ago");
        return TempleCollectionLogResult.fresh(evidence, lastChecked,
            "TempleOSRS Collection Log synced " + age);
    }

    private static CollectionLogEvidence parsePage(JsonObject categories,
        String category, String title)
    {
        JsonArray items = categories.getAsJsonArray(category);
        Map<String, Integer> acquired = new LinkedHashMap<>();
        int acquiredSlots = 0;
        int totalSlots = 0;
        for (JsonElement element : items)
        {
            JsonObject item = element.getAsJsonObject();
            String name = item.get("name").getAsString();
            if (isRaid(category)
                && CollectionLogCaptureService.isExcludedGreenLogSlot(name))
            {
                continue;
            }
            int count = item.get("count").getAsInt();
            totalSlots++;
            if (count > 0)
            {
                acquiredSlots++;
                acquired.put(name, count);
            }
        }
        return CollectionLogEvidence.page(title, acquired, acquiredSlots,
            totalSlots);
    }

    private static boolean isRaid(String category)
    {
        return category.equals("chambers_of_xeric")
            || category.equals("theatre_of_blood")
            || category.equals("tombs_of_amascut");
    }

    private static JsonObject object(JsonObject parent, String name)
    {
        JsonElement value = parent.get(name);
        return value == null || !value.isJsonObject()
            ? null : value.getAsJsonObject();
    }

    private static long longValue(JsonObject parent, String name)
    {
        JsonElement value = parent.get(name);
        return value == null || value.isJsonNull() ? 0 : value.getAsLong();
    }

    private static Map<String, String> pageTitles()
    {
        Map<String, String> titles = new LinkedHashMap<>();
        titles.put("chambers_of_xeric", "Chambers of Xeric");
        titles.put("theatre_of_blood", "Theatre of Blood");
        titles.put("tombs_of_amascut", "Tombs of Amascut");
        titles.put("yama", "Yama");
        titles.put("doom_of_mokhaiotl", "Doom of Mokhaiotl");
        return titles;
    }
}
