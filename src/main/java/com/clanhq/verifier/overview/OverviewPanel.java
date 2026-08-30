package com.clanhq.verifier.overview;

import com.clanhq.verifier.bingo.model.BingoManifest;
import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import net.runelite.client.ui.ColorScheme;

final class OverviewPanel extends JPanel
{
    private static final NumberFormat NUMBERS =
        NumberFormat.getIntegerInstance(Locale.US);
    private final JLabel title = new JLabel("ClanHQ Overview");
    private final JLabel status = new JLabel();
    private final JLabel playingAs = new JLabel();
    private final JLabel linkStatus = new JLabel();
    private final JLabel characters = new JLabel();
    private final JLabel balance = new JLabel();
    private final JPanel statsPeriods = new JPanel();
    private final JButton pair = new JButton("Pair ClanHQ");
    private final JButton connection = new JButton("Connection Settings");
    private final JButton refresh = new JButton("↻ Refresh");

    OverviewPanel(Runnable pairAction, Runnable refreshAction,
        Runnable disconnectAction)
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);

        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(Color.WHITE);
        addFullWidth(content, title);
        content.add(Box.createVerticalStrut(7));
        addFullWidth(content, status);
        content.add(Box.createVerticalStrut(8));

        addSectionHeading(content, "PLAYING AS");
        addFullWidth(content, playingAs);
        addFullWidth(content, linkStatus);
        content.add(Box.createVerticalStrut(7));
        addSectionHeading(content, "CHARACTERS");
        addFullWidth(content, characters);
        content.add(Box.createVerticalStrut(8));
        addFullWidth(content, balance);
        content.add(Box.createVerticalStrut(8));
        content.add(separator());
        content.add(Box.createVerticalStrut(7));
        statsPeriods.setLayout(new BoxLayout(statsPeriods, BoxLayout.Y_AXIS));
        statsPeriods.setBackground(ColorScheme.DARK_GRAY_COLOR);
        statsPeriods.setAlignmentX(LEFT_ALIGNMENT);
        statsPeriods.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            Integer.MAX_VALUE));
        content.add(statsPeriods);
        content.add(Box.createVerticalStrut(8));
        content.add(separator());
        content.add(Box.createVerticalStrut(7));

        pair.addActionListener(event -> pairAction.run());
        connection.addActionListener(event -> disconnectAction.run());
        refresh.addActionListener(event -> refreshAction.run());
        content.add(pair);
        content.add(connection);
        content.add(refresh);
        add(content, BorderLayout.NORTH);
        showUnpaired("Connect RuneLite to ClanHQ to get started.");
    }

    void setLoading(boolean hasStoredPairing)
    {
        pair.setVisible(!hasStoredPairing);
        connection.setVisible(hasStoredPairing);
        refresh.setEnabled(false);
        showStatus("Checking ClanHQ connection...");
    }

    void showIdentity(IdentitySnapshot value, DailyTasksSnapshot taskSnapshot,
        BingoManifest bingoSnapshot,
        String currentRsn)
    {
        title.setText(value.getServerName() + " Overview");
        status.setForeground(new Color(0x70C090));
        status.setText("● Connected to ClanHQ");
        String activeRsn = currentRsn == null || currentRsn.trim().isEmpty()
            ? "Not logged in" : currentRsn.trim();
        playingAs.setText(activeRsn);
        linkStatus.setText(isLinked(value.getRsns(), activeRsn)
            ? "✓ Linked" : ("Not logged in".equals(activeRsn)
                ? "" : "⚠ Not linked to ClanHQ"));
        playingAs.setVisible(true);
        linkStatus.setVisible(!linkStatus.getText().isEmpty());
        characters.setText(formatCharacters(value.getRsns(), activeRsn));
        characters.setVisible(true);
        balance.setText(value.getCurrencyName() + ": " + value.getBalance()
            + (value.getCurrencySymbol().isEmpty() ? ""
                : " " + value.getCurrencySymbol()));
        balance.setVisible(true);
        renderStats(value);
        pair.setVisible(false);
        connection.setVisible(true);
        refresh.setVisible(true);
        refresh.setEnabled(true);
        revalidate();
    }

    void showError(String message, boolean hasStoredPairing)
    {
        title.setText("ClanHQ Overview");
        status.setForeground(new Color(0xD95C5C));
        status.setText("● Connection Issue");
        showMessage(message);
        pair.setVisible(!hasStoredPairing);
        connection.setVisible(hasStoredPairing);
        refresh.setVisible(true);
        refresh.setEnabled(true);
        revalidate();
    }

    void showUnpaired(String message)
    {
        title.setText("ClanHQ");
        status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        status.setText("○ Not Connected");
        characters.setVisible(false);
        playingAs.setVisible(false);
        linkStatus.setVisible(false);
        balance.setVisible(true);
        statsPeriods.setVisible(false);
        pair.setVisible(true);
        connection.setVisible(false);
        refresh.setVisible(false);
        showMessage(message);
        revalidate();
    }

    void showDisconnected(String message)
    {
        showUnpaired(message);
        pair.setEnabled(true);
    }

    void setPairing()
    {
        pair.setEnabled(false);
        showStatus("Connecting to ClanHQ...");
    }

    void setDisconnecting()
    {
        connection.setEnabled(false);
        refresh.setEnabled(false);
        showStatus("Disconnecting from ClanHQ...");
    }

    // Task and Bingo state continue to be owned by their dedicated tabs. This
    // method intentionally keeps the existing synchronization callback safe
    // without duplicating their live state on Overview.
    void updateToday(DailyTasksSnapshot taskSnapshot,
        BingoManifest bingoSnapshot)
    {
        // No Overview activity rendering by design.
    }

    boolean confirmDisconnect()
    {
        return JOptionPane.showConfirmDialog(this,
            "Disconnect this RuneLite installation from ClanHQ?\n\n"
                + "The server token will be revoked and the local pairing removed.",
            "Disconnect ClanHQ", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void showStatus(String message) { status.setText(message); }

    private void showMessage(String message)
    {
        balance.setText(message == null ? "" : message);
    }

    private static String formatCharacters(List<String> rsns, String currentRsn)
    {
        if (rsns == null || rsns.isEmpty()) return "No linked characters";
        StringBuilder result = new StringBuilder("<html>");
        for (int index = 0; index < rsns.size(); index++)
        {
            if (index > 0) result.append("<br>");
            boolean active = rsns.get(index) != null
                && normalizeName(rsns.get(index)).equals(normalizeName(currentRsn));
            if (active)
            {
                result.append("<font color='#70C090'>●</font> ");
            }
            else
            {
                result.append("  ");
            }
            result.append(escape(rsns.get(index)));
        }
        return result.append("</html>").toString();
    }

    private static boolean isLinked(List<String> rsns, String currentRsn)
    {
        for (String rsn : rsns)
        {
            if (rsn != null && normalizeName(rsn).equals(normalizeName(currentRsn)))
            {
                return true;
            }
        }
        return false;
    }

    private static String normalizeName(String value)
    {
        return value == null ? "" : value.replace(" ", "").trim().toLowerCase();
    }

    private static void addSectionHeading(JPanel panel, String text)
    {
        JLabel heading = new JLabel(text);
        heading.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 11f));
        addFullWidth(panel, heading);
        panel.add(Box.createVerticalStrut(3));
    }

    private void renderStats(IdentitySnapshot value)
    {
        statsPeriods.removeAll();
        addStatsPeriod("TODAY", value.getTodayEarned());
        addStatsPeriod("THIS WEEK", value.getWeekEarned());
        addStatsPeriod("THIS MONTH", value.getMonthEarned());
        addStatsPeriod("ALL TIME", value.getAllTimeEarned(), value.getAllTimeRank());
        statsPeriods.setVisible(true);
        statsPeriods.revalidate();
    }

    private void addStatsPeriod(String titleText, int earned)
    {
        addStatsPeriod(titleText, earned, 0);
    }

    private void addStatsPeriod(String titleText, int earned, int rank)
    {
        JLabel heading = new JLabel(titleText);
        heading.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 11f));
        addFullWidth(statsPeriods, heading);
        JLabel earnedLabel = new JLabel(stat("DripDrops", "+" + NUMBERS.format(earned)));
        addFullWidth(statsPeriods, earnedLabel);
        if ("ALL TIME".equals(titleText))
        {
            JLabel rankLabel = new JLabel(stat("DD Rank",
                rank > 0 ? "#" + rank : "—"));
            addFullWidth(statsPeriods, rankLabel);
        }
        statsPeriods.add(Box.createVerticalStrut(5));
    }

    private static void addFullWidth(JPanel panel, JLabel label)
    {
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            Integer.MAX_VALUE));
        panel.add(label);
    }

    private static JSeparator separator()
    {
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return separator;
    }

    private static String escape(String value)
    {
        return value == null ? "" : value.replace("&", "&amp;")
            .replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String stat(String label, String value)
    {
        return "<html><table width='180'><tr><td>" + escape(label)
            + "</td><td align='right'>" + escape(value)
            + "</td></tr></table></html>";
    }
}