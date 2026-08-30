package com.clanhq.verifier.overview;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.feature.ClanHQFeature;
import com.clanhq.verifier.bingo.model.BingoManifest;
import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import java.security.SecureRandom;
import java.util.Base64;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.function.Supplier;
import net.runelite.client.config.ConfigManager;

public final class OverviewFeature implements ClanHQFeature
{
    private final IdentityApiClient apiClient;
    private final ClanHQVerifierConfig config;
    private final ConfigManager configManager;
    private final OverviewPanel panel;
    private final Supplier<DailyTasksSnapshot> taskSnapshot;
    private final Supplier<BingoManifest> bingoManifest;
    private final Supplier<String> currentRsn;
    private volatile IdentitySnapshot identity;
    private volatile boolean running;

    public OverviewFeature(IdentityApiClient apiClient,
        ClanHQVerifierConfig config, ConfigManager configManager,
        Supplier<DailyTasksSnapshot> taskSnapshot,
        Supplier<BingoManifest> bingoManifest,
        Supplier<String> currentRsn)
    {
        this.apiClient = apiClient;
        this.config = config;
        this.configManager = configManager;
        this.taskSnapshot = taskSnapshot;
        this.bingoManifest = bingoManifest;
        this.currentRsn = currentRsn;
        this.panel = new OverviewPanel(
            this::pair, this::refresh, this::disconnect);
    }

    public String getId() { return "overview"; }
    public String getDisplayName() { return "Overview"; }
    public String getNavigationIconResource()
    {
        return "/com/clanhq/verifier/icons/overview.png";
    }
    public String getDescription() { return "Connection, identity, and ClanHQ status."; }
    public JComponent getPanel() { return panel; }

    public void startUp()
    {
        running = true;
        refresh();
    }

    public void shutDown()
    {
        running = false;
        identity = null;
    }

    public void refresh()
    {
        if (!running) { return; }
        boolean hasStoredPairing = !normalized(
            config.installationToken()).isEmpty();
        panel.setLoading(hasStoredPairing);
        apiClient.fetch().thenAccept(result -> SwingUtilities.invokeLater(() ->
        {
            if (!running) { return; }
            result.getIdentity().ifPresentOrElse(
                value -> {
                    identity = value;
                    panel.showIdentity(value, taskSnapshot.get(),
                        bingoManifest.get(), currentRsn.get());
                },
                () -> panel.showError(
                    result.getMessage(), hasStoredPairing));
        }));
    }

    public void refreshSummary()
    {
        IdentitySnapshot value = identity;
        if (running && value != null)
        {
            panel.updateToday(taskSnapshot.get(), bingoManifest.get());
        }
    }

    public void pair()
    {
        String code = normalized(config.pairingCode());
        if (code.isEmpty())
        {
            panel.showError(
                "Run /plugin pair in Discord and enter the code in settings.",
                false);
            return;
        }
        String pendingToken = normalized(config.pendingInstallationToken());
        if (pendingToken.isEmpty())
        {
            pendingToken = generateToken();
            configManager.setConfiguration(
                ClanHQVerifierConfig.GROUP,
                "pendingInstallationToken",
                pendingToken);
        }
        panel.setPairing();
        final String installationToken = pendingToken;
        apiClient.pair(code, config.installationLabel(), installationToken)
            .thenAccept(result -> SwingUtilities.invokeLater(() ->
            {
                if (!running)
                {
                    return;
                }
                if (!result.isSuccessful())
                {
                    panel.showError(result.getMessage(), false);
                    return;
                }
                configManager.setConfiguration(
                    ClanHQVerifierConfig.GROUP,
                    "installationToken",
                    installationToken);
                configManager.unsetConfiguration(
                    ClanHQVerifierConfig.GROUP,
                    "pairingCode");
                configManager.unsetConfiguration(
                    ClanHQVerifierConfig.GROUP,
                    "pendingInstallationToken");
                refresh();
            }));
    }

    public void disconnect()
    {
        if (!running || normalized(config.installationToken()).isEmpty())
        {
            panel.showDisconnected("This installation is not paired.");
            return;
        }
        if (!panel.confirmDisconnect())
        {
            return;
        }
        panel.setDisconnecting();
        apiClient.disconnect().thenAccept(result ->
            SwingUtilities.invokeLater(() ->
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
                configManager.unsetConfiguration(
                    ClanHQVerifierConfig.GROUP,
                    "installationToken");
                configManager.unsetConfiguration(
                    ClanHQVerifierConfig.GROUP,
                    "pendingInstallationToken");
                configManager.unsetConfiguration(
                    ClanHQVerifierConfig.GROUP,
                    "pairingCode");
                panel.showDisconnected(result.getMessage());
            }));
    }

    private static String generateToken()
    {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "chq_" + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(bytes);
    }

    private static String normalized(String value)
    {
        return value == null ? "" : value.trim();
    }
}