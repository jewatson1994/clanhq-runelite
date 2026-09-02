package com.clanhq.verifier.daily;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ActivityTelemetryTestFeatureTest
{
    @Test
    public void monitorsRealSignalsWithoutAssignmentOrSubmission()
        throws Exception
    {
        List<String> submissions = new ArrayList<>();
        ActivityTelemetryDetector detector = new ActivityTelemetryDetector(
            (rsn, activity, quantity, metadata) -> submissions.add(activity),
            () -> "Mr Dimples");
        ActivityTelemetryTestFeature feature =
            new ActivityTelemetryTestFeature(detector);

        SwingUtilities.invokeAndWait(() ->
        {
            JCheckBox enabled = findButton(feature, JCheckBox.class,
                "Enable testing");
            JButton reset = findButton(feature, JButton.class, "Reset");
            Container titheRow = findLabelParent(feature, "Tithe Farm");

            assertNotNull(enabled);
            assertNotNull(reset);
            assertNotNull(titheRow);
            assertNull(findButton(feature, JButton.class, "Add +1"));

            enabled.doClick();
            detector.onTitheFruitDeposited(4);
            assertTrue(hasLabelContaining(titheRow, "DETECTED  +4"));
            assertEquals(0, submissions.size());

            reset.doClick();
            assertTrue(hasLabelContaining(
                titheRow, "Waiting for gameplay signal"));

            enabled.doClick();
            detector.onTitheFruitDeposited(1);
        });

        assertEquals(1, submissions.size());
        assertEquals("tithe_farm_fruit_deposited", submissions.get(0));
    }

    private static Container findLabelParent(Container root, String text)
    {
        for (Component component : root.getComponents())
        {
            if (component instanceof JLabel
                && text.equals(((JLabel) component).getText()))
            {
                return component.getParent();
            }
            if (component instanceof Container)
            {
                Container nested = findLabelParent((Container) component, text);
                if (nested != null)
                {
                    return nested;
                }
            }
        }
        return null;
    }

    private static boolean hasLabelContaining(Container root, String text)
    {
        for (Component component : root.getComponents())
        {
            if (component instanceof JLabel
                && ((JLabel) component).getText().contains(text))
            {
                return true;
            }
        }
        return false;
    }

    private static <T extends AbstractButton> T findButton(Container root,
        Class<T> type, String text)
    {
        if (root == null)
        {
            return null;
        }
        for (Component component : root.getComponents())
        {
            if (type.isInstance(component)
                && text.equals(((AbstractButton) component).getText()))
            {
                return type.cast(component);
            }
            if (component instanceof Container)
            {
                T nested = findButton((Container) component, type, text);
                if (nested != null)
                {
                    return nested;
                }
            }
        }
        return null;
    }
}
