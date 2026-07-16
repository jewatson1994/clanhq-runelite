package com.clanhq.verifier.feature;

import javax.swing.JComponent;

/** Exposes the existing review workflow as a ClanHQ shell feature. */
public final class RankReviewFeature implements ClanHQFeature
{
    private final JComponent panel;

    public RankReviewFeature(JComponent panel)
    {
        this.panel = panel;
    }

    @Override
    public String getId()
    {
        return "rank-review";
    }

    @Override
    public String getDisplayName()
    {
        return "Rank Review";
    }

    @Override
    public String getDescription()
    {
        return "Capture account evidence for an Iron Drop rank review.";
    }

    @Override
    public JComponent getPanel()
    {
        return panel;
    }
}
