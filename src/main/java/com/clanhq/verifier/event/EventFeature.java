package com.clanhq.verifier.event;

import com.clanhq.verifier.event.transport.EventApiClient;
import com.clanhq.verifier.event.model.ClanEventSummary;
import com.clanhq.verifier.feature.ClanHQFeature;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public final class EventFeature implements ClanHQFeature
{
    private static final Duration SKILL_SUBMISSION_INTERVAL =
        Duration.ofSeconds(60);
    private final EventApiClient apiClient;
    private final EventPanel panel;
    private final Supplier<String> rsnSupplier;
    private volatile boolean running;
    private volatile ClanEventSummary currentEvent;
    private Instant lastSkillSubmission;

    public EventFeature(EventApiClient apiClient, Supplier<String> rsnSupplier)
    {
        this.apiClient = apiClient;
        this.rsnSupplier = rsnSupplier;
        this.panel = new EventPanel(this::refresh);
    }

    @Override
    public String getId()
    {
        return "events";
    }

    @Override
    public String getDisplayName()
    {
        return "Events";
    }

    @Override
    public String getNavigationIconResource()
    {
        return "/com/clanhq/verifier/icons/events.png";
    }

    @Override
    public String getDescription()
    {
        return "View the ClanHQ event associated with an event code.";
    }

    @Override
    public JComponent getPanel()
    {
        return panel;
    }

    @Override
    public void startUp()
    {
        running = true;
        refresh();
    }

    @Override
    public void shutDown()
    {
        running = false;
        currentEvent = null;
        lastSkillSubmission = null;
    }

    public void refresh()
    {
        panel.setLoading();
        apiClient.fetchCurrentEvent().thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                if (!running)
                {
                    return;
                }
                result.getEvent().ifPresentOrElse(
                    event -> showAndJoin(event),
                    () -> panel.showError(result.getMessage()));
            }));
    }

    private void showAndJoin(
        ClanEventSummary event)
    {
        currentEvent = event;
        panel.showEvent(event);
        String rsn = rsnSupplier.get();
        if (rsn == null || rsn.trim().isEmpty())
        {
            panel.showParticipation(
                false,
                "Log in to join this event.",
                null);
            return;
        }
        apiClient.joinEvent(event, rsn).thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                if (running)
                {
                    panel.showParticipation(
                        result.isJoined(),
                        result.getMessage(),
                        result.getTeamName());
                }
            }));
    }

    public void onSkillExperience(String skillName, int experience)
    {
        ClanEventSummary event = currentEvent;
        Instant now = Instant.now();
        if (event == null || !event.isActive() || !event.isSkillEvent()
            || !matches(event.getTarget(), skillName)
            || (lastSkillSubmission != null
                && Duration.between(lastSkillSubmission, now)
                    .compareTo(SKILL_SUBMISSION_INTERVAL) < 0))
        {
            return;
        }
        lastSkillSubmission = now;
        submitObservation(event, "SKILL_XP", experience);
    }

    public void onLoot(String sourceName)
    {
        ClanEventSummary event = currentEvent;
        if (event == null || !event.isActive() || !event.isBossEvent()
            || !matches(event.getTarget(), sourceName))
        {
            return;
        }
        submitObservation(event, "BOSS_KILL", 1);
    }

    private void submitObservation(
        ClanEventSummary event,
        String metricType,
        int value)
    {
        String rsn = rsnSupplier.get();
        if (rsn == null || rsn.trim().isEmpty())
        {
            return;
        }
        apiClient.submitObservation(
            event,
            rsn,
            metricType,
            event.getTarget(),
            value).thenAccept(result -> SwingUtilities.invokeLater(() ->
            {
                if (running)
                {
                    panel.showObservation(
                        result.isRecorded(),
                        event.getTarget(),
                        result.getMessage());
                }
            }));
    }

    private static boolean matches(String expected, String actual)
    {
        return expected != null && actual != null
            && normalize(expected).equals(normalize(actual));
    }

    private static String normalize(String value)
    {
        return value.replace('_', ' ').trim().toLowerCase(Locale.ENGLISH);
    }
}
