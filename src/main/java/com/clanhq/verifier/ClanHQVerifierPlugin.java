package com.clanhq.verifier;

import com.clanhq.verifier.model.VerificationSnapshot;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.EvidenceStage;
import com.clanhq.verifier.model.EvidenceStageStatus;
import com.clanhq.verifier.model.RaidKillCounts;
import com.clanhq.verifier.model.VerificationSession;
import com.clanhq.verifier.service.LocalPlayerSnapshotService;
import com.clanhq.verifier.service.IronDropQualificationService;
import com.clanhq.verifier.service.RaidKillCountService;
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
    private static final int CAPTURE_DURATION_SECONDS = 15;

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

    @Inject
    private RaidKillCountService raidKillCountService;

    private ClanHQVerifierPanel panel;
    private NavigationButton navigationButton;
    private Timer captureTimer;
    private int secondsRemaining;
    private String requestedRank;
    private VerificationSession verificationSession;
    private VerificationSnapshot capturedSnapshot;
    private RaidKillCounts raidKillCounts;

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
            this::captureEvidence,
            this::startSession);
        startSession(qualificationService.getRankNames().get(0));
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

    private void captureEvidence(String rankName, EvidenceStage stage)
    {
        ensureSession(rankName);
        switch (stage)
        {
            case ACCOUNT:
                captureAccount();
                break;
            case GEAR:
                captureGear();
                break;
            case RAID_KC:
                fetchRaidKillCounts();
                break;
            case COLLECTION_LOG:
            case POH:
                verificationSession.setStatus(stage,
                    EvidenceStageStatus.MANUAL_REVIEW);
                panel.showStageStatus(stage, EvidenceStageStatus.MANUAL_REVIEW);
                panel.showMessage(stage.getDisplayName()
                    + " collector is the next implementation step.");
                break;
            default:
                throw new IllegalArgumentException("Unsupported evidence stage");
        }
    }

    private void captureAccount()
    {
        verificationSession.setStatus(EvidenceStage.ACCOUNT,
            EvidenceStageStatus.CAPTURING);
        panel.showStageStatus(EvidenceStage.ACCOUNT,
            EvidenceStageStatus.CAPTURING);
        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot snapshot = snapshotService.captureAccountEvidence();
                SwingUtilities.invokeLater(() -> acceptAccountSnapshot(snapshot));
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() -> failStage(
                    EvidenceStage.ACCOUNT, exception));
            }
        });
    }

    private void captureGear()
    {
        panel.setGearBusy(CAPTURE_DURATION_SECONDS);
        verificationSession.setStatus(EvidenceStage.GEAR,
            EvidenceStageStatus.CAPTURING);

        clientThread.invokeLater(() ->
        {
            try
            {
                snapshotService.startCaptureSession();
                SwingUtilities.invokeLater(this::startCaptureTimer);
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() -> failStage(
                    EvidenceStage.GEAR, exception));
            }
        });
    }

    private void fetchRaidKillCounts()
    {
        verificationSession.setStatus(EvidenceStage.RAID_KC,
            EvidenceStageStatus.CAPTURING);
        panel.showStageStatus(EvidenceStage.RAID_KC,
            EvidenceStageStatus.CAPTURING);
        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot account = capturedSnapshot == null
                    ? snapshotService.captureAccountEvidence() : capturedSnapshot;
                String rsn = account.getRsn();
                SwingUtilities.invokeLater(() -> acceptAccountSnapshot(account));
                raidKillCountService.lookupAsync(rsn).thenAccept(counts ->
                    SwingUtilities.invokeLater(() -> acceptRaidKillCounts(counts)));
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() -> failStage(
                    EvidenceStage.RAID_KC, exception));
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
                    failStage(EvidenceStage.GEAR, exception);
                });
            }
        });
    }

    private void finishCapture()
    {
        VerificationSnapshot snapshot =
            snapshotService.finishCaptureSession();
        SwingUtilities.invokeLater(() ->
        {
            capturedSnapshot = raidKillCounts == null
                ? snapshot : snapshot.withRaidKillCounts(raidKillCounts);
            verificationSession.bindRsn(snapshot.getRsn());
            verificationSession.setStatus(EvidenceStage.ACCOUNT,
                EvidenceStageStatus.CAPTURED);
            verificationSession.setStatus(EvidenceStage.GEAR,
                EvidenceStageStatus.CAPTURED);
            panel.showStageStatus(EvidenceStage.ACCOUNT,
                EvidenceStageStatus.CAPTURED);
            panel.showStageStatus(EvidenceStage.GEAR,
                EvidenceStageStatus.CAPTURED);
            renderSnapshot("Gear captured.");
        });
    }

    private void acceptAccountSnapshot(VerificationSnapshot snapshot)
    {
        capturedSnapshot = raidKillCounts == null
            ? snapshot : snapshot.withRaidKillCounts(raidKillCounts);
        verificationSession.bindRsn(snapshot.getRsn());
        verificationSession.setStatus(EvidenceStage.ACCOUNT,
            EvidenceStageStatus.CAPTURED);
        panel.showStageStatus(EvidenceStage.ACCOUNT,
            EvidenceStageStatus.CAPTURED);
        renderSnapshot("Account evidence captured.");
    }

    private void acceptRaidKillCounts(RaidKillCounts counts)
    {
        raidKillCounts = counts;
        if (capturedSnapshot != null)
        {
            capturedSnapshot = capturedSnapshot.withRaidKillCounts(counts);
        }
        EvidenceStageStatus status = counts.isAvailable()
            ? EvidenceStageStatus.CAPTURED
            : EvidenceStageStatus.MANUAL_REVIEW;
        verificationSession.setStatus(EvidenceStage.RAID_KC, status);
        panel.showStageStatus(EvidenceStage.RAID_KC, status);
        renderSnapshot(counts.isAvailable()
            ? "Raid KC fetched." : counts.getDetail());
    }

    private void failStage(EvidenceStage stage, RuntimeException exception)
    {
        verificationSession.setStatus(stage, EvidenceStageStatus.FAILED);
        panel.showStageStatus(stage, EvidenceStageStatus.FAILED);
        panel.showError(exception.getMessage());
    }

    private void renderSnapshot(String status)
    {
        if (capturedSnapshot == null)
        {
            panel.showMessage(status);
            return;
        }
        RankQualificationResult qualification =
            qualificationService.evaluateTarget(capturedSnapshot, requestedRank);
        panel.showSnapshot(capturedSnapshot, qualification, status);
    }

    private void startSession(String rankName)
    {
        requestedRank = rankName;
        capturedSnapshot = null;
        raidKillCounts = null;
        verificationSession = new VerificationSession(rankName,
            qualificationService.getRequiredStages(rankName));
        if (panel != null)
        {
            panel.setRequiredStages(verificationSession.getRequiredStages());
        }
    }

    private void ensureSession(String rankName)
    {
        if (verificationSession == null
            || !verificationSession.getRequestedRank().equals(rankName))
        {
            startSession(rankName);
        }
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
