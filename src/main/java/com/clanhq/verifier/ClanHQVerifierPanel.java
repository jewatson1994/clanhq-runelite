package com.clanhq.verifier;

import com.clanhq.verifier.model.EvidenceStage;
import com.clanhq.verifier.model.EvidenceStageStatus;
import com.clanhq.verifier.model.ProgressionEvaluation;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.function.Consumer;
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
    private static final int STATUS_WRAP_WIDTH = 190;
    private static final Set<EvidenceStage> PRIMARY_STAGES = EnumSet.of(
        EvidenceStage.CHARACTER, EvidenceStage.GEAR, EvidenceStage.POH);
    private final JPanel stagePanel = new JPanel();
    private final Map<EvidenceStage, JPanel> stageRows =
        new EnumMap<>(EvidenceStage.class);
    private final Map<EvidenceStage, JButton> stageButtons =
        new EnumMap<>(EvidenceStage.class);
    private final Map<EvidenceStage, JLabel> stageStatuses =
        new EnumMap<>(EvidenceStage.class);
    private final Map<EvidenceStage, EvidenceStageStatus> currentStatuses =
        new EnumMap<>(EvidenceStage.class);
    private Set<EvidenceStage> requiredStages = EnumSet.noneOf(EvidenceStage.class);
    private final JButton submitButton = new JButton("Submit Review Ticket");
    private final JButton resetButton = new JButton("Reset Session");
    private final JLabel apiDestinationLabel = new JLabel("API: Not configured");
    private final JLabel statusLabel = new JLabel();
    private final JTextArea previewArea = new JTextArea();
    private final JLabel highestRankSummary = new JLabel();
    private final JLabel nextRankSummary = new JLabel();
    private final JLabel evidenceSummary = new JLabel("Evidence: 0/0 sources");
    private final JLabel passedSummary = new JLabel("Passed: 0");
    private final JLabel missingSummary = new JLabel("Missing: 0");
    private final JLabel reviewSummary = new JLabel("Staff Review: 0");
    private int passedRequirements;
    private int missingRequirements;
    private int reviewRequirements;

    ClanHQVerifierPanel(Consumer<EvidenceStage> captureAction,
        Runnable resetAction)
    {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = verticalPanel();
        header.add(new JLabel("ClanHQ Rank Verifier"));
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        header.add(new JLabel("<html>Evidence remains local until submitted.<br>"
            + "Submission is not connected yet.</html>"));
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(apiDestinationLabel);
        header.add(Box.createRigidArea(new Dimension(0, 10)));

        stagePanel.setLayout(new BoxLayout(stagePanel, BoxLayout.Y_AXIS));
        stagePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        for (EvidenceStage stage : EvidenceStage.values())
        {
            addStage(stage, captureAction);
        }
        header.add(stagePanel);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        setWrappedLabelText(highestRankSummary,
            "Highest verified rank: Not evaluated");
        setWrappedLabelText(nextRankSummary,
            "Next rank: Verify character to begin");
        header.add(highestRankSummary);
        header.add(nextRankSummary);
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(evidenceSummary);
        header.add(passedSummary);
        header.add(missingSummary);
        header.add(reviewSummary);
        header.add(Box.createRigidArea(new Dimension(0, 8)));

        resetButton.addActionListener(event -> resetAction.run());
        header.add(resetButton);
        header.add(Box.createRigidArea(new Dimension(0, 6)));

        submitButton.setEnabled(false);
        submitButton.setToolTipText("ClanHQ submission API is not connected yet");
        header.add(submitButton);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        setStatusText("Start a verification session.");
        header.add(statusLabel);

        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        previewArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        previewArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane previewScroll = new JScrollPane(previewArea);
        previewScroll.setPreferredSize(new Dimension(200, 320));
        previewScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JPanel content = verticalPanel();
        content.add(header);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(previewScroll);

        JScrollPane panelScroll = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panelScroll.setBorder(null);
        panelScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(panelScroll, BorderLayout.CENTER);
    }

    void setRequiredStages(Set<EvidenceStage> requiredStages)
    {
        this.requiredStages = requiredStages.isEmpty()
            ? EnumSet.noneOf(EvidenceStage.class)
            : EnumSet.copyOf(requiredStages);
        passedRequirements = 0;
        missingRequirements = 0;
        reviewRequirements = 0;
        updateRequirementSummary();
        for (EvidenceStage stage : EvidenceStage.values())
        {
            boolean required = requiredStages.contains(stage);
            stageRows.get(stage).setVisible(required
                && PRIMARY_STAGES.contains(stage));
            showStageStatus(stage, EvidenceStageStatus.NOT_CAPTURED);
        }
        previewArea.setText("Capture evidence to calculate the highest verified rank.");
        setWrappedLabelText(highestRankSummary,
            "Highest verified rank: Not evaluated");
        setWrappedLabelText(nextRankSummary,
            "Next rank: Verify character to begin");
        setStatusText("New verification session.");
        updateProgressSummary();
        revalidate();
        repaint();
    }

    void showFallbackStage(EvidenceStage stage)
    {
        if (requiredStages.contains(stage))
        {
            stageRows.get(stage).setVisible(true);
            revalidate();
            repaint();
        }
    }

    void showApiDestination(String description)
    {
        apiDestinationLabel.setText(description);
    }

    void showStageStatus(EvidenceStage stage, EvidenceStageStatus status)
    {
        currentStatuses.put(stage, status);
        stageStatuses.get(stage).setText(status.getDisplayText());
        updateProgressSummary();
    }

    void setStageBusy(EvidenceStage stage)
    {
        setControlsEnabled(false);
        showStageStatus(stage, EvidenceStageStatus.CAPTURING);
        setStatusText("Capturing " + stage.getDisplayName() + "...");
    }

    void showSnapshot(VerificationSnapshot snapshot,
        ProgressionEvaluation progression, String status)
    {
        setControlsEnabled(true);
        setStatusText(status);
        previewArea.setText(progression.toProgressText()
            + "\n\nCaptured evidence\n" + snapshot.toPreviewText());
        previewArea.setCaretPosition(0);
        setWrappedLabelText(highestRankSummary, "Highest verified rank: "
            + progression.getHighestVerifiedRankName());
        setWrappedLabelText(nextRankSummary, "Next rank: "
            + progression.getNextRank()
                .map(result -> result.getRankName())
                .orElse("All configured ranks verified"));
        passedRequirements = (int) progression.count(RequirementStatus.PASSED);
        missingRequirements = (int) (progression.count(RequirementStatus.MISSING)
            + progression.count(RequirementStatus.NOT_CAPTURED));
        reviewRequirements = (int) progression.count(RequirementStatus.UNVERIFIED);
        updateRequirementSummary();
        updateProgressSummary();
    }

    void showMessage(String message)
    {
        setControlsEnabled(true);
        setStatusText(message);
    }

    void showError(String message)
    {
        setControlsEnabled(true);
        setStatusText("Capture failed.");
        previewArea.setText(message);
    }

    private void addStage(EvidenceStage stage,
        Consumer<EvidenceStage> captureAction)
    {
        JPanel row = verticalPanel();
        JButton button = new JButton(buttonLabel(stage));
        JLabel status = new JLabel(EvidenceStageStatus.NOT_CAPTURED.getDisplayText());
        button.addActionListener(event -> captureAction.accept(stage));
        row.add(button);
        row.add(status);
        row.add(Box.createRigidArea(new Dimension(0, 6)));
        stageRows.put(stage, row);
        stageButtons.put(stage, button);
        stageStatuses.put(stage, status);
        currentStatuses.put(stage, EvidenceStageStatus.NOT_CAPTURED);
        stagePanel.add(row);
    }

    private void setControlsEnabled(boolean enabled)
    {
        resetButton.setEnabled(enabled);
        stageButtons.values().forEach(button -> button.setEnabled(enabled));
    }

    private static JPanel verticalPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        return panel;
    }

    private void updateRequirementSummary()
    {
        passedSummary.setText("Passed: " + passedRequirements);
        missingSummary.setText("Missing: " + missingRequirements);
        reviewSummary.setText("Staff Review: " + reviewRequirements);
    }

    private void setStatusText(String message)
    {
        setWrappedLabelText(statusLabel, message);
    }

    private static void setWrappedLabelText(JLabel label, String message)
    {
        label.setText("<html><body style='width: "
            + STATUS_WRAP_WIDTH + "px'>" + escapeHtml(message)
            + "</body></html>");
    }

    static String escapeHtml(String text)
    {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static String buttonLabel(EvidenceStage stage)
    {
        switch (stage)
        {
            case CHARACTER: return "Verify Character";
            case PRAYERS: return "Capture Prayers";
            case GEAR: return "Capture Bank & Gear";
            case RAID_KC: return "Fetch Raid KC";
            case COX_LOG: return "Capture COX Log";
            case TOB_LOG: return "Capture TOB Log";
            case TOA_LOG: return "Capture TOA Log";
            case YAMA_LOG: return "Capture Yama Log";
            case DOOM_LOG: return "Capture Doom Log";
            case POH: return "Capture POH Instance";
            case BOAT: return "Capture Boat";
            default: throw new IllegalArgumentException("Unknown evidence stage");
        }
    }

    private void updateProgressSummary()
    {
        long ready = requiredStages.stream()
            .filter(stage -> currentStatuses.getOrDefault(stage,
                EvidenceStageStatus.NOT_CAPTURED).isSubmissionReady())
            .count();
        evidenceSummary.setText("Evidence: " + ready + "/"
            + requiredStages.size() + " sources");
    }
}
