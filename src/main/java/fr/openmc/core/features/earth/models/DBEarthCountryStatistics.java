package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

/** Persisted aggregate state derived from a country's bounded regional simulation. */
@Getter
@DatabaseTable(tableName = "earth_country_statistics")
public class DBEarthCountryStatistics {
    @DatabaseField(id = true, columnName = "country_id")
    private String countryId;
    @DatabaseField(canBeNull = false)
    private double treasury;
    @DatabaseField(canBeNull = false)
    private int population;
    @DatabaseField(canBeNull = false)
    private int citizenCount;
    @DatabaseField(canBeNull = false)
    private double economicState;
    @DatabaseField(canBeNull = false)
    private boolean active;
    @DatabaseField(canBeNull = false)
    private long updatedAt;

    DBEarthCountryStatistics() { }

    public DBEarthCountryStatistics(String countryId, long now) {
        this.countryId = countryId;
        this.active = true;
        this.updatedAt = now;
    }

    public void refresh(double treasury, int population, int citizenCount, double economicState, long now) {
        this.treasury = Math.max(0D, treasury);
        this.population = Math.max(0, population);
        this.citizenCount = Math.max(0, citizenCount);
        this.economicState = Math.max(0D, Math.min(1D, economicState));
        this.updatedAt = now;
    }

    public void setActive(boolean active, long now) {
        this.active = active;
        this.updatedAt = now;
    }
}
