package com.clanhq.verifier;

import com.clanhq.verifier.feature.ClanHQFeature;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.imageio.ImageIO;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/** Top-level navigation for independent ClanHQ feature panels. */
final class ClanHQPanel extends PluginPanel
{
    private static final int ICON_SIZE = 20;
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final Map<String, JButton> navigation = new LinkedHashMap<>();

    ClanHQPanel(List<ClanHQFeature> features)
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("ClanHQ");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, Math.max(1, features.size()), 4, 0));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (ClanHQFeature feature : features)
        {
            JButton button = new JButton(loadIcon(
                feature.getNavigationIconResource()));
            button.setPreferredSize(new Dimension(36, 32));
            button.setToolTipText(feature.getDisplayName() + " — "
                + feature.getDescription());
            button.getAccessibleContext().setAccessibleName(
                feature.getDisplayName());
            button.getAccessibleContext().setAccessibleDescription(
                feature.getDescription());
            button.setFocusable(false);
            applyInactiveBorder(button);
            button.addActionListener(event -> showFeature(feature.getId()));
            navigation.put(feature.getId(), button);
            buttons.add(button);
            cardPanel.add(feature.getPanel(), feature.getId());
        }

        header.add(buttons);
        cardPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(header, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        if (!features.isEmpty())
        {
            showFeature(features.get(0).getId());
        }
        else
        {
            JLabel empty = new JLabel(
                "<html>Enable a ClanHQ feature in the plugin settings.</html>");
            empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            cardPanel.add(empty, "empty");
            cards.show(cardPanel, "empty");
        }
    }

    private void showFeature(String featureId)
    {
        cards.show(cardPanel, featureId);
        navigation.forEach((id, button) ->
        {
            button.setEnabled(true);
            if (id.equals(featureId))
            {
                applyActiveBorder(button);
            }
            else
            {
                applyInactiveBorder(button);
            }
        });
    }

    private static ImageIcon loadIcon(String resource)
    {
        URL url = ClanHQPanel.class.getResource(resource);
        if (url == null)
        {
            throw new IllegalStateException(
                "Missing ClanHQ navigation icon: " + resource);
        }
        try
        {
            BufferedImage source = ImageIO.read(url);
            BufferedImage scaled = new BufferedImage(
                ICON_SIZE,
                ICON_SIZE,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(source, 0, 0, ICON_SIZE, ICON_SIZE, null);
            graphics.dispose();
            return new ImageIcon(scaled);
        }
        catch (IOException error)
        {
            throw new IllegalStateException(
                "Could not load ClanHQ navigation icon: " + resource,
                error);
        }
    }

    private static void applyActiveBorder(JButton button)
    {
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                0, 0, 2, 0, ColorScheme.BRAND_ORANGE),
            BorderFactory.createEmptyBorder(2, 4, 0, 4)));
    }

    private static void applyInactiveBorder(JButton button)
    {
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, TRANSPARENT),
            BorderFactory.createEmptyBorder(2, 4, 0, 4)));
    }
}
