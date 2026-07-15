package com.clanhq.verifier.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BoatConfiguration
{
    private final int slot;
    private final String type;
    private final String hull;
    private final String sail;
    private final String steering;
    private final String keel;

    public BoatConfiguration(int slot, String type, String hull, String sail,
        String steering, String keel)
    {
        this.slot = slot;
        this.type = Objects.requireNonNull(type);
        this.hull = Objects.requireNonNull(hull);
        this.sail = Objects.requireNonNull(sail);
        this.steering = Objects.requireNonNull(steering);
        this.keel = Objects.requireNonNull(keel);
    }

    public int getSlot() { return slot; }
    public String getType() { return type; }
    public String getHull() { return hull; }
    public String getSail() { return sail; }
    public String getSteering() { return steering; }
    public String getKeel() { return keel; }

    public boolean isType(String expected)
    {
        return normalize(type).contains(normalize(expected));
    }

    public boolean isMaxedCore()
    {
        return normalize(hull).contains("rosewood")
            && normalize(sail).contains("rosewood")
            && normalize(steering).contains("dragon")
            && normalize(keel).contains("dragon");
    }

    public List<String> getMissingMaxedComponents()
    {
        List<String> missing = new ArrayList<>();
        addIfMissing(missing, "hull", hull, "rosewood");
        addIfMissing(missing, "mast and sails", sail, "rosewood");
        addIfMissing(missing, "helm", steering, "dragon");
        addIfMissing(missing, "keel", keel, "dragon");
        return missing;
    }

    public String toSummary()
    {
        return type + " (slot " + slot + "): hull=" + hull
            + ", mast/sails=" + sail + ", helm=" + steering
            + ", keel=" + keel;
    }

    private static void addIfMissing(List<String> missing, String label,
        String actual, String required)
    {
        if (!normalize(actual).contains(required))
        {
            missing.add(label + "=" + actual);
        }
    }

    private static String normalize(String value)
    {
        return value.toLowerCase(Locale.ENGLISH);
    }
}
