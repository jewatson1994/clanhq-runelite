package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTaskSummary;
import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.ColorScheme;

final class DailyTasksPanel extends JPanel
{
    private static final DateTimeFormatter RESET_FORMAT =
        DateTimeFormatter.ofPattern("MMM d, h:mm a")
            .withZone(ZoneId.systemDefault());
    private static final NumberFormat NUMBERS =
        NumberFormat.getIntegerInstance(Locale.US);

    private final JLabel statusLabel = new JLabel();
    private final JLabel resetLabel = new JLabel();
    private final JButton refreshButton = new JButton("Refresh Tasks");
    private final TaskCard skillingCard;
    private final TaskCard minigameCard;
    private final TaskCard pvmCard;

    DailyTasksPanel(Runnable refreshAction, Runnable skillingAction,
        Runnable minigameAction, Runnable pvmAction)
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.add(new JLabel("ClanHQ Daily Tasks"));
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        refreshButton.addActionListener(event -> refreshAction.run());
        content.add(refreshButton);
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        content.add(statusLabel);
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        skillingCard = new TaskCard("Claim Skilling Task", skillingAction);
        minigameCard = new TaskCard("Claim Minigame Task", minigameAction);
        pvmCard = new TaskCard("Claim PvM Task", pvmAction);
        content.add(skillingCard);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(minigameCard);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(pvmCard);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(resetLabel);

        JScrollPane scroll = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        showUnpaired("Pair this installation from Overview.");
    }

    void showUnpaired(String message)
    {
        refreshButton.setEnabled(false);
        setClaimButtons(false);
        clearTasks();
        showStatus(message);
    }

    void setLoading(String message)
    {
        refreshButton.setEnabled(false);
        setClaimButtons(false);
        showStatus(message);
    }

    void showTasks(DailyTasksSnapshot snapshot, String message)
    {
        refreshButton.setEnabled(true);
        setClaimButtons(true);
        clearTasks();
        for (DailyTaskSummary task : snapshot.getTasks())
        {
            TaskCard card = cardFor(task.getCategory());
            if (card != null)
            {
                card.showTask(task);
                card.setEnabled(!task.isCompleted());
            }
        }
        resetLabel.setText("Resets: "
            + RESET_FORMAT.format(snapshot.getResetAt()));
        showStatus(message);
    }

    void showError(String message, boolean paired)
    {
        if (!paired)
        {
            showUnpaired(message);
            return;
        }
        refreshButton.setEnabled(true);
        setClaimButtons(true);
        showStatus(message);
    }

    private TaskCard cardFor(String category)
    {
        if ("SKILLING".equals(category))
        {
            return skillingCard;
        }
        if ("MINIGAME".equals(category))
        {
            return minigameCard;
        }
        return "PVM".equals(category) ? pvmCard : null;
    }

    private void clearTasks()
    {
        skillingCard.clear();
        minigameCard.clear();
        pvmCard.clear();
        resetLabel.setText("");
    }

    private void showStatus(String message)
    {
        statusLabel.setText(html(message));
    }

    private void setClaimButtons(boolean enabled)
    {
        skillingCard.setEnabled(enabled);
        minigameCard.setEnabled(enabled);
        pvmCard.setEnabled(enabled);
    }

    private static String html(String text)
    {
        return "<html><body style='width: 190px'>"
            + escapeHtml(text) + "</body></html>";
    }

    private static String escapeHtml(String text)
    {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static final class TaskCard extends JPanel
    {
        private final JButton claimButton;
        private final JLabel details = new JLabel();

        private TaskCard(String buttonText, Runnable action)
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(ColorScheme.DARK_GRAY_COLOR);
            claimButton = new JButton(buttonText);
            claimButton.addActionListener(event -> action.run());
            add(claimButton);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(details);
            clear();
        }

        private void showTask(DailyTaskSummary task)
        {
            String marker = task.isCompleted() ? "&#10003;" : "&#9675;";
            StringBuilder value = new StringBuilder("<html><body style='width: 190px'>")
                .append(marker).append(' ')
                .append(escapeHtml(task.getName())).append("<br>")
                .append(escapeHtml(task.getDescription())).append("<br>")
                .append("Progress: ")
                .append(NUMBERS.format(task.getProgress()))
                .append(" / ").append(NUMBERS.format(task.getTarget()))
                .append("<br>Base reward: ")
                .append(NUMBERS.format(task.getReward()))
                .append(" DripDrops");
            if (task.isCompleted())
            {
                value.append("<br>Claimed: ")
                    .append(NUMBERS.format(task.getAwarded()));
                if (task.getPlacement() != null)
                {
                    value.append(" (place ")
                        .append(task.getPlacement()).append(')');
                }
            }
            details.setText(value.append("</body></html>").toString());
        }

        private void clear()
        {
            details.setText(html("Task unavailable."));
        }

        @Override
        public void setEnabled(boolean enabled)
        {
            super.setEnabled(enabled);
            if (claimButton != null)
            {
                claimButton.setEnabled(enabled);
            }
        }
    }
}
