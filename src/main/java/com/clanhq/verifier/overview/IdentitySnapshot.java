package com.clanhq.verifier.overview;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IdentitySnapshot
{
    private final String deviceName;
    private final List<String> rsns;
    private final int balance;
    private final String currencyName;
    private final String currencySymbol;
    private final String serverName;
    private final int todayEarned;
    private final int weekEarned;
    private final int monthEarned;
    private final int allTimeEarned;
    private final int allTimeRank;

    private IdentitySnapshot(String deviceName, List<String> rsns, int balance,
        String currencyName, String currencySymbol, String serverName,
        int todayEarned, int weekEarned, int monthEarned, int allTimeEarned,
        int allTimeRank)
    {
        this.deviceName = deviceName;
        this.rsns = Collections.unmodifiableList(new ArrayList<>(rsns));
        this.balance = balance;
        this.currencyName = currencyName;
        this.currencySymbol = currencySymbol;
        this.serverName = serverName;
        this.todayEarned = todayEarned;
        this.weekEarned = weekEarned;
        this.monthEarned = monthEarned;
        this.allTimeEarned = allTimeEarned;
        this.allTimeRank = allTimeRank;
    }

    public static IdentitySnapshot fromJson(String json)
    {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        List<String> rsns = new ArrayList<>();
        JsonArray values = root.getAsJsonArray("rsns");
        for (JsonElement value : values)
        {
            rsns.add(value.getAsString());
        }
        JsonElement balanceValue = root.has("currency_balance")
            ? root.get("currency_balance") : root.get("dripdrops_balance");
        boolean hasAllTimeEarned = root.has("all_time_earned")
            && !root.get("all_time_earned").isJsonNull();
        int allTimeEarned = hasAllTimeEarned
            ? root.get("all_time_earned").getAsInt() : 0;
        // Older ClanHQ API builds did not expose the earned-total field.  A
        // paired wallet with a positive balance should not render a misleading
        // zero while that server is being upgraded.
        if (!hasAllTimeEarned && balanceValue != null
            && balanceValue.getAsInt() > 0)
        {
            allTimeEarned = balanceValue.getAsInt();
        }
        return new IdentitySnapshot(
            root.get("device_name").getAsString(),
            rsns,
            balanceValue == null ? 0 : balanceValue.getAsInt(),
            root.has("currency_name")
                ? root.get("currency_name").getAsString() : "Currency",
            root.has("currency_symbol")
                ? root.get("currency_symbol").getAsString() : "",
            root.has("server_name") ? root.get("server_name").getAsString()
                : "ClanHQ",
            root.has("today_earned") ? root.get("today_earned").getAsInt() : 0,
            root.has("week_earned") ? root.get("week_earned").getAsInt() : 0,
            root.has("month_earned") ? root.get("month_earned").getAsInt() : 0,
            allTimeEarned,
            root.has("all_time_rank") && !root.get("all_time_rank").isJsonNull()
                ? root.get("all_time_rank").getAsInt() : 0);
    }

    public String getDeviceName() { return deviceName; }
    public List<String> getRsns() { return rsns; }
    public int getBalance() { return balance; }
    public String getCurrencyName() { return currencyName; }
    public String getCurrencySymbol() { return currencySymbol; }
    public String getServerName() { return serverName; }
    public int getTodayEarned() { return todayEarned; }
    public int getWeekEarned() { return weekEarned; }
    public int getMonthEarned() { return monthEarned; }
    public int getAllTimeEarned() { return allTimeEarned; }
    public int getAllTimeRank() { return allTimeRank; }
}
