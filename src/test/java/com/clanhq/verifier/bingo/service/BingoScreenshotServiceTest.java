package com.clanhq.verifier.bingo.service;

import com.clanhq.verifier.bingo.model.BingoDrop;
import com.clanhq.verifier.bingo.model.BingoItem;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BingoScreenshotServiceTest
{
    @Test
    public void scalesWatermarksAndEncodesScreenshot() throws Exception
    {
        BufferedImage source = new BufferedImage(
            1600,
            900,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        BingoDrop drop = new BingoDrop(
            "BINGO-ABC123",
            "Mr Dimples",
            new BingoItem(4151, "Abyssal whip", 1, 5),
            1,
            "EVENT",
            "Chambers of Xeric",
            Instant.parse("2026-08-02T03:04:05Z"));
        BingoScreenshotService service = new BingoScreenshotService(null, null);

        byte[] encoded = service.render(source, drop, "Summer Bingo");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(encoded));

        assertTrue(encoded.length > 1000);
        assertEquals((byte) 0xff, encoded[0]);
        assertEquals((byte) 0xd8, encoded[1]);
        assertNotNull(decoded);
        assertEquals(1280, decoded.getWidth());
        assertEquals(720, decoded.getHeight());
    }
}
