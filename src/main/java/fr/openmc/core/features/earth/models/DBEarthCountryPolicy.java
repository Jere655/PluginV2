package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

/** Persistent national budget policy applied by each regional simulation cycle. */
@Getter
@DatabaseTable(tableName = "earth_country_policies")
public class DBEarthCountryPolicy {
    @DatabaseField(id = true, columnName = "country_id")
    private String countryId;
    @DatabaseField(canBeNull = false)
    private double socialServices;
    @DatabaseField(canBeNull = false)
    private double infrastructure;
    @DatabaseField(canBeNull = false)
    private double investment;
    @DatabaseField(canBeNull = false, columnName = "updated_at")
    private long updatedAt;

    DBEarthCountryPolicy() {
        // ORMLite
    }

    public DBEarthCountryPolicy(String countryId, double socialServices, double infrastructure, double investment, long updatedAt) {
        this.countryId = countryId;
        setAllocations(socialServices, infrastructure, investment, updatedAt);
    }

    public void setAllocations(double socialServices, double infrastructure, double investment, long updatedAt) {
        double total = Math.max(0.0001D, socialServices + infrastructure + investment);
        this.socialServices = Math.max(0D, socialServices) / total;
        this.infrastructure = Math.max(0D, infrastructure) / total;
        this.investment = Math.max(0D, investment) / total;
        this.updatedAt = updatedAt;
    }
}
