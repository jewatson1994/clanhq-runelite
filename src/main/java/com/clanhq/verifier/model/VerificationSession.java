package com.clanhq.verifier.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class VerificationSession
{
    private final Set<EvidenceStage> requiredStages;
    private final Map<EvidenceStage, EvidenceStageStatus> statuses =
        new EnumMap<>(EvidenceStage.class);
    private String rsn;

    public VerificationSession(Set<EvidenceStage> requiredStages)
    {
        this.requiredStages = Collections.unmodifiableSet(
            EnumSet.copyOf(requiredStages));
        for (EvidenceStage stage : EvidenceStage.values())
        {
            statuses.put(stage, EvidenceStageStatus.NOT_CAPTURED);
        }
    }

    public void bindRsn(String capturedRsn)
    {
        if (rsn != null && !rsn.equals(capturedRsn))
        {
            throw new IllegalStateException(
                "Verification evidence belongs to a different character.");
        }
        rsn = Objects.requireNonNull(capturedRsn);
    }

    public void setStatus(EvidenceStage stage, EvidenceStageStatus status)
    {
        statuses.put(stage, status);
    }

    public String getRsn() { return rsn; }
    public Set<EvidenceStage> getRequiredStages() { return requiredStages; }
    public EvidenceStageStatus getStatus(EvidenceStage stage) { return statuses.get(stage); }

    public boolean isReadyForSubmission()
    {
        return rsn != null && requiredStages.stream()
            .allMatch(stage -> statuses.get(stage).isSubmissionReady());
    }
}
