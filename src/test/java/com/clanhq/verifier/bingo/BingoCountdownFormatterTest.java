package com.clanhq.verifier.bingo;

import java.time.Duration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class BingoCountdownFormatterTest
{
    @Test
    public void formatsDaysAndHours()
    {
        assertEquals("2d 5h",
            BingoCountdownFormatter.format(Duration.ofHours(53)));
    }

    @Test
    public void formatsHoursAndMinutes()
    {
        assertEquals("3h 9m",
            BingoCountdownFormatter.format(Duration.ofMinutes(189)));
    }

    @Test
    public void formatsMinutesAndSeconds()
    {
        assertEquals("4m 5s",
            BingoCountdownFormatter.format(Duration.ofSeconds(245)));
    }

    @Test
    public void formatsSecondsOnlyUnderOneMinute()
    {
        assertEquals("30s", BingoCountdownFormatter.format(Duration.ofSeconds(30)));
    }

    @Test
    public void treatsPastStartTimeAsZero()
    {
        assertEquals("0s", BingoCountdownFormatter.format(Duration.ofSeconds(-120)));
    }
}
