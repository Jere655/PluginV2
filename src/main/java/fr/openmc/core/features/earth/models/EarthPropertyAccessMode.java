package fr.openmc.core.features.earth.models;

/** Access intent for the Earth overlay; existing city claims remain enforcement authority. */
public enum EarthPropertyAccessMode {
    PUBLIC,
    VISITABLE,
    PRIVATE,
    BUSINESS,
    GOVERNMENT
}
