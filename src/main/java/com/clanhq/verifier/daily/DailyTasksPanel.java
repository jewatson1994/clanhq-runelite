package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTaskSummary;
import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import com.clanhq.verifier.task.VerificationType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.game.SkillIconManager;

final class DailyTasksPanel extends JPanel
{
    // Leaves room for the task icon and card padding in the narrow sidebar.
    private static final int CONTENT_WIDTH = 180;
    private static final Color DAILY_GREEN = new Color(0x70C090);
    private static final Color DRIP_RUSH_BLUE = new Color(0x6FA8DC);
    private static final Color DROP_RUSH_RED = new Color(0xD95C5C);
    private static final DateTimeFormatter RESET_FORMAT =
        DateTimeFormatter.ofPattern("MMM d, h:mm a")
            .withZone(ZoneId.systemDefault());
    private static final NumberFormat NUMBERS =
        NumberFormat.getIntegerInstance(Locale.US);

    private final JLabel titleLabel = new JLabel("Tasks");
    private final JLabel summaryLabel = new JLabel("0 / 0 Complete");
    private final JLabel statusLabel = new JLabel();
    private final JLabel resetLabel = new JLabel();
    private final JButton refreshButton = new JButton("Refresh Tasks");
    private final JPanel taskCards = new JPanel();
    private final List<TaskCard> cards = new ArrayList<>();
    private final Consumer<String> claimAction;
    private final SkillIconManager skillIconManager;

