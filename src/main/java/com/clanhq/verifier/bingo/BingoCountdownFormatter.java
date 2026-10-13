package com.clanhq.verifier.bingo;

import java.time.Duration;

/**
 * Formats the time remaining until a scheduled Bingo event starts, for the
 * overlay's "Starts in ..." line. Pulled out of
 * {@link BingoPasswordOverlay} as its own class so it can be unit tested
 * without needing to render an {@code OverlayPanel}.
 */
public final class BingoCountdownFormatter
{
    private BingoCountdownFormatter()
    {
    }

    /**
     * @param remaining time until the event starts; a negative or zero
     *     duration (the start time has passed) is treated as zero
     * @return a short "Xd Xh" / "Xh Xm" / "Xm Xs" / "Xs" string, coarsest
     *     two units only, so the overlay line stays compact
     */
    public static String format(Duration remaining)
    {
        long totalSeconds = Math.max(0, remaining.getSeconds());
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (days > 0)
        {
            return days + "d " + hours + "h";
        }
        if (hours > 0)
        {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0)
        {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
