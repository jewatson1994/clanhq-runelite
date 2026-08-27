package com.clanhq.verifier.daily.model;

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

    private DailyTasksSnapshot(String periodDate, Instant resetAt,
        List<DailyTaskSummary> tasks,
        int balance, String currencyName, String currencySymbol,
        String serverName)
    {
        this.periodDate = periodDate;
        this.resetAt = resetAt;
        this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
        this.balance = balance;
        this.currencyName = currencyName;
        this.currencySymbol = currencySymbol;
        this.serverName = serverName;
    }

    public static DailyTasksSnapshot fromJson(String json)
    {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        String periodDate = root.has("period_date")
            ? root.get("period_date").getAsString() : "";
        Instant resetAt = Instant.parse(root.get("reset_at").getAsString());
        JsonArray values = root.getAsJsonArray("tasks");
        List<DailyTaskSummary> tasks = new ArrayList<>();
        for (JsonElement element : values)
        {
            JsonObject value = element.getAsJsonObject();
            tasks.add(new DailyTaskSummary(
                value.get("category").getAsString(),
                value.get("name").getAsString(),
                value.get("description").getAsString(),
                value.get("target").getAsInt(),
                value.has("progress") ? value.get("progress").getAsInt()
                    : (value.get("completed").getAsBoolean()
                        ? value.get("target").getAsInt() : 0),
                value.get("reward").getAsInt(),
                value.get("completed").getAsBoolean(),
                value.get("awarded").getAsInt(),
                value.has("placement") && !value.get("placement").isJsonNull()
                    ? value.get("placement").getAsInt() : null));
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
                : "ClanHQ");
    }

    public Instant getResetAt() { return resetAt; }
    public String getPeriodDate() { return periodDate; }
    public List<DailyTaskSummary> getTasks() { return tasks; }
    public int getBalance() { return balance; }
    public String getCurrencyName() { return currencyName; }
    public String getCurrencySymbol() { return currencySymbol; }
    public String getServerName() { return serverName; }
}
