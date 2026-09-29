package com.clanhq.verifier.bingo;

import com.clanhq.verifier.bingo.model.BingoManifest;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Shows the active Bingo event's password lines, mirroring the standalone
 * Clan Events plugin: each player types their own Event/Challenge password
 * into the local config below, and those values show here. When the active
 * ClanHQ Bingo manifest carries a clan-wide password, it overrides the
 * local value for that line - see {@link BingoPasswordResolver}.
 */
public final class BingoPasswordOverlay extends OverlayPanel
{
    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'");

    private final Supplier<BingoManifest> manifestSupplier;
    private final Supplier<String> localEventPasswordSupplier;
    private final Supplier<String> localChallengePasswordSupplier;
    private final Supplier<Color> eventPasswordColorSupplier;
    private final Supplier<Color> challengePasswordColorSupplier;
    private final BooleanSupplier dateTimeEnabledSupplier;

    public BingoPasswordOverlay(
        Supplier<BingoManifest> manifestSupplier,
        Supplier<String> localEventPasswordSupplier,
        Supplier<String> localChallengePasswordSupplier,
        Supplier<Color> eventPasswordColorSupplier,
        Supplier<Color> challengePasswordColorSupplier,
        BooleanSupplier dateTimeEnabledSupplier)
    {
        this.manifestSupplier = manifestSupplier;
        this.localEventPasswordSupplier = localEventPasswordSupplier;
        this.localChallengePasswordSupplier = localChallengePasswordSupplier;
        this.eventPasswordColorSupplier = eventPasswordColorSupplier;
        this.challengePasswordColorSupplier = challengePasswordColorSupplier;
        this.dateTimeEnabledSupplier = dateTimeEnabledSupplier;
        setPosition(OverlayPosition.TOP_LEFT);
        setMovable(true);
        setResizable(true);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        BingoManifest manifest = manifestSupplier.get();
        String eventPassword = BingoPasswordResolver.resolve(
            localEventPasswordSupplier.get(),
            manifest == null ? null : manifest.getEventPassword());
        String challengePassword = BingoPasswordResolver.resolve(
            localChallengePasswordSupplier.get(),
            manifest == null ? null : manifest.getChallengePassword());
        boolean showDateTime = dateTimeEnabledSupplier.getAsBoolean();

        if (eventPassword.isEmpty() && challengePassword.isEmpty()
            && !showDateTime)
        {
            return null;
        }

        PanelComponent panel = getPanelComponent();
        panel.setGap(new Point(0, 2));

        if (!eventPassword.isEmpty())
        {
            panel.getChildren().add(LineComponent.builder()
                .left(eventPassword)
                .leftColor(eventPasswordColorSupplier.get())
                .build());
        }
        if (!challengePassword.isEmpty())
        {
            panel.getChildren().add(LineComponent.builder()
                .left(challengePassword)
                .leftColor(challengePasswordColorSupplier.get())
                .build());
        }
        if (showDateTime)
        {
            panel.getChildren().add(LineComponent.builder()
                .left(DATE_TIME_FORMAT.format(
                    java.time.Instant.now().atZone(ZoneOffset.UTC)))
                .leftColor(Color.WHITE)
                .build());
        }
        return super.render(graphics);
    }
}
