package com.clanhq.verifier.character;

import com.clanhq.verifier.bingo.model.BingoCharacterSubmission;
import com.clanhq.verifier.feature.ClanHQFeature;
import com.clanhq.verifier.service.LocalPlayerSnapshotService;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;

public final class CharacterSyncFeature implements ClanHQFeature
{
    private final CharacterSyncApiClient apiClient;
    private final LocalPlayerSnapshotService snapshotService;
    private final ClientThread clientThread;
    private final CharacterSyncPanel panel;
    private volatile boolean running;
    private volatile BingoCharacterSubmission pending;

    public CharacterSyncFeature(CharacterSyncApiClient apiClient,
        LocalPlayerSnapshotService snapshotService, ClientThread clientThread)
    {
        this.apiClient = apiClient;
        this.snapshotService = snapshotService;
        this.clientThread = clientThread;
        this.panel = new CharacterSyncPanel(this::submit);
    }

    public String getId() { return "character"; }
    public String getDisplayName() { return "Character"; }
    public String getNavigationIconResource()
    {
        return "/com/clanhq/verifier/icons/character.png";
    }
    public String getDescription() { return "Synchronize complete character item data."; }
    public JComponent getPanel() { return panel; }
    public void startUp() { running = true; }
    public void shutDown() { running = false; pending = null; }

    public void submit()
    {
        if (!running) { return; }
        panel.setSubmitting();
        if (pending != null)
        {
            deliver(pending);
            return;
        }
        clientThread.invokeLater(() ->
        {
            try
            {
                pending = new BingoCharacterSubmission(
                    "CHARACTER-SYNC",
                    snapshotService.captureCompleteItemsEvidence());
                deliver(pending);
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() -> panel.showResult(
                    false, exception.getMessage()));
            }
        });
    }

    private void deliver(BingoCharacterSubmission submission)
    {
        apiClient.submit(submission).thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                if (!running) { return; }
                if (result.isSuccessful()) { pending = null; }
                panel.showResult(result.isSuccessful(), result.getMessage());
            }));
    }
}
