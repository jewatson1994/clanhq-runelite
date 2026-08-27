package com.clanhq.verifier.event;

import com.clanhq.verifier.event.model.ClanEventSummary;
import com.clanhq.verifier.event.model.ClanEventsSnapshot;
import com.clanhq.verifier.event.transport.EventApiClient;
import com.clanhq.verifier.feature.ClanHQFeature;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private volatile List<ClanEventSummary> currentEvents =
        Collections.emptyList();
    private final Map<Long, Instant> lastSkillSubmissions = new HashMap<>();

    public EventFeature(EventApiClient apiClient, Supplier<String> rsnSupplier)
    {
        this.apiClient = apiClient;
        this.rsnSupplier = rsnSupplier;
        this.panel = new EventPanel(this::refresh);
    }

    @Override
    public String getId() { return "events"; }

    @Override
    public String getDisplayName() { return "Events"; }

    @Override
    public String getNavigationIconResource()
    {
        return "/com/clanhq/verifier/icons/events.png";
    }

    @Override
    public String getDescription()
    {
        return "View current ClanHQ events. Bingo linking is handled separately.";
    }

    @Override
    public JComponent getPanel() { return panel; }

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
        currentEvents = Collections.emptyList();
        synchronized (lastSkillSubmissions)
        {
            lastSkillSubmissions.clear();
        }
    }

    public void refresh()
    {
        panel.setLoading();
        apiClient.fetchEvents().thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                if (!running)
                {
                    return;
                }
                result.getSnapshot().ifPresentOrElse(
                    this::showEvents,
                    () -> panel.showError(result.getMessage()));
            }));
    }

    private void showEvents(ClanEventsSnapshot snapshot)
    {
        currentEvents = snapshot.getEvents();
        panel.showEvents(snapshot.getServerName(), currentEvents);
    }

    public void onSkillExperience(String skillName, int experience)
    {
        if (skillName == null || experience <= 0)
        {
            return;
        }
        Instant now = Instant.now();
        for (ClanEventSummary event : currentEvents)
        {
            if (!event.isActive() || !event.isSkillEvent()
                || !matches(event.getTarget(), skillName)
                || !skillSubmissionAllowed(event.getEventId(), now))
            {
                continue;
            }
            synchronized (lastSkillSubmissions)
            {
                lastSkillSubmissions.put(event.getEventId(), now);
            }
            submitObservation(event, "SKILL_XP", event.getTarget(), experience);
        }
    }

    public void onLoot(String sourceName)
    {
        if (sourceName == null || sourceName.trim().isEmpty())
        {
            return;
        }
        for (ClanEventSummary event : currentEvents)
        {
            if (event.isActive() && event.isBossEvent()
                && matches(event.getTarget(), sourceName))
            {
                submitObservation(event, "BOSS_KILL", event.getTarget(), 1);
            }
        }
    }

    private boolean skillSubmissionAllowed(long eventId, Instant now)
    {
        synchronized (lastSkillSubmissions)
        {
            Instant last = lastSkillSubmissions.get(eventId);
            return last == null
                || Duration.between(last, now)
                    .compareTo(SKILL_SUBMISSION_INTERVAL) >= 0;
        }
    }

    private void submitObservation(
        ClanEventSummary event,
        String metricType,
        String target,
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
            target,
            value).thenAccept(result -> SwingUtilities.invokeLater(() ->
            {
                if (running)
                {
                    panel.showObservation(
                        result.isRecorded(),
                        event.getName(),
                        target,
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
