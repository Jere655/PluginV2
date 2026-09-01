package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

@Getter
@DatabaseTable(tableName = "earth_citizenships")
public class DBEarthCitizenship {
    @DatabaseField(id = true, columnName = "player_uuid")
    private UUID playerId;
    @DatabaseField(canBeNull = false, columnName = "country_id")
    private String countryId;
    @DatabaseField(columnName = "home_region_id")
    private String homeRegionId;
    @DatabaseField(canBeNull = false, columnName = "granted_at")
    private long grantedAt;

    DBEarthCitizenship() {
        // ORMLite
    }

    public DBEarthCitizenship(UUID playerId, String countryId, String homeRegionId, long grantedAt) {
        this.playerId = playerId;
        this.countryId = countryId;
        this.homeRegionId = homeRegionId;
        this.grantedAt = grantedAt;
    }
}
