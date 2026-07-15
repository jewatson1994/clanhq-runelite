package com.clanhq.verifier.model;

public enum RequirementStatus
{
    PASSED("[PASS]"),
    MISSING("[MISS]"),
    UNVERIFIED("[CHECK]");

    private final String symbol;

    RequirementStatus(String symbol)
    {
        this.symbol = symbol;
    }

    public String getSymbol()
    {
        return symbol;
    }
}
