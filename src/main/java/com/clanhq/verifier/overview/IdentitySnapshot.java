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

    private IdentitySnapshot(String deviceName, List<String> rsns, int balance,
        String currencyName, String currencySymbol)
    {
        this.deviceName = deviceName;
        this.rsns = Collections.unmodifiableList(new ArrayList<>(rsns));
        this.balance = balance;
        this.currencyName = currencyName;
        this.currencySymbol = currencySymbol;
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
        return new IdentitySnapshot(
            root.get("device_name").getAsString(),
            rsns,
            balanceValue == null ? 0 : balanceValue.getAsInt(),
            root.has("currency_name")
                ? root.get("currency_name").getAsString() : "Currency",
            root.has("currency_symbol")
                ? root.get("currency_symbol").getAsString() : "");
    }

    public String getDeviceName() { return deviceName; }
    public List<String> getRsns() { return rsns; }
    public int getBalance() { return balance; }
    public String getCurrencyName() { return currencyName; }
    public String getCurrencySymbol() { return currencySymbol; }
}
