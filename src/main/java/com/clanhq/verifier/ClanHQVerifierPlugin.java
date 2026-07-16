package com.clanhq.verifier;

import com.clanhq.verifier.feature.ClanHQFeature;
import com.clanhq.verifier.feature.RankReviewFeature;
import com.clanhq.verifier.bingo.BingoFeature;
import com.clanhq.verifier.bingo.transport.BingoApiClient;
import com.clanhq.verifier.model.VerificationSnapshot;
import com.clanhq.verifier.model.ProgressionEvaluation;
import com.clanhq.verifier.model.EvidenceStage;
import com.clanhq.verifier.model.EvidenceStageStatus;
import com.clanhq.verifier.model.RaidKillCounts;
import com.clanhq.verifier.model.TempleCollectionLogResult;
import com.clanhq.verifier.model.VerificationSession;
import com.clanhq.verifier.model.CollectionLogEvidence;
import com.clanhq.verifier.model.PohEvidence;
import com.clanhq.verifier.service.LocalPlayerSnapshotService;
import com.clanhq.verifier.service.IronDropQualificationService;
import com.clanhq.verifier.service.RaidKillCountService;
import com.clanhq.verifier.service.ApiDestinationService;
import com.clanhq.verifier.service.CollectionLogCaptureService;
import com.clanhq.verifier.service.PohCaptureService;
import com.clanhq.verifier.service.BoatCaptureService;
import com.clanhq.verifier.service.TempleCollectionLogService;
import com.clanhq.verifier.transport.HttpVerificationTransport;
import com.clanhq.verifier.transport.VerificationTransport;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import okhttp3.OkHttpClient;

@PluginDescriptor(
    name = "ClanHQ",
    description = "Clan tools including rank review and event tracking",
    tags = {"clan", "gear", "rank", "verification", "bingo"})
public final class ClanHQVerifierPlugin extends Plugin
{
    private static final Set<EvidenceStage> CORE_SUBMISSION_STAGES =
        Collections.unmodifiableSet(EnumSet.of(
            EvidenceStage.CHARACTER,
            EvidenceStage.GEAR));
    private static final EvidenceStage[] TEMPLE_LOG_STAGES = {
        EvidenceStage.COX_LOG, EvidenceStage.TOB_LOG,
        EvidenceStage.TOA_LOG, EvidenceStage.YAMA_LOG,
        EvidenceStage.DOOM_LOG
    };
    @Inject
    private ClientThread clientThread;

    @Inject
    private Client client;

    @Inject
    private OkHttpClient httpClient;

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

    @Inject
    private ApiDestinationService apiDestinationService;

    @Inject
    private ClanHQVerifierConfig config;

    @Inject
    private CollectionLogCaptureService collectionLogCaptureService;

    @Inject
    private PohCaptureService pohCaptureService;

    @Inject
    private BoatCaptureService boatCaptureService;

    @Inject
    private TempleCollectionLogService templeCollectionLogService;

    private ClanHQVerifierPanel panel;
    private ClanHQPanel shellPanel;
    private BingoFeature bingoFeature;
    private List<ClanHQFeature> features = Collections.emptyList();
    private NavigationButton navigationButton;
    private VerificationSession verificationSession;
    private VerificationSnapshot capturedSnapshot;
    private RaidKillCounts raidKillCounts;
    private boolean reviewSubmitted;

