package com.clanhq.verifier.daily;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.daily.transport.DailyTasksApiClient;
import com.clanhq.verifier.feature.ClanHQFeature;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public final class DailyTasksFeature implements ClanHQFeature
{
    private final DailyTasksApiClient apiClient;
    private final ClanHQVerifierConfig config;
    private final DailyTasksPanel panel;
    private volatile boolean running;

    public DailyTasksFeature(DailyTasksApiClient apiClient,
        ClanHQVerifierConfig config)
    {
        this.apiClient = apiClient;
        this.config = config;
        this.panel = new DailyTasksPanel(
            this::refresh,
            () -> claim("SKILLING"),
            () -> claim("MINIGAME"),
            () -> claim("PVM"));
    }

    @Override
    public String getId() { return "daily-tasks"; }

    @Override
    public String getDisplayName() { return "Dailies"; }

    @Override
    public String getNavigationIconResource()
    {
        return "/com/clanhq/verifier/icons/dailies.png";
    }

    @Override
    public String getDescription()
    {
        return "View and claim WOM-verified currency daily tasks.";
    }

    @Override
    public JComponent getPanel() { return panel; }

    @Override
    public void startUp()
    {
        running = true;
        refresh();
    }

    @Override
    public void shutDown()
    {
        running = false;
    }

    public void refresh()
    {
        refresh(null);
    }

    private void refresh(String successMessage)
    {
        if (!running)
        {
            return;
        }
        if (normalized(config.installationToken()).isEmpty())
        {
            panel.showUnpaired(
                "Use /plugin pair in Discord, then enter the code in settings.");
            return;
        }
        panel.setLoading("Loading today's tasks...");
        apiClient.fetch().thenAccept(result -> SwingUtilities.invokeLater(() ->
        {
            if (!running)
            {
                return;
            }
            result.getSnapshot().ifPresentOrElse(
                snapshot -> panel.showTasks(snapshot,
                    successMessage == null ? result.getMessage() : successMessage),
                () -> panel.showError(result.getMessage(), true));
        }));
    }

    public void claim(String category)
    {
        panel.setLoading("Refreshing WOM and checking the "
            + category.toLowerCase() + " task...");
        apiClient.claim(category).thenAccept(result -> SwingUtilities.invokeLater(() ->
        {
            if (!running)
            {
                return;
            }
            if (!result.isSuccessful())
            {
                panel.showError(result.getMessage(), true);
                return;
            }
            String message = result.getMessage();
            if (result.getRewardAmount() > 0)
            {
                message += " Awarded " + result.getRewardAmount() + " "
                    + result.getCurrencyName()
                    + (result.getCurrencySymbol().isEmpty()
                        ? "." : " " + result.getCurrencySymbol() + ".");
            }
            refresh(message);
        }));
    }

    private static String normalized(String value)
    {
        return value == null ? "" : value.trim();
    }
}
