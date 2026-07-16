package com.clanhq.verifier.feature;

import javax.swing.JComponent;

/** A self-contained feature displayed by the ClanHQ RuneLite shell. */
public interface ClanHQFeature
{
    String getId();

    String getDisplayName();

    String getDescription();

    JComponent getPanel();

    default void startUp()
    {
    }

    default void shutDown()
    {
    }
}
