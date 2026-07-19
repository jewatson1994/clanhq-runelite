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
    private final List<DailyTaskSummary> tasks;
    private final int balance;

    private DailyTasksSnapshot(Instant resetAt, List<DailyTaskSummary> tasks,
        int balance)
    {
        this.resetAt = resetAt;
        this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
        this.balance = balance;
    }

    public static DailyTasksSnapshot fromJson(String json)
    {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
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
        return new DailyTasksSnapshot(resetAt, tasks,
            root.has("balance") ? root.get("balance").getAsInt() : 0);
    }

    public Instant getResetAt() { return resetAt; }
    public List<DailyTaskSummary> getTasks() { return tasks; }
    public int getBalance() { return balance; }
}
