package com.clanhq.verifier.bingo;

import com.clanhq.verifier.bingo.model.BingoManifest;
import com.clanhq.verifier.bingo.model.RaidProgress;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;

final class BingoPanel extends JPanel
{
    private static final int WRAP_WIDTH = 190;
    private static final Color CARD_COLOR = new Color(39, 39, 43);
    private static final Color TITLE_COLOR = new Color(0x6FA8DC);
    private static final Color SENT_COLOR = new Color(0x6B, 0xBF, 0x6B);
    private static final Color FAILED_COLOR = new Color(0xE0, 0x6C, 0x6C);
    private static final Color SENDING_COLOR = new Color(90, 90, 96);
    private static final Color BUTTON_TEXT_COLOR = new Color(0x24, 0x15, 0x05);

    private final JLabel titleLabel = new JLabel("Bingo Companion");
    private final JLabel eventLabel = new JLabel("No active Bingo");
    private final JLabel participationLabel = new JLabel("Not joined");
    private final JLabel teamLabel = new JLabel("Team: Unassigned");
    private final JLabel trackingLabel = new JLabel("Tracking: Inactive");
    private final JLabel statusLabel = new JLabel();
    private final JPanel userActivityCards = new JPanel();
    private final JTextArea userActivityLog = new JTextArea();
    private final JTextArea clanActivity = new JTextArea();
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton siteButton = new JButton("Bingo site ↗");
    private final JLabel raidProgressHeader = sectionLabel("Raid Progress");
    private final JPanel raidProgressPanel = new JPanel();
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

        raidProgressHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        raidProgressHeader.setVisible(false);
        content.add(raidProgressHeader);
        raidProgressPanel.setLayout(
            new BoxLayout(raidProgressPanel, BoxLayout.Y_AXIS));
        raidProgressPanel.setOpaque(false);
        raidProgressPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        raidProgressPanel.setVisible(false);
        content.add(raidProgressPanel);
        content.add(Box.createRigidArea(new Dimension(0, 7)));

        content.add(sectionLabel("User Activity"));
        userActivityCards.setLayout(new BoxLayout(userActivityCards, BoxLayout.Y_AXIS));
        userActivityCards.setOpaque(false);
        userActivityCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        capHeightToPreferred(userActivityCards);
        content.add(userActivityCards);
        userActivityLog.setEditable(false);
        userActivityLog.setLineWrap(true);
        userActivityLog.setWrapStyleWord(true);
        userActivityLog.setColumns(20);
        userActivityLog.setRows(3);
        userActivityLog.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        userActivityLog.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        userActivityLog.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        userActivityLog.setAlignmentX(Component.LEFT_ALIGNMENT);
        capHeightToPreferred(userActivityLog);
        userActivityLog.setText("No matching drops detected this session.");
        content.add(userActivityLog);
        content.add(Box.createRigidArea(new Dimension(0, 7)));

        content.add(sectionLabel("Clan Activity"));
        clanActivity.setEditable(false);
        clanActivity.setLineWrap(true);
        clanActivity.setWrapStyleWord(true);
        clanActivity.setColumns(20);
        clanActivity.setRows(4);
        clanActivity.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        clanActivity.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        clanActivity.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        clanActivity.setAlignmentX(Component.LEFT_ALIGNMENT);
        capHeightToPreferred(clanActivity);
        clanActivity.setText("No recent clan activity.");
        content.add(clanActivity);
        content.add(Box.createRigidArea(new Dimension(0, 7)));
        content.add(Box.createVerticalGlue());

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

    /**
     * Renders the permanent, clan-wide "CoX - 3/12" style raid-unique
     * progress rows. The whole section stays hidden whenever there is
     * nothing to show - Bingo disabled server-side, or simply no raid
     * items tagged in the current manifest - rather than displaying an
     * empty or zeroed-out section.
     */
    void showRaidProgress(RaidProgress progress)
    {
        raidProgressPanel.removeAll();
        boolean hasRaids = progress != null && !progress.isEmpty();
        raidProgressHeader.setVisible(hasRaids);
        raidProgressPanel.setVisible(hasRaids);
        if (hasRaids)
        {
            for (String raidKey : progress.getRaidKeys())
            {
                RaidProgress.Raid raid = progress.getRaid(raidKey);
                JLabel label = new JLabel(raidDisplayName(raidKey) + " - "
                    + raid.getObtained() + "/" + raid.getTotal());
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                raidProgressPanel.add(label);
                raidProgressPanel.add(Box.createRigidArea(new Dimension(0, 2)));
            }
        }
        revalidate();
        repaint();
    }

