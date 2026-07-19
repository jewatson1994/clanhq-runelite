package com.clanhq.verifier.service;

import com.clanhq.verifier.ClanHQVerifierConfig;
import java.awt.Component;
import javax.swing.JOptionPane;

/** Presents the explicit disclosure required before complete character data leaves RuneLite. */
public final class SubmissionConsentService
{
    private final ClanHQVerifierConfig config;
    private final ApiDestinationService destinationService;

    public SubmissionConsentService(
        ClanHQVerifierConfig config,
        ApiDestinationService destinationService)
    {
        this.config = config;
        this.destinationService = destinationService;
    }

    public boolean confirm(Component parent, String purpose)
    {
        String destination = destinationService.describe(config.apiBaseUrl())
            .replace("Submits to: ", "");
        String message = "Submit your complete bank, inventory, and equipped "
            + "item IDs and quantities to " + destination + " for "
            + purpose + "?\n\nClanHQ will retain the submitted snapshot "
            + "for auditing and future server-side verification.";
        return JOptionPane.showConfirmDialog(
            parent,
            message,
            "Confirm Character Submission",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }
}