    DailyTasksPanel(Runnable refreshAction, Consumer<String> claimAction,
        SkillIconManager skillIconManager)
    {
        this.claimAction = claimAction;
        this.skillIconManager = skillIconManager;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            titleLabel.getPreferredSize().height));
        content.add(titleLabel);
        content.add(Box.createRigidArea(new Dimension(0, 5)));
        summaryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        content.add(summaryLabel);
        summaryLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(resetLabel);
        resetLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        content.add(Box.createRigidArea(new Dimension(0, 4)));

        refreshButton.addActionListener(event -> refreshAction.run());
        refreshButton.setToolTipText("Refresh task progress from ClanHQ");
        content.add(refreshButton);
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        content.add(statusLabel);
        content.add(Box.createRigidArea(new Dimension(0, 6)));

        taskCards.setLayout(new BoxLayout(taskCards, BoxLayout.Y_AXIS));
        taskCards.setBackground(ColorScheme.DARK_GRAY_COLOR);
        taskCards.setAlignmentX(LEFT_ALIGNMENT);
        content.add(taskCards);
        add(content, BorderLayout.NORTH);
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
        String contextTitle = snapshot.getContext() == null
            ? "DAILY" : snapshot.getContext().getTitle();
        String contextType = snapshot.getContext() == null
            ? "daily" : snapshot.getContext().getType();
        if (isLegacyDailyTaskContext(snapshot))
        {
            contextTitle = "DAILY";
            contextType = "daily";
        }
        refreshButton.setEnabled(true);
        setClaimButtons(true);
        clearTasks();
        updateTitle(contextTitle.isEmpty() ? "DAILY" : contextTitle,
            contextType);
        boolean unsupported = false;
        for (DailyTaskSummary task : snapshot.getTasks())
        {
            TaskCard card = new TaskCard("Claim Task",
                () -> claimAction.accept(task.getCategory()), task.getCategory(),
                skillIconManager);
            cards.add(card);
            taskCards.add(card);
            if (taskCards.getComponentCount() > 1)
            {
                taskCards.add(Box.createRigidArea(new Dimension(0, 6)),
                    taskCards.getComponentCount() - 1);
            }
            card.showTask(task, snapshot.getCurrencyName(),
                snapshot.getCurrencySymbol());
            card.setEnabled(!task.isCompleted());
            if (requiresNewerPlugin(snapshot, task))
            {
                unsupported = true;
            }
        }
        updateSummary();
        resetLabel.setText("Resets at: "
            + RESET_FORMAT.format(rotationEnd(snapshot)));
        showStatus(unsupported
            ? "A task requires a newer ClanHQ plugin."
            : (isNormalLoadMessage(message) ? "" : message));
        revalidate();
        repaint();
    }

    /**
     * Update the rendered progress for a task from the local RuneLite event
     * stream. The server snapshot remains authoritative for claim state; this
     * only keeps the panel in step with the live overlay between refreshes.
     */
    void updateLiveProgress(String category, int progress)
    {
        TaskCard card = cardFor(category);
        if (card == null)
        {
            return;
        }
        card.updateLiveProgress(progress);
        updateSummary();
        revalidate();
        repaint();
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

    void setClaiming(String category)
    {
        TaskCard card = cardFor(category);
        if (card != null)
        {
            card.setClaiming();
        }
    }

    void restoreClaim(String category)
    {
        TaskCard card = cardFor(category);
        if (card != null)
        {
            card.restoreClaim();
        }
    }

    private TaskCard cardFor(String category)
    {
        for (TaskCard card : cards)
        {
            if (category != null && card.category.equals(category))
            {
                return card;
            }
        }
        return null;
    }

    private void clearTasks()
    {
        updateTitle("DAILY", "daily");
        summaryLabel.setText("0 / 0 Complete");
        cards.clear();
        taskCards.removeAll();
        resetLabel.setText("");
    }

    private void updateSummary()
    {
        int completed = 0;
        int claimed = 0;
        int earned = 0;
        for (TaskCard card : cards)
        {
            if (card.isCompleteForSummary())
            {
                completed++;
            }
            if (card.isServerCompleted())
            {
                claimed++;
                earned += Math.max(0, card.getAwarded());
            }
        }
        String summary = completed + " / " + cards.size() + " Complete"
            + (claimed > 0 ? "   •   " + claimed + " Claimed" : "")
            + (earned > 0 ? "<br>" + NUMBERS.format(earned) + " 💧 earned" : "");
        summaryLabel.setText("<html><body style='width: " + CONTENT_WIDTH
            + "px'>" + summary + "</body></html>");
    }

    private void updateTitle()
    {
        updateTitle("DAILY", "daily");
    }

    /**
     * Legacy Daily Tasks use WOM/claim-time verification even when their
     * generic metadata names an observation type that this client does not
     * implement. Only explicitly generic task contexts are gated by the
     * live-observation capability list.
     */
    private static boolean requiresNewerPlugin(DailyTasksSnapshot snapshot,
        DailyTaskSummary task)
    {
        if (isLegacyDailyTaskContext(snapshot))
        {
            return false;
        }
        VerificationType type = task.getVerificationType();
        return type != VerificationType.SKILL_XP
            && type != VerificationType.NPC_KILL
            && type != VerificationType.ITEM_DROP;
    }

    private static boolean isLegacyDailyTaskContext(DailyTasksSnapshot snapshot)
    {
        if (snapshot.getContext() == null)
        {
            return true;
        }
        String type = snapshot.getContext().getType();
        return type == null || type.trim().isEmpty()
            || "daily".equalsIgnoreCase(type)
            || "daily_tasks".equalsIgnoreCase(type);
    }

    private static java.time.Instant rotationEnd(DailyTasksSnapshot snapshot)
    {
        return snapshot.getContext() != null
            && snapshot.getContext().getRotationEndsAt() != null
            ? snapshot.getContext().getRotationEndsAt()
            : snapshot.getResetAt();
    }

    private void updateTitle(String contextTitle, String contextType)
    {
        Color color = "DROP_RUSH".equalsIgnoreCase(contextType)
            ? DROP_RUSH_RED
            : ("DRIP_RUSH".equalsIgnoreCase(contextType)
                ? DRIP_RUSH_BLUE : DAILY_GREEN);
        titleLabel.setForeground(color);
        titleLabel.setText("<html><div style='width: " + CONTENT_WIDTH
            + "px'><b>" + escapeHtml(contextTitle.toUpperCase(Locale.ROOT))
            + "</b></div></html>");
    }

    private void updateTitle(String contextTitle)
    {
        updateTitle(contextTitle, "daily");
    }

    private void showStatus(String message)
    {
        statusLabel.setText(html(message));
    }

    private static boolean isNormalLoadMessage(String message)
    {
        return message != null
            && message.toLowerCase(Locale.ROOT).contains("tasks loaded");
    }

    private void setClaimButtons(boolean enabled)
    {
        for (TaskCard card : cards)
        {
            card.setEnabled(enabled);
        }
    }

    private static String html(String text)
    {
        return "<html><body style='width: " + CONTENT_WIDTH + "px'>"
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
        private final JLabel progressValue = new JLabel();
        private final JLabel reward = new JLabel();
        private final JLabel claimed = new JLabel();
        private final JLabel icon = new JLabel();
        private final String category;
        private final SkillIconManager skillIconManager;
        private final JProgressBar progress = new JProgressBar();
        private int target = 1;
        private int currentProgress;
        private int awarded;
        private boolean serverCompleted;

        private TaskCard(String buttonText, Runnable action, String category,
            SkillIconManager skillIconManager)
        {
            this.category = category;
            this.skillIconManager = skillIconManager;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(ColorScheme.DARKER_GRAY_COLOR);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
                BorderFactory.createEmptyBorder(6, 7, 6, 7)));
            claimButton = new JButton(buttonText);
            claimButton.addActionListener(event -> action.run());
            JPanel taskRow = new JPanel(new BorderLayout(6, 0));
            taskRow.setOpaque(false);
            icon.setVerticalAlignment(JLabel.TOP);
            details.setVerticalAlignment(JLabel.TOP);
            taskRow.add(icon, BorderLayout.WEST);
            taskRow.add(details, BorderLayout.CENTER);
            add(taskRow);
            JPanel progressRow = new JPanel(new BorderLayout());
            progressRow.setOpaque(false);
            progressRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            progressValue.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            reward.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            progressRow.add(progressValue, BorderLayout.WEST);
            progressRow.add(reward, BorderLayout.EAST);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(progressRow);
            progress.setPreferredSize(new Dimension(0, 8));
            progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
            progress.setBorderPainted(false);
            progress.setBackground(new Color(0x3B3B3B));
            add(Box.createRigidArea(new Dimension(0, 3)));
            add(progress);
            add(Box.createRigidArea(new Dimension(0, 3)));
            add(claimed);
            JPanel claimRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            claimRow.setOpaque(false);
            claimRow.setAlignmentX(LEFT_ALIGNMENT);
            claimRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
            claimButton.setPreferredSize(new Dimension(104, 25));
            claimButton.setMinimumSize(new Dimension(104, 25));
            claimButton.setMaximumSize(new Dimension(104, 25));
            claimRow.add(claimButton);
            add(claimRow);
            clear();
        }

        private void showTask(DailyTaskSummary task, String currencyName,
            String currencySymbol)
        {
            updateIcon(task);
            target = Math.max(1, task.getTarget());
            currentProgress = Math.min(target, Math.max(0, task.getProgress()));
            awarded = task.getAwarded();
            serverCompleted = task.isCompleted();
            progress.setMaximum(target);
            progress.setValue(currentProgress);
            progress.setForeground(task.isCompleted()
                ? new Color(0x70C090) : new Color(0xC88A3D));
            claimText = "Claim " + NUMBERS.format(task.getReward())
                + (currencySymbol.isEmpty() ? "" : " " + currencySymbol);
            claimButton.setText(claimText);
            // Keep the action available for unclaimed tasks. The server combines
            // the latest RuneLite progress with WOM data when validating a claim.
            claimButton.setVisible(!task.isCompleted());
            StringBuilder value = new StringBuilder("<html><body style='width: ")
                .append(CONTENT_WIDTH).append("px'>")
                .append("<b>").append(escapeHtml(task.getName())).append("</b><br>")
                .append("<font color='#B8B8B8'>")
                .append(escapeHtml(task.getDescription()
                    .replace(" experience", " XP"))).append("</font></body></html>");
            progressValue.setText(compact(currentProgress) + " / "
                + compact(target));
            reward.setText(NUMBERS.format(task.getReward())
                + (currencySymbol.isEmpty() ? " " + currencyName
                    : " " + currencySymbol));
            claimed.setVisible(task.isCompleted());
            claimed.setForeground(new Color(0x70C090));
            if (task.isCompleted())
            {
                claimed.setText("✓ Claimed • " + NUMBERS.format(task.getAwarded())
                    + (currencySymbol.isEmpty() ? " " + currencyName
                        : " " + currencySymbol));
                if (task.getPlacement() != null)
                {
                    claimed.setText(claimed.getText() + " (place "
                        + task.getPlacement() + ")");
                }
            }
            details.setText(value.toString());
            revalidate();
            repaint();
        }

        private void updateLiveProgress(int value)
        {
            currentProgress = Math.max(currentProgress,
                Math.min(target, Math.max(0, value)));
            progress.setValue(currentProgress);
            if (currentProgress >= target)
            {
                progress.setForeground(DAILY_GREEN);
            }
            progressValue.setText(compact(currentProgress) + " / "
                + compact(target));
            repaint();
        }

        private boolean isCompleteForSummary()
        {
            return serverCompleted || currentProgress >= target;
        }

        private boolean isServerCompleted()
        {
            return serverCompleted;
        }

        private int getAwarded()
        {
            return awarded;
        }

        private void setClaiming()
        {
            claimButton.setText("Claiming...");
            claimButton.setEnabled(false);
        }

        private void restoreClaim()
        {
            claimButton.setText(claimText);
            claimButton.setEnabled(true);
        }

        private String claimText = "Claim";

        private void clear()
        {
            icon.setIcon(null);
            icon.setText(defaultIcon());
            progress.setValue(0);
            target = 1;
            currentProgress = 0;
            awarded = 0;
            serverCompleted = false;
            progressValue.setText("");
            reward.setText("");
            claimed.setText("");
            claimed.setVisible(false);
            claimButton.setVisible(false);
            details.setText(html("Task unavailable."));
        }

        private void updateIcon(DailyTaskSummary task)
        {
            if (!category.equals("SKILLING") || skillIconManager == null)
            {
                icon.setText(defaultIcon());
                icon.setIcon(null);
                return;
            }
            Skill skill = DailyTaskSkillMatcher.findTaskSkill(task);
            BufferedImage image = skill == null
                ? null : skillIconManager.getSkillImage(skill);
            if (image == null)
            {
                icon.setText("🪓");
                icon.setIcon(null);
            }
            else
            {
                icon.setText("");
                icon.setIcon(new ImageIcon(image));
            }
        }

        private String defaultIcon()
        {
            if (category.equals("PVM"))
            {
                return "⚔";
            }
            if (category.equals("DROP"))
            {
                return "⬇";
            }
            return category.equals("SKILLING") ? "⚒" : "🎮";
        }

        private static String compact(int value)
        {
            if (value >= 1_000_000)
            {
                return String.format(Locale.US, "%.1fM", value / 1_000_000.0)
                    .replace(".0M", "M");
            }
            if (value >= 1_000)
            {
                return String.format(Locale.US, "%.1fK", value / 1_000.0)
                    .replace(".0K", "K");
            }
            return NUMBERS.format(value);
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
