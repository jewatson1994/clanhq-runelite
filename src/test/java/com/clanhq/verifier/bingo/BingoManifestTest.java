package com.clanhq.verifier.bingo;

import com.clanhq.verifier.bingo.model.BingoManifest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class BingoManifestTest
{
    private static final String BASE_JSON = "{"
        + "\"schema_version\":1,"
        + "\"event_id\":\"evt-1\","
        + "\"name\":\"Winter Bingo\","
        + "\"starts_at\":\"2026-01-01T00:00:00Z\","
        + "\"ends_at\":\"2026-01-08T00:00:00Z\","
        + "\"items\":[{\"item_id\":1,\"name\":\"Item\","
        + "\"minimum_quantity\":1,\"points\":1}]";

    @Test
    public void parsesEventAndChallengePasswordsWhenPresent()
    {
        BingoManifest manifest = BingoManifest.fromJson(BASE_JSON
            + ",\"event_password\":\"clan-event\","
            + "\"challenge_password\":\"clan-challenge\"}");

        assertEquals("clan-event", manifest.getEventPassword());
        assertEquals("clan-challenge", manifest.getChallengePassword());
    }

    @Test
    public void passwordsAreNullWhenAbsentFromManifest()
    {
        BingoManifest manifest = BingoManifest.fromJson(BASE_JSON + "}");

        assertNull(manifest.getEventPassword());
        assertNull(manifest.getChallengePassword());
    }

    @Test
    public void blankPasswordsAreTreatedAsAbsent()
    {
        BingoManifest manifest = BingoManifest.fromJson(BASE_JSON
            + ",\"event_password\":\"   \","
            + "\"challenge_password\":\"\"}");

        assertNull(manifest.getEventPassword());
        assertNull(manifest.getChallengePassword());
    }

    @Test
    public void nullJsonPasswordsAreTreatedAsAbsent()
    {
        BingoManifest manifest = BingoManifest.fromJson(BASE_JSON
            + ",\"event_password\":null,"
            + "\"challenge_password\":null}");

        assertNull(manifest.getEventPassword());
        assertNull(manifest.getChallengePassword());
    }

    @Test
    public void passwordsAreTrimmed()
    {
        BingoManifest manifest = BingoManifest.fromJson(BASE_JSON
            + ",\"event_password\":\"  clan-event  \"}");

        assertEquals("clan-event", manifest.getEventPassword());
    }
}
