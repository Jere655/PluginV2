package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

@Getter
@DatabaseTable(tableName = "earth_region_waypoints")
public class DBEarthRegionWaypoint {
    @DatabaseField(id = true, columnName = "region_id") private String regionId;
    @DatabaseField(canBeNull = false) private String world;
    @DatabaseField(canBeNull = false) private double x;
    @DatabaseField(canBeNull = false) private double y;
    @DatabaseField(canBeNull = false) private double z;
    @DatabaseField(canBeNull = false) private float yaw;
    @DatabaseField(canBeNull = false) private float pitch;
    DBEarthRegionWaypoint() { }
    public DBEarthRegionWaypoint(String regionId, Location location) { this.regionId=regionId; this.world=location.getWorld().getName();this.x=location.getX();this.y=location.getY();this.z=location.getZ();this.yaw=location.getYaw();this.pitch=location.getPitch(); }
    public Location toLocation() { return new Location(Bukkit.getWorld(world),x,y,z,yaw,pitch); }
}
