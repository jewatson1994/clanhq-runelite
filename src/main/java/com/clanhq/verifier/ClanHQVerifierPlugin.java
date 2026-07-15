package com.clanhq.verifier;

import com.clanhq.verifier.model.VerificationSnapshot;
import com.clanhq.verifier.service.LocalPlayerSnapshotService;
import com.clanhq.verifier.transport.PreviewOnlyVerificationTransport;
import com.clanhq.verifier.transport.VerificationTransport;
import com.clanhq.verifier.transport.VerificationTransportResult;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
    name = "ClanHQ Verifier",
    description = "Preview account evidence for ClanHQ rank verification",
    tags = {"clan", "gear", "rank", "verification"})
public final class ClanHQVerifierPlugin extends Plugin
{
    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private LocalPlayerSnapshotService snapshotService;

    @Inject
    private VerificationTransport transport;

    private ClanHQVerifierPanel panel;
    private NavigationButton navigationButton;

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
        panel = new ClanHQVerifierPanel(this::captureCurrentCharacter);
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
        clientToolbar.removeNavigation(navigationButton);
        navigationButton = null;
        panel = null;
    }

    private void captureCurrentCharacter()
    {
        panel.setBusy();

        clientThread.invokeLater(() ->
        {
            try
            {
                VerificationSnapshot snapshot = snapshotService.capture();
                VerificationTransportResult result = transport.submit(snapshot);

                SwingUtilities.invokeLater(() -> panel.showSnapshot(
                    snapshot,
                    result.getMessage()));
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() -> panel.showError(
                    exception.getMessage()));
            }
        });
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
