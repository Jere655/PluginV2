package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

/** Aggregate NPC society for a region; intentionally not one Bukkit entity per resident. */
@Getter
@DatabaseTable(tableName = "earth_demographics")
public class DBEarthDemographics {
    @DatabaseField(id = true, columnName = "region_id") private String regionId;
    @DatabaseField(canBeNull = false) private int residents;
    @DatabaseField(canBeNull = false) private int workforce;
    @DatabaseField(canBeNull = false) private double housingPressure;
    @DatabaseField(canBeNull = false, columnName = "last_updated_at") private long lastUpdatedAt;
    DBEarthDemographics() { }
    public DBEarthDemographics(String regionId, int residents, int workforce, double housingPressure, long lastUpdatedAt) {
        this.regionId=regionId; this.residents=Math.max(0,residents); this.workforce=Math.max(0,Math.min(workforce,residents)); this.housingPressure=clamp(housingPressure); this.lastUpdatedAt=lastUpdatedAt;
    }
    public void simulate(double prosperity, double employment, int residentialProperties, long now) {
        int capacity = Math.max(10, residentialProperties * 4 + 50);
        double growth = (prosperity + employment - housingPressure) * 0.8D;
        residents = Math.max(0, residents + (int) Math.round(growth));
        workforce = Math.max(0, Math.min(residents, (int) Math.round(residents * (0.45D + employment * 0.35D))));
        housingPressure = clamp(residents / (double) capacity);
        lastUpdatedAt = now;
    }
    private static double clamp(double value) { return Math.max(0D, Math.min(2D, value)); }
}
