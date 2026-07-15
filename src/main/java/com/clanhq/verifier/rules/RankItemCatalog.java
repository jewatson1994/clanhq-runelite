package com.clanhq.verifier.rules;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

public final class RankItemCatalog
{
    private final Set<Integer> relevantItemIds = new HashSet<>();

    @Inject
    public RankItemCatalog()
    {
        relevantItemIds.addAll(OpalItemRequirements.allItemIds());
    }

    public boolean isRelevant(int itemId)
    {
        return relevantItemIds.contains(itemId);
    }
}
