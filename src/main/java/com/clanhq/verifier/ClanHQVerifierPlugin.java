package com.clanhq.verifier;

import com.clanhq.verifier.model.VerificationSnapshot;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.service.LocalPlayerSnapshotService;
import com.clanhq.verifier.service.IronDropQualificationService;
import com.clanhq.verifier.transport.PreviewOnlyVerificationTransport;
import com.clanhq.verifier.transport.VerificationTransport;
import com.clanhq.verifier.transport.VerificationTransportResult;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;

@PluginDescriptor(
    name = "ClanHQ Verifier",
    description = "Preview account evidence for ClanHQ rank verification",
    tags = {"clan", "gear", "rank", "verification"})
public final class ClanHQVerifierPlugin extends Plugin
{
    private static final int CAPTURE_DURATION_SECONDS = 30;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private LocalPlayerSnapshotService snapshotService;

    @Inject
    private VerificationTransport transport;

    @Inject
    private IronDropQualificationService qualificationService;

    private ClanHQVerifierPanel panel;
    private NavigationButton navigationButton;
    private Timer captureTimer;
    private int secondsRemaining;
    private String requestedRank;

    @Provides
    ClanHQVerifierConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ClanHQVerifierConfig.class);
    }

    @Provides
    VerificationTransport provideTransport()
    {
        return new PreviewOnlyVerificationTransport();
    }

    @Override
    protected void startUp()
    {
        panel = new ClanHQVerifierPanel(
            qualificationService.getRankNames(),
            this::captureCurrentCharacter);
        navigationButton = NavigationButton.builder()
            .tooltip("ClanHQ Verifier")
            .icon(createIcon())
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navigationButton);
    }

    @Override
    protected void shutDown()
    {
        stopCaptureTimer();
        clientToolbar.removeNavigation(navigationButton);
        snapshotService.cancelCaptureSession();
        navigationButton = null;
        panel = null;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        snapshotService.observeItemContainer(event);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            boolean captureWasActive = snapshotService.isCaptureActive();
            snapshotService.cancelCaptureSession();

            if (captureWasActive)
            {
                SwingUtilities.invokeLater(() ->
                {
                    stopCaptureTimer();
                    panel.showError("Capture cancelled because you logged out.");
                });
            }
        }
    }

    private void captureCurrentCharacter(String rankName)
    {
        requestedRank = rankName;
        panel.setBusy(CAPTURE_DURATION_SECONDS);

        clientThread.invokeLater(() ->
        {
            try
            {
                snapshotService.startCaptureSession();
                SwingUtilities.invokeLater(this::startCaptureTimer);
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() -> panel.showError(
                    exception.getMessage()));
            }
        });
    }

    private void startCaptureTimer()
    {
        stopCaptureTimer();
        secondsRemaining = CAPTURE_DURATION_SECONDS;
        captureTimer = new Timer(1000, event -> captureTick());
        captureTimer.start();
    }

    private void captureTick()
    {
        int remainingAfterTick = --secondsRemaining;
        boolean shouldFinish = remainingAfterTick <= 0;
        panel.showCaptureProgress(Math.max(remainingAfterTick, 0));

        if (shouldFinish)
        {
            stopCaptureTimer();
        }

        clientThread.invokeLater(() ->
        {
            try
            {
                snapshotService.observeCurrentState();

                if (shouldFinish && snapshotService.isCaptureActive())
                {
                    finishCapture();
                }
            }
            catch (RuntimeException exception)
            {
                snapshotService.cancelCaptureSession();
                SwingUtilities.invokeLater(() ->
                {
                    stopCaptureTimer();
                    panel.showError(exception.getMessage());
                });
            }
        });
    }

    private void finishCapture()
    {
        VerificationSnapshot snapshot =
            snapshotService.finishCaptureSession();
        RankQualificationResult qualification =
            qualificationService.evaluateTarget(snapshot, requestedRank);
        VerificationTransportResult result = transport.submit(snapshot);

        SwingUtilities.invokeLater(() -> panel.showSnapshot(
            snapshot,
            java.util.Collections.singletonList(qualification),
            result.getMessage()));
    }

    private void stopCaptureTimer()
    {
        if (captureTimer != null)
        {
            captureTimer.stop();
            captureTimer = null;
        }
    }

    private static BufferedImage createIcon()
    {
        BufferedImage icon = new BufferedImage(
            16,
            16,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();

        graphics.setColor(new Color(212, 175, 55));
        graphics.fillOval(1, 1, 14, 14);
        graphics.setColor(new Color(30, 33, 36));
        graphics.fillOval(4, 4, 8, 8);
        graphics.dispose();

        return icon;
    }
}
