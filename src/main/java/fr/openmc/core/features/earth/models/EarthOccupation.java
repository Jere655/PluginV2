package fr.openmc.core.features.earth.models;

/** Initial occupations with bounded wage and regional productivity values. */
public enum EarthOccupation {
    RETAIL(8D, 20D),
    CIVIL_SERVICE(7D, 14D),
    CONSTRUCTION(9D, 18D),
    AGRICULTURE(6D, 16D);

    private final double wage;
    private final double productivity;

    EarthOccupation(double wage, double productivity) {
        this.wage = wage;
        this.productivity = productivity;
    }

    public double wage() {
        return wage;
    }

    public double productivity() {
        return productivity;
    }
}
