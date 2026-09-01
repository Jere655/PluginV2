package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** One active player occupation; a retail job may reference an Earth business/shop. */
@Getter
@DatabaseTable(tableName = "earth_jobs")
public class DBEarthJob {
    @DatabaseField(id = true, columnName = "player_uuid")
    private UUID playerId;
    @DatabaseField(canBeNull = false)
    private String occupation;
    @DatabaseField(canBeNull = false, columnName = "region_id")
    private String regionId;
    @DatabaseField(columnName = "employer_shop_uuid")
    private UUID employerShopId;
    @DatabaseField(canBeNull = false)
    private long workUnits;
    @DatabaseField(canBeNull = false, columnName = "last_worked_at")
    private long lastWorkedAt;
    @DatabaseField(canBeNull = false, columnName = "started_at")
    private long startedAt;

    DBEarthJob() {
        // ORMLite
    }

    public DBEarthJob(UUID playerId, EarthOccupation occupation, String regionId, UUID employerShopId, long startedAt) {
        this.playerId = playerId;
        this.occupation = occupation.name();
        this.regionId = regionId;
        this.employerShopId = employerShopId;
        this.startedAt = startedAt;
    }

    public EarthOccupation getOccupationType() {
        return EarthOccupation.valueOf(occupation);
    }

    public boolean canWork(long now, long cooldownMillis) {
        return now - lastWorkedAt >= cooldownMillis;
    }

    public void recordWork(long now) {
        workUnits++;
        lastWorkedAt = now;
    }
}
