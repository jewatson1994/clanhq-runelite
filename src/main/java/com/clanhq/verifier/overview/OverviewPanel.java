package com.clanhq.verifier.overview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

final class OverviewPanel extends JPanel
{
    private final JLabel connection = new JLabel("Connection: Checking...");
    private final JLabel identity = new JLabel("Linked RSNs: —");
    private final JLabel balance = new JLabel();
    private final JLabel status = new JLabel();
    private final JButton pair = new JButton("Pair Installation");
    private final JButton refresh = new JButton("Refresh Overview");

    OverviewPanel(Runnable pairAction, Runnable refreshAction)
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.add(new JLabel("ClanHQ Overview"));
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(new JLabel("<html>ClanHQ securely connects RuneLite to "
            + "events, Bingo, daily tasks, and character data.</html>"));
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(connection);
        content.add(identity);
        content.add(balance);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        pair.addActionListener(event -> pairAction.run());
        content.add(pair);
        content.add(Box.createRigidArea(new Dimension(0, 5)));
        refresh.addActionListener(event -> refreshAction.run());
        content.add(refresh);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(status);
        add(content, BorderLayout.NORTH);
    }

    void setLoading()
    {
        refresh.setEnabled(false);
        showStatus("Checking the ClanHQ connection...");
    }

    void setPairing()
    {
        pair.setEnabled(false);
        refresh.setEnabled(false);
        showStatus("Pairing this RuneLite installation...");
    }

    void showIdentity(IdentitySnapshot value, boolean showBalance)
    {
        refresh.setEnabled(true);
        pair.setEnabled(false);
        connection.setText("Connection: Paired (" + value.getDeviceName() + ")");
        identity.setText("Linked RSNs: " + String.join(", ", value.getRsns()));
        balance.setText(showBalance
            ? "DripDrops: " + value.getBalance() : "");
        showStatus("Connection verified.");
    }

    void showError(String message)
    {
        refresh.setEnabled(true);
        pair.setEnabled(true);
        connection.setText("Connection: Not available");
        identity.setText("Linked RSNs: —");
        balance.setText("");
        showStatus(message);
    }

    private void showStatus(String message)
    {
        status.setText("<html><body style='width: 190px'>"
            + message.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;")
            + "</body></html>");
    }
}
