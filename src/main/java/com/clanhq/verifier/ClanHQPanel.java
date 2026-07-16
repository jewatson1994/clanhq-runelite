package com.clanhq.verifier;

import com.clanhq.verifier.feature.ClanHQFeature;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/** Top-level navigation for independent ClanHQ feature panels. */
final class ClanHQPanel extends PluginPanel
{
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
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (ClanHQFeature feature : features)
        {
            JButton button = new JButton(feature.getDisplayName());
            button.setToolTipText(feature.getDescription());
            button.addActionListener(event -> showFeature(feature.getId()));
            navigation.put(feature.getId(), button);
            buttons.add(button);
            buttons.add(Box.createRigidArea(new Dimension(4, 0)));
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
            button.setEnabled(!id.equals(featureId)));
    }
}
