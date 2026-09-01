package fr.openmc.core.features.earth.commands;

/**
 * LuckPerms-compatible permission nodes for the Earth simulation command surface.
 * Nodes are intentionally declared in one place so server roles can be reviewed
 * without reverse-engineering command handlers.
 */
public final class EarthPermissions {
    public static final String USE = "omc.commands.earth";
    public static final String CITIZENSHIP = "omc.commands.earth.citizenship";
    public static final String CIVIC = "omc.commands.earth.civic";
    public static final String ELECTION_NOMINATE = "omc.commands.earth.election.nominate";
    public static final String ELECTION_VOTE = "omc.commands.earth.election.vote";
    public static final String PROPERTY_MANAGE = "omc.commands.earth.property.manage";
    public static final String PROPERTY_PURCHASE = "omc.commands.earth.property.purchase";
    public static final String PROPERTY_RENT = "omc.commands.earth.property.rent";
    public static final String JOB_TAKE = "omc.commands.earth.job.take";
    public static final String JOB_WORK = "omc.commands.earth.job.work";
    public static final String TRAVEL = "omc.commands.earth.travel";
    public static final String GOVERNMENT_MANAGE = "omc.commands.earth.government.manage";
    public static final String PARTY_MANAGE = "omc.commands.earth.party.manage";

    // Match the existing paper-plugin.yml administration wildcard.
    public static final String ADMIN_ELECTION = "omc.admins.commands.earth.election";
    public static final String ADMIN_EVENT = "omc.admins.commands.earth.event";
    public static final String ADMIN_POPULATION = "omc.admins.commands.earth.population";
    public static final String ADMIN_DIPLOMACY = "omc.admins.commands.earth.diplomacy";
    public static final String ADMIN_DIAGNOSE = "omc.admins.commands.earth.diagnose";
    public static final String ADMIN_SIMULATION = "omc.admins.commands.earth.simulation";
    public static final String ADMIN_TRANSPORT = "omc.admins.commands.earth.transport";
    public static final String ADMIN_GOVERNMENT = "omc.admins.commands.earth.government";
    public static final String ADMIN_CITY = "omc.admins.commands.earth.city";

    private EarthPermissions() { }
}
