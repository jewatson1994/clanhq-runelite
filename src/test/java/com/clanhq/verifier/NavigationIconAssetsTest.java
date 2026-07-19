package com.clanhq.verifier;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NavigationIconAssetsTest
{
    @Test
    public void packagesTransparentTwentyPixelNavigationIcons()
        throws IOException
    {
        for (String name : new String[] {
            "overview", "character", "events", "bingo", "dailies"
        })
        {
            URL resource = getClass().getResource(
                "/com/clanhq/verifier/icons/" + name + ".png");
            assertNotNull(name, resource);
            BufferedImage image = ImageIO.read(resource);
            assertEquals(name, 20, image.getWidth());
            assertEquals(name, 20, image.getHeight());
            assertTrue(name, image.getColorModel().hasAlpha());
            assertEquals(name, 0, image.getRGB(0, 0) >>> 24);
        }
    }
}
