package com.clanhq.verifier.model;

import java.util.Objects;

public final class RequirementResult
{
    private final String name;
    private final RequirementStatus status;
    private final String detail;

    public RequirementResult(
        String name,
        RequirementStatus status,
        String detail)
    {
        this.name = Objects.requireNonNull(name);
        this.status = Objects.requireNonNull(status);
        this.detail = Objects.requireNonNull(detail);
    }

    public String getName()
    {
        return name;
    }

    public RequirementStatus getStatus()
    {
        return status;
    }

    public String getDetail()
    {
        return detail;
    }
}