    @Provides
    ClanHQVerifierConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ClanHQVerifierConfig.class);
    }

    @Provides
    VerificationTransport provideTransport(OkHttpClient httpClient,
        ClanHQVerifierConfig verifierConfig,
        ApiDestinationService destinationService)
    {
        return new HttpVerificationTransport(httpClient, verifierConfig,
            destinationService);
    }

    @Override
    protected void startUp()
    {
        panel = new ClanHQVerifierPanel(
            this::captureEvidence,
            this::startSession,
            this::submitVerification);
        refreshApiDestination();
        startSession();
        rebuildFeatures();
    }

    @Override
    protected void shutDown()
    {
        disposeFeatures();
        verificationSession = null;
        capturedSnapshot = null;
        raidKillCounts = null;
        shellPanel = null;
        panel = null;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (ClanHQVerifierConfig.GROUP.equals(event.getGroup()))
        {
            if ("rankReviewEnabled".equals(event.getKey())
                || "bingoEnabled".equals(event.getKey()))
            {
                SwingUtilities.invokeLater(this::rebuildFeatures);
                return;
            }
            SwingUtilities.invokeLater(this::refreshApiDestination);
            if (bingoFeature != null)
            {
                bingoFeature.refreshManifest();
            }
        }
    }

    private void rebuildFeatures()
    {
        disposeFeatures();
        List<ClanHQFeature> enabled = new ArrayList<>();
        if (config.rankReviewEnabled())
        {
            enabled.add(new RankReviewFeature(panel));
        }
        if (config.bingoEnabled())
        {
            bingoFeature = new BingoFeature(new BingoApiClient(
                httpClient, config, apiDestinationService));
            enabled.add(bingoFeature);
        }
        features = enabled;
        shellPanel = new ClanHQPanel(features);
        features.forEach(ClanHQFeature::startUp);
        navigationButton = NavigationButton.builder()
            .tooltip("ClanHQ")
            .icon(createIcon())
            .panel(shellPanel)
            .build();
        clientToolbar.addNavigation(navigationButton);
    }

    private void disposeFeatures()
    {
        features.forEach(ClanHQFeature::shutDown);
        features = Collections.emptyList();
        bingoFeature = null;
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
            navigationButton = null;
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        if (bingoFeature == null || client.getLocalPlayer() == null)
        {
            return;
        }
        bingoFeature.onNpcLoot(
            client.getLocalPlayer().getName(),
            event.getNpc().getName(),
            event.getItems());
    }

    private void captureEvidence(EvidenceStage stage)
    {
        switch (stage)
        {
            case CHARACTER:
                captureAccount();
                break;
            case PRAYERS:
                capturePrayers();
                break;
            case GEAR:
                captureGear();
                break;
            case RAID_KC:
                fetchRaidKillCounts();
                break;
            case COX_LOG:
            case TOB_LOG:
            case TOA_LOG:
            case YAMA_LOG:
            case DOOM_LOG:
                captureCollectionLog(stage);
                break;
            case POH:
                capturePoh();
                break;
            case BOAT:
                captureBoat();
                break;
            default:
                throw new IllegalArgumentException("Unsupported evidence stage");
        }
    }

    private void captureAccount()
    {
        VerificationSession session = verificationSession;
        beginStage(EvidenceStage.CHARACTER);
        beginAutomaticStage(EvidenceStage.PRAYERS);
        beginAutomaticStage(EvidenceStage.BOAT);
        beginAutomaticStage(EvidenceStage.RAID_KC);
        for (EvidenceStage stage : TEMPLE_LOG_STAGES)
        {
            beginAutomaticStage(stage);
        }
        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot snapshot = snapshotService
                    .captureAccountEvidence()
                    .withBoatEvidence(boatCaptureService.captureStoredEvidence());
                raidKillCountService.lookupAsync(snapshot.getRsn())
                    .thenCombine(templeCollectionLogService.lookupAsync(
                            snapshot.getRsn()),
                        (counts, temple) -> new AutomaticEvidence(snapshot,
                            counts, temple))
                    .thenAccept(evidence -> SwingUtilities.invokeLater(() ->
                        acceptAutomaticEvidence(session, evidence)));
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() ->
                {
                    if (isCurrentSession(session))
                    {
                        failStage(EvidenceStage.CHARACTER, exception);
                    }
                });
            }
        });
    }

    private void acceptAutomaticEvidence(VerificationSession session,
        AutomaticEvidence evidence)
    {
        if (!isCurrentSession(session))
        {
            return;
        }
        acceptAccountSnapshot(evidence.snapshot);
        acceptRaidKillCounts(evidence.raidKillCounts);
        acceptTempleCollectionLog(evidence.templeCollectionLog);
        completeStage(EvidenceStage.CHARACTER,
            automaticCaptureMessage(evidence));
    }

    private void acceptTempleCollectionLog(TempleCollectionLogResult result)
    {
        if (result.isFresh())
        {
            capturedSnapshot = capturedSnapshot.withCollectionLogEvidence(
                result.getEvidence());
            for (EvidenceStage stage : TEMPLE_LOG_STAGES)
            {
                verificationSession.setStatus(stage,
                    EvidenceStageStatus.CAPTURED);
                panel.showStageStatus(stage, EvidenceStageStatus.CAPTURED);
            }
            return;
        }
        for (EvidenceStage stage : TEMPLE_LOG_STAGES)
        {
            verificationSession.setStatus(stage,
                EvidenceStageStatus.NOT_CAPTURED);
            panel.showStageStatus(stage, EvidenceStageStatus.NOT_CAPTURED);
            panel.showFallbackStage(stage);
        }
    }

    private static String automaticCaptureMessage(AutomaticEvidence evidence)
    {
        StringBuilder message = new StringBuilder(
            "Character, prayers, and saved boats captured.");
        if (evidence.raidKillCounts.isAvailable())
        {
            message.append(" Raid KC fetched.");
        }
        else
        {
            message.append(' ').append(evidence.raidKillCounts.getDetail())
                .append("; use Fetch Raid KC to retry.");
        }
        if (evidence.templeCollectionLog.isFresh())
        {
            message.append(' ')
                .append(evidence.templeCollectionLog.getMessage()).append('.');
        }
        else
        {
            message.append(' ')
                .append(evidence.templeCollectionLog.getMessage())
                .append("; manual Collection Log capture is available.");
        }
        return message.toString();
    }

    private void capturePrayers()
    {
        VerificationSession session = verificationSession;
        beginStage(EvidenceStage.PRAYERS);
        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot evidence =
                    snapshotService.captureAccountEvidence();
                SwingUtilities.invokeLater(() ->
                {
                    if (!isCurrentSession(session))
                    {
                        return;
                    }
                    if (capturedSnapshot == null)
                    {
                        acceptAccountSnapshot(evidence);
                    }
                    else
                    {
                        capturedSnapshot = capturedSnapshot
                            .withPrayerEvidenceFrom(evidence);
                    }
                    completeStage(EvidenceStage.PRAYERS,
                        "Prayer evidence captured.");
                });
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() ->
                {
                    if (isCurrentSession(session))
                    {
                        failStage(EvidenceStage.PRAYERS, exception);
                    }
                });
            }
        });
    }

    private void captureGear()
    {
        VerificationSession session = verificationSession;
        beginStage(EvidenceStage.GEAR);

        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot snapshot =
                    snapshotService.captureItemsEvidence();
                SwingUtilities.invokeLater(() ->
                {
                    if (isCurrentSession(session))
                    {
                        acceptItemSnapshot(snapshot);
                    }
                });
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() ->
                {
                    if (isCurrentSession(session))
                    {
                        failStage(EvidenceStage.GEAR, exception);
                    }
                });
            }
        });
    }

    private void fetchRaidKillCounts()
    {
        VerificationSession session = verificationSession;
        beginStage(EvidenceStage.RAID_KC);
        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot account = capturedSnapshot == null
                    ? snapshotService.captureAccountEvidence() : capturedSnapshot;
                String rsn = account.getRsn();
                raidKillCountService.lookupAsync(rsn).thenAccept(counts ->
                    SwingUtilities.invokeLater(() ->
                    {
                        if (isCurrentSession(session))
                        {
                            acceptAccountSnapshot(account);
                            acceptRaidKillCounts(counts);
                        }
                    }));
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() ->
                {
                    if (isCurrentSession(session))
                    {
                        failStage(EvidenceStage.RAID_KC, exception);
                    }
                });
            }
        });
    }

    private void captureCollectionLog(EvidenceStage stage)
    {
        VerificationSession session = verificationSession;
        beginStage(stage);
        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot account = capturedSnapshot == null
                    ? snapshotService.captureAccountEvidence() : capturedSnapshot;
                CollectionLogEvidence evidence = captureCollectionLogEvidence(stage);
                SwingUtilities.invokeLater(() ->
                {
                    if (!isCurrentSession(session))
                    {
                        return;
                    }
                    acceptAccountSnapshot(account);
                    capturedSnapshot = capturedSnapshot.withCollectionLogEvidence(evidence);
                    completeStage(stage, stage.getDisplayName() + " captured.");
                });
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() ->
                {
                    if (isCurrentSession(session))
                    {
                        failStage(stage, exception);
                    }
                });
            }
        });
    }

    private CollectionLogEvidence captureCollectionLogEvidence(EvidenceStage stage)
    {
        switch (stage)
        {
            case COX_LOG:
                return collectionLogCaptureService.capturePage("Chambers of Xeric");
            case TOB_LOG:
                return collectionLogCaptureService.capturePage("Theatre of Blood");
            case TOA_LOG:
                return collectionLogCaptureService.capturePage("Tombs of Amascut");
            case YAMA_LOG:
                return collectionLogCaptureService.capturePage("Yama");
            case DOOM_LOG:
                return collectionLogCaptureService.capturePage("Doom of Mokhaiotl");
            default:
                throw new IllegalArgumentException("Not a Collection Log stage: " + stage);
        }
    }

    private void capturePoh()
    {
        VerificationSession session = verificationSession;
        beginStage(EvidenceStage.POH);
        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot account = capturedSnapshot == null
                    ? snapshotService.captureAccountEvidence() : capturedSnapshot;
                PohEvidence evidence = pohCaptureService.capture();
                SwingUtilities.invokeLater(() ->
                {
                    if (!isCurrentSession(session))
                    {
                        return;
                    }
                    acceptAccountSnapshot(account);
                    capturedSnapshot = capturedSnapshot.withPohEvidence(evidence);
                    completeStage(EvidenceStage.POH, "Owner POH captured.");
                });
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() ->
                {
                    if (isCurrentSession(session))
                    {
                        failStage(EvidenceStage.POH, exception);
                    }
                });
            }
        });
    }

    private void captureBoat()
    {
        VerificationSession session = verificationSession;
        beginStage(EvidenceStage.BOAT);
        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot account = capturedSnapshot == null
                    ? snapshotService.captureAccountEvidence() : capturedSnapshot;
                com.clanhq.verifier.model.BoatEvidence evidence =
                    boatCaptureService.captureVisiblePanel();
                SwingUtilities.invokeLater(() ->
                {
                    if (!isCurrentSession(session))
                    {
                        return;
                    }
                    acceptAccountSnapshot(account);
                    capturedSnapshot = capturedSnapshot.withBoatEvidence(evidence);
                    completeStage(EvidenceStage.BOAT,
                        "Sailing boat panel captured.");
                });
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() ->
                {
                    if (isCurrentSession(session))
                    {
                        failStage(EvidenceStage.BOAT, exception);
                    }
                });
            }
        });
    }

    private void beginStage(EvidenceStage stage)
    {
        verificationSession.setStatus(stage, EvidenceStageStatus.CAPTURING);
        panel.setStageBusy(stage);
    }

    private void beginAutomaticStage(EvidenceStage stage)
    {
        verificationSession.setStatus(stage, EvidenceStageStatus.CAPTURING);
        panel.showStageStatus(stage, EvidenceStageStatus.CAPTURING);
    }

    private void completeStage(EvidenceStage stage, String message)
    {
        verificationSession.setStatus(stage, EvidenceStageStatus.CAPTURED);
        panel.showStageStatus(stage, EvidenceStageStatus.CAPTURED);
        renderSnapshot(message);
    }

    private void acceptItemSnapshot(VerificationSnapshot snapshot)
    {
        capturedSnapshot = capturedSnapshot == null
            ? snapshot : capturedSnapshot.withItemEvidenceFrom(snapshot);
        if (raidKillCounts != null)
        {
            capturedSnapshot = capturedSnapshot.withRaidKillCounts(raidKillCounts);
        }
        verificationSession.bindRsn(snapshot.getRsn());
        verificationSession.setStatus(EvidenceStage.CHARACTER,
            EvidenceStageStatus.CAPTURED);
        verificationSession.setStatus(EvidenceStage.GEAR,
            EvidenceStageStatus.CAPTURED);
        panel.showStageStatus(EvidenceStage.CHARACTER,
            EvidenceStageStatus.CAPTURED);
        panel.showStageStatus(EvidenceStage.GEAR,
            EvidenceStageStatus.CAPTURED);
        renderSnapshot("Bank, inventory, and equipped gear captured.");
    }

    private void acceptAccountSnapshot(VerificationSnapshot snapshot)
    {
        VerificationSnapshot merged = capturedSnapshot == null
            ? snapshot : capturedSnapshot.withAccountEvidenceFrom(snapshot);
        capturedSnapshot = raidKillCounts == null
            ? merged : merged.withRaidKillCounts(raidKillCounts);
        verificationSession.bindRsn(snapshot.getRsn());
        verificationSession.setStatus(EvidenceStage.CHARACTER,
            EvidenceStageStatus.CAPTURED);
        verificationSession.setStatus(EvidenceStage.PRAYERS,
            EvidenceStageStatus.CAPTURED);
        panel.showStageStatus(EvidenceStage.CHARACTER,
            EvidenceStageStatus.CAPTURED);
        panel.showStageStatus(EvidenceStage.PRAYERS,
            EvidenceStageStatus.CAPTURED);
        if (capturedSnapshot.getBoatEvidence().isCaptured())
        {
            verificationSession.setStatus(EvidenceStage.BOAT,
                EvidenceStageStatus.CAPTURED);
            panel.showStageStatus(EvidenceStage.BOAT,
                EvidenceStageStatus.CAPTURED);
        }
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
        if (!counts.isAvailable())
        {
            panel.showFallbackStage(EvidenceStage.RAID_KC);
        }
        renderSnapshot(counts.isAvailable()
            ? "Raid KC fetched." : counts.getDetail());
    }

    private void failStage(EvidenceStage stage, RuntimeException exception)
    {
        verificationSession.setStatus(stage, EvidenceStageStatus.FAILED);
        panel.showStageStatus(stage, EvidenceStageStatus.FAILED);
        panel.showError(exception.getMessage());
        refreshSubmissionAvailability();
    }

    private void renderSnapshot(String status)
    {
        if (capturedSnapshot == null)
        {
            panel.showMessage(status);
            return;
        }
        ProgressionEvaluation progression =
            qualificationService.evaluateProgression(capturedSnapshot);
        panel.showSnapshot(capturedSnapshot, progression, status);
        refreshSubmissionAvailability();
    }

    private void startSession()
    {
        capturedSnapshot = null;
        raidKillCounts = null;
        reviewSubmitted = false;
        verificationSession = new VerificationSession(
            qualificationService.getAllEvidenceStages());
        if (panel != null)
        {
            panel.setRequiredStages(verificationSession.getRequiredStages());
            refreshSubmissionAvailability();
        }
    }

    private boolean isCurrentSession(VerificationSession session)
    {
        return verificationSession == session;
    }

    private void refreshApiDestination()
    {
        if (panel != null)
        {
            panel.showApiDestination(apiDestinationService.describe(
                config.apiBaseUrl()));
            refreshSubmissionAvailability();
        }
    }

    private void submitVerification()
    {
        if (capturedSnapshot == null || verificationSession == null
            || !verificationSession.isReadyForSubmission(
                CORE_SUBMISSION_STAGES))
        {
            panel.showMessage("Capture every required evidence source first.");
            return;
        }
        ProgressionEvaluation progression =
            qualificationService.evaluateProgression(capturedSnapshot);
        panel.setSubmissionBusy();
        transport.submit(capturedSnapshot, progression)
            .thenAccept(result -> SwingUtilities.invokeLater(() ->
            {
                reviewSubmitted = result.isSubmitted();
                panel.showSubmissionResult(result);
                if (!result.isSubmitted())
                {
                    refreshSubmissionAvailability();
                }
            }));
    }

    private void refreshSubmissionAvailability()
    {
        if (panel == null)
        {
            return;
        }
        boolean configured = apiDestinationService.normalize(
            config.apiBaseUrl()) != null
            && config.clanCode() != null
            && !config.clanCode().trim().isEmpty();
        boolean ready = verificationSession != null
            && verificationSession.isReadyForSubmission(
                CORE_SUBMISSION_STAGES)
            && capturedSnapshot != null;
        String reason = !configured
            ? "Configure the ClanHQ API URL and clan code"
            : !ready
                ? "Verify Character and Capture Bank & Gear first"
                : reviewSubmitted
                    ? "A review was already submitted for this session"
                    : "Submit to the ClanHQ promotions feed";
        panel.setSubmissionAvailable(
            configured && ready && !reviewSubmitted,
            reason);
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

    private static final class AutomaticEvidence
    {
        private final VerificationSnapshot snapshot;
        private final RaidKillCounts raidKillCounts;
        private final TempleCollectionLogResult templeCollectionLog;

        private AutomaticEvidence(VerificationSnapshot snapshot,
            RaidKillCounts raidKillCounts,
            TempleCollectionLogResult templeCollectionLog)
        {
            this.snapshot = snapshot;
            this.raidKillCounts = raidKillCounts;
            this.templeCollectionLog = templeCollectionLog;
        }
    }
}
