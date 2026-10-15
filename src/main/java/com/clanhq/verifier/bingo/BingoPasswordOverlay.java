package com.clanhq.verifier.bingo;

import com.clanhq.verifier.bingo.model.BingoManifest;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Shows the active/scheduled ClanHQ Bingo event above the player's head:
 * the event name, a countdown when it hasn't started yet, the player's own
 * RSN and ClanHQ team (both automatic - never typed in), and the event's
 * passwords. The Event Password is always whatever the active manifest
 * carries; the Challenge Password falls back to a local config value when
 * the clan hasn't set one centrally - see {@link BingoPasswordResolver}.
 * That Challenge Password field is the only thing left for a player to
 * configure here.
 */
public final class BingoPasswordOverlay extends OverlayPanel
{
    private static final Color NAME_COLOR = Color.WHITE;
    private static final Color COUNTDOWN_COLOR = new Color(0xFFB347);
    private static final Color IDENTITY_COLOR = Color.LIGHT_GRAY;

    private final Supplier<BingoManifest> manifestSupplier;
    private final Supplier<String> rsnSupplier;
    private final Supplier<String> localChallengePasswordSupplier;
    private final Supplier<Color> eventPasswordColorSupplier;
    private final Supplier<Color> challengePasswordColorSupplier;

    public BingoPasswordOverlay(
        Supplier<BingoManifest> manifestSupplier,
        Supplier<String> rsnSupplier,
        Supplier<String> localChallengePasswordSupplier,
        Supplier<Color> eventPasswordColorSupplier,
        Supplier<Color> challengePasswordColorSupplier)
    {
        this.manifestSupplier = manifestSupplier;
        this.rsnSupplier = rsnSupplier;
        this.localChallengePasswordSupplier = localChallengePasswordSupplier;
        this.eventPasswordColorSupplier = eventPasswordColorSupplier;
        this.challengePasswordColorSupplier = challengePasswordColorSupplier;
        setPosition(OverlayPosition.TOP_LEFT);
        setMovable(true);
        setResizable(true);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        BingoManifest manifest = manifestSupplier.get();
        PanelComponent panel = getPanelComponent();
        panel.setGap(new Point(0, 2));

        if (manifest == null)
        {
            // No active or scheduled event is known to the client, but a
            // player may still have a Challenge Password set locally (for
            // example a clan running challenges outside a tracked Bingo
            // event). Show just that instead of hiding the overlay
            // entirely - it used to always render from local config alone.
            String localChallengePassword = localChallengePasswordSupplier.get();
            if (localChallengePassword == null || localChallengePassword.trim().isEmpty())
            {
                return null;
            }
            panel.getChildren().add(LineComponent.builder()
                .left(localChallengePassword.trim())
                .leftColor(challengePasswordColorSupplier.get())
                .build());
            return super.render(graphics);
        }

        String eventPassword = manifest.getEventPassword();
        String challengePassword = BingoPasswordResolver.resolve(
            localChallengePasswordSupplier.get(), manifest.getChallengePassword());
        String rsn = rsnSupplier.get();
        String teamName = manifest.getTeamName();
        Instant now = Instant.now();
        boolean scheduled = now.isBefore(manifest.getStartsAt());

        panel.getChildren().add(LineComponent.builder()
            .left(manifest.getName())
            .leftColor(NAME_COLOR)
            .build());

        if (scheduled)
        {
            panel.getChildren().add(LineComponent.builder()
                .left("Starts in " + BingoCountdownFormatter.format(
                    Duration.between(now, manifest.getStartsAt())))
                .leftColor(COUNTDOWN_COLOR)
                .build());
        }

        if (rsn != null && !rsn.trim().isEmpty())
        {
            panel.getChildren().add(LineComponent.builder()
                .left("RSN: " + rsn.trim())
                .leftColor(IDENTITY_COLOR)
                .build());
        }
        // Always shown, even with no team yet, so a player who hasn't
        // joined a team still sees that the event is tracking them rather
        // than the line silently vanishing.
        panel.getChildren().add(LineComponent.builder()
            .left("Team: " + (teamName != null && !teamName.trim().isEmpty()
                ? teamName.trim() : "No team yet"))
            .leftColor(IDENTITY_COLOR)
            .build());
        if (eventPassword != null && !eventPassword.trim().isEmpty())
        {
            panel.getChildren().add(LineComponent.builder()
                .left(eventPassword.trim())
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
        return super.render(graphics);
    }
}
