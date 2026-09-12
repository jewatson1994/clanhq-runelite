package com.clanhq.verifier.bingo;

import com.clanhq.verifier.bingo.model.BingoManifest;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.net.URI;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;

final class BingoPanel extends JPanel
{
    private static final int WRAP_WIDTH = 190;
    private static final Color CARD_COLOR = new Color(39, 39, 43);
    private static final Color TITLE_COLOR = new Color(0x6FA8DC);
    private final JLabel titleLabel = new JLabel("Bingo Companion");
    private final JLabel eventLabel = new JLabel("No active Bingo");
    private final JLabel participationLabel = new JLabel("Not joined");
    private final JLabel teamLabel = new JLabel("Team: Unassigned");
    private final JLabel trackingLabel = new JLabel("Tracking: Inactive");
    private final JLabel statusLabel = new JLabel();
    private final JTextArea activity = new JTextArea();
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton siteButton = new JButton("Bingo site ↗");
    private String siteUrl;

    BingoPanel(Runnable refreshAction)
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = verticalPanel();
        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(TITLE_COLOR);
        titleRow.add(titleLabel, BorderLayout.CENTER);
        siteButton.setMargin(new java.awt.Insets(3, 6, 3, 6));
        siteButton.addActionListener(event -> openSite());
        titleRow.add(siteButton, BorderLayout.EAST);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            titleRow.getPreferredSize().height));
        header.add(titleRow);
        header.add(Box.createRigidArea(new Dimension(0, 5)));

        eventLabel.setFont(eventLabel.getFont().deriveFont(Font.BOLD));
        eventLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(eventLabel);
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(statusLabel);

        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setBackground(CARD_COLOR);
        summary.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        participationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        teamLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        trackingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.add(participationLabel);
        summary.add(Box.createRigidArea(new Dimension(0, 2)));
        summary.add(trackingLabel);
        summary.add(Box.createRigidArea(new Dimension(0, 2)));
        summary.add(teamLabel);
        summary.add(Box.createRigidArea(new Dimension(0, 4)));
        refreshButton.addActionListener(event -> refreshAction.run());
        refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.add(refreshButton);
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            summary.getPreferredSize().height));

        JPanel content = verticalPanel();
        content.add(summary);
        content.add(Box.createRigidArea(new Dimension(0, 7)));
        content.add(sectionLabel("Recent Activity"));
        activity.setEditable(false);
        activity.setLineWrap(true);
        activity.setWrapStyleWord(true);
        activity.setColumns(20);
        activity.setRows(4);
        activity.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        activity.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        activity.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        activity.setAlignmentX(Component.LEFT_ALIGNMENT);
        activity.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            Integer.MAX_VALUE));
        activity.setText("No matching drops detected this session.");
        content.add(activity);
        content.add(Box.createRigidArea(new Dimension(0, 7)));

        JScrollPane scroll = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        showStatus("Load the active Bingo event to begin.");
    }

    void setLoading()
    {
        refreshButton.setEnabled(false);
        showStatus("Loading the active Bingo event...");
    }

    void showManifest(BingoManifest manifest)
    {
        refreshButton.setEnabled(true);
        siteUrl = manifest.getSiteUrl();
        titleLabel.setText("Bingo Companion");
        eventLabel.setText("Bingo Status");
        participationLabel.setText(manifest.isJoined() ? "Joined" : "Not joined");
        teamLabel.setText("Team: "
            + (manifest.getTeamName() == null
                ? "Unassigned" : manifest.getTeamName()));
        trackingLabel.setText("Tracking: Active");
        showStatus("Ready to track eligible Bingo drops.");
    }

    void showManifestError(String message, String responseSiteUrl)
    {
        refreshButton.setEnabled(true);
        siteUrl = responseSiteUrl;
        eventLabel.setText("No active Bingo");
        participationLabel.setText("Not joined");
        teamLabel.setText("Team: Unassigned");
        trackingLabel.setText("Tracking: Inactive");
        showStatus(message);
    }

    void showParticipation(boolean joined, String teamName, String message)
    {
        participationLabel.setText(joined ? "Joined" : "Not joined");
        teamLabel.setText("Team: "
            + (teamName == null ? "Unassigned" : teamName));
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

    void setCharacterSubmitting()
    {
        showStatus("Capturing and submitting the character snapshot...");
    }

    void showCharacterSubmission(boolean successful, String message)
    {
        showStatus((successful ? "Character submitted. " : "Submission failed. ")
            + message);
    }

    void resetCharacterSubmission()
    {
    }

    void showCharacterSubmissionCancelled()
    {
        showStatus("Character submission cancelled.");
    }

    private void openSite()
    {
        String configured = siteUrl;
        try
        {
            URI uri = URI.create(configured == null ? "" : configured.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                || !("https".equalsIgnoreCase(scheme)
                    || "http".equalsIgnoreCase(scheme)))
            {
                throw new IllegalArgumentException();
            }
            LinkBrowser.browse(uri.toString());
            showStatus("Opened the Bingo website.");
        }
        catch (IllegalArgumentException exception)
        {
            showStatus("ClanHQ did not provide a valid Bingo site URL.");
        }
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

    private static JLabel sectionLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setForeground(ColorScheme.BRAND_ORANGE);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
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
