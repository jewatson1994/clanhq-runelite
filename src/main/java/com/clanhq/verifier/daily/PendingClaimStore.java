package com.clanhq.verifier.daily;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.runelite.client.config.ConfigManager;

/** Locally durable, installation-scoped claims created only by a Claim click. */
final class PendingClaimStore
{
    private static final String KEY = "dailyTasksPendingClaims";
    private final Consumer<String> save;
    private final List<JsonObject> claims = new ArrayList<>();

    PendingClaimStore(ConfigManager config)
    {
        this(() -> config.getConfiguration(ClanHQVerifierConfig.GROUP, KEY),
            value -> config.setConfiguration(ClanHQVerifierConfig.GROUP, KEY, value));
    }

    PendingClaimStore(Supplier<String> load, Consumer<String> save)
    {
        this.save = save;
        String raw = load.get();
        if (raw == null || raw.trim().isEmpty()) { return; }
        try
        {
            for (JsonElement value : new JsonParser().parse(raw).getAsJsonArray())
            {
                JsonObject claim = value.getAsJsonObject();
                if (claim.has("claim_id") && claim.has("scope")
                    && claim.has("period_date") && claim.has("reset_at")
                    && claim.has("category") && claim.has("client_progress"))
                {
                    claims.add(claim);
                }
            }
        }
        catch (RuntimeException error)
        {
            // Preserve malformed data for support rather than silently deleting it.
            claims.clear();
        }
    }

    synchronized JsonObject enqueue(String scope, String periodDate,
        String resetAt, String clickedAt, String contextId, String category, String taskId,
        JsonArray progress)
    {
        JsonObject existing = find(scope, periodDate, resetAt, contextId, category);
        if (existing != null) { return existing.deepCopy(); }
        JsonObject claim = new JsonObject();
        claim.addProperty("claim_id", UUID.randomUUID().toString());
        claim.addProperty("scope", scope);
        claim.addProperty("period_date", periodDate);
        claim.addProperty("reset_at", resetAt);
        claim.addProperty("clicked_at", clickedAt);
        claim.addProperty("context_id", contextId);
        claim.addProperty("category", category);
        claim.addProperty("task_id", taskId);
        claim.add("client_progress", progress.deepCopy());
        claims.add(claim);
        persist();
        return claim.deepCopy();
    }

    synchronized JsonObject find(String scope, String periodDate,
        String resetAt, String contextId, String category)
    {
        for (JsonObject claim : claims)
        {
            if (scope.equals(string(claim, "scope"))
                && periodDate.equals(string(claim, "period_date"))
                && resetAt.equals(string(claim, "reset_at"))
                && contextId.equals(string(claim, "context_id"))
                && category.equals(string(claim, "category")))
            {
                return claim.deepCopy();
            }
        }
        return null;
    }

    synchronized List<JsonObject> forScope(String scope)
    {
        List<JsonObject> result = new ArrayList<>();
        for (JsonObject claim : claims)
        {
            if (scope.equals(string(claim, "scope")))
            {
                result.add(claim.deepCopy());
            }
        }
        return result;
    }

    synchronized void acknowledge(String claimId)
    {
        if (claims.removeIf(claim -> claimId.equals(string(claim, "claim_id"))))
        {
            persist();
        }
    }

    private void persist()
    {
        JsonArray values = new JsonArray();
        claims.forEach(values::add);
        save.accept(values.toString());
    }

    static String string(JsonObject value, String key)
    {
        return value.has(key) && !value.get(key).isJsonNull()
            ? value.get(key).getAsString() : "";
    }
}
