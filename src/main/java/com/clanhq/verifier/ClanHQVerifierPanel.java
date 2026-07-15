package com.clanhq.verifier;

import com.clanhq.verifier.model.EvidenceStage;
import com.clanhq.verifier.model.EvidenceStageStatus;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

final class ClanHQVerifierPanel extends PluginPanel
{
    private final JComboBox<String> rankSelector;
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
    private final JLabel statusLabel = new JLabel("Start a verification session.");
    private final JTextArea previewArea = new JTextArea();
    private final JLabel evidenceSummary = new JLabel("Evidence: 0/0 sources");
    private final JLabel passedSummary = new JLabel("Passed: 0");
    private final JLabel missingSummary = new JLabel("Missing: 0");
    private final JLabel reviewSummary = new JLabel("Staff Review: 0");
    private int passedRequirements;
    private int missingRequirements;
    private int reviewRequirements;

    ClanHQVerifierPanel(List<String> rankNames,
        BiConsumer<String, EvidenceStage> captureAction,
        Consumer<String> rankChangedAction)
    {
        rankSelector = new JComboBox<>(rankNames.toArray(new String[0]));
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
        header.add(new JLabel("Requested rank:"));
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(rankSelector);
        header.add(Box.createRigidArea(new Dimension(0, 10)));

        stagePanel.setLayout(new BoxLayout(stagePanel, BoxLayout.Y_AXIS));
        stagePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        for (EvidenceStage stage : EvidenceStage.values())
        {
            addStage(stage, captureAction);
        }
        header.add(stagePanel);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        header.add(evidenceSummary);
        header.add(passedSummary);
        header.add(missingSummary);
        header.add(reviewSummary);
        header.add(Box.createRigidArea(new Dimension(0, 8)));

        resetButton.addActionListener(event -> rankChangedAction.accept(
            (String) rankSelector.getSelectedItem()));
        header.add(resetButton);
        header.add(Box.createRigidArea(new Dimension(0, 6)));

        submitButton.setEnabled(false);
        submitButton.setToolTipText("ClanHQ submission API is not connected yet");
        header.add(submitButton);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        header.add(statusLabel);

        rankSelector.addActionListener(event -> rankChangedAction.accept(
            (String) rankSelector.getSelectedItem()));

        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        previewArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        previewArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(previewArea), BorderLayout.CENTER);
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
            stageRows.get(stage).setVisible(required);
            showStageStatus(stage, EvidenceStageStatus.NOT_CAPTURED);
        }
        previewArea.setText("Capture each required evidence source for the selected rank.");
        statusLabel.setText("New verification session.");
        updateProgressSummary();
        revalidate();
        repaint();
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
        statusLabel.setText("Capturing " + stage.getDisplayName() + "...");
    }

    void showSnapshot(VerificationSnapshot snapshot,
        RankQualificationResult qualification, String status)
    {
        setControlsEnabled(true);
        statusLabel.setText(status);
        previewArea.setText(qualification.toChecklistText()
            + "\n\nCaptured evidence\n" + snapshot.toPreviewText());
        previewArea.setCaretPosition(0);
        passedRequirements = (int) qualification.getRequirements().stream()
            .filter(item -> item.getStatus() == RequirementStatus.PASSED).count();
        missingRequirements = (int) qualification.getRequirements().stream()
            .filter(item -> item.getStatus() == RequirementStatus.MISSING).count();
        reviewRequirements = (int) qualification.getRequirements().stream()
            .filter(item -> item.getStatus() == RequirementStatus.UNVERIFIED).count();
        updateRequirementSummary();
        updateProgressSummary();
    }

    void showMessage(String message)
    {
        setControlsEnabled(true);
        statusLabel.setText(message);
    }

    void showError(String message)
    {
        setControlsEnabled(true);
        statusLabel.setText("Capture failed.");
        previewArea.setText(message);
    }

    private void addStage(EvidenceStage stage,
        BiConsumer<String, EvidenceStage> captureAction)
    {
        JPanel row = verticalPanel();
        JButton button = new JButton(buttonLabel(stage));
        JLabel status = new JLabel(EvidenceStageStatus.NOT_CAPTURED.getDisplayText());
        button.addActionListener(event -> captureAction.accept(
            (String) rankSelector.getSelectedItem(), stage));
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
        rankSelector.setEnabled(enabled);
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
