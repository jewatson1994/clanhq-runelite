package com.clanhq.verifier.daily;

import com.clanhq.verifier.feature.ClanHQFeature;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;

/** Developer-mode monitor for real RuneLite activity detections. */
public final class ActivityTelemetryTestFeature extends JPanel
    implements ClanHQFeature
{
    private static final Color DETECTED_GREEN = new Color(0x70C090);
    private static final TestActivity[] ACTIVITIES = {
        new TestActivity("pest_control_game", "Pest Control"),
        new TestActivity("hunter_rumour", "Hunter Rumours"),
        new TestActivity("fishing_trawler_game", "Fishing Trawler"),
        new TestActivity("barbarian_assault_wave", "Barbarian Assault"),
        new TestActivity("giants_foundry_commission", "Giants' Foundry"),
        new TestActivity("mahogany_homes_contract", "Mahogany Homes"),
        new TestActivity("agility_lap", "Agility Courses"),
        new TestActivity("tithe_farm_fruit_deposited", "Tithe Farm"),
    };

    private final ActivityTelemetryDetector detector;
    private final Map<String, TestActivity> activitiesByKey =
        new LinkedHashMap<>();
    private final Map<String, Integer> detections = new LinkedHashMap<>();
    private final Map<String, JLabel> activityStatuses =
        new LinkedHashMap<>();
    private final JLabel resultLabel = new JLabel();
    private final JCheckBox enabledCheck = new JCheckBox("Enable testing");
    private final JButton resetButton = new JButton("Reset");

    public ActivityTelemetryTestFeature(ActivityTelemetryDetector detector)
    {
        this.detector = detector;
        for (TestActivity activity : ACTIVITIES)
        {
            activitiesByKey.put(activity.key, activity);
        }
        buildPanel();
    }

    @Override
    public String getId()
    {
        return "activity-test";
    }

    @Override
    public String getDisplayName()
    {
        return "Activity Test";
    }

    @Override
    public String getDescription()
    {
        return "Monitor real RuneLite-backed daily-task signals.";
    }

    @Override
    public String getNavigationIconResource()
    {
        return "/com/clanhq/verifier/icons/dailies.png";
    }

    @Override
    public JPanel getPanel()
    {
        return this;
    }

    @Override
    public void shutDown()
    {
        detector.setTestObserver(null);
    }

    private void buildPanel()
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("Activity Test");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel explanation = new JLabel(
            "<html><body style='width: 180px'>Enable testing, then complete "
                + "an activity in game. Detected signals appear below even "
                + "when that task is not assigned.</body></html>");
        explanation.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        explanation.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(explanation);
        content.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel warning = new JLabel(
            "<html><body style='width: 180px'>While enabled, detected "
                + "activity signals stay local and are not submitted to "
                + "ClanHQ.</body></html>");
        warning.setForeground(new Color(0xD9A85C));
        warning.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(warning);
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        enabledCheck.setOpaque(false);
        enabledCheck.setForeground(Color.WHITE);
        enabledCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        enabledCheck.addActionListener(event -> updateTestingState());
        content.add(enabledCheck);
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        resetButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetButton.addActionListener(event -> reset());
        content.add(resetButton);
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        for (TestActivity activity : ACTIVITIES)
        {
            content.add(createActivityRow(activity));
            content.add(Box.createRigidArea(new Dimension(0, 7)));
        }

        resultLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultLabel.setText("Testing is off.");
        content.add(resultLabel);
        add(content, BorderLayout.NORTH);
    }

    private JPanel createActivityRow(TestActivity activity)
    {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel name = new JLabel(activity.displayName);
        name.setForeground(Color.WHITE);
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(name);

        JLabel status = new JLabel("Waiting for gameplay signal");
        status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        activityStatuses.put(activity.key, status);
        row.add(status);
        return row;
    }

    private void updateTestingState()
    {
        if (enabledCheck.isSelected())
        {
            detector.setTestObserver(this::detected);
            resultLabel.setText("Listening for gameplay signals...");
        }
        else
        {
            detector.setTestObserver(null);
            resultLabel.setText("Testing is off. Normal submission resumed.");
        }
    }

    private void detected(String activityKey, int quantity,
        Map<String, String> metadata)
    {
        Runnable update = () -> applyDetection(activityKey, quantity);
        if (SwingUtilities.isEventDispatchThread())
        {
            update.run();
        }
        else
        {
            SwingUtilities.invokeLater(update);
        }
    }

    private void applyDetection(String activityKey, int quantity)
    {
        TestActivity activity = activitiesByKey.get(activityKey);
        if (activity == null)
        {
            return;
        }
        int total = detections.getOrDefault(activityKey, 0)
            + Math.max(1, quantity);
        detections.put(activityKey, total);
        JLabel status = activityStatuses.get(activityKey);
        status.setText("DETECTED  +" + total);
        status.setForeground(DETECTED_GREEN);
        resultLabel.setText("<html>Detected " + activity.displayName
            + " (" + Math.max(1, quantity) + ").</html>");
    }

    private void reset()
    {
        detections.clear();
        activityStatuses.values().forEach(status ->
        {
            status.setText("Waiting for gameplay signal");
            status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        });
        resultLabel.setText(enabledCheck.isSelected()
            ? "Detection log cleared. Still listening..."
            : "Detection log cleared. Testing is off.");
    }

    private static final class TestActivity
    {
        private final String key;
        private final String displayName;

        private TestActivity(String key, String displayName)
        {
            this.key = key;
            this.displayName = displayName;
        }
    }
}
