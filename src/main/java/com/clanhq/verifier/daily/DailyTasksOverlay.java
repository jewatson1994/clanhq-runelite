package com.clanhq.verifier.daily;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.daily.model.DailyTaskSummary;
import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import com.clanhq.verifier.loot.ObservedDrop;
import com.clanhq.verifier.task.VerificationType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.config.ConfigManager;

/**
 * Compact in-game view of the current ClanHQ tasks.
 *
 * The API snapshot is the source of truth. While the player is logged in, local
 * skill and loot events are layered on top so the bars move immediately instead
 * of waiting for the next server refresh.
 */
public final class DailyTasksOverlay extends OverlayPanel
{
    private static final String STATE_KEY = "dailyTasksLocalProgress";
    private static final NumberFormat NUMBERS =
        NumberFormat.getIntegerInstance(Locale.US);
    private static final Color ACCENT = new Color(86, 168, 255);
    private static final Color COMPLETE = new Color(67, 190, 117);
    private static final Color MUTED = new Color(185, 185, 185);
    private static final int MIN_OVERLAY_WIDTH = 180;

    private final Supplier<DailyTasksSnapshot> snapshotSupplier;
    private final ClanHQVerifierConfig config;
    private final ConfigManager configManager;
    private final BiConsumer<String, Integer> liveProgressListener;
    private final Object stateLock = new Object();
    private final Map<String, Integer> liveProgress = new HashMap<>();
    private final Map<String, Integer> skillBaselines = new HashMap<>();
    private final Map<String, Integer> latestSkillExperience = new HashMap<>();
    private volatile String selectedCategory;
    private volatile Instant loadedResetAt;

    public DailyTasksOverlay(Supplier<DailyTasksSnapshot> snapshotSupplier,
        ConfigManager configManager,
        ClanHQVerifierConfig config)
    {
        this(snapshotSupplier, configManager, config, (category, progress) -> { });
    }

