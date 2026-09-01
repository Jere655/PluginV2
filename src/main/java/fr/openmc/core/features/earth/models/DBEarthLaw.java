package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** One active legal instrument for a country. */
@Getter
@DatabaseTable(tableName = "earth_laws")
public class DBEarthLaw {
    @DatabaseField(id = true)
    private String id;
    @DatabaseField(canBeNull = false, columnName = "country_id")
    private String countryId;
    @DatabaseField(canBeNull = false)
    private String lawType;
    @DatabaseField(columnName = "enacted_by")
    private UUID enactedBy;
    @DatabaseField(canBeNull = false, columnName = "enacted_at")
    private long enactedAt;

    DBEarthLaw() {
        // ORMLite
    }

    public DBEarthLaw(String countryId, EarthLawType lawType, UUID enactedBy, long enactedAt) {
        this.countryId = countryId;
        this.lawType = lawType.name();
        this.id = idFor(countryId, lawType);
        this.enactedBy = enactedBy;
        this.enactedAt = enactedAt;
    }

    public EarthLawType getType() {
        return EarthLawType.valueOf(lawType);
    }

    public static String idFor(String countryId, EarthLawType type) {
        return countryId.toLowerCase(java.util.Locale.ROOT) + ":" + type.name();
    }
}
