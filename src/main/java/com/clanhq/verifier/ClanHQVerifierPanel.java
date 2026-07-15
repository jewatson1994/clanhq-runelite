package com.clanhq.verifier;

import com.clanhq.verifier.model.VerificationSnapshot;
import java.awt.BorderLayout;
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
import net.runelite.client.ui.PluginPanel;

final class ClanHQVerifierPanel extends PluginPanel
{
    private final JButton captureButton = new JButton(
        "Capture Current Character");
    private final JLabel statusLabel = new JLabel(
        "Nothing captured yet.");
    private final JTextArea previewArea = new JTextArea();

    ClanHQVerifierPanel(Runnable captureAction)
    {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("ClanHQ Rank Verifier");
        JLabel privacy = new JLabel(
            "<html>Capture is local and preview-only.<br>"
                + "No data is sent.</html>");

        captureButton.addActionListener(event -> captureAction.run());

        header.add(title);
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        header.add(privacy);
        header.add(Box.createRigidArea(new Dimension(0, 10)));
        header.add(captureButton);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        header.add(statusLabel);

        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        previewArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        previewArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(previewArea), BorderLayout.CENTER);
    }

    void setBusy()
    {
        captureButton.setEnabled(false);
        statusLabel.setText("Capturing…");
    }

    void showSnapshot(VerificationSnapshot snapshot, String status)
    {
        captureButton.setEnabled(true);
        statusLabel.setText(status);
        previewArea.setText(snapshot.toPreviewText());
        previewArea.setCaretPosition(0);
    }

    void showError(String message)
    {
        captureButton.setEnabled(true);
        statusLabel.setText("Capture failed.");
        previewArea.setText(message);
    }
}
