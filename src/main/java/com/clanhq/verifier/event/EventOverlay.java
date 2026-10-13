package com.clanhq.verifier.event;

import com.clanhq.verifier.event.model.ClanEventSummary;
import com.clanhq.verifier.event.model.EventStanding;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Compact in-game standings for active ClanHQ events, mirroring the
 * "Weekly Competitions" leaderboard ClanHQ already posts to Discord.
 *
 * Only events the server has attached standings to are shown at all -
 * see {@link ClanEventSummary#getStandings()}. Today that means active
 * Skill/Boss of the Week competitions with a settled Wise Old Man
 * snapshot; anything else (Bingo, Community events, monthly competitions,
 * or a weekly one whose WOM snapshot hasn't landed yet) is simply left
 * out rather than shown with a permanent "pending" placeholder, since the
 * client has no reliable way to tell "never coming" apart from "not yet."
 *
 * Two independent settings control what's drawn per event: the top-3
 * {@code showLeaderboard} and the viewer's own {@code showMyRank} line
 * (server-attached via {@link ClanEventSummary#getMyStanding()}). "My
 * rank" is only drawn as its own line when it isn't already visible in
 * the top 3, so turning both settings on for a top-3 player doesn't show
 * their name twice.
 */
public final class EventOverlay extends OverlayPanel
{
    private static final NumberFormat SCORE_FORMAT =
        NumberFormat.getIntegerInstance(Locale.US);
    private static final Color HEADER_COLOR = new Color(0xFF, 0xB8, 0x33);
    private static final Color RANK_COLOR = Color.WHITE;
    private static final Color SCORE_COLOR = new Color(198, 198, 198);
    private static final Color MY_RANK_COLOR = new Color(0x70, 0xC0, 0x90);
    private static final int MAX_STANDINGS_SHOWN = 3;

    private final Supplier<List<ClanEventSummary>> eventsSupplier;
    private final BooleanSupplier showLeaderboard;
    private final BooleanSupplier showMyRank;

    public EventOverlay(Supplier<List<ClanEventSummary>> eventsSupplier,
        BooleanSupplier showLeaderboard, BooleanSupplier showMyRank)
    {
        this.eventsSupplier = eventsSupplier;
        this.showLeaderboard = showLeaderboard;
        this.showMyRank = showMyRank;
        setPosition(OverlayPosition.TOP_LEFT);
        setMovable(true);
        setResizable(true);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        List<ClanEventSummary> events = eventsSupplier.get();
        if (events == null || events.isEmpty())
        {
            return null;
        }
        boolean leaderboardOn = showLeaderboard.getAsBoolean();
        boolean myRankOn = showMyRank.getAsBoolean();
        if (!leaderboardOn && !myRankOn)
        {
            return null;
        }

        PanelComponent panel = getPanelComponent();
        panel.setGap(new Point(0, 2));
        boolean any = false;
        for (ClanEventSummary event : events)
        {
            List<EventStanding> standings = event.getStandings();
            EventStanding myStanding = event.getMyStanding();
            boolean hasLeaderboard = leaderboardOn && !standings.isEmpty();
            boolean hasMyRank = myRankOn && myStanding != null;
            if (!event.isActive() || (!hasLeaderboard && !hasMyRank))
            {
                continue;
            }
            if (any)
            {
                panel.getChildren().add(LineComponent.builder().left(" ").build());
            }
            any = true;
            panel.getChildren().add(LineComponent.builder()
                .left(event.getName() + (event.getTarget() == null ? ""
                    : ": " + event.getTarget()))
                .leftColor(HEADER_COLOR)
                .build());
            int shown = 0;
            if (hasLeaderboard)
            {
                for (EventStanding standing : standings)
                {
                    if (shown >= MAX_STANDINGS_SHOWN)
                    {
                        break;
                    }
                    shown++;
                    panel.getChildren().add(LineComponent.builder()
                        .left(shown + ". " + standing.getRsn())
                        .leftColor(RANK_COLOR)
                        .right(SCORE_FORMAT.format(standing.getScore()))
                        .rightColor(SCORE_COLOR)
                        .build());
                }
            }
            if (hasMyRank && (!hasLeaderboard || myStanding.getRank() > shown
                || myStanding.getRank() <= 0))
            {
                panel.getChildren().add(LineComponent.builder()
                    .left("You (" + (myStanding.getRank() > 0
                        ? "#" + myStanding.getRank() : "?") + ")")
                    .leftColor(MY_RANK_COLOR)
                    .right(SCORE_FORMAT.format(myStanding.getScore()))
                    .rightColor(SCORE_COLOR)
                    .build());
            }
        }
        if (!any)
        {
            return null;
        }
        return super.render(graphics);
    }
}
