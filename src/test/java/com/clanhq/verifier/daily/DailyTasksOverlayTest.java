package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import com.google.gson.JsonArray;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class DailyTasksOverlayTest
{
    private static DailyTasksSnapshot snapshot(String reset)
    {
        return DailyTasksSnapshot.fromJson("{\"period_date\":\"2026-10-14\","
            + "\"reset_at\":\"" + reset + "\",\"tasks\":[{"
            + "\"id\":\"mining\",\"category\":\"SKILLING\",\"name\":\"Mining\","
            + "\"target\":1000,\"progress\":0}]} ");
    }

    private final AtomicReference<DailyTasksSnapshot> snapshot =
        new AtomicReference<>(snapshot("2026-10-15T00:00:00Z"));
    private final Map<String, String> disk = new HashMap<>();

    private DailyTasksOverlay client()
    {
        return new DailyTasksOverlay(snapshot::get, disk::get, (key, value) -> {
            if (value == null) { disk.remove(key); }
            else { disk.put(key, value); }
        });
    }

    private int progress(DailyTasksOverlay overlay)
    {
        return overlay.progressFor(snapshot.get().getTasks().get(0));
    }

    @Test
    public void simultaneousClientsSharingProfileNeverCompareDifferentAccountsXp()
    {
        DailyTasksOverlay low = client();
        low.setAccount("installation", "Low Level");
        low.observeSkillExperience("Low Level", "Mining", 100);
        low.observeSkillExperience("Low Level", "Mining", 150);
        DailyTasksOverlay high = client();
        high.setAccount("installation", "High Level");
        high.observeSkillExperience("High Level", "Mining", 13034431);
        assertEquals(0, progress(high));
        high.observeSkillExperience("High Level", "Mining", 13034531);
        assertEquals(100, progress(high));
        low.observeSkillExperience("Low Level", "Mining", 200);
        assertEquals(100, progress(low));
        assertEquals(2, disk.size());
    }

    @Test
    public void switchingAccountsAndRestartingRestoresOnlyTheirOwnProgress()
    {
        DailyTasksOverlay overlay = client();
        overlay.setAccount("installation", "Low Level");
        overlay.observeSkillExperience("Low Level", "Mining", 100);
        overlay.observeSkillExperience("Low Level", "Mining", 200);
        overlay.setAccount("installation", "High Level");
        overlay.observeSkillExperience("High Level", "Mining", 13034431);
        assertEquals(0, progress(overlay));
        overlay.setAccount("installation", "LOW_LEVEL");
        assertEquals(100, progress(overlay));
        DailyTasksOverlay restarted = client();
        restarted.setAccount("installation", "low\u00a0level");
        restarted.observeSkillExperience("low level", "Mining", 250);
        assertEquals(150, progress(restarted));
    }

    @Test
    public void loginEventsWithoutAnAccountOrForAnotherAccountAreIgnored()
    {
        DailyTasksOverlay overlay = client();
        overlay.observeSkillExperience("Low Level", "Mining", 100);
        assertTrue(overlay.buildClientProgress(null).size() == 0);
        overlay.setAccount("installation", "High Level");
        overlay.observeSkillExperience("Low Level", "Mining", 100);
        overlay.observeSkillExperience(null, "Mining", 100);
        overlay.observeSkillExperience("High Level", "Mining", 13034431);
        assertEquals(0, progress(overlay));
        overlay.setAccount("installation", null);
        overlay.observeSkillExperience("High Level", "Mining", 13035431);
        assertEquals(0, progress(overlay));
        assertEquals(0, overlay.buildClientProgress(null).size());
    }

    @Test
    public void legacyProfileWideProgressIsNotAssignedToTheNextAccount()
    {
        disk.put("dailyTasksLocalProgress", "{\"reset_at\":\"2026-10-15T00:00:00Z\","
            + "\"baselines\":{\"id:mining\":100},\"progress\":{\"id:mining\":1000}}");
        DailyTasksOverlay overlay = client();
        overlay.setAccount("installation", "High Level");
        overlay.observeSkillExperience("High Level", "Mining", 13034431);
        assertEquals(0, progress(overlay));
    }

    @Test
    public void claimsKeepTheRsnThatProducedTheirProgress()
    {
        DailyTasksOverlay overlay = client();
        overlay.setAccount("installation", "Low Level");
        overlay.observeSkillExperience("Low Level", "Mining", 100);
        overlay.observeSkillExperience("Low Level", "Mining", 1100);
        JsonArray claim = overlay.buildClientProgress(null);
        overlay.setAccount("installation", "High Level");
        assertEquals("low level", claim.get(0).getAsJsonObject().get("rsn").getAsString());
        assertEquals(1000, claim.get(0).getAsJsonObject().get("progress").getAsInt());
        assertEquals(0, progress(overlay));
    }

    @Test
    public void newPeriodAndDifferentPairingCannotReuseOldProgress()
    {
        DailyTasksOverlay overlay = client();
        overlay.setAccount("installation", "Low Level");
        overlay.observeSkillExperience("Low Level", "Mining", 100);
        overlay.observeSkillExperience("Low Level", "Mining", 200);
        overlay.setAccount("different-installation", "Low Level");
        assertEquals(0, progress(overlay));
        overlay.setAccount("installation", "Low Level");
        assertEquals(100, progress(overlay));
        snapshot.set(snapshot("2026-10-16T00:00:00Z"));
        overlay.setSnapshot(snapshot.get());
        overlay.observeSkillExperience("Low Level", "Mining", 200);
        assertEquals(0, progress(overlay));
        overlay.observeSkillExperience("Low Level", "Mining", 250);
        assertEquals(50, progress(overlay));
    }
}