    private static String raidDisplayName(String raidKey)
    {
        switch (raidKey)
        {
            case "COX":
                return "CoX";
            case "TOB":
                return "ToB";
            case "TOA":
                return "ToA";
            default:
                return raidKey;
        }
    }

    /**
     * Called when a drop is auto-submitted immediately (Automatic submit
     * mode). Manual mode uses {@link #addPendingUserDrop} instead.
     */
    void showUserActivityDetected(String itemName, int quantity, String source)
    {
        appendTo(userActivityLog, "Detected " + quantity + " × " + itemName
            + " from " + source + "; submitting...", "No matching drops");
    }

    void showUserActivityDelivery(String itemName, boolean successful,
        String message)
    {
        appendTo(userActivityLog, (successful ? "✓ Sent " : "✗ Failed ")
            + itemName + ": " + message, "No matching drops");
    }

    /**
     * Adds a pending-confirmation card to the User Activity section for a
     * drop detected under Manual submit mode. {@code onSend} is invoked
     * with a fresh {@link PendingDropHandle} both the first time the
     * player clicks "Send to ClanHQ" and again if they click "Retry"
     * after a failure.
     */
    void addPendingUserDrop(BufferedImage icon, String itemName, int quantity,
        String source, Instant occurredAt, Consumer<PendingDropHandle> onSend)
    {
        DropCard card = new DropCard(ColorScheme.BRAND_ORANGE);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel(icon == null
            ? placeholderIcon() : new ImageIcon(icon));
        iconLabel.setPreferredSize(new Dimension(30, 30));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(iconLabel, BorderLayout.WEST);

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setOpaque(false);
        main.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nameLabel = new JLabel(itemName);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleRow.add(nameLabel, BorderLayout.CENTER);
        PulsingBadge badge = new PulsingBadge();
        titleRow.add(badge, BorderLayout.EAST);
        main.add(titleRow);

        JLabel metaLabel = new JLabel(quantity + "×  ·  " + source
            + "  ·  " + relativeTime(occurredAt));
        metaLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
        metaLabel.setFont(metaLabel.getFont().deriveFont(10f));
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(Box.createRigidArea(new Dimension(0, 2)));
        main.add(metaLabel);
        main.add(Box.createRigidArea(new Dimension(0, 6)));

        JButton sendButton = new JButton("Send to ClanHQ");
        sendButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        sendButton.setBackground(ColorScheme.BRAND_ORANGE);
        sendButton.setForeground(BUTTON_TEXT_COLOR);
        sendButton.setFocusPainted(false);
        sendButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        main.add(sendButton);

        JPanel resultRow = new JPanel(new BorderLayout(6, 0));
        resultRow.setOpaque(false);
        resultRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultRow.setVisible(false);
        JLabel resultLabel = new JLabel();
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 10.5f));
        resultRow.add(resultLabel, BorderLayout.CENTER);
        main.add(resultRow);

        card.add(main, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            card.getPreferredSize().height));

        PendingDropHandle[] handleBox = new PendingDropHandle[1];
        handleBox[0] = new PendingDropHandle()
        {
            @Override
            public void markSending()
            {
                badge.stopPulsing();
                badge.setVisible(false);
                sendButton.setEnabled(false);
                sendButton.setText("Sending…");
                sendButton.setBackground(SENDING_COLOR);
                sendButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            }

            @Override
            public void markResult(boolean successful, String message)
            {
                badge.stopPulsing();
                badge.setVisible(false);
                sendButton.setVisible(false);
                card.setAccent(successful ? SENT_COLOR : FAILED_COLOR);
                resultLabel.setForeground(successful ? SENT_COLOR : FAILED_COLOR);
                resultLabel.setText(successful
                    ? "✓ Sent to ClanHQ" : "✗ " + message);
                resultRow.removeAll();
                resultRow.add(resultLabel, BorderLayout.CENTER);
                if (!successful)
                {
                    JButton retry = new JButton("Retry");
                    retry.setForeground(FAILED_COLOR);
                    retry.setFocusPainted(false);
                    retry.addActionListener(event ->
                    {
                        retry.setEnabled(false);
                        resultLabel.setText("Retrying…");
                        onSend.accept(handleBox[0]);
                    });
                    resultRow.add(retry, BorderLayout.EAST);
                }
                resultRow.setVisible(true);
                main.revalidate();
                main.repaint();
                capHeightToPreferred(card);
                capHeightToPreferred(userActivityCards);
                userActivityCards.revalidate();
            }
        };
        sendButton.addActionListener(event -> onSend.accept(handleBox[0]));

        userActivityCards.add(card, 0);
        userActivityCards.add(Box.createRigidArea(new Dimension(0, 6)), 1);
        capHeightToPreferred(userActivityCards);
        userActivityCards.revalidate();
        userActivityCards.repaint();
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

    private static void appendTo(JTextArea area, String message,
        String placeholderPrefix)
    {
        String existing = area.getText();
        if (existing.startsWith(placeholderPrefix))
        {
            existing = "";
        }
        area.setText(message + (existing.isEmpty() ? "" : "\n\n" + existing));
        area.setCaretPosition(0);
    }

    private void showStatus(String message)
    {
        statusLabel.setText("<html><body style='width: " + WRAP_WIDTH
            + "px'>" + escapeHtml(message) + "</body></html>");
    }

    private static String relativeTime(Instant occurredAt)
    {
        long seconds = Duration.between(occurredAt, Instant.now()).getSeconds();
        if (seconds < 45)
        {
            return "just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60)
        {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        return hours + "h ago";
    }

    private static Icon placeholderIcon()
    {
        BufferedImage image = new BufferedImage(26, 26,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(70, 70, 78));
        graphics.fillRoundRect(0, 0, 26, 26, 6, 6);
        graphics.dispose();
        return new ImageIcon(image);
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

    private static void capHeightToPreferred(JComponent component)
    {
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            component.getPreferredSize().height));
    }

    private static JPanel verticalPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    /**
     * Given to the click handler passed into {@link #addPendingUserDrop},
     * so the caller can report progress back onto the same card without
     * the panel needing to track pending drops by id.
     */
    interface PendingDropHandle
    {
        void markSending();

        void markResult(boolean successful, String message);
    }

    /**
     * A rounded card with a colored left accent stripe, used for pending
     * Bingo drops awaiting manual confirmation.
     */
    private static final class DropCard extends JPanel
    {
        private Color accent;

        private DropCard(Color accent)
        {
            this.accent = accent;
            setOpaque(false);
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 9));
        }

        void setAccent(Color accent)
        {
            this.accent = accent;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(CARD_COLOR);
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            graphics.setColor(accent);
            graphics.fillRoundRect(0, 0, 4, getHeight(), 8, 8);
            graphics.setColor(CARD_COLOR);
            graphics.fillRect(4, 0, 4, getHeight());
            graphics.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * A small "NEW" chip that gently pulses while a drop is awaiting
     * confirmation, so a freshly detected item catches the eye. Stops
     * pulsing (and hides) once the player acts on it.
     */
    private static final class PulsingBadge extends JLabel
    {
        private final Timer timer;
        private float phase;

        private PulsingBadge()
        {
            super("NEW");
            setOpaque(true);
            setBackground(ColorScheme.BRAND_ORANGE);
            setForeground(BUTTON_TEXT_COLOR);
            setFont(getFont().deriveFont(Font.BOLD, 9f));
            setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
            timer = new Timer(45, event ->
            {
                phase += 0.09f;
                float pulse = (float) (0.5 + 0.5 * Math.sin(phase));
                setBackground(blend(ColorScheme.BRAND_ORANGE, CARD_COLOR,
                    pulse * 0.4f));
                repaint();
            });
            timer.start();
        }

        void stopPulsing()
        {
            timer.stop();
        }

        private static Color blend(Color from, Color to, float t)
        {
            int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * t);
            int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t);
            int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t);
            return new Color(r, g, b);
        }
    }
}
