package fr.openmc.core.features.earth;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import fr.openmc.core.features.earth.models.*;

import java.sql.SQLException;

/**
 * Applies additive Earth data migrations in a durable, ordered manner.
 * No migration drops or rewrites existing OpenMC-owned city, home, shop, or economy data.
 */
public final class EarthSchemaManager {
    public static final int CURRENT_VERSION = 11;

    private EarthSchemaManager() { }

    public static void migrate(ConnectionSource connectionSource) throws SQLException {
        TableUtils.createTableIfNotExists(connectionSource, DBEarthSchemaVersion.class);
        Dao<DBEarthSchemaVersion, String> versions = DaoManager.createDao(connectionSource, DBEarthSchemaVersion.class);
        DBEarthSchemaVersion schema = versions.queryForId(DBEarthSchemaVersion.ID);
        if (schema == null) {
            schema = new DBEarthSchemaVersion(0, System.currentTimeMillis());
            versions.create(schema);
        }
        if (schema.getVersion() > CURRENT_VERSION) {
            throw new SQLException("Earth database schema " + schema.getVersion()
                    + " is newer than this plugin supports (" + CURRENT_VERSION + ")");
        }

        while (schema.getVersion() < CURRENT_VERSION) {
            int targetVersion = schema.getVersion() + 1;
            applyMigration(connectionSource, targetVersion);
            schema.advanceTo(targetVersion, System.currentTimeMillis());
            versions.update(schema);
        }
    }

    private static void applyMigration(ConnectionSource connectionSource, int targetVersion) throws SQLException {
        switch (targetVersion) {
            case 1 -> createTables(connectionSource,
                    DBEarthCountry.class, DBEarthRegion.class, DBEarthCityLink.class, DBEarthCitizenship.class,
                    DBEarthRegionState.class, DBEarthCountryPolicy.class, DBEarthProperty.class, DBEarthBusiness.class,
                    DBEarthRegionalLedger.class, DBEarthJob.class, DBEarthLaw.class);
            case 2 -> createTables(connectionSource,
                    DBEarthCitizenStatus.class, DBEarthElectionCandidate.class, DBEarthElectionVote.class,
                    DBEarthCountryOffice.class, DBEarthElectionCycle.class);
            case 3 -> createTables(connectionSource,
                    DBEarthRegionalEvent.class, DBEarthTransportRoute.class, DBEarthTravelPass.class,
                    DBEarthDemographics.class, DBEarthRegionWaypoint.class, DBEarthDiplomaticRelation.class);
            case 4 -> createTables(connectionSource, DBEarthRegionalIndicators.class);
            case 5 -> createTables(connectionSource,
                    DBEarthCountryStatistics.class, DBEarthPropertyAccess.class, DBEarthSimulationClock.class);
            case 6 -> addCountryMetadataColumns(connectionSource);
            case 7 -> createTables(connectionSource, DBEarthGovernment.class);
            case 8 -> addPartyTablesAndElectionAffiliation(connectionSource);
            case 9 -> createTables(connectionSource, DBEarthBill.class);
            case 10 -> createTables(connectionSource, DBEarthGovernmentAppointment.class);
            case 11 -> createTables(connectionSource, DBEarthPropertySaleOffer.class);
            default -> throw new SQLException("Unknown Earth schema migration " + targetVersion);
        }
    }

    private static void createTables(ConnectionSource connectionSource, Class<?>... types) throws SQLException {
        for (Class<?> type : types) {
            TableUtils.createTableIfNotExists(connectionSource, type);
        }
    }

    /**
     * Version 6 enriches pre-existing country rows without replacing them. H2,
     * MySQL 8+, and MariaDB support this additive IF NOT EXISTS form.
     */
    private static void addCountryMetadataColumns(ConnectionSource connectionSource) throws SQLException {
        Dao<DBEarthCountry, String> countries = DaoManager.createDao(connectionSource, DBEarthCountry.class);
        countries.executeRaw("ALTER TABLE earth_countries ADD COLUMN IF NOT EXISTS code VARCHAR(8) NOT NULL DEFAULT ''");
        countries.executeRaw("ALTER TABLE earth_countries ADD COLUMN IF NOT EXISTS governmentId VARCHAR(128) NOT NULL DEFAULT ''");
        countries.executeRaw("ALTER TABLE earth_countries ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE");
        countries.executeRaw("ALTER TABLE earth_countries ADD COLUMN IF NOT EXISTS createdAt BIGINT NOT NULL DEFAULT 0");
        countries.executeRaw("ALTER TABLE earth_countries ADD COLUMN IF NOT EXISTS updatedAt BIGINT NOT NULL DEFAULT 0");
    }

    private static void addPartyTablesAndElectionAffiliation(ConnectionSource connectionSource) throws SQLException {
        createTables(connectionSource, DBEarthPoliticalParty.class, DBEarthPartyMembership.class,
                DBEarthElectionCandidate.class, DBEarthGovernment.class);
        Dao<DBEarthElectionCandidate, String> candidates = DaoManager.createDao(connectionSource, DBEarthElectionCandidate.class);
        candidates.executeRaw("ALTER TABLE earth_election_candidates ADD COLUMN IF NOT EXISTS party_id VARCHAR(192)");
        Dao<DBEarthGovernment, String> governments = DaoManager.createDao(connectionSource, DBEarthGovernment.class);
        governments.executeRaw("ALTER TABLE earth_governments ADD COLUMN IF NOT EXISTS ruling_party_id VARCHAR(192)");
    }
}
