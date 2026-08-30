package com.clanhq.verifier.character;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
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
            + "equipped item IDs and quantities to ClanHQ for server-side "
            + "verification. You will confirm the destination before each "
            + "submission.</html>"));
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
        status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        showStatus("Syncing...");
    }

    void showResult(boolean successful, String message)
    {
        submit.setEnabled(true);
        showStatus((successful ? "" : "Sync failed. ") + message);
        status.setForeground(successful
            ? ColorScheme.LIGHT_GRAY_COLOR : new java.awt.Color(0xD95C5C));
    }

    void showSynced()
    {
        submit.setEnabled(true);
        status.setForeground(new java.awt.Color(0x70C090));
        status.setText("✓ Synced");
    }

    void showCancelled()
    {
        submit.setEnabled(true);
        status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        showStatus("Sync cancelled.");
    }

    private void showStatus(String message)
    {
        status.setText("<html><body style='width: 190px'>"
            + message.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;")
            + "</body></html>");
    }
    void addBelow(JComponent component)
    {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JSeparator divider = new JSeparator();
        divider.setAlignmentX(LEFT_ALIGNMENT);
        section.add(divider);
        section.add(Box.createRigidArea(new Dimension(0, 6)));
        component.setAlignmentX(LEFT_ALIGNMENT);
        section.add(component);
        add(section, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
