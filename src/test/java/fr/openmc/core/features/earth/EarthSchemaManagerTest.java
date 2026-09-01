package fr.openmc.core.features.earth;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import fr.openmc.core.features.earth.models.DBEarthCountry;
import fr.openmc.core.features.earth.models.DBEarthGovernmentAppointment;
import fr.openmc.core.features.earth.models.DBEarthSchemaVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EarthSchemaManagerTest {
    @Test
    void additiveMigrationsAreVersionedIdempotentAndRetainEarthRecords() throws Exception {
        try (JdbcConnectionSource source = new JdbcConnectionSource("jdbc:h2:mem:earth-schema;DB_CLOSE_DELAY=-1")) {
            EarthSchemaManager.migrate(source);

            Dao<DBEarthSchemaVersion, String> versions = DaoManager.createDao(source, DBEarthSchemaVersion.class);
            Dao<DBEarthCountry, String> countries = DaoManager.createDao(source, DBEarthCountry.class);
            Dao<DBEarthGovernmentAppointment, String> appointments = DaoManager.createDao(source, DBEarthGovernmentAppointment.class);
            countries.create(new DBEarthCountry("canada", "Canada", "Ottawa", "CAD", "federal_democracy", .12D));

            EarthSchemaManager.migrate(source);

            assertEquals(EarthSchemaManager.CURRENT_VERSION, versions.queryForId(DBEarthSchemaVersion.ID).getVersion());
            assertNotNull(countries.queryForId("canada"));
            assertNotNull(appointments);
        }
    }

    @Test
    void countryMetadataMigrationPreservesLegacyCountryRows() throws Exception {
        try (JdbcConnectionSource source = new JdbcConnectionSource("jdbc:h2:mem:earth-country-v5;DB_CLOSE_DELAY=-1")) {
            TableUtils.createTableIfNotExists(source, LegacyCountry.class);
            TableUtils.createTableIfNotExists(source, DBEarthSchemaVersion.class);
            DaoManager.createDao(source, LegacyCountry.class).create(new LegacyCountry("canada", "Canada", "Ottawa", "CAD", "federal", .12D));
            DaoManager.createDao(source, DBEarthSchemaVersion.class).create(new DBEarthSchemaVersion(5, 10L));

            EarthSchemaManager.migrate(source);

            Dao<DBEarthCountry, String> countries = DaoManager.createDao(source, DBEarthCountry.class);
            Dao<DBEarthSchemaVersion, String> versions = DaoManager.createDao(source, DBEarthSchemaVersion.class);
            DBEarthCountry country = countries.queryForId("canada");
            assertEquals("Canada", country.getName());
            assertEquals("", country.getCode());
            assertEquals("", country.getGovernmentId());
            assertEquals(EarthSchemaManager.CURRENT_VERSION,
                    versions.queryForId(DBEarthSchemaVersion.ID).getVersion());
        }
    }

    @DatabaseTable(tableName = "earth_countries")
    public static class LegacyCountry {
        @DatabaseField(id = true)
        private String id;
        @DatabaseField(canBeNull = false)
        private String name;
        @DatabaseField(canBeNull = false)
        private String capital;
        @DatabaseField(canBeNull = false)
        private String currency;
        @DatabaseField(canBeNull = false)
        private String governmentType;
        @DatabaseField(canBeNull = false)
        private double taxRate;

        LegacyCountry() { }

        LegacyCountry(String id, String name, String capital, String currency, String governmentType, double taxRate) {
            this.id = id;
            this.name = name;
            this.capital = capital;
            this.currency = currency;
            this.governmentType = governmentType;
            this.taxRate = taxRate;
        }
    }
}
