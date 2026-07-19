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
    private final JLabel participationLabel = new JLabel("Participation: —");
    private final JLabel teamLabel = new JLabel("Team: Unassigned");
    private final JLabel boardLabel = new JLabel("Board: Not loaded");
    private final JLabel trackingLabel = new JLabel("Drop tracking: Inactive");
    private final JLabel characterCheckLabel = new JLabel(
        "Character check: Not submitted");
    private final JLabel statusLabel = new JLabel();
    private final JTextArea activity = new JTextArea();
    private final JButton refreshButton = new JButton("Refresh Bingo Board");
    private final JButton characterSubmitButton = new JButton("Character Submit");

    BingoPanel(Runnable refreshAction, Runnable characterSubmitAction)
    {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = verticalPanel();
        header.add(new JLabel("ClanHQ Bingo"));
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        header.add(new JLabel("<html>Enter the code identified with the event. "
            + "Please reach out to Staff for support.</html>"));
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        header.add(eventLabel);
        header.add(participationLabel);
        header.add(teamLabel);
        header.add(boardLabel);
        header.add(trackingLabel);
        header.add(characterCheckLabel);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        refreshButton.addActionListener(event -> refreshAction.run());
        header.add(refreshButton);
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        characterSubmitButton.addActionListener(
            event -> characterSubmitAction.run());
        characterSubmitButton.setEnabled(false);
        header.add(characterSubmitButton);
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(new JLabel("<html>Character Submit sends your complete "
            + "bank, inventory, and equipped item IDs and quantities. "
            + "RuneLite asks for confirmation first.</html>"));
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
        characterSubmitButton.setEnabled(true);
        eventLabel.setText("Event: " + manifest.getName());
        boardLabel.setText("Board: Loaded (" + manifest.getItems().size()
            + " tracked items/tasks)");
        trackingLabel.setText("Drop tracking: Active");
        characterCheckLabel.setText("Character check: "
            + manifest.getCharacterCheck().getDisplayStatus());
        characterSubmitButton.setText(
            manifest.getCharacterCheck().getButtonLabel());
        characterSubmitButton.setEnabled(
            manifest.getCharacterCheck().canSubmit());
        showStatus("Bingo board loaded.");
    }

    void showManifestError(String message)
    {
        refreshButton.setEnabled(true);
        characterSubmitButton.setEnabled(false);
        eventLabel.setText("Event: Not loaded");
        participationLabel.setText("Participation: —");
        teamLabel.setText("Team: Unassigned");
        boardLabel.setText("Board: Not loaded");
        trackingLabel.setText("Drop tracking: Inactive");
        characterCheckLabel.setText("Character check: Not submitted");
        showStatus(message);
    }

    void showParticipation(boolean joined, String teamName, String message)
    {
        participationLabel.setText("Participation: "
            + (joined ? "Joined" : "Not joined"));
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
        characterSubmitButton.setEnabled(false);
        showStatus("Capturing and submitting the complete character snapshot...");
    }

    void showCharacterSubmission(boolean successful, String message)
    {
        characterSubmitButton.setEnabled(!successful);
        showStatus((successful ? "Character submitted. " : "Submission failed. ")
            + message);
    }

    void resetCharacterSubmission()
    {
        characterSubmitButton.setEnabled(true);
    }

    void showCharacterSubmissionCancelled()
    {
        characterSubmitButton.setEnabled(true);
        showStatus("Character submission cancelled.");
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
