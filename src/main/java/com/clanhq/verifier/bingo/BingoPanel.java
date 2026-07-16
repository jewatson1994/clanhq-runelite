package com.clanhq.verifier.bingo;

import com.clanhq.verifier.bingo.model.BingoManifest;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;

final class BingoPanel extends JPanel
{
    private static final int WRAP_WIDTH = 190;
    private final JLabel eventLabel = new JLabel("Event: Not loaded");
    private final JLabel itemCountLabel = new JLabel("Eligible drops: 0");
    private final JLabel statusLabel = new JLabel();
    private final JTextArea activity = new JTextArea();
    private final JButton refreshButton = new JButton("Refresh Bingo Board");

    BingoPanel(Runnable refreshAction)
    {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = verticalPanel();
        header.add(new JLabel("ClanHQ Bingo"));
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        header.add(new JLabel("<html>Eligible RuneLite Loot Tracker drops are "
            + "sent automatically.</html>"));
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        header.add(eventLabel);
        header.add(itemCountLabel);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        refreshButton.addActionListener(event -> refreshAction.run());
        header.add(refreshButton);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        header.add(statusLabel);

        activity.setEditable(false);
        activity.setLineWrap(true);
        activity.setWrapStyleWord(true);
        activity.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        activity.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        activity.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        activity.setText("No matching drops detected this session.");

        JScrollPane scroll = new JScrollPane(activity,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        showStatus("Load the active board to begin.");
    }

    void setLoading()
    {
        refreshButton.setEnabled(false);
        showStatus("Loading the active Bingo board...");
    }

    void showManifest(BingoManifest manifest)
    {
        refreshButton.setEnabled(true);
        eventLabel.setText("Event: " + manifest.getName());
        itemCountLabel.setText("Eligible drops: " + manifest.getItems().size());
        showStatus("Tracking eligible loot and reward events.");
    }

    void showManifestError(String message)
    {
        refreshButton.setEnabled(true);
        eventLabel.setText("Event: Not loaded");
        itemCountLabel.setText("Eligible drops: 0");
        showStatus(message);
    }

    void showDetected(String itemName, int quantity, String source)
    {
        append("Detected " + quantity + " × " + itemName + " from "
            + source + "; submitting...");
    }

    void showDelivery(String itemName, boolean successful, String message)
    {
        append((successful ? "✓ Sent " : "✗ Failed ") + itemName
            + ": " + message);
    }

    private void append(String message)
    {
        String existing = activity.getText();
        if (existing.startsWith("No matching drops"))
        {
            existing = "";
        }
        activity.setText(message + (existing.isEmpty() ? "" : "\n\n" + existing));
        activity.setCaretPosition(0);
    }

    private void showStatus(String message)
    {
        statusLabel.setText("<html><body style='width: " + WRAP_WIDTH
            + "px'>" + escapeHtml(message) + "</body></html>");
    }

    private static String escapeHtml(String text)
    {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static JPanel verticalPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }
}
