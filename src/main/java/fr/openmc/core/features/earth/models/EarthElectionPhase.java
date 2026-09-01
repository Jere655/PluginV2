package fr.openmc.core.features.earth.models;

/** Persisted election phases; never derived from server uptime. */
public enum EarthElectionPhase {
    CANDIDACY,
    CAMPAIGN,
    VOTING,
    MANDATE
}
