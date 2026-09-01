package com.clanhq.verifier.daily.model;

import com.clanhq.verifier.task.VerificationType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DailyTasksSnapshot
{
    private final Instant resetAt;
    private final String periodDate;
    private final List<DailyTaskSummary> tasks;
    private final int balance;
    private final String currencyName;
    private final String currencySymbol;
    private final String serverName;
    private final TaskContext context;

    private DailyTasksSnapshot(String periodDate, Instant resetAt,
        List<DailyTaskSummary> tasks,
        int balance, String currencyName, String currencySymbol,
        String serverName, TaskContext context)
    {
        this.periodDate = periodDate;
        this.resetAt = resetAt;
        this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
        this.balance = balance;
        this.currencyName = currencyName;
        this.currencySymbol = currencySymbol;
        this.serverName = serverName;
        this.context = context;
    }

    public static DailyTasksSnapshot fromJson(String json)
    {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        JsonObject contextValue = object(root, "context");
        String periodDate = root.has("period_date")
            ? root.get("period_date").getAsString() : "";
        Instant resetAt = instant(root, "reset_at");
        if (resetAt == null)
        {
            resetAt = instant(contextValue, "ends_at");
        }
        if (resetAt == null)
        {
            throw new IllegalArgumentException("Tasks response has no end time");
        }
        TaskContext context = new TaskContext(
            string(contextValue, "id", root, "period_date"),
            string(contextValue, "type", null, null),
            string(contextValue, "title", null, null),
            nullableString(contextValue, "description"),
            instant(contextValue, "starts_at"),
            instant(contextValue, "ends_at"),
            instant(contextValue, "rotation_ends_at"));
        JsonArray values = root.getAsJsonArray("tasks");
        List<DailyTaskSummary> tasks = new ArrayList<>();
        for (JsonElement element : values)
        {
            JsonObject value = element.getAsJsonObject();
            JsonObject display = object(value, "display");
            JsonObject progressState = value.has("progress_state")
                ? object(value, "progress_state") : object(value, "progress");
            JsonObject rewardState = value.has("reward_state")
                ? object(value, "reward_state") : object(value, "reward");
            JsonObject state = object(value, "state");
            JsonObject verification = object(value, "verification");
            String name = string(display, "name", value, "name");
            String description = string(display, "description", value, "description");
            int target = integer(progressState, "target", value, "target");
            int progress = integer(progressState, "current", value, "progress");
            int reward = integer(rewardState, "amount", value, "reward");
            boolean claimed = bool(state, "claimed", value, "completed");
            int awarded = integer(value, "awarded", null, null);
            if (state.has("awarded") && !state.get("awarded").isJsonNull())
            {
                awarded = state.get("awarded").getAsInt();
            }
            String verificationName = verification.has("type")
                ? verification.get("type").getAsString() : null;
            Integer verificationItemId = verification.has("item_id")
                && !verification.get("item_id").isJsonNull()
                    ? verification.get("item_id").getAsInt() : null;
            String category = value.has("category")
                ? value.get("category").getAsString()
                : categoryFor(VerificationType.from(verificationName));
            // MINIGAME is a persisted/API compatibility value only.  Never
            // expose it in the current overlay or panel UI.
            if ("MINIGAME".equalsIgnoreCase(category.trim()))
            {
                category = "ACTIVITIES";
            }
            tasks.add(new DailyTaskSummary(
                value.has("id") && !value.get("id").isJsonNull()
                    ? value.get("id").getAsString() : null,
                category,
                name,
                description,
                target,
                progress > 0 ? progress : (claimed ? target : 0),
                reward,
                claimed,
                awarded,
                value.has("placement") && !value.get("placement").isJsonNull()
                    ? value.get("placement").getAsInt() : null,
                VerificationType.from(verificationName), verificationItemId));
        }
        return new DailyTasksSnapshot(
            periodDate,
            resetAt,
            tasks,
            root.has("balance") ? root.get("balance").getAsInt() : 0,
            root.has("currency_name")
                ? root.get("currency_name").getAsString() : "Currency",
            root.has("currency_symbol")
                ? root.get("currency_symbol").getAsString() : "",
            root.has("server_name") ? root.get("server_name").getAsString()
                : "ClanHQ",
            context);
    }

    public Instant getResetAt() { return resetAt; }
    public String getPeriodDate() { return periodDate; }
    public List<DailyTaskSummary> getTasks() { return tasks; }
    public int getBalance() { return balance; }
    public String getCurrencyName() { return currencyName; }
    public String getCurrencySymbol() { return currencySymbol; }
    public String getServerName() { return serverName; }
    public TaskContext getContext() { return context; }

    private static String categoryFor(VerificationType type)
    {
        switch (type)
        {
            case SKILL_XP:
                return "SKILLING";
            case NPC_KILL:
                return "PVM";
            case MINIGAME_SCORE:
            case ITEM_DROP:
            case CLUE_COMPLETE:
            case ACTIVITY_TELEMETRY:
                return "ACTIVITIES";
            default:
                return "UNKNOWN";
        }
    }

    private static JsonObject object(JsonObject parent, String key)
    {
        return parent.has(key) && parent.get(key).isJsonObject()
            ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static Instant instant(JsonObject parent, String key)
    {
        if (parent == null || !parent.has(key) || parent.get(key).isJsonNull())
        {
            return null;
        }
        try
        {
            return Instant.parse(parent.get(key).getAsString());
        }
        catch (RuntimeException exception)
        {
            return null;
        }
    }

    private static String nullableString(JsonObject parent, String key)
    {
        return parent.has(key) && !parent.get(key).isJsonNull()
            ? parent.get(key).getAsString() : null;
    }

    private static String string(JsonObject preferred, String preferredKey,
        JsonObject fallback, String fallbackKey)
    {
        if (preferred.has(preferredKey) && !preferred.get(preferredKey).isJsonNull())
        {
            return preferred.get(preferredKey).getAsString();
        }
        return fallback != null && fallbackKey != null
            && fallback.has(fallbackKey) && !fallback.get(fallbackKey).isJsonNull()
            ? fallback.get(fallbackKey).getAsString() : "";
    }

    private static int integer(JsonObject preferred, String preferredKey,
        JsonObject fallback, String fallbackKey)
    {
        if (preferred.has(preferredKey) && !preferred.get(preferredKey).isJsonNull())
        {
            return preferred.get(preferredKey).getAsInt();
        }
        return fallback != null && fallback.has(fallbackKey)
            && !fallback.get(fallbackKey).isJsonNull()
                ? fallback.get(fallbackKey).getAsInt() : 0;
    }

    private static boolean bool(JsonObject preferred, String preferredKey,
        JsonObject fallback, String fallbackKey)
    {
        if (preferred.has(preferredKey) && !preferred.get(preferredKey).isJsonNull())
        {
            return preferred.get(preferredKey).getAsBoolean();
        }
        return fallback.has(fallbackKey) && !fallback.get(fallbackKey).isJsonNull()
            && fallback.get(fallbackKey).getAsBoolean();
    }
}
