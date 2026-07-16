package com.clanhq.verifier.event;

import com.clanhq.verifier.event.transport.EventApiClient;
import com.clanhq.verifier.feature.ClanHQFeature;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.function.Supplier;

public final class EventFeature implements ClanHQFeature
{
    private final EventApiClient apiClient;
    private final EventPanel panel;
    private final Supplier<String> rsnSupplier;
    private volatile boolean running;

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
        com.clanhq.verifier.event.model.ClanEventSummary event)
    {
        panel.showEvent(event);
        String rsn = rsnSupplier.get();
        if (rsn == null || rsn.trim().isEmpty())
        {
            panel.showParticipation(false, "Log in to join this event.");
            return;
        }
        apiClient.joinEvent(event, rsn).thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                if (running)
                {
                    panel.showParticipation(
                        result.isJoined(),
                        result.getMessage());
                }
            }));
    }
}
