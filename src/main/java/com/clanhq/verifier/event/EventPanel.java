package com.clanhq.verifier.event;

import com.clanhq.verifier.event.model.ClanEventSummary;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.ColorScheme;

final class EventPanel extends JPanel
{
    private static final int WRAP_WIDTH = 190;
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("MMM d, yyyy '@' h:mm a", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    private static final Color ACTIVE_COLOR = new Color(112, 194, 130);
    private static final Color SCHEDULED_COLOR = new Color(111, 166, 224);
    private static final Color BINGO_COLOR = new Color(178, 137, 224);
    private final JLabel titleLabel = new JLabel("ClanHQ Events");
    private final JLabel subtitleLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JPanel eventsPanel = verticalPanel();
    private final JButton refreshButton = new JButton("Refresh Events");

    EventPanel(Runnable refreshAction)
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = verticalPanel();
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        content.add(titleLabel);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        subtitleLabel.setText(html("Live events in your ClanHQ clan."));
        subtitleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        content.add(Box.createRigidArea(new Dimension(0, 3)));
        content.add(subtitleLabel);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        content.add(sectionLabel("CURRENT EVENTS"));
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        content.add(eventsPanel);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        refreshButton.addActionListener(event -> refreshAction.run());
        refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(refreshButton);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                1, 0, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR),
            BorderFactory.createEmptyBorder(8, 0, 0, 0)));
        content.add(statusLabel);

        JScrollPane scroll = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        showStatus("Loading current events...");
    }

    void setLoading()
    {
        refreshButton.setEnabled(false);
        showStatus("Loading current events...");
    }

    void showEvents(String serverName, List<ClanEventSummary> events)
    {
        titleLabel.setText(serverName + " Events");
        refreshButton.setEnabled(true);
        eventsPanel.removeAll();
        if (events.isEmpty())
        {
            eventsPanel.add(emptyState("No scheduled or active events."));
        }
        else
        {
            for (ClanEventSummary event : events)
            {
                eventsPanel.add(eventPanel(event));
                eventsPanel.add(Box.createRigidArea(new Dimension(0, 9)));
            }
        }
        showStatus(events.size() + " current event"
            + (events.size() == 1 ? "" : "s")
            + ". Bingo linking is handled in the Bingo tab.");
        eventsPanel.revalidate();
        eventsPanel.repaint();
    }

    void showError(String message)
    {
        titleLabel.setText("ClanHQ Events");
        refreshButton.setEnabled(true);
        eventsPanel.removeAll();
        eventsPanel.add(emptyState("Events unavailable."));
        eventsPanel.revalidate();
        eventsPanel.repaint();
        showStatus(message);
    }

    void showObservation(
        boolean recorded,
        String eventName,
        String target,
        String message)
    {
        showStatus((recorded ? "Recorded: " : "Failed: ")
            + eventName + " — " + target);
    }

    private JPanel eventPanel(ClanEventSummary event)
    {
        Color accent = accentFor(event);
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(true);
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, event.isActive() ? 2 : 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JPanel details = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
        JLabel name = new JLabel(html("<b>" + escapeHtml(event.getName())
            + "</b>"));
        name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        details.add(name);
        details.add(Box.createRigidArea(new Dimension(0, 4)));
        JLabel status = new JLabel(displayValue(event.getStatus()));
        status.setForeground(accent);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        details.add(status);
        details.add(Box.createRigidArea(new Dimension(0, 5)));
        String target = event.getTarget() == null
            ? "No target" : event.getTarget();
        details.add(new JLabel(html("<b>Target</b><br>"
            + escapeHtml(target))));
        details.add(Box.createRigidArea(new Dimension(0, 5)));
        details.add(new JLabel(html("<b>Start</b><br>"
            + formatDateTime(event.getStartAt(), event.getStartDate()))));
        details.add(Box.createRigidArea(new Dimension(0, 5)));
        details.add(new JLabel(html("<b>End</b><br>"
            + formatDateTime(event.getEndAt(), event.getEndDate()))));
        if ("BINGO".equals(event.getEventType()))
        {
            details.add(Box.createRigidArea(new Dimension(0, 5)));
            JLabel bingo = new JLabel(html("<font color='#" + hex(BINGO_COLOR)
                + ">Bingo linking is available in the Bingo tab.</font>"));
            details.add(bingo);
        }
        panel.add(details, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(
            WRAP_WIDTH + 20, panel.getPreferredSize().height));
        return panel;
    }

    private static JPanel emptyState(String message)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel label = new JLabel(html(message));
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel sectionLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        return label;
    }

    private static Color accentFor(ClanEventSummary event)
    {
        if ("BINGO".equals(event.getEventType()))
        {
            return BINGO_COLOR;
        }
        return event.isActive() ? ACTIVE_COLOR : SCHEDULED_COLOR;
    }

    private void showStatus(String message)
    {
        statusLabel.setText("<html><body style='width: " + WRAP_WIDTH
            + "px'>" + escapeHtml(message) + "</body></html>");
    }

    private static String displayValue(String value)
    {
        if (value == null || value.isEmpty())
        {
            return "Unknown";
        }
        String normalized = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0))
            + normalized.substring(1);
    }

    private static String formatDateTime(Instant instant, LocalDate fallback)
    {
        return instant == null ? DATE_FORMAT.format(fallback)
            : DATE_TIME_FORMAT.format(instant);
    }

    private static String html(String text)
    {
        return "<html><body style='width: " + WRAP_WIDTH + "px'>"
            + text + "</body></html>";
    }

    private static String hex(Color color)
    {
        return String.format("%02x%02x%02x",
            color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String escapeHtml(String text)
    {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static JPanel verticalPanel()
    {
        return verticalPanel(ColorScheme.DARK_GRAY_COLOR);
    }

    private static JPanel verticalPanel(Color background)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }
}
