package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** A non-destructive Earth overlay on top of existing city claims. */
@Getter
@DatabaseTable(tableName = "earth_properties")
public class DBEarthProperty {
    @DatabaseField(id = true, columnName = "property_uuid")
    private UUID propertyId;
    @DatabaseField(canBeNull = false, columnName = "owner_uuid")
    private UUID ownerId;
    @DatabaseField(columnName = "city_uuid")
    private UUID cityId;
    @DatabaseField(canBeNull = false, columnName = "region_id")
    private String regionId;
    @DatabaseField(canBeNull = false)
    private String world;
    @DatabaseField(canBeNull = false)
    private int chunkX;
    @DatabaseField(canBeNull = false)
    private int chunkZ;
    @DatabaseField(canBeNull = false)
    private String propertyType;
    @DatabaseField(canBeNull = false)
    private double assessedValue;
    @DatabaseField(canBeNull = false, columnName = "created_at")
    private long createdAt;

    DBEarthProperty() {
        // ORMLite
    }

    public DBEarthProperty(UUID propertyId, UUID ownerId, UUID cityId, String regionId, String world,
                           int chunkX, int chunkZ, EarthPropertyType propertyType, double assessedValue, long createdAt) {
        this.propertyId = propertyId;
        this.ownerId = ownerId;
        this.cityId = cityId;
        this.regionId = regionId;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.propertyType = propertyType.name();
        this.assessedValue = assessedValue;
        this.createdAt = createdAt;
    }

    public EarthPropertyType getType() {
        return EarthPropertyType.valueOf(propertyType);
    }

    public void transferTo(UUID newOwnerId, double newAssessedValue) {
        this.ownerId = newOwnerId;
        this.assessedValue = Math.max(0D, newAssessedValue);
    }
}
