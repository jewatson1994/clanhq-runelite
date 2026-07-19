package com.clanhq.verifier.character;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

final class CharacterSyncPanel extends JPanel
{
    private final JButton submit = new JButton("Sync Character Data");
    private final JLabel status = new JLabel();

    CharacterSyncPanel(Runnable submitAction)
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.add(new JLabel("Character Sync"));
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(new JLabel("<html>Send your complete bank, inventory, and "
            + "equipped items to ClanHQ for server-side verification.</html>"));
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        submit.addActionListener(event -> submitAction.run());
        content.add(submit);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(status);
        add(content, BorderLayout.NORTH);
        showResult(true, "Open your bank before synchronizing.");
    }

    void setSubmitting()
    {
        submit.setEnabled(false);
        showStatus("Capturing complete character data...");
    }

    void showResult(boolean successful, String message)
    {
        submit.setEnabled(true);
        showStatus((successful ? "" : "Sync failed. ") + message);
    }

    private void showStatus(String message)
    {
        status.setText("<html><body style='width: 190px'>"
            + message.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;")
            + "</body></html>");
    }
}
