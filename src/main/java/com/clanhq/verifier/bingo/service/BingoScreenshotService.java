package com.clanhq.verifier.bingo.service;

import com.clanhq.verifier.bingo.model.BingoDrop;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import net.runelite.client.ui.DrawManager;

public final class BingoScreenshotService
{
    private static final int MAXIMUM_WIDTH = 1280;
    private static final int WATERMARK_HEIGHT = 112;
    private static final float JPEG_QUALITY = 0.82f;
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);
    private final DrawManager drawManager;
    private final ScheduledExecutorService executor;

    public BingoScreenshotService(
        DrawManager drawManager,
        ScheduledExecutorService executor)
    {
        this.drawManager = drawManager;
        this.executor = executor;
    }

    public CompletableFuture<byte[]> capture(
        BingoDrop drop,
        String eventName)
    {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        drawManager.requestNextFrameListener(image -> executor.execute(() ->
        {
            try
            {
                future.complete(render(image, drop, eventName));
            }
            catch (RuntimeException | IOException exception)
            {
                future.completeExceptionally(exception);
            }
        }));
        return future;
    }

    byte[] render(Image image, BingoDrop drop, String eventName)
        throws IOException
    {
        int sourceWidth = image.getWidth(null);
        int sourceHeight = image.getHeight(null);
        if (sourceWidth <= 0 || sourceHeight <= 0)
        {
            throw new IOException("RuneLite screenshot was unavailable");
        }
        double scale = Math.min(1.0, (double) MAXIMUM_WIDTH / sourceWidth);
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        BufferedImage screenshot = new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = screenshot.createGraphics();
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, width, height, null);
        addWatermark(graphics, width, height, drop, eventName);
        graphics.dispose();
        return encodeJpeg(screenshot);
    }

    private static void addWatermark(
        Graphics2D graphics,
        int width,
        int height,
        BingoDrop drop,
        String eventName)
    {
        int top = Math.max(0, height - WATERMARK_HEIGHT);
        graphics.setComposite(AlphaComposite.SrcOver.derive(0.78f));
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, top, width, WATERMARK_HEIGHT);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        graphics.setColor(new Color(212, 175, 55));
        graphics.drawString("ClanHQ • " + eventName, 16, top + 24);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        graphics.setColor(Color.WHITE);
        graphics.drawString("RSN: " + drop.getRsn(), 16, top + 48);
        graphics.drawString(
            "Drop: " + drop.getItem().getName() + " × " + drop.getQuantity(),
            16,
            top + 70);
        graphics.drawString(
            "UTC: " + TIMESTAMP_FORMAT.format(drop.getOccurredAt()),
            16,
            top + 92);
        graphics.drawString(
            "Event: " + drop.getEventId(),
            Math.max(16, width - 260),
            top + 92);
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException
    {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext())
        {
            throw new IOException("JPEG encoder is unavailable");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam parameters = writer.getDefaultWriteParam();
        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parameters.setCompressionQuality(JPEG_QUALITY);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output))
        {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(image, null, null), parameters);
            imageOutput.flush();
            return output.toByteArray();
        }
        finally
        {
            writer.dispose();
        }
    }
}
