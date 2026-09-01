package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

@Getter
@DatabaseTable(tableName = "earth_regions")
public class DBEarthRegion {
    @DatabaseField(id = true, canBeNull = false)
    private String id;
    @DatabaseField(canBeNull = false, columnName = "country_id")
    private String countryId;
    @DatabaseField(canBeNull = false)
    private String name;
    @DatabaseField(defaultValue = "-30000000")
    private int minChunkX;
    @DatabaseField(defaultValue = "30000000")
    private int maxChunkX;
    @DatabaseField(defaultValue = "-30000000")
    private int minChunkZ;
    @DatabaseField(defaultValue = "30000000")
    private int maxChunkZ;

    DBEarthRegion() {
        // ORMLite
    }

    public DBEarthRegion(String id, String countryId, String name, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
        this.id = id;
        this.countryId = countryId;
        this.name = name;
        this.minChunkX = minChunkX;
        this.maxChunkX = maxChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkZ = maxChunkZ;
    }

    public boolean containsChunk(int chunkX, int chunkZ) {
        return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    public boolean hasValidBounds() {
        return minChunkX <= maxChunkX && minChunkZ <= maxChunkZ;
    }

    /** Inclusive chunk rectangles intersect when neither axis is disjoint. */
    public boolean overlaps(DBEarthRegion other) {
        return other != null && maxChunkX >= other.minChunkX && minChunkX <= other.maxChunkX
                && maxChunkZ >= other.minChunkZ && minChunkZ <= other.maxChunkZ;
    }
}
