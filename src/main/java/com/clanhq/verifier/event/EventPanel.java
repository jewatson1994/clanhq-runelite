package com.clanhq.verifier.event;

import com.clanhq.verifier.event.model.ClanEventSummary;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

final class EventPanel extends JPanel
{
    private static final int WRAP_WIDTH = 190;
    private final JLabel nameLabel = new JLabel("Event: Not connected");
    private final JLabel typeLabel = new JLabel("Type: —");
    private final JLabel targetLabel = new JLabel("Target: —");
    private final JLabel datesLabel = new JLabel("Dates: —");
    private final JLabel statusLabel = new JLabel();
    private final JLabel participationLabel = new JLabel("Participation: —");
    private final JButton refreshButton = new JButton("Refresh Event");

    EventPanel(Runnable refreshAction)
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = verticalPanel();
        content.add(new JLabel("ClanHQ Events"));
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        content.add(new JLabel("<html>Enter the code identified with the "
            + "event. Please reach out to Staff for support.</html>"));
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(nameLabel);
        content.add(typeLabel);
        content.add(targetLabel);
        content.add(datesLabel);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        refreshButton.addActionListener(event -> refreshAction.run());
        content.add(refreshButton);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(statusLabel);
        content.add(participationLabel);
        add(content, BorderLayout.NORTH);
        showStatus("Configure an event code to begin.");
    }

    void setLoading()
    {
        refreshButton.setEnabled(false);
        showStatus("Loading event information...");
    }

    void showEvent(ClanEventSummary event)
    {
        refreshButton.setEnabled(true);
        nameLabel.setText("Event: " + event.getName());
        typeLabel.setText("Type: " + displayValue(event.getEventType()));
        targetLabel.setText("Target: "
            + (event.getTarget() == null ? "Not applicable" : event.getTarget()));
        datesLabel.setText("Dates: " + event.getStartDate() + " through "
            + event.getEndDate());
        showStatus("Status: " + displayValue(event.getStatus()));
        participationLabel.setText("Participation: Registering...");
    }

    void showError(String message)
    {
        refreshButton.setEnabled(true);
        nameLabel.setText("Event: Not connected");
        typeLabel.setText("Type: —");
        targetLabel.setText("Target: —");
        datesLabel.setText("Dates: —");
        showStatus(message);
        participationLabel.setText("Participation: —");
    }

    void showObservation(boolean recorded, String target, String message)
    {
        showStatus((recorded ? "Progress recorded for " : "Progress failed for ")
            + target + ": " + message);
    }

    void showParticipation(boolean joined, String message, String teamName)
    {
        String team = teamName == null ? "Unassigned" : teamName;
        participationLabel.setText(
            "<html>Participation: " + (joined ? "✓ " : "✗ ")
                + escapeHtml(message) + "<br>Team: "
                + escapeHtml(team) + "</html>"
        );
    }

    private void showStatus(String message)
    {
        statusLabel.setText("<html><body style='width: " + WRAP_WIDTH
            + "px'>" + escapeHtml(message) + "</body></html>");
    }

    private static String displayValue(String value)
    {
        String normalized = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0))
            + normalized.substring(1);
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
