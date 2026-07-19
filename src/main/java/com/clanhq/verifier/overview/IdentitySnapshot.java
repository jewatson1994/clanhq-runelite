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

    private IdentitySnapshot(String deviceName, List<String> rsns, int balance)
    {
        this.deviceName = deviceName;
        this.rsns = Collections.unmodifiableList(new ArrayList<>(rsns));
        this.balance = balance;
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
        return new IdentitySnapshot(
            root.get("device_name").getAsString(),
            rsns,
            root.get("dripdrops_balance").getAsInt());
    }

    public String getDeviceName() { return deviceName; }
    public List<String> getRsns() { return rsns; }
    public int getBalance() { return balance; }
}
