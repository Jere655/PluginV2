package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

@Getter
@DatabaseTable(tableName = "earth_countries")
public class DBEarthCountry {
    @DatabaseField(id = true, canBeNull = false)
    private String id;
    @DatabaseField(canBeNull = false)
    private String name;
    @DatabaseField(canBeNull = false, defaultValue = "")
    private String code;
    @DatabaseField(canBeNull = false)
    private String capital;
    @DatabaseField(canBeNull = false)
    private String currency;
    @DatabaseField(canBeNull = false)
    private String governmentType;
    @DatabaseField(canBeNull = false, defaultValue = "")
    private String governmentId;
    @DatabaseField(canBeNull = false)
    private double taxRate;
    @DatabaseField(canBeNull = false, defaultValue = "1")
    private boolean active;
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private long createdAt;
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private long updatedAt;

    DBEarthCountry() {
        // ORMLite
    }

    public DBEarthCountry(String id, String name, String capital, String currency, String governmentType, double taxRate) {
        this(id, id.toUpperCase(java.util.Locale.ROOT), name, capital, currency, governmentType, governmentType, taxRate, true,
                System.currentTimeMillis());
    }

    public DBEarthCountry(String id, String code, String name, String capital, String currency, String governmentType,
                          String governmentId, double taxRate, boolean active, long now) {
        this.id = id;
        this.code = code == null ? "" : code.toUpperCase(java.util.Locale.ROOT);
        this.name = name;
        this.capital = capital;
        this.currency = currency;
        this.governmentType = governmentType;
        this.governmentId = governmentId;
        this.taxRate = taxRate;
        this.active = active;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Applies operator-configured metadata without changing the immutable country identifier or creation time. */
    public void applyConfiguredMetadata(DBEarthCountry configured, long now) {
        this.code = configured.code;
        this.name = configured.name;
        this.capital = configured.capital;
        this.currency = configured.currency;
        this.governmentType = configured.governmentType;
        this.governmentId = configured.governmentId;
        this.taxRate = configured.taxRate;
        this.active = configured.active;
        this.updatedAt = now;
    }
}