    public DailyTasksOverlay(Supplier<DailyTasksSnapshot> snapshotSupplier,
        ConfigManager configManager,
        ClanHQVerifierConfig config,
        BiConsumer<String, Integer> liveProgressListener)
    {
        this.snapshotSupplier = snapshotSupplier;
        this.config = config;
        this.configManager = configManager;
        this.liveProgressListener = liveProgressListener == null
            ? (category, progress) -> { } : liveProgressListener;
        loadPersistedState();
        setPosition(OverlayPosition.TOP_LEFT);
        setPreferredSize(new Dimension(config.dailyTasksOverlayWidth(), 0));
        // Width is controlled through ClanHQ's RuneLite configuration. Keep
        // native overlay resizing disabled so no resize hitbox can overlap
        // normal game controls.
        setMovable(false);
        setResizable(false);
        setMinimumSize(MIN_OVERLAY_WIDTH);
        setPreferredColor(ColorScheme.DARK_GRAY_COLOR);
        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Show", "All tasks",
            entry -> selectTask(null));
        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Show", "Skilling task",
            entry -> selectTask("SKILLING"));
        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Show", "Activities task",
            entry -> selectTask("ACTIVITIES"));
        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Show", "PvM task",
            entry -> selectTask("PVM"));
        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Show", "Drop task",
            entry -> selectTask("DROP"));
    }

    /** Replace the server snapshot and preserve local progress for this period. */
    public void setSnapshot(DailyTasksSnapshot snapshot)
    {
        synchronized (stateLock)
        {
            boolean newPeriod = snapshot != null && (loadedResetAt == null
                || !loadedResetAt.equals(snapshot.getResetAt()));
            if (newPeriod)
            {
                liveProgress.clear();
                skillBaselines.clear();
                latestSkillExperience.clear();
            }
            if (snapshot != null)
            {
                for (DailyTaskSummary task : snapshot.getTasks())
                {
                    String key = taskKey(task);
                    liveProgress.put(key, Math.max(
                        liveProgress.getOrDefault(key, 0), task.getProgress()));
                    if ("SKILLING".equals(task.getCategory()))
                    {
                        latestSkillExperience.forEach((skill, experience) ->
                        {
                            if (DailyTaskSkillMatcher.matches(task, skill))
                            {
                                int baseline = skillBaselines.computeIfAbsent(key,
                                    ignored -> Math.max(0,
                                        experience - task.getProgress()));
                                int progress = Math.min(task.getTarget(),
                                    Math.max(task.getProgress(), experience - baseline));
                                liveProgress.put(key, Math.max(
                                    liveProgress.getOrDefault(key, 0), progress));
                            }
                        });
                    }
                }
                loadedResetAt = snapshot.getResetAt();
                persistState();
            }
        }
        revalidate();
    }

    /** Apply a local skill XP event to the matching skilling task. */
    public void observeSkillExperience(String skillName, int experience)
    {
        // RuneLite can briefly report -1 or 0 while the logged-in skill table
        // is still initializing. Never use those values as a task baseline.
        if (experience <= 0)
        {
            return;
        }
        synchronized (stateLock)
        {
            latestSkillExperience.put(normalize(skillName), experience);
        }
        DailyTasksSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null || skillName == null)
        {
            return;
        }
        synchronized (stateLock)
        {
            for (DailyTaskSummary task : snapshot.getTasks())
            {
                if (!"SKILLING".equals(task.getCategory())
                    || !DailyTaskSkillMatcher.matches(task, skillName))
                {
                    continue;
                }
                String key = taskKey(task);
                int baseline = skillBaselines.computeIfAbsent(key,
                    ignored -> Math.max(0, experience - task.getProgress()));
                int progress = Math.max(task.getProgress(), experience - baseline);
                int live = Math.min(task.getTarget(), progress);
                liveProgress.put(key, live);
                persistState();
                revalidate();
                liveProgressListener.accept(task.getCategory(), live);
                return;
            }
        }
    }

    /** Apply a local loot-tracker event to the matching PvM/activities task. */
    public void observeLoot(String sourceName)
    {
        DailyTasksSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null || sourceName == null || sourceName.trim().isEmpty())
        {
            return;
        }
        synchronized (stateLock)
        {
            for (DailyTaskSummary task : snapshot.getTasks())
            {
                if (("PVM".equals(task.getCategory())
                        || isActivities(task.getCategory()))
                    && task.getVerificationType() != VerificationType.ITEM_DROP
                    && mentions(task, sourceName))
                {
                    String key = taskKey(task);
                    int progress = liveProgress.getOrDefault(key,
                        task.getProgress());
                    int live = Math.min(task.getTarget(), progress + 1);
                    liveProgress.put(key, live);
                    persistState();
                    revalidate();
                    liveProgressListener.accept(task.getCategory(), live);
                    return;
                }
            }
        }
    }

    /** Apply a verified loot-tracker drop to an ITEM_DROP task. */
    public void observeDrop(ObservedDrop drop)
    {
        DailyTasksSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null || drop == null || drop.getItems() == null)
        {
            return;
        }
        synchronized (stateLock)
        {
            for (DailyTaskSummary task : snapshot.getTasks())
            {
                if (task.getVerificationType() != VerificationType.ITEM_DROP
                    || task.getVerificationItemId() == null)
                {
                    continue;
                }
                int quantity = 0;
                for (net.runelite.client.game.ItemStack item : drop.getItems())
                {
                    if (item.getId() == task.getVerificationItemId())
                    {
                        quantity += Math.max(0, item.getQuantity());
                    }
                }
                if (quantity <= 0)
                {
                    continue;
                }
                String key = taskKey(task);
                int progress = liveProgress.getOrDefault(key,
                    task.getProgress());
                int live = Math.min(task.getTarget(), progress + quantity);
                liveProgress.put(key, live);
                persistState();
                revalidate();
                liveProgressListener.accept(task.getCategory(), live);
                return;
            }
        }
    }

    /**
     * Push the current local progress to consumers after a fresh snapshot is
     * rendered. This also covers progress restored from the persisted local
     * state before the next in-game event arrives.
     */
    public void publishLiveProgress()
    {
        DailyTasksSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null)
        {
            return;
        }
        for (DailyTaskSummary task : snapshot.getTasks())
        {
            liveProgressListener.accept(task.getCategory(), progressFor(task));
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        DailyTasksSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null)
        {
            return null;
        }

        PanelComponent panel = new PanelComponent();
        int width = currentWidth();
        panel.setPreferredSize(new Dimension(width, 0));
        panel.setBackgroundColor(new Color(35, 35, 39, 238));
        panel.setGap(new Point(0, 4));
        panel.getChildren().add(TitleComponent.builder()
            .text(headerTitle(snapshot))
            .color(Color.WHITE)
            .build());
        panel.getChildren().add(LineComponent.builder()
            .left("Live progress")
            .right("resets " + resetText(snapshot))
            .leftColor(MUTED)
            .rightColor(MUTED)
            .build());

        for (DailyTaskSummary task : snapshot.getTasks())
        {
            if (selectedCategory != null
                && !selectedCategory.equals(task.getCategory()))
            {
                continue;
            }
            addTask(panel, task);
        }
        panel.getChildren().add(LineComponent.builder()
            .left("Balance")
            .right(NUMBERS.format(snapshot.getBalance()) + " "
                + snapshot.getCurrencyName())
            .leftColor(Color.WHITE)
            .rightColor(ACCENT)
            .build());
        return panel.render(graphics);
    }

    private void addTask(PanelComponent panel, DailyTaskSummary task)
    {
        int progress = progressFor(task);
        boolean complete = task.isCompleted() || progress >= task.getTarget();
        Color color = complete ? COMPLETE : categoryColor(task.getCategory());
        String label = titleCase(task.getCategory()).toUpperCase(Locale.ROOT) + "  "
            + (complete ? "\u2713" : "\u2022");
        panel.getChildren().add(LineComponent.builder()
            .left(label + "  " + truncate(displayTaskName(task),
                Math.max(12, (currentWidth() - 80) / 7)))
            .right(isSkilling(task.getCategory()) ? "" : NUMBERS.format(progress) + "/"
                + NUMBERS.format(task.getTarget()))
            .leftColor(color)
            .rightColor(complete ? COMPLETE : Color.WHITE)
            .build());
        if (isSkilling(task.getCategory()) && task.getTarget() > 1)
        {
            ProgressBarComponent bar = new ProgressBarComponent();
            bar.setMinimum(0);
            bar.setMaximum(Math.max(1, task.getTarget()));
            bar.setValue(progress);
            bar.setPreferredSize(new Dimension(Math.max(1, overlayWidth()), 12));
            bar.setForegroundColor(color);
            bar.setBackgroundColor(new Color(70, 70, 76));
            bar.setFontColor(Color.WHITE);
            bar.setCenterLabel(NUMBERS.format(progress) + " / "
                + NUMBERS.format(task.getTarget()));
            panel.getChildren().add(bar);
        }
    }

    private static String displayTaskName(DailyTaskSummary task)
    {
        String name = task.getName();
        if (isSkilling(task.getCategory()) && name != null
            && name.matches("(?i).*\\s+training$"))
        {
            return name.replaceFirst("(?i)\\s+training$", "");
        }
        return name;
    }

    private int progressFor(DailyTaskSummary task)
    {
        synchronized (stateLock)
        {
            return Math.max(task.getProgress(), liveProgress.getOrDefault(
                taskKey(task), task.getProgress()));
        }
    }

    public JsonArray buildClientProgress(String category)
    {
        JsonArray values = new JsonArray();
        DailyTasksSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null)
        {
            return values;
        }
        for (DailyTaskSummary task : snapshot.getTasks())
        {
            if (category != null && !category.equals(task.getCategory()))
            {
                continue;
            }
            JsonObject value = new JsonObject();
            value.addProperty("category", task.getCategory());
            value.addProperty("name", task.getName());
            value.addProperty("target", task.getTarget());
            value.addProperty("progress", progressFor(task));
            values.add(value);
        }
        return values;
    }

    public void clearPersistedState()
    {
        synchronized (stateLock)
        {
            liveProgress.clear();
            skillBaselines.clear();
            latestSkillExperience.clear();
            loadedResetAt = null;
            configManager.unsetConfiguration(
                ClanHQVerifierConfig.GROUP, STATE_KEY);
        }
        revalidate();
    }

    private void loadPersistedState()
    {
        String raw = configManager.getConfiguration(
            ClanHQVerifierConfig.GROUP, STATE_KEY);
        if (raw == null || raw.trim().isEmpty())
        {
            return;
        }
        try
        {
            JsonObject root = new JsonParser().parse(raw).getAsJsonObject();
            if (root.has("reset_at"))
            {
                loadedResetAt = Instant.parse(root.get("reset_at").getAsString());
            }
            readIntMap(root, "progress", liveProgress);
            readIntMap(root, "baselines", skillBaselines);
            readIntMap(root, "latest_xp", latestSkillExperience);
        }
        catch (RuntimeException ignored)
        {
            configManager.unsetConfiguration(ClanHQVerifierConfig.GROUP, STATE_KEY);
        }
    }

    private void persistState()
    {
        if (loadedResetAt == null)
        {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("reset_at", loadedResetAt.toString());
        root.add("progress", writeIntMap(liveProgress));
        root.add("baselines", writeIntMap(skillBaselines));
        root.add("latest_xp", writeIntMap(latestSkillExperience));
        configManager.setConfiguration(
            ClanHQVerifierConfig.GROUP, STATE_KEY, root.toString());
    }

    private static JsonObject writeIntMap(Map<String, Integer> values)
    {
        JsonObject result = new JsonObject();
        values.forEach((key, value) -> result.addProperty(key, value));
        return result;
    }

    private static void readIntMap(
        JsonObject root,
        String name,
        Map<String, Integer> target)
    {
        if (!root.has(name) || !root.get(name).isJsonObject())
        {
            return;
        }
        root.getAsJsonObject(name).entrySet().forEach(entry ->
            target.put(entry.getKey(), entry.getValue().getAsInt()));
    }

    private void selectTask(String category)
    {
        selectedCategory = category;
        revalidate();
    }

    private int overlayWidth()
    {
        return currentWidth() - 12;
    }

    private int currentWidth()
    {
        int configured = config.dailyTasksOverlayWidth();
        return Math.max(MIN_OVERLAY_WIDTH, Math.min(500, configured));
    }

    private static String truncate(String value, int maxLength)
    {
        if (value == null || value.length() <= maxLength)
        {
            return value == null ? "" : value;
        }
        return maxLength <= 3 ? value.substring(0, maxLength)
            : value.substring(0, maxLength - 3) + "...";
    }

    private static String headerTitle(DailyTasksSnapshot snapshot)
    {
        if (snapshot.getContext() != null
            && snapshot.getContext().getTitle() != null
            && !snapshot.getContext().getTitle().trim().isEmpty())
        {
            return snapshot.getContext().getTitle();
        }
        return "ClanHQ Tasks";
    }

    private static String resetText(DailyTasksSnapshot snapshot)
    {
        long seconds = Math.max(0,
            rotationEnd(snapshot).getEpochSecond() - Instant.now().getEpochSecond());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    private static Instant rotationEnd(DailyTasksSnapshot snapshot)
    {
        return snapshot.getContext() != null
            && snapshot.getContext().getRotationEndsAt() != null
            ? snapshot.getContext().getRotationEndsAt()
            : snapshot.getResetAt();
    }

    private static Color categoryColor(String category)
    {
        if ("SKILLING".equals(category))
        {
            return new Color(219, 174, 76);
        }
        if (isActivities(category))
        {
            return new Color(174, 126, 236);
        }
        return new Color(239, 111, 111);
    }

    private static boolean isSkilling(String category)
    {
        return category != null && "SKILLING".equalsIgnoreCase(category.trim());
    }

    private static boolean isActivities(String category)
    {
        return category != null
            && ("ACTIVITIES".equalsIgnoreCase(category.trim())
                || "MINIGAME".equalsIgnoreCase(category.trim()));
    }

    private static boolean mentions(DailyTaskSummary task, String value)
    {
        String expected = normalize(value);
        if (expected.isEmpty())
        {
            return false;
        }
        return normalize(task.getName()).contains(expected)
            || normalize(task.getDescription()).contains(expected)
            || expected.contains(normalize(task.getName()));
    }

    private static String taskKey(DailyTaskSummary task)
    {
        if (task.getId() != null && !task.getId().trim().isEmpty())
        {
            return "id:" + task.getId().trim();
        }
        return task.getCategory() + ":" + task.getName();
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.replace('_', ' ')
            .trim().toLowerCase(Locale.ENGLISH);
    }

    private static String titleCase(String value)
    {
        if (value == null || value.isEmpty())
        {
            return "Task";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0))
            + normalized.substring(1);
    }
}
